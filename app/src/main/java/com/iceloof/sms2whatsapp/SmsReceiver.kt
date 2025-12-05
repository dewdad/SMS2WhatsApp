package com.iceloof.sms2whatsapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.telephony.SmsMessage
import android.util.Log
import android.widget.Toast

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.provider.Telephony.SMS_RECEIVED") {
            val sendTo = SecurePreferences.getPhoneNumber(context)
            val bundle: Bundle? = intent.extras
            Log.d("SmsReceiver", "From: $bundle tO:$sendTo")
            if (bundle != null && sendTo != null) {
                var wakeLock: PowerManager.WakeLock? = null
                try {
                    @Suppress("SpellCheckingInspection") val pdus = bundle["pdus"] as Array<*>
                    val format = bundle.getString("format")
                    val messages = pdus.map { pdu ->
                        SmsMessage.createFromPdu(pdu as ByteArray, format)
                    }
                    val message = messages.joinToString(separator = "") { it.messageBody }
                    val sender = messages.first().originatingAddress

                    // Wake up the device
                    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                    wakeLock = pm.newWakeLock(
                        PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                        Constants.WAKE_LOCK_TAG
                    )
                    wakeLock.acquire(Constants.WAKE_LOCK_TIMEOUT)

                    // Log the message
                    Log.d("SmsReceiver", "From: $sender, Message: $message")
                    // Forward the message via WhatsApp
                    forwardMessageViaWhatsApp(context, sendTo, sender, message)
                } catch (e: Exception) {
                    Log.e("SmsReceiver", "Error processing SMS", e)
                    Toast.makeText(context, "Error forwarding SMS", Toast.LENGTH_SHORT).show()
                } finally {
                    // Ensure wakeLock is released even if an exception occurs
                    wakeLock?.let {
                        if (it.isHeld) {
                            it.release()
                        }
                    }
                }
            }
        }
    }

    private fun forwardMessageViaWhatsApp(context: Context, sendTo: String?, sender: String?, message: String) {
        try {
            if (sendTo.isNullOrEmpty()) {
                Log.e("SmsReceiver", "Cannot forward: recipient phone number is empty")
                Toast.makeText(context, "No recipient configured", Toast.LENGTH_SHORT).show()
                return
            }

            val sendIntent = Intent(Intent.ACTION_VIEW)
            sendIntent.setPackage(Constants.WHATSAPP_PACKAGE)
            sendIntent.data = Uri.parse("https://api.whatsapp.com/send?phone=$sendTo&text=${Uri.encode("From: $sender\nMsg: $message")}")
            sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            context.startActivity(sendIntent)
            Log.d("SmsReceiver", "Successfully launched WhatsApp to forward message")
        } catch (ex: android.content.ActivityNotFoundException) {
            Log.e("SmsReceiver", "WhatsApp not installed", ex)
            Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("SmsReceiver", "Error launching WhatsApp", e)
            Toast.makeText(context, "Error launching WhatsApp", Toast.LENGTH_SHORT).show()
        }
    }
}