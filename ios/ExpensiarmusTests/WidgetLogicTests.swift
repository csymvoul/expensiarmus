import XCTest
@testable import Expensiarmus

final class WidgetLogicTests: XCTestCase {
    private let message: [String: Any] = [
        "month": "2026-11", "heading": "Expensiarmus · November", "total": "€560.00", "label": "Gold spent this month",
        "wards": [["text": "Whole month · €560.00 of €1,000.00", "percent": 56, "over": false, "icon": "logo_pouch"]],
        "empty": NSNull(),
        "trips": [
            ["name": "Rome", "start": "2026-11-20", "end": "2026-11-28", "spent": "€60.00", "budget": "€500.00"],
            ["name": "Xmas", "start": "2026-12-23", "end": "2026-12-27", "spent": "€0.00", "budget": NSNull()],
        ],
    ]

    func testTheWebAppsMessageBecomesASnapshot() throws {
        let s = try XCTUnwrap(WidgetSnapshot(message: message))
        XCTAssertEqual(s.total, "€560.00")
        XCTAssertEqual(s.wards, [WidgetSnapshot.Ward(text: "Whole month · €560.00 of €1,000.00", percent: 56, over: false, icon: "logo_pouch")])
        XCTAssertNil(s.empty)
        XCTAssertNil(s.trips[1].budget)
        XCTAssertNil(WidgetSnapshot(message: ["month": "2026-11"]))
        XCTAssertNil(WidgetSnapshot(message: nil))
    }

    func testTheTripLineFollowsTheDayWhileTheAppIsClosed() throws {
        let s = try XCTUnwrap(WidgetSnapshot(message: message))
        XCTAssertEqual(s.tripLine(on: "2026-11-22"), "Rome · day 3 of 9 · €60.00 of €500.00")
        XCTAssertEqual(s.tripLine(on: "2026-11-28"), "Rome · day 9 of 9 · €60.00 of €500.00")
        XCTAssertNil(s.tripLine(on: "2026-11-29"))
        XCTAssertEqual(s.tripLine(on: "2026-12-24"), "Xmas · day 2 of 5 · €0.00")
    }

    func testASnapshotFromLastMonthIsNotShownAsThisMonth() throws {
        let s = try XCTUnwrap(WidgetSnapshot(message: message))
        XCTAssertTrue(s.isFor(day: "2026-11-30"))
        XCTAssertFalse(s.isFor(day: "2026-12-01"))
    }

    func testTheDayIsTakenInThePhonesTimeZone() {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "Europe/Athens")!
        // 23:30 UTC on 30 November is already 1 December in Athens.
        XCTAssertEqual(WidgetSnapshot.day(of: Date(timeIntervalSince1970: 1_796_081_400), calendar: calendar), "2026-12-01")
    }

    func testTheAppGroupIsReadFromTheProvisioningProfile() {
        let profile = Data(("garbage\u{0}<?xml version=\"1.0\"?><plist version=\"1.0\"><dict><key>Entitlements</key><dict>"
            + "<key>com.apple.security.application-groups</key><array><string>group.other</string>"
            + "<string>group.com.csymvoul.expensiarmus.ABCDE12345</string></array></dict></dict></plist>\u{1}tail").utf8)
        XCTAssertEqual(AppGroup.groups(inProvisioningProfile: profile), ["group.other", "group.com.csymvoul.expensiarmus.ABCDE12345"])
        XCTAssertEqual(AppGroup.pick(from: AppGroup.groups(inProvisioningProfile: profile)), "group.com.csymvoul.expensiarmus.ABCDE12345")
        XCTAssertEqual(AppGroup.pick(from: ["group.com.csymvoul.expensiarmus.X", "group.com.csymvoul.expensiarmus"]), "group.com.csymvoul.expensiarmus")
        XCTAssertEqual(AppGroup.pick(from: []), "group.com.csymvoul.expensiarmus")
        XCTAssertEqual(AppGroup.groups(inProvisioningProfile: nil), [])
    }

    func testEveryEmblemTheWebAppNamesIsInTheWidgetsAssets() throws {
        // The widget bundle's asset catalog is checked at build time; here: the names the web app can send.
        let names = ["logo_pouch", "trip", "cat_rent", "cat_supermarket", "cat_entertainment", "cat_bills", "cat_shopping", "cat_fuel", "cat_other"]
        let catalog = URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent()
            .appendingPathComponent("ExpensiarmusWidget/Assets.xcassets")
        for name in names {
            XCTAssertTrue(FileManager.default.fileExists(atPath: catalog.appendingPathComponent("\(name).imageset/\(name).png").path), name)
        }
        for look in ["light", "dark"] {
            XCTAssertTrue(FileManager.default.fileExists(atPath: catalog.appendingPathComponent("quill.imageset/quill_\(look).png").path), look)
        }
    }

    func testWidgetLinksOpenTheirScreen() {
        XCTAssertEqual(ExpensiarmusApp.route(from: URL(string: "expensiarmus://localhost/index.html#/edit")!), "#/edit")
        XCTAssertEqual(ExpensiarmusApp.route(from: URL(string: "expensiarmus://localhost/index.html#/")!), "#/")
        XCTAssertNil(ExpensiarmusApp.route(from: URL(string: "https://example.com/#/edit")!))
        XCTAssertNil(ExpensiarmusApp.route(from: URL(string: "expensiarmus://localhost/index.html")!))
    }
}
