# Project Structure

## Root Level Organization
```
├── app/                    # Main Android application module
├── lib/                    # Shared library module (currently minimal)
├── gradle/                 # Gradle wrapper files
├── .kiro/                  # Kiro IDE configuration and specs
├── build.gradle           # Root build configuration
├── settings.gradle        # Project settings
└── gradle.properties      # Build properties
```

## App Module Structure (`app/src/main/java/io/github/gmathi/novellibrary/`)

### Core Architecture Layers
- **`activity/`** - UI controllers and screen management
- **`fragment/`** - Reusable UI components and screens
- **`adapter/`** - RecyclerView adapters and list management
- **`viewmodel/`** - MVVM presentation layer logic

### Business Logic
- **`service/`** - Background services (download, TTS, sync, Firebase)
- **`worker/`** - WorkManager background tasks
- **`extension/`** - Plugin system for novel sources
- **`source/`** - Built-in novel source implementations

### Data Layer
- **`database/`** - SQLite database helpers and DAOs
- **`model/`** - Data models organized by layer:
  - `database/` - Database entities
  - `source/` - Source-specific models
  - `ui/` - UI-specific models
  - `other/` - Utility models
  - `preference/` - Settings models
- **`network/`** - HTTP clients, APIs, and networking utilities

### Utilities
- **`util/`** - Helper classes organized by domain:
  - `coroutines/` - Coroutine utilities and error handling
  - `network/` - Network-specific utilities
  - `storage/` - File and storage management
  - `system/` - System integration utilities
  - `view/` - UI helper functions
  - `notification/` - Notification management
  - `preference/` - Settings management

### Content Processing
- **`cleaner/`** - HTML content cleaners for different novel sources

## Package Naming Conventions
- Base package: `io.github.gmathi.novellibrary`
- Feature-based organization within each layer
- Clear separation between UI, business logic, and data layers

## Resource Organization (`app/src/main/res/`)
- **Multi-language support** with 10+ language variants
- **Flavor-specific resources** in `app/src/mirror/res/`
- **Standard Android resource structure** (layout, drawable, values, etc.)

## Test Structure
- **Unit tests**: `app/src/test/java/`
- **Integration tests**: `app/src/androidTest/java/`
- **Test organization mirrors main source structure**

## Build Variants
- **Debug/Release** build types
- **Three product flavors**: normal, mirror, canary
- **Flavor-specific source sets** for customization

## Key Files
- **`NovelLibraryApplication.kt`** - Application entry point
- **`AppModule.kt`** - Dependency injection configuration
- **`Constants.kt`** - Application-wide constants
- **`Extensions.kt`** - Kotlin extension functions

## Architectural Patterns
- **MVVM** for UI layer
- **Repository pattern** for data access
- **Dependency injection** with Injekt
- **Extension system** for pluggable novel sources
- **Clean architecture** with clear layer separation