# Core Module

## Purpose

The **core** module serves as the abstraction layer for the Novel Library application. It defines contracts, interfaces, and abstract base classes that establish the architectural foundation without depending on any concrete implementations from other project modules.

This module follows the **Dependency Inversion Principle**: high-level abstractions are defined here, while low-level implementations are provided by the app and settings modules through dependency injection.

## Key Principle

**Core defines "what" (interfaces and contracts), app/settings provide "how" (implementations) and "when" (wiring).**

## Module Independence

The core module has **zero dependencies** on other project modules:
- ❌ No dependency on `common` module
- ❌ No dependency on `util` module  
- ❌ No dependency on `app` module
- ❌ No dependency on `settings` module

This independence ensures that core remains a pure abstraction layer that can be depended upon by any module without creating circular dependencies.

## Dependency Structure

```
┌─────────────┐     ┌─────────────┐
│     app     │     │  settings   │
│             │     │             │
└──────┬──────┘     └──────┬──────┘
       │                   │
       └─────────┬─────────┘
                 │
         ┌───────┴────────┬────────────┐
         │                │            │
         ▼                ▼            ▼
  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐
  │    core     │  │   common    │  │    util     │
  │(abstractions│  │  (models &  │  │ (utilities) │
  │     &       │  │  adapters)  │  │             │
  │ interfaces) │  │             │  │             │
  │             │  │             │  │             │
  │ independent │  │ independent │  │ independent │
  └─────────────┘  └─────────────┘  └─────────────┘
```

**App and settings modules** depend on core, common, and util. The three foundation modules (core, common, util) are completely independent of each other.

## Contents

### Abstract Base Classes

#### 1. BaseActivity
**Location**: `io.github.gmathi.novellibrary.core.activity.BaseActivity`

Abstract base class for all activities in the application.

**Responsibilities**:
- Implements `DataAccessor` interface with abstract properties
- Defines template methods for activity lifecycle
- Provides hooks for edge-to-edge display setup
- Manages locale-aware context wrapping

**Abstract Methods**:
- `setupEdgeToEdge()`: Configure edge-to-edge display
- `applyWindowInsets()`: Handle system window insets
- `getLocaleContext(Context): Context`: Provide locale-aware context

**Abstract Properties** (from DataAccessor):
- `firebaseAnalytics: Any`
- `dataCenter: Any`
- `dbHelper: Any`
- `sourceManager: Any`
- `networkHelper: Any`

#### 2. BaseFragment
**Location**: `io.github.gmathi.novellibrary.core.fragment.BaseFragment`

Abstract base class for all fragments in the application.

**Responsibilities**:
- Implements `DataAccessor` interface with abstract properties
- Provides template methods for fragment operations
- Enables consistent dependency access across fragments

**Abstract Properties** (from DataAccessor):
- `firebaseAnalytics: Any`
- `dataCenter: Any`
- `dbHelper: Any`
- `sourceManager: Any`
- `networkHelper: Any`

#### 3. BaseSettingsActivity
**Location**: `io.github.gmathi.novellibrary.core.activity.settings.BaseSettingsActivity`

Abstract base class for settings screens, extending BaseActivity with settings-specific abstractions.

**Responsibilities**:
- Defines contract for settings data management
- Provides template methods for settings UI setup
- Handles settings item interactions

**Abstract Methods**:
- `getSettingsItems(): List<Any>`: Provide settings data
- `setupSettingsRecyclerView()`: Configure settings UI
- `onSettingsItemClick(item: Any, position: Int)`: Handle item clicks

### Interfaces

#### DataAccessor
**Location**: `io.github.gmathi.novellibrary.core.system.DataAccessor`

Interface defining the contract for accessing injected dependencies.

**Properties**:
- `firebaseAnalytics: Any`: Analytics tracking
- `dataCenter: Any`: Application preferences
- `dbHelper: Any`: Database access
- `sourceManager: Any`: Novel source management
- `networkHelper: Any`: Network operations

**Methods**:
- `getContext(): Context?`: Access to Android context

