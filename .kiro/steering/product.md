# Novel Library

Novel Library is an Android application that serves as a one-stop solution for reading novels from various online sources. The app aggregates content from multiple novel websites and provides a unified reading experience.

## Key Features
- Multi-source novel aggregation and reading
- Library management for tracking reading progress
- Download functionality for offline reading
- Extension system for adding new novel sources
- Text-to-speech support
- Firebase integration for analytics and crash reporting
- Multi-language support (10+ languages)
- Google Drive backup/sync capabilities

## Target Platform
- Android mobile application
- Minimum SDK: 23 (Android 6.0)
- Target SDK: 35 (Android 15)
- Multi-APK support with flavors (normal, mirror, canary)

## Architecture
The app follows a modular Android architecture with clear separation between:
- UI layer (Activities, Fragments, Adapters)
- Business logic (ViewModels, Services, Workers)
- Data layer (Database helpers, Network layer, Models)
- Extension system for pluggable novel sources