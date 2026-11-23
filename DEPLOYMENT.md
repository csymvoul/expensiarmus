# Deployment

The app is distributed as a signed APK that you install on your phone yourself (sideloading). It is not on the Play Store.

## Requirements (to build)

- JDK 17: `brew install openjdk@17`
- Android SDK command-line tools: `brew install --cask android-commandlinetools`, then:
  ```bash
  sdkmanager --sdk_root=$HOME/Library/Android/sdk "platforms;android-35" "build-tools;35.0.0" "platform-tools"
  ```
- `local.properties` in the project root containing `sdk.dir=/Users/<you>/Library/Android/sdk`

## Signing key

Release builds are signed with `keystore/expenses-release.jks`. Its passwords are in `keystore.properties`; both files are git-ignored.

**Back up both files somewhere safe** (e.g. a password manager). Android only installs an update over the existing app if it is signed with the same key. If you lose the key, the only way to install a new version is to uninstall the app first, and that deletes its data. Export a CSV first if that ever happens.

If `keystore.properties` is missing, the release build falls back to the debug key.

## Build

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
./gradlew testReleaseUnitTest      # run unit tests
./gradlew assembleRelease lintRelease   # build and lint
cp app/build/outputs/apk/release/app-release.apk Expensiarmus-<version>.apk
```

For a new version, increase `versionCode` (and `versionName`) in `app/build.gradle.kts` first.

## Install on the phone

1. Copy the APK to the phone, using any of these:
   - **USB cable:** connect the phone, pick "File transfer" on it, and copy the APK into `Download`. On a Mac this needs Android's file-transfer app, or use `adb install` (below).
   - **Google Drive:** upload the APK, open it in the Drive app on the phone and tap Install.
   - **adb:** turn on USB debugging (Settings → About phone → tap Build number 7 times → Developer options → USB debugging), then run
     `~/Library/Android/sdk/platform-tools/adb install -r Expensiarmus-1.8.0.apk`
2. Open the APK on the phone. The first time, Android asks you to allow installing unknown apps from that source (Files, Drive or Chrome). Allow it, then tap Install.
3. On Android 13+ the app asks once to allow notifications. Allow it to get the 21:30 reminder. If you missed it: Settings → Apps → Expensiarmus → Notifications.
4. To add the widget: long-press the home screen → Widgets → Expensiarmus. If the 21:30 owl still doesn't come, exempt the app from battery optimisation: Settings → Apps → Expensiarmus → Battery → Unrestricted.
5. Play Protect may warn that it doesn't recognise the app. Choose "Install anyway"; this is expected for apps not from the Play Store.

## Updating

Build with a higher `versionCode` and the same key, then install the new APK the same way. Your data is kept.

## Web app (iPhone)

The web app in `web/` is published to GitHub Pages at https://csymvoul.github.io/expensiarmus/ by the workflow `.github/workflows/pages.yml`. Every push to `main` that changes `web/` runs the tests and then deploys. It can also be started by hand from the repository's **Actions** tab ("Deploy web app to GitHub Pages" → Run workflow).

One-time setup (already done): in the repository's **Settings → Pages**, the source is **GitHub Actions**.

### Run it locally

```bash
cd web && npm test
```

```bash
python3 -m http.server 8642 --directory web
```

Then open http://127.0.0.1:8642/.

### Releasing a change

When you change any file in `web/`, also bump `VERSION` in `web/sw.js` (for example `expensiarmus-web-1.0.1`). Phones then drop the old offline copy. Installed apps pick up a new version the second time they are opened after the deploy: the first launch downloads it in the background.

### Install on an iPhone

1. Open https://csymvoul.github.io/expensiarmus/ in **Safari**. Other browsers can't add web apps to the home screen on all iOS versions.
2. Tap **Share** → **Add to Home Screen** → **Add**.
3. Open Expensiarmus from the home screen.
4. For the evening reminder, follow the steps in the app's ⚙️ Settings tab. It sets up a daily reminder in the iPhone Reminders app.

Data is stored on the phone only. Use ⚙️ Settings → 📜 Geminio · export CSV from time to time as a backup.

## iPhone app (personal use, free Apple ID)

The app in `ios/` is installed on your own iPhone with **AltStore**, which signs it with a free Apple ID and renews the signature before the 7-day limit.

### Requirements

- **Xcode** from the Mac App Store. Open it once and let it install the iOS components.
- **XcodeGen**: `brew install xcodegen`
- **AltServer** for Mac and **AltStore** on the iPhone, from https://altstore.io (follow their install guide; it needs your Apple ID, and many people use a separate free Apple ID for this).

### Build

```bash
ios/build-ipa.sh
```

This generates the Xcode project, builds the app with the current `web/` folder inside, and writes an unsigned `ios/Expensiarmus.ipa`. AltStore signs it when installing. Run the Swift unit tests with:

```bash
cd ios && xcodegen generate && xcodebuild test -project Expensiarmus.xcodeproj -scheme Expensiarmus -destination 'platform=iOS Simulator,name=iPhone 16'
```

(Pick any installed simulator for the name.)

### Install

1. Start **AltServer** on the Mac.
2. Get `ios/Expensiarmus.ipa` onto the iPhone (AirDrop or Files), open it with **AltStore**, and let it install.
3. If iOS asks: turn on **Settings → Privacy & Security → Developer Mode**, and trust your Apple ID under **Settings → General → VPN & Device Management**.
4. Open Expensiarmus and allow notifications for the 21:30 owl.

### Keep it from expiring (automatic)

A free Apple ID signature lasts 7 days. AltStore renews it in the background whenever the iPhone and the Mac are on the same Wi-Fi with AltServer running. To make that reliable:

- iPhone: **Settings → General → Background App Refresh** on for AltStore; avoid leaving Low Power Mode on all the time.
- iPhone: **Shortcuts → Automation → New → Charger → Is Connected → Run Immediately**, action **AltStore: Refresh All**. Every night on charge at home it renews the app.
- Mac: keep AltServer running and let the Mac stay awake on power (**System Settings → Battery/Energy → Prevent automatic sleeping when the display is off**), at least overnight.

If the app ever expires, it just won't open until AltStore refreshes it (open AltStore → My Apps → Refresh All with the Mac nearby). Your data is kept.

### Updating

Build again with `ios/build-ipa.sh`, AirDrop the new `.ipa` to the iPhone and install it through **AltStore → My Apps → +** (not AltServer's "Sideload .ipa", which can create a new certificate and make iOS ask to trust the developer again). Bump `MARKETING_VERSION` / `CURRENT_PROJECT_VERSION` in `ios/project.yml` for each update. The data stays in the app (and in its backup file).

### Old web address

Until 1.8 the repository was called `expenses-app`, so the web app lived at https://csymvoul.github.io/expenses-app/. A separate small repository, `csymvoul/expenses-app`, now holds only a forwarding page (and an offline worker that removes the old installed copy), so old links and home-screen icons open https://csymvoul.github.io/expensiarmus/. Saved expenses carry over, because both addresses are on the same site and share its storage.

## App icons

The icons for all three apps are made from `art/logo.jpg`:

```bash
swiftc -O -o /tmp/make-icons art/make-icons.swift && /tmp/make-icons art/logo.jpg /tmp/icons
```

Then copy `icon-512.png`, `icon-192.png`, `icon-maskable-512.png`, `apple-touch-icon.png` and `favicon-64.png` to `web/icons/`, `icon-1024.png` to `ios/Expensiarmus/Assets.xcassets/AppIcon.appiconset/`, and each `ic_launcher_foreground-<density>.png` to `app/src/main/res/mipmap-<density>/ic_launcher_foreground.png`.

## Category and tab emblems

The 15 emblems are cut from `art/emblems-categories.jpg` and `art/emblems-tabs.jpg` with `art/cut-emblems.swift` (the crop positions are listed at the top of the script):

```bash
swiftc -O -o /tmp/cut-emblems art/cut-emblems.swift && /tmp/cut-emblems art/emblems-tabs.jpg /tmp/emblems tab_ledger:80:277:213 tab_calendar:339:277:213 tab_annals:597:277:213 tab_goals:856:277:213 tab_settings:1114:277:213
```

The Settings emblems come from `art/emblems-settings.jpg` (`set_geminio:86:262:246 set_reminder:379:290:184 set_theme_system:595:267:232 set_theme_light:867:267:232 set_theme_dark:1115:267:232`) and the top-bar pouch from `art/logo.jpg` (`logo_pouch:540:280:330`).

Copy each `<name>.png` to `web/icons/emblems/<name>.png` and to `app/src/main/res/drawable-nodpi/emblem_<name>.png`. A new emblem also needs a line in `web/sw.js` so it works offline.
