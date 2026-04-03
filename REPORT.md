# AllergyGuard - Technical Report

## 1. Idea and Motivation
Traveling and exploring global cuisines is a universally beloved experience. However, for individuals with food allergies, dining in a foreign country where they do not speak the language can turn a joyful experience into a life-threatening situation. Language barriers make it incredibly difficult to read food labels, ingredient lists, or restaurant menus accurately. 

**AllergyGuard** was designed to solve this exact problem. It acts as a real-time safety net, saving users from accidental exposure to allergens. By integrating on-device optical character recognition (OCR) and translation, the app allows users to simply point their camera at a menu or label and immediately see if any of their specific dietary restrictions are present, regardless of the language barrier.

## 2. Core Features and Technical Implementation
The app is built natively in Android using Kotlin and XML, adhering to the MVVM (Model-View-ViewModel) architecture to separate UI, data, and logic layers cleanly. 

### Home Screen
The home screen serves as the central navigation hub for the user, providing a clear dashboard for the app's primary functions.
*   **UI Layout**: The interface is built using a `GridLayout` containing four `MaterialCardView` components. Each card uses a combination of `ImageView` and `TextView` wrapped in a vertical `LinearLayout` to provide a consistent visual weight. To enhance user engagement, a slide-up entrance animation is applied to the cards during the `onCreate` lifecycle hook of `MainActivity`. ViewBinding is utilized to ensure null-safe and type-safe interaction with these layout elements.
*   **Architecture & Navigation**: The app follows the **MVVM (Model-View-ViewModel)** architectural pattern. From the `MainActivity`, navigation is handled via explicit `Intent` objects. We utilize `ActivityOptionsCompat.makeSceneTransitionAnimation` to provide meaningful motion during screen transitions, making the app feel fluid and responsive.

### Scan View
This is the technical heart of the application, orchestrating complex hardware and AI interactions in real-time.
*   **Camera & Image Analysis**: Powered by the **CameraX** Jetpack library, the scan view manages a `PreviewView` and an `ImageAnalysis` use case. The analyzer is configured with `STRATEGY_KEEP_ONLY_LATEST` to avoid frame lag, processing raw `ImageProxy` data on a dedicated `Executor` thread pool (`cameraExecutor`).
*   **On-Device OCR Engine**: The app integrates the **Google ML Kit Text Recognition** API. It supports multiple scripts including Latin, Chinese, Japanese, and Korean. Recognition is performed on-device to ensure privacy and speed, returning a `Text` object containing blocks, lines, and elements along with their precise bounding boxes in pixels.
*   **Automated Translation**: To support international travelers, the `CameraScanViewModel` integrates a translation pipeline using **ML Kit's Language Identification and Translation** APIs. When the system detects non-English text, it automatically identifies the source language and translates it into English before the matching phase. This ensures that allergens written in foreign languages like "花生" (peanut) or "Lait" (milk) are correctly identified even if the user only understands English.
*   **The Scan Workflow**: The real-time detection follows a strictly optimized pipeline:
    1.  **Capture**: CameraX captures a frame and passes it to the `CameraFrameAnalyzer`.
    2.  **Recognition**: ML Kit extracts raw text blocks and their coordinates.
    3.  **Translation**: (If enabled) The raw text is translated on-device.
    4.  **Matching**: The (translated) text is normalized and checked against the enabled allergens in the ROOM database.
    5.  **Rendering**: The `CoordinateTransformer` maps the bounding boxes to the `OverlayView`, where they are drawn as red or green pulsed boxes to provide immediate visual feedback.
*   **Coordinate Transformation Pipeline**: A critical technical component is the `CoordinateTransformer`. Because the camera resolution (e.g., 1280x720) rarely matches the screen resolution, and the preview is scaled using `FILL_CENTER`, the raw OCR bounding boxes must be mathematically mapped. Our pipeline calculates scale factors and offsets, handles device rotation (90°, 180°, 270°), and applies an X-axis inversion for front-camera "mirror mode" to ensure the `OverlayView` visual indicators (red/green boxes) perfectly track the physical text.
*   **Fuzzy Matching & Synonyms**: Raw OCR strings are first normalized (lowercase, punctuation removed) using Regex. We then apply an **Allergen Matching Algorithm** based on **Levenshtein Distance**. This allows the system to detect "Peanut" even if the OCR returns "PeAnu t". The matching sensitivity is dynamically adjusted based on the length of the allergen word (e.g., 0-distance for short words, up to 2-distance for long words like 'Gluten-free'). This logic is integrated with a synonym dictionary stored in Room, allowing "Milk" to trigger for "Dairy" as well.

