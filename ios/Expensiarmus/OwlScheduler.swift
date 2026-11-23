import Foundation
import UserNotifications

/// One evening's reminder, as sent by the web app (see web/js/reminder.js).
struct Owl {
    let date: DateComponents
    let title: String
    let body: String

    init?(message: [String: Any]) {
        guard let day = message["date"] as? String, let title = message["title"] as? String, let body = message["body"] as? String else { return nil }
        let parts = day.split(separator: "-").compactMap { Int($0) }
        guard parts.count == 3 else { return nil }
        var date = DateComponents()
        date.year = parts[0]
        date.month = parts[1]
        date.day = parts[2]
        date.hour = message["hour"] as? Int ?? 21
        date.minute = message["minute"] as? Int ?? 30
        self.date = date
        self.title = title
        self.body = body
    }
}

/// Replaces the scheduled 21:30 notifications with the ones the web app sends. They are local
/// notifications, which a free Apple developer account is allowed to use.
final class OwlScheduler {
    private let center = UNUserNotificationCenter.current()
    private let prefix = "owl-"

    /// Shows a notification straight away, e.g. a goal warning. The same [id] replaces the earlier one,
    /// so the over-the-goal warning replaces the 80% one for that category and month.
    func notifyNow(id: String, title: String, body: String) {
        center.requestAuthorization(options: [.alert, .sound]) { granted, _ in
            guard granted else { return }
            let content = UNMutableNotificationContent()
            content.title = title
            content.body = body
            content.sound = .default
            content.threadIdentifier = "goals"
            self.center.add(UNNotificationRequest(identifier: id, content: content, trigger: nil))
        }
    }

    /// `report` receives "granted" or "denied" so the web app can explain a refusal.
    func replace(with owls: [Owl], enabled: Bool, report: @escaping (String) -> Void) {
        center.getPendingNotificationRequests { pending in
            let old = pending.map(\.identifier).filter { $0.hasPrefix(self.prefix) }
            self.center.removePendingNotificationRequests(withIdentifiers: old)
            guard enabled, !owls.isEmpty else { return }
            self.center.requestAuthorization(options: [.alert, .sound]) { granted, _ in
                DispatchQueue.main.async { report(granted ? "granted" : "denied") }
                guard granted else { return }
                for owl in owls {
                    let content = UNMutableNotificationContent()
                    content.title = owl.title
                    content.body = owl.body
                    content.sound = .default
                    content.threadIdentifier = "owl"
                    content.userInfo = ["route": "#/edit"]
                    let id = self.prefix + String(format: "%04d-%02d-%02d", owl.date.year ?? 0, owl.date.month ?? 0, owl.date.day ?? 0)
                    let trigger = UNCalendarNotificationTrigger(dateMatching: owl.date, repeats: false)
                    self.center.add(UNNotificationRequest(identifier: id, content: content, trigger: trigger))
                }
            }
        }
    }
}
