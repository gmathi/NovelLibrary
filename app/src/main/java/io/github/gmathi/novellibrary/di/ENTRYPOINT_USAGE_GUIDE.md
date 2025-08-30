# EntryPoint Usage Guide

This document explains how to use the EntryPoint interfaces for accessing Hilt dependencies in scenarios where constructor injection is not possible.

## Overview

EntryPoints provide a way to access Hilt-managed dependencies in classes that cannot use constructor injection, such as:
- Object classes
- Extension functions
- Static utility methods
- Classes not managed by Hilt

## Available EntryPoints

### NetworkEntryPoint
Provides access to network-related dependencies:
- `networkHelper()`: Returns NetworkHelper instance
- `json()`: Returns Json serializer instance

### SourceEntryPoint
Provides access to source and extension management dependencies:
- `extensionManager()`: Returns ExtensionManager instance
- `sourceManager()`: Returns SourceManager instance

### DatabaseEntryPoint
Provides access to database-related dependencies:
- `dbHelper()`: Returns DBHelper instance
- `dataCenter()`: Returns DataCenter instance

## Usage Examples

### In Object Classes

```kotlin
import dagger.hilt.android.EntryPointAccessors
import io.github.gmathi.novellibrary.di.NetworkEntryPoint

object NetworkUtility {
    private lateinit var entryPoint: NetworkEntryPoint
    
    fun initialize(context: Context) {
        entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            NetworkEntryPoint::class.java
        )
    }
    
    fun performNetworkOperation() {
        val networkHelper = entryPoint.networkHelper()
        // Use networkHelper for network operations
    }
}
```

### In Extension Functions

```kotlin
import dagger.hilt.android.EntryPointAccessors
import io.github.gmathi.novellibrary.di.SourceEntryPoint

fun Source.icon(context: Context): Drawable? {
    val entryPoint = EntryPointAccessors.fromApplication(
        context.applicationContext,
        SourceEntryPoint::class.java
    )
    return entryPoint.extensionManager().getAppIconForSource(this)
}
```

### In Utility Classes

```kotlin
import dagger.hilt.android.EntryPointAccessors
import io.github.gmathi.novellibrary.di.DatabaseEntryPoint

class DatabaseUtility {
    companion object {
        fun performDatabaseOperation(context: Context) {
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                DatabaseEntryPoint::class.java
            )
            val dbHelper = entryPoint.dbHelper()
            val dataCenter = entryPoint.dataCenter()
            // Use dependencies for database operations
        }
    }
}
```

### Safe EntryPoint Access

For production code, consider using safe access patterns:

```kotlin
object SafeEntryPointAccess {
    inline fun <reified T> safeGetEntryPoint(
        context: Context,
        entryPointClass: Class<T>
    ): T? {
        return try {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                entryPointClass
            )
        } catch (e: Exception) {
            Logs.error("EntryPoint", "Failed to access ${entryPointClass.simpleName}", e)
            null
        }
    }
}

// Usage
val entryPoint = SafeEntryPointAccess.safeGetEntryPoint(
    context, 
    NetworkEntryPoint::class.java
)
entryPoint?.let { ep ->
    val networkHelper = ep.networkHelper()
    // Use networkHelper safely
}
```

## Best Practices

1. **Initialize Early**: For object classes, initialize EntryPoints early in the application lifecycle
2. **Cache EntryPoints**: Store EntryPoint references to avoid repeated lookups
3. **Use Application Context**: Always use `context.applicationContext` to avoid memory leaks
4. **Error Handling**: Implement proper error handling for EntryPoint access failures
5. **Prefer Constructor Injection**: Use EntryPoints only when constructor injection is not possible

## Migration from Injekt

### Before (Injekt)
```kotlin
object MyUtility {
    private val networkHelper: NetworkHelper by injectLazy()
    
    fun doSomething() {
        val json = Injekt.get<Json>()
        // Use dependencies
    }
}
```

### After (Hilt EntryPoint)
```kotlin
object MyUtility {
    private lateinit var entryPoint: NetworkEntryPoint
    
    fun initialize(context: Context) {
        entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            NetworkEntryPoint::class.java
        )
    }
    
    fun doSomething() {
        val networkHelper = entryPoint.networkHelper()
        val json = entryPoint.json()
        // Use dependencies
    }
}
```

## Testing

EntryPoints can be tested using Hilt's testing utilities:

```kotlin
@HiltAndroidTest
class EntryPointTest {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)
    
    @Test
    fun testEntryPointAccess() {
        hiltRule.inject()
        val context = RuntimeEnvironment.getApplication()
        
        val entryPoint = EntryPointAccessors.fromApplication(
            context, 
            NetworkEntryPoint::class.java
        )
        
        assertNotNull(entryPoint.networkHelper())
        assertNotNull(entryPoint.json())
    }
}
```

## Performance Considerations

- EntryPoint access has minimal overhead compared to direct injection
- Cache EntryPoint references when possible to avoid repeated lookups
- EntryPoints provide singleton instances, so multiple calls return the same objects
- Use EntryPoints judiciously - prefer constructor injection when possible