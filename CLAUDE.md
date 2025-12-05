# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SMS2WhatsApp is a personal-use Android application that monitors incoming SMS messages and automatically forwards them to a specified WhatsApp contact. The app uses Android Accessibility Services and Device Admin permissions to automate the forwarding process.

**Important**: This is a personal-use tool designed for monitoring SMS on a backup phone. It requires sensitive permissions including SMS reading, accessibility services, and device administration.

## Build Commands

```bash
# Build the project
./gradlew build

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install and run on connected device
./gradlew installDebug

# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Clean build artifacts
./gradlew clean
```

## Architecture

### Core Components

The app consists of four main components that work together:

1. **MainActivity** ([MainActivity.kt](app/src/main/java/com/iceloof/sms2whatsapp/MainActivity.kt))
   - Main UI for saving WhatsApp recipient phone number (stored in SharedPreferences as "phoneNumber")
   - Handles permission requests: SMS, Internet, Accessibility Service, Device Admin, System Alert Window
   - Entry point for user configuration

2. **SmsReceiver** ([SmsReceiver.kt](app/src/main/java/com/iceloof/sms2whatsapp/SmsReceiver.kt))
   - BroadcastReceiver that listens for `android.provider.Telephony.SMS_RECEIVED`
   - Extracts SMS sender and message body from PDUs
   - Wakes device screen for 15 seconds using WakeLock
   - Opens WhatsApp via Intent with pre-filled message: "From: {sender}\nMsg: {message}"

3. **AccessibilityService** ([AccessibilityService.kt](app/src/main/java/com/iceloof/sms2whatsapp/AccessibilityService.kt))
   - Monitors WhatsApp window events (`TYPE_WINDOW_STATE_CHANGED`)
   - Automatically finds and clicks the "Send" button using content description
   - Returns to home screen and locks device after sending
   - Critical automation component that completes the forwarding flow

4. **MyDeviceAdminReceiver** ([MyDeviceAdminReceiver.kt](app/src/main/java/com/iceloof/sms2whatsapp/MyDeviceAdminReceiver.kt))
   - Enables device lock capability used by AccessibilityService
   - Required for the `lockScreen()` functionality

### Message Flow

```
SMS Received → SmsReceiver wakes device
            ↓
            Retrieves recipient from SharedPreferences
            ↓
            Opens WhatsApp with pre-filled message
            ↓
            AccessibilityService detects WhatsApp window
            ↓
            Automatically clicks "Send" button
            ↓
            Returns to home and locks device
```

### Configuration Files

- **AndroidManifest.xml**: Declares all permissions, receivers, services, and activities
  - Note: Uses `tools:ignore="ProtectedPermissions"` for BIND_DEVICE_ADMIN and BIND_ACCESSIBILITY_SERVICE
- **accessibility_service_config.xml**: Configures accessibility service to monitor WhatsApp
- **device_admin_receiver.xml**: Declares device admin capabilities
- Package name: `com.iceloof.sms2whatsapp`
- Min SDK: 30, Target SDK: 34, Compile SDK: 34

### Key Technical Details

- Uses Kotlin with JVM target 1.8
- Jetpack Compose for UI (though main activity uses XML layout)
- Version catalog pattern for dependency management ([gradle/libs.versions.toml](gradle/libs.versions.toml))
- APK naming: `SMS2WhatsApp_{versionName}.apk`
- SharedPreferences key: "SMS2WhatsAppPreferences" with "phoneNumber" field

## Development Notes

- The app uses deprecated APIs (marked with `@Suppress("DEPRECATION")`) for SMS PDU handling
- Accessibility service uses `Thread.sleep()` calls for timing (500ms delays between actions)
- The app requires screen lock to be disabled for optimal functionality
- WhatsApp must be installed on the device for forwarding to work
