package com.example.mobileproject1

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class RespiratoryRateSensing : AppCompatActivity() {

    private lateinit var getVid2: Button
    private lateinit var back2: Button
    private lateinit var rrvalue: TextView
    private fun finalizeResult(rrValue: String) {
        val resultIntent = Intent()
        resultIntent.putExtra("rrValue", rrValue)
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    private val pickCsvLauncher =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
            if (uris.isEmpty()) return@registerForActivityResult
            if (uris.size != 3) {
                return@registerForActivityResult
            }

            lifecycleScope.launch {
                val sortedUris = uris.sortedBy { uriLastPathSegment(it) }
                val x = readCsv(sortedUris[0])
                val y = readCsv(sortedUris[1])
                val z = readCsv(sortedUris[2])
                if (x.isEmpty() || y.isEmpty() || z.isEmpty()) {
                    return@launch
                }
                val rrpm = respiratoryRateCalculator(x, y, z)
                val rrpmText = "$rrpm bpm"
                rrvalue.text = rrpmText

                finalizeResult(rrpmText)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_respiratory_rate_sensing)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        getVid2 = findViewById(R.id.getVid2)
        back2 = findViewById(R.id.back2)
        rrvalue = findViewById(R.id.rr1)

        getVid2.setOnClickListener {
            pickCsvLauncher.launch(arrayOf("text/*", "text/csv", "application/csv"))
        }

        // just resets
        back2.setOnClickListener {
            finalizeResult(rrvalue.text.toString())
        }
    }

    private fun uriLastPathSegment(uri: Uri): String =
        uri.lastPathSegment ?: uri.toString()

    private suspend fun readCsv(uri: Uri): MutableList<Float> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Float>()
        contentResolver.openInputStream(uri)?.bufferedReader().use { br ->
            br?.lineSequence()
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.forEach { line ->
                    line.split(' ')
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .mapNotNull { it.toFloatOrNull() }
                        .let { list.addAll(it) }
                }
        }
        list
    }

    suspend fun respiratoryRateCalculator(
        accelValuesX: MutableList<Float>,
        accelValuesY: MutableList<Float>,
        accelValuesZ: MutableList<Float>,
    ): Int {
        var previousValue = 10f
        var k = 0
        for (i in 11 until accelValuesY.size) {
            val currentValue = sqrt(
                accelValuesZ[i].toDouble().pow(2.0) +
                        accelValuesX[i].toDouble().pow(2.0) +
                        accelValuesY[i].toDouble().pow(2.0)
            ).toFloat()
            if (abs(previousValue - currentValue) > 0.15f) {
                k++
            }
            previousValue = currentValue
        }
        val ret = (k.toDouble() / 45.0)
        return (ret * 30).toInt()
    }
}
