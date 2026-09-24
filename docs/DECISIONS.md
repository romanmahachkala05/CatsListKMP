# Decision record

[`ARCHITECTURE.md`](ARCHITECTURE.md) says *what* this codebase does. This file says *why*, and what
was given up for it.

Each entry follows the ADR shape — **Context**, **Decision**, **Consequences**,
and the alternatives that were rejected where the rejection is the interesting
part. Entries are append-only: a decision that turns out to be wrong is not
edited, it is **superseded** by a later one, so the reasoning stays legible in
both directions. A decision whose core still holds but which was written more
broadly than it needed to be is **amended** instead — same append-only rule,
narrower correction.

**What belongs here.** If another engineer could reasonably make a different
choice without knowing why this one was made, it is an ADR. If the answer is
obvious from the code, it is a comment. "Cancellation semantics", "fake vs mock"
and "which Room fallback" qualify; "used a `HashSet` for O(1) lookup" does not.

> Entries 1–8 were reconstructed from commit history and the architecture spec
> rather than written at the time; the reasoning is genuine, the format is
> retrofitted. From ADR-0009 onward each was recorded as the decision was made.

Several entries record defects that were introduced *by this modernization* —
not inherited from the 2022 app. ADR-0013, ADR-0014 and ADR-0015 are all bugs
written during the rewrite, found afterward, reproduced, and fixed with tests
that fail without the fix. They are here because finding them was the work.

