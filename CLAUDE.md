# CLAUDE.md — NovelLibrary

Guidance for AI assistants working on the **NovelLibrary** Android codebase.

---

## Project Overview

NovelLibrary is an Android app (Kotlin) for reading web novels from multiple sources. It supports offline downloads, TTS playback, cloud sync via Firebase, a dynamic extension system for novel sources, and Cloudflare bypass. Current version: **1.2.0** (versionCode 123), min SDK 23, target SDK 36.

---

## Repository Layout

```
NovelLibrary/
├── app/src/main/java/io/github/gmathi/novellibrary/
│   ├── activity/          # Activities (NavDrawerActivity is the main entry point)
│   ├── fragment/          # Legacy XML-based fragments
│   ├── compose/           # Jetpack Compose UI (modern, preferred for new screens)
│   │   ├── search/        # PersistentSearchView component
│   │   ├── recentnovels/  # Recent novels tab
│   │   ├── cloudflare/    # Cloudflare resolver UI
│   │   ├── common/        # LoadingView, ErrorView, EmptyView, URLImage
│   │   ├── components/    # NovelItem, RecentlyUpdatedItem
│   │   └── theme/         # Material 3 theme
│   ├── viewmodel/         # MVVM ViewModels (SearchTermViewModel, RecentNovelsViewModel, etc.)
│   ├── database/          # SQLite helpers (DBHelper singleton, NovelHelper, WebPageHelper, etc.)
│   ├── model/             # Data models (Novel, WebPage, Genre, Download, …)
│   ├── network/           # Retrofit/OkHttp networking, Cloudflare bypass, source proxies
│   ├── service/           # Background services (TTS, Download, Firebase Messaging)
│   ├── worker/            # WorkManager jobs (BackupWorker, RestoreWorker)
│   ├── extension/         # Dynamic extension system (APK-based source plugins)
│   ├── cleaner/           # Source-specific HTML cleaners (HtmlCleaner base + overrides)
│   ├── domain/usecase/    # Business logic use cases
│   ├── util/              # Constants, Utils, DataCenter (SharedPrefs), Logs, notifications
│   ├── AppModule.kt       # Injekt DI bindings
│   └── NovelLibraryApplication.kt  # App init: DI, SSL, Firebase, notifications
├── app/src/test/          # Unit tests (minimal — mostly example tests)
├── app/src/androidTest/   # Instrumented tests (minimal — mostly example tests)
├── app/src/mirror/        # Mirror flavor resources
├── docs/                  # Technical documentation (architecture, migration guides, etc.)
├── gradle/
│   └── libs.versions.toml # Version catalog — all dependency versions defined here
├── app/build.gradle       # App module Gradle config
├── build.gradle           # Root Gradle config
├── settings.gradle        # Module declarations
└── gradle.properties      # Build flags (parallel builds, config cache, R8, etc.)
```

---

## Build System

**Toolchain:** Gradle + Android Gradle Plugin (AGP) 9.1.0, Kotlin 2.3.10, KSP 2.3.6, JVM 17.

All dependency versions are centralized in `gradle/libs.versions.toml`. When adding or updating a dependency, edit the version catalog first, then reference `libs.*` aliases in `app/build.gradle`.

### Build Variants

| Flavor   | Description                                                        |
|----------|--------------------------------------------------------------------|
| `normal` | Default production build (applicationId: `io.github.gmathi.novellibrary`) |
| `mirror` | Alternative build with `.mirror` suffix — can coexist on the same device |
| `canary` | Debug build with LeakCanary enabled for memory leak detection       |

### Common Gradle Commands

```bash
# Debug build (normal flavor — the default)
./gradlew assembleNormalDebug

# Release build
./gradlew assembleNormalRelease

# Run unit tests
./gradlew test

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Lint
./gradlew lint

# Canary (memory leak detection) debug build
./gradlew assembleCanaryDebug
```

### Required Config Files (not in repo)

| File | Location | Purpose |
|------|----------|---------|
| `google-services.json` | `app/` | Firebase configuration |
| `keystore.properties` | project root | Release signing config |

Without `google-services.json` the build will fail. Download from the project's Google Drive (see `README.md`).

---

## Architecture

The app follows **MVVM** with a Repository pattern and is partway through a migration from legacy XML layouts to **Jetpack Compose**.

```
Presentation  →  ViewModel  →  Repository / UseCase  →  Database / Network
(Activity/Fragment/Compose)     (domain/usecase/)        (database/ network/)
```

### Dependency Injection

Uses **Injekt** (not Hilt/Dagger). Bindings are registered in `AppModule.kt` and resolved via `instance<T>()` or `injectLazy<T>()`. Do not introduce Hilt unless migrating the entire DI layer.

### UI: Compose vs. Legacy Views

- **New screens** should be written in Jetpack Compose using Material 3.
- **Legacy screens** use XML layouts with Data Binding / View Binding — leave them as-is unless actively migrating.
- Compose screens live under `compose/` and are hosted in Activities or `ComposeView`-based Fragments.
- The `PersistentSearchView` component (`compose/search/`) is the canonical search UI. See `docs/compose-search-quick-start.md` and `docs/compose-search-migration-guide.md`.

### Reactive Programming

The codebase uses **both** RxJava 1.x (legacy) and Kotlin Coroutines. Prefer coroutines for new code. Do not introduce RxJava 2/3.

---

## Key Classes

