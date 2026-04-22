# Android CLI Usage

This project uses the Google `android` CLI tool for building, running, SDK management, and other Android development tasks.

## CLI Location

- **Executable:** `E:\AndroidCLI\android.exe`
- **Version:** 0.7.x (run `android update` to stay current)
- **Skill installed at:** `C:\Users\Govani\.kiro\skills\android-cli`

## Build Command (IMPORTANT — overrides CLI preference)

**Always use the Gradle wrapper directly for builds:**

```bash
.\gradlew.bat assembleNormalDebug
```

Do **NOT** use `android run` or `E:\AndroidCLI\android.exe run --project_dir=.` for building. The Gradle wrapper is the preferred and reliable way to build this project.

## Prefer `android` CLI for Non-Build Tasks

For tasks **other than building**, prefer the `android` CLI over raw tools:

| Task | Use this | Not this |
|------|----------|----------|
| Build | `.\gradlew.bat assembleNormalDebug` | `android run` |
| SDK management | `android sdk install` | `sdkmanager` |
| Emulator management | `android emulator create/start/stop` | `avdmanager` / `emulator` |
| Search docs | `android docs search` | manual web search |
| Project info | `android describe` | manual inspection |
| UI inspection | `android layout` | `uiautomator dump` |
| Screenshots | `android screen capture` | `adb shell screencap` |

## Common Commands

### Build & Deploy
```bash
# Build the default debug variant
.\gradlew.bat assembleNormalDebug

# Build other variants
.\gradlew.bat assembleMirrorDebug
.\gradlew.bat assembleNormalRelease
```

### SDK Management
```bash
# List installed and available packages
android sdk list --all

# Install a specific SDK platform
android sdk install "platforms/android-36"

# Update all packages
android sdk update

# Remove a package
android sdk remove <package-name>
```

### Emulator Management
```bash
# List available virtual devices
android emulator list

# Create a new emulator
android emulator create

# Start an emulator
android emulator start <avd-name>

# Stop an emulator
android emulator stop <avd-name>
```

### Documentation & Debugging
```bash
# Search Android docs
android docs search "Jetpack Compose navigation"

# Inspect UI layout of running app
android layout --pretty

# Capture a screenshot
android screen capture -o screenshot.png
```

### Project Info
```bash
# Describe project structure and build targets
android describe --project_dir=.

# Print environment info (SDK location, etc.)
android info
```

## Project-Specific Notes

- **compileSdk / targetSdk:** 36
- **minSdk:** 26
- **Build tools:** 36.0.0
- **Gradle:** 9.3.1 (wrapper)
- **Java:** 17 (compile target)
- **Product flavors:** `normal` (default), `mirror`, `canary`
- **Build types:** `debug` (default), `release`

When building via CLI, the default variant is `normalDebug`. For release builds, a signing config from `keystore.properties` is required.

## Screen Testing After UI Changes

After completing work on a specific screen, perform a visual screen test to verify the result. Follow this workflow:

### Step 1: Build & Deploy
```bash
.\gradlew.bat assembleNormalDebug
E:\AndroidCLI\android.exe run --apks app/build/outputs/apk/normal/debug/app-normal-debug.apk
```

### Step 2: Navigate to the Target Screen
Use the annotated-screenshot loop to tap through the app to the screen you changed:

1. **Capture an annotated screenshot:**
   ```bash
   E:\AndroidCLI\android.exe screen capture --annotate -o screen_annotated.png
   ```
2. **Inspect the layout tree** (optional, for element details):
   ```bash
   E:\AndroidCLI\android.exe layout --pretty
   ```
3. **Resolve a tap target** — replace `#N` with the label number of the element to tap:
   ```bash
   E:\AndroidCLI\android.exe screen resolve --screenshot screen_annotated.png --string "adb shell input tap #N"
   ```
   This outputs a command like `adb shell input tap 540 1200`. Run that command to perform the tap.
4. **Repeat** steps 1–3 until you reach the target screen.

### Step 3: Capture Final Screenshot
```bash
E:\AndroidCLI\android.exe screen capture -o screen_final.png
```

### Step 4: Diff Check (optional)
If you need to confirm only specific elements changed:
```bash
E:\AndroidCLI\android.exe layout --diff
```

### When to Run Screen Tests
- After modifying any Activity, Fragment, or Composable screen
- After changing layouts, themes, styles, or drawable resources
- After modifying navigation logic that affects which screen is shown
- When the user explicitly asks for a visual check

### Tips
- The `--annotate` flag draws labeled bounding boxes on the screenshot — each label (`#1`, `#2`, etc.) corresponds to a tappable UI element
- `screen resolve` substitutes `#N` placeholders with the center coordinates of that bounding box
- Use `layout --pretty` to get element text, content descriptions, and resource IDs when the annotated screenshot labels aren't enough context
- Clean up temporary screenshot files after testing
