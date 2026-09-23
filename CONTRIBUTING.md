# Contributing

How to build, verify and change this codebase without breaking it.

- **What the architecture is** — [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)
- **Why it is that way** — [`docs/DECISIONS.md`](docs/DECISIONS.md)

---

## The verification gate

A change is not done until the verification build passes and no test was lost.
Two gates, both real tasks in the root `build.gradle.kts`:

    ./gradlew verify           # every change, every time. No device needed.
    ./gradlew verifyOnDevice   # before opening a PR. Needs a device/emulator.

- **`verify`** = `:app:assembleDebug` + every subproject's own `check` task —
  ktlint, detekt, and the unit tests all attach themselves to `check` by
  default, and the module list keeps growing, so `verify` depends on `check`
  itself rather than naming module-specific task paths. Adding a module wires
  it into `verify` automatically; nothing to remember to update.
- **`verifyOnDevice`** = `verify` + every module's `connectedDebugAndroidTest`
  (`:core:data` for the Room paths, `:core:designsystem` for the components that
  animate, each `:feature:*` for its screen — computed the same way, no
  hardcoded module list).

CI runs `verify` on every pull request. The local command is deliberately the
same one, so a red check can be reproduced without translating a CI step back
into Gradle tasks.

**`verifyOnDevice` is not optional before a PR.** It is the only thing that
checks the failures which destroy user data, and the only thing that reaches
the feed's branches at all — none of them reproducible off-device:

| Instrumented test | What only it can catch |
| --- | --- |
| `CatDaoTest` | `@Transaction` actually serializing concurrent toggles |
| `CatDatabaseMigrationTest` | `MIGRATION_2_3` copying every column correctly |
| `CatDatabaseUpgradeTest` | a v1 database opening instead of crashing |
| `CatRepositoryImplTest` | the real `Pager`, and favoriting not wiping loaded pages |
| `CatsListContentTest` | which branch the feed shows for a given `LoadState` |
| `FavoriteCatsContentTest` | which branch favorites shows for a given `UiStatus`, and the events its cards send |
| `CatPullToRefreshTest` | a real pull, and the spinner/tick/cross sequence it earns |
| `CatItemTest` | the card's image failing, and the retry asking again |

A JVM fake cannot stand in for any of them — `FakeCatDao` runs the transaction
block inline, so it proves the logic and not the atomicity, and the feed's
`LazyPagingItems.loadState` only exists inside composition. An instrumented
suite nobody runs is no suite at all.

Start an emulator first (`emulator -avd <name>`, or from Android Studio);
`connectedDebugAndroidTest` fails with no device attached.

**Keep the device awake.** A screen that goes to sleep mid-run takes every
Compose UI test in the module with it, all failing with:

    java.lang.IllegalStateException: No compose hierarchies found in the app.
    Possible reasons include: (1) the Activity that calls setContent did not launch...

That reads like a broken test setup, but a whole module's UI tests dying at once
is almost always a sleeping screen. Wake it and pin it before rerunning:

    adb shell input keyevent KEYCODE_WAKEUP
    adb shell svc power stayon true

Also useful:

| Command | Use |
| --- | --- |
| `./gradlew build` | assemble + all unit tests + lint (adds lint over `verify`) |
| `./gradlew lint` | Android lint |
| `./gradlew projects` | list every Gradle module |
| `./gradlew :app:dependencies --configuration debugRuntimeClasspath` | inspect the resolved graph |

Build JDK: **17**, for both halves of the build. Compilation and tests use the Gradle
toolchain; the Gradle daemon itself uses the criteria in
`gradle/gradle-daemon-jvm.properties`, so `./gradlew` picks a JDK 17 daemon (downloading one
if the machine has none) whatever `JAVA_HOME` happens to be. Regenerate that file with
`./gradlew updateDaemonJvm --jvm-version=17`; do not hand-edit it.

---

## Where things live

Nine Gradle modules — see [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) §2b
for the full dependency graph and the rules behind it.

