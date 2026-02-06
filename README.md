# LeetCode Progress Tracker

A beautiful Android app with Material Design 3 to track your LeetCode coding journey.

## Features

### 🎯 Core Features
- **Activity Heatmap**: GitHub-style green dot streak map showing your daily problem-solving activity
- **Live Statistics**: Real-time tracking of total problems solved and current streak
- **Home Widget**: Quick glance at your progress directly from your home screen
- **Smart Reminders**: Daily notifications if you haven't solved a problem before the day ends

### 🎨 Design
- Material Design 3 (Material You) with Expressive guidelines
- Dynamic color theming
- Smooth animations and transitions
- Light and Dark mode support
- Beautiful card-based layouts

### 📊 Data Display
- 365-day activity visualization
- Color-coded intensity levels (0, 1-2, 3-5, 6-10, 10+ problems per day)
- Current streak counter
- Total problems solved
- Daily completion status

## Setup Instructions

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 8 or higher
- Android SDK with API 26+ (Android 8.0 Oreo)
- Gradle 8.0+

### Building the APK

#### Method 1: Using Android Studio (Recommended)
1. Open Android Studio
2. Click "File > Open" and select the `LeetCodeTracker` folder
3. Wait for Gradle sync to complete
4. Click "Build > Build Bundle(s) / APK(s) > Build APK(s)"
5. Once built, click "locate" to find your APK file

#### Method 2: Command Line
```bash
# Navigate to project directory
cd LeetCodeTracker

# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Build release APK (you'll need to set up signing)
./gradlew assembleRelease

# APK will be located at:
# app/build/outputs/apk/debug/app-debug.apk
```

### Installation
1. Transfer the APK to your Android device
2. Enable "Install from Unknown Sources" in Settings
3. Tap the APK file to install
4. Grant necessary permissions (Notifications, Alarms)

## Usage Guide

### Initial Setup
1. **Enter LeetCode Username**: Open the app and enter your LeetCode username in the input field
2. **Save**: Click "Save and Load Data" to fetch your progress
3. **View Activity Map**: Your 365-day activity heatmap will display

### Setting Up Reminders
1. Click "Set Reminder Time" button
2. Choose your preferred reminder time (e.g., 8:00 PM)
3. You'll receive a notification if you haven't solved a problem by that time

### Adding the Widget
1. Long-press on your home screen
2. Select "Widgets"
3. Find "LeetCode Tracker"
4. Drag it to your home screen
5. Widget shows: Current streak, Total solved, Today's status

### Understanding the Activity Map
- **Gray**: No problems solved
- **Light Green**: 1-2 problems
- **Medium Green**: 3-5 problems
- **Dark Green**: 6-10 problems
- **Darkest Green**: 10+ problems

## Permissions Required

- **INTERNET**: To fetch LeetCode data
- **POST_NOTIFICATIONS**: For daily reminders (Android 13+)
- **SCHEDULE_EXACT_ALARM**: For precise reminder timing
- **RECEIVE_BOOT_COMPLETED**: To restore reminders after device restart

## Technical Details

### Architecture
- **Language**: Kotlin
- **UI Framework**: Material Design 3 Components
- **Networking**: OkHttp3 for API calls
- **Data Parsing**: Gson for JSON handling
- **Background Tasks**: AlarmManager for notifications
- **Widget**: AppWidgetProvider with RemoteViews

### API Integration
The app uses LeetCode's GraphQL API to fetch:
- User submission statistics
- Submission calendar (daily activity)
- Problem counts by difficulty

### Data Storage
- SharedPreferences for:
  - User ID
  - Reminder settings (time, enabled status)
  - Widget refresh timestamps

## Troubleshooting

### Data Not Loading
- Check internet connection
- Verify LeetCode username is correct (case-sensitive)
- Ensure the username's profile is public on LeetCode

### Notifications Not Working
- Grant notification permission in Settings
- Check that battery optimization is disabled for the app
- Verify reminder time is set

### Widget Not Updating
- Manually tap widget to refresh
- Check that user ID is saved in the main app
- Widget auto-refreshes every hour

## Project Structure
```
LeetCodeTracker/
├── src/main/
│   ├── java/com/leetcode/tracker/
│   │   ├── MainActivity.kt
│   │   ├── api/
│   │   │   └── LeetCodeApi.kt
│   │   ├── widget/
│   │   │   └── LeetCodeWidget.kt
│   │   └── notifications/
│   │       └── DailyReminderReceiver.kt
│   ├── res/
│   │   ├── layout/
│   │   ├── drawable/
│   │   ├── values/
│   │   └── xml/
│   └── AndroidManifest.xml
├── build.gradle
└── README.md
```

## Future Enhancements

Potential features for future versions:
- Multiple user profile support
- Problem difficulty breakdown charts
- Weekly/Monthly streak statistics
- Customizable color themes
- Export progress data
- Offline mode with cached data
- Problem recommendations based on progress

## License

This is a personal project for educational purposes. LeetCode is a trademark of LeetCode LLC.

## Support

For issues or questions:
1. Check the Troubleshooting section
2. Verify all permissions are granted
3. Ensure you're using a valid LeetCode username

---

Built with ❤️ using Material Design 3 and Kotlin
