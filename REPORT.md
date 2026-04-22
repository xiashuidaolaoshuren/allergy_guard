# AllergyGuard - Technical Report

## 1. Idea and Motivation
Many people rely on written ingredient information when making safe food choices, but that information is not always easy to understand in every setting. Labels, menus, and packaged products may use unfamiliar wording, different languages, or formatting that makes quick manual checking difficult.

**AllergyGuard** is designed to make that process easier by giving users a fast way to inspect food text and check it against their dietary restrictions. By combining on-device optical character recognition (OCR) with translation and allergen matching, the app helps users review menus and labels more confidently in a variety of everyday and travel scenarios.

## 2. Core Features and Technical Implementation
The app is built natively in Android using Kotlin and XML, adhering to the MVVM (Model-View-ViewModel) architecture to separate UI, data, and logic layers cleanly. 

### Home Screen
The home screen serves as the central navigation hub for the user, providing a clear dashboard for the app's primary functions.
*   **UI Layout**: The interface is built using a `ConstraintLayout` containing four `MaterialCardView` components arranged in a 2x2 grid using horizontal and vertical guidelines. Each card uses a `ConstraintLayout` to overlay a centered `TextView` label on top of a highly transparent `ImageView` (watermark). The watermark icons (`ic_home_scan`, `ic_home_manage`, `ic_home_history`, and `ic_home_settings`) are tinted white and set to `android:alpha="0.2"` to provide a subtle, modern visual cue without obstructing the text readability. To enhance user engagement, a staggered slide-up entrance animation is applied to the cards during the `onCreate` lifecycle hook of `MainActivity`. ViewBinding is utilized to ensure null-safe and type-safe interaction with these layout elements.
*   **Architecture & Navigation**: The app follows the **MVVM (Model-View-ViewModel)** architectural pattern. From the `MainActivity`, navigation is handled via explicit `Intent` objects using standard intent transitions for all destinations, including the Scan View. This prevents transient shared-element animation sizing from interfering with CameraX preview initialization, ensuring reliable camera preview rendering on repeated screen entries.

### Scan View
This is the technical heart of the application, orchestrating complex hardware and AI interactions in real-time.
*   **Camera & Image Analysis**: Powered by the **CameraX** Jetpack library, the scan view manages a `PreviewView` and an `ImageAnalysis` use case. The analyzer is configured with `STRATEGY_KEEP_ONLY_LATEST` to avoid frame lag, processing raw `ImageProxy` data on a dedicated `Executor` thread pool (`cameraExecutor`).
*   **On-Device OCR Engine**: The app integrates the **Google ML Kit Text Recognition** API. It supports multiple scripts including Latin, Chinese, Japanese, and Korean. Recognition is performed on-device to ensure privacy and speed, returning a `Text` object containing blocks, lines, and elements along with their precise bounding boxes in pixels.
*   **Automated Translation**: To support international travelers, the `CameraScanViewModel` integrates a translation pipeline using **ML Kit's Language Identification and Translation** APIs. When the system detects non-English text, it automatically identifies the source language and translates it into English before the matching phase. This ensures that allergens written in foreign languages like "花生" (peanut) or "Lait" (milk) are correctly identified even if the user only understands English.
*   **The Scan Workflow**: The real-time detection follows a strictly optimized pipeline:
    1.  **Capture**: CameraX captures a frame and passes it to the `CameraFrameAnalyzer`.
    2.  **Recognition**: ML Kit extracts raw text blocks and their coordinates.
    3.  **Language Check**: The text language is identified and translated to English (if enabled).
    4.  **Normalization**: Custom substitutions (like CJK character adjustments) are applied via `CjkFoodTermSubstitutions.apply()`.
    5.  **Matching**: The text runs through full-frame and block-level checks against enabled allergens using Levenshtein fuzzy matching.
    6.  **Rendering**: The `CoordinateTransformer` maps the bounding boxes to the `OverlayView`, rendering red or green pulsed boxes to provide immediate visual feedback.
