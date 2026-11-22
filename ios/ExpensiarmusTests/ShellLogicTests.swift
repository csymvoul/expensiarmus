import XCTest
@testable import Expensiarmus

final class ShellLogicTests: XCTestCase {
    func testOwlParsesAMessageFromTheWebApp() throws {
        let owl = try XCTUnwrap(Owl(message: ["date": "2026-11-22", "hour": 21, "minute": 30, "title": "🦉 Owl post: did you forget the ledger?", "body": "No expenses recorded today."]))
        XCTAssertEqual(owl.date.year, 2026)
        XCTAssertEqual(owl.date.month, 11)
        XCTAssertEqual(owl.date.day, 22)
        XCTAssertEqual(owl.date.hour, 21)
        XCTAssertEqual(owl.date.minute, 30)
        XCTAssertEqual(owl.title, "🦉 Owl post: did you forget the ledger?")
    }

    func testOwlDefaultsToHalfPastNine() throws {
        let owl = try XCTUnwrap(Owl(message: ["date": "2027-01-01", "title": "t", "body": "b"]))
        XCTAssertEqual(owl.date.hour, 21)
        XCTAssertEqual(owl.date.minute, 30)
    }

    func testOwlRejectsBrokenMessages() {
        XCTAssertNil(Owl(message: ["date": "22/11/2026", "title": "t", "body": "b"]))
        XCTAssertNil(Owl(message: ["date": "2026-11-22", "body": "b"]))
        XCTAssertNil(Owl(message: [:]))
    }

    func testJavaScriptLiteralsAreSafelyQuoted() {
        XCTAssertEqual(Backup.jsStringLiteral("#/edit"), "\"#\\/edit\"")
        XCTAssertEqual(Backup.jsStringLiteral("say \"hi\"\n</script>"), "\"say \\\"hi\\\"\\n<\\/script>\"")
        XCTAssertEqual(Backup.jsStringLiteral("Σουβλάκι €"), "\"Σουβλάκι €\"")
    }

    func testMimeTypes() {
        XCTAssertEqual(MimeType.forExtension("js"), "text/javascript; charset=utf-8")
        XCTAssertEqual(MimeType.forExtension("HTML"), "text/html; charset=utf-8")
        XCTAssertEqual(MimeType.forExtension("ttf"), "font/ttf")
        XCTAssertEqual(MimeType.forExtension("bin"), "application/octet-stream")
    }
}
