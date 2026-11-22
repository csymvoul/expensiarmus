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
     `~/Library/Android/sdk/platform-tools/adb install -r Expensiarmus-1.3.0.apk`
2. Open the APK on the phone. The first time, Android asks you to allow installing unknown apps from that source (Files, Drive or Chrome). Allow it, then tap Install.
3. On Android 13+ the app asks once to allow notifications. Allow it to get the 21:30 reminder. If you missed it: Settings → Apps → Expensiarmus → Notifications.
4. If the reminder never shows, exempt the app from battery optimisation: Settings → Apps → Expensiarmus → Battery → Unrestricted.
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