*   **Coordinate Transformation Pipeline**: A critical technical component is the `CoordinateTransformer`. Because the camera resolution (e.g., 1280x720) rarely matches the screen resolution, and the preview is scaled using `FILL_CENTER`, the raw OCR bounding boxes must be mathematically mapped. After the `CameraFrameAnalyzer` actively adjusts the source width and height based on device rotation (`rotationDegrees`), the transformer calculates scale factors and offsets, handling X-axis inversion for front-camera "mirror mode" to ensure the `OverlayView` visual indicators correctly track physical text on screen.
*   **Fuzzy Matching & Synonyms**: Raw OCR strings are first normalized (lowercase, punctuation removed) using Regex. We then apply an **Allergen Matching Algorithm** based on **Levenshtein Distance**. This allows the system to detect "Peanut" even if the OCR returns "PeAnu t". The matching sensitivity is dynamically adjusted based on the length of the allergen word (e.g., 0-distance for short words, up to 2-distance for long words like 'Gluten-free'). This logic is integrated with a dual synonym strategy: over 200+ built-in synonyms are hardcoded (`AllergenSynonymMap`), alongside dynamically fetched user-defined custom aliases from the Room database.

### Manage Allergies View
This screen allows users to define their safety profile through a robust data-driven interface.
*   **Reactive Data Flow**: The list of allergens is stored in a **Room Database**. We expose the data using **Kotlin Coroutine Flows**, which are collected directly in the `AllergenListViewModel`. This creates a reactive chain where any database change (e.g., toggling a checkbox) automatically triggers a UI update via `viewModelScope.launch` collecting the `StateFlow`.
*   **Persistence & Seed Data**: Upon first launch, the database is pre-populated with common allergens (Dairy, Nuts, Seafood, etc.) using a hardcoded Kotlin list loaded via a raw SQL execution within Room's `Callback.onCreate()` hook. Users can also add "Custom Allergens" and specific user-defined "Allergen Aliases".
*   **UI Efficiency**: The list is managed by a `RecyclerView` using a `ListAdapter` with `DiffUtil`. This ensures that only the items that change are re-bound, providing jank-free scrolling even with dozens of custom entries.

### Scan History & Map View
Provides a historical log and geographic context for where allergens were encountered.
*   **Automated Logging**: Every time a scan detects an allergen and the user initiates a save, a `ScanResult` entity is saved to the local database. The system enforces a strict 2-second cooldown (`MANUAL_SCAN_COOLDOWN_MS`) on button presses to naturally filter out unintentional duplicate saves. Each entry includes a timestamp, the target text, and a boolean flag for allergen presence.
*   **Geospatial Integration**: The app requests location permissions to use the **Google Play Services Location API**. Using `FusedLocationProviderClient`, we capture the user's coordinates at the moment of scanning.
*   **Reverse Geocoding & Visualization**: A `Geocoder` service runs on `Dispatchers.IO` to transform latitude/longitude pairs into human-readable addresses (Street, District, City). The history screen features a "View Map" button that opens **Google Maps SDK for Android**, plotting all safety-critical scans as interactive markers.

### Settings View
Manages the configuration for the app's translation and scanning features.
*   **Model Management**: Using the `TranslationManager` singleton, we handle the downloading and status tracking of each language model. This allows the app to perform real-time translation entirely offline once the models are cached, which is crucial for travelers without international roaming.
*   **Connectivity-Aware Auto-Download**: To optimize offline readiness, a dynamically-registered `ConnectivityReceiver` monitors Wi-Fi availability via `ConnectivityManager.CONNECTIVITY_ACTION`. When a Wi-Fi connection is detected while the settings screen is active, the app automatically checks for any undownloaded translation models. If pending models exist, a Material Snackbar prompts the user to "Download All," triggering a batch download through the `TranslationManager`. This demonstrates reactive UI patterns and lifecycle-aware broadcast management (registering in `onStart` and unregistering in `onStop`).

### Notification & Boot Architecture
Beyond the main UI, the app utilizes Android's broadcasting mechanism to provide background engagement and system-level integration.
*   **Boot Reminder Notification**: A manifest-registered `BootReceiver` listens for the `android.intent.action.BOOT_COMPLETED` system broadcast. Upon device restart, it triggers the `NotificationHelper` to post a daily reminder ("Don't forget to scan your food today!") to the user's notification tray. This ensures the app remains "top-of-mind" for users managing chronic allergies.
*   **Notification Channels**: The app establishes a structured `NotificationHelper` utility that defines two distinct channels: `CHANNEL_REMINDERS` (Importance: Default) for engagement prompts and `CHANNEL_ALLERGEN_ALERTS` (Importance: High) reserved for future real-time safety warnings.
*   **Runtime Permission Safety**: For devices running Android 13+ (API 33), the `POST_NOTIFICATIONS` runtime permission is gracefully handled. The request is deferred until `onWindowFocusChanged` in `MainActivity` to ensure the permission dialog does not interfere with the initial splash screen transition, avoiding potential startup deadlocks.

