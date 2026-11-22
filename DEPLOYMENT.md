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
     `~/Library/Android/sdk/platform-tools/adb install -r Expensiarmus-1.4.1.apk`
2. Open the APK on the phone. The first time, Android asks you to allow installing unknown apps from that source (Files, Drive or Chrome). Allow it, then tap Install.
3. On Android 13+ the app asks once to allow notifications. Allow it to get the 21:30 reminder. If you missed it: Settings → Apps → Expensiarmus → Notifications.
4. Check the reminder with ⋮ → 🦉 Send a test owl now. If the 21:30 owl still doesn't come, exempt the app from battery optimisation: Settings → Apps → Expensiarmus → Battery → Unrestricted.
5. Play Protect may warn that it doesn't recognise the app. Choose "Install anyway"; this is expected for apps not from the Play Store.

## Updating

Build with a higher `versionCode` and the same key, then install the new APK the same way. Your data is kept.

## Web app (iPhone)

The web app in `web/` is published to GitHub Pages at https://csymvoul.github.io/expenses-app/ by the workflow `.github/workflows/pages.yml`. Every push to `main` that changes `web/` runs the tests and then deploys. It can also be started by hand from the repository's **Actions** tab ("Deploy web app to GitHub Pages" → Run workflow).

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

1. Open https://csymvoul.github.io/expenses-app/ in **Safari**. Other browsers can't add web apps to the home screen on all iOS versions.
2. Tap **Share** → **Add to Home Screen** → **Add**.
3. Open Expensiarmus from the home screen.
4. For the 21:30 reminder, follow the app's ⋮ → 🦉 Owl reminder screen. It sets up a daily reminder in the iPhone Reminders app.

Data is stored on the phone only. Use ⋮ → 📜 Geminio · export CSV from time to time as a backup.

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
