# GG Player - Handoff

Read this first if you are an AI picking the project up. Then read CHANGELOG.md.
Last updated: step 3 (version 0.3.0).

## What the app is

GG Player is one Android app for Fire TV (remote control) and phones or tablets (touch).
It plays two kinds of content, both from providers the user adds himself. The app ships with no content.

1. Live TV: the user adds IPTV providers (M3U playlist URL, Xtream Codes login, XMLTV guide URL). Providers are managed in Settings > IPTV providers.
2. Movies and series: the user adds addons by pasting a manifest.json link (Stremio addon protocol: manifest, catalog, meta, stream). No limit on how many addons.

App name: GG Player. Package: com.ggplayer.app. GitHub repo: GG-PLAYER (updater default is VIVEKISJOD/GG-PLAYER, editable in Settings > Updates).

## Owner rules (follow these every time)

The owner is not a programmer and only has an Android tablet and phone with a browser. No PC, no terminal.
- Reply in Hinglish (Hindi in English letters), short and simple
- His only jobs: tap, paste, upload. Never ask him to run commands, install tools or edit code
- At most ONE question per message. Claude makes the decisions
- Always deliver complete finished files, never patches or "change this line". Give exact GitHub upload steps
- EXACTLY 3 files per delivery: (1) one zip named GG-PLAYER-step<N>.zip with files at the zip root and no wrapping folder, the only thing he uploads to the GitHub repo root, (2) HANDOFF.md, (3) CHANGELOG.md. HANDOFF and CHANGELOG are also inside the zip so the repo stays current. They are given separately so another AI can continue if the token limit hits
- Keep the project to 3 steps in total (see roadmap). Do not spread work over many small steps
- Every app has in-app updates from GitHub (done in step 2) and its own settings screen with options users might need (done, extend it with every feature). Do not make silly mistakes
- Anything not built must be labelled "Not built yet". No fake features
- If he says he is tired, give one tiny step and wait for "done"
- Do not change technologies without explaining why first

## Design (decided in step 2)

The owner wants the Cinevia look (dark UI, orange accent, rounded cards, icon bottom bar, hero card, category circles, poster rows) on phone, and the same look on Fire TV so the UI stays consistent.
- Palette: near black background (0B0B0E), dark surfaces, one accent colour (default orange FF8A1F). The user can change the accent in Settings. Always use GG.* from ui/Theme.kt, never raw hex in screens. GG.accent reads settings state
- Phone: bottom bar with icons (Home, Live TV, Movies, Settings). The player is fullscreen and hides the bar and rail. TV: left rail with icons and labels, text scaled 1.25x, safe margin from settings
- Every interactive item uses FocusSurface (accent outline and slight grow on TV focus). Buttons use GGButton, inputs use GGField, settings rows use SettingRow, ToggleRow, ChoiceRow
- Icons: Material core icons plus three hand drawn ones in Components.kt (TvIcon, MovieIcon, SeriesIcon). material-icons-extended is NOT used on purpose (keeps the APK small). No emojis in the UI
- Home is a streaming style page: hero card from the first catalog, then poster rows from the addons. Posters and channel logos load with Coil (AsyncImage)

## How building works

- The repo contains .github/workflows/build.yml, pasted ONCE by the owner on github.com. A reference copy is in workflow/build.yml. It has NOT changed since step 1, so he does not need to paste it again
- Uploading GG-PLAYER-step<N>.zip to the repo root triggers the workflow. It unzips the files over the repo (skipping .github/), deletes the zip, commits, builds the release APK with Gradle 8.7 (no Gradle wrapper in the repo) and publishes a GitHub Release tagged v<VERSION_NAME> with GG-Player-<VERSION_NAME>.apk attached
- IMPORTANT: unzip only overwrites and adds files. It never deletes. So never rename or delete a source file in a later zip, because the old file stays in the repo and breaks the build with duplicate declarations. Overwrite the same file names (Screens.kt, Components.kt, GGApp.kt, Theme.kt stay)
- A change to the workflow means the owner must paste the new workflow file again (GitHub blocks the token from editing .github/)
- Release signing uses keystore/gg-release.jks (alias gg, password ggplayer, committed on purpose so updates install over older builds). Never replace it
- The AI that writes the zip has no Android SDK, so code is not compiled before delivery. The first failure usually shows in the Actions run. Ask the owner for the red error lines of the failed step and fix with a complete new zip (same step number plus a letter, for example GG-PLAYER-step2b.zip, version bump)
- Every zip must update CHANGELOG.md and HANDOFF.md and bump version.properties (VERSION_CODE +1, VERSION_NAME)

Installing: open the repo's Releases page, download the APK. On Fire TV use the Downloader app with the release APK link and allow "Install unknown apps" for Downloader. From version 0.2.0 on, updates install from inside the app (Settings > Updates). The first install of 0.2.0 over 0.1.0 is still manual because 0.1.0 has no updater.

## In-app updater (how it works)

