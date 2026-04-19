# Conventions

Rules that reviewers enforce during PR review. Violations block merge unless explicitly justified in the PR description.

## §1 Scope

- These rules apply to code under `app/src/main/`.
- Test code (`app/src/test/`, `app/src/androidTest/`) has relaxed rules noted in §9.
- Generated code (Apollo, Hilt, KSP output) is out of scope.

---

## §2 Null-safety

### §2.1 No new `!!`

The `!!` non-null assertion operator is forbidden in new code. Existing call sites (3 as of this writing) are all defensive invariants immediately after a null check:

| Site | Invariant guaranteeing non-null |
|------|----------------------------------|
| `ui/HackersPubApp.kt:169` | early `return` when `isLoggedInState == null` on the preceding line |
| `ui/components/ArticleCard.kt:83` | inside `if (isRepost && post.lastSharer != null)` |
| `ui/components/ReactionPicker.kt:53` | inside `.filter { it.emoji != null }` |

New `!!` requires a `// safe because <reason>` comment and reviewer approval. Before adding one, try:

- Elvis `?:` with a default or `return`
- Smart cast after an `if (x != null)` / early return on a local `val`
- `?.let { ... }` to scope the non-null binding
- Restructure to eliminate the nullable in the first place

### §2.2 No `lateinit` in domain/UI code

Prefer `val ... by lazy { ... }` or a nullable `var`. `lateinit` is acceptable only for framework requirements (test rules, some Hilt test fixtures).

---

## §3 Paging 3

### §3.1 Fixed `PagingConfig` values

All cursor-based feeds must use the factory in `app/src/main/java/pub/hackers/android/data/paging/CursorPagingSource.kt`:

```kotlin
pageSize = 20
prefetchDistance = 5
initialLoadSize = 20
enablePlaceholders = false
```

Do not override these without a written rationale. The specific values are not arbitrary — see `CursorPagingSource.kt` KDoc for each value's history:

- `pageSize = 20` matches the server's GraphQL `first: 20` (hard ceiling).
- `prefetchDistance = 5` — `20` caused cascading prefetches of pages 2/3/4 right after first render.
- `initialLoadSize = 20` — `30` made Paging think the first window was under-filled and immediately schedule an append.

### §3.2 Build pagers with `cursorPager { ... }`

ViewModels must not construct `Pager` or `PagingConfig` directly. Use the `cursorPager { repository.xxxPage(it) }` factory. Each new endpoint gets a one-line adapter in the `Repository adapters` region of `CursorPagingSource.kt`.

### §3.3 All post feeds must pass through `.distinctByEffectiveId()`

Every `Flow<PagingData<Post>>` must be composed with `.distinctByEffectiveId()` (defined in `data/paging/PostOverlay.kt`). This is the paging-layer half of the deduplication strategy described in §3.4.

### §3.4 Two-layer deduplication is mandatory

Keep both layers in place; neither alone is sufficient.

| Layer | What it catches | Key |
|-------|-----------------|-----|
| Repository `.distinctBy { it.id }` | Server returns the same outer post id across pages | `post.id` |
| Paging `.distinctByEffectiveId()` | Original + repost wrapper both land in the feed (different outer ids, same displayed content) | `post.sharedPost?.id ?: post.id` |

### §3.5 No `PagingSource.invalidate()` for optimistic updates

Do not call `invalidate()`, `refresh()`, or rebuild the `Pager` to reflect a share/reaction/repost toggle. Use the overlay system in `data/paging/PostOverlay.kt`:

1. `overlayStore.mutate(postId) { it.copy(...) }`
2. Compose the flow with `combine(pagingFlow, overlayStore.overlays) { paging, overlays -> paging.map { it.applyOverlays(overlays) } }`.

Invalidating the source drops the user's scroll position, refetches already-loaded pages, and defeats `cachedIn(viewModelScope)`.

### §3.6 ViewModel assembly template

