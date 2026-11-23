import CoreGraphics
import ImageIO
import UniformTypeIdentifiers
import Foundation

// Lifts a gold drawing off its dark burgundy background into a see-through PNG.
// Usage: cut-glyph <image> <x> <y> <size> <out.png> <brightness>
//   brightness 1.0 keeps the gold as painted (for the dark theme); about 0.6 deepens it to bronze (for the light theme).
let a = CommandLine.arguments
let src = CGImageSourceCreateWithURL(URL(fileURLWithPath: a[1]) as CFURL, nil)!
let full = CGImageSourceCreateImageAtIndex(src, 0, nil)!
let (x, y, size) = (Int(a[2])!, Int(a[3])!, Int(a[4])!)
let gain = Double(a[6])!
let piece = full.cropping(to: CGRect(x: x, y: y, width: size, height: size))!
let n = 192
var px = [UInt8](repeating: 0, count: n * n * 4)
let rgb = CGColorSpace(name: CGColorSpace.sRGB)!
let read = CGContext(data: &px, width: n, height: n, bitsPerComponent: 8, bytesPerRow: n * 4, space: rgb, bitmapInfo: CGImageAlphaInfo.noneSkipLast.rawValue)!
read.interpolationQuality = .high
read.draw(piece, in: CGRect(x: 0, y: 0, width: n, height: n))
let bg = (r: 46.0, g: 9.0, b: 12.0)
for i in stride(from: 0, to: px.count, by: 4) {
    let (r, g, b) = (Double(px[i]), Double(px[i + 1]), Double(px[i + 2]))
    let luma = 0.3 * r + 0.59 * g + 0.11 * b
    let alpha = min(1, max(0, (luma - 32) / 80))
    func un(_ c: Double, _ k: Double) -> UInt8 {
        guard alpha > 0 else { return 0 }
        let pure = min(255, max(0, (c - k * (1 - alpha)) / alpha)) * gain
        return UInt8(min(255, pure) * alpha) // premultiplied
    }
    px[i] = un(r, bg.r); px[i + 1] = un(g, bg.g); px[i + 2] = un(b, bg.b); px[i + 3] = UInt8(alpha * 255)
}
let out = CGContext(data: &px, width: n, height: n, bitsPerComponent: 8, bytesPerRow: n * 4, space: rgb, bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)!
let dest = CGImageDestinationCreateWithURL(URL(fileURLWithPath: a[5]) as CFURL, UTType.png.identifier as CFString, 1, nil)!
CGImageDestinationAddImage(dest, out.makeImage()!, nil)
CGImageDestinationFinalize(dest)
print("done")
