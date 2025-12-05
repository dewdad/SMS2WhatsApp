package com.iceloof.sms2whatsapp
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && event.packageName == Constants.WHATSAPP_PACKAGE) {
            val rootNode = rootInActiveWindow ?: return

            handler.postDelayed({
                try {
                    val sendButtonNode = findSendButton(rootNode)
                    if (sendButtonNode != null) {
                        sendButtonNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        android.util.Log.d("AccessibilityService", "Send button clicked successfully")

                        handler.postDelayed({
                            performGlobalAction(GLOBAL_ACTION_BACK)

                            handler.postDelayed({
                                performGlobalAction(GLOBAL_ACTION_HOME)
                                lockScreen()
                            }, Constants.DELAY_BEFORE_HOME)
                        }, Constants.DELAY_AFTER_SEND)
                    } else {
                        android.util.Log.e("AccessibilityService", "Send button not found")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AccessibilityService", "Error processing accessibility event", e)
                }
            }, Constants.DELAY_BEFORE_SEND)
        }
    }

    override fun onInterrupt() {
        // Handle service interruption
    }

    override fun onServiceConnected() {
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            packageNames = arrayOf(Constants.WHATSAPP_PACKAGE)
        }
        serviceInfo = info
    }

    private fun lockScreen() {
        val devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val componentName = ComponentName(this, MyDeviceAdminReceiver::class.java)
        if (devicePolicyManager.isAdminActive(componentName)) {
            devicePolicyManager.lockNow()
        }
    }

    private fun findNodeByContentDescription(root: AccessibilityNodeInfo, description: String): AccessibilityNodeInfo? {
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            if (child.contentDescription?.toString() == description) {
                return child
            }
            val result = findNodeByContentDescription(child, description)
            if (result != null) {
                return result
            }
        }
        return null
    }

    /**
     * Enhanced send button finding with multiple detection strategies
     * Tries multiple methods to find the send button:
     * 1. Content description matching
     * 2. View ID matching
     * 3. Text content matching (for different languages)
     */
    private fun findSendButton(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // Strategy 1: Try content description
        var sendButton = findNodeByContentDescription(root, Constants.SEND_BUTTON_DESCRIPTION)
        if (sendButton != null) {
            android.util.Log.d("AccessibilityService", "Found send button by content description")
            return sendButton
        }

        // Strategy 2: Try by View ID (WhatsApp's send button resource ID)
        sendButton = findNodeByViewId(root, "com.whatsapp:id/send")
        if (sendButton != null) {
            android.util.Log.d("AccessibilityService", "Found send button by view ID")
            return sendButton
        }

        // Strategy 3: Try alternative content descriptions for different languages
        val alternativeDescriptions = listOf("Send", "Enviar", "Senden", "Envoyer", "Invia", "보내기")
        for (desc in alternativeDescriptions) {
            sendButton = findNodeByContentDescription(root, desc)
            if (sendButton != null) {
                android.util.Log.d("AccessibilityService", "Found send button by alternative description: $desc")
                return sendButton
            }
        }

        android.util.Log.w("AccessibilityService", "Could not find send button with any strategy")
        return null
    }

    /**
     * Find node by View ID (resource name)
     */
    private fun findNodeByViewId(root: AccessibilityNodeInfo, viewId: String): AccessibilityNodeInfo? {
        if (root.viewIdResourceName == viewId) {
            return root
        }
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            if (child.viewIdResourceName == viewId) {
                return child
            }
            val result = findNodeByViewId(child, viewId)
            if (result != null) {
                return result
            }
        }
        return null
    }
}