| # | Decision | Status |
| --- | --- | --- |
| [0001](#adr-0001) | Single-module Clean Architecture | **Superseded** by 0022 |
| [0002](#adr-0002) | Kotlin DSL + version catalog, no hardcoded versions | Accepted |
| [0003](#adr-0003) | Compose with an explicit MVI screen contract | Accepted |
| [0004](#adr-0004) | Hilt for dependency injection | **Superseded** by 0026 |
| [0005](#adr-0005) | Real Room migrations, not destructive fallback | Accepted |
| [0006](#adr-0006) | Coil instead of Glide | Accepted |
| [0007](#adr-0007) | No UI side effects below the presentation layer | Accepted |
| [0008](#adr-0008) | Fetch the feed in pages, not one request per cat | Accepted |
| [0009](#adr-0009) | Navigation 3 | Accepted |
| [0010](#adr-0010) | Stay on AGP 8.x | Accepted |
| [0011](#adr-0011) | A narrow Snackbar collaborator, not an event bus | Accepted |
| [0012](#adr-0012) | Hand-written fakes, no mocking library | Accepted |
| [0013](#adr-0013) | `launchCatching`, never bare `runCatching` | Accepted |
| [0014](#adr-0014) | Atomic favorite toggle via `@Transaction` | Accepted |
| [0015](#adr-0015) | Dedupe a page on write, not with an in-flight guard | Accepted |
| [0016](#adr-0016) | Feed errors are not retryable | **Superseded** by 0019 |
| [0017](#adr-0017) | Version-scoped destructive fallback | Accepted |
| [0018](#adr-0018) | Two-tier verification | Accepted, **amended** by 0034, 0035 |
| [0019](#adr-0019) | Make the feed resubscribable instead | Accepted |
| [0020](#adr-0020) | One serialization library | Accepted |
| [0021](#adr-0021) | Derive versionCode from the version name | Accepted, **amended** by 0037 |
| [0022](#adr-0022) | Split the app into Gradle modules | Accepted |
| [0023](#adr-0023) | Paging 3 for the feed, with a `RemoteMediator` | **Superseded** by 0025 |
| [0024](#adr-0024) | Paging owns the feed's *load* state, not its whole state | Accepted |
| [0025](#adr-0025) | Page the feed from the network; persist only favorites | Accepted |
| [0026](#adr-0026) | Koin for dependency injection | Accepted |
| [0027](#adr-0027) | Ktor for HTTP, replacing Retrofit | Accepted |
| [0028](#adr-0028) | Migrate to KMP module by module, from the bottom | Accepted, **amended** by 0039 |
| [0029](#adr-0029) | `:core:data` on Room KMP, with a real desktop target | Accepted |
| [0030](#adr-0030) | One shared, configured `OkHttpClient` | Accepted |
| [0031](#adr-0031) | Connectivity as a `Flow`, not a pre-flight check | Accepted |
| [0032](#adr-0032) | Classify failures once, in `data`, as `AppError` | Accepted |
| [0033](#adr-0033) | Measure recomposition, then fix stability at the source | Accepted |
| [0034](#adr-0034) | Run CI on every pull request; `verify` was never enforced | Accepted |
| [0035](#adr-0035) | `verify` compiles the instrumented tests it cannot run | Accepted |
| [0036](#adr-0036) | Porting v2.3.0 from the Android app: what changed on the way in | Accepted |
| [0037](#adr-0037) | The UI moves to Compose Multiplatform, one module at a time | Accepted |
| [0038](#adr-0038) | Adaptive layout: one grid rule, one width breakpoint | Accepted |
| [0039](#adr-0039) | iOS targets for every multiplatform module | Accepted |
| [0040](#adr-0040) | The iOS app: a thin Xcode project around `CatsApp()` | Accepted |

---

## ADR-0001

### Single-module Clean Architecture

**Superseded** by [ADR-0022](#adr-0022) · 2026-09-11

**Context.** The 2022 codebase had no layering: an activity reached into a Room
database through a `CatStorage` singleton, and models carried Room annotations
all the way into the UI. The app is two screens.

**Decision.** Clean Architecture in one Gradle module — `presentation → domain ←
data`. `domain` depends on nothing Android: no SDK, no Compose, no Room, no
Hilt. `data` implements interfaces that `domain` declares.

**Alternative rejected.** A `:core:*` / `:feature:*` module split. Module
boundaries buy compile-time enforcement of the layering and parallel builds —
neither of which pays for itself at two screens, while the ceremony is
immediate. Package boundaries carry the same design with none of the cost.

**Consequences.** The layering is a convention, not a compiler guarantee:
nothing stops someone importing Room from `presentation` except review.
[`ARCHITECTURE.md`](ARCHITECTURE.md) §2b specifies the target module graph so the split, when it
happens, is a mechanical move rather than a redesign. The trigger is a second
feature team or a build slow enough to notice — not repo size.

**Review when:** a second team needs to own a feature independently, the build
is slow enough that parallel module compilation would pay, or the layering has
been violated often enough that review is clearly not catching it.

**Why it was superseded.** None of those three triggers happened — there was
never a second team, and the build was never measured as slow. What actually
drove the split was different: a second animal type and a unified favorites
screen were about to need the same domain model and image card from two
places, which package boundaries cannot stop from drifting apart the way this
project's own retry-resubscription logic already had to be de-duplicated once.
ADR-0022 is the split this ADR's §2b anticipated, arrived at for a related but
not identical reason.

---

## ADR-0002

### Kotlin DSL + version catalog, no hardcoded versions

**Accepted** · 2026-09-11

**Context.** Versions were string literals spread across Groovy build files,
some of them duplicated at different values.

**Decision.** Groovy → Kotlin DSL, every version in
`gradle/libs.versions.toml`, referenced as `libs.…`. A hardcoded
`"group:name:version"` in a build file is a review failure.

**Consequences.** One place to see the dependency surface, and a dependency bump
becomes a one-line diff a reviewer can actually check. Kotlin DSL costs slightly
slower configuration in exchange for type safety and IDE completion. The catalog
is also what makes "never bump a version as a silent side effect of another
change" an enforceable rule rather than a wish.

---

## ADR-0003

### Compose with an explicit MVI screen contract

**Accepted** · 2026-09-11

**Context.** Screens were XML plus activities holding mutable fields, with
visibility toggled by hand — the state a screen was actually in existed only as
a combination of flags nobody could enumerate.

**Decision.** Compose, and every screen gets the same six parts: `XxxState`
(immutable, one sealed `UiStatus` — never `isXVisible` booleans), `XxxEvent`,
`XxxStateHolder` (the only thing that mutates state), `XxxErrorHandler`, a thin
`XxxViewModel` that only orchestrates, and a stateless `XxxContent` the previews
render.

**Consequences.** Mutually exclusive loading/content/error states become
unrepresentable, because `UiStatus` is one value rather than independent boolean
flags. It does not make every nonsensical `State` unconstructable — other fields
can still be combined in ways that mean nothing — but it removes the class of
bug that boolean soup is made of. The cost is
boilerplate: six files for a screen that could be one, which only pays off once
a screen has more than two states. It also makes the ViewModel testable without
Compose, which is what most of the test suite relies on.

---

## ADR-0004

### Hilt for dependency injection

**Accepted** · 2026-09-11

**Context.** Collaborators were constructed inline or reached through
singletons, so nothing could be substituted in a test.

**Decision.** Hilt, with scoping stated explicitly: `@Singleton` for the
repository and the database, `@ViewModelScoped` for StateHolders and
ErrorHandlers, qualifiers for anything ambiguous (`@Dispatcher(IO)`).

**Consequences.** Compile-time verification of the graph, which a
service-locator approach does not give. The cost is KSP build time and error
messages that point at generated code. Scoping had to become deliberate: the
ViewModel and its ErrorHandler both depend on `IXxxStateHolder`, and the
`@ViewModelScoped` binding is what makes those two injection points resolve to
the same instance — so both operate on one state object without it being passed
by hand. The scope alone would not do it; the shared dependency is half the
mechanism.

---

## ADR-0005

### Real Room migrations, not destructive fallback

**Accepted** · 2026-09-11

**Context.** The app shipped `fallbackToDestructiveMigration()`. Every schema
change silently deleted every favorite the user had saved, on upgrade, with no
error and no way to know it had happened.

**Decision.** Real `Migration` objects, `exportSchema = true`, schema JSON
committed to the repo, and migration tests against them.

**Consequences.** Schema changes become deliberate work rather than a free
action, which is the point. The committed schema JSON doubles as a review
signal: a *new* `N.json` in a diff is a new version, while a *modified* existing
one is a signal that the schema may have changed without a version bump — which
Room does not treat as a build failure. Other things can touch that file (an
export-configuration change, a Room version that emits the format differently),
so it is a prompt to check rather than proof on its own. See
[`ARCHITECTURE.md`](ARCHITECTURE.md) §11b.

---

## ADR-0006

### Coil instead of Glide

**Accepted** · 2026-09-12

**Context.** Image loading used Glide, via an `AndroidView` bridge once the UI
moved to Compose.

**Decision.** Coil 3, which is Compose-native and Kotlin/coroutines-first.

**Consequences.** `AsyncImage` composes directly, so the interop layer and its
manual lifecycle handling disappear. Glide is the more mature library with the
larger feature surface; none of that surface was in use here.

Not yet done: Coil and Retrofit each build their own default `OkHttpClient`, so
the app runs two. Supplying one shared, configured client to both is a loose end,
not a decision.

Partially closed by ADR-0022: splitting Retrofit and Coil into separate Gradle
modules (`:core:data`, `:core:designsystem`) surfaced that they'd been
resolving to *different* OkHttp versions — Retrofit's own transitive floor
(3.14.9) predates the Kotlin extension `:core:data` calls, and only Coil's
newer one, sharing the single-module classpath, was masking it. Both now
resolve to one pinned version (`libs.versions.toml`'s `okhttp`). They are
still two separate `OkHttpClient` instances, each with its own connection pool
and cache — that part of this gap is unchanged.

Fully closed by [ADR-0026](#adr-0026): one client, provided by Hilt, handed to
both.

---

## ADR-0007

### No UI side effects below the presentation layer

**Accepted** · 2026-09-12

**Context.** `CatImageDownloader` — a `data` class — showed a `Toast` directly.
It was untestable, it assumed a main thread, and it meant the data layer decided
what the user saw.

**Decision.** `data` and `domain` never produce UI. A failure travels back as a
return value or an exception; `presentation` decides what to show. The
downloader became a `suspend` function on an injected dispatcher behind a
domain-owned `ImageDownloader` port.

**Consequences.** The download path is testable off-device with a fake, and it
is main-safe by construction. One more interface and one more Hilt binding to
carry. Recorded as an anti-pattern in [`ARCHITECTURE.md`](ARCHITECTURE.md) §12 so it does not creep
back.

---

## ADR-0008

### Fetch the feed in pages, not one request per cat

**Accepted** · 2026-09-12

**Context.** The feed issued one HTTP request per cat, because the original code
called the random-image endpoint in a loop.

**Decision.** One request per page using the API's `limit` parameter, with a
`PAGE_SIZE` constant in the repository.

**Consequences.** A page of ten cats now takes one HTTP request instead of ten,
so the API's rate limit stops being a practical ceiling. The endpoint can return the same cat more
than once, so the repository has to deduplicate — see ADR-0015, where getting
that deduplication subtly wrong turned out to be a crash.

---

## ADR-0009

### Navigation 3

**Accepted** · 2026-09-13

**Context.** The two screens were a `TabRow` inside one composable, so there was
no back stack, no per-destination state scoping, and no route to a third screen
that is pushed rather than switched.

**Decision.** Navigation 3 (`NavDisplay` + `rememberNavBackStack` + typed
`NavKey`s), with the two destinations as peers replaced on the back stack rather
than pushed.

**Alternative rejected.** Navigation Compose. It is the stable, well-documented
option. Navigation 3's state-driven back stack and typed `NavKey`s fit this
app's navigation model better than a route-based API with `SavedStateHandle`
argument passing, and adopting the older one now would mean migrating later.

**Consequences.** Typed keys instead of string routes, and the back stack is
ordinary state the app owns rather than a framework object it queries.
Navigation 3 is new, so the documentation is thin and community answers mostly
do not exist yet — several of its APIs here were confirmed by reading the
published sources rather than the guides.

`rememberViewModelStoreNavEntryDecorator` is deliberately *not* used: it
requires lifecycle 2.11, which requires compileSdk 37, which requires AGP 9.1 —
see ADR-0010. ViewModels are therefore activity-scoped rather than
destination-scoped, which is acceptable while both destinations are top-level
and permanent.

**Review when:** a destination needs its own ViewModel lifecycle — a detail screen
whose state should die with it — or Navigation 3's API changes in a way that
invalidates the shapes used here.

---

## ADR-0010

### Stay on AGP 8.x

**Accepted** · 2026-09-13

**Context.** Adding Navigation 3 pulled in a lifecycle 2.11 bump, which failed
the build with thirteen AAR metadata errors: lifecycle 2.11 wants compileSdk 37,
which wants AGP 9.1. Navigation 3 itself only requires AGP ≥ 8.9.1.

**Decision.** Revert the lifecycle bump, drop the unused
`lifecycle-viewmodel-navigation3`, and stay on AGP 8.13 / Gradle 8.13.

**Consequences.** Moving to AGP 9 changes the Kotlin/Gradle integration and
would pull in unrelated build-system migration work with no benefit to this
project today. Staying on 8.x keeps that separate from the navigation work. The cost is deferred: destination-scoped ViewModels stay
unavailable until the AGP 9 move happens, and it has to happen eventually.

The useful lesson is in the diagnosis, not the outcome — the build failure
presented as "Navigation 3 is incompatible", and the actual culprit was a
transitive bump made in the same commit. Changing one thing at a time is what
made that separable.

**Review when:** a dependency this project actually needs requires AGP 9, or the
destination-scoped ViewModels deferred in ADR-0009 become necessary. Not before:
being on the newest AGP is not itself a reason.

---

## ADR-0011

### A narrow Snackbar collaborator, not an event bus

**Accepted** · 2026-09-13

**Context.** Each screen owned its own one-shot effect channel, so a Snackbar
raised by a screen died when the user switched destinations — the download
confirmation was the visible symptom, since the download outlives the screen.

**Decision.** One app-level `SnackbarNotifier`, injected as a `@Singleton`,
collected in exactly one place (`CatsNavDisplay`) above the `NavDisplay`.

**Boundary, deliberately.** This is for transient app-level feedback that does
not belong to a screen's own state. Anything that *is* screen-specific — a
dialog, an error the screen has to sit in — stays in that screen's state. Other
app-level surfaces get their own collaborator rather than widening this one. The
failure mode being avoided is a global event bus that everything publishes to
and nothing owns.

**Alternative rejected.** Exposing it through a Hilt `@EntryPoint` so
composables could fetch it. That is a service locator wearing a DI annotation:
the dependency stops being visible in any signature. It is injected into the
activity and passed down from the composition root instead.

**Consequences.** A Snackbar survives a destination switch. It is backed by a
`Channel`, which *distributes* rather than broadcasts — every message goes to
exactly one collector, chosen non-deterministically if more than one collects —
so the single-consumer contract is load-bearing and documented on the interface.
Fanning out to several collectors would need a `SharedFlow` instead.

**Review when:** a second app-level surface appears (a dialog host, a toast) — that
gets its own collaborator, and if three of them accumulate the boundary should be
rethought rather than widened. Also if anything ever needs to collect these
messages in two places, which the `Channel` cannot serve.

---

## ADR-0012

### Hand-written fakes, no mocking library

**Accepted** · 2026-09-14

**Context.** The test suite needed substitutes for the repository, the DAO, the
API service, the downloader and the notifier.

**Decision.** Real in-memory implementations backed by `MutableStateFlow`. No
MockK, no Mockito.

**Reasoning.** The repository has *behavior*, not just calls: favoriting a cat
has to change what the feed emits, without a re-fetch. A mocked
`coEvery { repo.feed } returns flowOf(...)` returns a static list, so the test
asserts the script the author wrote rather than the behavior. Reproducing
re-emission from a mock means putting a `MutableStateFlow` inside it — a fake
with extra ceremony. A fake also implements the interface contract directly, so
a change to that interface surfaces as a compilation error in the fake;
interaction-based assertions instead verify configured calls at runtime.

**Consequences.** A fake is production-quality code that can drift from the real
thing, and one did: `FakeCatDao` accepted duplicate inserts where Room's default
`ABORT` rejects them, which hid the bug in ADR-0014. It now extends the real
`CatDao` and inherits the transaction body, so the logic under test is the
shipped one. What a JVM fake still cannot reproduce is Room's atomicity — that
needs a device, which is ADR-0018.

Where MockK would genuinely win is a port with no behavior, only "was it
called": `FakeImageDownloader` is 18 lines that one `coVerify` would replace.
That was not worth a dependency here; at a larger scale it would be.

**Review when:** the interfaces being faked grow wide enough that maintaining the
fakes costs more than the fidelity buys, or a dependency that cannot be
substituted by hand has to be tested.

---

## ADR-0013

### `launchCatching`, never bare `runCatching`

**Accepted** · 2026-09-14

**Context.** Two `onEvent` branches called suspend repository methods in a bare
`viewModelScope.launch` with no handling at all, while the branch beside them
had a full `try`/`catch`. An uncaught exception in `viewModelScope` reaches the
uncaught-exception handler and can terminate the process. A third branch used
`runCatching { }.onFailure { }`, which catches `CancellationException` too — so
leaving the screen mid-fetch was reported to the user as "couldn't load a cat".

**Decision.** One primitive, `ViewModel.launchCatching(onFailure) { }`, which
rethrows `CancellationException` and routes everything else to the caller's
handler. Every fallible event handler goes through it.

**Consequences.** The rule becomes mechanical rather than a matter of care: a
branch either uses the primitive or is visibly wrong in review. `onFailure` is
what varies — a Snackbar for a one-off action that failed while the screen is
otherwise fine, the screen's `ErrorHandler` for a failure it has to sit in.

The project's own written convention had cited
`runCatching { }.onFailure { }` as the pattern to follow — the rule recommended
the bug. Corrected in the same change; it now points at `launchCatching`
(see [`CONTRIBUTING.md`](../CONTRIBUTING.md)). The general principle is worth more than the helper: **cancellation is
not failure**. Catching `CancellationException` without rethrowing it breaks
cancellation propagation for that coroutine and its children — the coroutine
carries on as though nothing had happened.

Scope of the rule: `runCatching` is not bad in general — it is fine around work
that is not cancellable. What this project bans is a bare `runCatching` around
cancellable coroutine work, where cancellation is silently absorbed. The title
is a project convention, not a universal Kotlin rule.

---

## ADR-0014

### Atomic favorite toggle via `@Transaction`

**Accepted** · 2026-09-14

**Context.** `toggleFavorite` read `isFavorite` and then wrote, as two separate
DAO calls. Each tap launches its own coroutine, so two quick taps could both
read "not a favorite" before either wrote, and both insert — aborting on the
primary key and crashing the app. Reproduced on-device as
`SQLiteConstraintException: UNIQUE constraint failed`.

**Decision.** `CatDao` became an abstract class with a
`@Transaction toggleFavorite`, so the read and the write execute as one database
transaction and another transaction cannot interleave a conflicting write
between them. That isolation
is the property being relied on; how Room schedules concurrent transactions
underneath is an implementation detail.

**Alternative rejected.** `@Insert(onConflict = REPLACE)`. It stops the crash,
which is why it is tempting, and it leaves the actual defect in place: two taps
that should cancel each other out would still end with the cat favorited. It
hides the race rather than removing it.

**Consequences.** The DAO is an abstract class rather than an interface, which
is ordinary Room usage but slightly less familiar. The guarantee is Room's, so
the test for it has to be instrumented — 50 concurrent toggles against an
in-memory database, asserting an even count cancels out. Removing only the
`@Transaction` annotation reproduces the original crash, which is what makes the
test worth having.

---

## ADR-0015

### Dedupe a page on write, not with an in-flight guard

**Accepted** · 2026-09-14

**Context.** `fetchNextBatch` read the set of ids already in the feed *before*
the network request and filtered against it *after*. Two overlapping loads — a
double-tapped Retry, or a retry while an auto-load is in flight — each snapshot
the feed before the other writes, so both append the same cats. Duplicate ids
reach `items(cats, key = { it.id })`, and `LazyColumn` throws on a duplicate
key. Reproduced with two gated concurrent calls: `[1, 2, 1, 2]`.

**Decision.** Do the whole read-filter-write inside `MutableStateFlow.update`,
computing the existing ids from the `current` value the lambda receives:

```kotlin
fetched.update { current ->
    val existingIds = current.mapTo(hashSetOf()) { it.id }
    current + newCats.filterNot { it.id in existingIds }
}
```

**The invariant.** *Network requests may overlap; each state update is applied
atomically against the current `StateFlow` value.* `update` is a compare-and-set
loop, so the ids are read from the same value that gets written. Concurrent
`update` calls are not prevented from running — the loser of the race simply
re-runs its lambda against the new value, which is what makes the result
correct either way.

An earlier version of this fix simply moved the read after the request, leaving
`fetched.value = fetched.value + ...`. That removes the long window around the
network call, but a separate read and write is still not atomic — two callers
can both read, then both write, and the second silently drops the first one's
page. It was safe only for as long as every caller happened to resume on the
same thread, which is not something a repository method can promise. `update`
makes the guarantee structural instead of circumstantial.

**Alternative rejected.** Tracking the in-flight job and ignoring overlapping
loads. It saves the redundant request, but it makes correctness depend on the
guard being present at every future call site. Making the write itself correct
is a stronger guarantee; the guard can still be added later as an efficiency
measure, and if it is ever removed nothing breaks.

**Consequences.** `update`'s lambda re-runs on contention, so it has to stay
free of side effects — which it is, since `newCats` is computed before it.

Two shapes worth naming, because the same pair caused ADR-0014:

- **A read-modify-write that spans a suspension point is not atomic unless the
  synchronization mechanism explicitly covers the whole operation** — a `Mutex`
  held across it, a database transaction, an actor. The suspension is not what
  breaks atomicity; the absence of synchronization across it is. In Kotlin the
  suspension is also invisible at the call site, so the window is easy to miss.
- **A read-modify-write with no suspension point is not automatically atomic
  either.** It depends on the synchronization guarantees of the state being
  modified. Use the primitive that provides them — `update` here, a transaction
  in ADR-0014 — rather than relying on a dispatcher the function does not
  control.

Honest limit on the test: the regression test pins the deduplication, not the
lost update. Reproducing a lost write needs genuine parallelism, and a test that
depends on losing a race is flaky by construction. The compare-and-set is a
structural guarantee rather than a tested one.

---

## ADR-0016

### Feed errors are not retryable

**Superseded** by [ADR-0019](#adr-0019) · 2026-09-14

**Context.** `getCatFeed().onEach(...).launchIn(viewModelScope)` had nothing
catching an exception from the flow, so a throwing Room query killed the
process. Adding `.catch` fixed that, but a `Flow` that has thrown is terminated
and will not emit again.

**Decision.** Split the error handler: `onLoadFailure` (a page failed,
retryable) and `onFeedFailure` (the stream died, not retryable). The error
screen said "Please restart the app".

**Reasoning at the time.** Offering Retry would have been a dead button —
fetching another page updates a feed nothing is collecting anymore. A button
that silently does nothing is worse than no button.

**Why it was superseded.** The premise was that a terminated flow is
unrecoverable. It is not — the *subscription* can be restarted, which ADR-0019
does. The reasoning was sound given the design; the design was the thing to
change.

---

## ADR-0017

### Version-scoped destructive fallback

**Accepted** · 2026-09-14

**Context.** v1 of the database used a different table name and column spelling,
and the original app wiped it via `fallbackToDestructiveMigration()`. Replacing
that with `addMigrations(MIGRATION_2_3)` alone (ADR-0005) turned the silent wipe
into `IllegalStateException: A migration from 1 to 3 was required but not found`
— a crash on launch. Reproduced on-device.

**Decision.** `fallbackToDestructiveMigrationFrom(dropAllTables = true, 1)`.

**Reasoning.** These are not the same call. The blanket
`fallbackToDestructiveMigration()` permits Room to drop and recreate the
database whenever *any* migration path is missing, rather than failing the
upgrade — including for a migration nobody has written yet. It is permission
granted in advance, for cases not yet known. The version-scoped form names one
obsolete version, and every other missing migration still fails loudly. That
difference is the whole point.

**`dropAllTables = true`, deliberately.** v1's table was `favoriteCats`, which is
not an entity in v3, so Room would not know to drop it: the default would leave
an orphaned table sitting in the file indefinitely. Passing `true` clears
everything and rebuilds from the current schema. It is the more destructive of
the two options, chosen because the data being destroyed is v1 data this decision
has already written off — not because it is the safer default. The argument is
named at the call site rather than passed as a bare `true`, so a reader sees what
is being asked for.

**Consequences.** Database configuration moved into `catDatabaseBuilder()` so
the test exercises the builder the app ships rather than a copy that can drift.
Honest caveat recorded with it: v1 existed only between two commits on the same
day in December 2022, before any release, so it almost certainly never reached a
device. This is a safety net making an implicit decision explicit, not a
response to a known field crash.

Retiring a migration path is now a stated policy decision rather than
maintenance — age alone is not a reason ([`ARCHITECTURE.md`](ARCHITECTURE.md) §11d).

**Review when:** someone decides v2 should also be retired — which is the same
policy decision, made again, and needs the same explicit justification.

---

## ADR-0018

### Two-tier verification

**Accepted** · 2026-09-14 · **amended** by [ADR-0030](#adr-0030), [ADR-0031](#adr-0031)

**Context.** The instrumented tests only ran when someone remembered to point
Gradle at a device — and they are the only coverage of the three failures that
destroy user data: the toggle's transaction, `MIGRATION_2_3`, and the v1 upgrade
path.

**Decision.** Two tasks in the root build. `verify` (assemble + every unit test,
no device) for the inner loop; `verifyOnDevice` (`verify` +
`connectedDebugAndroidTest`) before a PR.

**Alternative rejected.** One gate including the instrumented tests. It would
fail every time no emulator happened to be running, which teaches people to skip
it — the exact failure being fixed.

**Consequences.** `verify` names both `testDebugUnitTest` and `test`: an Android
module has only the first, a pure-Kotlin module only the second, so naming one
would silently skip the other's tests the day a second module appears.

CI is the enforcement point; the local command is deliberately identical to the
one CI runs, so a failed gate can be reproduced locally without translating a
YAML step back into Gradle tasks.

`verify` is a required status check on `dev`, so the gate is enforced rather
than remembered. `verifyOnDevice` is not: the instrumented tests still depend on
someone attaching a device, which is the remaining hole and the reason the tests
that guard the data-loss paths are the least-run tests in the project.

**Review when:** the instrumented tests run in CI — via a Gradle Managed Device
or an emulator action — at which point the two tiers may collapse into one and
this decision stops being needed.

---

## ADR-0019

### Make the feed resubscribable instead

**Accepted** · 2026-09-14 · supersedes [ADR-0016](#adr-0016)

**Context.** ADR-0016 left both screens telling the user to restart the app,
which is an admission that the app cannot recover from its own error state.

**Decision.** Each ViewModel holds a trigger `StateFlow` and flat-maps the feed
over it, so a retry resubscribes. One `Retry` event covers both failure kinds:
it resubscribes *and* loads a page, so the screen keeps no record of which
failure it hit.

**The load-bearing detail.** `catch` goes on the **inner** flow, inside
`flatMapLatest`. Downstream of `flatMapLatest` it would terminate the whole
chain *including the trigger*, and Retry would be a dead button — precisely the
bug being replaced. Moving it to the naive position fails exactly one test.

**Consequences.** `retryable` came off `CatsListUiStatus.Error`: once every
error was recoverable, the flag was always `true`, and a boolean that only ever
takes one value is noise. Easy to reintroduce if a genuinely unrecoverable error
appears.

Resubscribing is safe because the source of truth is the repository, not the
subscription: the fetched cats and the favorites both outlive any collector, so
a new subscription re-emits what the old one had. That invariant is what lets a
single event handle both failure kinds, and it is pinned by a test — if the feed
ever became subscription-scoped, that test is what would fail.

---

## ADR-0020

### One serialization library

**Accepted** · 2026-09-14

**Context.** The app shipped two: Gson for Retrofit (`CatDto`, `NetworkModule`)
and kotlinx.serialization for the Navigation 3 keys. Nobody chose that — Gson
came from 2022, kotlinx.serialization arrived with ADR-0009, and they were never
reconciled.

**Decision.** Gson is gone. Retrofit uses
`retrofit2-kotlinx-serialization-converter`, and `CatDto` is `@Serializable`
with `@SerialName` in place of `@SerializedName`.

**Reasoning.** Gson resolves models reflectively, which makes the shrinker
configuration more dependent on keep rules than generated serializers are — and
R8 is next on the list. kotlinx.serialization generates its serializers at
compile time, reducing that reflective surface. Two libraries doing one job is
also two ways to spell the same thing.

**The behavioral difference that matters.** Gson silently ignores a JSON key
the model does not declare; kotlinx.serialization rejects it. Swapping one for
the other therefore changes how the app reacts to an upstream field being added:
from ignoring it to failing every response. `Json { ignoreUnknownKeys = true }`
restores the tolerant behavior deliberately rather than by default.

Worth being precise, because the first version of that comment was wrong: the
search endpoint currently returns exactly the four fields `CatDto` declares, so
nothing was broken without the setting. It is forward-compatibility for a wire
model this project does not own, not a fix for a present failure.

**Consequences.** One serialization library, no reflective model lookup in the
release build, and `kotlinx-serialization-core` was replaced by `-json`, which
includes it — so the Navigation 3 keys are unaffected. Gson is off the runtime
classpath entirely, confirmed against the resolved dependency graph rather than
assumed.

The unit tests could not have caught a failure here — they use fakes, and the
converter only runs against real JSON. Verified by installing on a device and
confirming the feed loads.

**Review when:** the wire models grow enough that polymorphic or custom
serialization is needed, or a dependency forces a different JSON library back
into the graph.

---

## ADR-0021

### Derive versionCode from the version name

**Accepted** · 2026-09-15

**Context.** The app declared `versionCode = 1` and `versionName = "1.0.2"`, and the
repository carries tags `v1.0.1` and `v1.0.2`. Both of those releases shipped
`versionCode` 1, because nobody remembered to bump a number that no developer ever
looks at. Play rejects an upload whose `versionCode` has not increased, so the
second release could not have shipped, and the 2.0.0 release could not either.

**Decision.** Declare the version once, in parts, and compute both values:

```kotlin
val versionMajor = 2
val versionMinor = 0
val versionPatch = 0

versionCode = versionMajor * 10_000 + versionMinor * 100 + versionPatch
versionName = "$versionMajor.$versionMinor.$versionPatch"
```

**Alternatives rejected.** Bumping `versionCode` by hand is the status quo, and it
already failed twice — a rule that depends on remembering is the thing being
removed. Deriving it from the git commit count or a CI build number makes the
number monotonic too, but couples the app's identity to the build environment: the
same commit built locally and on CI would produce different versions, and the value
is not reproducible from the source alone.

**Consequences.** Bumping the name necessarily bumps the code, so the class of bug
is gone rather than fixed once. Minor and patch are limited to 0-99 each, which is
wider than this project will use. The new code is 20000, comfortably above the 1
that history left behind, so nothing is blocked by the old mistake.

**Review when:** the app is published somewhere with its own versioning expectations,
or CI needs a distinct build number per build rather than per version — at which
point the build number belongs beside this scheme, not instead of it.

---

## ADR-0022

### Split the app into Gradle modules

**Accepted** · 2026-09-17 · supersedes [ADR-0001](#adr-0001)

**Context.** The app is one `:app` module of roughly 40 source files across the
presentation/domain/data packages. Three features are next: a second animal type
(dogs, behind a `GET v1/images/search` endpoint on a sibling API that returns the
same `id`/`url`/`width`/`height` shape as the one already in use), a favorites
screen unified across both animal types with filtering and sorting, and a shared
image card (shimmer while loading, a placeholder on download failure instead of
the failure covering the whole screen). All three touch the same seam: a domain
model and a card component that today live inside the cats screen's own package
and would otherwise get copy-pasted into a second one, the way the retry and
download-feedback logic already had to be de-duplicated once in this project's
history. Module boundaries turn "don't reach into the other screen's package" from
a convention into a compile error.

**Decision.** Eight modules:

```
:app                — Hilt app, MainActivity, nav graph, DI wiring only
:core:model         — domain model, plain Kotlin/JVM: no Android, Compose, or
                      Hilt dependency
:core:data          — repository, API services, Room
:core:designsystem  — theme, the shared image card, buttons
:core:ui            — UiText, launchCatching, RetryableFlow, StateOwner,
                      SnackbarNotifier
:core:testing       — MainDispatcherRule, fakes
:feature:feed       — one paginated list screen
:feature:favorites  — the unified favorites screen
```

Each feature module exposes exactly two things — its `NavKey` and one entry
`@Composable` — everything else (ViewModel, StateHolder, contract, screen
internals) stays `internal`, enforced by the compiler rather than by convention.

Four convention plugins in a `build-logic` included build: `catslist.android.library`
(the standard Android library defaults, used by everything except `:app` and
`:core:model`) and `catslist.jvm.library` (plain Kotlin, used only by
`:core:model`) as the two bases, plus `catslist.compose` and `catslist.hilt` as
additive plugins applied only by the modules that actually render Compose UI
or declare a Hilt `@Module` — `:core:data` has neither reason to carry Compose,
and applying the Compose compiler where there is no Compose runtime on the
classpath fails outright rather than just wasting a build step. `catslist.quality`
(ktlint + detekt) rides along on both base plugins, so no module is quietly
uncovered by the checks the rest of the app runs.

The domain model itself is not generalized from `Cat` to a species-agnostic
`Animal` in this change. That rename belongs to the dogs feature, not to moving
existing files into new module boundaries — doing both at once would make a
large, mechanical diff (file moves, package renames) hard to tell apart from a
small, meaningful one (the domain model actually changing shape).

**Alternatives rejected.** An `-api`/`-impl` split per feature module (the
pattern a large multi-team codebase uses to keep incremental builds fast and
enforce that one team can't reach into another's internals) has nothing on the
other side of the boundary to protect yet — no feature module is consumed by
another feature module, only by `:app`. It would double the feature module
count for a guarantee `internal` visibility already gives for free at this
scale. Splitting each feature further into its own `domain`/`data`/`presentation`
modules was also rejected: with one shared domain model and two feature
screens, that multiplies module count without a matching payoff, and repeats
the same mistake a pre-release review already found in this codebase — six
use cases that only forward to a single repository method — a layer added
because it is a known-good pattern, not because something today needs it.

**Consequences.** Eight modules instead of one; every existing file's module
location changes, but packages did not need to — `com.example.catslist.domain.model`
is still the package `Cat` lives in, just under a different module's source
root, so the only import ever needing a fix was each file's own generated `R`
class. `:core:model` is checked dependency-free by construction (the
`jvm-library` convention plugin declares no Android/Compose/Hilt dependencies
for anything using it, so adding one is a build-file change, not a silent
accretion). Gradle can skip recompiling modules whose public surface did not
change, so a `:feature:favorites`-only edit no longer triggers a
`:feature:feed` recompile. The migration landed as one branch of individually
reviewable commits — convention plugins and `:core:model` first, then one
module's worth of code at a time — rather than one sweeping change, and each
commit was verified on-device, not just compiled: it caught two
`@Serializable`-without-the-compiler-plugin regressions that would otherwise
have shipped a crash on first launch.

**Review when:** a feature module needs to be consumed by another feature
module rather than only by `:app` — that is the trigger to reconsider the
`-api`/`-impl` split, not size or file count on their own. Or when `:core:data`
grows enough (e.g. a download manager needing its own persistence) that
"everything data-related in one module" stops being one responsibility —
split by what it does then, not ahead of time.

---

## ADR-0023

### Move the feed to Paging 3, with a RemoteMediator caching pages into Room

**Superseded** by [ADR-0025](#adr-0025) · 2026-09-20 · **amended** by [ADR-0024](#adr-0024)

**Context.** The feed was a hand-rolled `MutableStateFlow<List<Cat>>` the
ViewModel appended to on `fetchNextBatch()`, combined against Room's favorites
so toggling one updated the feed live (see [ADR-0015](#adr-0015) for the
duplicate-key crash that shape of code already caused once). Nothing about
that feed was persisted — a process death or a rotation past what
`rememberSaveable` covers re-fetched from page 1. Switching to Paging 3 was
requested directly, to have the same infrastructure a production app would use
for a list that can grow into the thousands, and to pick up scroll-driven
loading and load-state handling instead of the screen's own
`derivedStateOf`-on-scroll-position trigger.

**Decision.** A `RemoteMediator<Int, FeedCatEntity>` (`CatFeedRemoteMediator`)
fetches pages from `CatApiService` and caches them into a new Room table,
`feedCatsTable`, keyed by fetch order rather than by `id` — the API hands back
cats in no order a `SELECT` could otherwise recover. `CatFeedDao.pagingSource()`
reads only `feedCatsTable`; favorite status is *not* part of that query. It is
attached afterward, in `CatRepositoryImpl.feed`, by `combine()`-ing the paged
flow with a separate `Flow<Set<String>>` of favorite ids and re-mapping each
already-loaded item — the same live-update outcome the old hand-rolled
`combine()` version had, kept as a `combine()`, not moved into SQL.

An `EXISTS`-against-`favoriteCatsTable` join on `pagingSource()` was the first
version of this and reached this branch before being caught: Room's
invalidation tracker watches every table a `@Query` reads, so a favorite
toggle invalidated the query, which handed Paging a new `PagingSource`
generation, which made Paging re-run the `RemoteMediator`'s REFRESH — wiping
the entire cached feed back to page 0 on every favorite toggle, discovered by
scrolling down, favoriting a cat, and watching the list jump to the top. Fixed
by moving the favorite overlay out of the query entirely; see
`CatFeedDaoTest.pagingSource_isNotInvalidatedByAFavoriteToggle` for the
regression test — removed along with the Room-cached feed by
[ADR-0025](#adr-0025), so the link is deliberately not live.

The feed only ever appends. TheCatAPI's search endpoint has no signal for
"cats newer than what I already have," so `LoadType.PREPEND` is always a
no-op and `LoadType.REFRESH` always restarts from page 0. A second table,
`feedRemoteKeysTable`, holds a single row recording the next page to fetch —
not the per-item remote-keys table the Paging 3 samples use, which exists to
support prepending, something this feed never does.

**Alternatives rejected.** A `PagingSource` reading straight from the network,
with no Room cache, was the simpler option and was raised explicitly as the
alternative to a `RemoteMediator`. It was rejected because Paging caches
loaded pages internally and does not re-run a plain network `PagingSource` on
an unrelated write, so the favorite icon in an already-loaded page would not
update until the next full reload — a real behavior regression from what the
app already did, not a neutral simplification. (The join-based favorite
lookup above was a second, different way of chasing that same live-update
requirement, and turned out to have its own regression instead.)

**Consequences.** Two new Room entities, a fourth schema version
(`MIGRATION_3_4`), and a new `RemoteMediator`. The screen's own state machine
(`CatsListState`, `CatsListUiStatus`, the `LoadMore`/`Retry` events,
`CatsListStateHolder`, `CatsListErrorHandler`) is gone outright rather than
adapted — `LazyPagingItems.loadState`, collected in the Composable via
`collectAsLazyPagingItems()`, already tracks initial-load, append and error
state, and reimplementing that inside `CatsListViewModel` would just be
duplicating what Paging already owns. This is a real, deliberate narrowing of
this codebase's own "one sealed `UiStatus`, ViewModel merely orchestrates"
rule for this one screen: loading/error/retry for the paged list now lives in
the Composable, not the ViewModel, because `LazyPagingItems` is fundamentally
a Compose-collected type that cannot be constructed inside a ViewModel.
`CatsListViewModel` still owns everything that *is* still its job — favoriting
and downloading.

Running the new instrumented tests surfaced an unrelated, pre-existing bug:
`:core:data` never had `testInstrumentationRunner` configured after its
`androidTest` sources moved out of `:app` during [ADR-0022](#adr-0022)'s
module split, so `connectedDebugAndroidTest` silently discovered zero tests
and reported success. `verifyOnDevice` had not actually run an instrumented
test since the module split landed. Fixed in the convention plugin so every
module gets it, not just `:core:data`.

**Review when:** the feed needs to support prepending (e.g. a "jump to
newest" action) — the single global remote key stops being enough and this
needs the standard per-item remote-keys table instead.

**Why it was amended.** The Paging decision above stands unchanged. What was
drawn too wide is the sentence deleting "the screen's own state machine": only
*load* state was ever Paging's to own, and the clause was written as though it
covered all of the screen's state. It did not. The favorite overlay was left
outside any state object as a bare `StateFlow<Set<String>>` on the ViewModel,
and its failure path went missing with the error handler it was bundled into.
[ADR-0024](#adr-0024) redraws the line.

**Why it was superseded.** Paging 3 itself carries forward untouched — what
[ADR-0025](#adr-0025) reverses is the other half of this entry, the
`RemoteMediator` caching pages into Room. Two things undid it. The rejection of
a network-only `PagingSource` above rested entirely on the favorite icon going
stale in already-loaded pages, and [ADR-0024](#adr-0024) moved that overlay out
of the paging query into the UI layer, so the objection no longer applies to
any code that exists. And the cache turned out to have a cost this entry did
not anticipate: the cached cats render at launch and are then replaced by the
mandatory refresh, which reads as the screen loading twice. Keeping a copy of
the feed on disk was never a requirement — it was the price of a live favorite
star, and that price stopped being owed.


## ADR-0024

### Paging owns the feed's *load* state, not its whole state

**Accepted** · 2026-09-19 · amends [ADR-0023](#adr-0023)

**Context.** [ADR-0023](#adr-0023) removed `CatsListState`,
`CatsListStateHolder` and `CatsListErrorHandler` together, on the reasoning
that `LazyPagingItems.loadState` already tracks loading, error and retry. That
reasoning is correct and still holds — for loading, error and retry. It does
not extend to the rest of the screen, and bundling the removals together took
two things with it that Paging never replaced:

- **The favorite overlay had no state object.** It lived as a public
  `StateFlow<Set<String>>` beside `pagedCats`, which is the "state scattered
  outside one State object" shape [`ARCHITECTURE.md`](ARCHITECTURE.md) §3a
  exists to forbid — the feed was the only screen not following it.
- **The favorites stream had no failure path.** A plain `.map {}.stateIn(…)`
  with no `catch`: a throwing Room query would escape `viewModelScope` and
  reach the default handler, which on Android kills the process. This is the
  exact failure [ADR-0013](#adr-0013) introduced `launchCatching` for, and the
  favorites screen already guards with `RetryableFlow`. The feed lost that
  guard along with the error handler it had been attached to.

**Decision.** The carve-out is narrowed to what the library actually forces.
`PagingData` stays outside the state object, because `LazyPagingItems` is built
by the Composable collecting it and cannot be constructed in a ViewModel —
that constraint is real and is the whole of it. Everything else on the screen
follows §3 like any other: `CatsListState` holds the favorite ids and a
`CatsListFavoritesStatus`, `CatsListStateHolder` owns the mutation, and
`CatsListErrorHandler` maps a dead favorites stream into
`CatsListFavoritesStatus.Unavailable`.

`CatsListUiStatus` stays deleted, and so do the `LoadMore` and `Retry` events.
Those *were* duplicating Paging, which is the part of ADR-0023 that was right.

**Alternatives rejected.** Mapping `loadState` into a screen-wide `UiStatus`
so the feed looks like every other screen — rejected for ADR-0023's original
reason, unchanged: it is a second copy of a state machine Paging already runs,
kept in sync by hand, and the uniformity it buys is cosmetic.

Reporting the stream failure with a Snackbar instead of putting it in state was
the cheaper fix and was rejected as dishonest about duration.
[`LaunchCatching`](../core/ui/src/main/kotlin/com/example/catslist/presentation/LaunchCatching.kt)'s
own contract draws the line: a one-off action that failed while the screen is
fine gets a Snackbar, a condition the screen has to *stay* in belongs in state.
`catch` terminates the flow, so the overlay is dead for that ViewModel's whole
life — a transient Snackbar for a permanent condition, on a screen the user
keeps scrolling, would be gone long before it stopped being true.

**Consequences.** The feed keeps rendering when favorites break, with an inline
notice above the cats and the last known stars left as they were, rather than
blanking working content or silently freezing. The status is a two-case sealed
type scoped to the overlay, deliberately not named `UiStatus` — a reader who
greps for that name on this screen should find nothing, because the screen-wide
one genuinely does not exist here. The cost is that this one screen now reads
its state from two sources, `state` and `pagingItems`, which no other screen
does; the ViewModel is also at §3b's ≈7 constructor-dependency cap.

**Review when:** a second paged screen appears. One screen shaped like this is
a documented exception; two means the contract in §3 should describe paged
screens directly instead of carving them out.



## ADR-0025

### Page the feed from the network; persist only favorites

**Accepted** · 2026-09-20 · supersedes [ADR-0023](#adr-0023)

**Context.** [ADR-0023](#adr-0023) cached the feed into Room behind a
`RemoteMediator`, for one reason: a plain network `PagingSource` would leave
the favorite star stale on pages already loaded. [ADR-0024](#adr-0024) then
moved the favorite overlay out of the paging query entirely — the screen
applies it at render time from a separate flow — which left the cache with no
argument for its existence.

It was also costing something. Paging refreshes on every cold start, so the
cached cats render first and are replaced moments later by the ones just
fetched: the screen appears to load twice, and the second load is the app
discarding work it had just shown. Tuning `initialLoadSize` and
`prefetchDistance` removed a redundant *request*, but nothing in the paging
configuration can stop a cache from being displayed before the refresh that
replaces it.

**Decision.** `CatFeedPagingSource` calls `CatApiService` directly and holds
its pages in memory, for the lifetime of one Paging generation. Room keeps
`favoriteCatsTable` and nothing else. `MIGRATION_4_5` drops `feedCatsTable` and
`feedRemoteKeysTable`; no user data is lost, because none of what they held was
the user's. Paging 3 itself is unchanged — this replaces where the pages come
from, not how they are paged.

**Consequences.** A launch is one request, a skeleton, then cats. There is no
second load, because there is nothing cached to show first. The feed is also
gone on relaunch, which is the intended reading of "the feed is what the
network says right now": a cat seen yesterday was never promised to still be
there, and anything the user wanted to keep is a favorite.

The cache was doing one job nobody had written down: `feedCatsTable`'s primary
key deduplicated cats across pages. TheCatAPI repeats them, the list keys its
items by id, and a repeated key is a crash rather than a visible double — the
failure [ADR-0013](#adr-0013) and [ADR-0015](#adr-0015) both circle. The
mediator's `distinctBy` only ever covered a single response, so that protection
was entirely incidental. `CatFeedPagingSource` now carries an explicit
seen-id set per generation, which is the same guarantee stated out loud.

Offline is worse, and that is accepted rather than overlooked: with nothing on
disk there is nothing to show without a network, where the cache would have
offered the previous session's cats. The alternative — keep the cache and
refresh only when it is older than some window — needs a timestamp column and
another schema version to answer a question this app does not have: a feed of
random cats has no staleness, only novelty.

**Review when:** the feed gains an identity worth returning to — a search, a
filter, a breed — at which point the same cats on relaunch stops being noise
and starts being state, and something has to persist it again.

---

## ADR-0026

### Koin for dependency injection

**Accepted** · 2026-09-21 · supersedes [ADR-0004](#adr-0004)

**Context.** This repository is a fork of the Android app, taken at v2.2.0, whose
purpose is to become a Kotlin Multiplatform project. Hilt is the one piece of
the stack with no multiplatform story at all: it is a Dagger-based, JVM-only
annotation processor bound to Android's component hierarchy, and no amount of
source-set arrangement puts `@HiltViewModel` into `commonMain`.

The swap is recorded as its own decision, and done first, because it is the
only migration step that is worth making on its own terms. Retrofit, Room and
Compose all have multiplatform successors that the KMP work will reach for
anyway; DI had to be chosen.

**Decision.** Koin 4.1, with each Gradle module owning a `Module` value —
`dataModule`, `uiModule`, `feedModule`, `favoritesModule` — assembled by
`App.startKoin`. `single` where the binding was `@Singleton`, `factory` where it
was Hilt's unscoped default.

`@ViewModelScoped` has no direct equivalent, and does not need one. Hilt used it
so that a ViewModel and its ErrorHandler resolved to the *same* StateHolder; in
Koin the StateHolder is constructed inside the `viewModel { }` lambda and handed
to both, which is the same guarantee written as an assignment rather than
inferred from a scope annotation.

**Consequences.** The graph is no longer verified at compile time. This is the
real cost, and it is a genuine loss: a definition that drifts from its
constructor is now a crash when the screen opens rather than a build failure.
`FeedModuleTest` and `FavoritesModuleTest` buy most of it back — they build the
real feature module against the `:core:testing` fakes and resolve the ViewModel,
so drift fails a unit test instead of a user's launch. They are not a full
substitute: they cover the feature graphs, while `dataModule` needs a `Context`
and a real database and so is only exercised on a device.

What is gained beyond portability: KSP leaves the DI path entirely, and with it
the generated-code stack traces that ADR-0004 listed as Hilt's price. One
consequence worth naming — `:core:data` applied `catslist.hilt` partly to get
the KSP plugin that Room's compiler reused. `catslist.koin` brings no KSP, so
`:core:data` now applies it directly. The coupling was always accidental; it is
just visible now.

**Alternative rejected.** Koin Annotations (`@Single`, `@Factory`, KSP-generated
modules), which would keep an annotation-driven style and restore some
compile-time checking. Rejected because it puts KSP back in the DI path to
recover a fraction of what Dagger gave, and the hand-written modules are short
enough — four of them, none over thirty lines — that the DSL is not the part
that needed help.

**Review when:** the app has enough screens that `App.startKoin`'s module list
becomes a thing people forget to update, at which point module aggregation
needs to move somewhere that fails loudly.

---

## ADR-0027

### Ktor for HTTP, replacing Retrofit

**Accepted** · 2026-09-21

**Context.** Retrofit is JVM-only. It builds its implementation with
`java.lang.reflect.Proxy` over an annotated interface, which has no counterpart
on Kotlin/Native, so `CatApiService` as written could never move to
`commonMain`. Like Hilt in [ADR-0026](#adr-0026), it is a dependency the
multiplatform work has to replace rather than rearrange.

**Decision.** Ktor 3.6 with the OkHttp engine. `CatApiService` survives as a
plain `suspend fun` interface — it is what `FakeCatApiService` implements and
what `CatFeedPagingSource` is tested against — and `KtorCatApiService` becomes
its one real implementation.

The OkHttp engine specifically, not CIO: OkHttp is already pinned here because
Coil brings its own ([ADR-0006](#adr-0006)), and using it for both keeps one
HTTP stack in the app rather than two. It is also JVM-and-Android only, so the
engine is the piece that becomes `expect`/`actual` when this module moves to
`commonMain`; the client configuration around it does not.

**Consequences.** The request is now built by hand where Retrofit derived it
from annotations, which moves a class of mistake from compile time to runtime:
a wrong path or a mistyped query parameter used to be impossible, and is now
merely untested. `KtorCatApiServiceTest` is the answer — `catHttpClient` takes
its engine as a parameter so the test drives the *real* client configuration
against `MockEngine`, asserting the path, both paging parameters, and that an
unknown field in the response is still tolerated.

Retrofit's converter is gone too, so the `Json` instance is configured once on
the client rather than wrapped in a `Converter.Factory`. That is a small
simplification and the reason `CAT_API_BASE_URL` gained a trailing slash: Ktor
resolves a request path relative to the default URL, where Retrofit normalized
the base itself.

**Alternative rejected.** Ktorfit, which keeps the annotated-interface style on
top of Ktor via KSP. Rejected for the same reason as Koin Annotations in
ADR-0026 — it reintroduces code generation to preserve a syntax, and this API
surface is a single endpoint with two query parameters. There is not enough
here for the generator to earn its place in the build.

---

## ADR-0028

### Migrate to KMP module by module, from the bottom

**Accepted** · 2026-09-21

**Context.** This repository was forked from `CatsListApplication` at v2.2.0 to
become a Kotlin Multiplatform project. The question was not *whether* the code
ports — most of it is coroutines, Flow and plain Kotlin — but in what order, and
what "done" means at each step.

The tempting shape is one large change that stands up `commonMain`,
`androidMain` and an `iosApp/` at once. That produces a tree that does not build
for days and a single commit nobody can review.

**Decision.** One module at a time, lowest in the dependency graph first, with
the Android app building and `./gradlew verify` green at every commit. The order
follows dependencies, not enthusiasm:

1. Replace the dependencies with no multiplatform story at all, while everything
   is still Android — Hilt ([ADR-0026](#adr-0026)), then Retrofit
   ([ADR-0027](#adr-0027)). These are the changes that touch the most files, and
   they are much easier to review against an otherwise unchanged app.
2. `:core:model`, then `:core:domain` — pure Kotlin, so the port is a source-set
   move plus a convention plugin.
3. `:core:data` — Room, the HTTP engine, and `DownloadManager` all need
   `expect`/`actual`. Not yet done.
4. The UI, via Compose Multiplatform. Not yet done. Checked rather than
   assumed, though: Compose Multiplatform 1.12.0 compiles a `commonMain`
   Composable for the JVM against this project's Kotlin 2.3.21, and every
   library the UI depends on — `navigation3-runtime`, `navigation3-ui`,
   `paging-compose`, `lifecycle-viewmodel-compose`,
   `lifecycle-viewmodel-navigation3` — already publishes `common`, `jvm` and
   `native` variants. The obstacle is size, not feasibility: ~3,300 lines across
   four modules, 50 `R.string` lookups to move to Compose resources, the Compose
   UI test rules, and a desktop entry point.

`:core:domain` is a new module, split out of `:core:data`. The use cases,
`CatRepository` and `ImageDownloader` were always platform-free but sat in a
module that also owned Room and the API client, so they could not move without
it. Extracting the ports is what ADR-0022's layering already implied; KMP is
what finally forced it.

**Targets: `jvm()` only, for now.** The desktop target is the one non-Android
platform this build can compile and test on any host, so it is the one that gets
declared. iOS is not declared — a target that is configured but never built is a
claim the build cannot back up, and adding it needs a macOS machine in CI before
it means anything.

**Consequences.** Android consumers resolve the `jvm` variant of the
multiplatform modules through Kotlin's platform compatibility rules, so nothing
downstream changed when `:core:model` and `:core:domain` moved. That is what
makes the incremental order possible at all.

The cost is a tree that is *partly* multiplatform for a while, which is a real
state to be in and not a comfortable one: `:core:domain` is `commonMain` while
`:core:data` right below it is Android-only, so the ports are portable and
nothing that implements them is. Until step 3 lands, "multiplatform" describes
the build, not yet the app.

One deferred piece worth naming: the use-case tests still live in
`:core:data/src/test`, not with the code they exercise. They depend on
`:core:testing`'s fakes, and `:core:testing` is an Android library, so the tests
cannot follow the use cases into `commonTest` until it moves too. They still run
and still cover the domain; they are just in the wrong module.

**Review when:** `:core:data` reaches `commonMain`. At that point the desktop
target has a real data layer behind it, and whether the UI follows via Compose
Multiplatform stops being hypothetical.

---

## ADR-0029

### `:core:data` on Room KMP, with a real desktop target

**Accepted** · 2026-09-21

**Context.** Step 3 of [ADR-0028](#adr-0028). `:core:data` is where the platform
actually shows up: Room, an HTTP engine, and `DownloadManager` are three
different kinds of "this only exists on Android", and each needed a different
answer.

**Decision.** The module moves to `commonMain` with `androidTarget` and `jvm`,
and splits on exactly three seams:

- **The database file.** Room 2.8 is multiplatform, so the entity, DAO,
  `@Database` and all three migrations are common. Only *where the file lives*
  differs, so `withCatDatabaseDefaults()` holds the shared configuration and each
  platform supplies its own `catDatabaseBuilder`. Android uses
  `getDatabasePath()`; desktop uses `~/.catslist`.
- **The HTTP engine.** OkHttp runs on both targets, so the engine is not really
  a platform difference — only its construction is. `catHttpClient` already took
  its engine as a parameter ([ADR-0027](#adr-0027)), so the split is one Koin
  binding per platform and nothing else.
- **Downloading an image.** This one is a genuine difference.
  `DownloadManager` has no desktop equivalent, so `DesktopImageDownloader`
  fetches the bytes with the same Ktor client and writes them to `~/Downloads`.

DI splits the same way: `dataModule` is common and `includes(platformDataModule)`,
an `expect val` whose `actual` supplies the three answers above.

**Consequences.** The desktop target is not a configuration claim — it is tested.
`CatDatabaseJvmTest` builds the real database through the shipped
`withCatDatabaseDefaults()` and exercises the favorites round-trip on the JVM,
with no Android on the classpath. If Room's KMP codegen, the bundled SQLite
driver or the migration set were wrong for that target, nothing else in the build
would catch it.

Three things had to change that were not about Room at all:

- **`com.android.library` cannot be applied with the multiplatform plugin as of
  AGP 9.** The replacement, `com.android.kotlin.multiplatform.library`, renames
  the source sets: `androidMain`, `androidHostTest`, `androidDeviceTest` — not
  `main`, `test`, `androidTest`. It also leaves Android resources *off* by
  default, which `CatImageDownloader`'s `R.string` lookups need switched back on.
- **`:core:data` was getting KSP from the Hilt convention plugin** and now
  applies it directly — already true since ADR-0026, but the per-target
  `kspAndroid`/`kspJvm` wiring is new: Room's processor runs once per target.
- **detekt and ktlint both needed teaching.** detekt's default test exclusions
  predate AGP's multiplatform source-set names, and ktlint's generated-source
  filter compared `File.path` against `"/build/"` — which on Windows never
  matched, because that path uses backslashes. The filter had been silently
  inert; Room's KSP output in a multiplatform source set is simply the first
  thing that made it visible.

`CatFeedPagingSource` traded `ConcurrentHashMap.newKeySet()` for a `MutableSet`
behind a `Mutex`. The guarantee is unchanged — Paging can still have a refresh
and an append in flight at once — but `java.util.concurrent` is not a thing on
every target.

**What this does *not* claim.** The Android app compiles and its unit tests pass,
but nothing here has been run on a device or an emulator; the instrumented tests
are compiled, not executed, exactly as before ([ADR-0018](#adr-0018)). And there
is still no desktop *application* — the data layer runs on the JVM, the UI does
not, because Compose Multiplatform is step 4.

**Review when:** the UI moves. At that point `jvm()` stops being a target that
only tests exercise and becomes something a person can actually open.

---

## ADR-0030

### One shared, configured `OkHttpClient`

**Accepted** · 2026-09-21 · ported from `CatsListApplication`'s ADR-0026, adapted for Koin and Ktor

**Context.** [ADR-0006](#adr-0006) left this open and the README listed it under
**Known gaps**: the Ktor client was handed no `OkHttpClient` and `:app` built no
`ImageLoader`, so each library fell back to its own default. Two clients meant
two connection pools, two thread pools and two sets of timeouts — over a single
host, `api.thecatapi.com`, that the app talks to constantly. The timeouts were
the sharper half: OkHttp defaults `callTimeout` to 0, so no request had an
end-to-end cap at all. A call that kept almost-progressing could hang behind the
feed's spinner indefinitely, with nothing to report and nothing to retry.

**Decision.** `catOkHttpClient()`, in `:core:data`'s `commonMain`, builds one
`OkHttpClient` with a 15s call, connect and read timeout. Each platform's Koin
module registers it as a `single` and hands it to Ktor's OkHttp engine via
`OkHttp.create { preconfigured = get() }`. `App` implements
`SingletonImageLoader.Factory` and registers `OkHttpNetworkFetcherFactory` over
the same client, resolved with Koin's `get<OkHttpClient>()`.

**Consequences.** One connection pool, so an image request reuses the TLS
connection the feed's JSON request just warmed. One place to add an interceptor
or change a timeout. Every request now fails within a bounded time, so a
hung call becomes a failure the screen can report instead of a spinner with
nothing behind it.

No `dagger.Lazy` wrapper needed: Coil calls `newImageLoader` on first image
load, well after `onCreate`, so the client is already built on whichever
thread that turns out to be — the same guarantee `Lazy` gave in the original,
for free, because Koin's `get()` inside that function is itself the deferred
step.

The registration is explicit rather than left to `coil-network-okhttp`'s
`ServiceLoader`, which would build an `OkHttpClient()` of its own.
`RealImageLoader` assembles the builder's components ahead of the
ServiceLoader's, so the explicit factory is matched first and that default is
never constructed.

**Alternatives rejected.** Building the `ImageLoader` in `:core:designsystem`,
where `AsyncImage` lives: that module applies neither Koin nor `:core:data`, so
it cannot reach the client. Wiring one singleton across two libraries is
composition-root work, and `:app` is the composition root.

---

## ADR-0031

### Connectivity as a `Flow`, not a pre-flight check

**Accepted** · 2026-09-21 · ported from `CatsListApplication`'s ADR-0027

**Context.** The app had no idea whether the device was online. Nothing asked,
and `ACCESS_NETWORK_STATE` was not even requested. Every transport failure
therefore looked the same from the inside: a `UnknownHostException` is what you
get with the radio off *and* what you get when the host's DNS is down, and
without a second source of truth there is no way to tell which sentence to put
on screen.

**Decision.** A `NetworkMonitor` port in `:core:domain`, exposing
`isOnline: Flow<Boolean>`. `ConnectivityNetworkMonitor`, in `:core:data`'s
`androidMain`, implements it over `ConnectivityManager.registerNetworkCallback`.
The permission is declared in that source set's own manifest and merges up.

**Consequences.** The one thing it is for: a failure can now be classified as
"you are offline" only when that is actually true. Everything else stays
"couldn't reach the server", which is the difference between sending a connected
user to check a connection that is not broken and telling them what happened.

The stream is keyed on `NET_CAPABILITY_VALIDATED`, not on `onAvailable`.
`onAvailable` fires as soon as a network attaches, before anything has confirmed
it carries traffic — the state a captive-portal Wi-Fi never gets past, and where
requests fail while the device looks connected. Validated networks are tracked
as a **set**: Wi-Fi and cellular can be validated at once, and losing one of them
is not going offline.

The current state is seeded by hand on collection, because the callback only
reports changes from the moment it registers. Without that a collector on a
steady connection would wait forever for its first value.

**Desktop has no `ConnectivityManager`.** `DesktopNetworkMonitor`, in `jvmMain`,
always reports online — a real check (`java.net.NetworkInterface`, or platform
reachability) is future work, not something this port took on (ADR-0036).

**Alternatives rejected.** `suspend fun hasInternetConnection(): Boolean`, called
before each request — the shape this was modeled on, and the tempting one
because it reads as a guard. It answers only "should I try?", and it answers it
about an instant that has already passed by the time the request goes out: a
device can pass the check and lose the network mid-flight, which is precisely
the case that needs the good error message. A `Flow` answers that question too,
and also "did it come back?", which is the half a boolean cannot express at all
— it is what would let a screen recover on its own rather than waiting to be
tapped.

Reporting offline when `ConnectivityManager` is unavailable. The monitor sends
`true` instead: refusing to try on a device that may well be online fails a
request that would have worked, and the request itself is the better judge.

---

## ADR-0032

### Classify failures once, in `data`, as `AppError`

**Accepted** · 2026-09-21 · ported from `CatsListApplication`'s ADR-0028, adapted for Ktor

**Context.** [`ARCHITECTURE.md`](ARCHITECTURE.md) §3c has said since ADR-0003 that
an error handler branches on "a sealed error type from the data layer, never on
raw exception classes in the ViewModel". No such type existed. Both error
handlers took a `Throwable` and *ignored the parameter*:

    override fun onFavoriteIdsFailure(error: Throwable) {
        stateHolder.showFavoritesUnavailable()
    }

The cost was on screen. A failed feed load rendered one string —
"Couldn't load a cat. Check your connection and try again." — for every cause:
an unresolvable host, a 429 from TheCatAPI's anonymous rate limit, a 503, a
socket timeout, a response the wire model no longer parses. Two of those five
tell a connected user to go and fix a connection that is not broken, and the
rate-limited one, the most common of them in practice, hides the only advice
that would have worked: wait a moment.

**Decision.** A sealed `AppError` in `:core:model`, with a single `ErrorMapper`
in `:core:data` that is the only code in the app that knows what a
`SocketTimeoutException` or an HTTP 429 means. Everything crossing out of `data`
is classified: `CatRepositoryImpl` wraps its writes and its favorites stream,
`CatFeedPagingSource` puts one in `LoadResult.Error`. `presentation` unwraps
with `Throwable.asAppError()` and renders through a shared `AppError.toUiText()`,
which a screen overrides per case when it can say something better.

**Consequences.** Nine distinguishable messages where there was one. The mapping
is unit-tested per branch, including the two that need it most: the same
`UnknownHostException` is `NoConnection` offline and `Unreachable` online.

`ErrorMapper.map` is `suspend`, which is the price of that distinction — telling
those two apart means asking [ADR-0031](#adr-0031)'s `NetworkMonitor`, and asking
it *at the moment of failure* rather than before the request, when the answer
would have been a guess about the future.

`FakeCatRepository` now throws `AppErrorException` too, and its error fields
changed from `Throwable?` to `AppError?`. That is the point rather than a cost:
a fake that threw a bare `IOException` let a ViewModel pass a test it would fail
against the real repository.

**The throwable is not carried.** `AppError` holds a classification and, for HTTP,
a status code — not the exception. It is logged in `ErrorMapper`, the one place
with the full stack trace and the context to say what it was doing. Downstream,
nothing can act on a `SocketTimeoutException` that it cannot act on with
`Timeout`. Leaving it out also makes these compare by value, so a test asserts
`AppError.Server(503)` instead of reaching into an exception it had to construct
to get a value it can match.

**`AppErrorException` is a carrier, not a decision.** `PagingSource.LoadResult.Error`
and a `Flow`'s failure channel both insist on a `Throwable`, so one wraps the
`AppError` across those two boundaries. It is thrown only by `data` and unwrapped
only by `Throwable.asAppError()`, which is the single `as?` this design costs.

**Ported for Ktor.** The mapping source is different from the original —
`retrofit2.HttpException` becomes Ktor's `ClientRequestException`/
`ServerResponseException`, which only throw because `catHttpClient` sets
`expectSuccess = true` — but the classification each produces is unchanged, and
`ErrorMapperTest` covers both the connectivity-dependent cases and the HTTP-code
ranges against the real Ktor exception types (ADR-0036).

**Alternatives rejected.** Returning `Result<T, AppError>` from the repository
instead of throwing. It is the better shape in the abstract, and it does not fit
what is actually here: the two failing paths are a `Flow` that Room terminates
by throwing and a `PagingSource` that Paging requires to report a `Throwable`.
Neither returns a value that a `Result` could wrap, so the type would have been
carried by two `suspend` write methods and nothing else. Worth revisiting when
there is a call that genuinely returns a value that can fail.

---

## ADR-0033

### Measure recomposition, then fix stability at the source

**Accepted** · 2026-09-21 · ported from `CatsListApplication`'s ADR-0029

**Context.** Strong skipping has been on by default since Kotlin 2.0.2x, which
retired most of the `@Stable` annotation habit — an unstable parameter now costs
an identity comparison rather than an unconditional recomposition, and lambdas
are remembered automatically. What it did not retire is the two cases the
compiler genuinely cannot infer. Nobody here had looked, and "it is probably
fine" is not something this repo has a way to check.

**Decision.** Turn on the Compose compiler's own metrics and stability reports
behind `-Pcatslist.composeMetrics`, read them, and fix what they actually said.

**What they said.** Three findings, none of them guesses:

- `CatItem(unstable cat: Cat)`. `Cat` lives in `:core:model`, a pure-Kotlin
  module with no Compose compiler on it, so it carries no stability metadata and
  is assumed unstable. The feed hands every card a fresh instance each pass —
  `cat.copy(isFavorite = ...)`, the render-time overlay of [ADR-0024](#adr-0024)
  — and an unstable parameter is compared by **identity**, so no card in the
  list could ever skip. The `copy` is harmless against a stable type, whose
  comparison is `equals`; against an unstable one it defeats skipping entirely.
- `ErrorMessage(unstable message: UiText)` and `EmptyMessage` likewise.
  [`ARCHITECTURE.md`](ARCHITECTURE.md) §8 has specified `@Immutable sealed
  interface UiText` since ADR-0003; the code never had the annotation.
- `UiText.Resource` was *itself* inferred unstable — `args: List<Any>`, a raw
  interface that could be a `MutableList`. So the missing annotation would have
  been a lie as well as missing.

**The fixes, in the place each belongs.** `Cat` and `androidx.paging.LoadState`
are declared in `config/compose-stability.conf`, which every Compose module
points at — the mechanism that exists for classes you cannot annotate, whether
because the module has no Compose compiler or because you do not own the code.
`UiText` gained the `@Immutable` its spec already required, and `args` became an
`ImmutableList`, which is what makes that annotation true rather than merely
present.

**Consequences.** Every parameter across `:core:designsystem`, `:feature:feed`
and `:feature:favorites` is now stable, with two exceptions that should stay
that way: the `viewModel` on each screen's private entry overload. A ViewModel
is genuinely unstable, that composable is called once per screen with the
instance `koinViewModel()` returns, and skipping it is not a thing anyone wants.
`:feature:feed` went from 12 known-unstable arguments to 0 that matter.

Metrics stay **off** by default. They are diagnostic output, and generating them
on every build costs time an ordinary build gets nothing back for. The stability
config is always on, because unlike the reports it changes what the compiler
generates.

**No automated guard.** Re-running the flag and reading the report is a manual
step; nothing fails the build if a future change makes a parameter unstable
again. A recomposition-count test would catch it, but it needs a device and
would therefore sit behind `verifyOnDevice` ([ADR-0018](#adr-0018)) rather than
the gate every PR runs. Recorded as a known limit rather than papered over.

**Alternatives rejected.** Annotating more types by hand. It does not reach
either of the two real cases: a class in a module without the Compose compiler
cannot be annotated usefully from outside it, and `androidx.paging.LoadState` is
not ours to annotate at all.

Moving `Cat` out of `:core:model` into a module that applies the Compose
compiler. That trades a two-line config entry for putting Compose on the
classpath of the one module [ADR-0022](#adr-0022) deliberately keeps free of it.

---

## ADR-0034

### Run CI on every pull request; `verify` was never enforced

**Accepted** · 2026-09-21 · **amends** [ADR-0018](#adr-0018) · ported from `CatsListApplication`'s ADR-0030

**Context.** Four stacked pull requests were opened, each based on the branch
below it because each depended on the one before. One of them ran CI. The other
three reported no checks at all, and nothing said why. This happened in
`CatsListApplication`; this repo inherits the fix and the CI workflow it changed.

The cause is that `pull_request`'s `branches:` filter matches the **base**
branch, not the head:

    on:
      pull_request:
        branches: [dev, master]

A pull request based on `dev` matched. One based on `tech/network-monitor-flow`
matched nothing, so no workflow ran, so it sat with no checks — which looks
exactly like a queue that has not started yet. The filter reads as an economy
and behaves as a hole, and the hole opens precisely when a change was large
enough to be worth splitting up.

Checking whether the eventual merge into `dev` would catch these anyway turned
up the second half of this entry. [ADR-0018](#adr-0018) states:

> `verify` is a required status check on `dev`, so the gate is enforced rather
> than remembered.

**That was never true.** Neither `dev` nor `master` has ever had branch
protection — both report `protected: false`, and the repository's only two
rulesets are auto-imported tag protections for `v1.0.1` and `v1.0.2`. No rule
references `verify` anywhere. CI runs, CI reports, and a red pull request can be
merged.

**Decision.** Drop the `branches:` filter from the `pull_request` trigger, so
every pull request runs `verify` whatever it targets. The `push` trigger keeps
its `[dev]` filter, which is genuinely a base-branch question.

Turn on branch protection for `dev` and `master` with `verify` required, making
ADR-0018's sentence true as written. That is a repository setting rather than a
file here, so it is recorded in this entry and applied by hand.

**Consequences.** CI minutes are spent on intermediate bases in a stack — which
is the point: an intermediate pull request is the one whose code nobody has run.
The cost is bounded by `concurrency`, which already cancels superseded runs.

Until branch protection is actually switched on, CI in this repository is
**advisory**. ADR-0018's claim is corrected rather than deleted, because the
wrong sentence is the more useful record: it is the one that stopped anyone
checking, and it went unexamined through twelve merged pull requests.

**A second hazard, recorded because it also bit.** A stacked pull request must be
merged **bottom-up**, letting GitHub retarget each one to `dev` after the one
below it lands. Merging them in the other order — or merging each into the
literal base branch it was opened against — marks all four green and merged
while only the bottom one reaches `dev`; the rest land in feature branches that
nothing points at. That happened here, and took a branch-by-branch comparison
against `dev` to notice, because every pull request said "merged". CI could not
have caught it: each merge was individually valid. The defense is merge order,
and the cheaper alternative is not to stack at all. This repository's own stack
(ADR-0026 through ADR-0029, and this one) follows that discipline.

**Alternatives rejected.** Adding each stack's intermediate branches to the
filter. It puts the burden on whoever opens the stack, at the moment they are
least likely to be thinking about CI configuration, and it fails silently again
the first time someone forgets.

Flattening a stack so every pull request targets `dev` directly. The
dependencies are real — the error classification does not compile without the
network monitor — so flattening either duplicates commits across pull requests
or opens ones that cannot build.

---

## ADR-0035

### `verify` compiles the instrumented tests it cannot run

**Accepted** · 2026-09-21 · **amends** [ADR-0018](#adr-0018) · ported from `CatsListApplication`'s ADR-0031

**Context.** [ADR-0032](#adr-0032) changed what the feed renders for a failed
load, and [ADR-0033](#adr-0033) changed `UiText` and
`CatsListFavoritesStatus.Unavailable` from objects into types carrying a value.
The unit tests were updated with them. The instrumented tests were not, and
nothing said so: `verify` passed, CI passed, five pull requests merged — in
`CatsListApplication`, where this happened; this repo inherits the fix.

Three tests were broken, in two different ways. `CatsListContentTest` asserted
`catslist_error_loading_cats` — a string the screen had stopped rendering, which
would have failed at runtime. Worse, `CatsListContentTest` and
`CatRepositoryImplTest` no longer **compiled**: one constructed
`CatsListFavoritesStatus.Unavailable` as an object, the other called
`CatRepositoryImpl` without its new `ErrorMapper`. The whole instrumented source
set was unbuildable, and the gate had nothing to say about it.

[ADR-0018](#adr-0018) split verification because instrumented tests need a
device and a gate that fails without one teaches people to skip it. That
reasoning is still right, and it quietly conflated two different things:
*running* those tests needs a device, *compiling* them does not.

**Decision.** `verify` additionally depends on `assembleDebugAndroidTest` for
every module that has an instrumented test source set. No device, no emulator,
no change to what `verifyOnDevice` means. Both gates now share one
`androidTestModules` list rather than filtering `subprojects` twice.

**Consequences.** A change that breaks instrumented-test *source* now fails on
the same gate as everything else, seconds after it is made, instead of waiting
for whenever someone next attaches a device. Given those tests are the only
coverage of the three data-loss paths, and ADR-0018 already admits they are the
least-run tests in the project, the gap between "broken" and "noticed" was the
whole risk.

An assertion that compiles and is simply *wrong* — the
`catslist_error_loading_cats` one — still needs a device to catch. This closes
the larger half of the hole, not all of it.

`verify` gets slower by one APK build per module with instrumented tests. This
repo's own `androidDeviceTest` source sets (Kotlin Multiplatform's name for the
same thing, per ADR-0029) are covered by the same dependency.

**Alternatives rejected.** Running the instrumented tests in CI on a Gradle
Managed Device, which is ADR-0018's own **Review when** and would collapse the
two tiers entirely. It is the better answer and a bigger change; this one is a
two-line dependency that needed no new infrastructure, and it should not wait
behind that.

Leaving it to `verifyOnDevice`. That is where it was, and it is how three broken
tests reached `dev` across five pull requests.

---

## ADR-0036

### Porting v2.3.0 from the Android app: what changed on the way in

**Accepted** · 2026-09-22

**Context.** `CatsListApplication` kept moving after this repo forked from it at
v2.2.0 ([ADR-0028](#adr-0028)): ten pull requests landed there, recorded as
that repo's own ADR-0026 through ADR-0031. This repo needed that work — R8 for
release, a launcher icon, `AppError`/`ErrorMapper`, `NetworkMonitor`, Compose
stability fixes, and two CI corrections — and the two repos share Git history
back to the fork point, so it arrived as an ordinary merge of
`CatsListApplication`'s `master` rather than a re-fork.

**Decision.** `git merge` brought the six new ADRs in as ADR-0030 through
ADR-0035, renumbered because this repo had already claimed 0026–0029 for Koin,
Ktor and the module-by-module migration before the fork's sibling repo had
written its own 0026–0031 in parallel — a collision only visible once both
histories met. Each renumbered entry keeps its original reasoning; ADR-0030 and
ADR-0032 also note where the *mechanism* changed (Hilt → Koin, Retrofit → Ktor)
because the original text named APIs — `NetworkModule`, `dagger.Lazy`,
`retrofit2.HttpException` — that do not exist here.

**What the merge actually touched, beyond renumbering:**

- **`BindsModule.kt` and `NetworkModule.kt`**, Hilt's `@Binds`/`@Provides`
  modules, are gone; their bindings (`CatRepository`, `ImageDownloader`,
  `NetworkMonitor`, the shared `OkHttpClient`) moved into `dataModule` and each
  platform's `platformDataModule` (ADR-0026).
- **`ConnectivityNetworkMonitor`** moved from a flat Android source set into
  `:core:data`'s `androidMain`, and lost its Hilt constructor injection for a
  plain one Koin fills in.
- **`ErrorMapper`** moved into `commonMain`. Its HTTP-code branch now catches
  Ktor's `ClientRequestException`/`ServerResponseException` instead of
  `retrofit2.HttpException`; its connectivity branches still catch
  `java.net`/`java.io` types directly, which is the same scoped deviation
  `CatFeedPagingSource`'s `Mutex` swap and `CatOkHttpClient.kt` already
  document — valid for `jvm()`+`androidTarget()`, revisited if a non-JVM target
  arrives. A tiny `expect`/`actual` `dataLogWarning` replaces `android.util.Log`,
  which has no multiplatform form.
- **`DesktopNetworkMonitor`**, a new `jvmMain` file with no Android counterpart
  to merge from: always reports online, since desktop has no `ConnectivityManager`
  and a real check was out of scope for this port (ADR-0031's note).
- **The shared `OkHttpClient`** is one function, `catOkHttpClient()`, called
  from both platforms' Koin modules rather than duplicated — `okhttp3` types are
  usable in `commonMain` today for the same reason `java.net`/`java.io` are.

**Consequences.** `./gradlew verify` passes clean from a fresh checkout, and
`:core:data:jvmTest` still runs the real desktop database (ADR-0029). What is
**not** verified by this merge: nothing here has been run on a device — Koin's
graph, `ConnectivityNetworkMonitor`, and the new Coil/OkHttp wiring are
compiled and unit-tested, not launched. `ErrorMapperTest` was rewritten to
build its `ClientRequestException`/`ServerResponseException` cases from a real
`MockEngine` call rather than hand-constructing them, since Ktor's exception
types are not built the way Retrofit's `HttpException` was.

**Review when:** `CatsListApplication` moves again. The two repos share history
only up to the commit each merge actually pulls; the next one repeats this ADR's
shape — merge, renumber past whatever this repo has claimed since, adapt what
named a mechanism the fork replaced.

---

## ADR-0037

### The UI moves to Compose Multiplatform, one module at a time

**Context.** Step 4 of ADR-0028's order: the UI. ADR-0028 already checked that
every library the screens use publishes common, JVM and native variants, so the
question left was how, not whether. Two things made the "how" less obvious than
moving files into `commonMain`: Android's `R` class, which every screen reads
its strings and drawables through, has no multiplatform form; and the modules
cannot all move at once without one unreviewable pull request.

**Decision.** Compose Multiplatform (the `org.jetbrains.compose` plugin), applied
through a new additive convention plugin, `catslist.kmp.compose`, next to
`catslist.kmp.android.library`. Modules move bottom-up, the same order ADR-0028
used for the data layer: `:core:ui` first, then `:core:testing`,
`:core:designsystem`, `:feature:favorites`, `:feature:feed`.

Strings and drawables move to **Compose resources**
(`src/commonMain/composeResources/`), read through a generated `Res` class.
Every module gets its own `Res`, in a package derived from its Gradle path
(`:core:ui` → `com.example.catslist.core.ui.resources`) — the same
no-collisions guarantee a per-namespace `R` gave. `Res` stays internal unless
something outside the module has to name a resource.

`UiText.Resource` holds a `StringResource` instead of an `@StringRes Int`. The
non-composable `resolve(context)` becomes `suspend fun load()`: Compose resources
are read from files, not from a `Context`, and reading a file suspends.

**One transitional case, now gone.** Compose resources need the multiplatform
plugin — tried and confirmed: in a `com.android.library` module the plugin
generates no resource tasks. So while the features were still Android-only,
their own strings could only be `R` ids, and `UiText` carried them as
`UiText.AndroidResource`. It
resolved on Android and threw on desktop, where nothing could create one. It went
when `:feature:feed`, the last module to move, did — along with the
`load(context)` overload that existed only to resolve it outside composition.

**Consequences.** Desktop tests of anything that reads a resource need Skia's
native library for the host OS — reading a string asks it for the system theme —
so the convention plugin adds it to `jvmTest`. `:core:ui`'s tests now run there,
including one that reads the real strings: Android's `strings.xml` escapes
apostrophes and Compose resources does not, so a file copied verbatim would show
backslashes, and only reading the text catches that.

Vector drawables carry over as Android vector XML, which Compose resources
parses on every platform — including Android, where it replaces the framework's
own parser. It does not know Android's theme references: the icons'
`@android:color/white` fill crashed the first desktop test to draw one, and is
now a literal `#FFFFFFFF` (the icons are tinted where they are used, so the value
never showed). The Android-only parts that remain — dynamic colour and the status
bar's icon tint in `CatsListTheme` — are an `expect`/`actual` pair that does
nothing on desktop.

UI tests are split by what they need. Anything about the device — the card
sitting clear of the status bar, edge-to-edge — stays an instrumented test. The
components themselves get desktop tests (`runComposeUiTest` in `jvmTest`),
which `./gradlew verify` runs with no device.

On Android, CMP's artifacts resolve to the androidx Compose ones, so the Compose
BOM still decides what the app ships; nothing on the Android side changes
version.

**The root UI is shared too.** `CatsNavDisplay`, a `CatsApp()` composable and
the Koin module list (`appModules`) live in a multiplatform `:shared` module;
`:app` keeps only `MainActivity` and `App`, and a desktop entry point needs no
more than that either. Two things only showed up once the whole app ran on
desktop, in `CatsAppDesktopTest`:

- **ADR-0028's library check was one level too shallow.** Google's
  `navigation3-ui` *does* publish a desktop variant — whose `NavDisplay` throws
  "Implemented only in JetBrains fork". Off Android, Navigation 3's UI and its
  ViewModel integration have to come from JetBrains
  (`org.jetbrains.androidx.navigation3:navigation3-ui`,
  `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-navigation3`), which on
  Android are built from the same source against Google's `navigation3-runtime`.
  Publishing a variant is not the same as implementing one.
- **Off Android, a back stack cannot find its keys' serializers by
  reflection**, so every `NavKey` is registered in a `SavedStateConfiguration`
  next to `CatsNavDisplay`. A key missing from it fails only when the stack is
  saved — switching tabs does it, which is why the desktop test switches tabs.

Koin's Compose integration remembers the global Koin it first sees, so a UI test
that starts and stops Koin per test gets the previous test's closed scope. The
desktop tests hand the composition a Koin of their own (`KoinIsolatedContext`)
instead.

**Two apps, one version.** With a desktop app beside the Android one, the
version moved from `app/build.gradle.kts` into `gradle.properties`
(`catslist.version`): Android still derives `versionCode` from it (ADR-0021,
otherwise unchanged) and the desktop installers use it as it stands. The
installers are built by `.github/workflows/release.yml`, one runner per OS
because jpackage cannot cross-package, on every version tag and on any pull
request that touches `:desktopApp` — packaging fails in ways `verify` never
exercises, and the packaged app runs on a trimmed Java runtime whose missing
module only shows when the code needing it first runs.

**Review when:** AGP's Kotlin Multiplatform library plugin and Compose resources
stop agreeing — they are two separately versioned plugins meeting at the Android
resource pipeline, which is where an upgrade of either would break first.

---

## ADR-0038

### Adaptive layout: one grid rule, one width breakpoint

**Context.** Until now every screen was one full-width column under a bottom bar,
laid out for a phone held upright. A phone in landscape stretched each card into
a strip the photo barely showed through, and the desktop app only looked right
because its window opened phone-shaped. With desktop shipping and iOS (iPad
included) next, "phone, portrait" stopped being the only case.

**Decision.** Two rules, both in common code, so every platform gets them:

- **Cats lay out in a grid, not a column.** The feed, favorites and their
  loading skeleton are `LazyVerticalGrid`s sharing one column rule,
  `CatGridCells` in `:core:designsystem`: as many columns as fit at 340dp. A
  phone upright gets one, a phone on its side two, a wide desktop window three
  or four. The feed stays on Paging — `LazyPagingItems` indexes a grid exactly
  as it did a column — and its in-list notices span the full row.
- **Navigation follows the window's width.** Below 600dp, Material's
  compact/medium breakpoint, the floating bottom bar stays; from 600dp it
  becomes a floating `NavigationRail` at the start edge, and the lists leave room
  for it there the way they leave room for the bar at the bottom.

The width is read with `BoxWithConstraints` around the scaffold rather than
from `material3-adaptive`'s window size classes: one breakpoint does not need a
dependency, and constraints are what a test can set.

**Consequences.** Both rules are tested on desktop at fixed sizes: the bar at
400dp and the rail at 1000dp (`CatsAppDesktopTest`), one card per row at 400dp
and two side by side at 1000dp (`CatsListContentDesktopTest`).

**Alternatives rejected.** `NavigationSuiteScaffold`, which switches between bar
and rail by itself: it brings Material's standard bar and rail, and this app's
are floating pills. A grid with a fixed column count per breakpoint: an
adaptive minimum width gets the in-between widths — a narrow desktop window, a
small tablet — right without listing them.

**Review when:** a screen needs a layout that is not a list of cats — a detail
pane beside the grid would be the case for `material3-adaptive`'s list-detail
scaffolds.

---

## ADR-0039

### iOS targets for every multiplatform module

**Accepted** · 2026-09-24 · amends [ADR-0028](#adr-0028)'s "Targets: `jvm()` only"

**Context.** ADR-0028 declared only `jvm()` beside Android, because a target that
is configured but never built is a claim the build cannot back up, and iOS needs
a macOS machine to build at all. That machine is now here, and CI has a macOS
runner. Two pieces of `:core:data`'s `commonMain` only compiled because every
target was a JVM (ADR-0036 names both): `ErrorMapper` caught `java.net`/`java.io`
exceptions, and the shared `OkHttpClient` was built in common code.

**Decision.** `catslist.kmp.library` and `catslist.kmp.android.library` declare
`iosArm64()` and `iosSimulatorArm64()`, for a device and the simulator on Apple
silicon. There is no `iosX64`, since nothing here builds on an Intel Mac. On a
host that cannot build iOS, Kotlin skips those targets and the rest builds as
before. `:desktopApp` stays `jvm()` only and applies the multiplatform plugin
directly, since an application for the desktop has nothing to compile for iOS.

The two JVM-isms went first:

- **`ErrorMapper` catches Ktor's multiplatform types:**
  `io.ktor.client.network.sockets.SocketTimeoutException` and
  `kotlinx.io.IOException`. On the JVM both are typealiases for the `java.net`
  and `java.io` classes it caught before, so Android and desktop classify
  exactly as they did, and `ErrorMapperTest` passes unchanged. The Darwin engine
  throws the same `SocketTimeoutException` for `NSURLErrorTimedOut` and a
  `DarwinHttpRequestException`, which is an `IOException`, for everything else.
  The separate `UnknownHostException` branch is gone, because it produced the
  same result as the `IOException` branch below it.
- **OkHttp lives in a `jvmAndAndroid` source set**, now declared by the
  convention plugin for every Android-and-multiplatform module rather than by
  `:core:testing` alone. The 15-second cap is a common constant that each
  platform applies to its own engine: OkHttp's `callTimeout`, and URLSession's
  request and resource timeouts.

iOS then answers the same questions every platform does in `platformDataModule`:

- **Database:** Room's KSP processor runs for both iOS targets, over the same
  bundled SQLite. The file lives in Application Support, where iOS keeps data an
  app owns and a user never browses.
- **Network:** `IosNetworkMonitor` reads `NWPathMonitor`. A *satisfied* path is
  the closest iOS has to Android's `NET_CAPABILITY_VALIDATED`, but unlike Android
  it does not see past a captive portal.
- **Downloads:** `IosImageDownloader` saves to Photos with *add-only* access. The
  app can add a cat to the library but never read what is already there. The
  system asks on the first download, using the app's
  `NSPhotoLibraryAddUsageDescription` text. A refusal throws, and the screen
  reports it like any other failed download.
- **Theme and logging:** there is no dynamic color, and nothing to set on the
  status bar, whose default style already follows the system's light or dark
  mode, as the theme does. Logging goes to standard output, which Xcode's
  console shows. It does not go to `NSLog`: a Kotlin `String` passed through
  `NSLog`'s C varargs is not bridged to an `NSString`, and `NSLog` crashed
  formatting it. `ErrorMapperIosTest` found that on its first run. In the app it
  would have crashed on the first error it logged.

`:shared` builds a static `Shared` framework. It exposes `MainViewController()`,
which wraps `CatsApp()` in a `UIViewController` for Swift to host.

**Consequences.**

- **Coil is held at 3.4.0.** From 3.5.0, Coil's iOS klibs are built by Kotlin
  2.4, and Kotlin 2.3.21's native compiler refuses a newer klib ABI outright. The
  JVM tolerates newer metadata, which is why Android and desktop never showed it.
  Every other iOS dependency is built by Kotlin 2.3 or older.
- **Coil and Compose use different Skiko versions.** Coil 3.4.0 was built against
  Skiko 0.9.22.2, and Compose Multiplatform 1.12.1 brings 0.150.1. The framework
  links with no partial-linkage warnings, so every Skiko call Coil makes
  resolved. Whether images actually decode on iOS is for the app to show.
- **One type inference differed on native.** `listOf(CatsListNavKey,
  FavoriteCatsNavKey)` was `List<NavKey>` on the JVM and `List<Any>` on native,
  so the type is now written out.
- **On a Mac, `./gradlew verify` builds and tests iOS too,** through each
  module's `check`. The CI `ios` job runs the iOS tests on a simulator, compiles
  the device target and links the framework.
- **Two test classes run on iOS:**
  - `CatDatabaseIosTest` covers the real database, as `CatDatabaseJvmTest` does
    on desktop.
  - `ErrorMapperIosTest` covers the failures only URLSession produces.

  The rest of the suite uses JUnit and Truth, so it runs on the JVM targets
  only. Moving it to `commonTest` is listed under "After 3.0.0" in the roadmap.

**What this does *not* claim.** Nothing here has run as an app. The framework
links, and the iOS tests pass on a simulator. The Xcode project, Koin startup on
iOS, Coil's network fetcher and the Photos prompt are the next step.

**Review when:** Kotlin moves to 2.4. Coil can then return to its current
release, and the Skiko mismatch goes with it.

---

## ADR-0040

### The iOS app: a thin Xcode project around `CatsApp()`

**Accepted** · 2026-09-24

**Context.** [ADR-0039](#adr-0039) left a `Shared` framework that links and
tests that pass on a simulator, with nothing that a person could open. iOS
needs an Xcode project to be an app at all. The goal for all three platforms
is the same: a launcher and nothing else, with every screen in `commonMain`.

**Decision.** `iosApp/` is that launcher.

- **Swift does as little as it can.** `iOSApp.swift` calls `startCatsApp()` once,
  and `ContentView` hosts `MainViewController()` edge to edge, since Compose
  reads the safe area itself. The rest of the startup lives in Kotlin
  (`:shared`'s `iosMain`), in the same shape as Android's `App` and desktop's
  `main()`:
  - Koin starts with `appModules`.
  - Coil's singleton loader fetches over Ktor, using a client of its own on the
    graph's one URLSession engine. That is iOS's form of
    [ADR-0030](#adr-0030)'s shared client. The API's own client is not reused,
    because it resolves paths against TheCatAPI and throws on any non-2xx
    response.
- **Gradle builds the framework from inside Xcode.** A build phase runs
  `:shared:embedAndSignAppleFrameworkForXcode`, which also copies the Compose
  resources into the app. It calls `bash ./gradlew`, because the wrapper is
  committed without its executable bit.
- **The project is written by hand, and small.** It uses Xcode 16+'s
  synchronized folders, so adding a Swift file does not touch
  `project.pbxproj`. There is one target, one shared scheme and one
  `Config.xcconfig`.
- **Signing stays out of git.** The Team ID lives in a git-ignored
  `Config.local.xcconfig`, optionally included by the committed config. The
  bundle ID is suffixed with the team, because a bundle ID belongs to the first
  team that registers it, and a free Apple ID cannot take one someone else
  already holds.
- **The version is the same one as everywhere else.** A last build phase stamps
  `catslist.version` from `gradle.properties` into the built `Info.plist`
  before signing, so iOS adds no second number to keep in step
  ([ADR-0021](#adr-0021)).
- **The icon** is the macOS icon with its transparent corners filled in the
  launcher background (`#6650A4`). iOS wants a full-bleed opaque square and
  applies its own mask.
- **`Info.plist`:**
  - `NSPhotoLibraryAddUsageDescription`, the text of ADR-0039's add-only
    Photos prompt.
  - `CADisableMinimumFrameDurationOnPhone`, so Compose can draw at 120 Hz on
    ProMotion screens.
  - Every orientation except upside-down on iPhone, since
    [ADR-0038](#adr-0038)'s grid is built for landscape too.

**Consequences.**

- **Where it has run.** Built, installed and launched on the simulator and on an
  iPhone 11 running iOS 27.0, from Xcode 26.0.1. On the phone the feed loads
  and its images decode, which answers ADR-0039's open question about Coil and
  Skiko.
- **CI builds the app too.** The `ios` job adds an unsigned `xcodebuild` for the
  simulator, so a broken project or Swift file fails a pull request. Signing is
  the one step CI cannot check, because a runner has no Apple ID. Simulator
  builds exclude `x86_64`, because there is no `iosX64` target (ADR-0039). A
  generic simulator destination builds for Intel as well, and failed on exactly
  that in the first CI run.
- **Deployment target: iOS 16.** The linker warns that Compose's bundled ICU data
  is marked for 18.5. It is a data object with no code in it.
- **Two network failures seen while testing were the network, not the app:**
  - On this Mac, the simulator could not resolve TheCatAPI through a VPN that
    answers DNS with fake `198.18.x` addresses.
  - On the phone, the first request timed out on a direct connection.

  Both were classified as ADR-0032 intends: the first as `Unreachable` (the
  path was satisfied and the host was not found), the second as `Timeout`.

**Alternatives rejected.** XcodeGen or Tuist, which generate the project from a
spec. For one target that no one edits often, that is one more tool to install
on every Mac and in CI, and synchronized folders already keep the hand-written
file from churning.
