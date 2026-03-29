# AllergyGuard

AllergyGuard is an Android application designed to help users quickly and safely identify potential food allergens on packaging and menus. By leveraging on-device text recognition (OCR) and fuzzy string matching, the app scans labels in real-time, cross-references ingredients with personalized allergen profiles, and provides immediate visual warnings.

## 🚀 Key Features

*   **Real-Time Smart Scan:** Uses the device camera to instantly read text from food labels, packaging, and menus.
*   **Custom Profile Management:** Easily manage your dietary restrictions by enabling common allergens (Peanuts, Gluten, Dairy, Shellfish, etc.) or adding custom keywords.
*   **Robust Allergen Detection:** Utilizes fuzzy string matching architecture (Levenshtein distance) to accurately detect allergens even when OCR misreads characters (e.g., catching "Peanut" from a scanned "PeAnu t").
*   **Dynamic Visual Overlays:** Draws color-coded bounding boxes directly over the camera feed—pulsing red for detected allergens and green for safe text.
*   **Scan History & Location Tracking:** Automatically logs scans into a local database. Includes optional Google Maps integration to track where you scanned safe or unsafe products.
*   **Multi-language Support:** Detects non-English text automatically and translates it to English using on-device translation models before matching against allergens.

## 🛠 Tech Stack

*   **Language:** Kotlin, XML
*   **Architecture:** MVVM (Model-View-ViewModel)
*   **UI:** ViewBinding, Material Components
*   **Camera:** AndroidX CameraX (Preview & ImageAnalysis)
*   **Machine Learning / OCR:** Google ML Kit (Text Recognition, Language Identification, Translation)
*   **Local Storage:** Room Database
*   **Concurrency:** Kotlin Coroutines & Flow
*   **Location & Maps:** Google Play Services Fused Location & Google Maps SDK

## 🏗 Architecture & Data Flow

1.  **Camera Feed:** `CameraX` captures real-time frames using an `ImageAnalysis` use case.
2.  **Text Recognition:** Frames are processed by ML Kit on a background thread.
3.  **Language Processing:** If the text is in a foreign language (e.g., Chinese, Japanese, Korean), the app dynamically downloads the required model and translates it.
4.  **Fuzzy Matching:** The extracted text is normalized, tokenized, and checked against the user's active allergens.
5.  **UI Overlay & Alerts:** A custom `OverlayView` transforms OCR coordinates to the UI space, rendering bounding boxes. Dialog alerts are emitted respecting a cooldown period.
6.  **Persistence:** Scan results are debounced and saved asynchronously to the Room database alongside GPS coordinates.

## 💻 Getting Started

### Prerequisites

*   [Android Studio](https://developer.android.com/studio)
*   An Android device or emulator with camera support.
*   A valid Google Maps API Key.

### Installation

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/xiashuidaolaoshuren/allergy_guard.git
    cd allergy_guard
    ```

2.  **Add your Google Maps API Key:**
    To enable the Map History feature, add your API key to the `local.properties` file in the project root:
    ```properties
    MAPS_API_KEY=YOUR_API_KEY_HERE
    ```

3.  **Build and Run:**
    *   Open the project in Android Studio.
    *   Sync project with Gradle files.
    *   Select your target device/emulator and click **Run** (or `Shift + F10`).
    
    Alternatively, build the debug APK via the command line:
    ```bash
    ./gradlew assembleDebug
    ```

## 🧪 Testing

*   **Run Unit Tests:**
    ```bash
    ./gradlew testDebugUnitTest
    ```
*   **Run Instrumented/UI Tests:**
    ```bash
    ./gradlew connectedAndroidTest
    ```

## 📝 About
This project was initially developed as part of a university course (CSCI3310).