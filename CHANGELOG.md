# GG Player - Changelog

Newest entry first. Every step zip must add an entry here and bump version.properties.

## Step 1 - version 0.1.0 (build 1) - 2026-11-21

Project shell and build pipeline. No playback features yet.

Added
- Android project: Kotlin 1.9.24, Jetpack Compose (BOM 2024.06.00), Material 3, AGP 8.5.2, Gradle 8.7, Java 17
- One APK for Fire TV / Android TV and phones or tablets (minSdk 22, targetSdk 34)
- Manifest: LAUNCHER and LEANBACK_LAUNCHER entries, TV banner, touchscreen and leanback marked optional, cleartext HTTP allowed (many IPTV servers use http)
- Adaptive layout: TV gets a left side rail and wider margins, phone gets a bottom text bar. Detected in DeviceInfo.isTv()
- Remote control focus: FocusSurface draws a gold outline on the focused item. Rail takes focus on launch. Back returns to Home, then exits
- Sections: Home, Live TV, Movies & Series, Settings. Everything not built yet is labelled "Not built yet" with the step it arrives in
- Theme taken from the logo (slate background, gold and blue accents), no emojis
- Launcher icon, round icon and TV banner made from the supplied GG logo
- Fixed release signing key (keystore/gg-release.jks) so each build installs over the last one
- version.properties holds VERSION_NAME and VERSION_CODE
- GitHub Actions workflow (reference copy in workflow/build.yml): unpacks GG-PLAYER-step*.zip, commits it, builds the APK and publishes a GitHub Release

Known
- Step 1 was written without a local Android build. The first Actions run is the first real compile. If it fails, the error text from the failed step is needed to fix it.
