# Architecture Rules

These rules are mandatory. If a request conflicts with them, explain the conflict
before changing the architecture. This file is the *what*; `AGENTS.md` is the
*how to build / verify*.

> Template markers: replace `com.example.app` with the real applicationId,
> `Feature` / `Xxx` with the real screen name, and delete any section that
> doesn't apply (e.g. Room if there is no database).

---

## 1. Stack

- **Kotlin** (latest stable), **Jetpack Compose** (BOM), **Coroutines + Flow**.
- **Koin** for DI, **Navigation 3** for navigation, **Room** for local persistence
  (only if the app needs a database).
- **All dependency versions live in `gradle/libs.versions.toml`.** Never write a
  version string in a `build.gradle.kts`. Verify each version is the current
  stable before starting; the catalog is the single source of truth.

Known-good set (as of 2026‑09 — verify current): Kotlin 2.4.x, KSP 2.3.x,
AGP 9.x, Gradle 9.7.x, Compose BOM 2026.08.x, Koin 4.1.x,
Room 2.8.x, Ktor 3.6.x, androidx.navigation3 1.1.x,
lifecycle 2.11.x, coroutines 1.11.x, kotlinx-serialization 1.11.x,
kotlinx.collections.immutable (latest), Truth 1.4.x.

---

## 2. Architecture

### 2a. Single-module (historical)

This project shipped single-module — `presentation → domain ← data`, `domain`
depending on nothing Android — from ADR-0001 until ADR-0022. Kept here because
a project starting fresh at two screens should make the same call: module
boundaries buy compile-time layering enforcement and parallel builds, and
neither pays for itself yet. Package boundaries carry the same design with
none of the ceremony. Split when a real trigger shows up — see ADR-0001's
**Review when** and, for what actually triggered it here, ADR-0022.

### 2b. Multi-module (current)

Module graph (arrows = "depends on"):

    :app  ──▶ :feature:feed, :feature:favorites
      │          └──▶ :core:model, :core:data, :core:ui, :core:designsystem
      └──▶ :core:model, :core:data, :core:ui, :core:designsystem

    :core:data          ──▶ :core:model, :core:domain      (multiplatform: common + android + jvm)
    :core:domain        ──▶ :core:model                    (multiplatform: common + jvm)
    :core:model         ──▶ (nothing)                      (multiplatform: common + jvm)
    :core:ui            ──▶ :core:domain                    (multiplatform: common + android + jvm)
    :core:designsystem  ──▶ :core:model, :core:ui          (multiplatform: common + android + jvm)
    :core:testing       ──▶ :core:model, :core:data, :core:ui  (multiplatform; test-only, nothing depends on it in `main`)

Every `:core:*` and `:feature:*` module is Kotlin Multiplatform; only `:app` is
still Android-only. The UI moved to Compose Multiplatform one module at a time,
bottom-up (ADR-0037). See ADR-0028 for the migration order and ADR-0029 for what
`:core:data`'s split looks like.

Source sets in a multiplatform module are `commonMain` plus `androidMain`/`jvmMain`,
and its tests are `commonTest`, `androidHostTest` (JVM, no device), `androidDeviceTest`
(instrumented) and `jvmTest` — AGP's multiplatform plugin names them, not us.

Rules:

- `:core:model` is a pure-Kotlin library module: no Android SDK, Compose, Room
  or Koin on its classpath, checked by construction (its convention plugin
  declares none of them).
- **A `:feature:*` module MUST NOT depend on another `:feature:*` module.**
  Cross-feature navigation goes through `() -> Unit` callbacks (or a
  `Navigator` interface) wired by `:app`; shared logic goes in `:core:*`.
- **Each `:feature:*` module exposes exactly two public things: its `NavKey`
  and one entry `@Composable`.** Everything else — ViewModel, StateHolder,
  ErrorHandler, `XxxContract`, the Koin `Module`, the stateless
  `XxxContent` — is `internal`, enforced by the compiler. A public
  `@Composable` cannot take an `internal` type as a parameter (this is a
  compiler error, not a warning), so the public `XxxScreen(modifier, contentPadding)`
  delegates to a `private` overload that takes the `internal` ViewModel; that
  private overload is where `koinViewModel()`'s default lives.
