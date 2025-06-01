# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Common Commands

### Build and Development
- `./gradlew build` - Build the entire project
- `./gradlew assembleDebug` - Build debug APK
- `./gradlew assembleRelease` - Build release APK
- `./gradlew installDebug` - Build and install debug APK on connected device/emulator

### Testing
- `./gradlew test` - Run unit tests
- `./gradlew testDebugUnitTest` - Run debug unit tests specifically
- `./gradlew connectedAndroidTest` - Run instrumented tests on connected device/emulator
- `./gradlew testDebugUnitTest --tests "*.ExampleUnitTest"` - Run specific unit test

### Code Quality
- `./gradlew lint` - Run Android lint checks
- `./gradlew lintDebug` - Run lint on debug variant

## Architecture

This is an Android application built with:
- **Kotlin** as the primary language
- **Jetpack Compose** for modern declarative UI
- **Material Design 3** for UI components and theming
- **Single Activity Architecture** with MainActivity as the entry point

### Key Structure
- Package: `com.capricallctx.playaphotobooth`
- Main Activity: `MainActivity.kt` - Single activity hosting Compose UI
- Theme: Custom `PlayaPhotoboothTheme` in `ui.theme` package
- Target SDK: 35, Min SDK: 24

### Build Configuration
- Uses Gradle Version Catalogs (`libs.versions.toml`) for dependency management
- Kotlin target: JVM 11
- Compose BOM manages all Compose library versions
- ProGuard configured for release builds (currently disabled)

The project follows standard Android project structure with separate source sets for main code, unit tests, and instrumented tests.