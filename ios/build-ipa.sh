#!/bin/sh
# Builds Expensiarmus.ipa for AltStore (which signs it with your free Apple ID).
# Needs Xcode and XcodeGen. Usage: ios/build-ipa.sh
set -eu
cd "$(dirname "$0")"
xcodegen generate --quiet
xcodebuild -project Expensiarmus.xcodeproj -scheme Expensiarmus -configuration Release \
  -sdk iphoneos -destination 'generic/platform=iOS' -derivedDataPath build \
  CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO build -quiet
rm -rf build/ipa && mkdir -p build/ipa/Payload
cp -R build/Build/Products/Release-iphoneos/Expensiarmus.app build/ipa/Payload/
(cd build/ipa && zip -qry ../../Expensiarmus.ipa Payload)
echo "Built $(pwd)/Expensiarmus.ipa"
