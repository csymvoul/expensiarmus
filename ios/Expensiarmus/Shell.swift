import UIKit
import WebKit

/// Receives messages from the web app (`window.webkit.messageHandlers.expensiarmus`) and does
/// what a web page can't: schedule notifications, open the share sheet, keep a backup file.
final class Shell: NSObject, WKScriptMessageHandler, WKNavigationDelegate {
    static let scheme = "expensiarmus"
    static let startURL = URL(string: "\(scheme)://localhost/index.html")!

    weak var webView: WKWebView?
    private var pendingRoute: String?
    private let owls = OwlScheduler()
    private let backup = Backup()

    /// Called once the web view exists. Restores the backup into the page's storage if the
    /// page's own copy is gone, then loads the app.
    func attach(to webView: WKWebView) {
        self.webView = webView
        webView.navigationDelegate = self
        if let script = backup.restoreScript() {
            webView.configuration.userContentController.addUserScript(
                WKUserScript(source: script, injectionTime: .atDocumentStart, forMainFrameOnly: true)
            )
        }
        var url = Self.startURL
        if let route = pendingRoute, var parts = URLComponents(url: url, resolvingAgainstBaseURL: false) {
            parts.fragment = String(route.dropFirst())
            url = parts.url ?? url
            pendingRoute = nil
        }
        webView.load(URLRequest(url: url))
    }

    /// Opens a screen of the web app, e.g. "#/edit" from a tapped notification.
    func open(route: String) {
        guard let webView, !webView.isLoading else {
            pendingRoute = route
            return
        }
        let literal = Backup.jsStringLiteral(route)
        webView.evaluateJavaScript("window.expensiarmusOpen && window.expensiarmusOpen(\(literal))")
    }

    /// A notification tapped while the page was still loading is handled once it has loaded.
    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        if let route = pendingRoute {
            pendingRoute = nil
            open(route: route)
        }
    }

    func userContentController(_ controller: WKUserContentController, didReceive message: WKScriptMessage) {
        guard let body = message.body as? [String: Any], let type = body["type"] as? String else { return }
        switch type {
        case "save":
            if let json = body["json"] as? String { backup.write(json) }
        case "schedule":
            let enabled = body["enabled"] as? Bool ?? false
            let reminders = (body["reminders"] as? [[String: Any]] ?? []).compactMap(Owl.init(message:))
            owls.replace(with: reminders, enabled: enabled) { [weak self] status in
                self?.report(owlStatus: status)
            }
        case "share":
            if let name = body["filename"] as? String, let text = body["text"] as? String { share(text: text, filename: name) }
        default:
            break
        }
    }

    private func report(owlStatus: String) {
        let literal = Backup.jsStringLiteral(owlStatus)
        webView?.evaluateJavaScript("window.expensiarmusOwlStatus = \(literal); window.dispatchEvent(new Event('owlstatus'))")
    }

    /// Writes the CSV to a temporary file and offers it through the iPhone share sheet.
    private func share(text: String, filename: String) {
        let url = FileManager.default.temporaryDirectory.appendingPathComponent(filename)
        do {
            try text.write(to: url, atomically: true, encoding: .utf8)
        } catch {
            return
        }
        guard let presenter = webView?.window?.rootViewController else { return }
        let sheet = UIActivityViewController(activityItems: [url], applicationActivities: nil)
        sheet.popoverPresentationController?.sourceView = webView
        presenter.present(sheet, animated: true)
    }
}
