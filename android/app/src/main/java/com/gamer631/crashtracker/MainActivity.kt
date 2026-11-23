package com.gamer631.crashtracker

import android.app.Activity
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.Build
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("round_history", MODE_PRIVATE) }
    private val rounds = mutableListOf<Double>()
    private lateinit var status: TextView
    private lateinit var detected: TextView
    private lateinit var history: TextView
    private lateinit var input: EditText
    private var latest: Double? = null
    private val captureRequest = 700

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val value = intent?.getDoubleExtra("multiplier", 0.0) ?: 0.0
            if (value > 0) {
                latest = value
                detected.text = "Detected: %.2fx".format(value)
                status.text = "Screen scan active · review before saving"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        load()
        buildUI()
        refresh()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun label(text: String, size: Float, color: Int = Color.WHITE) =
        TextView(this).apply { setText(text); textSize = size; setTextColor(color); setPadding(0, dp(7), 0, dp(7)) }

    private fun buildUI() {
        window.statusBarColor = Color.rgb(10, 14, 25)
        window.navigationBarColor = Color.rgb(10, 14, 25)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(12))
            setBackgroundColor(Color.rgb(10, 14, 25))
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(label("CRASH TRACKER", 24f, Color.rgb(94, 220, 255)))
        content.addView(label("SCREEN OCR · ROUND HISTORY · ANALYTICS", 12f, Color.LTGRAY))
        status = label("Screen tracking stopped", 14f, Color.rgb(160, 210, 170))
        content.addView(status)
        detected = label("Detected: —", 23f, Color.rgb(100, 220, 255))
        content.addView(detected)

        content.addView(Button(this).apply {
            text = "START SCREEN TRACKING"
            setOnClickListener { startCapture() }
        })
        content.addView(Button(this).apply {
            text = "STOP SCREEN TRACKING"
            setOnClickListener { stopCapture() }
        })
        content.addView(Button(this).apply {
            text = "SAVE DETECTED MULTIPLIER"
            setOnClickListener {
                val value = latest
                if (value != null) addRound(value)
                else Toast.makeText(this@MainActivity, "No multiplier detected yet", Toast.LENGTH_SHORT).show()
            }
        })

        input = EditText(this).apply {
            hint = "Manual multiplier (e.g. 2.45)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setSingleLine()
        }
        content.addView(input)
        content.addView(Button(this).apply {
            text = "ADD MANUAL ROUND"
            setOnClickListener {
                val value = input.text.toString().replace("x", "", true).trim().toDoubleOrNull()
                if (value != null && value >= 1.0) addRound(value)
                else Toast.makeText(this@MainActivity, "Enter a multiplier of 1.00 or higher", Toast.LENGTH_SHORT).show()
            }
        })

        content.addView(label("STATISTICS", 18f, Color.rgb(100, 220, 255)))
        history = label("", 15f)
        content.addView(history)
        content.addView(Button(this).apply {
            text = "UNDO LAST ROUND"
            setOnClickListener {
                if (rounds.isNotEmpty()) {
                    rounds.removeAt(rounds.lastIndex)
                    save()
                    refresh()
                }
            }
        })
        content.addView(Button(this).apply {
            text = "CLEAR HISTORY"
            setOnClickListener {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Clear saved rounds?")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Clear") { _, _ -> rounds.clear(); save(); refresh() }
                    .show()
            }
        })
        content.addView(label("OCR may misread values. Confirm before saving. Statistics describe past observations and cannot predict the next crash.", 12f, Color.LTGRAY))
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun startCapture() {
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(manager.createScreenCaptureIntent(), captureRequest)
    }

    @Deprecated("Activity result legacy API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == captureRequest && resultCode == RESULT_OK && data != null) {
            val serviceIntent = Intent(this, ScreenScanService::class.java)
                .putExtra("resultCode", resultCode)
                .putExtra("data", data)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(serviceIntent)
            else startService(serviceIntent)
            status.text = "Starting OCR…"
            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(receiver, IntentFilter(ScreenScanService.ACTION_MULTIPLIER), RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                registerReceiver(receiver, IntentFilter(ScreenScanService.ACTION_MULTIPLIER))
            }
        }
    }

    private fun stopCapture() {
        stopService(Intent(this, ScreenScanService::class.java))
        try { unregisterReceiver(receiver) } catch (_: Exception) { }
        status.text = "Screen tracking stopped"
    }

    private fun addRound(value: Double) {
        if (value < 1.0 || value.isNaN() || value.isInfinite()) {
            Toast.makeText(this, "Invalid multiplier", Toast.LENGTH_SHORT).show()
            return
        }
        rounds.add(value)
        save()
        input.text.clear()
        refresh()
        Toast.makeText(this, "Saved %.2fx".format(value), Toast.LENGTH_SHORT).show()
    }

    private fun refresh() {
        val count = rounds.size
        val sorted = rounds.sorted()
        val average = if (count > 0) rounds.average() else 0.0
        val median = if (count == 0) 0.0 else if (count % 2 == 1) sorted[count / 2] else (sorted[count / 2 - 1] + sorted[count / 2]) / 2.0
        val low = rounds.count { it < 2.0 }
        val high = rounds.count { it >= 10.0 }
        val recent = rounds.takeLast(40).reversed().mapIndexed { index, value -> "%02d  %.2fx".format(count - index, value) }.joinToString("\n")
        history.text = "Rounds: $count\nAverage: %.2fx · Median: %.2fx\nBelow 2x: %d (%.1f%%)\n10x+: %d (%.1f%%)\n\nRECENT ROUNDS\n%s".format(
            average, median, low, if (count > 0) 100.0 * low / count else 0.0,
            high, if (count > 0) 100.0 * high / count else 0.0, recent
        )
    }

    private fun save() {
        prefs.edit().putString("data", JSONArray(rounds).toString()).apply()
    }

    private fun load() {
        try {
            val data = JSONArray(prefs.getString("data", "[]"))
            for (index in 0 until data.length()) {
                val value = data.optDouble(index)
                if (value >= 1.0 && value.isFinite()) rounds.add(value)
            }
        } catch (_: Exception) { }
    }
}
