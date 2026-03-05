# Settings Module Integration Guide

This guide explains how to integrate the settings module into your app using Compose Navigation.

## Overview

The settings module provides a complete settings UI built with Jetpack Compose. It uses Compose Navigation internally and exposes a simple API for integration with your app's navigation graph.

## Prerequisites

1. Your app module must depend on the settings module:
```gradle
// app/build.gradle
dependencies {
    implementation project(':settings')
}
```

2. Your app should use Jetpack Compose and Compose Navigation:
```gradle
dependencies {
    implementation libs.androidx.navigation.compose
    implementation libs.androidx.compose.ui
    implementation libs.androidx.compose.material3
}
```

## Integration Steps

### Step 1: Create ViewModels

Create instances of the settings ViewModels. These can be created using your dependency injection framework or manually:

```kotlin
import io.github.gmathi.novellibrary.settings.viewmodel.*
import io.github.gmathi.novellibrary.settings.data.repository.SettingsRepositoryDataStore

// In your app's ViewModel factory or DI module
val settingsRepository = SettingsRepositoryDataStore(context)

val mainSettingsViewModel = MainSettingsViewModel(settingsRepository)
val readerSettingsViewModel = ReaderSettingsViewModel(settingsRepository)
val generalSettingsViewModel = GeneralSettingsViewModel(settingsRepository)
val backupSettingsViewModel = BackupSettingsViewModel(settingsRepository)
val syncSettingsViewModel = SyncSettingsViewModel(settingsRepository)
val advancedSettingsViewModel = AdvancedSettingsViewModel(settingsRepository)
```

### Step 2: Add Settings to Your Navigation Graph

In your app's main composable where you define your NavHost, add the settings graph:

```kotlin
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.gmathi.novellibrary.settings.api.SettingsNavigator

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        // Your app's screens
        composable("home") {
            HomeScreen(
                onNavigateToSettings = {
                    SettingsNavigator.navigateToSettings(navController)
                }
            )
        }
        
        // Add settings navigation graph
        SettingsNavigator.addSettingsGraph(
            navGraphBuilder = this,
            mainSettingsViewModel = mainSettingsViewModel,
            readerSettingsViewModel = readerSettingsViewModel,
            generalSettingsViewModel = generalSettingsViewModel,
            backupSettingsViewModel = backupSettingsViewModel,
            syncSettingsViewModel = syncSettingsViewModel,
            advancedSettingsViewModel = advancedSettingsViewModel,
            appVersionName = BuildConfig.VERSION_NAME,
            appVersionCode = BuildConfig.VERSION_CODE,
            onNavigateBack = {
                navController.popBackStack()
            },
            onNavigateToContributors = {
                // Navigate to contributors screen
                // This can be a screen in your app or a web view
                navController.navigate("contributors")
            },
            onNavigateToCopyright = {
                // Navigate to copyright screen
                navController.navigate("copyright")
            },
            onNavigateToLicenses = {
                // Navigate to open source licenses screen
                navController.navigate("licenses")
            },
            onOpenPrivacyPolicy = {
                // Open privacy policy (e.g., in browser)
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://yourapp.com/privacy"))
                context.startActivity(intent)
            },
            onOpenTermsOfService = {
                // Open terms of service (e.g., in browser)
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://yourapp.com/terms"))
                context.startActivity(intent)
            },
            onCheckForUpdates = {
                // Check for app updates
                // This could open the Play Store or your custom update mechanism
            }
        )
    }
}
```

### Step 3: Navigate to Settings

From any screen in your app, navigate to settings using the NavController:

```kotlin
import io.github.gmathi.novellibrary.settings.api.SettingsNavigator

@Composable
fun HomeScreen(onNavigateToSettings: () -> Unit) {
    Button(onClick = onNavigateToSettings) {
        Text("Open Settings")
    }
}

// Or directly with NavController
Button(onClick = {
    SettingsNavigator.navigateToSettings(navController)
}) {
    Text("Open Settings")
}
```

## Deep Linking to Specific Settings

You can deep link directly to a specific settings category:

```kotlin
import io.github.gmathi.novellibrary.settings.ui.navigation.SettingsRoute

// Navigate directly to reader settings
SettingsNavigator.navigateToSettingsRoute(
    navController,
    SettingsRoute.Reader.route
)

// Navigate directly to backup & sync
SettingsNavigator.navigateToSettingsRoute(
    navController,
    SettingsRoute.BackupSync.route
)
```

## Available Settings Routes

The following routes are available for deep linking:

- `SettingsRoute.Main.route` - Main settings screen (entry point)
- `SettingsRoute.Reader.route` - Reader settings
- `SettingsRoute.BackupSync.route` - Backup & Sync settings
- `SettingsRoute.General.route` - General settings
- `SettingsRoute.Advanced.route` - Advanced settings
- `SettingsRoute.About.route` - About screen

## Handling Callbacks

The settings module requires several callbacks for app-level functionality:

### Navigation Callbacks

- `onNavigateBack` - Called when user wants to exit settings
- `onNavigateToContributors` - Navigate to contributors screen
- `onNavigateToCopyright` - Navigate to copyright screen
- `onNavigateToLicenses` - Navigate to open source licenses screen

### Action Callbacks

- `onOpenPrivacyPolicy` - Open privacy policy (typically in browser)
- `onOpenTermsOfService` - Open terms of service (typically in browser)
- `onCheckForUpdates` - Check for app updates

### Implementation Examples

