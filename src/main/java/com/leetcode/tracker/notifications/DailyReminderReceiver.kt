package com.leetcode.tracker.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.leetcode.tracker.MainActivity
import com.leetcode.tracker.R
import com.leetcode.tracker.api.LeetCodeApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class DailyReminderReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        val sharedPrefs = context.getSharedPreferences("LeetCodeTracker", Context.MODE_PRIVATE)
        val reminderEnabled = sharedPrefs.getBoolean("reminder_enabled", false)
        
        if (reminderEnabled) {
            checkAndNotify(context)
        }
    }
    
    private fun checkAndNotify(context: Context) {
        val sharedPrefs = context.getSharedPreferences("LeetCodeTracker", Context.MODE_PRIVATE)
        val userId = sharedPrefs.getString("user_id", "") ?: ""
        
        if (userId.isEmpty()) {
            return
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            val leetCodeApi = LeetCodeApi()
            val data = leetCodeApi.getUserSubmissions(userId)
            
            if (data != null) {
                val todayKey = getTodayKey()
                val solvedToday = data[todayKey] ?: 0
                
                if (solvedToday == 0) {
                    showNotification(context)
                }
            }
        }
    }
    
    private fun showNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, MainActivity.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("LeetCode Reminder")
            .setContentText("Don't forget to solve a problem today! Keep your streak alive!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        
        notificationManager.notify(1, notification)
    }
    
    private fun getTodayKey(): String {
        val calendar = Calendar.getInstance()
        return String.format(
            "%d-%02d-%02d",
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }
}
