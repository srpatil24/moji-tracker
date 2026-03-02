# 読書トラッカー (Dokusho Tracker) — Design Document

## Comprehensive Design & Implementation Specification

---

## 1. Project Overview

**Dokusho Tracker** is a fully offline Android application for tracking Japanese reading immersion. It allows learners to log books/visual novels they've read, track moji (character) counts, set goals, and visualize their reading progress over time through a beautiful, modern dashboard.

### Core Principles

- **Fully Offline**: Zero network dependencies. All data stored locally on-device.
- **Speed**: Instant interactions. No loading spinners for local operations.
- **Clean & Modern**: Material Design 3 (Material You) with tasteful, minimal aesthetics.
- **Motivating**: Statistics and visuals designed to encourage continued reading.
- **Frictionless Logging**: Minimize taps needed to log a new entry.

---

## 2. Technology Stack

### Framework: Jetpack Compose (Native Android — Kotlin)

**Rationale**: Jetpack Compose is Google's modern declarative UI toolkit for Android. It provides:

- Best-in-class performance for Android (no bridge overhead like cross-platform frameworks).
- Native Material Design 3 / Material You support with dynamic color theming.
- Excellent animation APIs for smooth, delightful transitions.
- First-class support for Room, Navigation, and all Jetpack libraries.
- Fully offline-capable with no web dependencies.
- Kotlin coroutines and Flow for reactive, efficient data handling.

### Architecture: MVVM + Clean Architecture

```
┌─────────────────────────────────────────────┐
│                UI Layer                      │
│   Jetpack Compose Screens + ViewModels      │
├─────────────────────────────────────────────┤
│              Domain Layer                    │
│     Use Cases / Business Logic              │
├─────────────────────────────────────────────┤
│               Data Layer                    │
│    Room Database + Repository Pattern       │
└─────────────────────────────────────────────┘
```

### Key Libraries

| Library | Purpose | Version Guidance |
|---|---|---|
| **Jetpack Compose BOM** | UI framework | Latest stable (2025.x+) |
| **Room** | Local SQLite database with type-safe queries | 2.7+ |
| **Hilt** | Dependency injection | 2.52+ |
| **Navigation Compose** | Screen navigation with bottom bar | 2.8+ |
| **Vico** | Compose-native charting (line, bar, pie) | 2.1+ |
| **Kotlin Coroutines + Flow** | Async data streams | 1.9+ |
| **Material 3** | Design system components | Latest via Compose BOM |
| **DataStore Preferences** | Lightweight key-value storage (settings/goals) | 1.1+ |
| **kotlinx-serialization** | JSON serialization for data export/import | 1.7+ |
| **Accompanist** | Compose utilities (system UI controller, etc.) | As needed |

### Build Configuration

- **Min SDK**: 26 (Android 8.0) — covers 95%+ of active devices
- **Target SDK**: 35 (latest)
- **Compile SDK**: 35
- **Kotlin**: 2.0+
- **Gradle**: Kotlin DSL with version catalogs (`libs.versions.toml`)
- **Java compatibility**: 17

---

## 3. Database Design

### 3.1 Entity: `ReadingEntry`

The core data entity representing a single logged reading.

```
TABLE reading_entries
├── id: Long (PK, auto-generate)
├── title: String (NOT NULL)
│     — For standalone books: the full book title
│     — For series entries: the series name (e.g., "ソードアート・オンライン")
├── mediaType: MediaType ENUM (NOT NULL)
│     — VN, WN, LN, NOVEL
├── isSeries: Boolean (NOT NULL, default false)
├── seriesNumber: Int? (NULLABLE)
│     — Only populated when isSeries = true
│     — The volume/installment number within the series
├── mojiCount: Long (NOT NULL)
│     — Number of Japanese characters (moji) in the entry
│     — Stored as Long to handle very large counts
├── dateFinished: LocalDate (NOT NULL)
│     — The date the user finished reading this entry
├── dateAdded: Instant (NOT NULL, default = now)
│     — Timestamp of when this entry was logged (for auditing)
├── notes: String? (NULLABLE)
│     — Optional user notes about this entry
└── UNIQUE CONSTRAINT on (title, seriesNumber, mediaType)
      — Prevents exact duplicate entries
```

### 3.2 Entity: `ReadingGoal`

Stores the user's active reading goal.

```
TABLE reading_goals
├── id: Long (PK, auto-generate)
├── goalType: GoalType ENUM (NOT NULL)
│     — MOJI or BOOKS
├── targetValue: Long (NOT NULL)
│     — Target moji count or target book count
├── startDate: LocalDate (NOT NULL)
│     — When the goal tracking begins
├── endDate: LocalDate? (NULLABLE)
│     — Optional deadline; null means open-ended
├── isActive: Boolean (NOT NULL, default true)
│     — Only one goal should be active at a time
└── createdAt: Instant (NOT NULL, default = now)
```

### 3.3 Enums

```kotlin
enum class MediaType(val displayName: String, val icon: String) {
    VN("Visual Novel", "🎮"),
    WN("Web Novel", "🌐"),
    LN("Light Novel", "📖"),
    NOVEL("Novel", "📚")
}

enum class GoalType(val displayName: String) {
    MOJI("Moji Read"),
    BOOKS("Books Read")
}
```

### 3.4 Type Converters

Room requires type converters for non-primitive types:

- `LocalDate` ↔ `Long` (epoch day)
- `Instant` ↔ `Long` (epoch milli)
- `MediaType` ↔ `String` (enum name)
- `GoalType` ↔ `String` (enum name)

### 3.5 DAO Queries

The `ReadingEntryDao` must provide:

```
— Insert / Update / Delete
insertEntry(entry): Long
updateEntry(entry): Unit
deleteEntry(entry): Unit

— All entries (Flow for reactive updates)
getAllEntries(): Flow<List<ReadingEntry>>
getAllEntriesSortedByDateDesc(): Flow<List<ReadingEntry>>
getAllEntriesSortedByDateAsc(): Flow<List<ReadingEntry>>
getAllEntriesSortedByMojiDesc(): Flow<List<ReadingEntry>>
getAllEntriesSortedByMojiAsc(): Flow<List<ReadingEntry>>
getAllEntriesSortedByTitleAsc(): Flow<List<ReadingEntry>>
getAllEntriesSortedByTitleDesc(): Flow<List<ReadingEntry>>

— Aggregations
getTotalMojiCount(): Flow<Long>
getTotalEntryCount(): Flow<Int>
getCountByMediaType(): Flow<List<MediaTypeCount>>
   — data class MediaTypeCount(val mediaType: MediaType, val count: Int)

— Series helpers
getDistinctSeriesTitles(): Flow<List<String>>
getMaxSeriesNumber(title: String, mediaType: MediaType): Flow<Int?>
checkDuplicateExists(title: String, seriesNumber: Int?, mediaType: MediaType): Flow<Boolean>

— Statistics queries
getMojiByMonth(): Flow<List<MonthlyMoji>>
   — data class MonthlyMoji(val yearMonth: String, val totalMoji: Long)
getEntriesByMonth(): Flow<List<MonthlyCount>>
   — data class MonthlyCount(val yearMonth: String, val count: Int)
getCumulativeMojiOverTime(): Flow<List<CumulativeMoji>>
   — data class CumulativeMoji(val date: LocalDate, val cumulativeMoji: Long)
getEntriesInDateRange(start: LocalDate, end: LocalDate): Flow<List<ReadingEntry>>
getMojiByMediaType(): Flow<List<MediaTypeMoji>>
   — data class MediaTypeMoji(val mediaType: MediaType, val totalMoji: Long)
getLatestEntry(): Flow<ReadingEntry?>
getAverageMojiPerEntry(): Flow<Double>
getReadingStreak(): Flow<Int>
   — Calculate consecutive months/weeks with at least one entry
```

The `ReadingGoalDao` must provide:

```
insertGoal(goal): Long
updateGoal(goal): Unit
getActiveGoal(): Flow<ReadingGoal?>
deactivateAllGoals(): Unit
```

