package com.iceloof.sms2whatsapp

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var phoneNumberEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var checkPermissionsButton: Button
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var compName: ComponentName

    // Activity result launchers for modern permission handling
    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val deniedPermissions = permissions.filterValues { !it }.keys
        if (deniedPermissions.isEmpty()) {
            Toast.makeText(this, "All permissions are granted", Toast.LENGTH_SHORT).show()
        } else {
            Log.d("MainActivity", "Denied permissions: $deniedPermissions")
        }
    }

    private val enableDeviceAdminLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (devicePolicyManager.isAdminActive(compName)) {
            Log.d("MainActivity", "Device admin enabled successfully")
        }
    }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (Settings.canDrawOverlays(this)) {
            Log.d("MainActivity", "Overlay permission granted")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar?.hide()
        Log.d("SmsReceiver", "app start")

        phoneNumberEditText = findViewById(R.id.phoneNumberEditText)
        saveButton = findViewById(R.id.saveButton)
        checkPermissionsButton = findViewById(R.id.checkPermissionsButton)

        saveButton.setOnClickListener {
            savePhoneNumber()
        }

        checkPermissionsButton.setOnClickListener {
            checkPermissions()
        }

        // Load saved phone number
        try {
            val savedPhoneNumber = SecurePreferences.getPhoneNumber(this)
            phoneNumberEditText.setText(savedPhoneNumber)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error loading phone number", e)
            Toast.makeText(this, "Error loading saved data", Toast.LENGTH_SHORT).show()
        }

    }

    private fun savePhoneNumber() {
        val phoneNumber = phoneNumberEditText.text.toString().trim()

        // Validate phone number
        if (phoneNumber.isEmpty()) {
            Toast.makeText(this, "Please enter a phone number", Toast.LENGTH_SHORT).show()
            return
        }

        // Basic phone number validation: must contain only digits, +, -, (, ), and spaces
        // Must have at least MIN_PHONE_DIGITS digits
        val digitsOnly = phoneNumber.replace(Regex("[^0-9]"), "")
        if (digitsOnly.length < Constants.MIN_PHONE_DIGITS) {
            Toast.makeText(this, "Invalid phone number. Must have at least ${Constants.MIN_PHONE_DIGITS} digits", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            SecurePreferences.savePhoneNumber(this, phoneNumber)
            Toast.makeText(this, "Phone number saved", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error saving phone number", e)
            Toast.makeText(this, "Error saving phone number", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkPermissions() {
        checkAndRequestPermissions()
        if (!isAccessibilityServiceEnabled(this, AccessibilityService::class.java)) {
            promptUserToEnableAccessibilityService()
        }

        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        compName = ComponentName(this, MyDeviceAdminReceiver::class.java)

        if (!devicePolicyManager.isAdminActive(compName)) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName)
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "This app require device admin permission.")
            enableDeviceAdminLauncher.launch(intent)
        }

        // Request SYSTEM_ALERT_WINDOW permission if not granted
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            overlayPermissionLauncher.launch(intent)
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.INTERNET,
            Manifest.permission.READ_SMS,
            Manifest.permission.BIND_ACCESSIBILITY_SERVICE,
            Manifest.permission.BIND_DEVICE_ADMIN
        )

        val permissionsToRequest = permissions.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            // All permissions are granted
            Toast.makeText(this, "All permissions are granted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun promptUserToEnableAccessibilityService() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
        Toast.makeText(this, "Please enable the accessibility service for this app.", Toast.LENGTH_LONG).show()
    }

    private fun isAccessibilityServiceEnabled(context: Context, service: Class<out AccessibilityService>): Boolean {
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        if (enabledServices.isNullOrEmpty()) {
            Log.d("Debug", "No enabled accessibility services found.")
            return false
        }

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)
        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.equals(ComponentName(context, service).flattenToString(), ignoreCase = true)) {
                return true
            }
        }
        return false
    }
}

