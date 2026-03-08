package com.leetcode.tracker

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.snackbar.Snackbar
import com.leetcode.tracker.api.LeetCodeApi
import com.leetcode.tracker.api.LeetCodeUserData
import com.leetcode.tracker.notifications.DailyReminderReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {
    
    private lateinit var userIdInput: EditText
    private lateinit var saveButton: Button
    private lateinit var streakHeatmap: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var totalSolvedText: TextView
    private lateinit var currentStreakText: TextView
    private lateinit var setReminderButton: Button
    
    private val leetCodeApi = LeetCodeApi()
    private val sharedPrefs by lazy {
        getSharedPreferences("LeetCodeTracker", Context.MODE_PRIVATE)
    }
    
    companion object {
        private const val NOTIFICATION_PERMISSION_CODE = 100
        const val NOTIFICATION_CHANNEL_ID = "leetcode_reminders"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        
        setContentView(R.layout.activity_main)
        
        val rootView = findViewById<View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                top = insets.top,
                bottom = insets.bottom,
                left = insets.left,
                right = insets.right
            )
            WindowInsetsCompat.CONSUMED
        }
        
        initViews()
        createNotificationChannel()
        requestNotificationPermission()
        
        val savedUserId = sharedPrefs.getString("user_id", "")
        if (!savedUserId.isNullOrEmpty()) {
            userIdInput.setText(savedUserId)
            loadUserData(savedUserId)
        }
        
        saveButton.setOnClickListener {
            val userId = userIdInput.text.toString().trim()
            if (userId.isNotEmpty()) {
                saveUserId(userId)
                loadUserData(userId)
            } else {
                Toast.makeText(this, "Please enter a user ID", Toast.LENGTH_SHORT).show()
            }
        }
        
        setReminderButton.setOnClickListener {
            showTimePickerDialog()
        }
    }
    
    private fun initViews() {
        userIdInput = findViewById(R.id.userIdInput)
        saveButton = findViewById(R.id.saveButton)
        streakHeatmap = findViewById(R.id.streakHeatmap)
        progressBar = findViewById(R.id.progressBar)
        totalSolvedText = findViewById(R.id.totalSolvedText)
        currentStreakText = findViewById(R.id.currentStreakText)
        setReminderButton = findViewById(R.id.setReminderButton)
    }
    
    private fun saveUserId(userId: String) {
        sharedPrefs.edit().putString("user_id", userId).apply()
        Toast.makeText(this, "User ID saved", Toast.LENGTH_SHORT).show()
    }
    
    private fun loadUserData(userId: String) {
        progressBar.visibility = View.VISIBLE
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userData = leetCodeApi.getUserSubmissions(userId)
                
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    if (userData != null) {
                        displayHeatmap(userData.submissionCalendar)
                        displayStats(userData)
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Failed to load data. Check user ID.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@MainActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun displayHeatmap(data: Map<String, Int>) {
        val daysToShow = 365
        val cellSize = 20f
        val cellSpacing = 4f
        val textHeight = 40f
        val daysInWeek = 7
        val monthGap = cellSize + cellSpacing

        // ── Compute max submissions across all days for relative color scaling ──
        val maxCount = data.values.maxOrNull() ?: 1

        // 1. Calculate width dynamically
        var currentX = 0f
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -(daysToShow - 1))
        
        while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        val startCalendar = calendar.clone() as Calendar

        val simCal = startCalendar.clone() as Calendar
        var prevMonth = -1
        
        while (!simCal.after(Calendar.getInstance())) {
            val month = simCal.get(Calendar.MONTH)
            val dayOfWeek = simCal.get(Calendar.DAY_OF_WEEK) - 1

            if (dayOfWeek == 0) {
                currentX += cellSize + cellSpacing
            }
            
            if (month != prevMonth && prevMonth != -1) {
                currentX += monthGap
            }
            prevMonth = month
            simCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        val width = currentX.toInt() + 40
        val height = ((cellSize + cellSpacing) * daysInWeek + textHeight).toInt()

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        val textPaint = Paint().apply {
            color = Color.parseColor("#8B949E")
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

        // 2. Draw cells
        currentX = 0f
        val drawCal = startCalendar.clone() as Calendar
        prevMonth = -1
        val today = Calendar.getInstance()

        while (!drawCal.after(today)) {
            val month = drawCal.get(Calendar.MONTH)
            val dayOfWeek = drawCal.get(Calendar.DAY_OF_WEEK) - 1
            
            if (dayOfWeek == 0) {
                currentX += cellSize + cellSpacing
            }
            
            if (month != prevMonth && prevMonth != -1) {
                currentX += monthGap
                val monthName = drawCal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.US)
                canvas.drawText(monthName ?: "", currentX, height - 10f, textPaint)
            }
            prevMonth = month

            val dateKey = String.format(
                "%d-%02d-%02d",
                drawCal.get(Calendar.YEAR),
                drawCal.get(Calendar.MONTH) + 1,
                drawCal.get(Calendar.DAY_OF_MONTH)
            )

            val count = data[dateKey] ?: 0

            // ── Pass maxCount so color is relative to user's own activity ──
            paint.color = getStreakColor(count, maxCount)

            val top = dayOfWeek * (cellSize + cellSpacing)
            canvas.drawRoundRect(
                currentX, top,
                currentX + cellSize, top + cellSize,
                4f, 4f,
                paint
            )

            drawCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        streakHeatmap.setImageBitmap(bitmap)
    }

    /**
     * Returns a green color whose lightness (HSV value) scales continuously
     * with how many submissions were made relative to the user's personal max.
     *
     *  count == 0          → flat dark gray (#2D333B), no activity
     *  count == 1, max=10  → percentage 10%  → very light green (value ≈ 0.72)
     *  count == 10, max=10 → percentage 100% → rich dark green  (value = 0.30)
     *
     * Formula:  value = 0.75 - (percentage * 0.45)
     *   • Lightest possible : 0.75  (1 submission when max is huge)
     *   • Darkest possible  : 0.30  (matches personal max)
     */
    private fun getStreakColor(count: Int, maxCount: Int): Int {
        if (count == 0) return Color.parseColor("#2D333B") // no activity

        val percentage = count.toFloat() / maxCount.toFloat() // 0.0 → 1.0

        // More submissions  → lower value → darker green
        // Fewer submissions → higher value → lighter green
        val value = 0.75f - (percentage * 0.45f)

        return Color.HSVToColor(floatArrayOf(
            120f,   // Hue   – pure green
            0.80f,  // Saturation – vivid but not neon
            value   // Value – varies smoothly by submission count
        ))
    }
    
    private fun displayStats(userData: LeetCodeUserData) {
        val total = userData.totalSolved
        val streak = calculateCurrentStreak(userData.submissionCalendar)
        
        totalSolvedText.text = "Total Solved: $total"
        currentStreakText.text = "Current Streak: $streak days"
    }
    
    private fun calculateCurrentStreak(data: Map<String, Int>): Int {
        var streak = 0
        val calendar = Calendar.getInstance()
        
        while (true) {
            val dateKey = String.format(
                "%d-%02d-%02d",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            
            if ((data[dateKey] ?: 0) > 0) {
                streak++
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }
        
        return streak
    }
    
    private fun showTimePickerDialog() {
        val savedHour = sharedPrefs.getInt("reminder_hour", 20)
        val savedMinute = sharedPrefs.getInt("reminder_minute", 0)
        
        TimePickerDialog(this, { _, hourOfDay, minute ->
            sharedPrefs.edit()
                .putInt("reminder_hour", hourOfDay)
                .putInt("reminder_minute", minute)
                .putBoolean("reminder_enabled", true)
                .apply()
            
            scheduleReminder(hourOfDay, minute)
            
            Snackbar.make(
                findViewById(android.R.id.content),
                "Reminder set for ${String.format("%02d:%02d", hourOfDay, minute)}",
                Snackbar.LENGTH_LONG
            ).show()
            
        }, savedHour, savedMinute, true).show()
    }
    
    private fun scheduleReminder(hour: Int, minute: Int) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, DailyReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            
            if (timeInMillis < System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "LeetCode Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily reminders to solve LeetCode problems"
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_CODE
                )
            }
        }
    }
}