### 3.6 Database Migration Strategy

- Version 1: Initial schema as described above.
- All future schema changes must use Room auto-migrations or manual Migration objects.
- Destructive migration must NEVER be used in production (user data loss).

---

## 4. Application Architecture

### 4.1 Package Structure

```
com.dokushotracker
├── DokushoApplication.kt          — Hilt Application class
├── MainActivity.kt                 — Single Activity host
├── data/
│   ├── local/
│   │   ├── DokushoDatabase.kt     — Room database definition
│   │   ├── dao/
│   │   │   ├── ReadingEntryDao.kt
│   │   │   └── ReadingGoalDao.kt
│   │   ├── entity/
│   │   │   ├── ReadingEntryEntity.kt
│   │   │   └── ReadingGoalEntity.kt
│   │   └── converter/
│   │       └── TypeConverters.kt
│   ├── repository/
│   │   ├── ReadingRepository.kt        — Interface
│   │   ├── ReadingRepositoryImpl.kt    — Implementation
│   │   ├── GoalRepository.kt           — Interface
│   │   └── GoalRepositoryImpl.kt       — Implementation
│   └── model/
│       ├── MediaType.kt
│       ├── GoalType.kt
│       └── StatModels.kt              — Aggregation data classes
├── domain/
│   ├── model/
│   │   ├── ReadingEntry.kt            — Domain model (clean, not DB-annotated)
│   │   └── ReadingGoal.kt
│   └── usecase/
│       ├── AddEntryUseCase.kt
│       ├── UpdateEntryUseCase.kt
│       ├── DeleteEntryUseCase.kt
│       ├── GetEntriesUseCase.kt
│       ├── GetStatisticsUseCase.kt
│       ├── GetSeriesSuggestionsUseCase.kt
│       ├── CheckDuplicateUseCase.kt
│       ├── SetGoalUseCase.kt
│       ├── GetGoalProgressUseCase.kt
│       └── ExportDataUseCase.kt
├── ui/
│   ├── theme/
│   │   ├── Theme.kt                  — Material 3 theme definition
│   │   ├── Color.kt                  — Color palette
│   │   ├── Type.kt                   — Typography scale
│   │   └── Shape.kt                  — Shape definitions
│   ├── navigation/
│   │   ├── DokushoNavGraph.kt        — NavHost definition
│   │   ├── Screen.kt                 — Sealed class for routes
│   │   └── BottomNavBar.kt           — Bottom navigation bar composable
│   ├── screens/
│   │   ├── dashboard/
│   │   │   ├── DashboardScreen.kt
│   │   │   ├── DashboardViewModel.kt
│   │   │   └── components/
│   │   │       ├── TotalMojiCard.kt
│   │   │       ├── MediaTypeBreakdown.kt
│   │   │       ├── GoalProgressCard.kt
│   │   │       ├── ReadingChart.kt
│   │   │       ├── MojiOverTimeChart.kt
│   │   │       ├── StatsHighlightsRow.kt
│   │   │       ├── ReadingStreakCard.kt
│   │   │       ├── MonthlyComparisonCard.kt
│   │   │       └── MilestoneCard.kt
│   │   ├── log/
│   │   │   ├── LogScreen.kt
│   │   │   ├── LogViewModel.kt
│   │   │   └── components/
│   │   │       ├── MediaTypeSelector.kt
│   │   │       ├── SeriesToggle.kt
│   │   │       ├── TitleInput.kt
│   │   │       ├── SeriesNumberInput.kt
│   │   │       ├── MojiCountInput.kt
│   │   │       ├── DatePickerField.kt
│   │   │       └── DuplicateWarningDialog.kt
│   │   ├── history/
│   │   │   ├── HistoryScreen.kt
│   │   │   ├── HistoryViewModel.kt
│   │   │   └── components/
│   │   │       ├── HistoryEntryCard.kt
│   │   │       ├── SortSelector.kt
│   │   │       ├── EmptyHistoryState.kt
│   │   │       └── EditEntrySheet.kt
│   │   └── settings/
│   │       ├── SettingsScreen.kt
│   │       ├── SettingsViewModel.kt
│   │       └── components/
│   │           ├── GoalSettingDialog.kt
│   │           ├── ThemeSelector.kt
│   │           ├── DefaultMojiSetting.kt
│   │           └── DataManagementSection.kt
│   └── components/                    — Shared/common composables
│       ├── DokushoTopBar.kt
│       ├── ConfirmationDialog.kt
│       ├── NumberFormatter.kt
│       └── EmptyState.kt
├── util/
│   ├── DateUtils.kt
│   ├── NumberFormatUtils.kt
│   ├── DataExporter.kt
│   └── MilestoneChecker.kt
└── di/
    ├── DatabaseModule.kt              — Hilt module providing Room DB & DAOs
    └── RepositoryModule.kt            — Hilt module binding repository impls
```

### 4.2 Dependency Injection (Hilt)

**DatabaseModule** provides:
- `DokushoDatabase` singleton
- `ReadingEntryDao` from the database
- `ReadingGoalDao` from the database

**RepositoryModule** binds:
- `ReadingRepository` interface → `ReadingRepositoryImpl`
- `GoalRepository` interface → `GoalRepositoryImpl`

All use cases are constructor-injected (Hilt handles automatically with `@Inject constructor`).

### 4.3 Data Flow

```
User Action → Composable → ViewModel → UseCase → Repository → DAO → Room/SQLite
                ↑                                                        |
                └─── Flow<State> ← StateFlow ← Flow<Data> ←─────────────┘
```

- All database queries return `Flow<T>` for reactive UI updates.
- ViewModels expose `StateFlow<UiState>` to composables.
- UI state is modeled with sealed interfaces for each screen (Loading, Success, Error).

---

## 5. Screen Designs

### 5.1 Navigation Structure

The app uses a **single Activity** with Jetpack Compose Navigation.

**Bottom Navigation Bar** with 3 primary destinations:

| Icon | Label | Route | Description |
|---|---|---|---|
| 📊 BarChart icon | Stats | `/dashboard` | Default/home screen |
| ✏️ Add/Edit icon | Log | `/log` | Quick entry logging |
| 📋 List icon | History | `/history` | Full reading history |

An additional **Settings** screen is accessible via a gear icon in the top app bar (present on all screens). Route: `/settings`.

The bottom nav bar should:
- Use Material 3 `NavigationBar` component.
- Show labels always (not just when selected).
- Use filled icons when selected, outlined when not.
- Animate selection indicator smoothly.
- Be hidden when navigating to full-screen dialogs or settings.

---

### 5.2 Dashboard Screen (Default — Statistics)

This is the **home screen** and the most visually rich. It is a vertically scrollable column.

#### Layout (Top to Bottom):

##### A. Top App Bar
- Title: "Dokusho Tracker" (or 読書トラッカー) in a clean Medium TopAppBar style.
- Settings gear icon on the right.
- Uses `MediumTopAppBar` with collapse-on-scroll behavior.

##### B. Hero Stat: Total Moji Read
- A large, prominent card spanning full width.
- Background: Subtle gradient using primary container colors.
- Content:
  - Label: "Total Moji Read" (small text, secondary color).
  - Value: The total moji count formatted with commas and displayed in a **large, bold display font** (e.g., `DisplayMedium` or `DisplayLarge` typography).
  - Format numbers Japanese-style optionally: e.g., "1,234,567文字" or just "1,234,567".
  - Subtle animated count-up effect on first load (using `Animatable` with spring spec).
- Below the number: A small label like "across X books" in tertiary text.

##### C. Media Type Breakdown Row
- A horizontally arranged row of 4 small cards/chips, one per media type.
- Each card shows:
  - The media type icon/emoji (🎮 VN, 🌐 WN, 📖 LN, 📚 Novel).
  - The media type name.
  - The count of entries for that type (bold number).
