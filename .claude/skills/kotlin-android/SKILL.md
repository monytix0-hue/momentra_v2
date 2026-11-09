---
name: kotlin-android
description: Use when authoring, modifying, or reviewing Kotlin Android or Kotlin Multiplatform code, designing Jetpack Compose layouts, managing state with ViewModels or Compose remember, or writing coroutines and async flows.
---

# Kotlin Android & Jetpack Compose Engineering Guide

Idiomatic paradigms for Jetpack Compose, Kotlin Multiplatform (KMP), state management, and coroutines.

---

## 1. Jetpack Compose Layout Paradigms

### Modifier Order Matters
Modifiers are applied sequentially from left to right / top to bottom:
```kotlin
// ❌ BAD: Background does not clip to rounded corners; padding cuts ripple
Modifier
    .background(Color.White)
    .clip(RoundedCornerShape(12.dp))
    .padding(16.dp)
    .clickable { onClick() }

// ✅ GOOD: Clip first, then background, then clickable (ripple fills clip), then padding
Modifier
    .clip(RoundedCornerShape(12.dp))
    .background(MaterialTheme.colorScheme.surface)
    .clickable(onClick = onClick)
    .padding(16.dp)
```

### Layout Hierarchy & Window Insets
- **Scaffold & Insets**: Always pass and consume `innerPadding` in `Scaffold`. Never ignore it or calculate manual offsets unless explicitly implementing custom edge-to-edge drawing.
- **Root Containers**: Use `Box` with explicit alignment (`Alignment.BottomCenter`, `Alignment.TopCenter`) when pinning floating sheets or navigation bars over scrollable content.

```kotlin
@Composable
fun AppContentScreen(
    state: UiState,
    onAction: (UiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = { BottomBar(state.selectedTab, onAction) },
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            MainScrollableContent(modifier = Modifier.fillMaxSize())
            FloatingActionBanner(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            )
        }
    }
}
```

### Lazy Lists & Staggered Grids
- **Always provide stable keys**: Prevents item redraw and animation glitches.
- **Provide `contentType`**: Allows Compose to reuse layout slots between homogeneous items.
- **Defer reads with lambdas**: Avoid reading rapidly changing state (e.g., scroll offset) directly in composable arguments; wrap in `derivedStateOf`.

```kotlin
LazyColumn(
    modifier = Modifier.fillMaxSize(),
    state = listState,
) {
    items(
        items = feedItems,
        key = { item -> item.id },
        contentType = { item -> item.type },
    ) { item ->
        FeedCard(item = item)
    }
}

// Read scroll offset without triggering recomposition every single pixel
val showScrollToTop by remember {
    derivedStateOf { listState.firstVisibleItemIndex > 5 }
}
```

---

## 2. State Management & Unidirectional Data Flow (UDF)

### State Responsibility Matrix
| Scope / Purpose | Tool | Lifetime | Survives Process Death? |
|---|---|---|---|
| Transient UI animation / expanded accordion | `remember { mutableStateOf(...) }` | Composable node | No |
| User input in a textfield / transient sheet toggle | `rememberSaveable { mutableStateOf(...) }` | Configuration change / process death | Yes (via Bundle/Saver) |
| Screen business logic, data fetching, session | `ViewModel` + `StateFlow` | ViewModelStoreOwner | Yes (with `SavedStateHandle`) |

### State Hoisting Pattern
Composables should be stateless whenever possible. Pass state down, send events up:

```kotlin
// Stateless Composable (Reusable & Previewable)
@Composable
fun SearchInputBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        trailingIcon = {
            IconButton(onClick = onSearch) {
                Icon(Icons.Default.Search, contentDescription = "Search")
            }
        },
        modifier = modifier.fillMaxWidth(),
    )
}

// Stateful Host / Screen
@Composable
fun SearchRoute(
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SearchInputBar(
        query = uiState.query,
        onQueryChange = viewModel::onQueryChanged,
        onSearch = viewModel::executeSearch,
    )
}
```