```kotlin
// Navigate to a screen in your app
onNavigateToContributors = {
    navController.navigate("contributors")
}

// Open a URL in browser
onOpenPrivacyPolicy = {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://yourapp.com/privacy"))
    context.startActivity(intent)
}

// Check for updates using Play Store
onCheckForUpdates = {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse("market://details?id=${context.packageName}")
    }
    context.startActivity(intent)
}
```

## Activity-Based Integration (Legacy)

If your app still uses Activities instead of Compose Navigation, you can use the legacy Activity-based API:

```kotlin
import io.github.gmathi.novellibrary.settings.api.SettingsNavigator

// Open main settings
SettingsNavigator.openMainSettings(context)

// Open specific settings screens
SettingsNavigator.openReaderSettings(context)
SettingsNavigator.openGeneralSettings(context)
```

**Note:** The Activity-based API is deprecated and will be removed in a future version. Please migrate to Compose Navigation.

## Complete Example

Here's a complete example of integrating settings into a simple app:

```kotlin
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.gmathi.novellibrary.settings.api.SettingsNavigator
import io.github.gmathi.novellibrary.settings.data.repository.SettingsRepositoryDataStore
import io.github.gmathi.novellibrary.settings.viewmodel.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Create settings repository and ViewModels
        val settingsRepository = SettingsRepositoryDataStore(applicationContext)
        val mainSettingsViewModel = MainSettingsViewModel(settingsRepository)
        val readerSettingsViewModel = ReaderSettingsViewModel(settingsRepository)
        val generalSettingsViewModel = GeneralSettingsViewModel(settingsRepository)
        val backupSettingsViewModel = BackupSettingsViewModel(settingsRepository)
        val syncSettingsViewModel = SyncSettingsViewModel(settingsRepository)
        val advancedSettingsViewModel = AdvancedSettingsViewModel(settingsRepository)
        
        setContent {
            MaterialTheme {
                AppNavigation(
                    mainSettingsViewModel = mainSettingsViewModel,
                    readerSettingsViewModel = readerSettingsViewModel,
                    generalSettingsViewModel = generalSettingsViewModel,
                    backupSettingsViewModel = backupSettingsViewModel,
                    syncSettingsViewModel = syncSettingsViewModel,
                    advancedSettingsViewModel = advancedSettingsViewModel
                )
            }
        }
    }
}

@Composable
fun AppNavigation(
    mainSettingsViewModel: MainSettingsViewModel,
    readerSettingsViewModel: ReaderSettingsViewModel,
    generalSettingsViewModel: GeneralSettingsViewModel,
    backupSettingsViewModel: BackupSettingsViewModel,
    syncSettingsViewModel: SyncSettingsViewModel,
    advancedSettingsViewModel: AdvancedSettingsViewModel
) {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                onNavigateToSettings = {
                    SettingsNavigator.navigateToSettings(navController)
                }
            )
        }
        
        SettingsNavigator.addSettingsGraph(
            navGraphBuilder = this,
            mainSettingsViewModel = mainSettingsViewModel,
            readerSettingsViewModel = readerSettingsViewModel,
            generalSettingsViewModel = generalSettingsViewModel,
            backupSettingsViewModel = backupSettingsViewModel,
            syncSettingsViewModel = syncSettingsViewModel,
            advancedSettingsViewModel = advancedSettingsViewModel,
            appVersionName = "1.0.0",
            appVersionCode = 1,
            onNavigateBack = { navController.popBackStack() },
            onNavigateToContributors = { /* TODO */ },
            onNavigateToCopyright = { /* TODO */ },
            onNavigateToLicenses = { /* TODO */ },
            onOpenPrivacyPolicy = { /* TODO */ },
            onOpenTermsOfService = { /* TODO */ },
            onCheckForUpdates = { /* TODO */ }
        )
    }
}

@Composable
fun HomeScreen(onNavigateToSettings: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Welcome to My App", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onNavigateToSettings) {
            Text("Open Settings")
        }
    }
}
```

## Troubleshooting

### Settings screens not appearing

Make sure you've added the settings graph to your NavHost using `SettingsNavigator.addSettingsGraph()`.

### ViewModels not working

Ensure you're creating ViewModels with the correct SettingsRepository instance. All ViewModels need access to the same repository instance to share state.

### Navigation not working

Verify that you're using the same NavController instance throughout your app. The NavController passed to `SettingsNavigator.navigateToSettings()` must be the same one used in your NavHost.

### Callbacks not being called

Make sure you've implemented all required callbacks when calling `addSettingsGraph()`. Check the console for any error messages.

## Best Practices

1. **Single Repository Instance**: Create one SettingsRepository instance and share it across all ViewModels
2. **ViewModel Lifecycle**: Use proper ViewModel scoping (Activity or Navigation scope) to preserve state
3. **Dependency Injection**: Consider using Hilt or Koin to manage ViewModel creation
4. **Error Handling**: Implement proper error handling in your callbacks
5. **Testing**: Test navigation flows and callback implementations

## Migration from Activity-Based Navigation

If you're migrating from the old Activity-based navigation:

1. Replace all `SettingsNavigator.openXxx(context)` calls with Compose Navigation
2. Add the settings graph to your NavHost
3. Use `SettingsNavigator.navigateToSettings(navController)` instead
4. Remove any Activity declarations from your AndroidManifest.xml (they're now in the settings module)

## Support

For issues or questions, please refer to the settings module documentation or contact the development team.
