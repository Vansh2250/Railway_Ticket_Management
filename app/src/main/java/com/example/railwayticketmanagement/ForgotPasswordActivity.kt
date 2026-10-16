package com.example.railwayticketmanagement

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.railwayticketmanagement.utils.SupabaseAuthService
import com.example.railwayticketmanagement.utils.SupabaseConfig
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    // Header
    private lateinit var btnHeaderBack: ImageButton

    // Step Indicators
    private lateinit var tvStep1Indicator: TextView
    private lateinit var tvStep2Indicator: TextView
    private lateinit var tvStep3Indicator: TextView

    // Loading Progress
    private lateinit var layoutOtpProgress: LinearLayout
    private lateinit var tvOtpProgressMsg: TextView
    private lateinit var btnConfigureSupabase: MaterialButton

    // Step 1: Email
    private lateinit var layoutStep1Email: LinearLayout
    private lateinit var etForgotEmail: TextInputEditText
    private lateinit var btnSendOtp: MaterialButton

    // Step 2: Supabase OTP
    private lateinit var layoutStep2Otp: LinearLayout
    private lateinit var tvOtpSentNotice: TextView
    private lateinit var etForgotOtp: TextInputEditText
    private lateinit var btnVerifyOtp: MaterialButton
    private lateinit var btnResendOtp: MaterialButton

    // Step 3: New Password
    private lateinit var layoutStep3Password: LinearLayout
    private lateinit var etForgotNewPassword: TextInputEditText
    private lateinit var etForgotConfirmPassword: TextInputEditText
    private lateinit var btnResetPassword: MaterialButton

    // State
    private var verifiedEmail: String = ""
    private var supabaseAccessToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        dbHelper = DatabaseHelper(this)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        btnHeaderBack = findViewById(R.id.btnHeaderBack)
        btnHeaderBack.visibility = View.VISIBLE

        tvStep1Indicator = findViewById(R.id.tvStep1Indicator)
        tvStep2Indicator = findViewById(R.id.tvStep2Indicator)
        tvStep3Indicator = findViewById(R.id.tvStep3Indicator)

        layoutOtpProgress = findViewById(R.id.layoutOtpProgress)
        tvOtpProgressMsg = findViewById(R.id.tvOtpProgressMsg)
        btnConfigureSupabase = findViewById(R.id.btnConfigureSupabase)

        layoutStep1Email = findViewById(R.id.layoutStep1Email)
        etForgotEmail = findViewById(R.id.etForgotEmail)
        btnSendOtp = findViewById(R.id.btnSendOtp)

        layoutStep2Otp = findViewById(R.id.layoutStep2Otp)
        tvOtpSentNotice = findViewById(R.id.tvOtpSentNotice)
        etForgotOtp = findViewById(R.id.etForgotOtp)
        btnVerifyOtp = findViewById(R.id.btnVerifyOtp)
        btnResendOtp = findViewById(R.id.btnResendOtp)

        layoutStep3Password = findViewById(R.id.layoutStep3Password)
        etForgotNewPassword = findViewById(R.id.etForgotNewPassword)
        etForgotConfirmPassword = findViewById(R.id.etForgotConfirmPassword)
        btnResetPassword = findViewById(R.id.btnResetPassword)
    }

    private fun setupListeners() {
        btnHeaderBack.setOnClickListener {
            finish()
        }

        btnConfigureSupabase.setOnClickListener {
            showConfigureSupabaseDialog()
        }

        btnSendOtp.setOnClickListener {
            handleSendSupabaseOtp()
        }

        btnVerifyOtp.setOnClickListener {
            handleVerifySupabaseOtp()
        }

        btnResendOtp.setOnClickListener {
            handleResendSupabaseOtp()
        }

        btnResetPassword.setOnClickListener {
            handleResetPassword()
        }
    }

    private fun handleSendSupabaseOtp() {
        val email = etForgotEmail.text.toString().trim()

        if (email.isEmpty()) {
            etForgotEmail.error = "Please enter your email"
            etForgotEmail.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etForgotEmail.error = "Please enter a valid email address"
            etForgotEmail.requestFocus()
            return
        }

        // Verify that the email is registered in SQLite
        if (!dbHelper.checkEmailExists(email)) {
            etForgotEmail.error = "This email is not registered. Please register first."
            etForgotEmail.requestFocus()
            return
        }

        if (!SupabaseConfig.isConfigured(this)) {
            showConfigureSupabaseDialog(onSaved = {
                handleSendSupabaseOtp()
            })
            return
        }

        verifiedEmail = email
        setLoading(true, "Requesting OTP from Supabase Auth...")

        SupabaseAuthService.sendPasswordResetOtp(this, verifiedEmail) { isSuccess, message ->
            setLoading(false)
            if (isSuccess) {
                // Switch to Step 2
                layoutStep1Email.visibility = View.GONE
                layoutStep2Otp.visibility = View.VISIBLE
                layoutStep3Password.visibility = View.GONE

                tvOtpSentNotice.text = "6-digit OTP code sent to $verifiedEmail via Supabase Auth."
                updateStepIndicators(activeStep = 2)

                AlertDialog.Builder(this)
                    .setTitle("Supabase OTP Dispatched 📬")
                    .setIcon(R.drawable.ic_train_logo)
                    .setMessage("A real 6-digit verification code has been dispatched to $verifiedEmail via Supabase.\n\nPlease check your email inbox (and spam folder) and enter the code below.")
                    .setPositiveButton("Got It", null)
                    .show()
            } else {
                AlertDialog.Builder(this)
                    .setTitle("Supabase OTP Error")
                    .setMessage(message)
                    .setPositiveButton("Configure Settings") { _, _ ->
                        showConfigureSupabaseDialog()
                    }
                    .setNegativeButton("OK", null)
                    .show()
            }
        }
    }

    private fun handleResendSupabaseOtp() {
        setLoading(true, "Resending OTP via Supabase Auth...")
        SupabaseAuthService.sendPasswordResetOtp(this, verifiedEmail) { isSuccess, message ->
            setLoading(false)
            if (isSuccess) {
                Toast.makeText(this, "New OTP sent to $verifiedEmail via Supabase!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Failed to resend: $message", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun handleVerifySupabaseOtp() {
        val enteredOtp = etForgotOtp.text.toString().trim()

        if (enteredOtp.isEmpty()) {
            etForgotOtp.error = "Please enter the 6-digit OTP"
            etForgotOtp.requestFocus()
            return
        }

        if (enteredOtp.length != 6 || !enteredOtp.all { it.isDigit() }) {
            etForgotOtp.error = "OTP must be exactly 6 digits"
            etForgotOtp.requestFocus()
            return
        }

        setLoading(true, "Verifying OTP with Supabase...")

        SupabaseAuthService.verifyOtp(this, verifiedEmail, enteredOtp) { isSuccess, accessToken, message ->
            setLoading(false)
            if (isSuccess) {
                supabaseAccessToken = accessToken
                Toast.makeText(this, "Supabase OTP Verified Successfully!", Toast.LENGTH_SHORT).show()

                // Switch to Step 3
                layoutStep1Email.visibility = View.GONE
                layoutStep2Otp.visibility = View.GONE
                layoutStep3Password.visibility = View.VISIBLE

                updateStepIndicators(activeStep = 3)
            } else {
                etForgotOtp.error = message
                etForgotOtp.requestFocus()
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun handleResetPassword() {
        val newPassword = etForgotNewPassword.text.toString()
        val confirmPassword = etForgotConfirmPassword.text.toString()

        if (newPassword.isEmpty()) {
            etForgotNewPassword.error = "Please enter a new password"
            etForgotNewPassword.requestFocus()
            return
        }

        if (newPassword.length < 4) {
            etForgotNewPassword.error = "Password must be at least 4 characters"
            etForgotNewPassword.requestFocus()
            return
        }

        if (newPassword != confirmPassword) {
            etForgotConfirmPassword.error = "Passwords do not match"
            etForgotConfirmPassword.requestFocus()
            return
        }

        // 1. Update password in local SQLite database
        val updated = dbHelper.updatePasswordByEmail(verifiedEmail, newPassword)

        // 2. Also update password in Supabase Auth if session token is available
        val token = supabaseAccessToken
        if (token != null) {
            SupabaseAuthService.updateSupabasePassword(this, token, newPassword) { _, _ -> }
        }

        if (updated) {
            AlertDialog.Builder(this)
                .setTitle("Password Reset Successful! 🎉")
                .setMessage("Your password for $verifiedEmail has been updated.\n\nYou can now log in using your new password.")
                .setPositiveButton("Back to Login") { _, _ ->
                    finish()
                }
                .setCancelable(false)
                .show()
        } else {
            Toast.makeText(this, "Failed to update password. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showConfigureSupabaseDialog(onSaved: (() -> Unit)? = null) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_configure_supabase, null)
        val etUrl = dialogView.findViewById<TextInputEditText>(R.id.etDialogSupabaseUrl)
        val etKey = dialogView.findViewById<TextInputEditText>(R.id.etDialogSupabaseAnonKey)

        etUrl.setText(SupabaseConfig.getUrl(this))
        etKey.setText(SupabaseConfig.getAnonKey(this))

        AlertDialog.Builder(this)
            .setTitle("Supabase Configuration")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newUrl = etUrl.text.toString().trim()
                val newKey = etKey.text.toString().trim()
                if (newUrl.isNotBlank() && newKey.isNotBlank()) {
                    SupabaseConfig.saveCredentials(this, newUrl, newKey)
                    Toast.makeText(this, "Supabase settings saved!", Toast.LENGTH_SHORT).show()
                    onSaved?.invoke()
                } else {
                    Toast.makeText(this, "URL and Anon Key cannot be empty.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNeutralButton("Test Connection") { _, _ ->
                val newUrl = etUrl.text.toString().trim()
                val newKey = etKey.text.toString().trim()
                if (newUrl.isBlank() || newKey.isBlank()) {
                    Toast.makeText(this, "Please enter both URL and Key to test.", Toast.LENGTH_SHORT).show()
                    return@setNeutralButton
                }
                SupabaseAuthService.testConnection(newUrl, newKey) { success, msg ->
                    AlertDialog.Builder(this)
                        .setTitle(if (success) "Connection Successful" else "Connection Failed")
                        .setMessage(msg)
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setLoading(loading: Boolean, message: String = "") {
        layoutOtpProgress.visibility = if (loading) View.VISIBLE else View.GONE
        tvOtpProgressMsg.text = message
        btnSendOtp.isEnabled = !loading
        btnVerifyOtp.isEnabled = !loading
        btnResendOtp.isEnabled = !loading
    }

    private fun updateStepIndicators(activeStep: Int) {
        val activeBg = getDrawable(R.drawable.bg_table_header)
        val primaryColor = getColor(R.color.primary)
        val mutedColor = getColor(R.color.text_secondary)

        when (activeStep) {
            1 -> {
                tvStep1Indicator.background = activeBg
                tvStep1Indicator.setTextColor(primaryColor)
                tvStep2Indicator.background = null
                tvStep2Indicator.setTextColor(mutedColor)
                tvStep3Indicator.background = null
                tvStep3Indicator.setTextColor(mutedColor)
            }
            2 -> {
                tvStep1Indicator.background = null
                tvStep1Indicator.setTextColor(mutedColor)
                tvStep2Indicator.background = activeBg
                tvStep2Indicator.setTextColor(primaryColor)
                tvStep3Indicator.background = null
                tvStep3Indicator.setTextColor(mutedColor)
            }
            3 -> {
                tvStep1Indicator.background = null
                tvStep1Indicator.setTextColor(mutedColor)
                tvStep2Indicator.background = null
                tvStep2Indicator.setTextColor(mutedColor)
                tvStep3Indicator.background = activeBg
                tvStep3Indicator.setTextColor(primaryColor)
            }
        }
    }
}
