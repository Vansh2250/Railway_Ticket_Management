package com.example.railwayticketmanagement

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.railwayticketmanagement.models.User
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class RegisterActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    private lateinit var etRegName: TextInputEditText
    private lateinit var etRegEmail: TextInputEditText
    private lateinit var etRegMobile: TextInputEditText
    private lateinit var etRegPassword: TextInputEditText
    private lateinit var etRegConfirmPassword: TextInputEditText
    private lateinit var btnRegister: MaterialButton
    private lateinit var tvGoToLogin: TextView
    private lateinit var btnHeaderBack: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        dbHelper = DatabaseHelper(this)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        btnHeaderBack = findViewById(R.id.btnHeaderBack)
        btnHeaderBack.visibility = View.VISIBLE

        etRegName = findViewById(R.id.etRegName)
        etRegEmail = findViewById(R.id.etRegEmail)
        etRegMobile = findViewById(R.id.etRegMobile)
        etRegPassword = findViewById(R.id.etRegPassword)
        etRegConfirmPassword = findViewById(R.id.etRegConfirmPassword)
        btnRegister = findViewById(R.id.btnRegister)
        tvGoToLogin = findViewById(R.id.tvGoToLogin)
    }

    private fun setupListeners() {
        btnHeaderBack.setOnClickListener {
            finish()
        }

        btnRegister.setOnClickListener {
            handleRegister()
        }

        tvGoToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun handleRegister() {
        val name = etRegName.text.toString().trim()
        val email = etRegEmail.text.toString().trim()
        val mobile = etRegMobile.text.toString().trim()
        val password = etRegPassword.text.toString()
        val confirmPassword = etRegConfirmPassword.text.toString()

        if (name.isEmpty()) {
            etRegName.error = "Please enter your name"
            etRegName.requestFocus()
            return
        }

        if (email.isEmpty()) {
            etRegEmail.error = "Please enter your email"
            etRegEmail.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etRegEmail.error = "Please enter a valid email address"
            etRegEmail.requestFocus()
            return
        }

        if (mobile.isEmpty()) {
            etRegMobile.error = "Please enter your mobile number"
            etRegMobile.requestFocus()
            return
        }

        if (mobile.length != 10 || !mobile.all { it.isDigit() }) {
            etRegMobile.error = "Mobile number must be exactly 10 digits"
            etRegMobile.requestFocus()
            return
        }

        if (password.isEmpty()) {
            etRegPassword.error = "Please enter a password"
            etRegPassword.requestFocus()
            return
        }

        if (password.length < 4) {
            etRegPassword.error = "Password must be at least 4 characters"
            etRegPassword.requestFocus()
            return
        }

        if (password != confirmPassword) {
            etRegConfirmPassword.error = "Passwords do not match"
            etRegConfirmPassword.requestFocus()
            return
        }

        // Check if email already exists
        if (dbHelper.checkEmailExists(email)) {
            etRegEmail.error = "Email is already registered. Please login."
            etRegEmail.requestFocus()
            return
        }

        // Save new user in SQLite
        val newUser = User(
            name = name,
            email = email,
            mobile = mobile,
            password = password
        )

        val insertedId = dbHelper.insertUser(newUser)
        if (insertedId > 0) {
            Toast.makeText(this, "Registration Successful. Please login.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else {
            Toast.makeText(this, "Failed to register user. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }
}
