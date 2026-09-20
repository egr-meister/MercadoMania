# Mercado Mania

A fully offline Android game built with Kotlin and Jetpack Compose. Two modes —
a themed **Quiz** and a **Matching Pairs** board — wrapped in a golden-bazaar
visual concept, with every byte of progress kept on the device.

- `applicationId` / `namespace`: `com.mercadomania.game`
- `minSdk` 24 · `targetSdk` 36 · `compileSdk` 36 · JDK 17
- No internet permission. No permissions at all.

---

## Contents

1. [Features](#features)
2. [Offline only](#offline-only)
3. [Local storage and DataStore](#local-storage-and-datastore)
4. [Privacy](#privacy)
5. [What is deliberately missing](#what-is-deliberately-missing)
6. [Visual concept and the unique home layout](#visual-concept-and-the-unique-home-layout)
7. [App icon](#app-icon)
8. [Splash](#splash)
9. [Accessibility](#accessibility)
10. [Assets](#assets)
11. [Audio](#audio)
12. [Tech stack](#tech-stack)
13. [Architecture](#architecture)
14. [Open it in Android Studio](#open-it-in-android-studio)
15. [16 KB memory page size](#16-kb-memory-page-size)
16. [Debug build](#debug-build)
17. [Generating a PKCS12 keystore](#generating-a-pkcs12-keystore)
18. [Local release signing](#local-release-signing)
19. [GitHub Secrets](#github-secrets)
20. [GitHub Actions](#github-actions)
21. [apksigner verification](#apksigner-verification)
22. [APK vs AAB](#apk-vs-aab)
23. [Staged R8](#staged-r8)
24. [Local release verification](#local-release-verification)
25. [Unit tests](#unit-tests)
26. [Resetting data](#resetting-data)
27. [Known limitations](#known-limitations)

---

## Features

**Quiz**
- Three categories (`treasures`, `trade`, `legends`), ten single-answer
  questions each.
- One question at a time, with a gold progress strip and an `n/10` counter.
- Tapping an answer reveals the state for ~0.75 s — the correct option turns
  green with a check mark, a wrong pick turns red with a cross — then the quiz
  advances on its own.
- Best score per category is saved.
- Pause overlay: Continue / Restart / Menu.
- Results panel: `Score: X/10`, Continue / Restart / Menu.

**Matching Pairs** (never called "Puzzles")
- Nine levels of increasing difficulty — pairs / columns / seconds:

  | Level | Pairs | Columns | Seconds |
  |------:|------:|--------:|--------:|
  | 1 | 2 | 2 | 60 |
  | 2 | 3 | 2 | 70 |
  | 3 | 4 | 2 | 70 |
  | 4 | 6 | 3 | 80 |
  | 5 | 6 | 3 | 80 |
  | 6 | 8 | 4 | 100 |
  | 7 | 8 | 4 | 100 |
  | 8 | 10 | 4 | 110 |
  | 9 | 12 | 4 | 120 |

- The deck is built from the twelve themed icons, duplicated and shuffled.
- Flip two cards; a match stays open, a mismatch flips back after a short delay.
- Per-level countdown that stops while the game is paused.
- Clear every pair before time runs out → **Well Done**; run out → **Out of Time**.
- Best matched-pair count saved per level; clearing a level unlocks the next
  one (level 1 is always open).
- Pause overlay and results panel with Continue / Restart / Menu.

**Screens**

Main · Menu · Quiz category menu · Quiz · Quiz results · Levels (nine bubbles
plus Game Rules) · Pairs game · Pairs results · Results summary · Settings ·
Game Rules · a friendly fallback for anything that cannot render.

Navigation is Navigation Compose with type-safe routes. Quiz results and Pairs
results are modal panels drawn over their own game destination rather than
separate back-stack entries, so a rotation, a process death or a stray Back
press can never strand the player on an orphaned results screen.

---

## Offline only

The app never touches the network:

- `AndroidManifest.xml` declares **no** `<uses-permission>` element — no
  `INTERNET`, no storage, no runtime permissions.
- No services, `WorkManager`, `JobScheduler`, alarms, receivers, providers or
  notifications.
- No networking library is on the classpath (see
  [What is deliberately missing](#what-is-deliberately-missing)).
- The only outbound action possible is an explicit `ACTION_VIEW` intent the
  player triggers in Settings (privacy page, store listing), handed to another
  app. If nothing can handle it, the app says so and carries on.

---

## Local storage and DataStore

All state is a single serialized JSON string in **DataStore Preferences**
(`mercado_mania_store`, key `game_data_json`).

```kotlin
@Serializable
data class GameData(
    val quizBest: Map<String, Int> = emptyMap(),
    val pairsBest: Map<String, Int> = emptyMap(),
    val settings: Settings = Settings()
)

@Serializable
data class Settings(val soundEnabled: Boolean = true)
```

Every field has a default, so a payload written by an older or a newer build
still loads. Encoding and decoding live in the context-free `GameSerializer`
(`ignoreUnknownKeys = true`), which is why they can be unit tested on the JVM.

**Reads never crash.** Empty storage, a missing key, an empty string, truncated
or corrupted JSON, a structurally wrong document, or a value of the wrong type
all resolve to `GameData()` defaults. Negative values that a hand-edited file
could smuggle in are clamped to zero. Writes are equally defensive: if storage
fails, the round simply is not recorded.

`GameRepository` exposes `data: Flow<GameData>` and four operations: save a
quiz best, save a pairs best, toggle sound, reset everything.

---

## Privacy

- Nothing is collected, uploaded, shared or sold.
- The only data that exists is best scores and the sound toggle, in the app's
  own private storage.
- `android:allowBackup="false"` — not even a cloud backup copy is made.
- Settings shows the full privacy text from app resources, so it is readable
  with no connection at all. A web copy is one extra tap away via an external
  intent; replace `PRIVACY_URL` in `SettingsScreen.kt` with your real URL
  before publishing.
- Reset in Settings removes everything the app has stored.

---

## What is deliberately missing

None of these are present, by design:

React Native / Expo / Flutter · Java for logic · `WebView` · Retrofit / OkHttp /
Ktor · Firebase · Room · any DI framework (Hilt, Koin, Dagger) · any networking,
ads, analytics, payment or maps SDK · any attribution or tracking SDK
(AppsFlyer, OneSignal, Adjust, …) · native libraries.

---

## Visual concept and the unique home layout

**Concept: "Golden Bazaar."** A night market of gilded relics — warm polished
gold against deep lapis blue, lit by gem-cyan highlights.

### Palette

| Hex | Name | Usage |
|---|---|---|
| `#FFC800` | Bazaar Gold | primary; button plates, level bubbles, emphasis |
| `#FFE44D` | Light Gold | top of gold gradients, focus, sparkle |
| `#B87400` | Deep Gold | bottom of gold gradients, plate shadow line |
| `#02133F` | Midnight Lapis | window and scrim base, darkest panel stop |
| `#0A2A8C` | Royal Blue | panel fill, card backs, secondary containers |
| `#1546D6` | Azure | panel gradient top, selected states |
| `#33B5FF` | Gem Cyan | progress, timers, tertiary accents |
| `#FFF7DC` | Parchment Cream | body text on dark |
| `#FFFFFF` | White | every button label (bold, on the plate) |
| `#1FA34A` | Emerald | correct answer — always with a check mark |
| `#E0342C` | Ruby | wrong answer — always with a cross |
| `#7B8BB8` | Muted Slate | disabled / locked, secondary meta text |

### Typography

Material 3 type scale on the platform default family — no bundled font files.
The system font already carries the weights the design needs, it honours the
user's font-size setting, and it keeps the APK small. Sizes are in `sp`
throughout.

### The home screen is not a dashboard

Reading down `MainScreen.kt`: a swaying bunting of gold diamonds strung across
the top (drawn in Compose), the wordmark hanging from it on a cord with a gentle
float, sparkle dust over the empty market floor, and the three mascots staged
along the bottom edge — gecko low left, automaton low right, genie rising large
from the centre in front of both. The single Start plate floats *inside* that
composition rather than under it.

There is no stats card, no centred mascot-over-title stack and no column of
buttons: the deliberate opposite of the generic
mascot → title → stats-card → button-stack layout.

### Buttons and panels

- Buttons are an **image plate with the label drawn on top in code**, in white
  bold text. `btn_plate.png` is the everyday plate; `btn_menu.png` is the
  ornate framed plate used for the main menu. Neither is ever stretched: the
  plate keeps its own aspect ratio and the caller controls the width
  (main-menu plates are constrained to ~0.7 of the screen, and the menu scrolls).
- Because white on gold is a weak contrast pair on its own, labels are drawn
  twice — a dark stroked outline pass, then the white fill — so the white bold
  label stays legible without abandoning the design rule.
- **Modal panels (pause, results, rules, privacy, reset confirmation) are drawn
  in Compose**: rounded rectangle, vertical gradient fill, gold accent border
  and a thin inner cyan rule. No fixed-shape frame image is stretched to fit
  variable content.
- Quiz answer options are also Compose-drawn. Four fixed-aspect plates cannot
  fit on one portrait screen, and an option must grow with its text and with
  the user's font scale. Every real action button still uses a plate.

---

## App icon

Concept: the Mercado Mania crest — a crowned dome with the block **M**, flanked
by sparkles — in gold on a royal-blue sunburst.

- Adaptive icon (`mipmap-anydpi-v26/ic_launcher.xml` + `ic_launcher_round.xml`)
  with separate background and foreground layers; the crest sits inside the
  66 % safe zone so no launcher mask clips it.
- A `<monochrome>` layer (`drawable/ic_launcher_monochrome.xml`, a hand-written
  vector) for Android 13+ themed icons.
- Legacy density PNGs (`mipmap-mdpi` … `mipmap-xxxhdpi`, square and round) for
  API 24–25.
- The default Android icon is nowhere in the project.

## Splash

Themed background colour plus the small crest — no heavy asset.

- API 24–30: `Theme.MercadoMania.Splash` sets `windowBackground` to
  `drawable/splash_background.xml`, a layer-list of the theme colour
  `#02133F` with a 200 dp centred crest.
- API 31+: `values-v31/themes.xml` uses the platform
  `windowSplashScreenBackground` / `windowSplashScreenAnimatedIcon`
  attributes instead.
- `MainActivity` calls `setTheme(R.style.Theme_MercadoMania)` before
  `super.onCreate()`, so the splash does not linger behind the UI.

---

## Accessibility

- **Contrast**: all text sits on the lapis scrim or on a Compose-drawn panel,
  not directly on the bright artwork. White plate labels carry a dark outline.
- **Touch targets**: every interactive element is at least 48 dp; plates and
  round icon buttons enforce a 48 dp minimum.
- **Content descriptions**: back and pause buttons, mascots, the logo, every
  card ("face down card", "card showing golden key", "matched card showing …")
  and every level bubble ("Level 4, locked. Clear level 3 first.").
  Decorative artwork (background, plate images behind labels) is explicitly
  excluded from the semantics tree.
- **Colour is never the only signal**: correct/wrong answers carry a check or
  cross glyph *and* a spoken state description; locked levels carry a padlock
  *and* a description; cleared levels carry a check badge; the sound toggle
  spells out "On" / "Off" next to the switch.
- **Text alternatives**: progress is announced as "Question 3 of 10" alongside
  the bar; timers and counters are text, not just colour.
- **Font scaling**: sizes are in `sp` and every text-bearing screen is
  vertically scrollable, so large accessibility font scales do not clip.
- **Portrait only** with correct edge-to-edge insets — each screen applies
  `safeDrawingPadding()`, so nothing hides under the status or navigation bar.

---

## Assets

Every drawable is referenced through the single `Assets` object
(`ui/Assets.kt`). Files live in `res/drawable-nodpi/` under fixed names, so
final art can be dropped over the current files with **no code change**:

| File | Role |
|---|---|
| `bg_main.jpg` | full-bleed background (opaque, so JPG) |
| `logo.png` | wordmark badge |
| `mascot_a.png` | Zafir the lamp genie (host) |
| `mascot_b.png` | Pico the golden gecko (runner) |
| `mascot_c.png` | Tino the automaton (trader) |
| `btn_menu.png` | ornate framed plate, transparent PNG |
| `btn_plate.png` | simple plate |
| `plate_round.png` | round plate for level bubbles |
| `icon_back.png` | round back button |
| `icon_pause.png` | round pause button |
| `item_01.png` … `item_12.png` | the twelve match icons |
| `splash_logo.png` | small crest for the splash and card backs |

If replacement art has different proportions, change `BTN_MENU_ASPECT`,
`BTN_PLATE_ASPECT` and `LOGO_ASPECT` in `Assets.kt` and every button and bubble
re-proportions itself. `Assets.itemNames` holds the spoken name of each match
icon — update it alongside the art so card descriptions stay truthful.

All PNGs are colour-quantised and optimised; the background ships as a
progressive JPEG. Total drawable payload is well under 1 MB.

---

## Audio

Six short effects played through `SoundPool` in `SoundManager`, created once in
the `Application`:

| Sound | Trigger |
|---|---|
| `click.wav` | buttons, back, pause, card flip |
| `correct.wav` | correct quiz answer |
| `wrong.wav` | wrong quiz answer |
| `match.wav` | matched pair |
| `win.wav` | level cleared / good quiz result |
| `lose.wav` | out of time / poor quiz result |

The shipped files are simple synthesized tones in `res/raw/`, generated as
22 050 Hz 16-bit mono WAV. Drop real audio over them using the same file names.
Loading is asynchronous, so a sample that has not finished loading is skipped
rather than crashing.

The sound toggle persists in DataStore and is synced to `SoundManager` at
startup (from `MercadoApp`) and live whenever it changes.

---

## Tech stack

| | |
|---|---|
| Language | Kotlin 2.0.21 |
| UI | Jetpack Compose, Material 3, Compose BOM 2024.12.01 |
| Navigation | Navigation Compose 2.8.5 (type-safe routes) |
| State | Android ViewModel, Coroutines 1.9.0, Flow / StateFlow |
| Storage | DataStore Preferences 1.1.1 |
| Serialization | Kotlinx Serialization 1.7.3 |
| Build | AGP 8.7.3, Gradle 8.9, Gradle Kotlin DSL + version catalog |
| JDK | 17 |

Every package the app imports is a **direct** dependency in
`gradle/libs.versions.toml` — including `androidx.compose.foundation` and
`androidx.compose.animation`, which are declared explicitly rather than
inherited transitively.

`android.suppressUnsupportedCompileSdk=36` is set in `gradle.properties`
because AGP 8.7.x does not yet officially declare support for API 36.

---

## Architecture

Simple MVVM, no framework:

```
MercadoApp (Application)
 ├── GameRepository      created once, DataStore-backed
 └── SoundManager        created once, SoundPool-backed
        │
        ▼  passed explicitly
 ViewModelProvider.Factory  (GameDataViewModelFactory, QuizViewModelFactory,
        │                    PairsViewModelFactory — plain classes)
        ▼
 ViewModel  ──  immutable UI state via StateFlow
        │
        ▼  collectAsStateWithLifecycle()
 @Composable screens  ──  stateless, take state + callbacks
```

- `GameLogic` is a pure, context-free object: the level table, deck building,
  unlock and completion rules, and the per-round seed. It touches no Android
  class, so it is directly unit-testable.
- `GameSerializer` is likewise context-free.
- UI state classes (`QuizUiState`, `PairsUiState`, `GameDataUiState`) are
  immutable `data class`es. Screens never mutate them.
- No DI framework anywhere.

---

## Open it in Android Studio

1. Android Studio Ladybug or newer.
2. **File → Open** → the project root.
3. Make sure the Gradle JDK is **17**
   (*Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK*).
4. In the SDK Manager install **Android SDK Platform 36** and
   **Android SDK Build-Tools 36.0.0**.
5. Sync. The Gradle wrapper is pinned to **8.9**; if your checkout is missing
   `gradle/wrapper/gradle-wrapper.jar`, Android Studio regenerates it on sync,
   or run `gradle wrapper --gradle-version 8.9` once with a local Gradle.

---

## 16 KB memory page size

The app is pure Kotlin and Compose — there is no NDK, no `jniLibs`, no `.so`
of our own — so there is nothing to re-align for Android's 16 KB page size and
the build is compatible by construction.

Still verify the artifact you actually ship:

```bash
# List native libraries inside the bundle - expected: none from this project
unzip -l app/build/outputs/bundle/release/app-release.aab | grep -E '\.so$' || echo "no native libs"
```

If a future dependency drags in native code, check its alignment with
`zipalign -c -P 16 -v 4 <apk>` and require AGP 8.5.1+ packaging.

---

## Debug build

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The debug variant uses `applicationId` `com.mercadomania.game.debug`, so it can
sit beside a release install.

---

## Generating a PKCS12 keystore

Never use a debug key for anything you distribute.

```bash
keytool -genkeypair \
  -v \
  -storetype PKCS12 \
  -keystore mercado-mania-release.p12 \
  -alias mercado-upload \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000 \
  -dname "CN=Mercado Mania, OU=Games, O=Your Company, L=City, ST=State, C=US"
```

Back the file up somewhere safe and offline. Losing it means losing the ability
to update the app on Google Play (unless Play App Signing holds your app key,
in which case this is your *upload* key and it can be reset by support).

For CI, produce the base64 blob:

```bash
base64 -i mercado-mania-release.p12 | tr -d '\n' > keystore.b64   # macOS
base64 -w0 mercado-mania-release.p12 > keystore.b64               # Linux
```

---

## Local release signing

`app/build.gradle.kts` reads signing material from four environment variables
and nothing else:

| Variable | Meaning |
|---|---|
| `ANDROID_KEYSTORE_PATH` | absolute path to the `.p12` |
| `ANDROID_KEYSTORE_PASSWORD` | keystore password |
| `ANDROID_KEY_ALIAS` | key alias |
| `ANDROID_KEY_PASSWORD` | key password |

```bash
export ANDROID_KEYSTORE_PATH="$HOME/keys/mercado-mania-release.p12"
export ANDROID_KEYSTORE_PASSWORD='…'
export ANDROID_KEY_ALIAS='mercado-upload'
export ANDROID_KEY_PASSWORD='…'

./gradlew assembleRelease bundleRelease
```

If any of the four is missing, the build **fails with an explicit message**
instead of falling back to the debug key:

```
Mercado Mania: refusing to build a release artifact without release signing.
```

There is no `keystore.properties` fallback and no debug-signing fallback,
anywhere.

---

## GitHub Secrets

Add these under *Settings → Secrets and variables → Actions*:

| Secret | Value |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | contents of `keystore.b64` |
| `ANDROID_KEYSTORE_PASSWORD` | keystore password |
| `ANDROID_KEY_ALIAS` | key alias |
| `ANDROID_KEY_PASSWORD` | key password |

The workflow never echoes them: the keystore is decoded straight to a file
under `$RUNNER_TEMP` (and deleted afterwards), and passwords only travel as
environment variables.

---

## GitHub Actions

`.github/workflows/android-build.yml` runs on push to `main` and on
`workflow_dispatch`. It:

1. checks out the repo and sets up **JDK 17**;
2. installs the Android SDK plus **Platform 36** and **Build-Tools 36.0.0**;
3. provisions **Gradle 8.9** through `gradle/actions/setup-gradle`, so a
   committed `gradle-wrapper.jar` is not required;
4. fails early if any signing secret is missing;
5. decodes `ANDROID_KEYSTORE_BASE64` into `$RUNNER_TEMP/signing/release.p12`;
6. runs the unit tests;
7. builds signed `assembleRelease` **and** `bundleRelease`;
8. runs `apksigner verify --print-certs` and fails on a verification error or
   on `CN=Android Debug`;
9. deletes the decoded keystore;
10. uploads the APK (testing artifact) and the AAB (Google Play artifact).

There is **no mandatory emulator smoke test**.

> A green CI build is **not** proof that the app launches. It proves the project
> compiles, is signed with a non-debug certificate, and produced both artifacts.
> Always verify on a real device — see below.

---

## apksigner verification

```bash
"$ANDROID_SDK_ROOT/build-tools/36.0.0/apksigner" verify --print-certs \
  app/build/outputs/apk/release/app-release.apk
```

Expect `Verifies` plus your own certificate DN. If the output contains
`CN=Android Debug`, the artifact is debug-signed and must not be distributed —
CI fails on exactly this string.

---

## APK vs AAB

- **`.aab`** (`app/build/outputs/bundle/release/`) is the *only* artifact that
  goes to Google Play.
- **`.apk`** (`app/build/outputs/apk/release/`) is for your own device testing
  and sideloading. Do not upload it to Play.

---

## Staged R8

The project intentionally ships with:

```kotlin
isMinifyEnabled = false
isShrinkResources = false
```

Enable R8 **in a second step**, only after a non-minified release has been
built, installed and verified on a device:

1. Ship / verify the non-minified release (checklist below).
2. In `app/build.gradle.kts`, set both flags to `true` in `buildTypes.release`.
3. Rebuild, reinstall, and run the **entire** checklist again — with special
   attention to saved scores surviving an app restart, since Kotlinx
   Serialization resolves serializers reflectively.
4. `app/proguard-rules.pro` already keeps the Kotlinx-Serialization
   serializers, the `@Serializable` models and the navigation routes, so this
   step should be uneventful. Keep those rules.

---

## Local release verification

CI cannot tell you the app starts. Do this by hand:

```bash
./gradlew assembleRelease
adb install -r app/build/outputs/apk/release/app-release.apk
adb logcat -c && adb logcat *:E AndroidRuntime:E
```

Checklist:

- [ ] Launcher icon is the crest, not the default Android robot.
- [ ] Splash shows the themed background and crest, then the main screen.
- [ ] Main screen: bunting, logo, three mascots, Start — nothing under the
      status or navigation bar.
- [ ] Menu → each of the five entries opens its screen; Back returns.
- [ ] Quiz: progress strip and `n/10` advance; correct is green + check, wrong
      is red + cross; auto-advance ~0.75 s; results show `Score: X/10`.
- [ ] Quiz pause: Continue resumes, Restart restarts, Menu leaves.
- [ ] Levels: only level 1 is open on a fresh install; clearing it unlocks 2.
- [ ] Pairs: timer counts down, stops while paused; matches stay open,
      mismatches flip back; clearing all pairs → "Well Done"; timeout →
      "Out of Time".
- [ ] Results summary lists quiz bests and `cleared / 9`.
- [ ] Settings: sound toggle takes effect immediately and survives a restart;
      privacy panel opens offline; reset asks for confirmation then wipes.
- [ ] Kill and relaunch the app: best scores and unlocks are still there.
- [ ] Reset progress, then reopen Levels and a previously-unlocked level — you
      get the friendly "still locked" panel, not a crash.
- [ ] Rotate / change font size to the largest setting — nothing clips, nothing
      crashes.
- [ ] `adb logcat` shows no `AndroidRuntime` exceptions throughout.

Then repeat all of it with R8 enabled.

---

## Unit tests

```bash
./gradlew testDebugUnitTest
```

Covered:

- **Deck building** — card count, exactly two of each icon, indices in range,
  clamping when asked for more pairs than icons, determinism per seed, and a
  different board per round.
- **Level table** — the nine difficulty rows, and `null` outside 1..9.
- **Unlocking and completion** — level 1 always open, clearing unlocks exactly
  the next level, a partial best does not complete a level, junk keys ignored.
- **Completed-level counting** — empty, partial, junk, and all-cleared.
- **Safe JSON handling** — null, empty, blank, truncated, corrupted,
  wrong-typed, unknown-keys, missing-fields, negative-value clamping, and a
  large round trip.
- **Quiz content shape** — three stable category ids, ten valid questions each,
  four distinct options, `correctIndex` in range, emblems pointing at real
  icons, safe lookup for unknown ids.

---

## Resetting data

*Settings → Reset progress* → confirmation panel → **Reset**. This removes the
single DataStore entry, so best scores, unlocked levels and the sound
preference all return to defaults. There is no hidden second copy, and
`allowBackup="false"` means no cloud copy either.

---

## Known limitations

- Quiz questions and category blurbs are **themed placeholder content**, marked
  as such at the top of `QuizContent.kt`. Replace the `QuizQuestion` lists;
  keep the category ids so existing best scores still line up.
- Artwork is placeholder-grade for layout purposes; swap the files in
  `res/drawable-nodpi/` under the same names.
- Sound effects are synthesized tones, not designed audio.
- `PRIVACY_URL` in `SettingsScreen.kt` points at an example domain.
- English only (`resourceConfigurations += "en"`). Add locales by adding
  `values-xx/strings.xml` and widening that list.
- A game session is not persisted across process death: progress *records* are
  saved, but an in-flight quiz or board restarts. This is a deliberate
  trade-off — it keeps state handling simple and cannot strand the player in a
  half-restored board.
- Portrait only, by design.
- No leaderboards, achievements, cloud save or sharing — all of those would
  require the network, which the app does not have.
