# Common Module

## Purpose

The **common** module provides independent, reusable models, adapters, and UI components that are shared across the Novel Library application. It contains simple data structures and presentation components without business logic or app-specific dependencies.

This module focuses on **data models** and **UI adapters** that multiple modules can use without coupling to concrete implementations.

## Module Independence

The common module has **zero dependencies** on other project modules:
- ❌ No dependency on `core` module
- ❌ No dependency on `util` module
- ❌ No dependency on `app` module
- ❌ No dependency on `settings` module

This independence ensures that common remains a pure data and presentation layer that can be used by any module without creating circular dependencies.

## Contents

### Model Classes

#### 1. SettingItem
**Location**: `io.github.gmathi.novellibrary.common.model.SettingItem`

Generic data model for settings list items with type-safe callbacks.

**Type Parameters**:
- `T`: Context type for callbacks (typically the activity/fragment)
- `V`: View binding type for the item

**Properties**:
- `name: Int`: Resource ID for the setting name
- `description: Int`: Resource ID for the setting description
- `bindCallback: SettingItemBindingCallback<T, V>?`: Callback for binding data to views
- `clickCallback: SettingItemClickCallback<T, V>?`: Callback for handling clicks

**Methods**:
- `onBind(closure: SettingItemBindingCallback<T, V>?): SettingItem<T, V>`: Set bind callback
- `onClick(closure: SettingItemClickCallback<T, V>?): SettingItem<T, V>`: Set click callback

**Type Aliases**:
- `ListitemSetting<T>`: Convenience alias for `SettingItem<T, ListitemTitleSubtitleWidgetBinding>`

**Usage Example**:
```kotlin
val settingItem = SettingItem<MyActivity, ListitemTitleSubtitleWidgetBinding>(
    name = R.string.setting_name,
    description = R.string.setting_description
).onBind { item, binding, position ->
    binding.title.text = getString(item.name)
    binding.subtitle.text = getString(item.description)
}.onClick { item, position ->
    // Handle click
}
```

#### 2. ReaderMenu
**Location**: `io.github.gmathi.novellibrary.common.model.ReaderMenu`

Simple data model for reader menu items.

**Properties**:
- `icon: Drawable`: Menu item icon
- `title: String`: Menu item title

**Usage**: Used to represent menu options in the reader interface.

### Adapters

#### GenericAdapter
**Location**: `io.github.gmathi.novellibrary.common.adapter.GenericAdapter`

Reusable RecyclerView adapter for displaying lists with custom layouts and view binding.

**Type Parameters**:
- `T`: Data item type

**Constructor Parameters**:
- `items: ArrayList<T>`: List of items to display
- `layoutResId: Int`: Layout resource ID for each item
- `listener: Listener<T>`: Callback interface for binding and clicks
- `loadMoreListener: LoadMoreListener?`: Optional pagination support

**Key Features**:
- Dynamic data updates with efficient notifications
- Item click handling
- Pagination support with load more functionality
- Item manipulation (add, remove, update, move)
- Drag and drop support

**Methods**:
- `updateData(newItems: ArrayList<T>)`: Replace all items
- `addItems(newItems: ArrayList<T>)`: Append items to the list
- `updateItem(item: T)`: Update a specific item
- `updateItemAt(index: Int, item: T)`: Update item at position
- `removeItem(item: T)`: Remove a specific item
- `removeItemAt(position: Int)`: Remove item at position
- `removeAllItems()`: Clear all items
- `insertItem(item: T, position: Int)`: Insert item at position
- `onItemMove(fromPosition: Int, toPosition: Int)`: Move item (for drag and drop)
- `onItemDismiss(position: Int)`: Dismiss item (for swipe to dismiss)

**Listener Interface**:
```kotlin
interface Listener<in T> {
    fun bind(item: T, itemView: View, position: Int)
    fun bind(item: T, itemView: View, position: Int, payloads: MutableList<Any>?)
    fun onItemClick(item: T, position: Int)
}
```

**LoadMoreListener Interface**:
```kotlin
interface LoadMoreListener {
    var currentPageNumber: Int
    val preloadCount: Int
    val isPageLoading: AtomicBoolean
    fun loadMore()
}
```