| Class | Role |
|-------|------|
| `NovelLibraryApplication` | App entry point: initializes Injekt, SSL/TLS, Firebase Remote Config, notification channels |
| `AppModule` | Injekt module — registers `DBHelper`, `DataCenter`, `NetworkHelper`, `ExtensionManager`, analytics |
| `NavDrawerActivity` | Main container activity (singleTask, `launchMode`) with navigation drawer |
| `DBHelper` | SQLite singleton — all database access goes through `NovelHelper`, `WebPageHelper`, etc. |
| `DataCenter` | SharedPreferences wrapper — app settings and user preferences |
| `NetworkHelper` | Configures Retrofit + OkHttp client (user-agent, Cloudflare interceptor, DoH) |
| `CloudflareInterceptor` | Bypasses Cloudflare DDoS protection on novel source requests |
| `ExtensionManager` | Loads/manages APK-based source extensions at runtime |
| `HtmlCleaner` | Base class for source-specific HTML content cleaners |
| `TTSService` | Text-to-speech background service |
| `DownloadNovelService` | Background chapter download service |
| `BackgroundNovelSyncTask` | WorkManager task for periodic novel update checks |

---

## Conventions

### Kotlin Style

- Follow the [Kotlin official code style](https://kotlinlang.org/docs/coding-conventions.html) (`kotlin.code.style=official` in `gradle.properties`).
- Use `lateinit var` for non-null fields initialized after construction; prefer `val` everywhere else.
- Extension functions are preferred over utility static methods.
- Use `Logs` (not `android.util.Log`) for all logging.

### Naming

- Activities: `*Activity.kt`
- Fragments: `*Fragment.kt`
- ViewModels: `*ViewModel.kt`
- Compose screens/components: descriptive names, no suffix required
- Database helpers: `*Helper.kt`
- Database column constants: `DBKeys.kt`

### Compose Guidelines

- Use `Material3` theming (see `compose/theme/`).
- Hoist state up to ViewModels; composables receive state and lambdas.
- Use `@Preview` annotations for all non-trivial composables.
- Common UI states (loading, error, empty) use the shared components in `compose/common/`.

### Database

- All DB access is via `DBHelper` (singleton) and its helper classes.
- Column names are defined as constants in `DBHelper` / `DBKeys` — do not hardcode strings.
- The database is **not** Room-managed (it is a raw SQLite wrapper). Do not introduce Room unless migrating the entire data layer.

### Network / Extensions

- HTTP requests go through `NetworkHelper`'s OkHttp client to ensure consistent headers, cookies, and Cloudflare bypass.
- Novel source logic lives in extensions (separate APK repository: `gmathi/NovelLibrary-Extensions`). Source-specific scraping code in `network/proxy/` and `cleaner/` is the in-app fallback layer.
- Network security config allows cleartext HTTP for novel sources — do not remove this without verifying all sources support HTTPS.

### Error Handling

- Use `try/catch` around network and DB calls; surface errors via ViewModel `StateFlow`/`LiveData` rather than crashing.
- Crashlytics is enabled for release builds — ensure non-fatal errors are logged with `FirebaseCrashlytics.getInstance().recordException(e)`.

---

## Testing

Test coverage is currently minimal. When adding new logic:
- Add unit tests under `app/src/test/` using JUnit 4.
- Use WorkManager testing helpers (`androidx.work:work-testing`) for Worker tests.
- Instrumented tests under `app/src/androidTest/` use Espresso.
- There is no mocking framework configured — add MockK or Mockito if you need mocking for new tests.

---

## Documentation

The `docs/` directory contains technical documentation. When adding a significant new feature or architectural change, add a corresponding doc and link it in `docs/README.md`.

Key docs:
- `docs/recent-novels-architecture.md` — Clean architecture reference
- `docs/compose-search-quick-start.md` — PersistentSearchView setup
- `docs/compose-search-migration-guide.md` — Migrating from legacy search

---

## Git Workflow

```
main                   ← release / stable
feature/<name>         ← feature branches merged via PR
claude/<name>          ← AI assistant branches
```

- Branch from `main` (or the designated development branch for the current task).
- Use descriptive commit messages; conventional commit style is welcome but not enforced:
  - `feat: add X`, `fix: correct Y`, `chore: update Z`
- No CI/CD is currently configured — build and test manually before pushing.

---

## Gotchas & Pitfalls

1. **`google-services.json` is required** — the build will fail without it. Keep a copy outside the repo.
2. **RxJava 1.x only** — the project uses the ancient `io.reactivex:rxjava:1.x` API. `Observable`, `Single`, etc. come from that version, not RxJava 2/3.
3. **Injekt, not Hilt** — DI is done via `Injekt`. `instance<T>()` / `injectLazy<T>()` are how dependencies are retrieved.
4. **Raw SQLite, not Room** — `DBHelper` is a manual `SQLiteOpenHelper`. Do not add `@Entity`/`@Dao` annotations unless migrating.
5. **Cleartext HTTP** — `network_security_config.xml` permits cleartext traffic for novel sources. This is intentional.
6. **Dual reactive paradigms** — Legacy code uses RxJava 1.x; new code should use Coroutines. Both coexist today.
7. **Mirror flavor path separator** — `build.gradle` uses Windows-style `\\` in `sourceSets` for the mirror flavor (`src\\mirror\\res`). This works on all platforms via Gradle's path handling but looks unusual.
8. **Extension system** — Extensions are separate APKs installed at runtime. Source-parsing changes may need to go to the [Extensions repo](https://github.com/gmathi/NovelLibrary-Extensions) rather than here.
