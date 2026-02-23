# AllergyGuard - AI Agent Instructions

## Project Overview
AllergyGuard is an Android app (Kotlin/XML) that uses OCR to scan food labels and menus in real-time, alerting users to allergens based on their profile.

## Architecture & Tech Stack
- **Language**: Kotlin (Primary), XML (UI Layouts).
- **Build System**: Gradle Kotlin DSL (`build.gradle.kts`) with Version Catalogs (`gradle/libs.versions.toml`).
- **UI Architecture**: MVVM (Model-View-ViewModel) with ViewBinding.
- **Core Libraries**: CameraX (camera control), Google ML Kit (Text Recognition), Room (local database), Coroutines (async tasks).

## Project Structure & Conventions
- **Package Organization**: Place code in `com.xiashuidaolaoshuren.allergyguard.*` under specific domains: `data` (Room entities, DAOs), `ui` (Activities, ViewModels), `util` (extensions), and `logic` (OCR, matching).
- **Dependency Management**: Always define new dependencies in `gradle/libs.versions.toml` before referencing them in `app/build.gradle.kts` via `libs.*`.
- **UI Development**: Use XML layouts and ViewBinding. Do not use `findViewById` or Jetpack Compose unless explicitly requested.

## Critical Logic & Integration Points
- **OCR & Fuzzy Matching**: The core feature relies on Google ML Kit's `TextRecognizer` inside a CameraX `ImageAnalysis.Analyzer`. Implement robust fuzzy string matching (e.g., Levenshtein distance) to handle OCR typos, case variations, and whitespace issues (e.g., detecting "Peanut" from "PeAnu t").
- **Visual Overlays**: Drawing bounding boxes requires coordinate transformation from ML Kit's image coordinates to the custom `OverlayView` coordinates. Use Red for detected allergens and Green for safe scans.
- **Performance**: Run OCR analysis and Room database queries on background Coroutine dispatchers (`Dispatchers.IO` or `Dispatchers.Default`) to avoid freezing the camera preview. Optimize the scan loop to process frames efficiently.

## Developer Workflows
- **Environment**: Code is written in **VS Code**, while building, running, and debugging are typically handled via **Android Studio**.
- **Build APK**: `./gradlew assembleDebug`
- **Run Unit Tests**: `./gradlew testDebugUnitTest`
- **Run UI Tests**: `./gradlew connectedAndroidTest`
