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
     `~/Library/Android/sdk/platform-tools/adb install -r Expensiarmus-1.2.1.apk`
2. Open the APK on the phone. The first time, Android asks you to allow installing unknown apps from that source (Files, Drive or Chrome). Allow it, then tap Install.
3. On Android 13+ the app asks once to allow notifications. Allow it to get the 21:30 reminder. If you missed it: Settings → Apps → Expensiarmus → Notifications.
4. If the reminder never shows, exempt the app from battery optimisation: Settings → Apps → Expensiarmus → Battery → Unrestricted.
5. Play Protect may warn that it doesn't recognise the app. Choose "Install anyway"; this is expected for apps not from the Play Store.

## Updating

Build with a higher `versionCode` and the same key, then install the new APK the same way. Your data is kept.
