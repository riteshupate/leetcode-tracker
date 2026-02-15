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

    // ... (Keep existing onUpdate and onReceive) ...

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            // ... (Shared Prefs Logic) ...
            
            val views = RemoteViews(context.packageName, R.layout.widget_leetcode)
            
            // ... (Pending Intent Logic) ...

            if (userId.isNotEmpty()) {
                CoroutineScope(Dispatchers.IO).launch {
                    val leetCodeApi = LeetCodeApi()
                    val userData = leetCodeApi.getUserSubmissions(userId) // Returns Data Class now

                    if (userData != null) {
                        val data = userData.submissionCalendar
                        val total = userData.totalSolved // FIXED: Using real total
                        val streak = calculateCurrentStreak(data)
                        val todayKey = getTodayKey()
                        val solvedToday = (data[todayKey] ?: 0) > 0

                        // Draw the improved heatmap
                        val heatmapBitmap = drawHeatmap(data, context)

                        // FIXED: Removed Emojis, added clear labels
                        views.setTextViewText(R.id.widgetStreak, "$streak Days")
                        views.setTextViewText(R.id.widgetTotal, "$total Solved")
                        
                        views.setTextViewText(
                            R.id.widgetTodayStatus,
                            if (solvedToday) "Completed" else "Unfinished"
                        )
                        views.setTextColor(
                            R.id.widgetTodayStatus,
                            if (solvedToday) Color.parseColor("#39D353") else Color.parseColor("#FFA726")
                        )
                        
                        views.setImageViewBitmap(R.id.widgetHeatmap, heatmapBitmap)
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                }
            } 
            // ... (Else block) ...
        }

        /**
         * Draws the heatmap with GitHub-style spacing and month labels
         */
        private fun drawHeatmap(data: Map<String, Int>, context: Context): Bitmap {
            val weeksToShow = 20
            val daysInWeek = 7
            val cellSize = 20f
            val spacing = 4f
            val monthLabelHeight = 30f
            val monthGap = 15f // Gap between months
            
            // 1. Calculate Width dynamically to account for Month Gaps
            var currentX = 0f
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.WEEK_OF_YEAR, -(weeksToShow - 1))
            calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
            
            // We need to simulate the loop once to get exact width
            val simCalendar = calendar.clone() as Calendar
            for (w in 0 until weeksToShow) {
                val currentMonth = simCalendar.get(Calendar.MONTH)
                simCalendar.add(Calendar.WEEK_OF_YEAR, 1)
                val nextMonth = simCalendar.get(Calendar.MONTH)
                
                currentX += cellSize + spacing
                if (currentMonth != nextMonth && w < weeksToShow - 1) {
                    currentX += monthGap // Add extra space for new month
                }
            }

            val width = currentX.toInt() + 20 // Padding
            val height = ((cellSize + spacing) * daysInWeek + monthLabelHeight).toInt()
            
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint()
            val textPaint = Paint().apply {
                color = Color.parseColor("#8B949E") // Gray text
                textSize = 20f
                typeface = Typeface.DEFAULT_BOLD
                isAntiAlias = true
            }

            // Reset Calendar for actual drawing
            currentX = 0f
            
            for (w in 0 until weeksToShow) {
                // Check Month for Label
                val currentMonth = calendar.get(Calendar.MONTH)
                val monthName = calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.US)
                
                // Draw Month Name if it's the first week of that month displayed
                val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
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
                    
                    // FIXED: Blocks are separated
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
                
                // FIXED: Add spacing if month changes
                if (currentMonth != nextMonth && w < weeksToShow - 1) {
                     currentX += monthGap
                }
            }
            return bitmap
        }

        // FIXED: Bright green for high, Dark green for low (GitHub Dark Theme colors)
        private fun getStreakColor(count: Int): Int {
            return when {
                count == 0 -> Color.parseColor("#2D333B") // Dark Gray (Empty)
                count <= 2 -> Color.parseColor("#0E4429") // Dark Green
                count <= 5 -> Color.parseColor("#006D32") // Medium Green
                count <= 10 -> Color.parseColor("#26A641") // Bright Green
                else -> Color.parseColor("#39D353")       // Neon Green
            }
        }

        // ... (Keep existing helpers: calculateCurrentStreak, isToday, getTodayKey) ...
    }
}
