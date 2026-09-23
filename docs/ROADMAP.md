# Roadmap to 3.0.0

The goal is **CatsList 3.0.0: one Compose Multiplatform app on Android, desktop and iOS**,
with every screen, the navigation and the theme in `commonMain`, and `:app`,
`:desktopApp` and `iosApp/` reduced to thin launchers.

Tick a step off in the PR that finishes it. When a step turns out different from what is
written here, change it here — this file is the plan, not a record of the first guess.

## Ground rules

- **Nothing reaches `main` before 3.0.0.** Every PR targets `dev`. Swapping libraries and
  moving the core modules to KMP is groundwork; users see no change, so there is nothing
  to release until the app runs on the new platforms.
- **Stacked PRs are retargeted to `dev` by hand** after the one below merges (see
  `CLAUDE.md` / `CONTRIBUTING.md`).
- **iOS is built and run on the Mac** (M3, Xcode, iPhone 11 on a free Apple ID).
  Kotlin/Native only compiles for iOS on macOS, so on Windows the iOS targets are skipped
  and everything else builds as usual. CI's macOS job is what catches an iOS break
  between Mac sessions.
- **iOS ships as "build from source".** Without a paid Apple Developer account there is no
  TestFlight, App Store or installable file; the README says so. Android (APK) and desktop
  (MSI/DMG/DEB) are downloadable from the GitHub release.

## State at the start

- Multiplatform (Android + JVM): `:core:model`, `:core:domain`, `:core:data`.
- Android-only: `:core:ui`, `:core:designsystem`, `:core:testing`, `:feature:favorites`,
  `:feature:feed`, `:app` — about 3,300 lines, 37 strings, 7 drawables plus the launcher
  icon.
- Android APIs to replace: `Context` in `UiText`, `Log` in `LaunchCatching`, `SystemClock`
  in `HeldAtLeast` and `CatPullToRefresh`, the `Activity`/`Build` status-bar code in
  `Theme`. The rest is `R.string` / `R.drawable`.
- Every UI library already publishes common, JVM and native variants: Navigation 3,
  Paging Compose, Lifecycle ViewModel, Koin, Coil 3 ([DECISIONS.md](DECISIONS.md), step 4
  of the KMP migration).
- `DesktopNetworkMonitor` always reports online.

## Steps

### Groundwork

- [ ] **1. [TECH] Pull in the Android repo's README commits** (#91 onward). Independent.
- [ ] **2. Run `dev` on an Android device** (no PR). The only proof that Koin, the network
  monitor and the shared HTTP client work outside tests.
- [ ] **3. [TECH] Compose Multiplatform build setup.** CMP version in the catalog, a
  `catslist.kmp.compose` convention plugin, Compose resources.
- [ ] **4. [TECH] iOS targets for `:core:model`, `:core:domain`, `:core:data`.**
  `iosArm64` + `iosSimulatorArm64`; Ktor Darwin engine on iOS (OkHttp is JVM-only);
  Room with the bundled SQLite; iOS actuals for the network monitor (`NWPathMonitor`) and
  the image downloader (Photos, with its permission prompt); a macOS CI job that compiles
  and runs the tests for iOS; an ADR superseding "Targets: `jvm()` only".

### UI to KMP — stacked, each on the one before

- [ ] **5. [TECH] `:core:ui`.** Strings to Compose resources, `UiText` onto
  `StringResource`; `SystemClock` → `TimeSource.Monotonic`; `Log` behind
  `expect`/`actual` like `DataLog`.
- [ ] **6. [TECH] `:core:testing`.** Needed by every feature's tests. `MainDispatcherRule`
  is JUnit 4, so it goes in a source set shared by the JVM and Android targets only.
- [ ] **7. [TECH] `:core:designsystem`.** Drawables to Compose resources; `CatPullToRefresh`
  onto `TimeSource`; `Theme`'s dynamic colour and status bar become `expect`/`actual`
  (static palette off Android). `CatItemTest` moves to `runComposeUiTest`, so UI tests
  run on desktop in `./gradlew verify` without a device.
- [ ] **8. [TECH] `:feature:favorites`.** No paging, so the simpler one first.
  `koin-androidx-compose` → `koin-compose-viewmodel`.
- [ ] **9. [TECH] `:feature:feed`.** Paging Compose and `itemKey` work in common code.
- [ ] **10. [TECH] `:shared` — the root UI.** `NavDisplay`, the back stack and a
  `CatsApp()` composable move out of `:app`, which keeps only `MainActivity` + `App`.
  Also builds the iOS framework. Off Android, `rememberNavBackStack` needs the `NavKey`
  types registered for polymorphic serialization, or saving the back stack fails at
  runtime.

### Apps

- [ ] **11. [FEATURE] `:desktopApp`.** `main()` with a window, Koin startup, a Coil loader
  on the shared OkHttp client (ADR-0030), the Room database in the user's app-data
  folder, a title and an icon. Labels: `FEATURE` + `UX/UI`.
- [ ] **12. [FEATURE] `iosApp/`.** The Xcode project hosting `CatsApp()`, Koin startup,
  Coil on Ktor, app icon. Run on the simulator and on the iPhone 11. Labels: `FEATURE` +
  `UX/UI`.
- [ ] **13. [FEATURE] A real desktop network monitor.** Offline behaviour is a feature on
  Android; desktop does not ship without it.

### Release

- [ ] **14. [TECH] Desktop installers in CI.** `nativeDistributions` for MSI, DMG and DEB,
  version read from the one in `app/build.gradle.kts`; a tag-triggered workflow builds
  them on Windows, macOS and Linux runners.
- [ ] **15. [RELEASE] v3.0.0.** Bump the version; README (platforms, desktop and iOS
  screenshots, "iOS: build from source"); ADRs. Run by hand on an Android device, on
  Windows desktop, on the iOS simulator and on the iPhone 11. Then the `dev` → `main`
  PR, the `v3.0.0` tag, and a GitHub release with the APK and the three installers.

## After 3.0.0

- `:core:testing`'s JUnit 4 pieces as common test utilities, if the tests move to
  `commonTest`.
- iOS distribution (TestFlight) if a paid Apple Developer account ever makes sense.
