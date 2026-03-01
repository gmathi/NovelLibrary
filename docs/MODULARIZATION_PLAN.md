# NovelLibrary Modularization Plan

## Goal
Extract settings into a separate module to improve code organization and build times.

## Current Challenge
Settings activities depend heavily on:
- BaseActivity (from app module)
- Utilities and extensions (from app module)  
- Data models and database (from app module)
- Resources (strings, layouts, themes from app module)

## Proposed Architecture

```
novellibrary/
├── core/                    # Shared foundation
│   ├── base/               # Base classes (BaseActivity, etc.)
│   ├── util/               # Utilities and extensions
│   ├── model/              # Data models
│   └── res/                # Shared resources
│
├── app/                     # Main application
│   ├── activity/           # Main activities
│   ├── fragment/           # Fragments
│   ├── service/            # Services
│   └── depends on: core, settings
│
└── settings/                # Settings module
    ├── activity/           # Settings activities
    └── depends on: core
```

## Implementation Steps

### Phase 1: Create Core Module
1. Create `core` module
2. Move BaseActivity to core
3. Move common utilities to core
4. Move data models to core
5. Update app module to depend on core

### Phase 2: Extract Settings
1. Create `settings` module depending on core
2. Move settings activities to settings module
3. Update app module to depend on settings
4. Update AndroidManifest entries

### Phase 3: Verification
1. Build and test all modules
2. Verify no circular dependencies
3. Run app and test settings functionality

## Benefits
- Better code organization
- Faster incremental builds
- Clearer dependencies
- Easier to maintain and test
- Potential for feature modules in future
