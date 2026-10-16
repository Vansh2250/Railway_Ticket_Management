package com.example.railwayticketmanagement

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.railwayticketmanagement.models.Booking
import com.example.railwayticketmanagement.models.Passenger
import com.example.railwayticketmanagement.models.Train
import com.example.railwayticketmanagement.utils.RazorpayConfig
import com.example.railwayticketmanagement.utils.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import org.json.JSONObject

class PaymentActivity : AppCompatActivity(), PaymentResultWithDataListener {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var session: SessionManager

    private var train: Train? = null
    private var journeyDate: String = ""
    private var passengers: ArrayList<Passenger> = arrayListOf()
    private var totalFare: Double = 0.0

    private lateinit var tvPaymentTrainName: TextView
    private lateinit var tvPaymentJourneyDate: TextView
    private lateinit var tvPaymentPassengerCount: TextView
    private lateinit var tvPaymentTotalFare: TextView
    private lateinit var rgPaymentMethod: RadioGroup
    private lateinit var btnPayNow: MaterialButton
    private lateinit var btnConfigureRazorpay: MaterialButton
    private lateinit var btnHeaderBack: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        // Preload Razorpay Checkout resources for faster launch
        Checkout.preload(applicationContext)

        dbHelper = DatabaseHelper(this)
        session = SessionManager(this)

        if (!session.isLoggedIn()) {
            Toast.makeText(this, "Session expired. Please log in.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        extractIntentData()

        if (train == null || passengers.isEmpty()) {
            Toast.makeText(this, "Invalid booking information", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        populateDetails()
        setupListeners()
    }

    @Suppress("DEPRECATION", "UNCHECKED_CAST")
    private fun extractIntentData() {
        train = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("EXTRA_TRAIN", Train::class.java)
        } else {
            intent.getSerializableExtra("EXTRA_TRAIN") as? Train
        }

        journeyDate = intent.getStringExtra("EXTRA_JOURNEY_DATE") ?: ""

        val list = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("EXTRA_PASSENGER_LIST", ArrayList::class.java) as? ArrayList<Passenger>
        } else {
            intent.getSerializableExtra("EXTRA_PASSENGER_LIST") as? ArrayList<Passenger>
        }
        passengers = list ?: arrayListOf()

        totalFare = intent.getDoubleExtra("EXTRA_TOTAL_FARE", 0.0)
    }

    private fun initViews() {
        btnHeaderBack = findViewById(R.id.btnHeaderBack)
        btnHeaderBack.visibility = View.VISIBLE

        tvPaymentTrainName = findViewById(R.id.tvPaymentTrainName)
        tvPaymentJourneyDate = findViewById(R.id.tvPaymentJourneyDate)
        tvPaymentPassengerCount = findViewById(R.id.tvPaymentPassengerCount)
        tvPaymentTotalFare = findViewById(R.id.tvPaymentTotalFare)
        rgPaymentMethod = findViewById(R.id.rgPaymentMethod)
        btnConfigureRazorpay = findViewById(R.id.btnConfigureRazorpay)
        btnPayNow = findViewById(R.id.btnPayNow)
    }

    private fun populateDetails() {
        val currentTrain = train!!
        tvPaymentTrainName.text = "${currentTrain.trainName} (#${currentTrain.trainNumber})"
        tvPaymentJourneyDate.text = journeyDate
        tvPaymentPassengerCount.text = "${passengers.size} Passenger(s)"
        tvPaymentTotalFare.text = "₹${String.format("%.2f", totalFare)}"
        btnPayNow.text = "Pay ₹${String.format("%.2f", totalFare)} with Razorpay 🔒"
    }

    private fun setupListeners() {
        btnHeaderBack.setOnClickListener {
            finish()
        }

        btnConfigureRazorpay.setOnClickListener {
            showConfigureRazorpayDialog()
        }

        btnPayNow.setOnClickListener {
            startRazorpayCheckout()
        }
    }

    private fun startRazorpayCheckout() {
        val currentUserId = session.getUserId()
        if (currentUserId == -1L) {
            Toast.makeText(this, "Session not found. Please log in again.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val checkout = Checkout()
        val keyId = RazorpayConfig.getKeyId(this)
        checkout.setKeyID(keyId)

        try {
            val options = JSONObject()
            options.put("name", "Railway Reservation System")
            options.put("description", "Booking: ${train?.trainName} (${train?.trainNumber})")
            options.put("theme.color", "#0D47A1")
            options.put("currency", "INR")

            // Amount in paise (1 INR = 100 paise)
            val amountInPaise = Math.round(totalFare * 100)
            options.put("amount", amountInPaise)

            // Prefill customer details from session
            val prefill = JSONObject()
            val userEmail = session.getUserEmail()
            val userMobile = session.getUserMobile()
            if (userEmail.isNotBlank()) prefill.put("email", userEmail)
            if (userMobile.isNotBlank()) prefill.put("contact", userMobile)
            options.put("prefill", prefill)

            val retryObj = JSONObject()
            retryObj.put("enabled", true)
            retryObj.put("max_count", 4)
            options.put("retry", retryObj)

            checkout.open(this, options)
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to launch Razorpay: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        val txnId = razorpayPaymentId ?: paymentData?.paymentId ?: "pay_${System.currentTimeMillis()}"
        val currentUserId = session.getUserId()
        val currentTrain = train ?: return

        val booking = Booking(
            userId = currentUserId,
            trainId = currentTrain.trainId,
            journeyDate = journeyDate,
            passengerCount = passengers.size,
            totalFare = totalFare,
            bookingStatus = "CONFIRMED",
            bookingDate = ""
        )

        val bookingId = dbHelper.createBooking(booking, passengers, "Razorpay ($txnId)")

        if (bookingId > 0) {
            Toast.makeText(this, "Payment Successful! Transaction ID: $txnId", Toast.LENGTH_LONG).show()
            val confirmationIntent = Intent(this, BookingConfirmationActivity::class.java).apply {
                putExtra("EXTRA_BOOKING_ID", bookingId)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(confirmationIntent)
            finish()
        } else {
            Toast.makeText(
                this,
                "Payment was received ($txnId), but tickets could not be booked due to seat unavailability.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onPaymentError(code: Int, response: String?, paymentData: PaymentData?) {
        val errorMsg = when (code) {
            Checkout.NETWORK_ERROR -> "Network error during Razorpay checkout. Please check internet connection."
            Checkout.INVALID_OPTIONS -> "Invalid payment options provided to Razorpay: $response"
            else -> "Payment Cancelled / Incomplete: ${response ?: "Transaction could not be completed"}"
        }
        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
    }

    private fun showConfigureRazorpayDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_configure_razorpay, null)
        val etKey = dialogView.findViewById<TextInputEditText>(R.id.etDialogRazorpayKey)
        etKey.setText(RazorpayConfig.getKeyId(this))

        AlertDialog.Builder(this)
            .setTitle("Razorpay Settings")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val enteredKey = etKey.text.toString().trim()
                if (enteredKey.isNotBlank()) {
                    RazorpayConfig.setKeyId(this, enteredKey)
                    Toast.makeText(this, "Razorpay Key ID saved!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Key ID cannot be blank.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNeutralButton("Reset to Default") { _, _ ->
                RazorpayConfig.setKeyId(this, RazorpayConfig.DEFAULT_KEY_ID)
                Toast.makeText(this, "Reset to default test key!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