- Use `ElevatedCard` or `OutlinedCard` with rounded corners.
- Equal width distribution (weight-based) or horizontally scrollable if needed.
- Tapping a media type card could filter the dashboard charts to that type only (stretch feature).

##### D. Goal Progress Card
- Only shown if the user has set an active goal.
- Card contains:
  - Goal description: e.g., "Goal: Read 2,000,000 moji" or "Goal: Read 50 books".
  - A **linear progress bar** (Material 3 `LinearProgressIndicator`) with:
    - Rounded track and indicator.
    - Primary color fill.
    - Animated progress value.
  - Percentage text: e.g., "67% complete" shown to the right of the bar or below.
  - Current vs Target: e.g., "1,340,000 / 2,000,000 moji".
  - If the goal has a deadline: show days remaining.
  - An edit button (pencil icon) to quickly modify the goal → opens `GoalSettingDialog`.
- If no goal is set: Show a motivational prompt card: "Set a reading goal to track your progress!" with a "Set Goal" button.
- The progress bar should use `animateFloatAsState` for smooth fill animation.

##### E. Quick Stats Highlights Row
- A row of 2-3 small statistic items, displayed as compact info chips or mini-cards:
  - **Average Moji/Book**: Formatted number (e.g., "~102,000 moji/book").
  - **Last Book Finished**: Date or "X days ago" (e.g., "3 days ago" or "Feb 25, 2026").
  - **Reading Streak**: "X months active" or "X weeks active" — count of consecutive calendar months/weeks that contain at least one finished entry.
- Use `Surface` or `Card` with `tonalElevation` for subtle depth.

##### F. Moji Over Time Chart
- **Line chart** showing cumulative moji read over time.
- X-axis: Dates (formatted as months: "Jan", "Feb", etc., or more granular if short time period).
- Y-axis: Cumulative moji count (formatted with K/M suffixes for readability, e.g., "500K", "1M").
- The line should be smooth (curved/spline interpolation).
- Area under the line filled with a translucent gradient of the primary color.
- Interactive: Tapping/hovering on the chart shows a tooltip with the exact date and value.
- Use the **Vico** charting library for Compose-native rendering.
- Card wrapper with title "Cumulative Moji Read" and optional time range selector (All Time, Past Year, Past 6 Months).

##### G. Monthly Reading Activity Chart
- **Bar chart** showing the number of books/entries finished per month.
- X-axis: Months (e.g., "Jan '26", "Feb '26").
- Y-axis: Number of entries.
- Each bar can be optionally color-coded by media type (stacked bar chart).
- Card wrapper with title "Books Finished Per Month".
- Optionally toggle between "Books" and "Moji" views.

##### H. Monthly Moji Breakdown Chart (Optional / Nice-to-Have)
- **Bar chart** showing total moji read per month.
- Useful for seeing reading volume trends even if book count is the same.
- Can be combined with section G as a toggle.

##### I. Media Type Distribution
- **Donut/Pie chart** or **horizontal stacked bar** showing the proportion of each media type by count or by moji.
- Labels showing percentages.
- Use Vico or custom Canvas composable for the donut chart.

##### J. Milestone & Motivational Section
- Show milestones achieved: "🎉 You've read over 1,000,000 moji!"
- Milestones at: 100K, 250K, 500K, 1M, 2M, 5M, 10M moji; and 5, 10, 25, 50, 100 books.
- Show the next upcoming milestone: "Only 160,000 moji to 2,000,000!"
- Use a visually distinct card with a celebration accent color.

##### K. Empty State (No Data)
- When the user has no entries, the dashboard should show:
  - A friendly illustration or icon.
  - Text: "No reading data yet. Log your first book to get started!"
  - A prominent "Log Your First Book" button that navigates to the Log screen.
  - Do NOT show empty/zero charts. Only show the prompt.

---

### 5.3 Log Screen

The Log screen is designed for **speed and ease of input**. The user should be able to log a new entry in under 10 seconds for the common case.

#### Layout:

##### A. Top App Bar
- Title: "Log Reading" (or "新しい記録").
- Settings gear icon.
- Clean `TopAppBar` (not collapsing — form should be fully visible).

##### B. Form Content (Scrollable Column)

The form is a vertical column with clear spacing between fields. All fields use Material 3 `OutlinedTextField` or equivalent components.

###### 1. Media Type Selector
- **Component**: `SegmentedButtonRow` (Material 3 segmented buttons) or `FilterChip` row.
- **Options**: VN | WN | LN | Novel
- **Default**: None selected (user must choose). Or LN as a sensible default if user prefers.
- **Behavior**: Single-select. Tapping one deselects the others.
- **Visual**: Horizontally arranged, equal-width segmented buttons with filled selection state.
- This is the FIRST field because it contextualizes the rest of the form.

###### 2. Series Toggle
- **Component**: A `Switch` or `ToggleButton` with label "Part of a series".
- **Default**: Off (standalone book).
- **Behavior**: When toggled on, shows the series-specific fields (series title autocomplete and series number). When off, shows a simple title field.
- **Animation**: Series fields should animate in/out smoothly with `AnimatedVisibility` (expand vertically).

###### 3a. Title Input (Non-Series Mode)
- **Component**: `OutlinedTextField` with standard text input.
- **Label**: "Title" / "タイトル".
- **Behavior**: Free text entry. Standard keyboard.
- **Validation**: Cannot be empty.
- Shown only when series toggle is OFF.

###### 3b. Series Title Input (Series Mode)
- **Component**: `ExposedDropdownMenuBox` with `OutlinedTextField`.
- **Label**: "Series Title" / "シリーズ名".
- **Behavior**: 
  - Dropdown shows existing series titles from the database (queried via `getDistinctSeriesTitles()`).
  - Filtered as the user types (autocomplete).
  - If the user types a new name not in the list, it becomes a new series.
  - Dropdown items show the series name and the count of existing entries (e.g., "ソードアート・オンライン (3 volumes)").
- **Validation**: Cannot be empty.
- Shown only when series toggle is ON.

###### 3c. Series Number Input (Series Mode)
- **Component**: `OutlinedTextField` with number keyboard type.
- **Label**: "Volume Number" / "巻数".
- **Behavior**:
  - When a known series is selected, auto-suggest the next volume number (max existing + 1).
  - User can override the suggestion.
  - Must be a positive integer.
- **Validation**: Must be a positive integer. Check for duplicates within the series.
- Shown only when series toggle is ON.

###### 4. Moji Count Input
- **Component**: `OutlinedTextField` with number keyboard type.
- **Label**: "Moji Count" / "文字数".
- **Default Value**: 100,000 (pre-filled). This default should be configurable in Settings.
- **Behavior**:
  - Accept only numeric input.
  - Format with commas as the user types (e.g., "100,000").
  - Allow clearing and re-entering.
- **Validation**: Must be a positive number greater than 0.
- **Helper text**: Show the value in a readable format below: "100,000文字".

###### 5. Date Finished Picker
- **Component**: `OutlinedTextField` (read-only, clickable) that opens a `DatePickerDialog`.
- **Label**: "Date Finished" / "読了日".
- **Default Value**: Today's date (pre-filled with current date).
- **Behavior**:
  - Tapping the field opens Material 3 `DatePickerDialog`.
  - User selects a date (should not allow future dates).
  - Display format: "March 2, 2026" or locale-appropriate format.
- **Validation**: Cannot be empty. Cannot be in the future.

###### 6. Notes Field (Optional)
- **Component**: `OutlinedTextField` with multi-line support (2-3 lines visible).
- **Label**: "Notes (optional)" / "メモ".
- **Behavior**: Free text. Fully optional. Allows the user to jot down thoughts.
- **Max length**: 500 characters with counter.