- `:app` is the **composition root only**: `Application`, `MainActivity`, the
  `NavDisplay` and its back stack. No screens, ViewModels, use cases, entities
  or feature-specific DI modules.
- `:core:domain` owns the use cases, `CatRepository` and `ImageDownloader` —
  the ports, with no implementation and no platform. `:core:data` implements them.
- `:core:data` owns the repository implementation, the API service and Room. It
  splits on three seams only — where the database file lives, how the HTTP engine
  is constructed, and how an image is downloaded (ADR-0029). Anything else that
  reaches for a platform API belongs behind one of those, not in a fourth
  that wrap the repository — this project does not split those into separate
  domain/data/database modules; see ADR-0022's **Alternatives rejected** for
  why a finer split was not worth it at two features.
- `:core:designsystem` (theme, shared components like the cat image card) and
  `:core:ui` (ViewModel-facing primitives — `UiText`, `launchCatching`,
  `RetryableFlow`, `StateOwner`, `SnackbarNotifier`) are deliberately separate:
  one is Compose-visual, the other is Compose-independent logic a ViewModel
  can use without pulling in a design system.
- Feature UI (screens + ViewModels) MUST NOT touch the repository or Room
  directly — only use cases.
- The Kotlin package of a file does **not** need to mirror its module in this
  project — `Cat` stayed in `com.example.catslist.domain.model` when it moved
  from `:app` into `:core:model`, and every other file kept its package too.
  Only each file's own generated `R` class reference changes when it crosses a
  module boundary.
- Shared build config lives in the `build-logic` composite build as
  **convention plugins**: `catslist.kmp.library` and `catslist.android.library`
  are the two bases (`catslist.jvm.library` remains for any module not yet
  moved to `commonMain`); `catslist.compose`, `catslist.koin`, and `catslist.quality`
  (ktlint + detekt) are additive, applied only by the modules that actually
  need them. Never copy an `android { }` block between modules.
- **A module that declares a `@Serializable` type (a feature module's
  `NavKey`) needs `kotlin.plugin.serialization` applied directly — it does not
  come for free from `catslist.koin` or any other convention plugin.** Missing
  it compiles cleanly and crashes only when something actually looks up the
  serializer at runtime (i.e. on first navigation to that screen). This bit
  twice during the ADR-0022 migration; check it explicitly when a new feature
  module's `NavKey` is added.

`api` vs `implementation`:

- Use `api` **only** for a dependency whose types appear in this module's
  *public* signatures. `:core:testing`'s fakes implement `:core:data`'s
  `CatRepository`/`CatApiService`/`CatDao` and `:core:ui`'s `SnackbarNotifier`,
  so a consumer needs those types too — `:core:testing` exposes `:core:model`,
  `:core:data`, and `:core:ui` as `api`.
