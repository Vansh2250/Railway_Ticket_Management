package com.example.railwayticketmanagement.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Service to handle Supabase Auth operations:
 * - Sending email OTP (Password recovery / verification)
 * - Verifying OTP codes
 * - Updating account password in Supabase
 */
object SupabaseAuthService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Sends a 6-digit recovery OTP to the user's email address via Supabase Auth.
     */
    fun sendPasswordResetOtp(
        context: Context,
        email: String,
        onResult: (isSuccess: Boolean, message: String) -> Unit
    ) {
        if (!SupabaseConfig.isConfigured(context)) {
            mainHandler.post {
                onResult(
                    false,
                    "Supabase is not configured yet. Please configure your Supabase Project URL and Anon Key in settings."
                )
            }
            return
        }

        val url = "${SupabaseConfig.getUrl(context)}/auth/v1/recover"
        val anonKey = SupabaseConfig.getAnonKey(context)

        val jsonBody = JSONObject().apply {
            put("email", email.trim())
        }.toString()

        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer $anonKey")
            .addHeader("Content-Type", "application/json")
            .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post {
                    onResult(false, "Network error contacting Supabase: ${e.localizedMessage ?: "Unknown error"}")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string() ?: ""
                val isSuccess = response.isSuccessful

                mainHandler.post {
                    if (isSuccess) {
                        onResult(true, "A 6-digit verification OTP has been sent to $email via Supabase Auth.")
                    } else {
                        val errorMsg = parseErrorMessage(responseBody, "Failed to send OTP (HTTP ${response.code})")
                        onResult(false, errorMsg)
                    }
                }
            }
        })
    }

    /**
     * Verifies the 6-digit OTP code against Supabase Auth.
     */
    fun verifyOtp(
        context: Context,
        email: String,
        token: String,
        onResult: (isSuccess: Boolean, accessToken: String?, message: String) -> Unit
    ) {
        if (!SupabaseConfig.isConfigured(context)) {
            mainHandler.post {
                onResult(
                    false,
                    null,
                    "Supabase is not configured yet. Please configure your Supabase Project URL and Anon Key."
                )
            }
            return
        }

        val url = "${SupabaseConfig.getUrl(context)}/auth/v1/verify"
        val anonKey = SupabaseConfig.getAnonKey(context)

        val jsonBody = JSONObject().apply {
            put("type", "recovery")
            put("email", email.trim())
            put("token", token.trim())
        }.toString()

        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer $anonKey")
            .addHeader("Content-Type", "application/json")
            .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post {
                    onResult(false, null, "Network error: ${e.localizedMessage ?: "Unable to verify OTP"}")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string() ?: ""
                val isSuccess = response.isSuccessful

                mainHandler.post {
                    if (isSuccess) {
                        var accessToken: String? = null
                        try {
                            val json = JSONObject(responseBody)
                            if (json.has("access_token")) {
                                accessToken = json.getString("access_token")
                            }
                        } catch (_: Exception) {}

                        onResult(true, accessToken, "Supabase OTP verified successfully!")
                    } else {
                        val errorMsg = parseErrorMessage(responseBody, "Invalid or expired OTP code.")
                        onResult(false, null, errorMsg)
                    }
                }
            }
        })
    }

    /**
     * Updates the password in Supabase Auth if an access token is available.
     */
    fun updateSupabasePassword(
        context: Context,
        accessToken: String,
        newPassword: String,
        onResult: (isSuccess: Boolean, message: String) -> Unit
    ) {
        if (!SupabaseConfig.isConfigured(context)) {
            mainHandler.post { onResult(false, "Supabase not configured") }
            return
        }

        val url = "${SupabaseConfig.getUrl(context)}/auth/v1/user"
        val anonKey = SupabaseConfig.getAnonKey(context)

        val jsonBody = JSONObject().apply {
            put("password", newPassword)
        }.toString()

        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json")
            .put(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post {
                    onResult(false, e.localizedMessage ?: "Error updating Supabase password")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string() ?: ""
                val isSuccess = response.isSuccessful
                mainHandler.post {
                    if (isSuccess) {
                        onResult(true, "Password updated in Supabase Auth.")
                    } else {
                        val errorMsg = parseErrorMessage(responseBody, "Could not update password in Supabase.")
                        onResult(false, errorMsg)
                    }
                }
            }
        })
    }

    /**
     * Tests connectivity to Supabase project with provided credentials.
     */
    fun testConnection(
        url: String,
        anonKey: String,
        onResult: (isSuccess: Boolean, message: String) -> Unit
    ) {
        val cleanUrl = url.trim().removeSuffix("/")
        val testUrl = "$cleanUrl/auth/v1/settings"

        val request = Request.Builder()
            .url(testUrl)
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer $anonKey")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post {
                    onResult(false, "Failed to connect: ${e.localizedMessage}")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val isSuccess = response.isSuccessful
                mainHandler.post {
                    if (isSuccess) {
                        onResult(true, "Successfully connected to Supabase Auth API!")
                    } else {
                        onResult(false, "Connection failed (HTTP ${response.code}). Check URL & Anon Key.")
                    }
                }
            }
        })
    }

    private fun parseErrorMessage(rawJson: String, defaultMsg: String): String {
        return try {
            val json = JSONObject(rawJson)
            when {
                json.has("msg") -> json.getString("msg")
                json.has("message") -> json.getString("message")
                json.has("error_description") -> json.getString("error_description")
                else -> defaultMsg
            }
        } catch (_: Exception) {
            defaultMsg
        }
    }
}