```kotlin
val posts: Flow<PagingData<Post>> = combine(
    cursorPager { repository.xxxPage(it) }
        .flow
        .distinctByEffectiveId()
        .cachedIn(viewModelScope),
    overlayStore.overlays,
) { paging, overlays ->
    paging.map { post -> post.applyOverlays(overlays) }
}.cachedIn(viewModelScope)
```

Note the **two** `cachedIn` calls: the first caches raw paging data, the second caches after overlay application so downstream collectors don't re-apply on every recomposition.

### §3.7 Optimistic update error handling

Mutate the overlay first, launch the network call, and on failure either revert the mutation or `overlayStore.clear(postId)`. See `TimelineViewModel.sharePost` / `toggleReaction` for the reference shape.

---

## §4 Coroutine dispatchers

### §4.1 HTML rendering

Parsing longer than `HTML_SYNC_PARSE_THRESHOLD = 500` characters must run on `Dispatchers.Default` via `produceState`, with results cached in `LruCache<HtmlCacheKey, AnnotatedString>(256)`. See `ui/components/HtmlContent.kt`. Short HTML and cache hits stay on the main thread.

### §4.2 Code block syntax highlighting

Same pattern at `CODE_SYNC_PARSE_THRESHOLD = 300` characters, `LruCache(128)`. See `ui/components/CodeBlockView.kt`.

### §4.3 Repository response mapping

GraphQL response → domain model conversion must run inside `withContext(Dispatchers.Default)`. `HackersPubRepository` does this at every paginated / detail endpoint. Mapping post graphs (with nested shared/quoted posts, media, reactions) is CPU-heavy enough to show up as main-thread jank on timeline refresh.

### §4.4 File and disk I/O

Use `Dispatchers.IO`. Examples: cache-size computation and cache clearing in `SettingsViewModel`, translation model downloads in `PostCard`.

### §4.5 Network

Do not wrap Apollo calls in `withContext(Dispatchers.IO)` — Apollo/OkHttp manage their own dispatchers.

---

## §5 Compose stability

### §5.1 Domain models must be `@Immutable`

Every class in `domain/model/Models.kt` (and any new domain model) is annotated `@Immutable`. Adding a domain model without the annotation is a blocking review comment.

### §5.2 Memoize hot-path computations

Inside Composables in the post list / detail feed, wrap non-trivial transformations in `remember(key) { ... }`. Established examples:

- `ReactionPicker`: reaction grouping, emoji map, sorted reaction list
- `PostCard`: `DateTimeFormatter`, relative-time formatter
- Any list filter/sort over `reactionGroups`

Simple formatting of primitive values (numbers, short strings) is cheap enough that `remember` adds noise without benefit — use judgment.

### §5.3 Prefer `ImmutableList<T>` for Composable parameters

For new Composable parameters that accept collections, prefer `kotlinx.collections.immutable.ImmutableList<T>` over `List<T>` to preserve Compose's stable-skipping. Existing `List<T>` signatures are not retroactively migrated by this rule; it applies to new code.

### §5.4 `Modifier` parameter position

Composable parameters follow the Compose API Guidelines for `Modifier`:

- `modifier: Modifier = Modifier` is the **first optional parameter**, immediately after all required parameters.
- The default value is bare `Modifier` — do not pre-apply modifiers inside the default (`Modifier.fillMaxWidth()` etc.). The caller composes modifiers.
- When a Composable has other optional parameters, `modifier` comes **before** them. Trailing-lambda content (`content: @Composable () -> Unit`) is the usual exception and goes last.
- Internal `@Composable` helpers that always require a modifier from the caller may omit the default, but must still place the `modifier` parameter as the first `Modifier`-typed parameter.

