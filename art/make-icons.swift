import AppKit
import CoreGraphics
import ImageIO
import UniformTypeIdentifiers

// Makes every app icon from art/logo.jpg (a gold coin pouch with a sparkling wand on burgundy).
//   swiftc -O -o /tmp/make-icons art/make-icons.swift && /tmp/make-icons art/logo.jpg <outdir>
// then copy the PNGs to web/icons, ios/.../AppIcon.appiconset and app/src/main/res/mipmap-*.
// Crops the logo art into squares and writes PNG icons.
let args = CommandLine.arguments
let src = CGImageSourceCreateWithURL(URL(fileURLWithPath: args[1]) as CFURL, nil)!
let image = CGImageSourceCreateImageAtIndex(src, 0, nil)!
let out = URL(fileURLWithPath: args[2])

func crop(centerX: Int, centerY: Int, size: Int) -> CGImage {
    image.cropping(to: CGRect(x: centerX - size / 2, y: centerY - size / 2, width: size, height: size))!
}

func write(_ img: CGImage, _ px: Int, _ name: String) {
    let ctx = CGContext(data: nil, width: px, height: px, bitsPerComponent: 8, bytesPerRow: 0,
                        space: CGColorSpace(name: CGColorSpace.sRGB)!, bitmapInfo: CGImageAlphaInfo.noneSkipLast.rawValue)!
    ctx.interpolationQuality = .high
    ctx.draw(img, in: CGRect(x: 0, y: 0, width: px, height: px))
    let dest = CGImageDestinationCreateWithURL(out.appendingPathComponent(name) as CFURL, UTType.png.identifier as CFString, 1, nil)!
    CGImageDestinationAddImage(dest, ctx.makeImage()!, nil)
    CGImageDestinationFinalize(dest)
}

// Background colour, sampled from a corner.
let ctx1 = CGContext(data: nil, width: 1, height: 1, bitsPerComponent: 8, bytesPerRow: 4,
                     space: CGColorSpace(name: CGColorSpace.sRGB)!, bitmapInfo: CGImageAlphaInfo.noneSkipLast.rawValue)!
ctx1.draw(image.cropping(to: CGRect(x: 20, y: 20, width: 1, height: 1))!, in: CGRect(x: 0, y: 0, width: 1, height: 1))
let p = ctx1.data!.assumingMemoryBound(to: UInt8.self)
print(String(format: "background #%02X%02X%02X", p[0], p[1], p[2]))

// The art spans about x 490–915, y 160–605 of the 1408×768 picture; centre (702, 383).
let tight = crop(centerX: 702, centerY: 383, size: 600)   // art fills ~74%: iPhone and web icons
let roomy = crop(centerX: 702, centerY: 383, size: 760)   // art fills ~58%: inside Android's safe zone
write(tight, 1024, "icon-1024.png")
write(tight, 512, "icon-512.png")
write(tight, 192, "icon-192.png")
write(tight, 180, "apple-touch-icon.png")
write(tight, 64, "favicon-64.png")
write(roomy, 512, "icon-maskable-512.png")
for (dpi, px) in [("mdpi", 108), ("hdpi", 162), ("xhdpi", 216), ("xxhdpi", 324), ("xxxhdpi", 432)] {
    write(roomy, px, "ic_launcher_foreground-\(dpi).png")
}
write(roomy, 512, "android-preview.png")
print("written")
