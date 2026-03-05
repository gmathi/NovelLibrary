# Settings Module

A modern, Compose-based settings module for the Novel Library app.

## Overview

This module provides a complete settings experience built with Jetpack Compose, featuring:

- **Modern UI**: Built entirely with Jetpack Compose and Material Design 3
- **Compose Navigation**: Seamless navigation between settings screens
- **DataStore Integration**: Type-safe, reactive settings persistence
- **Modular Architecture**: Clean separation of concerns with MVVM pattern
- **Reusable Components**: Consistent UI components across all settings screens
- **Improved UX**: Better categorization and organization of settings

## Features

### Settings Categories

1. **Reader Settings** - Customize reading experience
   - Text size, font, and line spacing
   - Theme selection (light, dark, sepia, custom)
   - Scroll behavior and navigation
   - Text-to-Speech configuration

2. **Backup & Sync** - Protect your data
   - Local backup creation and restoration
   - Google Drive backup
   - Sync service configuration
   - Auto-backup settings

3. **General Settings** - App preferences
   - App theme (light, dark, system)
   - Language selection
   - Notification preferences
   - Default source and download location

4. **Advanced Settings** - Technical settings
   - Cloudflare bypass configuration
   - Network timeout settings
   - Cache management
   - Debug logging and developer options

5. **About** - App information
   - Version information
   - Contributors
   - Copyright information
   - Open source licenses

## Architecture

```
settings/
├── api/                    # Public API for app integration
│   ├── SettingsNavigator   # Navigation API
│   ├── SettingsActivity    # Standalone Activity
│   └── SettingsCallbacks   # Callback interfaces
├── ui/
│   ├── components/         # Reusable Compose components
│   ├── screens/            # Settings screens
│   └── navigation/         # Navigation graph
├── viewmodel/              # ViewModels for state management
└── data/
    ├── repository/         # Data access layer
    └── datastore/          # DataStore implementation
```

## Integration

### Quick Start (Activity-based)

For apps that haven't migrated to Compose Navigation:

```kotlin
import io.github.gmathi.novellibrary.settings.api.SettingsNavigator

// Open settings
SettingsNavigator.openSettings(context)
```

### Full Integration (Compose Navigation)

For apps using Compose Navigation:

```kotlin
import io.github.gmathi.novellibrary.settings.api.SettingsNavigator

NavHost(navController = navController, startDestination = "home") {
    composable("home") { HomeScreen() }
    
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
        onNavigateBack = { navController.popBackStack() },
        // ... other callbacks
    )
}

// Navigate to settings
SettingsNavigator.navigateToSettings(navController)
```

See [INTEGRATION.md](INTEGRATION.md) for detailed integration instructions.

## Dependencies

### Required Dependencies

- Jetpack Compose (UI, Material3, Navigation)
- DataStore Preferences
- AndroidX Core KTX
- AndroidX Lifecycle (Runtime, ViewModel)

### Module Dependencies

- `:core` - Base classes and abstractions

## Testing

The module includes comprehensive test coverage:

- **Unit Tests**: ViewModel logic, repository operations, DataStore migration
- **Property-Based Tests**: Settings persistence, data integrity
- **Compose UI Tests**: Screen rendering, user interactions, navigation

Run tests:
```bash
./gradlew :settings:test                    # Unit tests
./gradlew :settings:connectedAndroidTest    # Instrumented tests
```

## Development

### Building the Module

```bash
# Build settings module independently
./gradlew :settings:build

# Build with app module
./gradlew build
```

### Code Style

- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Document public APIs with KDoc
- Keep functions small and focused
- Prefer composition over inheritance

### Adding New Settings

1. Add setting to appropriate ViewModel
2. Update corresponding screen composable
3. Add DataStore key and accessor in SettingsDataStore
4. Write tests for new functionality
5. Update documentation

## Migration Status

This module is part of a comprehensive refactoring effort:

- ✅ Module structure created
- ✅ Reusable Compose components implemented
- ✅ DataStore infrastructure implemented
- ✅ ViewModels created
- ✅ All settings screens implemented
- ✅ Compose Navigation implemented
- ⏳ Integration with app module (in progress)
- ⏳ Activity migration from app module (pending)
- ⏳ Manual testing (pending)

## Documentation

- [Integration Guide](INTEGRATION.md) - How to integrate settings into your app
- [Design Document](../.kiro/specs/settings-module-migration/design.md) - Architecture and design decisions
- [Requirements](../.kiro/specs/settings-module-migration/requirements.md) - Feature requirements
- [Tasks](../.kiro/specs/settings-module-migration/tasks.md) - Implementation plan

## Contributing

When contributing to this module:

1. Follow the existing architecture patterns
2. Write tests for new functionality
3. Update documentation
4. Ensure all tests pass
5. Follow the code style guidelines

## License

This module is part of the Novel Library app. See the main app LICENSE file for details.

## Support

For issues or questions:
- Check the [Integration Guide](INTEGRATION.md)
- Review the [Design Document](../.kiro/specs/settings-module-migration/design.md)
- Contact the development team

## Version History

### 1.0.0 (In Development)
- Initial Compose-based implementation
- All settings screens migrated to Compose
- DataStore integration
- Compose Navigation support
- Improved settings categorization