###### 7. Submit Button
- **Component**: `FilledButton` (Material 3 primary filled button), full-width.
- **Label**: "Log Entry" / "記録する".
- **Behavior**:
  - Validates all fields before submission.
  - If validation fails: highlight invalid fields with error state and messages.
  - If a potential duplicate is detected (same title + series number + media type exists):
    - Show a `DuplicateWarningDialog` asking the user to confirm or cancel.
    - Dialog text: "An entry for [Title] (Vol. X) already exists. Add anyway?"
    - Options: "Cancel" and "Add Anyway".
  - On successful insertion:
    - Show a `Snackbar` at the bottom: "Entry logged successfully!" with an "Undo" action.
    - The undo action deletes the just-inserted entry within 5 seconds.
    - Clear the form (reset all fields to defaults) for the next entry.
    - Do NOT navigate away — the user may want to log multiple entries quickly.

##### C. Keyboard & Input Optimization
- When the form loads, do NOT auto-focus any field (let user choose where to start).
- The form must handle keyboard IME actions properly:
  - "Next" action on each field moves focus to the next field.
  - "Done" action on the last field before the button dismisses the keyboard.
- The form should scroll to keep the active field visible above the keyboard.

##### D. Quick Log Enhancement (Stretch Feature)
- A "Quick Log" button at the top that opens a minimal bottom sheet.
- Pre-fills with: last used media type, last used series (if applicable, with series number incremented), default moji count, today's date.
- One-tap logging for the common case of reading the next volume in a series.

---

### 5.4 History Screen

Displays all logged entries in a scrollable, sortable list.

#### Layout:

##### A. Top App Bar
- Title: "History" (or "履歴").
- Settings gear icon.
- **Search icon** that expands into a search bar (optional enhancement: `SearchBar` composable).

##### B. Sort Controls
- Positioned just below the top app bar, inside a horizontal scrollable row.
- **Component**: `FilterChip` or `AssistChip` row.
- **Sort Options**:
  - Date (Newest First) ← **DEFAULT, selected on load**
  - Date (Oldest First)
  - Moji (Most First)
  - Moji (Least First)
  - Title (A → Z)
  - Title (Z → A)
- **Behavior**:
  - Tapping a chip selects it and deselects others.
  - The list re-sorts immediately (animated with `animateItemPlacement` on `LazyColumn`).
  - Active chip has a filled/selected visual state.
- **Alternative Design Option**: A single "Sort" chip that opens a dropdown menu with options. This is cleaner if horizontal space is limited.
- **Additional Filter** (nice-to-have): A "Filter" chip that opens a bottom sheet allowing filtering by media type, date range, or series.

##### C. Entry List
- **Component**: `LazyColumn` for efficient scrolling of potentially hundreds of entries.
- **Item animation**: Use `animateItem()` modifier for smooth sort transitions.

###### Entry Card Design
Each entry is displayed as an `ElevatedCard` or `Card` with the following layout:

```
┌─────────────────────────────────────────────────┐
│  [VN chip]                          Feb 25, 2026│
│                                                  │
│  ソードアート・オンライン Vol. 3                    │
│                                                  │
│  📝 150,000 moji                                 │
│                                                  │
│  (Optional: "Great story arc..." - notes)        │
└─────────────────────────────────────────────────┘
```

- **Top row**: Media type as a small colored chip/badge (left) and date finished (right, secondary text).
- **Title row**: Full title in `TitleMedium` or `BodyLarge` font. For series: "Title Vol. X".
- **Moji row**: Moji count formatted with commas, with a small icon prefix.
- **Notes row**: If notes exist, show a truncated preview (1 line, ellipsis).
- **Card styling**:
  - Rounded corners (16dp).
  - Subtle elevation or tonal surface.
  - Media type chip uses a distinct color per type:
    - VN: Purple/Indigo
    - WN: Teal/Green
    - LN: Blue
    - Novel: Amber/Orange
  - Horizontal padding: 16dp. Vertical spacing between cards: 8dp.

###### Entry Interactions
- **Tap**: Opens a bottom sheet (`ModalBottomSheet`) or dialog showing full entry details with an "Edit" button.
- **Long press or swipe**: 
  - Swipe left to reveal a red "Delete" action.
  - On delete swipe: Show a confirmation dialog: "Delete [Title]? This cannot be undone."
  - After deletion: Show `Snackbar` with "Entry deleted" and "Undo" action.
- **Edit functionality**: 
  - Opens a bottom sheet pre-filled with the entry's current data.
  - Same form layout as the Log screen but in edit mode.
  - Save button updates the entry. Cancel dismisses the sheet.

##### D. Empty State
- When no entries exist:
  - Friendly illustration (e.g., a book icon with a "+" symbol).
  - Text: "No entries yet. Start logging your reading!"
  - "Log Your First Book" button navigating to the Log screen.

##### E. Performance Considerations
- Use `LazyColumn` with `key` parameter set to `entry.id` for stable item identity.
- For very large lists (1000+ entries), implement pagination or use Room's `PagingSource` with the Paging 3 library.
- Sort operations happen at the database level (different DAO queries), not in-memory, for efficiency.

---

### 5.5 Settings Screen

Accessed via the gear icon in the top app bar.

#### Layout:

##### A. Top App Bar
- Title: "Settings" (or "設定").
- Back arrow navigation.

##### B. Settings Sections

###### 1. Reading Goal Section
- **Header**: "Reading Goal" with icon.
- **Current Goal Display**: Shows the active goal if set: "Read 2,000,000 moji" or "Read 50 books".
- **"Set / Change Goal" button**: Opens `GoalSettingDialog`.
  - Dialog contains:
    - Goal type toggle: Moji / Books (segmented button).
    - Target value input field (number input).
    - Optional start date picker (defaults to today).
    - Optional end date / deadline picker.
    - Save and Cancel buttons.
  - When saving a new goal, deactivate any previous active goal.
- **"Clear Goal" button**: Deactivates the current goal (with confirmation).

###### 2. Defaults Section
- **Header**: "Defaults".
- **Default Moji Count**: Number input field showing the current default (100,000). User can change this. Stored in DataStore Preferences.
- **Default Media Type**: Optional dropdown to set a preferred default media type for the Log screen.

###### 3. Appearance Section
- **Header**: "Appearance".
- **Theme**: Three-option selector: System (default) / Light / Dark.
  - Uses Material 3 dynamic color when available (Android 12+).
  - Falls back to a handpicked color scheme on older devices.
- **Language**: Display language toggle (English / Japanese labels). This controls only the UI labels, not a full localization. Store preference in DataStore.

###### 4. Data Management Section
- **Header**: "Data Management".
- **"Export Data"**: Exports all entries as a JSON file to the user's chosen location (using SAF / `ActivityResultContracts.CreateDocument`).
  - JSON format: Array of entry objects with all fields.
  - Include metadata (export date, app version).
- **"Import Data"**: Imports a JSON file, merging or replacing entries.
  - Opens file picker (`ActivityResultContracts.OpenDocument`).
  - Shows a confirmation dialog with import summary: "Found X entries. Import?"
  - Handle duplicates: Skip entries that already exist (based on unique constraint) or ask user.
- **"Clear All Data"**: Deletes all entries and goals.
  - **Triple confirmation**: 
    1. First tap: "Are you sure?"
    2. Second confirmation: "This will permanently delete all X entries. Type DELETE to confirm."
    3. Only proceed if user types "DELETE".

###### 5. About Section
- **Header**: "About".
- App name, version, brief description.
- "Made with ❤️ for Japanese learners".

---

## 6. Theming & Visual Design

### 6.1 Color Scheme

The app uses **Material Design 3** with **Material You dynamic color** on Android 12+.

**Fallback Static Color Scheme** (for Android 8-11):

```
Primary:          #4A6741 (Muted sage green — evokes calm, nature, reading)
On Primary:       #FFFFFF
Primary Container:#CCE8C5
On Primary Cont.: #0A2005

Secondary:        #54634D
On Secondary:     #FFFFFF
Sec. Container:   #D7E8CD
On Sec. Container:#122810

Tertiary:         #386568
On Tertiary:      #FFFFFF
Tert. Container:  #BBEBEE
On Tert. Cont.:   #002022

Background:       #F8FAF5
On Background:    #1A1C19
Surface:          #F8FAF5
On Surface:       #1A1C19

Error:            #BA1A1A
```