This is an established convention in the project — see commit `0e92f03` (PR #86) which refactored `HtmlContent` specifically to fix a violation, and every `ui/components/*.kt` Composable follows the pattern.

### §5.5 Read configuration from Compose state

When a Composable needs locale / screen / orientation-dependent values from the current Android configuration, read `LocalConfiguration.current` instead of reaching through `LocalContext.current.resources.configuration`.

- `LocalContext.current.resources.configuration` is not tracked as Compose state, so configuration changes can leave the caller with stale values.
- `LocalConfiguration.current` invalidates and recomposes correctly when the `Configuration` changes.
- If code only needs resources lookup (`getString`, dimensions, etc.), `stringResource(...)` and other Compose resource APIs remain preferred. Use `LocalConfiguration.current` specifically when you need the `Configuration` object itself.

Reference examples:

- `ui/components/PostCard.kt` translation locale selection
- `ui/screens/postdetail/PostDetailScreen.kt` translation locale selection

### §5.6 Avoid unnecessary `LocalContext` and `Activity` dependencies

Do not read `LocalContext.current` just to satisfy an obsolete parameter or to pass context through layers that do not need it. In Compose, every `LocalContext.current` read is still a composition-local dependency; keep it scoped to the smallest call site that actually uses Android context APIs.

- Prefer removing the context parameter entirely when the callee already owns the context it needs.
- Prefer `Context` over `Activity` unless the called API explicitly requires an `Activity` or an activity-bound lifecycle/window token.
- Do not cast `LocalContext.current as Activity` speculatively. If an API can operate with `Context`, keep the UI and ViewModel signatures context-free or `Context`-based.

Reference examples:

- Passkey sign-in uses the `PasskeyManager`'s injected application context instead of threading an `Activity` through `SignInScreen` / `SignInViewModel`.
- Passkey registration accepts a `Context` from `SettingsScreen` only where Credential Manager registration is initiated.

### §5.7 Do not read frequently changing state in composition

Values annotated with `@FrequentlyChangingValue` should not be read directly in Composable function bodies. Reading them during composition makes that composition depend on high-frequency updates and can cause avoidable recompositions.

- Use `derivedStateOf` only when the derived result changes much less often than the source value, such as reducing scroll position to "at top" / "not at top".
- Use `snapshotFlow` inside `LaunchedEffect` when a frequently changing value needs to drive state updates or side effects without being read directly by composition.
- Prefer layout- or draw-phase reads, such as lambda-based modifiers, when the value only affects placement or drawing.

Reference example:

- `ui/screens/compose/ComposeScreen.kt` mention autocomplete observes text-field scroll with `snapshotFlow` and feeds a calculated popup offset into composition.

---

## §6 ViewModel and state

### §6.1 Hilt

ViewModels must be `@HiltViewModel` and injected via `hiltViewModel()` in the Composable that owns them.

### §6.2 UI state as `StateFlow<UiState>`

`UiState` is a `data class` (or sealed class if the states are disjoint). Expose it as `StateFlow<UiState>`, not `LiveData` and not a raw `Flow`.

### §6.3 One-shot events are separate from state

Snackbar messages, navigation triggers, and other one-shot effects go through a `Channel` or `SharedFlow(replay = 0)`, never mixed into `StateFlow<UiState>`. Mixing them creates re-play bugs on configuration change.

### §6.4 Optimistic update pattern

```kotlin
overlayStore.mutate(postId) { it.copy(viewerHasShared = true, shareDelta = +1) }
viewModelScope.launch {
    repository.sharePost(postId).onFailure {
        overlayStore.mutate(postId) { it.copy(viewerHasShared = false, shareDelta = 0) }
    }
}
```

---

## §7 Repository

### §7.1 Return `Result<T>`

Repository methods return `Result<T>`. Don't throw for expected error paths (network failure, auth expiration, not-found). Reserve exceptions for programmer errors.

### §7.2 `FetchPolicy` pattern

Paginated feeds use `FetchPolicy.NetworkOnly` on refresh (first page) and fall through to Apollo's default (CacheFirst) for subsequent pages. The canonical shape is:

```kotlin
.apply { if (refresh) fetchPolicy(FetchPolicy.NetworkOnly) }
```

Detail queries apply `NetworkOnly` explicitly when freshness is load-bearing (post detail after a reaction, profile after a follow action). Normalized-cache writes still happen under `NetworkOnly` — the policy controls reads, not writes, so fragment-shared data remains consistent across screens.

New queries should follow this pattern. If a new query needs a different policy (e.g., `CacheAndNetwork` for "show cached then update"), justify it in the PR description or a code comment.

---

## §8 Navigation

### §8.1 Sealed `Route` types

Route destinations are a sealed class hierarchy. Do not pass raw route strings through the NavController.

### §8.2 Deep links through `HackersPubUrlRouter`

All URL-based entry points (external links, email verification, push notifications) are parsed by `HackersPubUrlRouter` into the sealed `Route` type. Don't add ad-hoc deep-link handling elsewhere.

---

## §9 Testing

### §9.1 `MainDispatcherRule`

ViewModel tests must install `MainDispatcherRule` (`app/src/test/java/pub/hackers/android/testutil/MainDispatcherRule.kt`), which uses `StandardTestDispatcher`.

### §9.2 `!!` allowed under test invariants

In test code, `!!` is permitted when a setUp method or the test's own preceding assertion establishes the non-null invariant. Example:

```kotlin
val inner = post(id = "inner")
val outer = post(sharedPost = inner).applyOverlays(...)
assertEquals(expected, outer.sharedPost!!.field)  // setUp guarantees sharedPost != null
```

### §9.3 New ViewModels require tests

New or substantially refactored ViewModels ship with tests in the same PR. Reference tests live in `app/src/test/java/pub/hackers/android/ui/screens/**/*ViewModelTest.kt`.

---

## §10 Build and variants

### §10.1 Dependencies via Version Catalog

All dependency declarations go in `gradle/libs.versions.toml`. Module `build.gradle.kts` files reference them as `libs.xxx`. No hardcoded versions in build scripts.

### §10.2 Debug variant separation

The `debug` build type sets `applicationIdSuffix = ".dev"` and ships a distinct DEV launcher icon from `app/src/debug/res/mipmap-*`. Do not remove this separation — the DEV icon is what prevents debug-vs-release confusion on developer devices.

### §10.3 ProGuard keep rules

`app/proguard-rules.pro` keeps generated Apollo classes, Coil classes, and WorkManager `HiltWorker` / `WorkerAssistedFactory`. Removing or narrowing these rules requires a minified-release smoke test in the PR.

### §10.4 Release is minified

`release` build type has `isMinifyEnabled = true`. Don't disable minification to "make debugging easier" — add the missing keep rule instead.

### §10.5 Run lint before pushing

Run `./gradlew :app:lintDebug` after finishing each task and again right before `git push`. The target is **0 errors** — warnings are tolerated but new warnings should be justified in the PR body.

Lint catches problems that `compileDebugKotlin` and unit tests do not: manifest issues, API-level traps (§13), resource hygiene, and typographic issues. Compilation passing does not mean lint passes.

For a **genuine false positive** (e.g., the AGP 9.1.1 `Instantiatable` bug on `@AndroidEntryPoint` activities), suppress narrowly at the single call site with `tools:ignore="..."` or a `//noinspection` comment, and cite the upstream issue in the commit message. Do not disable a check globally via `lintOptions { disable ... }` — it hides future real failures of the same kind.

---

## §11 Localization and accessibility

### §11.1 No hardcoded user-facing strings

All user-visible text goes through `stringResource(R.string.xxx)`. No string literals in Composables that will render to the screen. Exceptions: debug-only labels, log messages, format templates that are themselves composed from resources.

String keys are descriptive: `error_no_network`, not `msg3`.

Use the single ellipsis character (`…`) instead of three periods (`...`) in user-facing strings.

### §11.2 `contentDescription` on interactive icons

Every `Icon`, `IconButton`, `AsyncImage` that carries meaning requires a `contentDescription`, usually sourced from `stringResource`. Decorative-only icons pass `contentDescription = null` explicitly — the argument is never omitted.

### §11.3 Don't rely on color alone

State conveyed through color (success/error/active) must also have an icon, text label, or shape cue so color-blind users and grayscale displays don't lose information.

---

## §12 GraphQL hygiene

### §12.1 Fragments are reused

When editing `app/src/main/graphql/pub/hackers/android/operations.graphql`, prefer extending or composing existing fragments (`ActorFields`, `PostFields`, `EngagementStatsFields`, etc.) over introducing parallel ad-hoc selections. Divergent selections across operations break normalized-cache sharing.

### §12.2 Generated code stays in `build/`

Do not commit KSP-generated Apollo code. Regenerate locally with `./gradlew generateApolloSources`. If a generated file appears in `git status`, the Gradle setup is misconfigured — fix the config, don't commit the artifact.

---

## §13 API level compatibility

The app ships with `minSdk = 26` (Android 8.0, Oreo). Any platform API referenced unconditionally must be available at API 26, or the call site must be gated with a `Build.VERSION.SDK_INT` check and a fallback.

### §13.1 Check the API level for every platform call

Before accepting code that touches `android.*`, `java.util.*`, or Kotlin stdlib extensions that alias newer JDK methods, verify the minimum API level on the official doc. Android Studio's `NewApi` lint catches most cases, but **Kotlin extension functions that delegate to newer Java methods can escape the detector**. Review such calls manually.

### §13.2 Known traps

Concrete cases that have bitten this codebase:

- **`List.removeFirst()` / `List.removeLast()`** — Kotlin's `MutableList.removeFirst()` / `removeLast()` now resolve to `java.util.SequencedCollection` methods added in **API 35** (Android 15). They crash with `NoSuchMethodError` on older devices. Use `removeAt(0)` / `removeAt(lastIndex)` instead. See PR #84 for the `removeLast` incident on HTML parsing.
- **`PackageInfo.longVersionCode`** — API 28+. Use `androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(packageInfo)` which falls back on older devices.
- **`Icons.Filled.Reply` / `Icons.Filled.Login`** — have `AutoMirrored` replacements for RTL correctness. Not strictly API-level but same class of "newer API exists, older still works, lint warns". Prefer the `AutoMirrored` variants in new code.

### §13.3 AI-assisted code is a specific risk vector

AI code-completion tools frequently suggest API calls that match the intent but violate `minSdk`. They tend to pick the most idiomatic modern API regardless of the project's min-SDK constraint. Treat every AI-suggested platform call as requiring API-level verification, especially:

- Collection operations (Kotlin stdlib increasingly delegates to newer Java methods)
- Anything that looks like it might have a `*Compat` variant in `androidx.core` — if a Compat helper exists, it exists *because* the platform API has a version requirement.

---

## Rule index (for review comments)

| § | One-line summary |
|---|-------------------|
| §2.1 | No new `!!` |
| §3.1 | `PagingConfig` values are fixed |
| §3.3 | Post flows pass `.distinctByEffectiveId()` |
| §3.5 | Use overlay, not `invalidate()` |
| §4.1 | HTML ≥ 500 chars → `Dispatchers.Default` |
| §4.2 | Code ≥ 300 chars → `Dispatchers.Default` |
| §4.3 | Repository response mapping → `Dispatchers.Default` |
| §4.4 | File I/O → `Dispatchers.IO` |
| §5.1 | Domain models → `@Immutable` |
| §5.4 | `modifier: Modifier = Modifier` is the first optional param |
| §6.2 | UI state → `StateFlow<UiState>` |
| §6.3 | One-shot events → `Channel` / `SharedFlow` |
| §7.1 | Repository returns `Result<T>` |
| §10.1 | Dependencies → Version Catalog |
| §10.5 | Run `./gradlew :app:lintDebug` before pushing; target 0 errors |
| §11.1 | No hardcoded user-facing strings |
| §11.2 | `contentDescription` on interactive icons |
| §12.2 | Don't commit generated Apollo code |
| §13.1 | Verify API level for every platform call (minSdk = 26) |
| §13.2 | Avoid `removeFirst/Last` (API 35), use `PackageInfoCompat` (API 28) |
