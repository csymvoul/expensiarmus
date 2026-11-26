import SwiftUI
import WidgetKit

/// The Ledger on the home screen: this month's total, the trip on today and up to three wards.
/// Tapping opens the app; on the medium widget the ＋ opens Quick add.
@main
struct LedgerWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "LedgerWidget", provider: LedgerProvider()) { entry in
            LedgerWidgetView(entry: entry)
        }
        .configurationDisplayName("The Ledger")
        .description("This month's gold, your wards and the trip you are on.")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}

struct LedgerEntry: TimelineEntry {
    let date: Date
    let snapshot: WidgetSnapshot?
}

/// One entry now and one at each of the next seven midnights (the trip's day moves on, the month may end).
struct LedgerProvider: TimelineProvider {
    func placeholder(in context: Context) -> LedgerEntry { LedgerEntry(date: Date(), snapshot: .sample) }

    func getSnapshot(in context: Context, completion: @escaping (LedgerEntry) -> Void) {
        completion(LedgerEntry(date: Date(), snapshot: context.isPreview ? (WidgetSnapshot.load() ?? .sample) : WidgetSnapshot.load()))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<LedgerEntry>) -> Void) {
        let snapshot = WidgetSnapshot.load()
        let calendar = Calendar.current
        let midnight = calendar.startOfDay(for: Date())
        let dates = [Date()] + (1...7).compactMap { calendar.date(byAdding: .day, value: $0, to: midnight) }
        completion(Timeline(entries: dates.map { LedgerEntry(date: $0, snapshot: snapshot) }, policy: .atEnd))
    }
}

extension WidgetSnapshot {
    static let sample = WidgetSnapshot(
        month: WidgetSnapshot.day(of: Date()).prefix(7).description,
        heading: "Expensiarmus", total: "€560.00", label: "Gold spent this month",
        wards: [Ward(text: "Whole month · €560.00 of €1,000.00", percent: 56, over: false, icon: "logo_pouch")],
        empty: nil, trips: []
    )
}

/// Parchment by day, a candle-lit castle by night (the same colours as the Android widget).
private struct Palette {
    let paper, gold, heading, ink, soft, track: Color

    static func of(_ scheme: ColorScheme) -> Palette {
        scheme == .dark
            ? Palette(paper: hex(0x241C16), gold: hex(0xD9A441), heading: hex(0xE0B656), ink: hex(0xEDE0C4), soft: hex(0xCDBB98), track: hex(0x3A2E22))
            : Palette(paper: hex(0xF8EDD3), gold: hex(0xA87A1E), heading: hex(0x7B1E1E), ink: hex(0x3B2A1A), soft: hex(0x6B5236), track: hex(0xE6D3AA))
    }

    private static func hex(_ v: Int) -> Color {
        Color(red: Double((v >> 16) & 0xFF) / 255, green: Double((v >> 8) & 0xFF) / 255, blue: Double(v & 0xFF) / 255)
    }
}

enum WidgetLinks {
    static let ledger = URL(string: "expensiarmus://localhost/index.html#/")!
    static let quickAdd = URL(string: "expensiarmus://localhost/index.html#/edit")!
}

struct LedgerWidgetView: View {
    let entry: LedgerEntry
    @Environment(\.colorScheme) private var scheme
    @Environment(\.widgetFamily) private var family

    var body: some View {
        let p = Palette.of(scheme)
        content(p)
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
            .widgetURL(WidgetLinks.ledger)
            .modifier(PaperBackground(color: p.paper))
    }

    @ViewBuilder
    private func content(_ p: Palette) -> some View {
        let today = WidgetSnapshot.day(of: entry.date)
        if let s = entry.snapshot, s.isFor(day: today) {
            VStack(alignment: .leading, spacing: 3) {
                HStack(alignment: .center, spacing: 5) {
                    Emblem(name: "logo_pouch", size: 18)
                    Text(family == .systemSmall ? String(s.heading.split(separator: "·").last ?? "").trimmingCharacters(in: .whitespaces) : s.heading)
                        .font(.system(size: 12, weight: .bold, design: .serif)).foregroundStyle(p.heading).lineLimit(1)
                    Spacer(minLength: 4)
                    if family != .systemSmall {
                        // Quick add: the quill glyph (bronze by day, gold by night), drawn without a badge.
                        Link(destination: WidgetLinks.quickAdd) { Emblem(name: "quill", size: 28) }
                    }
                }
                Text(s.total).font(.system(size: family == .systemSmall ? 24 : 28, weight: .bold, design: .serif))
                    .foregroundStyle(p.ink).minimumScaleFactor(0.6).lineLimit(1)
                Text(s.label).font(.system(size: 11, design: .serif)).foregroundStyle(p.soft).lineLimit(1)
                if let trip = s.tripLine(on: today) {
                    HStack(alignment: .center, spacing: 5) {
                        Emblem(name: "trip", size: 16)
                        Text(trip).font(.system(size: 11, weight: .bold, design: .serif)).foregroundStyle(p.heading)
                            .lineLimit(family == .systemSmall ? 2 : 1)
                    }
                    .padding(.top, 2)
                }
                Spacer(minLength: 2)
                let wards = family == .systemSmall ? Array(s.wards.prefix(1)) : s.wards
                if wards.isEmpty, let empty = s.empty {
                    Text(empty).font(.system(size: 11, design: .serif)).foregroundStyle(p.soft).lineLimit(2)
                }
                ForEach(Array(wards.enumerated()), id: \.offset) { _, ward in
                    VStack(alignment: .leading, spacing: 2) {
                        HStack(alignment: .center, spacing: 4) {
                            if let icon = ward.icon { Emblem(name: icon, size: 14) }
                            // Over its goal the line turns burgundy (gold by night), like the app's broken ward.
                            Text(ward.text).font(.system(size: 10, design: .serif)).foregroundStyle(ward.over ? p.heading : p.ink).lineLimit(1)
                        }
                        Bar(percent: ward.percent, fill: ward.over ? p.heading : p.gold, track: p.track)
                    }
                }
            }
        } else {
            VStack(alignment: .leading, spacing: 6) {
                HStack(spacing: 5) {
                    Emblem(name: "logo_pouch", size: 18)
                    Text("Expensiarmus").font(.system(size: 13, weight: .bold, design: .serif)).foregroundStyle(p.heading)
                }
                Spacer()
                Text("Open the app to fill the Ledger.").font(.system(size: 12, design: .serif)).foregroundStyle(p.soft)
            }
        }
    }
}

/// One of the app's painted emblems (the same PNGs as the web app and Android), from the widget's assets.
private struct Emblem: View {
    let name: String
    let size: CGFloat

    var body: some View {
        Image(name).resizable().interpolation(.high).scaledToFit().frame(width: size, height: size).accessibilityHidden(true)
    }
}

private struct Bar: View {
    let percent: Int
    let fill: Color
    let track: Color

    var body: some View {
        GeometryReader { g in
            ZStack(alignment: .leading) {
                Capsule().fill(track)
                Capsule().fill(fill).frame(width: g.size.width * CGFloat(min(max(percent, 0), 100)) / 100)
            }
        }
        .frame(height: 4)
    }
}

/// iOS 17 wants the widget's background declared; iOS 16 just draws it.
private struct PaperBackground: ViewModifier {
    let color: Color

    func body(content: Content) -> some View {
        if #available(iOS 17.0, *) {
            content.containerBackground(color, for: .widget)
        } else {
            content.padding().background(color)
        }
    }
}