**Dark Mode Fallback**:
Derived automatically using Material 3 color utilities (`darkColorScheme` with the same seed colors, adjusted for dark backgrounds).

**Media Type Accent Colors** (used for chips, chart segments):
```
VN:    #7B5EA7 (Purple)     — Dark: #D4BBFF
WN:    #2E7D6F (Teal)       — Dark: #70DBC4
LN:    #3F6BA5 (Blue)       — Dark: #A4C8F0
Novel: #A67C2E (Amber)      — Dark: #F0D48A
```

### 6.2 Typography

Use Material 3 default type scale based on a clean, readable font. The system default (Roboto) is excellent, but for a Japanese-reading app, ensure CJK characters render beautifully:

- Use `Noto Sans JP` or `Noto Sans CJK` as the font family if bundled, or rely on the system Japanese font.
- **Display Large**: Used for the hero moji count on the dashboard (57sp).
- **Display Medium**: Used for chart titles or secondary hero stats (45sp).
- **Headline Medium**: Screen titles (28sp).
- **Title Medium**: Card titles, entry titles (16sp, medium weight).
- **Body Large**: Primary body text (16sp).
- **Body Medium**: Secondary text, descriptions (14sp).
- **Label Large**: Buttons, chips (14sp, medium weight).
- **Label Small**: Tiny annotations, chart axis labels (11sp).

### 6.3 Shape

Material 3 shape scale:

- **Extra Small**: 4dp corner radius (chips, small elements).
- **Small**: 8dp (text fields, small cards).
- **Medium**: 12dp (standard cards).
- **Large**: 16dp (large cards, bottom sheets).
- **Extra Large**: 28dp (FAB, dialogs).

### 6.4 Motion & Animation

- **Screen transitions**: Use Compose Navigation's built-in slide/fade transitions:
  - Navigating between bottom nav items: Fade through (300ms).
  - Navigating to Settings: Slide up from bottom.
  - Navigating back: Reverse of enter.
- **List animations**: `LazyColumn` items use `animateItem()` for reorder animations during sort changes.
- **Chart animations**: Charts animate in on first appearance with a horizontal reveal or value growth animation.
- **Progress bar**: Animated fill using `animateFloatAsState` with `spring` spec.
- **Number counter**: The total moji hero count does a count-up animation using `Animatable<Long>`.
- **Form transitions**: Series fields animate in/out with `AnimatedVisibility` using `expandVertically` + `fadeIn`.
- **Card press feedback**: Use `Modifier.clickable` with a ripple indication.
- **All animations** should use Material 3 motion tokens (emphasized easing, standard duration of 300ms, spring for interactive elements).

### 6.5 Spacing & Layout

- **Screen padding**: 16dp horizontal, 16dp top.
- **Card internal padding**: 16dp all sides.
- **Space between cards**: 12dp vertical.
- **Space between sections**: 24dp vertical.
- **Bottom nav height**: Standard Material 3 (80dp).
- **Content bottom padding**: 80dp + 16dp to ensure content isn't hidden behind the bottom nav.

---

## 7. Detailed Component Specifications

### 7.1 TotalMojiCard (Dashboard Hero)

```
Component: ElevatedCard with gradient background
Width: Match parent with 16dp horizontal margin
Height: Wrap content
Padding: 24dp all sides

Content:
  - "Total Moji Read" — LabelLarge, onSurfaceVariant color, centered
  - Spacer(8dp)
  - Animated moji count — DisplayLarge, onSurface color, centered, bold
    Format: "1,234,567" with comma separators
    Count-up animation from 0 to actual value over 1.5 seconds on first composition
  - Spacer(4dp)
  - "across X books" — BodyMedium, onSurfaceVariant, centered

Background: Gradient from primaryContainer (10% opacity) to surfaceVariant
Elevation: 2dp
Shape: Large (16dp corners)
```

### 7.2 MediaTypeBreakdown (Dashboard)

```
Component: Row of 4 OutlinedCards
Distribution: Each card takes 1/4 width with 8dp spacing
Each card:
  - Padding: 12dp
  - Icon/Emoji: 24sp, centered
  - Media type name: LabelSmall, centered
  - Count: TitleMedium, bold, centered, media type accent color
  - Shape: Medium (12dp corners)
  - Border: 1dp outlineVariant
```

### 7.3 GoalProgressCard (Dashboard)

```
Component: ElevatedCard
Width: Match parent with 16dp horizontal margin

Content:
  - Row: "Reading Goal" (TitleSmall) + Edit IconButton (pencil icon, 24dp)
  - Spacer(8dp)
  - Goal description: BodyMedium (e.g., "Read 2,000,000 moji")
  - Spacer(12dp)
  - LinearProgressIndicator:
      Height: 12dp (thicker than default for visual impact)
      Track color: surfaceVariant
      Indicator color: primary
      Shape: Fully rounded (6dp radius)
      Value: animated float
  - Spacer(8dp)
  - Row:
      Left: "67%" (TitleMedium, primary color, bold)
      Right: "1,340,000 / 2,000,000" (BodySmall, onSurfaceVariant)
  - If deadline exists:
      Spacer(4dp)
      "23 days remaining" (LabelSmall, tertiary color)
```

### 7.4 HistoryEntryCard

```
Component: ElevatedCard with Modifier.clickable
Width: Match parent with 16dp horizontal margin
Padding: 16dp

Layout:
  Row (top):
    - MediaType chip: SmallChip with media type color background, white text, LabelSmall
    - Spacer(weight=1f)
    - Date: BodySmall, onSurfaceVariant (e.g., "Feb 25, 2026")
  
  Spacer(8dp)
  
  Title: TitleMedium, onSurface
    For series: "シリーズ名 Vol. X"
    For standalone: "タイトル"
  
  Spacer(4dp)
  
  Moji count: BodyMedium, onSurfaceVariant
    "📝 150,000 moji"
  
  If notes not null:
    Spacer(4dp)
    Notes preview: BodySmall, onSurfaceVariant, maxLines=1, overflow=ellipsis

Shape: Large (16dp corners)
Elevation: 1dp
SwipeToDismiss: Left swipe reveals red delete background with trash icon
```

### 7.5 MediaTypeSelector (Log Screen)

```
Component: SingleChoiceSegmentedButtonRow (Material 3)
Width: Match parent

4 SegmentedButtons:
  - Each shows media type short name: "VN" | "WN" | "LN" | "Novel"
  - Selected state: Filled with secondaryContainer, icon checkmark appears
  - Unselected state: Outlined, transparent background
  - Shape: Fully rounded ends, squared internal dividers
  - Equal width distribution
```

### 7.6 Chart Components

#### MojiOverTimeChart
```
Library: Vico
Type: Line chart with area fill
Data: List<CumulativeMoji> (date, cumulativeMoji)

Configuration:
  - X axis: Date labels (monthly), rotated 45° if needed
  - Y axis: Moji count with K/M suffix formatter
  - Line: 3dp width, primary color, cubic bezier interpolation
  - Area fill: Primary color at 20% opacity, gradient to transparent at bottom
  - Background grid: Subtle horizontal lines at onSurface 5% opacity
  - Marker: On touch, show a vertical line with dot at data point and tooltip
  - Animation: Horizontal reveal from left to right on first display (800ms)
  
Card wrapper:
  - Title: "Cumulative Moji Read" (TitleSmall)
  - Optional: Time range chips (All | 1Y | 6M | 3M)
  - Chart height: 200dp
  - Padding: 16dp
```

#### MonthlyReadingChart
```
Library: Vico
Type: Column/Bar chart
Data: List<MonthlyCount> (yearMonth, count)

Configuration:
  - X axis: Month labels ("Jan", "Feb", etc.)
  - Y axis: Integer count
  - Bars: Primary color, 60% of available width, rounded top corners (4dp)
  - Optionally stacked by media type using media type accent colors
  - Animation: Bars grow upward from 0 on first display
  
Card wrapper:
  - Title: "Monthly Reading Activity" (TitleSmall)
  - Toggle: "Books" | "Moji" (switch between count and moji sum)
  - Chart height: 200dp
```

