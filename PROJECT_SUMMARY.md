# LeetCode Progress Tracker - Project Summary

## What This App Does

This is a fully-functional Android app that helps you track your LeetCode coding practice with:

1. **Activity Heatmap** - Beautiful green dot visualization (like GitHub) showing 365 days of progress
2. **Live Statistics** - Current streak and total problems solved
3. **Home Screen Widget** - Quick view of your stats without opening the app
4. **Smart Reminders** - Get notified if you haven't solved a problem before day ends

## Technology Stack

- **Language**: Kotlin (100% Kotlin)
- **UI**: Material Design 3 (Google's latest design system)
- **Architecture**: Clean MVVM-like structure
- **Networking**: OkHttp3 for LeetCode API calls
- **Data**: Gson for JSON parsing
- **Storage**: SharedPreferences for user data
- **Background**: AlarmManager for notifications

## Key Files

### Core Application
- `MainActivity.kt` - Main app screen with UI logic
- `LeetCodeApi.kt` - Handles all LeetCode API communication
- `LeetCodeWidget.kt` - Home screen widget implementation
- `DailyReminderReceiver.kt` - Notification system

### UI Resources
- `activity_main.xml` - Main screen layout (Material Design 3)
- `widget_leetcode.xml` - Widget layout
- `themes.xml` - Material 3 color scheme
- Drawable resources for the activity heatmap

### Configuration
- `AndroidManifest.xml` - App permissions and components
- `build.gradle` - Dependencies and build configuration

## Material Design 3 Features Used

✓ Dynamic color theming
✓ Elevated cards with proper elevation levels
✓ Rounded corners (24dp, 16dp, 12dp)
✓ Material buttons with icons
✓ Proper typography scale
✓ Surface tinting
✓ State layers
✓ Light/Dark theme support

## How to Build

This project needs Android Studio to compile into an APK. See BUILD_INSTRUCTIONS.txt for complete steps.

Quick steps:
1. Open in Android Studio
2. Let Gradle sync
3. Build > Build APK
4. Install on Android device

## Features Breakdown

### 1. Activity Heatmap
- Shows last 365 days in a grid (52 weeks × 7 days)
- Color intensity based on problems solved per day:
  - Gray: 0 problems
  - Light green: 1-2 problems
  - Medium green: 3-5 problems
  - Dark green: 6-10 problems
  - Darkest green: 10+ problems

### 2. Statistics
- **Total Solved**: Sum of all problems across all time
- **Current Streak**: Consecutive days with at least 1 problem solved

### 3. Widget
Updates every hour showing:
- Current streak (🔥)
- Total solved (✓)
- Today's status (✓ Solved / ⚠ Not yet)

### 4. Reminders
- User sets preferred time
- App checks LeetCode at that time
- Sends notification if no problems solved that day
- Notification opens app when tapped

## API Integration

Uses LeetCode's GraphQL API:
```
Endpoint: https://leetcode.com/graphql
Query: matchedUser { submissionCalendar }
```

Returns timestamp-based submission data that the app converts to daily counts.

## Permissions Explained

- **INTERNET**: Fetch data from LeetCode
- **POST_NOTIFICATIONS**: Show daily reminders
- **SCHEDULE_EXACT_ALARM**: Precise reminder timing
- **RECEIVE_BOOT_COMPLETED**: Restore reminders after reboot

## Project Structure
```
LeetCodeTracker/
├── src/main/
│   ├── java/com/leetcode/tracker/
│   │   ├── MainActivity.kt           # Main UI controller
│   │   ├── api/
│   │   │   └── LeetCodeApi.kt        # API communication
│   │   ├── widget/
│   │   │   └── LeetCodeWidget.kt     # Home screen widget
│   │   └── notifications/
│   │       └── DailyReminderReceiver.kt  # Notification system
│   ├── res/
│   │   ├── layout/                   # XML layouts
│   │   ├── drawable/                 # Icons and shapes
│   │   ├── values/                   # Strings, colors, themes
│   │   └── xml/                      # Widget configuration
│   └── AndroidManifest.xml           # App configuration
├── build.gradle                      # Dependencies
├── README.md                         # User guide
└── BUILD_INSTRUCTIONS.txt           # Build steps
```

## Code Quality

- Well-commented Kotlin code
- Proper error handling
- Coroutines for async operations
- Material Design 3 best practices
- Responsive layouts
- Memory efficient heatmap rendering

## Testing Recommendations

1. Test with different usernames
2. Test notification permissions on Android 13+
3. Test widget updates
4. Test reminder at different times
5. Test with no internet connection
6. Test dark mode

## Known Limitations

- Requires public LeetCode profile
- Widget updates every 1 hour (API rate limiting)
- Heatmap shows last 365 days only
- Requires Android 8.0+ (96% of Android devices)

## Future Enhancement Ideas

- Multiple user profiles
- Problem difficulty breakdown
- Weekly/monthly trends
- Customizable themes
- Offline mode
- Export data
- GitHub-style contribution graph animations

---

This is a production-ready Android app following Google's latest design guidelines!
