# GG Player - Handoff

Read this first if you are an AI picking the project up. Then read CHANGELOG.md.
Last updated: step 1 (version 0.1.0).

## What the app is

GG Player is one Android app for Fire TV (remote control) and phones or tablets (touch).
It plays two kinds of content, both from providers the user adds himself. The app ships with no content.

1. Live TV: the user adds IPTV providers (M3U playlist URL, Xtream Codes login, XMLTV guide URL).
2. Movies and series: the user adds addons by pasting a manifest.json link (Stremio addon protocol: manifest, catalog, meta, stream). No limit on how many addons.

Requirements from the owner
- TV friendly: everything reachable and visible with a D-pad remote, large text, clear focus outline, safe margins
- Same app works on phones and tablets
- Built-in player with the settings a proper player has (tracks, subtitles, speed, aspect ratio, buffer, resume)
- Built-in updater (checks GitHub Releases, downloads, installs)
- Looks designed, not generic. No emojis anywhere in the UI. Palette comes from the logo (slate, gold, blue)
- Anything not built must be labelled "Not built yet". No fake features

App name: GG Player. Package: com.ggplayer.app. Repo: GG-PLAYER on GitHub (owner's account).

## Who the owner is and how to work with him

The owner is not a programmer and only has an Android tablet and phone with a browser. No PC, no terminal.
- His only jobs: tap, paste, upload. Never ask him to run commands, install tools or edit code
- Guide ONE step at a time. Few, bigger steps. Say exactly where to tap. At most one question per message
- Explain errors in simple words
- Deliver complete finished files, never "change a few lines"
- Deliver each step as ONE zip named GG-PLAYER-step<N>.zip with the files at the zip root (no wrapping folder). He uploads it to the repo root on github.com
- Every zip must update CHANGELOG.md and this HANDOFF.md, and bump version.properties (VERSION_CODE +1, VERSION_NAME)
- If he says he is tired, give one tiny step and wait for "done"
- Do not change technologies without explaining why first

## How building works

- The repo contains .github/workflows/build.yml, pasted ONCE by the owner on github.com. A reference copy is in workflow/build.yml
- Uploading GG-PLAYER-step<N>.zip to the repo root triggers the workflow. It unzips the files over the repo (skipping .github/), deletes the zip, commits, builds the release APK with Gradle 8.7 (no Gradle wrapper in the repo, the workflow installs Gradle) and publishes a GitHub Release tagged v<VERSION_NAME> with GG-Player-<VERSION_NAME>.apk attached
- Because the workflow cannot overwrite files in .github/ (GitHub blocks that for the default token), a change to the workflow means the owner must paste the new workflow file again
- Release signing uses keystore/gg-release.jks (alias gg, password ggplayer, committed on purpose so updates install over older builds). Never replace it, or every installed copy would have to be uninstalled first
- The AI that writes the zip had no Android SDK, so code is not compiled before delivery. The first failure usually shows in the Actions run. Ask the owner for the red error lines of the failed step

Installing: open the repo's Releases page, download the APK. On Fire TV use the Downloader app with the release APK link, and allow "Install unknown apps" for Downloader. On a phone, open the APK from the browser download.

## Tech stack (decided, with reason)

- Kotlin + Jetpack Compose + Material 3: same stack as the owner's other apps, one code base for TV and phone
- No navigation library yet. Section state lives in GGApp.kt. When the player screen arrives (step 3), add a small back stack or navigation-compose and say so in the changelog
- Player (planned): AndroidX Media3 ExoPlayer, because it handles HLS, DASH, progressive MP4 and MPEG-TS, which covers IPTV and addon streams
- Storage (planned): JSON files or DataStore for providers, addons and settings, to keep the build simple. Room only if lists get large (channel lists can be tens of thousands of rows, decide in step 3)
- Networking (planned): OkHttp
- JSON (planned): kotlinx.serialization or org.json

## Code layout

```
app/src/main/java/com/ggplayer/app/
  MainActivity.kt        entry point, picks TV or phone mode
  DeviceInfo.kt          isTv() check (UiModeManager, Fire TV and leanback features)
  ui/Theme.kt            colours (object GG), typography, LocalIsTv
  ui/Components.kt       FocusSurface, StatusTag, ScreenHeader
  ui/GGApp.kt            sections, SideRail (TV), BottomBar (phone), back handling
  ui/Screens.kt          Home, Live TV, Movies and Series, Settings screens
app/src/main/res/        icons, banner, gg_mark logo, theme
keystore/gg-release.jks  fixed signing key
version.properties       VERSION_NAME and VERSION_CODE, read by app/build.gradle.kts
workflow/build.yml       reference copy of the GitHub Actions workflow
```

UI rules
- Every interactive item uses FocusSurface so TV focus looks the same everywhere
- Use GG.* colours, never raw hex in screens
- TV margins 48dp horizontal, text scaled 1.25x (see Theme.kt)
- Do not add icons packs or gradients for decoration. Text and thin lines only

## Roadmap

| Step | Content | Status |
|------|---------|--------|
| 1 | Project shell, TV and phone layout, theme, icon, build pipeline | Done (not yet confirmed building) |
| 2 | Provider storage and "Add IPTV provider" screens (M3U URL, Xtream login), provider list | Not started |
| 3 | M3U parser, channel list with groups and search, Media3 player with basic controls, back stack | Not started |
| 4 | XMLTV guide: now and next, full guide grid | Not started |
| 5 | Addon manager: paste manifest.json link, validate, list, remove | Not started |
| 6 | Addon browsing: catalogs, search, meta page, stream picker, play | Not started |
| 7 | Player features: audio and subtitle tracks, external subtitles, speed, aspect ratio, resume position | Not started |
| 8 | Full settings: playback, subtitles, network and buffer, user agent | Not started |
| 9 | In-app updater: read latest GitHub Release, compare VERSION_CODE, download, install. Needs REQUEST_INSTALL_PACKAGES and a FileProvider. Confirm the exact GitHub owner/repo name with the owner first | Not started |
| 10 | Favorites, parental PIN, backup and restore, polish | Not started |

Order can change if the owner asks. Always tell him why.

## Next step to do

Step 2, after the owner confirms that the step 1 build is green and the app opens on his devices. If the build failed, fix that first and ship it as a step 1 fix (same step number plus a letter, for example GG-PLAYER-step1b.zip, version 0.1.1).