---

## 8. State Management

### 8.1 UI State Models

Each screen has a sealed interface for its UI state:

```kotlin
// Dashboard
sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Success(
        val totalMoji: Long,
        val totalBooks: Int,
        val mediaTypeCounts: List<MediaTypeCount>,
        val mediaTypeMoji: List<MediaTypeMoji>,
        val activeGoal: ReadingGoal?,
        val goalProgress: Float, // 0.0 to 1.0
        val goalCurrentValue: Long,
        val averageMojiPerBook: Long,
        val lastEntryDate: LocalDate?,
        val readingStreakMonths: Int,
        val cumulativeMojiData: List<CumulativeMoji>,
        val monthlyCountData: List<MonthlyCount>,
        val monthlyMojiData: List<MonthlyMoji>,
        val nextMilestone: Milestone?,
        val achievedMilestones: List<Milestone>
    ) : DashboardUiState
}

// Log
data class LogUiState(
    val mediaType: MediaType? = null,
    val isSeries: Boolean = false,
    val title: String = "",
    val seriesTitles: List<String> = emptyList(), // Autocomplete suggestions
    val seriesNumber: String = "",
    val suggestedSeriesNumber: Int? = null,
    val mojiCount: String = "100000",
    val dateFinished: LocalDate = LocalDate.now(),
    val notes: String = "",
    val isSubmitting: Boolean = false,
    val validationErrors: Map<String, String> = emptyMap(),
    val showDuplicateWarning: Boolean = false,
    val duplicateTitle: String = ""
)

// History
data class HistoryUiState(
    val entries: List<ReadingEntry> = emptyList(),
    val sortOption: SortOption = SortOption.DATE_DESC,
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val editingEntry: ReadingEntry? = null
)

enum class SortOption(val displayName: String) {
    DATE_DESC("Newest First"),
    DATE_ASC("Oldest First"),
    MOJI_DESC("Most Moji"),
    MOJI_ASC("Least Moji"),
    TITLE_ASC("Title A→Z"),
    TITLE_DESC("Title Z→A")
}
```

### 8.2 ViewModel Structure

Each ViewModel:
- Is annotated with `@HiltViewModel`.
- Injects relevant use cases via constructor.
- Exposes a single `StateFlow<UiState>` to the UI.
- Handles all user actions via public functions.
- Uses `viewModelScope.launch` for coroutine operations.
- Collects database Flows and combines them into the UI state using `combine()`.

Example pattern:
```kotlin
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getStatisticsUseCase: GetStatisticsUseCase,
    private val getGoalProgressUseCase: GetGoalProgressUseCase
) : ViewModel() {
    
    val uiState: StateFlow<DashboardUiState> = combine(
        getStatisticsUseCase.getTotalMoji(),
        getStatisticsUseCase.getTotalBooks(),
        // ... more flows
    ) { totalMoji, totalBooks, ... ->
        DashboardUiState.Success(...)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState.Loading
    )
}
```

---

## 9. Edge Cases & Error Handling

### 9.1 Duplicate Prevention

- **Exact duplicate**: Same title + seriesNumber + mediaType.
  - Detected before insertion via `checkDuplicateExists()` query.
  - If detected: Show `DuplicateWarningDialog`. User can cancel or force-add.
- **Near duplicate**: Same title, different series number.
  - This is a VALID entry (next volume). No warning needed.
- **Same title, different media type**: Valid (e.g., a VN and LN of the same series).

### 9.2 Series Number Gaps

- Allow non-sequential series numbers (user may skip volumes or read out of order).
- The auto-suggest feature suggests `max + 1` but does not enforce it.

### 9.3 Data Validation

- **Title**: Required, non-empty, trimmed of whitespace. Max 200 characters.
- **Series Number**: Must be positive integer (1+). Required when `isSeries = true`.
- **Moji Count**: Must be positive long (1+). Reject 0 or negative.
- **Date Finished**: Cannot be null. Cannot be in the future. Can be any past date.
- **Media Type**: Must be selected (cannot submit without choosing one).

### 9.4 Large Numbers

- Moji count stored as `Long` (max ~9.2 quintillion). More than sufficient.
- Display formatting must handle large numbers gracefully:
  - Up to 999: Show as-is.
  - 1,000 - 999,999: Show with commas (e.g., "150,000").
  - 1,000,000+: Show with commas (e.g., "1,234,567") or optionally as "1.23M" in compact contexts.

### 9.5 Empty States

Every screen must handle zero-data state gracefully:
- Dashboard: Show onboarding prompt instead of empty charts.
- History: Show friendly empty state with call-to-action.
- Log: Always functional (no dependency on existing data for the non-series case).

### 9.6 Undo Operations

- After adding an entry: Snackbar with "Undo" that deletes the entry.
- After deleting an entry: Snackbar with "Undo" that re-inserts the entry.
- Undo timeout: 5 seconds. After that, the snackbar dismisses and the action is permanent.

### 9.7 Keyboard Handling

- The Log screen form must properly handle IME insets.
- The form should scroll to keep the focused field visible when the keyboard opens.
- Use `Modifier.imePadding()` and `BringIntoViewRequester` as needed.

---

## 10. Data Export/Import Format

### 10.1 Export JSON Schema

```json
{
  "version": 1,
  "exportedAt": "2026-03-02T12:00:00Z",
  "appVersion": "1.0.0",
  "entries": [
    {
      "title": "ソードアート・オンライン",
      "mediaType": "LN",
      "isSeries": true,
      "seriesNumber": 1,
      "mojiCount": 120000,
      "dateFinished": "2026-01-15",
      "dateAdded": "2026-01-15T18:30:00Z",
      "notes": "Great introduction to the series"
    }
  ],
  "goals": [
    {
      "goalType": "MOJI",
      "targetValue": 2000000,
      "startDate": "2026-01-01",
      "endDate": null,
      "isActive": true
    }
  ],
  "settings": {
    "defaultMojiCount": 100000,
    "theme": "SYSTEM"
  }
}
```

### 10.2 Import Behavior

- Parse and validate the JSON structure.
- Show the user a summary before importing.
- For each entry: Check if an identical entry exists (same title + seriesNumber + mediaType + dateFinished). If yes, skip. If no, insert.
- Import goals: Replace current active goal if one exists in the import and is marked active.
- Report results: "Imported X new entries. Skipped Y duplicates."

---

## 11. Milestones & Achievements

The milestone system provides positive reinforcement. It is purely cosmetic and motivational.

### 11.1 Moji Milestones

| Threshold | Label | Emoji |
|---|---|---|
| 100,000 | 100K Moji! | 🌱 |
| 250,000 | 250K Moji! | 🌿 |
| 500,000 | 500K Moji! | 🌳 |
| 1,000,000 | 1 Million Moji! | ⭐ |
| 2,000,000 | 2 Million Moji! | 🌟 |
| 5,000,000 | 5 Million Moji! | 💫 |
| 10,000,000 | 10 Million Moji! | 🏆 |
| 25,000,000 | 25 Million Moji! | 👑 |
| 50,000,000 | 50 Million Moji! | 🐉 |
| 100,000,000 | 100 Million Moji! | 🎌 |

### 11.2 Book Count Milestones

| Threshold | Label | Emoji |
|---|---|---|
| 1 | First Book! | 📕 |
| 5 | 5 Books Read | 📚 |
| 10 | 10 Books Read | 📖 |
| 25 | 25 Books Read | 🎯 |
| 50 | 50 Books Read | 🔥 |
| 100 | Century Reader | 💯 |
| 250 | 250 Books Read | 🏅 |
| 500 | 500 Books Read | 🏆 |

### 11.3 Display

