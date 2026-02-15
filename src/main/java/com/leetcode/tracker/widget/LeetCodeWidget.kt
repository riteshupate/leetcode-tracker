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
import android.graphics.Typeface
import android.widget.RemoteViews
import com.leetcode.tracker.MainActivity
import com.leetcode.tracker.R
import com.leetcode.tracker.api.LeetCodeApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

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
                    // Fetch the new data object containing both totalSolved and calendar
                    val userData = leetCodeApi.getUserSubmissions(userId)

                    if (userData != null) {
                        val data = userData.submissionCalendar
                        val total = userData.totalSolved
                        val streak = calculateCurrentStreak(data)
                        val todayKey = getTodayKey()
                        val solvedToday = (data[todayKey] ?: 0) > 0

                        // Draw the improved heatmap
                        val heatmapBitmap = drawHeatmap(data)

                        // Update TextViews with clear labels (No emojis)
                        views.setTextViewText(R.id.widgetStreak, "$streak Days")
                        views.setTextViewText(R.id.widgetTotal, "$total Solved")
                        
                        views.setTextViewText(
                            R.id.widgetTodayStatus,
                            if (solvedToday) "Completed" else "Unfinished"
                        )
                        
                        // Text Color: Green if done, Orange/Yellow if not
                        views.setTextColor(
                            R.id.widgetTodayStatus,
                            if (solvedToday) Color.parseColor("#39D353") else Color.parseColor("#FFA726")
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
         * Draws the heatmap with GitHub-style spacing and month labels
         */
        private fun drawHeatmap(data: Map<String, Int>): Bitmap {
            val weeksToShow = 20
            val daysInWeek = 7
            val cellSize = 20f
            val spacing = 4f
            val monthLabelHeight = 30f
            val monthGap = 15f // Gap between months
            
            // 1. Calculate Width dynamically to account for Month Gaps
            var currentX = 0f
            val calendar = Calendar.getInstance()
            // Move back (weeksToShow - 1) weeks to find start date
            calendar.add(Calendar.WEEK_OF_YEAR, -(weeksToShow - 1))
            calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
            
            // We need to simulate the loop once to get the exact bitmap width
            val simCalendar = calendar.clone() as Calendar
            for (w in 0 until weeksToShow) {
                val currentMonth = simCalendar.get(Calendar.MONTH)
                simCalendar.add(Calendar.WEEK_OF_YEAR, 1)
                val nextMonth = simCalendar.get(Calendar.MONTH)
                
                currentX += cellSize + spacing
                // Add extra space if the month changes
                if (currentMonth != nextMonth && w < weeksToShow - 1) {
                    currentX += monthGap 
                }
            }

            val width = currentX.toInt() + 20 // Add some padding
            val height = ((cellSize + spacing) * daysInWeek + monthLabelHeight).toInt()
            
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint()
            val textPaint = Paint().apply {
                color = Color.parseColor("#8B949E") // Gray text for months
                textSize = 20f
                typeface = Typeface.DEFAULT_BOLD
                isAntiAlias = true
            }

            // Reset Calendar and X position for actual drawing
            currentX = 0f
            
            for (w in 0 until weeksToShow) {
                // Check Month for Label
                val currentMonth = calendar.get(Calendar.MONTH)
                val monthName = calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.US)
                
                // Draw Month Name if it's the first week of that month displayed
                val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
                // Draw label if it's the start of the widget OR start of a month (~1st week)
                if (dayOfMonth <= 7 || w == 0) {
                     canvas.drawText(monthName ?: "", currentX, height - 5f, textPaint)
                }

                for (d in 0 until daysInWeek) {
                    val dateKey = String.format(
                        "%d-%02d-%02d",
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH) + 1,
                        calendar.get(Calendar.DAY_OF_MONTH)
                    )

                    val count = data[dateKey] ?: 0
                    paint.color = getStreakColor(count)
                    
                    val top = d * (cellSize + spacing)
                    
                    // Draw separated rounded rectangle for the day
                    canvas.drawRoundRect(
                        currentX, top, 
                        currentX + cellSize, top + cellSize, 
                        4f, 4f, 
                        paint
                    )

                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
                
                // Check if next week is a new month to add gap
                val checkNextCal = calendar.clone() as Calendar
                val nextMonth = checkNextCal.get(Calendar.MONTH)
                
                currentX += cellSize + spacing
                
                // Add spacing if month changes
                if (currentMonth != nextMonth && w < weeksToShow - 1) {
                     currentX += monthGap
                }
            }
            return bitmap
        }

        private fun getStreakColor(count: Int): Int {
            return when {
                count == 0 -> Color.parseColor("#2D333B") // Dark Gray (Empty/Background)
                count <= 2 -> Color.parseColor("#0E4429") // Dark Green (Low)
                count <= 5 -> Color.parseColor("#006D32") // Medium Green
                count <= 10 -> Color.parseColor("#26A641") // Bright Green
                else -> Color.parseColor("#39D353")       // Neon Green (High)
            }
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
