import Foundation

/// Content types for the files of the bundled web app.
enum MimeType {
    static func forExtension(_ ext: String) -> String {
        switch ext.lowercased() {
        case "html": return "text/html; charset=utf-8"
        case "js": return "text/javascript; charset=utf-8"
        case "css": return "text/css; charset=utf-8"
        case "json", "webmanifest": return "application/json"
        case "svg": return "image/svg+xml"
        case "png": return "image/png"
        case "ttf": return "font/ttf"
        default: return "application/octet-stream"
        }
    }
}
