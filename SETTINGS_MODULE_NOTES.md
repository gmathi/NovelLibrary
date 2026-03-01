# Settings Module Extraction - Analysis

## Current Situation

The settings activities have been moved to a separate `settings` module, but there's a circular dependency issue:
- Settings module needs to depend on app module (for BaseActivity, utilities, resources, etc.)
- App module needs to depend on settings module (to use the settings activities)

## Problem

This creates a circular dependency that Gradle cannot resolve.

## Solutions

### Option 1: Create a Core/Common Module (Recommended)
Extract shared code into a `core` module:
```
core/
  - BaseActivity
  - Utilities
  - Data models
  - Common resources

app/
  - Main app code
  - Depends on: core, settings

settings/
  - Settings activities
  - Depends on: core
```

### Option 2: Keep Settings in App Module (Current State)
Keep settings in the app module but organize them in a clear package structure:
```
app/src/main/java/io/github/gmathi/novellibrary/
  - activity/
    - settings/  (all settings activities here)
  - ...
```

### Option 3: Make Settings Truly Independent
Create a settings module that:
- Defines interfaces for app dependencies
- App module implements these interfaces
- Use dependency injection to provide implementations

## Recommendation

For this codebase, **Option 2** is the most practical:
1. Keep settings in the app module
2. Organize them in a dedicated package
3. Consider extracting to a module later when you can also extract a core module

The settings code is already well-organized in `app/src/main/java/io/github/gmathi/novellibrary/activity/settings/`
