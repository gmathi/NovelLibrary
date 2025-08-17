# Coroutine-Based API Interface Templates

This directory contains template interfaces and utilities for converting RxJava-based API interfaces to Kotlin Coroutines. These templates demonstrate best practices for the migration from RxJava 1.x to coroutines in the Novel Library app.

## Overview

The migration from RxJava to Coroutines involves several key patterns:

1. **Single<T> → suspend fun(): T** - Convert single-value operations to suspend functions
2. **Observable<T> → Flow<T>** - Convert streaming operations to Flow (implemented in repository layer)
3. **Error Handling** - Replace RxJava error operators with try-catch and exception mapping
4. **Threading** - Replace Schedulers with Dispatchers

## Files

### Core Interface Templates

#### `NovelApiService.kt`
Template interface demonstrating common API patterns converted to coroutines:
- GET requests with suspend functions
- POST requests with form data
- Error handling with Response<T> wrapper
- URL parameter and query parameter usage

#### `StreamingApiService.kt`
Template interface for streaming data patterns:
- Paginated data fetching
- Real-time updates (polling-based)
- Streaming content handling

### Utility Classes

#### `ApiErrorHandler.kt`
Comprehensive error handling utilities:
- Response validation and error mapping
- Exception type mapping (network errors to domain exceptions)
- Retry logic implementation
- Fallback value support

#### `CoroutineApiExtensions.kt`
Extension functions for common patterns:
- Converting suspend functions to Flow
- Polling Flow implementation
- Retry Flow patterns
- Pagination Flow helpers
- Cache-first data loading

## Conversion Patterns

### Basic API Call Conversion

**Before (RxJava):**
```kotlin
interface OldApiService {
    @GET("novels/{id}")
    fun getNovel(@Path("id") id: String): Single<Novel>
}

// Usage
apiService.getNovel("123")
    .subscribeOn(Schedulers.io())
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe(
        { novel -> handleSuccess(novel) },
        { error -> handleError(error) }
    )
```

**After (Coroutines):**
```kotlin
interface NewApiService {
    @GET("novels/{id}")
    suspend fun getNovel(@Path("id") id: String): Response<Novel>
}

// Usage
viewModelScope.launch {
    try {
        val response = apiService.getNovel("123")
        val novel = ApiErrorHandler.handleResponse(response)
        handleSuccess(novel)
    } catch (error: Exception) {
        handleError(error)
    }
}
```

### Streaming Data Conversion

**Before (RxJava):**
```kotlin
interface OldApiService {
    @GET("novels")
    fun getNovels(): Observable<List<Novel>>
}

// Usage
apiService.getNovels()
    .subscribeOn(Schedulers.io())
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe { novels -> updateUI(novels) }
```

**After (Coroutines with Flow):**
```kotlin
interface NewApiService {
    @GET("novels")
    suspend fun getNovels(): Response<List<Novel>>
}

// Repository implementation
class NovelRepository {
    fun getNovelsFlow(): Flow<List<Novel>> = flow {
        val response = apiService.getNovels()
        emit(ApiErrorHandler.handleResponse(response))
    }.flowOn(Dispatchers.IO)
}

// Usage
viewModel.novelsFlow.collect { novels ->
    updateUI(novels)
}
```

### Error Handling Conversion

**Before (RxJava):**
```kotlin
apiService.getData()
    .onErrorReturn { defaultValue }
    .onErrorResumeNext { fallbackObservable }
    .retry(3)
```

**After (Coroutines):**
```kotlin
// Using ApiErrorHandler utilities
val result = ApiErrorHandler.safeApiCallWithFallback(
    apiCall = { apiService.getData() },
    fallback = defaultValue
)

// Or with retry
val result = ApiErrorHandler.safeApiCallWithRetry(
    maxRetries = 3,
    apiCall = { apiService.getData() }
)
```

### Pagination Conversion

**Before (RxJava):**
```kotlin
fun loadAllPages(): Observable<List<Item>> {
    return Observable.range(1, Int.MAX_VALUE)
        .concatMap { page ->
            apiService.getPage(page)
                .takeUntil { it.isEmpty() }
        }
        .takeWhile { it.isNotEmpty() }
}
```

**After (Coroutines with Flow):**
```kotlin
fun loadAllPages(): Flow<List<Item>> = createPaginatedFlow { page ->
    apiService.getPage(page)
}
```

## Error Handling Strategy

### Exception Hierarchy

```
Exception
├── NetworkException (network-related errors)
├── ApiException (API response errors)
│   ├── NotFoundException (404)
│   ├── UnauthorizedException (401)
│   ├── RateLimitException (429)
│   └── ServerException (5xx)
└── Other exceptions (mapped to NetworkException)
```

### Error Mapping

The `ApiErrorHandler.mapException()` function maps common network exceptions:
- `SocketTimeoutException` → `NetworkException("Request timeout")`
- `UnknownHostException` → `NetworkException("No internet connection")`
- `IOException` → `NetworkException("Network error")`

## Testing Patterns

### Unit Testing Suspend Functions

```kotlin
@Test
fun `test api call success`() = runTest {
    val expectedData = "test"
    val response = Response.success(expectedData)
    
    val result = ApiErrorHandler.handleResponse(response)
    
    assertEquals(expectedData, result)
}
```

### Testing Flow Patterns

```kotlin
@Test
fun `test flow emission`() = runTest {
    val apiCall: suspend () -> Response<String> = { Response.success("data") }
    
    val result = apiCall.asFlow().first()
    
    assertEquals("data", result)
}
```

## Migration Checklist

When converting an RxJava API interface to coroutines:

1. **Convert method signatures:**
   - [ ] Replace `Single<T>` with `suspend fun(): Response<T>`
   - [ ] Replace `Observable<T>` with `suspend fun(): Response<List<T>>`
   - [ ] Keep `ResponseBody` for raw content

2. **Update annotations:**
   - [ ] Keep existing Retrofit annotations (@GET, @POST, etc.)
   - [ ] Keep parameter annotations (@Path, @Query, etc.)

3. **Implement error handling:**
   - [ ] Use `ApiErrorHandler.handleResponse()` for response validation
   - [ ] Use `ApiErrorHandler.safeApiCall()` for automatic error mapping
   - [ ] Implement retry logic where needed

4. **Create Flow patterns in repository:**
   - [ ] Use `flow { }` builder for streaming data
   - [ ] Use `flowOn(Dispatchers.IO)` for background execution
   - [ ] Implement pagination with `createPaginatedFlow()`

5. **Write tests:**
   - [ ] Unit tests for interface methods
   - [ ] Tests for error handling
   - [ ] Tests for Flow patterns

## Best Practices

1. **Always use Response<T> wrapper** for proper error handling
2. **Implement error mapping** to provide meaningful exceptions
3. **Use appropriate dispatchers** (IO for network operations)
4. **Implement retry logic** for transient failures
5. **Provide fallback values** where appropriate
6. **Test all error scenarios** including network failures
7. **Document conversion patterns** for team consistency

## Integration with Existing Code

These templates work with the existing coroutine infrastructure:
- `CoroutineUtils` for retry and debouncing patterns
- `CoroutineErrorHandler` for global exception handling
- `NetworkHelper` for OkHttp client configuration
- `RetrofitServiceFactory` for service creation

The templates are designed to be:
- **Drop-in replacements** for existing RxJava interfaces
- **Compatible** with existing error handling patterns
- **Testable** with standard coroutine testing utilities
- **Performant** with proper dispatcher usage