- Everything else is `implementation`.
- `:core:data` exposes `dataModule` and use cases
  wrapping its own repository, so almost all its dependencies are
  `implementation` — the one exception is `androidx.paging:paging-runtime`,
  `api` because `CatRepository.feed` returns `Flow<PagingData<Cat>>` and
  `PagingData` is therefore part of this module's public surface too
  ([ADR-0025](DECISIONS.md#adr-0025)).

---

## 3. MVI — the screen contract

Every screen is a subpackage / module with these parts, including a *paged*
screen (currently only `:feature:feed`). The one exception there is narrow:
Paging 3 owns **load** state — loading, error and retry for the list itself —
read from `LazyPagingItems.loadState` in the Composable, so a paged screen has
no screen-wide `UiStatus` and no `LoadMore`/`Retry` events. It still has a
`State`, a `StateHolder` and an `ErrorHandler` for everything Paging does not
load; `PagingData` is exposed alongside `state` rather than inside it, because
`LazyPagingItems` can only be built by the Composable that collects it. See
[ADR-0024](DECISIONS.md#adr-0024) and [ADR-0025](DECISIONS.md#adr-0025).

| File | Role |
| --- | --- |
| `XxxContract.kt` | `XxxState` (immutable), `XxxEvent` (sealed — all user intents) |
| `XxxStateHolder.kt` | `IXxxStateHolder` + impl — owns the `MutableStateFlow`, exposes intent-named mutators (§3a) |
| `XxxViewModel.kt` | thin orchestrator, registered in the feature's Koin module (§3b) |
| `XxxScreen.kt` | stateless `XxxContent(state, onEvent)` + thin `koinViewModel()` entry (§7) |
| `XxxErrorHandler.kt` | `IXxxErrorHandler` + impl — maps failures to state / notifications (§3c) |
| `XxxUiMapper.kt` *(if the screen renders a list/sections)* | domain model → UI model |
| `XxxDialogFactory.kt` *(if the screen shows dialogs)* | builds `DialogModel` from a sealed `DialogType` (§3d) |
| `XxxViewModelTest`, `XxxStateHolderTest`, mapper/factory tests | §9 |

### 3a. State + StateHolder

- `XxxState` is an **`@Immutable data class`** holding the *complete* UI state.
- **Screen status is ONE sealed type, never a set of `isXVisible` booleans:**

      @Immutable
      sealed interface UiStatus {
          data object Content : UiStatus
          data object Empty : UiStatus
          data object Loading : UiStatus
          data class Error(val message: UiText, val retryable: Boolean) : UiStatus
      }

- **Declare the statuses most-likely-first, and make every `when` over them
  mirror that order.** `when` on a sealed type is a linear chain of identity /
  `instanceof` checks, so the likeliest case is tested first — though at this
  size that costs nothing measurable, and the real point is that branch order
  becomes checkable against the declaration instead of resting on an unverifiable
  claim about which state is common.

      @Immutable
      data class XxxState(
          val status: UiStatus = UiStatus.Loading,
          val items: ImmutableList<XxxUiItem> = persistentListOf(),
          val dialog: DialogModel? = null,
          val pendingAction: XxxAction? = null,   // model in-flight ops here — NOT as a var on the VM
          val isRefreshing: Boolean = false,
      )

- **No mutable state outside the StateFlow.** Anything the old-style code would
  keep as a `private var` on the ViewModel (a pending action, a cached list, a
  "which dialog") is a field in `XxxState`.
- The **StateHolder owns the flow and all mutation**:

      interface IXxxStateHolder : StateOwner<XxxState> {
          fun showLoading()
          fun showContent(items: ImmutableList<XxxUiItem>)
          fun showEmpty()
          fun showError(message: UiText, retryable: Boolean)
          fun setRefreshing(value: Boolean)
          fun setDialog(dialog: DialogModel?)
          fun setPendingAction(action: XxxAction?)
          fun reset()
      }

      @ViewModelScoped
      class XxxStateHolder @Inject constructor() : IXxxStateHolder {
          private val _state = MutableStateFlow(XxxState())
          override val state: StateFlow<XxxState> = _state.asStateFlow()

          override fun showContent(items: ImmutableList<XxxUiItem>) = _state.update {
              it.copy(status = UiStatus.Content, items = items, isRefreshing = false)
          }
          // …one mutator per transition; each produces a valid full state
          override fun reset() = _state.update { XxxState() }
      }

- The StateHolder is **`@ViewModelScoped`** so the ViewModel *and* every
  collaborator that mutates it (error handler, etc.) get the **same instance**.
- The ViewModel re-exposes the state: `class XxxViewModel(...) : ViewModel(),
  StateOwner<XxxState> by stateHolder`.

### 3b. ViewModel — thin orchestrator

- Registered in the feature's Koin module, delegates state via `by stateHolder`, contains **no** state
  mutation and **no** error branching.
- One **typed event entry point**: `fun onEvent(event: XxxEvent)` over a sealed
  `XxxEvent`. (For click-heavy screens, route events through a shared
  `EventThrottler` / `ScreenEventBus` in `core:ui` that applies `throttleFirst`
  before dispatch — one shared utility, not per-screen.)
- On an event: call a use case, then a StateHolder mutator and/or a collaborator.
- **Constructor dependency cap ≈ 7.** Consolidate collaborators:
  `navigator`, `notifier` (snackbars + toasts + dialog surface),
  `telemetry` (analytics + crash/trace), `errorHandler`, plus the use case(s)
  and the state holder.

      class XxxViewModel @Inject constructor(
          private val stateHolder: IXxxStateHolder,
          private val errorHandler: IXxxErrorHandler,
          private val getItems: GetXxxUseCase,
          private val navigator: Navigator,
          private val notifier: UiNotifier,
          private val dialogFactory: IXxxDialogFactory,
      ) : ViewModel(), StateOwner<XxxState> by stateHolder {

          init { observeItems() }
          override fun onCleared() { stateHolder.reset() }

          private fun observeItems() {
              getItems()                              // Flow<Result<List<Xxx>>>
                  .onEach(::render)
                  .launchIn(viewModelScope)           // set up ONCE, never re-launch
          }

          fun onEvent(event: XxxEvent) = when (event) {
              XxxEvent.Refresh        -> refresh()
              XxxEvent.Back           -> navigator.back()
              is XxxEvent.ItemClicked -> navigator.toDetail(event.id)
              is XxxEvent.DeleteClicked -> confirmDelete(event.id)
              XxxEvent.DialogConfirmed -> runPendingAction()
              XxxEvent.DialogDismissed -> stateHolder.setDialog(null)
          }

          private fun render(result: Result<List<Xxx>>) = result
              .onSuccess { stateHolder.showContent(it.toUi()) }
              .onFailure { errorHandler.onLoadFailure(it) }
      }

- **No `viewModelScope.launch` nested inside a coroutine you are already in.**
- Transient state that must survive process death (a form draft, a
  `pendingAction` mid-dialog) is persisted via **`SavedStateHandle`**.

### 3c. Extracted error handler

- All failure → UI mapping lives in `IXxxErrorHandler`. The ViewModel calls it;
  the handler calls StateHolder mutators + `notifier`.
- Branch on a **sealed error type** from the data layer (e.g. `AppError.Network`,
  `AppError.Unauthorized`, `AppError.Unknown`), never on raw exception classes in
  the ViewModel.

      @ViewModelScoped
      class XxxErrorHandler @Inject constructor(
          private val stateHolder: IXxxStateHolder,
          private val notifier: UiNotifier,
          private val session: SessionController,
      ) : IXxxErrorHandler {
          override fun onLoadFailure(error: AppError) = when (error) {
              is AppError.Network      -> stateHolder.showError(UiText.Resource(R.string.err_network), retryable = true)
              is AppError.Unauthorized -> session.logout()
              else                     -> stateHolder.showError(UiText.Resource(R.string.err_generic), retryable = true)
          }
      }

### 3d. Dialog factory + dialog-as-state

- Dialogs are data, not navigation. `XxxState.dialog: DialogModel?` (nullable
  field). The Composable renders it when non-null.
- `DialogModel` is an `@Immutable` sealed type; text is `UiText`.
- `IXxxDialogFactory` builds a `DialogModel` from a sealed `XxxDialogType`. The
  ViewModel: `stateHolder.setDialog(dialogFactory.confirmDelete())`.
- Confirm/dismiss come back as `XxxEvent`s; the pending operation to run on
  confirm is stored in `XxxState.pendingAction`.

---

## 4. Coroutines & Flow

- `viewModelScope` only. Expose state as `MutableStateFlow` → `asStateFlow()`.
- Collect upstream with `flow.onEach { }.catch { }.launchIn(scope)` — **set up
  once**; never re-`launchIn` the same source on a repeated call.
- Domain / data `suspend` functions are **main-safe**. Room already is; any other
  blocking work uses an **injected dispatcher** (`@Dispatcher(IO)` qualifier +
  a `DispatchersModule`), never a hardcoded `Dispatchers.IO`.
- **Read-modify-write on a `MutableStateFlow` uses `update { }`**, never
  `state.value = f(state.value)`. `update` is a compare-and-set loop, so the
  value read is the value written. The plain form reads and writes separately
  and is safe only while every caller happens to resume on the same thread —
  which a repository or data source cannot promise. Across a suspension point
  the window is far wider and the suspension is invisible at the call site —
  there, atomicity needs synchronization that explicitly spans the whole
  operation (a `Mutex` held across it, or a database transaction); the
  suspension itself is not what breaks it.
- Recoverable failures are returned as `Result<T>` (or a project `Outcome` /
  `Either`), not thrown across the domain boundary. Only truly exceptional cases
  throw.
- One-shot effects (navigation, snackbars): injected collaborators
  (`Navigator`, `SnackbarNotifier`) **or** a `Channel<XxxEffect>` consumed once — pick
  one per project and be consistent. Do not put one-shot signals in `State`.
- **No second event bus** for "another screen wants this one to refresh." Observe
  a `Flow` from the data layer (single source of truth) instead.

---

## 5. Navigation 3

- Each feature owns its `NavKey` (`@Serializable data object` / `data class`) in
  an `XxxNavKey.kt`.
- The `NavDisplay` + back stack lives in `:app` (or a dedicated `:core:navigation`
  module) — the only place that knows every feature's key.
- Screens take `onNavigateX: () -> Unit` callbacks (or an injected `Navigator`
  whose impl is in `:app`). A feature never references another feature's key and
  never builds the graph.
- **Screen arguments** — Navigation 3 does **not** route args through
  `SavedStateHandle`. Use Koin's injected parameters — the whole of what Hilt
  needed `@AssistedInject` + an `@AssistedFactory` interface for:

      class XxxViewModel(
          private val id: Long,
          …
      ) : ViewModel()

      // in the feature's Koin module:
      viewModel { (id: Long) -> XxxViewModel(id = id, …) }

      // in the NavDisplay entry:
      val vm = koinViewModel<XxxViewModel> { parametersOf(key.id) }

  Pass a plain value, not the `NavKey` type, so the ViewModel stays free of
  navigation types.
- `SavedStateHandle` is for surviving **process death** of transient state
  (drafts, `pendingAction`), never for route args.

---

## 6. Koin

- Each module owns its own DI as a `Module` value — `dataModule`, `uiModule`,
  `feedModule`, `favoritesModule`. Koin does **not** aggregate them: `App.startKoin`
  lists every one by hand, and a new module must be added there or nothing in it
  resolves.
- **domain carries no DI at all.** Use cases are plain classes with plain
  constructors; `dataModule` is what knows how to build them.
- `single` for what was `@Singleton` (repository, database, the `HttpClient`, the
  SnackbarNotifier); `factory` for everything else.
- Screen collaborators that must **share** a StateHolder (ViewModel + ErrorHandler)
  are constructed **inside the `viewModel { }` lambda** and passed to both. This is
  the explicit replacement for `@ViewModelScoped`; do not register a StateHolder as
  its own definition, or the two will resolve to different instances.
- Apply Koin through the `catslist.koin` convention plugin, never by hand per module.
- **Every feature module gets a graph test** (`FeedModuleTest`) that resolves its
  ViewModel against `:core:testing` fakes. Koin resolves at runtime, so this is
  what catches a definition that drifted from its constructor (ADR-0026).

---

## 7. Compose / UI

- Split every screen: a **stateless** `XxxContent(state: XxxState, onEvent: (XxxEvent) -> Unit)`
  plus a thin `XxxScreen(viewModel: XxxViewModel = koinViewModel())` that does
  `val state by viewModel.state.collectAsStateWithLifecycle()` and forwards
  `viewModel::onEvent`.
- Composables contain **no business logic** — render state, emit events.
- `@Immutable` / `@Stable` on `State` and every UI model type. Use
  `ImmutableList` / `persistentListOf()` (kotlinx.collections.immutable) in state,
  never a raw `List` you rebuild each emission.
- **Strong skipping is on, so most manual annotation is obsolete — but not all of
  it.** Two cases the compiler still cannot work out on its own, and neither is
  fixed by an annotation on the call site:
  - a **sealed interface** used as a parameter type. An implementation the
    compiler has not seen could be anything, so it is unstable unless the
    interface itself is `@Immutable` — and that promise is only true if every
    case is really immutable (`UiText.Resource` holds an `ImmutableList`, not a
    `List`, for exactly this reason).
  - a class from a module the **Compose compiler does not compile**
    (`:core:model`) or from a **third-party library**. Nothing there carries
    stability metadata, so it is assumed unstable. These are declared in
    [`config/compose-stability.conf`](../config/compose-stability.conf), which
    every Compose module points at — never by moving the class or wrapping it.
- **Do not guess at any of this — measure it.** `./gradlew assembleRelease
  -Pcatslist.composeMetrics` writes the compiler's own stability and skippability
  reports to each module's `build/compose-metrics/`. A parameter listed
  `unstable` is compared by identity and its composable never skips. See
  [ADR-0033](DECISIONS.md#adr-0033).
- **Design system** lives in `core:designsystem`: theme + tokens + reusable
  components (buttons, cells, loaders, error block, empty state, dialog host).
  Screens compose these; they don't hand-roll spacing/colors. `core:ui` is a
  separate module for the ViewModel-facing primitives (§2b) — it has no
  Compose-visual content of its own.

---

## 8. Strings & resources

- **No user-facing string literal in code.**
- Model text as a `UiText` sealed type, resolved only in Composables:

      @Immutable
      sealed interface UiText {
          data class Raw(val value: String) : UiText
          data class Resource(val id: StringResource, val args: ImmutableList<Any>) : UiText
      }
      @Composable fun UiText.resolve(): String = when (this) { … }
      suspend fun UiText.load(): String = when (this) { … }  // outside composition

  Strings are Compose resources (`src/commonMain/composeResources/values/strings.xml`),
  read through each module's generated `Res` (ADR-0037).

- ViewModels / StateHolders / mappers **never** call `context.getString` — they
  put a `UiText` in state.
- Formatting / pluralization via string resources with placeholders (`%1$s`) or
  `plurals`, never string concatenation.
- **`strings.xml` is grouped by screen**, one comment-headed block per screen,
  in the order the screens appear in the app. Strings used by 2+ screens (e.g.
  from a shared component) get their own `common` block instead of picking one
  screen to own them.
- **Every string name is prefixed with the name of the screen it belongs to**:
  `catslist_*`, `favoritecats_*`, `common_*` for shared strings. Exception:
  truly app-wide strings with no screen owner (`app_name`) stay unprefixed.

---

## 9. Testing

Every use case, repository impl, mapper/factory, **StateHolder**, and
**ViewModel** has a unit test, in the same module under `src/test/kotlin`,
same package.

- **Hand-written fakes, not a mocking library.** A fake is a real in-memory
  implementation backed by `MutableStateFlow`. Shared fakes live in
  `core:testing`; screen-only fakes stay in that module's `src/test`.
- ViewModel tests: `@get:Rule val mainDispatcherRule = MainDispatcherRule()`
  (`UnconfinedTestDispatcher`), build the real ViewModel with fake collaborators
  + real use cases over a fake repository, then assert on
  `viewModel.state.value` after calling `onEvent(...)`. Never involve Koin.
- `runTest { }` for anything touching `suspend` / `Flow`. Assertions with Google
  Truth.
- StateHolder tests assert each mutator produces a fully valid state (no
  half-set status).
- **Every screen gets a Compose UI test**, in its own module under
  `src/androidTest/kotlin`: `createComposeRule()`, the stateless `XxxContent`
  with fake state, one test per branch, plus the events its controls send. A
  ViewModel test proves which status the screen reaches; only this proves what
  that status puts on screen. For the feed there is no alternative at all — its
  loading, empty, error and retry come from `LazyPagingItems.loadState`
  ([ADR-0024](DECISIONS.md#adr-0024)), which exists only inside composition.
  Hold the test clock (`mainClock.autoAdvance = false`) — the shimmer never
  ends, so a test that waits for idle waits forever.
- **A design-system component with behavior of its own gets the same treatment**
  in `:core:designsystem`: a gesture, a phase that is held for a while, an
  image that fails. Work the component from the outside — a real swipe, a real
  request — because that is where these break, and a component test is the only
  place a screen test's setup cannot hide it.
- Recommended additions: screenshot tests over previews (Paparazzi /
  Roborazzi), and — if Room is used — migration tests with
  `MigrationTestHelper` (set `exportSchema = true` and commit the schema JSON).

---

## 10. Previews

- **1 preview per screen (max 2)** — e.g. `Content`, and one of `Error` /
  `dark`. Use `@PreviewParameter` with a small provider, or two `@Preview` funcs.
- **Every reusable design-system component** gets a preview.
- Previews render the **stateless** `XxxContent` with fake state + no-op
  `onEvent`; never `koinViewModel()`, never DI.
- Wrap every preview in the app theme.

---

## 11. Room (only if the app has a database)

- `@Entity` / `@Dao` / `@Database` / migrations live in the data layer
  (`:core:database` or `data/local/`).
- Real `Migration` objects + `exportSchema = true` + committed schema JSON +
  migration tests.
- Entities never leave the data layer — map `Entity ↔ domain model`.
- DAO returns `Flow<…>` for observation, `suspend` for one-shot reads/writes.

### 11a. Every schema version bump

Four things, none optional:

1. **Bump `version` on `@Database`** alongside the entity change.
2. **Write the migration for that one step** — `MIGRATION_3_4`, never a
   `MIGRATION_2_4`. Room chains them, so a v2 database runs 2→3 then 3→4 on its
   own. (Once both schema JSONs are committed, `@AutoMigration(from, to)` can
   generate the SQL for added columns, and for renames/deletes declared with
   `@RenameColumn` / `@DeleteColumn`. Hand-write anything that actually
   transforms data. Generated SQL still gets a test — it is only as correct as
   the annotations.)
3. **Register it** in the one place the database is configured.
4. **Test the single step *and* the chain from the oldest supported version.**
   A chain breaks in ways one step cannot: `MIGRATION_3_4` referencing a column
   that `MIGRATION_2_3` removed passes 3→4 and dies on a real user's v2
   database. `MigrationTestHelper.runMigrationsAndValidate(db, 4, true,
   MIGRATION_2_3, MIGRATION_3_4)` runs the whole sequence.

### 11b. A changed schema JSON is a review signal

Changing an entity without bumping `version` does **not** fail the build — Room
silently overwrites that version's JSON with the new shape, and the app then
opens existing databases against a schema that no longer describes them.

Git is the tripwire. In a diff:

- a **new** `N.json` is correct — a version was added;
- a **modified** existing `N.json` is a signal that the schema may have changed
  without a version bump. Other things can touch that file — an export-config
  change, a Room version emitting the format differently — so treat it as a
  prompt to check, and as a bug until someone shows otherwise.

### 11c. Destructive fallback: blanket vs. version-scoped

These are not the same thing and must not be reviewed as if they were.

- `fallbackToDestructiveMigration()` — **never ships.** It is a blanket standing
  permission to wipe user data for *any* missing migration, including one you
  forget to write next year. The failure is silent and retroactive.
- `fallbackToDestructiveMigrationFrom(dropAllTables = true, N)` — **allowed, and
  deliberate.** It names one obsolete version and says data from it is not worth
  carrying forward. Every *other* missing migration still fails loudly, which is
  the property the blanket call throws away. Pass `dropAllTables = true` when the
  old version's tables are no longer entities.

Comment the reason at the call site: which version, and why its data is not
being migrated.

### 11d. Dropping support for an old version is a policy decision

Age alone is not a reason. Retiring a migration path means every user still on
that version loses their data on upgrade, so it is decided deliberately — not
applied automatically once a version feels old.

Retire a path only when someone has decided, and recorded, that it is
acceptable: the version never shipped, telemetry says nobody is on it, or the
product has accepted the loss. Until then a migration path that is merely old is
still a migration path, and it keeps its test.

---

## 12. Anti-patterns — do NOT

- `isLoadingVisible` / `isErrorVisible` / `isEmptyVisible` boolean soup → one
  sealed `UiStatus`.
- A `private var` on the ViewModel holding state outside the `StateFlow` → model
  it as a `State` field.
- A second event bus / `Channel` for cross-screen "refresh" triggers → observe a
  data-layer `Flow`.
- `viewModelScope.launch { }` nested inside a coroutine you're already in.
- Re-`launchIn`-ing the same upstream flow on a repeated call.
- `context.getString(...)` in a ViewModel / StateHolder / mapper → `UiText`.
- Repository or DAO access from a Composable or ViewModel → use cases only.
- A `:feature:*` module depending on another `:feature:*` module.
- A hardcoded dependency version in a build file → version catalog.
- More than ~7 constructor parameters on a ViewModel → consolidate collaborators.
- Repository interfaces or use cases in the data or presentation layer → domain.
- A UI-producing side effect (`Toast`, `Snackbar`, `Dialog`, `AlertDialog`) called
  from `data` or `domain` → route it as a one-shot `CatsListEffect`-style signal
  (§4) from the ViewModel instead; `data`/`domain` return/throw a result, they
  never produce UI feedback themselves.

---

## 13. Before writing code

1. Read this file and `AGENTS.md`.
2. Inspect the existing structure; reuse existing abstractions.
3. Do not duplicate a repository, use case, state, event, mapper or ViewModel
   that already exists.
4. If the request conflicts with these rules, explain the conflict before
   changing the architecture.
