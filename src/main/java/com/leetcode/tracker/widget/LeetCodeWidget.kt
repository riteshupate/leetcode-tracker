package com.leetcode.tracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.leetcode.tracker.MainActivity
import com.leetcode.tracker.R
import com.leetcode.tracker.api.LeetCodeApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class LeetCodeWidget : AppWidgetProvider() {
    
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        if (intent.action == "com.leetcode.tracker.REFRESH_WIDGET") {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, LeetCodeWidget::class.java)
            )
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }
    
    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val sharedPrefs = context.getSharedPreferences("LeetCodeTracker", Context.MODE_PRIVATE)
            val userId = sharedPrefs.getString("user_id", "") ?: ""
            
            val views = RemoteViews(context.packageName, R.layout.widget_leetcode)
            
            // Set click to open app
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widgetContainer, pendingIntent)
            
            if (userId.isNotEmpty()) {
                CoroutineScope(Dispatchers.IO).launch {
                    val leetCodeApi = LeetCodeApi()
                    val data = leetCodeApi.getUserSubmissions(userId)
                    
                    if (data != null) {
                        val streak = calculateCurrentStreak(data)
                        val total = data.values.sum()
                        val todayKey = getTodayKey()
                        val solvedToday = data[todayKey] ?: 0
                        
                        views.setTextViewText(R.id.widgetStreak, "$streak")
                        views.setTextViewText(R.id.widgetTotal, "$total")
                        views.setTextViewText(
                            R.id.widgetTodayStatus,
                            if (solvedToday > 0) "✓ Solved today" else "⚠ Not solved yet"
                        )
                        
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                }
            } else {
                views.setTextViewText(R.id.widgetStreak, "0")
                views.setTextViewText(R.id.widgetTotal, "0")
                views.setTextViewText(R.id.widgetTodayStatus, "Set user ID in app")
            }
            
            appWidgetManager.updateAppWidget(appWidgetId, views)
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
}
