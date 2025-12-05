package com.iceloof.sms2whatsapp

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecurePreferences {
    private const val PREFS_NAME = Constants.PREFS_NAME
    private const val KEY_PHONE_NUMBER = Constants.KEY_PHONE_NUMBER

    private fun getEncryptedPreferences(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun savePhoneNumber(context: Context, phoneNumber: String) {
        try {
            val prefs = getEncryptedPreferences(context)
            prefs.edit().putString(KEY_PHONE_NUMBER, phoneNumber).apply()
        } catch (e: Exception) {
            android.util.Log.e("SecurePreferences", "Error saving phone number", e)
            throw e
        }
    }

    fun getPhoneNumber(context: Context): String? {
        return try {
            val prefs = getEncryptedPreferences(context)
            prefs.getString(KEY_PHONE_NUMBER, null)
        } catch (e: Exception) {
            android.util.Log.e("SecurePreferences", "Error retrieving phone number", e)
            null
        }
    }
}
