# AllergyGuard Project TODO List

## Phase 1: Project Setup & Infrastructure
- [x] Initialize Android Studio Project (Target API 30+)
- [x] Enable ViewBinding in `build.gradle`
- [x] Add Dependencies:
    - [x] Google ML Kit (Text Recognition)
    - [x] CameraX (Core, Lifecycle, View)
    - [x] Room Persistence Library
    - [x] Kotlin Coroutines & Lifecycle components
- [x] Set up basic project package structure (data, ui, util, logic)

## Phase 2: Repository & Local Storage (Room)
- [x] Define `Allergen` Entity (id, name, isEnabled, isCustom)
- [x] Define `ScanResult` Entity (id, timestamp, textContent, hasAllergens, location)
- [x] Implement `AllergenDao` and `ScanHistoryDao`
- [x] Create `AppDatabase` singleton
- [x] Seed database with common allergens (Peanuts, Gluten, Shellfish, etc.)

## Phase 3: UI - Profile & Allergen Management
- [ ] Create `AllergenListActivity/Fragment`
- [ ] Implement `RecyclerView` for toggling allergens
- [ ] Add "Add Custom Allergen" dialog/form
- [ ] Implement `ViewModel` for state management

## Phase 4: Camera Integration (CameraX)
- [ ] Request Camera Permissions handling
- [ ] Implement `PreviewView` layout
- [ ] Set up `ProcessCameraProvider` and bind to lifecycle
- [ ] Implement Image Analysis use case (Analyzer)

## Phase 5: OCR & Detection Logic
- [ ] Integrate ML Kit `TextRecognizer` in `ImageAnalysis.Analyzer`
- [ ] Implement fuzzy matching utility (e.g., Levenshtein distance or simplified keyword filtering)
- [ ] Handle case-insensitivity and whitespace variations in detected text
- [ ] Optimize the scan loop (e.g., process every X frames)

## Phase 6: UI - Visual Overlays & Feedback
- [ ] Create `OverlayView` (Custom View) to draw bounds on top of camera
- [ ] Implement coordinate transformation (ML Kit coordinates to View coordinates)
- [ ] Logic for color-coded feedback:
    - [ ] **Red** box/text for detected allergen
    - [ ] **Green** indicator for "Safe" scan completion
- [ ] Add clear alert notification when an allergen is found

## Phase 7: History & Results
- [ ] Create `HistoryActivity/Fragment`
- [ ] Implement list of previous scans with timestamps
- [ ] (Optional) Map integration for scan locations

## Phase 8: Refinement & Testing
- [ ] Performance profiling (memory usage during OCR)
- [ ] UI Polish (Logo, theme, animations)
- [ ] Test with physical food labels under varied lighting
- [ ] Final bug fixing and cleanup
