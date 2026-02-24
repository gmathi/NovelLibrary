# Novel Library

Your one-stop Android app for reading web novels from multiple sources with offline support, customizable reading experience, and cloud sync.

| Version | Support Server |
|---------|----------------|
| 0.26.1.beta | [![Discord](https://img.shields.io/discord/339610451711361024.svg?label=discord&labelColor=7289da&color=00aa39&style=flat)](https://discord.gg/qFZX4vdEdF) |

## Features

- Read novels from multiple online sources via extensions
- Download chapters for offline reading
- Customizable reader with multiple themes and fonts
- Text-to-Speech (TTS) support with media controls
- Cloud sync across devices (Firebase integration)
- Library management with search and filtering
- Chapter tracking and reading progress
- Import reading lists from NovelUpdates
- Push notifications for novel updates
- Dark theme support
- Multi-language support

## Screenshots

[Add screenshots here to showcase the app]

## Getting Started

### Prerequisites

- Android Studio Arctic Fox or later
- JDK 17
- Android SDK with API 35
- Kotlin 2.0.21 or later

### Required Configuration Files

Before building the app, you need to download the following configuration files:

Download from: https://drive.google.com/drive/folders/0B2NcxiuA0KTIQnY0akJjb0NxWVE?usp=sharing

1. `google-services.json` - Firebase configuration file
   - Copy to: `app/` directory
   
2. `keystore.properties` - Signing configuration (for release builds)
   - Copy to: project root directory
   - Format:
     ```properties
     storeFile=path/to/keystore
     storePassword=your_store_password
     keyAlias=your_key_alias
     keyPassword=your_key_password
     ```

### Building the App

1. Clone the repository:
   ```bash
   git clone https://github.com/gmathi/NovelLibrary.git
   cd NovelLibrary
   ```

2. Add the required configuration files (see above)

3. Open the project in Android Studio

4. Sync Gradle files

5. Build and run:
   ```bash
   ./gradlew assembleNormalDebug
   ```

### Build Variants

The app has three product flavors:

- `normal` - Standard production build
- `mirror` - Alternative build with different package name (can be installed alongside normal build)
- `canary` - Development build with LeakCanary for memory leak detection

## Extensions

Novel Library uses a modular extension system to support multiple novel sources. Extensions are distributed separately.

Extensions Repository: https://github.com/gmathi/NovelLibrary-Extensions

## Tech Stack

- Kotlin
- Android Jetpack (ViewModel, LiveData, WorkManager, Room)
- Firebase (Auth, Messaging, Crashlytics, Remote Config)
- Retrofit + OkHttp for networking
- Glide & Coil for image loading
- Jsoup for HTML parsing
- RxJava for reactive programming
- Kotlin Coroutines
- Material Design Components

## Architecture

The app follows MVVM architecture pattern with:
- Repository pattern for data management
- Dependency injection using Injekt
- Background processing with WorkManager
- Local database for offline storage

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

This project is licensed under the terms specified in the [LICENSE](LICENSE) file.

## Privacy Policy

See [Privacy-Policy](Privacy-Policy) for details on data collection and usage.

## Support

- Join our Discord server: https://discord.gg/qFZX4vdEdF
- Report issues on GitHub Issues
- Check the wiki for documentation

## Acknowledgments

- All contributors who have helped improve this project
- Open source libraries used in this project (see Libraries Used in app settings)
- Novel source websites for providing content
