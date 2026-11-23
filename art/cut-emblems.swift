import AppKit
import CoreGraphics
import ImageIO
import UniformTypeIdentifiers

// Usage: swiftc art/cut-emblems.swift -o /tmp/cut && /tmp/cut <image> <outdir> name:x:y:size ...  (crops squares, writes 192px PNGs).
// Tabs from art/emblems-tabs.jpg: y 277, size 213, x 80/339/597/856/1114 (ledger, calendar, annals, goals, settings).
// Categories from art/emblems-categories.jpg: row 1 y 118 size 192, row 2 y 430 size 190, x 91/350/608/867/1125
//   (rent, supermarket, entertainment, bills, shopping / fuel, other, food_delivery, food_dine_out, food_day_to_day).
// Settings, logo pouch and search: see DEPLOYMENT.md, "Category and tab emblems".
let args = CommandLine.arguments
let src = CGImageSourceCreateWithURL(URL(fileURLWithPath: args[1]) as CFURL, nil)!
let image = CGImageSourceCreateImageAtIndex(src, 0, nil)!
let out = URL(fileURLWithPath: args[2])
for spec in args.dropFirst(3) {
    let p = spec.split(separator: ":")
    let (name, x, y, size) = (String(p[0]), Int(p[1])!, Int(p[2])!, Int(p[3])!)
    let piece = image.cropping(to: CGRect(x: x, y: y, width: size, height: size))!
    let ctx = CGContext(data: nil, width: 192, height: 192, bitsPerComponent: 8, bytesPerRow: 0,
                        space: CGColorSpace(name: CGColorSpace.sRGB)!, bitmapInfo: CGImageAlphaInfo.noneSkipLast.rawValue)!
    ctx.interpolationQuality = .high
    ctx.draw(piece, in: CGRect(x: 0, y: 0, width: 192, height: 192))
    let dest = CGImageDestinationCreateWithURL(out.appendingPathComponent("\(name).png") as CFURL, UTType.png.identifier as CFString, 1, nil)!
    CGImageDestinationAddImage(dest, ctx.makeImage()!, nil)
    CGImageDestinationFinalize(dest)
}
print("done")