**Usage Example**:
```kotlin
val adapter = GenericAdapter(
    items = ArrayList(myItems),
    layoutResId = R.layout.my_item_layout,
    listener = object : GenericAdapter.Listener<MyItem> {
        override fun bind(item: MyItem, itemView: View, position: Int) {
            // Bind data to views
            itemView.findViewById<TextView>(R.id.title).text = item.title
        }
        
        override fun onItemClick(item: MyItem, position: Int) {
            // Handle click
        }
    }
)
recyclerView.adapter = adapter
```

### Extensions

#### ViewGroup Extensions
**Location**: `io.github.gmathi.novellibrary.common.extensions.ViewGroupExt`

Extension functions for ViewGroup operations.

**Functions**:
- `ViewGroup.inflate(@LayoutRes layout: Int, attachToRoot: Boolean = false): View`: Inflates a layout resource into a View

**Usage**: Used by GenericAdapter to inflate item layouts efficiently.

### UI Components

The common module contains shared UI layouts:
- `listitem_title_subtitle_widget.xml`: Standard list item with title and subtitle
- `listitem_progress_bar.xml`: Progress indicator for pagination

## When to Add Models to Common

Add new models to the common module when:

✅ **Simple data structures**: The model is a simple data class without business logic

✅ **Shared across modules**: Multiple modules (app, settings) need the same model

✅ **No dependencies**: The model doesn't depend on app-specific classes or business logic

✅ **Presentation layer**: The model is used for UI presentation (list items, menu items, etc.)

✅ **Independent**: The model can exist without knowing about core abstractions or util functions

❌ **Don't add to common if**:
- The model contains business logic (keep it in app module)
- The model depends on database entities or network models (keep it in app module)
- The model is only used in one module (keep it local)
- The model requires dependencies on core, util, or app modules (violates independence)

## When to Add Adapters to Common

Add new adapters to the common module when:

✅ **Generic and reusable**: The adapter can work with any data type through generics

✅ **No business logic**: The adapter only handles presentation, not business rules

✅ **Shared patterns**: Multiple modules use the same adapter pattern

❌ **Don't add to common if**:
- The adapter contains app-specific business logic
- The adapter depends on specific database or network operations
- The adapter is only used in one module

## Guidelines

### Adding New Models

When adding a new model to common:

1. Keep it simple - data classes with properties only
2. Use resource IDs for strings (not hardcoded strings)
3. Avoid business logic - models should be pure data
4. Use generic types where appropriate for flexibility
5. Document the model's purpose and usage with KDoc comments

### Adding New Adapters

When adding a new adapter to common:

1. Use generics to make it reusable across different data types
2. Provide listener interfaces for callbacks (binding, clicks)
3. Support efficient data updates with proper notify calls
4. Keep business logic out - adapters should only handle presentation
5. Document usage examples in KDoc comments

### Adding New Extensions

When adding extension functions to common:

1. Keep them focused on UI/presentation concerns
2. Avoid dependencies on app-specific classes
3. Make them generic and reusable
4. Document parameters and return values clearly

## Build Configuration

**Namespace**: `io.github.gmathi.novellibrary.common`  
**Min SDK**: 23  
**Target SDK**: 36  
**Product Flavors**: mirror, canary, normal

**Key Dependencies**:
- Material Design Components
- AndroidX RecyclerView
- AndroidX AppCompat
- ViewBinding

**No project module dependencies** - common is completely independent.

## Resources

The common module contains:
- Layout files for shared UI components
- Drawable resources referenced by adapters and models
- Minimal string resources (most strings should be in app module)

## Package Structure

```
io.github.gmathi.novellibrary.common/
├── adapter/
│   └── GenericAdapter.kt
├── extensions/
│   └── ViewGroupExt.kt
├── model/
│   ├── ReaderMenu.kt
│   └── SettingItem.kt
└── ui/
    └── [shared UI components]
```

## Testing

The common module includes:
- Unit tests for GenericAdapter data operations
- Unit tests for model creation and callbacks
- Tests verifying zero project dependencies

## Related Modules

- **core**: Provides abstractions and base classes (no dependency relationship with common)
- **util**: Provides utilities and extensions (no dependency relationship with common)
- **app**: Depends on common and uses its models and adapters
- **settings**: Depends on common and uses its models and adapters

---

**Remember**: Common provides simple, reusable data structures and presentation components. Keep it independent, generic, and free of business logic.
