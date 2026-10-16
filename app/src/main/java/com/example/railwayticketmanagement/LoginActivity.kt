package com.example.railwayticketmanagement

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.railwayticketmanagement.utils.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class LoginActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var session: SessionManager

    private lateinit var etLoginEmail: TextInputEditText
    private lateinit var etLoginPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var tvForgotPassword: TextView
    private lateinit var tvGoToRegister: TextView
    private lateinit var btnHeaderBack: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        dbHelper = DatabaseHelper(this)
        session = SessionManager(this)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        btnHeaderBack = findViewById(R.id.btnHeaderBack)
        btnHeaderBack.visibility = View.VISIBLE

        etLoginEmail = findViewById(R.id.etLoginEmail)
        etLoginPassword = findViewById(R.id.etLoginPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        tvGoToRegister = findViewById(R.id.tvGoToRegister)
    }

    private fun setupListeners() {
        btnHeaderBack.setOnClickListener {
            finish()
        }

        btnLogin.setOnClickListener {
            handleLogin()
        }

        tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }
    }

    private fun handleLogin() {
        val email = etLoginEmail.text.toString().trim()
        val password = etLoginPassword.text.toString()

        if (email.isEmpty()) {
            etLoginEmail.error = "Please enter your email"
            etLoginEmail.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etLoginEmail.error = "Please enter a valid email address"
            etLoginEmail.requestFocus()
            return
        }

        if (password.isEmpty()) {
            etLoginPassword.error = "Please enter your password"
            etLoginPassword.requestFocus()
            return
        }

        val user = dbHelper.checkLogin(email, password)
        if (user != null) {
            // Save session in SharedPreferences
            session.setLoginSession(user.id, user.name, user.email, user.mobile)
            Toast.makeText(this, "Welcome back, ${user.name}!", Toast.LENGTH_SHORT).show()

            // Check if user came from a pending booking
            if (session.hasPendingBooking()) {
                val pendingTrainId = session.getPendingTrainId()
                val pendingJourneyDate = session.getPendingJourneyDate()
                session.clearPendingBooking()

                val bookIntent = Intent(this, BookTicketActivity::class.java).apply {
                    putExtra("EXTRA_TRAIN_ID", pendingTrainId)
                    putExtra("EXTRA_JOURNEY_DATE", pendingJourneyDate)
                }
                startActivity(bookIntent)
            }
            finish()
        } else {
            Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show()
        }
    }
}