## 3. Technical Difficulties
The core scanning and broadcasting functionality introduced several significant technical challenges:

1. **Coordinate Mapping and UI Alignment**: The most difficult part of the implementation was aligning the bounding boxes provided by ML Kit with the real-world preview on the screen. The ML Kit image dimensions often differ from the phone's screen dimensions, and adjusting for `ScaleType.FILL_CENTER`, device rotation, and especially front-camera flipping required complex matrix math and offset calculations.
2. **Performance Optimization (Frame Throttling)**: Running OCR and fuzzy matching on every single frame heavily throttled the CPU and caused the camera preview to stutter. This was resolved by implementing a frame-skipping strategy (processing every Nth frame) and offloading the logic (Regex normalization, Levenshtein distance calculations) to dedicated Kotlin Coroutine threads (`Dispatchers.IO` and `Dispatchers.Default`), strictly isolating them from the Main UI thread.
3. **OCR Noise and Accuracy**: Environmental factors like low light, crumpled packaging, or stylized menu fonts cause noisy text recognition. Tuning the fuzzy matching thresholds (handling length-dependent distance tolerance) to avoid false positives while catching true allergens required extensive trial and error.

## 4. Future Improvements
While the current app is highly functional, several features could elevate the core experience. These improvements are considered out of the scope of the current course but represent potential directions for professional-grade development:

1. **AI-Powered Ingredient Analysis**: Instead of simple keyword matching, the app could integrate a lightweight Large Language Model (LLM) API to infer allergen relationships. For instance, understanding that an ingredient like "Whey" or "Casein" implies "Milk"—even if the word "Milk" is missing from the label.
2. **Advanced Performance Optimization**: While current frame-skipping works, implementing a more sophisticated backpressure strategy or using GPU-accelerated computing (via RenderScript or Vulkan) could allow for processing higher-resolution frames with even lower latency.
3. **Expanded Language and Script Support**: Integration of additional OCR scripts (such as Arabic, Devanagari, or Cyrillic) and broader translation model support would make the app truly global, covering almost all major travel destinations.
4. **Enhanced Offline Capabilities**: Implementing a "Travel Pack" feature that automatically pre-caches all necessary OCR and translation models based on a user's upcoming travel destinations, ensuring a seamless experience even in remote areas with zero connectivity.

## 5. Build Instructions
To facilitate testing by the course teaching team, the following environment and steps are required:

### Development Environment
- **IDE**: Developed in Android Studio Panda 2 (2025.3.2).
- **JDK**: Java 11
- **Gradle**: 8.13.2
- **SDK Targets**: Compile/Target SDK 36, Min SDK 30.

### Setup & Build
1.  **Clone & Open**: Open the project folder in Android Studio and allow Gradle to sync.
2.  **API Keys**: A Google Maps SDK API key is required for the Map History feature. Add it to `local.properties`:
    ```properties
    MAPS_API_KEY=YOUR_KEY_HERE
    ```
3.  **Build APK**: Run `./gradlew assembleDebug` from the terminal or use the "Build APK" task in Android Studio.
4.  **No External Dependencies**: This is a standalone Android project. There are no external Java clients or backend servers to install; all ML models and database logic are on-device.

### Testing Prerequisites
- **Device/Emulator**: Use a physical device or an emulator with **Camera support** and **Services** (e.g., Google Play Services) enabled.
- **Initial Sync**: When testing translation, ensure the device has an internet connection initially to download the ML models (triggered via Wi-Fi notification in Settings).

## 6. Reflection and Project Status
### Comparison with Proposal
The final implementation successfully delivers all 5 core features promised in the proposal:
- **Profile Management**: Fully implemented with Room database and custom keywords.
- **Smart Scan**: Implemented using CameraX and ML Kit.
- **Allergen Detection**: Implemented with length-adaptive Levenshtein fuzzy matching.
- **Visual Alerts**: Delivered via pulsing bounding boxes.
- **History Log**: Implemented with Room and integrated with Google Maps.

### Deviations and Extensions
The project **exceeded the original scope** in several key areas to improve real-world utility:
- **On-Device Translation**: Added automatic language identification and translation for CJK scripts, which was not explicitly detailed in the proposal.
- **Broadcasting Mechanism**: Implemented `BootReceiver` for user engagement and `ConnectivityReceiver` for model management.
- **Offline-First Choice**: We intentionally deviated from a potential client-server architecture in favor of a purely on-device design to maximize user privacy and reliability for travelers.