### Manage Allergies View
This screen allows users to define their safety profile through a robust data-driven interface.
*   **Reactive Data Flow**: The list of allergens is stored in a **Room Database**. We expose the data using **Kotlin Coroutine Flows**, which are collected in the `AllergenListViewModel`. This creates a reactive chain where any database change (e.g., toggling a checkbox) automatically triggers a UI update via `collectLatest`.
*   **Persistence & Seed Data**: Upon first launch, the database is pre-populated with common allergens (Dairy, Nuts, Seafood, etc.) using a JSON-based seeding strategy. Users can also add "Custom Allergens" and "Allergen Aliases" (synonyms).
*   **UI Efficiency**: The list is managed by a `RecyclerView` using a `ListAdapter` with `DiffUtil`. This ensures that only the items that change are re-bound, providing jank-free scrolling even with dozens of custom entries.

### Scan History & Map View
Provides a historical log and geographic context for where allergens were encountered.
*   **Automated Logging**: Every time a scan detects an allergen and the save scan button is pressed, a `ScanResult` entity is saved to the local database. Each entry includes a timestamp, the target text, and a boolean flag for allergen presence.
*   **Geospatial Integration**: The app requests location permissions to use the **Google Play Services Location API**. Using `FusedLocationProviderClient`, we capture the user's coordinates at the moment of scanning.
*   **Reverse Geocoding & Visualization**: A `Geocoder` service runs on `Dispatchers.IO` to transform latitude/longitude pairs into human-readable addresses (Street, District, City). The history screen features a "View Map" button that opens **Google Maps SDK for Android**, plotting all safety-critical scans as interactive markers.

### Settings View
Manages the configuration for the app's translation and scanning features.
*   **Model Management**: Using the `TranslationManager` singleton, we handle the downloading and status tracking of each language model. This allows the app to perform real-time translation entirely offline once the models are cached, which is crucial for travelers without international roaming.

## 3. Technical Difficulties
The core scanning functionality introduced several significant technical challenges:

1. **Coordinate Mapping and UI Alignment**: The most difficult part of the implementation was aligning the bounding boxes provided by ML Kit with the real-world preview on the screen. The ML Kit image dimensions often differ from the phone's screen dimensions, and adjusting for `ScaleType.FILL_CENTER`, device rotation, and especially front-camera flipping required complex matrix math and offset calculations.
2. **Performance Optimization (Frame Throttling)**: Running OCR and fuzzy matching on every single frame heavily throttled the CPU and caused the camera preview to stutter. This was resolved by implementing a frame-skipping strategy (processing every Nth frame) and offloading the logic (Regex normalization, Levenshtein distance calculations) to dedicated Kotlin Coroutine threads (`Dispatchers.IO` and `Dispatchers.Default`), strictly isolating them from the Main UI thread.
3. **OCR Noise and Accuracy**: Environmental factors like low light, crumpled packaging, or stylized menu fonts cause noisy text recognition. Tuning the fuzzy matching thresholds (handling length-dependent distance tolerance) to avoid false positives while catching true allergens required extensive trial and error.

## 4. Future Improvements
While the current app is highly functional, several features could elevate the core experience outside the scope of the current course:

1. **AI-Powered Ingredient Analysis**: Instead of simple keyword matching, the app could integrate a lightweight Large Language Model (LLM) API to infer allergen relationships. For instance, understanding that an ingredient like "Whey" or "Casein" implies "Milk"—even if the word "Milk" is missing from the label.
2. **Cloud Sync & User Accounts**: Integrating Firebase Authentication and Firestore to allow users to back up their customized allergy profiles, custom aliases, and scan history across multiple devices.
3. **Crowdsourcing / Social Community**: A feature allowing users to flag specific restaurant menus or packaged bar codes as "Allergy-Safe" or "Dangerous," creating a community-driven database of safe eating spots worldwide.
4. **Enhanced Offline Capabilities**: Automatically caching translation and OCR models based on the user's upcoming travel destinations, ensuring a fully offline experience when roaming data is unavailable.