- On the Dashboard: Show the highest achieved milestone with its emoji.
- Show "Next milestone" with progress: "Only 160,000 moji to 2M! 🌟"
- When a new milestone is achieved (detected upon logging an entry), show a celebratory dialog with confetti animation or a special snackbar.

---

## 12. Performance Requirements

- **App launch to interactive dashboard**: < 500ms.
- **Log entry submission**: < 100ms (local DB write).
- **Sort change in History**: < 200ms for up to 1,000 entries.
- **Chart rendering**: < 300ms initial render.
- **Database queries**: All return Flows that emit within 50ms for datasets up to 10,000 entries.
- **Memory usage**: < 100MB baseline. Charts should not leak memory on recomposition.
- **APK size**: Target < 15MB (Compose + Room + Vico are relatively lightweight).

---

## 13. Accessibility

- All interactive elements must have `contentDescription` for screen readers.
- Charts must have text alternatives (summary descriptions accessible to TalkBack).
- Color is never the sole indicator of information (always paired with text/icons).
- Minimum touch target size: 48dp × 48dp (Material 3 guideline).
- Support dynamic font scaling (use `sp` for text, test at 200% scale).
- High contrast mode: Ensure all text meets WCAG AA contrast ratios (4.5:1 for body text, 3:1 for large text).

---

## 14. Testing Strategy

### 14.1 Unit Tests
- All use cases: Test business logic (validation, duplicate detection, milestone calculation).
- All ViewModels: Test state transformations with fake repositories.
- Type converters: Test serialization/deserialization of all custom types.
- Number formatting utilities.
- Date utilities.

### 14.2 Integration Tests
- Repository + DAO: Test actual Room database operations using in-memory database.
- Data export/import: Test round-trip serialization.

### 14.3 UI Tests
- Compose UI tests for each screen using `ComposeTestRule`.
- Test form validation flows on the Log screen.
- Test sort behavior on the History screen.
- Test navigation between screens.
- Test empty states render correctly.

---

## 15. Future Enhancements (Post-MVP)

These features are NOT part of the initial release but should be considered in the architecture:

1. **Widget**: Home screen widget showing total moji and current goal progress.
2. **Notifications**: Optional daily/weekly reading reminders.
3. **Yearly Summaries**: Year-in-review summary card (total moji that year, total books, favorite series, etc.).
4. **Series Detail View**: Tap a series name to see all volumes in that series with individual stats.
5. **Reading Timer**: Optional stopwatch/timer for active reading sessions (could auto-estimate moji based on time and reading speed).
6. **Multiple Goals**: Support multiple concurrent goals (e.g., "50 books this year" + "10M moji lifetime").
7. **Cloud Backup**: Optional encrypted backup to Google Drive (preserving offline-first principle).
8. **Tags/Categories**: User-defined tags for entries (e.g., "horror", "romance", genre tracking).
9. **Calendar Heatmap**: A GitHub-style contribution heatmap showing reading days on the dashboard.
10. **Comparison Mode**: Compare current month/year stats vs previous period.

---

## 16. File & Folder Structure (Complete)

```
ReadingTracker/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/dokushotracker/
│       │   │   ├── DokushoApplication.kt
│       │   │   ├── MainActivity.kt
│       │   │   ├── data/
│       │   │   │   ├── local/
│       │   │   │   │   ├── DokushoDatabase.kt
│       │   │   │   │   ├── dao/
│       │   │   │   │   │   ├── ReadingEntryDao.kt
│       │   │   │   │   │   └── ReadingGoalDao.kt
│       │   │   │   │   ├── entity/
│       │   │   │   │   │   ├── ReadingEntryEntity.kt
│       │   │   │   │   │   └── ReadingGoalEntity.kt
│       │   │   │   │   └── converter/
│       │   │   │   │       └── Converters.kt
│       │   │   │   ├── repository/
│       │   │   │   │   ├── ReadingRepository.kt
│       │   │   │   │   ├── ReadingRepositoryImpl.kt
│       │   │   │   │   ├── GoalRepository.kt
│       │   │   │   │   └── GoalRepositoryImpl.kt
│       │   │   │   └── model/
│       │   │   │       ├── MediaType.kt
│       │   │   │       ├── GoalType.kt
│       │   │   │       └── StatModels.kt
│       │   │   ├── domain/
│       │   │   │   ├── model/
│       │   │   │   │   ├── ReadingEntry.kt
│       │   │   │   │   ├── ReadingGoal.kt
│       │   │   │   │   └── Milestone.kt
│       │   │   │   └── usecase/
│       │   │   │       ├── AddEntryUseCase.kt
│       │   │   │       ├── UpdateEntryUseCase.kt
│       │   │   │       ├── DeleteEntryUseCase.kt
│       │   │   │       ├── GetEntriesUseCase.kt
│       │   │   │       ├── GetStatisticsUseCase.kt
│       │   │   │       ├── GetSeriesSuggestionsUseCase.kt
│       │   │   │       ├── CheckDuplicateUseCase.kt
│       │   │   │       ├── SetGoalUseCase.kt
│       │   │   │       ├── GetGoalProgressUseCase.kt
│       │   │   │       └── ExportImportUseCase.kt
│       │   │   ├── ui/
│       │   │   │   ├── theme/
│       │   │   │   │   ├── Theme.kt
│       │   │   │   │   ├── Color.kt
│       │   │   │   │   ├── Type.kt
│       │   │   │   │   └── Shape.kt
│       │   │   │   ├── navigation/
│       │   │   │   │   ├── DokushoNavGraph.kt
│       │   │   │   │   ├── Screen.kt
│       │   │   │   │   └── BottomNavBar.kt
│       │   │   │   ├── screens/
│       │   │   │   │   ├── dashboard/
│       │   │   │   │   │   ├── DashboardScreen.kt
│       │   │   │   │   │   ├── DashboardViewModel.kt
│       │   │   │   │   │   └── components/
│       │   │   │   │   │       ├── TotalMojiCard.kt
│       │   │   │   │   │       ├── MediaTypeBreakdown.kt
│       │   │   │   │   │       ├── GoalProgressCard.kt
│       │   │   │   │   │       ├── MojiOverTimeChart.kt
│       │   │   │   │   │       ├── MonthlyReadingChart.kt
│       │   │   │   │   │       ├── MediaDistributionChart.kt
│       │   │   │   │   │       ├── StatsHighlightsRow.kt
│       │   │   │   │   │       ├── MilestoneCard.kt
│       │   │   │   │   │       └── EmptyDashboardState.kt
│       │   │   │   │   ├── log/
│       │   │   │   │   │   ├── LogScreen.kt
│       │   │   │   │   │   ├── LogViewModel.kt
│       │   │   │   │   │   └── components/
│       │   │   │   │   │       ├── MediaTypeSelector.kt
│       │   │   │   │   │       ├── SeriesToggle.kt
│       │   │   │   │   │       ├── TitleInput.kt
│       │   │   │   │   │       ├── SeriesNumberInput.kt
│       │   │   │   │   │       ├── MojiCountInput.kt
│       │   │   │   │   │       ├── DatePickerField.kt
│       │   │   │   │   │       └── DuplicateWarningDialog.kt
│       │   │   │   │   ├── history/
│       │   │   │   │   │   ├── HistoryScreen.kt
│       │   │   │   │   │   ├── HistoryViewModel.kt
│       │   │   │   │   │   └── components/
│       │   │   │   │   │       ├── HistoryEntryCard.kt
│       │   │   │   │   │       ├── SortSelector.kt
│       │   │   │   │   │       ├── EmptyHistoryState.kt
│       │   │   │   │   │       └── EditEntrySheet.kt
│       │   │   │   │   └── settings/
│       │   │   │   │       ├── SettingsScreen.kt
│       │   │   │   │       ├── SettingsViewModel.kt
│       │   │   │   │       └── components/
│       │   │   │   │           ├── GoalSettingDialog.kt
│       │   │   │   │           ├── ThemeSelector.kt
│       │   │   │   │           ├── DefaultMojiSetting.kt
│       │   │   │   │           └── DataManagementSection.kt
│       │   │   │   └── components/
│       │   │   │       ├── DokushoTopBar.kt
│       │   │   │       ├── ConfirmationDialog.kt
│       │   │   │       ├── AnimatedCounter.kt
│       │   │   │       └── EmptyState.kt
│       │   │   ├── util/
│       │   │   │   ├── DateUtils.kt
│       │   │   │   ├── NumberFormatUtils.kt
│       │   │   │   ├── DataExporter.kt
│       │   │   │   └── MilestoneChecker.kt
│       │   │   └── di/
│       │   │       ├── DatabaseModule.kt
│       │   │       └── RepositoryModule.kt
│       │   └── res/
│       │       ├── values/
│       │       │   ├── strings.xml
│       │       │   ├── themes.xml
│       │       │   └── colors.xml
│       │       ├── values-ja/
│       │       │   └── strings.xml          — Japanese string translations
│       │       ├── mipmap-*/
│       │       │   └── ic_launcher.*        — App icon (book with 読 kanji)
│       │       └── drawable/
│       │           └── (vector assets for empty states, etc.)
│       ├── test/                             — Unit tests
│       │   └── java/com/dokushotracker/
│       │       ├── data/repository/
│       │       ├── domain/usecase/
│       │       └── ui/viewmodel/
│       └── androidTest/                      — Instrumented tests
│           └── java/com/dokushotracker/
│               ├── data/local/
│               └── ui/screens/
├── build.gradle.kts                          — Project-level build
├── settings.gradle.kts
├── gradle.properties
├── gradle/
│   └── libs.versions.toml                    — Version catalog
└── DESIGN_DOCUMENT.md                        — This file
```

