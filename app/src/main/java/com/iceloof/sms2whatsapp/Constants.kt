package com.iceloof.sms2whatsapp

object Constants {
    // WhatsApp package names
    const val WHATSAPP_PACKAGE = "com.whatsapp"
    const val WHATSAPP_BUSINESS_PACKAGE = "com.whatsapp.w4b"

    // UI element identifiers for WhatsApp
    const val SEND_BUTTON_DESCRIPTION = "Send"

    // Preferences
    const val PREFS_NAME = "SMS2WhatsAppPreferences"
    const val KEY_PHONE_NUMBER = "phoneNumber"

    // WakeLock
    const val WAKE_LOCK_TAG = "SMS2WhatsApp::MyWakelockTag"
    const val WAKE_LOCK_TIMEOUT = 15000L // 15 seconds

    // Delays for AccessibilityService (in milliseconds)
    const val DELAY_BEFORE_SEND = 500L
    const val DELAY_AFTER_SEND = 500L
    const val DELAY_BEFORE_HOME = 500L

    // Phone number validation
    const val MIN_PHONE_DIGITS = 10
}
