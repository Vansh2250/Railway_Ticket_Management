package com.example.railwayticketmanagement.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Configuration manager for Supabase Authentication and Email OTP.
 * Retrieve your Project URL and Anon/Public Key from:
 * Supabase Dashboard -> Project Settings -> API
 */
object SupabaseConfig {

    // Default configuration (replace with your project URL & Anon key if desired)
    const val DEFAULT_SUPABASE_URL = "https://your-project-id.supabase.co"
    const val DEFAULT_SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

    private const val PREFS_NAME = "SupabaseConfigPrefs"
    private const val PREF_URL = "supabase_url"
    private const val PREF_ANON_KEY = "supabase_anon_key"

    fun getUrl(context: Context): String {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val url = prefs.getString(PREF_URL, null)?.takeIf { it.isNotBlank() } ?: DEFAULT_SUPABASE_URL
        return url.trim().removeSuffix("/")
    }

    fun getAnonKey(context: Context): String {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(PREF_ANON_KEY, null)?.takeIf { it.isNotBlank() } ?: DEFAULT_SUPABASE_ANON_KEY
    }

    fun saveCredentials(context: Context, url: String, anonKey: String) {
        val cleanUrl = url.trim().removeSuffix("/")
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(PREF_URL, cleanUrl)
            .putString(PREF_ANON_KEY, anonKey.trim())
            .apply()
    }

    fun isConfigured(context: Context): Boolean {
        val url = getUrl(context)
        val key = getAnonKey(context)
        return url.startsWith("https://") &&
                !url.contains("your-project-id") &&
                key.isNotBlank() &&
                !key.contains("your-anon-key") &&
                !key.contains("...")
    }
}
