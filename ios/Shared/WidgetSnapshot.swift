import Foundation

/// What the home-screen widget shows. The web app works it out (web/js/widget.js), the app saves it
/// in the App Group container, and the widget reads it. The widget picks the trip on each day itself,
/// so a trip's "day 3 of 9" stays right while the app is closed.
struct WidgetSnapshot: Codable, Equatable {
    struct Ward: Codable, Equatable {
        let text: String
        let percent: Int
        let over: Bool
        /// The painted emblem shown before it ("cat_rent", "logo_pouch"), from the widget's assets.
        var icon: String? = nil
    }

    struct Trip: Codable, Equatable {
        let name: String
        let start: String
        let end: String
        let spent: String
        let budget: String?
    }

    let month: String
    let heading: String
    let total: String
    let label: String
    let wards: [Ward]
    let empty: String?
    let trips: [Trip]

    static let fileName = "widget.json"

    /// Was this worked out for the month that [day] ("2026-11-22") is in?
    func isFor(day: String) -> Bool { month == String(day.prefix(7)) }

    /// "Rome · day 3 of 9 · €60.00 of €500.00" (shown after the trip emblem) for the trip on [day] (the one started last, as in the app), or nil.
    func tripLine(on day: String) -> String? {
        guard let trip = trips.filter({ $0.start <= day && day <= $0.end }).max(by: { $0.start < $1.start }),
              let length = Self.daysBetween(trip.start, trip.end), let gone = Self.daysBetween(trip.start, day) else { return nil }
        return "\(trip.name) · day \(gone + 1) of \(length + 1) · \(trip.spent)" + (trip.budget.map { " of \($0)" } ?? "")
    }

    /// "2026-11-22" for [date] in the phone's own time zone.
    static func day(of date: Date, calendar: Calendar = .current) -> String {
        let c = calendar.dateComponents([.year, .month, .day], from: date)
        return String(format: "%04d-%02d-%02d", c.year ?? 0, c.month ?? 0, c.day ?? 0)
    }

    static func daysBetween(_ a: String, _ b: String) -> Int? {
        let f = DateFormatter()
        f.calendar = Calendar(identifier: .gregorian)
        f.locale = Locale(identifier: "en_US_POSIX")
        f.timeZone = TimeZone(identifier: "UTC")
        f.dateFormat = "yyyy-MM-dd"
        guard let x = f.date(from: a), let y = f.date(from: b) else { return nil }
        return Int((y.timeIntervalSince(x) / 86_400).rounded())
    }

    /// The snapshot sent by the web app ({ type: 'widget', content: … }).
    init?(message: Any?) {
        guard let object = message, JSONSerialization.isValidJSONObject(object),
              let data = try? JSONSerialization.data(withJSONObject: object),
              let snapshot = try? JSONDecoder().decode(WidgetSnapshot.self, from: data) else { return nil }
        self = snapshot
    }

    init(month: String, heading: String, total: String, label: String, wards: [Ward], empty: String?, trips: [Trip]) {
        (self.month, self.heading, self.total, self.label, self.wards, self.empty, self.trips) = (month, heading, total, label, wards, empty, trips)
    }

    // MARK: the shared file

    static func load() -> WidgetSnapshot? {
        guard let url = AppGroup.containerURL?.appendingPathComponent(fileName), let data = try? Data(contentsOf: url) else { return nil }
        return try? JSONDecoder().decode(WidgetSnapshot.self, from: data)
    }

    /// Saves it for the widget; false when the app has no App Group (then the widget asks to open the app).
    @discardableResult
    func save() -> Bool {
        guard let url = AppGroup.containerURL?.appendingPathComponent(Self.fileName), let data = try? JSONEncoder().encode(self) else { return false }
        return (try? data.write(to: url, options: .atomic)) != nil
    }
}

/// The App Group the app and its widget share. Sideloading tools (AltStore) may give the group a
/// different name when they sign the app, so the name is read from the app's own provisioning profile.
enum AppGroup {
    static let base = "group.com.csymvoul.expensiarmus"

    static var identifier: String {
        let profile = Bundle.main.url(forResource: "embedded", withExtension: "mobileprovision").flatMap { try? Data(contentsOf: $0) }
        return pick(from: groups(inProvisioningProfile: profile))
    }

    static var containerURL: URL? {
        FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: identifier)
    }

    /// Ours from a list of groups: the plain name, or one a signing tool made from it; else the plain name.
    static func pick(from groups: [String]) -> String {
        groups.first { $0 == base } ?? groups.first { $0.hasPrefix(base) } ?? base
    }

    /// The application groups in a provisioning profile (a signed envelope around an XML plist).
    static func groups(inProvisioningProfile data: Data?) -> [String] {
        guard let data, let text = String(data: data, encoding: .isoLatin1),
              let start = text.range(of: "<?xml"), let end = text.range(of: "</plist>", range: start.lowerBound..<text.endIndex),
              let xml = String(text[start.lowerBound..<end.upperBound]).data(using: .isoLatin1),
              let plist = try? PropertyListSerialization.propertyList(from: xml, format: nil) as? [String: Any],
              let entitlements = plist["Entitlements"] as? [String: Any] else { return [] }
        return entitlements["com.apple.security.application-groups"] as? [String] ?? []
    }
}