---

## 17. Gradle Dependencies (libs.versions.toml)

```toml
[versions]
agp = "8.7.3"
kotlin = "2.1.0"
compose-bom = "2025.02.00"
room = "2.7.0"
hilt = "2.52"
hilt-navigation-compose = "1.2.0"
navigation-compose = "2.8.7"
vico = "2.1.1"
datastore = "1.1.2"
serialization = "1.7.3"
coroutines = "1.9.0"
lifecycle = "2.8.7"
ksp = "2.1.0-1.0.29"

[libraries]
# Compose
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-animation = { group = "androidx.compose.animation", name = "animation" }
compose-material-icons = { group = "androidx.compose.material", name = "material-icons-extended" }

# Navigation
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation-compose" }

# Room
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }

# Hilt
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hilt-navigation-compose" }

# Lifecycle
lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }
lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }

# Vico (Charts)
vico-compose-m3 = { group = "com.patrykandpatrick.vico", name = "compose-m3", version.ref = "vico" }

# DataStore
datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }

# Serialization
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "serialization" }

# Coroutines
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }

# Activity
activity-compose = { group = "androidx.activity", name = "activity-compose", version = "1.9.3" }

# Core
core-ktx = { group = "androidx.core", name = "core-ktx", version = "1.15.0" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
room = { id = "androidx.room", version.ref = "room" }
```

---

## 18. Implementation Order (Recommended Phases)

### Phase 1: Foundation (Core Infrastructure)
1. Project setup with Gradle, version catalog, and all dependencies.
2. Room database with entities, DAOs, type converters, and database class.
3. Repositories (interfaces + implementations).
4. Hilt dependency injection modules.
5. Theme setup (Color, Type, Shape, Theme composables).
6. Navigation graph with bottom nav bar and placeholder screens.
7. `MainActivity` wiring with Hilt and Navigation.

### Phase 2: Log Screen (Data Input)
1. `LogViewModel` with form state management.
2. All Log screen components (MediaTypeSelector, TitleInput, etc.).
3. Form validation logic.
4. Duplicate detection.
5. Entry submission with snackbar + undo.
6. Series autocomplete and auto-suggest series number.

### Phase 3: History Screen (Data Display)
1. `HistoryViewModel` with sort state.
2. `HistoryEntryCard` composable.
3. `SortSelector` composable.
4. Sort functionality (all 6 options).
5. Swipe-to-delete with undo.
6. Edit entry bottom sheet.
7. Empty state.

### Phase 4: Dashboard Screen (Statistics & Charts)
1. `DashboardViewModel` combining all stats flows.
2. `TotalMojiCard` with animated counter.
3. `MediaTypeBreakdown` row.
4. `GoalProgressCard` with animated progress bar.
5. `StatsHighlightsRow`.
6. `MojiOverTimeChart` (Vico line chart).
7. `MonthlyReadingChart` (Vico bar chart).
8. `MediaDistributionChart`.
9. `MilestoneCard`.
10. Empty dashboard state.

### Phase 5: Settings & Polish
1. Settings screen with all sections.
2. Goal setting dialog.
3. Theme preference (light/dark/system).
4. Default moji count setting.
5. Data export (JSON).
6. Data import (JSON).
7. Clear all data with safety confirmations.

### Phase 6: Polish & Testing
1. Animation refinement across all screens.
2. Edge case handling and error states.
3. Accessibility audit (content descriptions, contrast, touch targets).
4. Performance optimization (profiling, lazy loading).
5. Unit tests for use cases and ViewModels.
6. Integration tests for Room operations.
7. UI tests for critical flows.
8. App icon and splash screen.

---

## 19. Key Implementation Notes

### 19.1 Number Input Formatting
When the user types in the moji count field, format the number with commas in real-time using a `VisualTransformation`:
```
User types: 150000
Display shows: 150,000
Stored value: 150000 (raw Long)
```
Use a custom `VisualTransformation` that inserts commas every 3 digits from the right. The `OutlinedTextField` value should store the raw digits only; the visual transformation handles display.

### 19.2 Date Display
- Use `java.time.LocalDate` throughout (API 26+ guaranteed by minSDK 26).
- Display dates using `DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)` for locale-appropriate formatting.
- Relative dates on the dashboard: "Today", "Yesterday", "3 days ago", etc. (for recency up to 7 days, then use absolute date).

### 19.3 Chart Data Preparation
- Charts should only show data points where entries exist (no padding of zero-value months unless it creates gaps in the chart).
- For cumulative charts: Compute running total in the DAO query or in the use case by sorting entries by date and accumulating.
- For monthly aggregation: Group by year-month string (e.g., "2026-03") in the DAO using SQLite's `strftime`.

### 19.4 Snackbar Host
- Use a shared `SnackbarHostState` provided at the `Scaffold` level.
- ViewModels emit one-shot events (using `Channel` or `SharedFlow`) for snackbar messages.
- The Scaffold's `SnackbarHost` shows them with appropriate duration and action.

### 19.5 Process Death Handling
- Room + Flow handles most state restoration automatically.
- Log screen form state should be preserved using `SavedStateHandle` in the ViewModel:
  - All form fields saved/restored automatically.
  - User doesn't lose partially filled forms if the process dies.

### 19.6 ProGuard/R8 Rules
- Room entities: Keep with `@Keep` annotation or proguard rules.
- Kotlin serialization: Add rules for `kotlinx-serialization`.
- Hilt: Handled automatically by the Hilt Gradle plugin.

---

## 20. App Icon Concept

The app icon should be simple and recognizable:
- A stylized open book silhouette.
- The kanji 「読」(read) or 「本」(book) centered on the book.
- Primary sage green color as the book, with white/cream pages.
- Rounded square adaptive icon shape (Android standard).
- Monochrome variant for themed icons (Android 13+).

---

## 21. Summary

Dokusho Tracker is a focused, beautifully designed Android app built with Jetpack Compose and Material Design 3. It serves one purpose excellently: tracking Japanese reading immersion. The three-screen architecture (Dashboard → Log → History) provides a clean mental model. The Dashboard motivates with rich statistics and visualizations, the Log screen prioritizes speed with smart defaults and autocomplete, and the History screen offers flexible exploration of past entries. All data stays on-device with optional JSON export for backup safety.

The app should feel like a premium, native Android experience — fast, smooth, and delightful to use.