update/UpdateManager.kt: reads https://api.github.com/repos/<owner>/<repo>/releases/latest, takes tag_name (v0.3.0 becomes 0.3.0) and the first .apk asset, compares with BuildConfig.VERSION_NAME, downloads to cache/updates, then opens the system installer through FileProvider (authority <applicationId>.fileprovider, paths in res/xml/file_paths.xml). If "install unknown apps" is not allowed it opens that settings page or shows instructions. Auto check on start is a setting. The repo (owner/name) is editable in Settings > Updates. The repo must be public for the API call to work without a token.
To publish an update: owner uploads the next zip, the workflow builds and creates the release, the app shows "Update available". The tag must be higher than the installed version, so always bump VERSION_NAME.

## Code layout

```
app/src/main/java/com/ggplayer/app/
  MainActivity.kt          entry point, init of settings, providers, addons, favorites, resume
  DeviceInfo.kt            isTv() check
  data/AppSettings.kt      all settings as Compose state in SharedPreferences. Step 3 added language, quality, playback, audio, network, parental. Backup uses rawValues and writeRaw
  data/Languages.kt        enums: Lang (with the words used to detect a language in a stream name), QualityPref, AspectChoice, BufferChoice, TimeoutChoice, SubSize, SeekStep
  data/Net.kt              small HTTP helper (manual redirects, openStream for big files)
  data/Providers.kt        Provider model, ProviderStore (providers.json), connection test
  data/Live.kt             Channel, M3uParser, XtreamApi, ChannelRepo (memory plus TSV cache), FavoriteStore, ResumeStore, Parental filter, EpgStore (XMLTV, gzip ok), Backup (export and restore)
  data/Stremio.kt          Addon models, AddonStore (addons.json), StremioApi (catalog, meta, stream)
  data/Streams.kt          StreamParser (language, quality, size detection and scoring), StreamResolver (asks all stream addons, caches 10 min), HomeRepo (poster rows and search)
  update/UpdateManager.kt  GitHub release check, download, install
  ui/Theme.kt              palette object GG, typography, LocalIsTv
  ui/Components.kt         FocusSurface, GGButton, GGField, rows, icons, pagePadding
  ui/Media.kt              Badge, StreamBadges, Chip, PosterCard, PosterRow
  ui/GGApp.kt              Section enum, Route, back stack, side rail (TV), bottom bar (phone)
  ui/Screens.kt            Home (hero, poster rows) and Movies and Series (type switch, search)
  ui/TitleScreens.kt       Title details page (pre-selects best source, episodes) and Sources list with badges
  ui/PlayerScreen.kt       PlayRequest, PlayerHost, Media3 player screen with all controls
  ui/LiveScreens.kt        Live TV channel list (groups, search, favorites, now and next) and TV guide grid
  ui/ProviderScreens.kt    IPTV providers list and add or edit form with test
  ui/AddonScreens.kt       Settings > Movie and series sources
  ui/SettingsScreens.kt    Settings main page, Appearance, Updates
  ui/MoreSettingsScreens.kt Playback, Audio and subtitles, Network, Parental, Backup
app/src/main/res/          icons, banner, gg_mark logo, theme, file_paths.xml
keystore/gg-release.jks    fixed signing key
version.properties         VERSION_NAME and VERSION_CODE
workflow/build.yml         reference copy of the workflow (unchanged)
```

Navigation: no library. Section state plus a back stack (mutableStateListOf of Route) in GGApp.kt. Back pops the stack, then returns to Home, then exits. Route.Player is fullscreen. To play something: set PlayerHost.request = PlayRequest(...) (use PlayerHost.forStream or forChannels), then open Route.Player.

## How the best source is chosen (data/Streams.kt)

Each stream gets a score. Language counts most: first language 5, dual or multi audio 4, second language 3, language unknown 2, other language 1. Then quality: exact match of the preferred quality is best, lower qualities come next (nearest first), higher ones after that, unknown quality in the middle, CAM and TS heavily punished. Then the order of the addons. Streams without a direct url (torrent infoHash only) are shown but never picked and cannot play, there is no torrent engine. Language comes from words in the stream name, title, description and file name, so it is a best guess. Settings: First language, Second language, Preferred video quality, Pick best source automatically, Start playing when a title opens.

## Roadmap (3 steps total, all done)

| Step | Content | Status |
|------|---------|--------|
| 1 | Project shell, TV and phone layout, theme, icon, build pipeline | Done, APK built (0.1.0) |
| 2 | Cinevia style UI, settings, in-app updater, providers | Done, build green (0.2.0) |
| 3 | M3U and Xtream channels, player, XMLTV guide, Stremio addons with posters, best source badges, player features, real settings | Done (0.3.0), build not yet confirmed |

Known limits of step 3 (honest list, not fake features):
- Written without a local Android build. If the Actions run fails, send the red error lines and fix as step 3b
- No torrent playback. Torrent-only streams show a "Torrent, cannot play" badge. A debrid addon that returns direct links works
- Language detection is a guess from text. Xtream VOD and series are not loaded, only live channels
- External subtitles are added by link inside the player. Subtitles from addons are not used
- Parental control hides adult channel groups by name and protects the option with a PIN. It does not lock single channels
- Backup is copy and paste text, no file picker

## Next step to do

Owner confirms the step 3 build is green and tests on phone and Fire TV. Then fix what he reports as step 3b (same zip rules, version bump) or small follow ups.