**Design Note**: Uses generic type `Any` for dependencies to avoid concrete type dependencies on other modules. Concrete types are provided by app/settings modules through dependency injection.

## Dependency Injection Pattern

The core module uses a **Template Method Pattern** combined with **Dependency Injection**:

1. **Core module** defines abstract base classes with abstract properties for dependencies
2. **App/settings modules** extend these base classes and implement abstract properties
3. **Dependency injection** (using Injekt or similar) provides concrete implementations at runtime

### Example Usage in App Module

```kotlin
class MainActivity : BaseActivity() {
    // Inject dependencies using lazy initialization
    override val firebaseAnalytics: FirebaseAnalytics by injectLazy()
    override val dataCenter: DataCenter by injectLazy()
    override val dbHelper: DBHelper by injectLazy()
    override val sourceManager: SourceManager by injectLazy()
    override val networkHelper: NetworkHelper by injectLazy()
    
    override fun setupEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }
    
    override fun applyWindowInsets() {
        // Apply insets using util module extensions
        findViewById<View>(R.id.root).applyTopSystemWindowInsetsPadding()
    }
    
    override fun getLocaleContext(context: Context): Context {
        return LocaleManager.updateContext(context)
    }
}
```

## When to Add Abstractions to Core

Add new abstractions to the core module when:

✅ **Multiple modules need the same contract**: If app and settings modules both need a common interface or base class

✅ **You want to enforce architectural patterns**: When you need to ensure consistent behavior across implementations (e.g., all activities handle edge-to-edge display)

✅ **You need dependency inversion**: When high-level modules should depend on abstractions rather than concrete implementations

✅ **You want to enable testability**: Abstract base classes make it easier to mock dependencies in tests

❌ **Don't add to core if**:
- The abstraction is only used in one module (keep it local)
- It requires dependencies on concrete types from other modules (violates independence)
- It contains business logic (core should only define contracts, not implementations)
- It's a simple data model (those belong in common module)
- It's a utility function (those belong in util module)

## Guidelines

### Adding New Base Classes

When adding a new abstract base class to core:

1. Extend from appropriate Android framework class (Activity, Fragment, etc.)
2. Implement `DataAccessor` if the class needs access to injected dependencies
3. Define abstract methods for behavior that subclasses must implement
4. Use generic types (`Any`) for dependencies to avoid concrete type dependencies
5. Document the contract clearly with KDoc comments
6. Ensure zero dependencies on other project modules

### Adding New Interfaces

When adding a new interface to core:

1. Use generic types or minimal Android framework types only
2. Avoid referencing classes from common, util, app, or settings modules
3. Document the contract and expected implementations
4. Consider if the interface should be implemented by base classes

### Testing

All base classes and interfaces in core should have:
- Unit tests verifying abstract method contracts
- Property-based tests verifying architectural properties (e.g., zero project dependencies)
- Mock implementations for testing concrete subclasses

## Build Configuration

**Namespace**: `io.github.gmathi.novellibrary.core`  
**Min SDK**: 23  
**Target SDK**: 36  
**Product Flavors**: mirror, canary, normal

**Key Dependencies**:
- AndroidX AppCompat
- AndroidX Fragment
- Firebase Analytics
- EventBus
- Injekt (dependency injection)

**No project module dependencies** - core is completely independent.

## Resources

The core module contains minimal resources:
- Layout files referenced by base classes
- String resources for common UI elements

Resources are kept minimal to maintain the abstraction layer focus.

## Package Structure

```
io.github.gmathi.novellibrary.core/
├── activity/
│   ├── BaseActivity.kt
│   └── settings/
│       └── BaseSettingsActivity.kt
├── fragment/
│   └── BaseFragment.kt
└── system/
    └── DataAccessor.kt
```

## Related Modules

- **common**: Provides independent models and adapters (no dependency relationship with core)
- **util**: Provides independent utilities and extensions (no dependency relationship with core)
- **app**: Depends on core and provides concrete implementations
- **settings**: Depends on core and provides concrete implementations

---

**Remember**: Core defines the architecture, app/settings implement it. Keep core pure, abstract, and independent.