| Thing | Module | Location |
| --- | --- | --- |
| Domain model | `:core:model` | `src/commonMain/kotlin/…/domain/model/` |
| Use cases, `CatRepository`, `ImageDownloader` | `:core:domain` | `src/commonMain/kotlin/…/domain/` |
| Repository impl, API service, Room entity/DAO/migrations, DI modules | `:core:data` | `src/commonMain/kotlin/…/data/` |
| Platform splits (database path, HTTP engine, image download) | `:core:data` | `src/androidMain/`, `src/jvmMain/` |
| Committed Room schemas | `:core:data` | `schemas/` |
| ViewModel-facing shared primitives: `UiText`, `launchCatching`, `RetryableFlow`, `StateOwner`, `SnackbarNotifier` | `:core:ui` | `src/commonMain/kotlin/…/presentation/`, strings in `src/commonMain/composeResources/` |
| Theme, shared components (e.g. the cat image card) | `:core:designsystem` | `src/commonMain/kotlin/…/presentation/theme/`, `…/components/`; icons and strings in `src/commonMain/composeResources/` |
| `MainDispatcherRule` (JUnit, so JVM + Android only) and shared test fakes | `:core:testing` | fakes in `src/commonMain/kotlin/…/testing/`, the rule in `src/jvmAndAndroidMain/…` |
| One MVI screen (State/Event/StateHolder/VM/Screen/ErrorHandler) | `:feature:favorites` | `src/commonMain/kotlin/…/presentation/<name>/`, strings in `src/commonMain/composeResources/` |
| One paged screen (Event/VM/Screen; Paging 3 owns load/error/retry state — [ADR-0024](docs/DECISIONS.md#adr-0024)) | `:feature:feed` | `src/main/kotlin/…/presentation/<name>/` |
| Unit tests | same module as the code they test | `src/test/kotlin/` |
| Device tests (Room behavior, migrations, upgrades) | `:core:data` | `src/androidDeviceTest/kotlin/` |
| Desktop tests (the real database on the JVM) | `:core:data` | `src/jvmTest/kotlin/` |
| Compose UI tests (which branch a screen shows) | the screen's own module | `src/androidTest/kotlin/` |
| Compose UI tests for a component (gestures, phases, image states) | `:core:designsystem` | `src/androidTest/kotlin/` |
| `App`, `MainActivity`, `NavDisplay` + back stack — composition root only | `:app` | `src/main/java/…/`, `…/presentation/navigation/` |
| Convention plugins (`catslist.android.library`, `.kmp.library`, `.jvm.library`, `.compose`, `.koin`, `.quality`) | `build-logic` | `build-logic/convention/src/main/kotlin/` |
| Every dependency and version | — | `gradle/libs.versions.toml` |
| `verify` / `verifyOnDevice` | — | root `build.gradle.kts` |

The Kotlin package does not need to mirror its module — most files kept their
original package when they moved into a new module; only each file's own
generated `R` class reference changes. A `:feature:*` module exposes only its
`NavKey` and one entry `@Composable`; everything else in it is `internal`.
This is the outcome ADR-0001 anticipated and deferred — see
[ADR-0022](docs/DECISIONS.md#adr-0022) for why the split happened, and when.

---

## Do not change without saying so

- `gradle/libs.versions.toml` version bumps — call them out explicitly in the PR;
  never bump a version as a side effect of another change.
- `gradle/wrapper/`, `gradlew`, `gradlew.bat`.
- `build-logic/` — its convention plugins configure every module; a change
  here affects all of them at once.
- `.idea/` and generated `build/` directories.

---

## Conventions

- **New dependency** → add to `gradle/libs.versions.toml`, reference as `libs.…`.
  Never hardcode `"group:name:version"` in a build file.
- **Screen status** is one sealed `UiStatus`, never `isXVisible` booleans —
  except a paged screen's list-loading state, which is `LazyPagingItems.loadState`
  ([ADR-0024](docs/DECISIONS.md#adr-0024)), not something to duplicate into a
  `UiStatus` of its own.
- **No `var` state on a ViewModel** outside the `StateFlow` — model it in
  `XxxState`.
- **Screen arguments** do not come from `SavedStateHandle` (Navigation 3). Use
  Koin injected parameters — [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) §6.
- **Strings** live in `strings.xml`, grouped by screen. ViewModels and
  StateHolders use `UiText`, never `context.getString`.
- **Previews** render the stateless `XxxContent` with fake data, never
  `koinViewModel()`. One per screen, two at most.
- **Read-modify-write on a `MutableStateFlow` uses `update { }`**, never
  `state.value = f(state.value)` — see [ADR-0015](docs/DECISIONS.md#adr-0015).
- **Every `onEvent` branch that calls a suspend or fallible operation handles
  errors the same way as its sibling branches.** Don't write one branch carefully
  and leave the next one bare — that asymmetry is what let a real crash ship here
  once already. Every such branch goes through `ViewModel.launchCatching`.
  **Not a bare `runCatching`**: it catches `CancellationException` too, so
  leaving a screen mid-request gets reported to the user as a failure. See
  [ADR-0013](docs/DECISIONS.md#adr-0013).
- **A feature never depends on another feature.**
- **A `:feature:*` module's ViewModel, StateHolder, ErrorHandler and contracts
  are `internal`.** Only the `NavKey` and the entry `@Composable` are public.
  If a public `@Composable` needs to take the ViewModel as a parameter (for
  `koinViewModel()`'s default), split it into a public overload with no
  ViewModel parameter and a `private` one that takes it — a public function
  cannot take an `internal` type as a parameter.
- **A module with a `@Serializable` type needs `kotlin.plugin.serialization`
  applied directly in its own `build.gradle.kts`** — it is not pulled in by
  `catslist.koin` or any other convention plugin. Missing it compiles fine and
  crashes only at runtime, on first use of the type.
- **A branch on one value with a per-branch extra condition uses a subject
  `when` with a guard (`is X if cond -> …`, Kotlin 2.1+), not `when { x is X
  && cond -> … }`.** The subject form smart-casts and reads as one decision
  tree instead of a flat boolean list.
- **Comments are short and rare.** One or two lines, only where the code cannot
  say it itself — a workaround, a constraint, a non-obvious ordering. No
  paragraph-long rationale essays: durable reasoning belongs in
  [`docs/DECISIONS.md`](docs/DECISIONS.md), and a KDoc that restates the
  signature is noise.
- **American English**, in code, comments, docs and strings alike: `color`,
  `behavior`, `canceled`, `initialize`, `gray`.

---

## Commits

- One logical change per commit. Every commit builds and passes tests.
- **One-line message: a bracket tag, then a capitalized imperative summary. No
  body** — rationale belongs in the PR description, and durable reasoning belongs
  in [`docs/DECISIONS.md`](docs/DECISIONS.md).

  | Tag | Use |
  | --- | --- |
  | `[TECH]` | **engineering-only work** — library/plugin upgrades, new deps, build tooling, the version catalog, the Gradle wrapper, tests, and documentation |
  | `[FEATURE]` | new or changed user-facing behavior |
  | `[FIX]` | bug fixes, and corrections to existing code — refactors, renames, cleanups |
  | `[MERGE]` | merge commits only |

  `[TECH]` is **not** a catch-all for "no user-facing change" — a refactor or
  rename that touches no dependency is `[FIX]`.

  Example: `[TECH] Modernize Gradle/AGP/Kotlin toolchain, target JDK 17`

- Name any forced dependency bump in the PR description.
- No attribution trailers — no `Co-Authored-By:`, no "Generated with" line.
- Don't stage `.idea/`, `build/`, or anything listed in `.gitignore`.

---

## Branches

Branch off the integration branch (`dev`) — never commit straight to it:

    git switch -c <prefix>/<short-kebab-name>

| Prefix | For | Commits |
| --- | --- | --- |
| `tech/` | dependencies, build tooling, tests, documentation | `[TECH]` |
| `feature/` | new or changed behavior | `[FEATURE]` |
| `bugfix/` / `fix/` | bug fixes, refactors, renames, cleanups | `[FIX]` |
| `merge/` | long-running integration branches | `[MERGE]` |

One branch → one PR into `dev`.

**A branch's commits all match its prefix's tag.** If work of a different kind
turns up mid-branch — a real bug found while testing a `tech/` branch, say — it
does not go on the current branch. Branch off `dev` (or off the current tip if
it depends on uncommitted work, then rebase once that merges) and open a
separate PR.

**Review the branch diff before opening the PR.** The verification build catches
compile errors and failing tests; it does not catch a misplaced side effect (UI
code in the data layer), an unhandled failure path, a main-thread blocking call,
or an `onEvent` branch guarded differently from its siblings. Every one of those
has shipped here at least once.
