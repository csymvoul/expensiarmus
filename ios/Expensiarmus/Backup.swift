import Foundation

/// A copy of the app's data in Documents/expensiarmus-data.json. It survives if iOS ever clears
/// the web view's storage, and it is visible in the Files app (On My iPhone → Expensiarmus).
struct Backup {
    static let storageKey = "expensiarmus.data.v1"

    private var url: URL {
        FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("expensiarmus-data.json")
    }

    func write(_ json: String) {
        try? json.write(to: url, atomically: true, encoding: .utf8)
    }

    /// JavaScript that puts the backup into the page's storage when the page has none.
    func restoreScript() -> String? {
        guard let json = try? String(contentsOf: url, encoding: .utf8), !json.isEmpty else { return nil }
        let key = Self.jsStringLiteral(Self.storageKey)
        return "if (!localStorage.getItem(\(key))) { localStorage.setItem(\(key), \(Self.jsStringLiteral(json))); }"
    }

    /// A safely quoted JavaScript string literal for any text.
    static func jsStringLiteral(_ text: String) -> String {
        guard let data = try? JSONSerialization.data(withJSONObject: [text]),
              let array = String(data: data, encoding: .utf8) else { return "\"\"" }
        return String(array.dropFirst().dropLast())
    }
}
