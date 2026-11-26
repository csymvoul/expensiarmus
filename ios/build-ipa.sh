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
# Sign ad hoc with the entitlements (the App Group the app and its widget share), so AltStore
# sees them and asks for them when it signs the app with your Apple ID. Widget first, then the app.
APP=build/ipa/Payload/Expensiarmus.app
codesign -f -s - --entitlements ExpensiarmusWidget/ExpensiarmusWidget.entitlements "$APP/PlugIns/ExpensiarmusWidget.appex"
codesign -f -s - --entitlements Expensiarmus/Expensiarmus.entitlements "$APP"
(cd build/ipa && zip -qry ../../Expensiarmus.ipa Payload)
echo "Built $(pwd)/Expensiarmus.ipa"
