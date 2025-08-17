# Technology Stack

## Build System
- **Gradle** with Kotlin DSL
- **Android Gradle Plugin** 8.6.1
- **Kotlin** 2.0.21 with coroutines support
- **KSP** (Kotlin Symbol Processing) for annotation processing

## Core Technologies
- **Android SDK** 35 (compile), targeting SDK 35, minimum SDK 23
- **Kotlin** as primary language with Java 17 compatibility
- **AndroidX** libraries with Jetifier enabled
- **Multi-dex** support enabled

## Key Libraries & Frameworks

### Networking
- **OkHttp** 5.0.0-alpha.2 with logging interceptor and DNS-over-HTTPS
- **Retrofit** 2.9.0 for REST API calls
- **Conscrypt** 2.5.2 for TLS 1.3 support on Android < 10

### Reactive Programming
- **RxJava** 1.3.8 and **RxAndroid** 1.2.1 (legacy, being migrated)
- **Kotlin Coroutines** 1.7.3 (current standard)

### UI & Graphics
- **Material Design Components** 1.12.0
- **Glide** 4.16.0 for image loading
- **Coil** 2.4.0 for modern image loading
- **Lottie** 6.1.0 for animations
- **PhotoView** for image viewing

### Data & Serialization
- **Gson** 2.10.1 and **Kotson** 2.5.0
- **Kotlinx Serialization** 1.6.0
- **JSoup** 1.16.1 for HTML parsing

### Firebase & Analytics
- **Firebase BOM** 33.12.0
- **Firebase Analytics, Crashlytics, Auth, Messaging, Remote Config**

### Dependency Injection
- **Injekt** 1.16.1 for lightweight DI

### Background Processing
- **WorkManager** 2.10.0 for job scheduling
- **Android Services** for long-running tasks

## Build Configuration

### Flavors
- **normal**: Standard production build
- **mirror**: Alternative package name (.mirror suffix)
- **canary**: Development build with LeakCanary

### Common Commands
```bash
# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run tests
./gradlew test

# Run Android tests
./gradlew connectedAndroidTest

# Count translations
./gradlew countTranslations
```

### Build Optimizations
- Gradle daemon enabled
- Parallel builds enabled
- 4GB heap size for builds
- Configuration cache enabled

## Code Style
- **Kotlin official** code style
- **KSP** for annotation processing instead of kapt
- **Coroutines** preferred over RxJava for new code

## Development Guidelines
- **Build Verification**: Every code change must be followed by a successful debug build check using `./gradlew assembleDebug`
- **Test Execution**: Run relevant tests after changes to ensure functionality remains intact
- **Incremental Verification**: For large changes, verify builds incrementally to catch issues early