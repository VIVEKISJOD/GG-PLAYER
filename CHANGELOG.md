# GG Player - Changelog

Newest entry first. Every step zip must add an entry here and bump version.properties.

## Step 2 - version 0.2.0 (build 2) - 2026-11-21

New Cinevia style UI, settings, in-app updater and providers.

Added
- New look on phone and TV: near black background, orange accent, rounded cards, icon bottom bar (phone) and icon side rail (TV), hero card, category circles, provider cards, Trending row placeholder
- TV: focused items get an accent outline and grow slightly, margins and text are larger. Same components as phone so both look alike
- In-app updater (Settings > Updates): checks the latest GitHub Release, shows size and notes, downloads with progress, opens the installer. Optional auto check on start, "Update available" banner on Home, editable GitHub repo (default VIVEKISJOD/GG-PLAYER). Manifest has REQUEST_INSTALL_PACKAGES and a FileProvider
- Settings screen: updates, check on start, appearance and layout (accent colour, text size, layout Auto or TV or Phone, TV safe margin), start screen, reset, about. Unbuilt options are listed under "Coming in step 3" and labelled "Not built yet"
- Providers: add, edit, delete IPTV providers (M3U link or Xtream login, optional XMLTV link), saved on the device, "Test connection" button that really contacts the provider
- Dependency: kotlinx-coroutines-android 1.8.1. Lint no longer stops the release build

Changed
- Palette changed from slate and gold to near black and orange (accent can be changed in Settings)
- Section "Movies & Series" is labelled "Movies" in the bar, the page title is "Movies and Series"
- version.properties: 0.2.0, build 2

Known
- Written without a local Android build. The first Actions run is the first real compile. If it fails, send the red error lines of the failed step
- Channels, player, guide and addons are not built yet (step 3)
- The first install of 0.2.0 over 0.1.0 is manual, because 0.1.0 has no updater

## Step 1 - version 0.1.0 (build 1) - 2026-11-21

Project shell and build pipeline. No playback features yet.

Added
- Android project: Kotlin 1.9.24, Jetpack Compose (BOM 2024.06.00), Material 3, AGP 8.5.2, Gradle 8.7, Java 17
- One APK for Fire TV / Android TV and phones or tablets (minSdk 22, targetSdk 34)
- Manifest: LAUNCHER and LEANBACK_LAUNCHER entries, TV banner, touchscreen and leanback marked optional, cleartext HTTP allowed (many IPTV servers use http)
- Adaptive layout: TV gets a left side rail and wider margins, phone gets a bottom text bar. Detected in DeviceInfo.isTv()
- Remote control focus: FocusSurface draws a focus outline. Rail takes focus on launch. Back returns to Home, then exits
- Sections: Home, Live TV, Movies & Series, Settings. Everything not built yet is labelled "Not built yet"
- Theme taken from the logo, no emojis
- Launcher icon, round icon and TV banner made from the supplied GG logo
- Fixed release signing key (keystore/gg-release.jks) so each build installs over the last one
- version.properties holds VERSION_NAME and VERSION_CODE
- GitHub Actions workflow (reference copy in workflow/build.yml): unpacks GG-PLAYER-step*.zip, commits it, builds the APK and publishes a GitHub Release
