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
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import com.leetcode.tracker.api.LeetCodeApi
import com.leetcode.tracker.notifications.DailyReminderReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class MainActivity : AppCompatActivity() {
    
    private lateinit var userIdInput: EditText
    private lateinit var saveButton: Button
    private lateinit var streakGrid: GridLayout
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
        setContentView(R.layout.activity_main)
        
        initViews()
        createNotificationChannel()
        requestNotificationPermission()
        
        // Load saved user ID
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
        streakGrid = findViewById(R.id.streakGrid)
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
                val submissionData = leetCodeApi.getUserSubmissions(userId)
                
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    if (submissionData != null) {
                        displayStreakMap(submissionData)
                        displayStats(submissionData)
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
    
    private fun displayStreakMap(data: Map<String, Int>) {
        streakGrid.removeAllViews()
        
        val calendar = Calendar.getInstance()
        val daysToShow = 365
        
        // Create grid of days (52 weeks x 7 days)
        for (week in 0 until 52) {
            for (day in 0 until 7) {
                val dayIndex = week * 7 + day
                if (dayIndex < daysToShow) {
                    val dateCalendar = Calendar.getInstance()
                    dateCalendar.add(Calendar.DAY_OF_YEAR, -(daysToShow - 1 - dayIndex))
                    
                    val dateKey = String.format(
                        "%d-%02d-%02d",
                        dateCalendar.get(Calendar.YEAR),
                        dateCalendar.get(Calendar.MONTH) + 1,
                        dateCalendar.get(Calendar.DAY_OF_MONTH)
                    )
                    
                    val view = View(this)
                    val size = resources.getDimensionPixelSize(R.dimen.streak_cell_size)
                    val margin = resources.getDimensionPixelSize(R.dimen.streak_cell_margin)
                    
                    val params = GridLayout.LayoutParams()
                    params.width = size
                    params.height = size
                    params.setMargins(margin, margin, margin, margin)
                    params.rowSpec = GridLayout.spec(day)
                    params.columnSpec = GridLayout.spec(week)
                    view.layoutParams = params
                    
                    // Set color based on activity
                    val count = data[dateKey] ?: 0
                    view.setBackgroundResource(getStreakColor(count))
                    
                    streakGrid.addView(view)
                }
            }
        }
    }
    
    private fun getStreakColor(count: Int): Int {
        return when {
            count == 0 -> R.drawable.streak_level_0
            count <= 2 -> R.drawable.streak_level_1
            count <= 5 -> R.drawable.streak_level_2
            count <= 10 -> R.drawable.streak_level_3
            else -> R.drawable.streak_level_4
        }
    }
    
    private fun displayStats(data: Map<String, Int>) {
        val total = data.values.sum()
        val streak = calculateCurrentStreak(data)
        
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
        
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
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
