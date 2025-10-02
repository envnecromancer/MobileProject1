package com.example.mobileproject1

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RatingBar
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.database.FirebaseDatabase

private const val HEARKEY = "hrValue"
private const val RESPKEY = "rrValue"
// send to firebase
data class HealthRecord(    val heartRate: String = "",
                            val respiratoryRate: String = "",
                            val symptom: String = "",
                            val rating: Float = 0.0f, val recordedAt: Long = 0L
)

class RecordHealth : AppCompatActivity() {

    private lateinit var hrValue1: TextView
    private lateinit var hrValue2: TextView
    private lateinit var symptomSpinner: Spinner
    private lateinit var starBar: RatingBar
    private lateinit var sendSave: Button

    private val healthDataLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            data?.getStringExtra(HEARKEY)?.let { newHrValue ->
                saveValue(HEARKEY, newHrValue)
            }

            data?.getStringExtra(RESPKEY)?.let { newRrValue ->
                saveValue(RESPKEY, newRrValue)
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_record_health)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        hrValue1 = findViewById(R.id.hr1)
        hrValue2 = findViewById(R.id.bpm2)
        symptomSpinner = findViewById(R.id.spinner)
        starBar = findViewById(R.id.ratingBar)

        val button3 = findViewById<Button>(R.id.button3)
        button3.setOnClickListener {
            val hrintent = Intent(this, HeartRateSensing::class.java)
            healthDataLauncher.launch(hrintent)
        }

        val button4 = findViewById<Button>(R.id.button4)
        button4.setOnClickListener {
            val rrintent = Intent(this, RespiratoryRateSensing::class.java)
            healthDataLauncher.launch(rrintent)
        }

        val home1 = findViewById<Button>(R.id.home1)
        home1.setOnClickListener {
            val homeint = Intent(this, MainActivity::class.java)
            startActivity(homeint)
        }

        val sendSave = findViewById<Button>(R.id.sendsave)
        sendSave.setOnClickListener {
            sendDataToPsql()
        }
    }

    override fun onResume() {
        super.onResume()
        updateVals()
    }
    private fun saveValue(key: String, value: String) {
        val sharedPref = getSharedPreferences("records", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString(key, value)
            apply()
        }
    }

    private fun updateVals() {
        val sharedPref = getSharedPreferences("records", Context.MODE_PRIVATE)
        hrValue1.text = sharedPref.getString(HEARKEY, "")
        hrValue2.text = sharedPref.getString(RESPKEY, "")
    }

    private fun sendDataToPsql() {
        val heartRate = hrValue1.text.toString()
        val respiratoryRate = hrValue2.text.toString()
        val spina = symptomSpinner.selectedItem.toString()
        val stb = starBar.rating

        if (heartRate.isBlank() || respiratoryRate.isBlank() || heartRate == "") {
            return
        }

        val database = FirebaseDatabase.getInstance().getReference("records")

        val recordId = database.push().key
        if (recordId == null) {
            return
        }

        val record = HealthRecord(
            heartRate = heartRate,
            respiratoryRate = respiratoryRate,
            symptom = spina,
            rating = stb,
            recordedAt = System.currentTimeMillis()
        )

        database.child(recordId).setValue(record)
    }



}