### State Stability & Recomposition Optimization
- Use immutable data classes (`val` properties only).
- Avoid passing raw unstable types like `List<T>` or lambdas that capture changing values unless marked `@Immutable` or wrapped in Kotlinx immutable collections (`ImmutableList<T>`).
- Use method references or remembered lambdas: `remember(viewModel) { { viewModel.onAction() } }` to avoid breaking `equals` checks on lambda parameters.

---

## 3. Coroutines & Asynchronous Flow

### Scopes & Lifecycle Rules
- **ViewModels**: Always launch in `viewModelScope`.
- **UI Event Handlers**: Use `rememberCoroutineScope()` ONLY for user interactions (e.g., scrolling to a list position on button click, showing a `Snackbar`).
- **State-driven Async Side-effects**: Use `LaunchedEffect(key1, key2)` when a suspend call should execute when a key changes, cancelling previous runs.
- **Never launch coroutines in the composition body**: Calling `coroutineScope.launch` outside of `LaunchedEffect` or callback causes infinite launches on every recomposition.

```kotlin
// ❌ WRONG: Launching in composition body
@Composable
fun BadScreen(viewModel: MyViewModel) {
    val scope = rememberCoroutineScope()
    scope.launch { viewModel.fetchData() } // RUNS ON EVERY RECOMPOSITION!
}

// ✅ RIGHT: Controlled side-effect
@Composable
fun GoodScreen(userId: String, viewModel: MyViewModel) {
    LaunchedEffect(userId) {
        viewModel.fetchDataForUser(userId)
    }
}
```

### Lifecycle-Aware StateFlow Collection
Always collect state using `collectAsStateWithLifecycle()` in Android Compose:
```kotlin
// Prevents collecting flows and wasting resources when app is in the background
val state by viewModel.uiState.collectAsStateWithLifecycle()
```

### Dispatchers & Structured Concurrency
1. **Repository / DataSource rule**: Every suspend function must be main-safe. Switch dispatchers internally using `withContext(Dispatchers.IO)`:
   ```kotlin
   class NetworkRepository(private val api: ApiService) {
       suspend fun fetchRecords(): List<Record> = withContext(Dispatchers.IO) {
           api.getRecords().map { it.toDomain() }
       }
   }
   ```
2. **Never swallow `CancellationException`**:
   ```kotlin
   // ❌ BAD: Swallows cancellation, breaking structured concurrency
   try {
       doSuspendWork()
   } catch (e: Exception) {
       log(e)
   }

   // ✅ GOOD: Re-throw CancellationException or catch non-cancellation
   try {
       doSuspendWork()
   } catch (e: CancellationException) {
       throw e
   } catch (e: Exception) {
       log(e)
   }
   ```

---

## 4. Kotlin Multiplatform (KMP) Conventions

When sharing UI or business logic across Android & iOS:
- **Architecture**: Keep ViewModels / State Machines in `commonMain` exposing `StateFlow<UiState>` and accepting `Intent` / `Action`.
- **Resources**: Use Compose Multiplatform resources (`Res.string.*`, `Res.drawable.*`) instead of Android-specific `R.*`.
- **Platform Insets**: Rely on Compose Multiplatform `WindowInsets.safeDrawing` for safe area handling on both iOS notch/home bar and Android gesture navigation.

---

## Quick Reference Checklist

| Objective | Recommended Pattern | Avoid |
|---|---|---|
| Screen UI State | `StateFlow<UiState>` via `asStateFlow()` | LiveData, raw `MutableStateFlow` in public API |
| Local Form State | `rememberSaveable { mutableStateOf("") }` | Global variables, plain `var` |
| Screen Collection | `collectAsStateWithLifecycle()` | `collectAsState()` (leaks background work) |
| Background Work | `withContext(Dispatchers.IO)` in Repository | Calling blocking I/O on `Dispatchers.Main` |
| List Performance | `items(items, key = { it.id })` | Omitting keys in dynamic lists |
| Layout Padding | Chaining order: `clip` -> `background` -> `clickable` -> `padding` | Padding before clip (clips content incorrectly) |
