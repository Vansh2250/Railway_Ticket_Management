package com.example.railwayticketmanagement.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Configuration manager for Razorpay Payment Gateway integration.
 * Users can use the built-in test key or configure their own Key ID from
 * https://dashboard.razorpay.com/#/access/api-keys
 */
object RazorpayConfig {

    // Default Razorpay Test Key ID for development & demonstration
    const val DEFAULT_KEY_ID = "rzp_test_1DP5mmOlF5G5ag"

    private const val PREFS_NAME = "RazorpayConfigPrefs"
    private const val PREF_KEY_ID = "razorpay_key_id"

    fun getKeyId(context: Context): String {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(PREF_KEY_ID, null)?.takeIf { it.isNotBlank() } ?: DEFAULT_KEY_ID
    }

    fun setKeyId(context: Context, keyId: String) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(PREF_KEY_ID, keyId.trim()).apply()
    }

    fun isCustomKeySet(context: Context): Boolean {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(PREF_KEY_ID, null)
        return !saved.isNullOrBlank() && saved != DEFAULT_KEY_ID
    }
}
