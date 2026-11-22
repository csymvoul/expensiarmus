import SwiftUI
import UserNotifications

/// Expensiarmus for iPhone: the web app, bundled inside a native shell that adds the
/// 21:30 notification, the iPhone share sheet and a backup of the data in the app's Documents.
@main
struct ExpensiarmusApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate
    @Environment(\.colorScheme) private var colorScheme

    var body: some Scene {
        WindowGroup {
            WebView(shell: appDelegate.shell)
                .ignoresSafeArea()
                // Parchment by day, castle by night, so there is no white flash while the page loads.
                .background(colorScheme == .dark ? Color(red: 0.09, green: 0.07, blue: 0.055) : Color(red: 0.953, green: 0.902, blue: 0.784))
        }
    }
}

final class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    let shell = Shell()

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        return true
    }

    /// Show the owl even when the app happens to be open at 21:30.
    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification) async -> UNNotificationPresentationOptions {
        [.banner, .sound]
    }

    /// Tapping the owl opens "Record expense".
    func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse) async {
        let route = response.notification.request.content.userInfo["route"] as? String ?? "#/edit"
        await MainActor.run { shell.open(route: route) }
    }
}
