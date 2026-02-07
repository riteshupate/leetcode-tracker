package com.leetcode.tracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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

            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
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

                        // Draw the heatmap!
                        val heatmapBitmap = drawHeatmap(data)

                        views.setTextViewText(R.id.widgetStreak, "$streak")
                        views.setTextViewText(R.id.widgetTotal, "$total")
                        views.setTextViewText(
                            R.id.widgetTodayStatus,
                            if (solvedToday > 0) "✓ Solved" else "⚠ Not yet"
                        )
                        views.setTextColor(
                            R.id.widgetTodayStatus,
                            if (solvedToday > 0) Color.parseColor("#4CAF50") else Color.parseColor("#FFA726")
                        )
                        
                        // Set the drawn bitmap to the ImageView
                        views.setImageViewBitmap(R.id.widgetHeatmap, heatmapBitmap)

                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                }
            } else {
                views.setTextViewText(R.id.widgetStreak, "0")
                views.setTextViewText(R.id.widgetTotal, "0")
                views.setTextViewText(R.id.widgetTodayStatus, "Set User ID")
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }

        /**
         * Draws the last 20 weeks of activity onto a Bitmap
         */
        private fun drawHeatmap(data: Map<String, Int>): Bitmap {
            // Configuration
            val weeksToShow = 20 // Show last 20 weeks
            val daysInWeek = 7
            val cellSize = 20f
            val spacing = 4f
            
            // Calculate Bitmap dimensions
            val width = ((cellSize + spacing) * weeksToShow).toInt()
            val height = ((cellSize + spacing) * daysInWeek).toInt()
            
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint()

            val calendar = Calendar.getInstance()
            // Go back to the start of the period we want to show
            // Move back (weeksToShow - 1) weeks, then to the start of that week
            calendar.add(Calendar.WEEK_OF_YEAR, -(weeksToShow - 1))
            calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)

            for (w in 0 until weeksToShow) {
                for (d in 0 until daysInWeek) {
                    val dateKey = String.format(
                        "%d-%02d-%02d",
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH) + 1,
                        calendar.get(Calendar.DAY_OF_MONTH)
                    )

                    val count = data[dateKey] ?: 0
                    paint.color = getStreakColor(count)
                    
                    val left = w * (cellSize + spacing)
                    val top = d * (cellSize + spacing)
                    
                    // Draw rounded rectangle for the day
                    canvas.drawRoundRect(
                        left, top, 
                        left + cellSize, top + cellSize, 
                        4f, 4f, // Corner radius
                        paint
                    )

                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            return bitmap
        }

        private fun getStreakColor(count: Int): Int {
            return when {
                count == 0 -> Color.parseColor("#EBEDF0") // Gray
                count <= 2 -> Color.parseColor("#9BE9A8") // Light Green
                count <= 5 -> Color.parseColor("#40C463") // Medium Green
                count <= 10 -> Color.parseColor("#30A14E") // Dark Green
                else -> Color.parseColor("#216E39")       // Darkest Green
            }
        }

        private fun calculateCurrentStreak(data: Map<String, Int>): Int {
            var streak = 0
            val calendar = Calendar.getInstance()
            
            // Check yesterday if today is 0 to keep streak alive logic
            // But simple logic: verify backward from today
            
            // If today has 0, we can check if the streak is still valid from yesterday
            // For simplicity, let's just count backwards from today
             while (true) {
                val dateKey = String.format(
                    "%d-%02d-%02d",
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH) + 1,
                    calendar.get(Calendar.DAY_OF_MONTH)
                )
                
                // Special case: If it's today and 0, don't break streak yet, just continue to yesterday
                if (isToday(calendar) && (data[dateKey] ?: 0) == 0) {
                     calendar.add(Calendar.DAY_OF_YEAR, -1)
                     continue
                }

                if ((data[dateKey] ?: 0) > 0) {
                    streak++
                    calendar.add(Calendar.DAY_OF_YEAR, -1)
                } else {
                    break
                }
            }
            return streak
        }

        private fun isToday(cal: Calendar): Boolean {
            val today = Calendar.getInstance()
            return cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                   cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
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
