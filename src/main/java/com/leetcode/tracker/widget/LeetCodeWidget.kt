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
                    val userData = leetCodeApi.getUserSubmissions(userId)

                    if (userData != null) {
                        val data = userData.submissionCalendar
                        val streak = calculateCurrentStreak(data)
                        val total = userData.totalSolved
                        val todayKey = String.format(
                            "%d-%02d-%02d",
                            Calendar.getInstance().get(Calendar.YEAR),
                            Calendar.getInstance().get(Calendar.MONTH) + 1,
                            Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
                        )
                        val solvedToday = (data[todayKey] ?: 0) > 0

                        val heatmapBitmap = drawHeatmap(data)

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
            } else {
                views.setTextViewText(R.id.widgetStreak, "0")
                views.setTextViewText(R.id.widgetTotal, "0")
                views.setTextViewText(R.id.widgetTodayStatus, "Set User ID")
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }

        private fun drawHeatmap(data: Map<String, Int>): Bitmap {
            val weeksToShow = 20
            val daysInWeek = 7
            val cellSize = 20f
            val spacing = 4f
            val monthLabelHeight = 30f
            
            // FIXED: Gap equals one full column (cell width + spacing)
            val monthGap = cellSize + spacing 
            
            // 1. Calculate Width dynamically
            var currentX = 0f
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.WEEK_OF_YEAR, -(weeksToShow - 1))
            while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
                 calendar.add(Calendar.DAY_OF_YEAR, -1)
            }
            
            val startCal = calendar.clone() as Calendar
            var prevMonth = -1
            val today = Calendar.getInstance()
            
            // Simulation to determine bitmap width
            val simCal = startCal.clone() as Calendar
            while (!simCal.after(today)) {
                val month = simCal.get(Calendar.MONTH)
                val dayOfWeek = simCal.get(Calendar.DAY_OF_WEEK) - 1

                if (dayOfWeek == 0) {
                    currentX += cellSize + spacing
                }
                
                // Add full column gap for new month
                if (month != prevMonth && prevMonth != -1) {
                    currentX += monthGap
                }
                prevMonth = month
                simCal.add(Calendar.DAY_OF_YEAR, 1)
            }

            val width = currentX.toInt() + 20
            val height = ((cellSize + spacing) * daysInWeek + monthLabelHeight).toInt()
            
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint()
            val textPaint = Paint().apply {
                color = Color.parseColor("#8B949E") 
                textSize = 20f
                typeface = Typeface.DEFAULT_BOLD
                isAntiAlias = true
            }

            // 2. Actual Drawing
            currentX = 0f
            prevMonth = -1
            val drawCal = startCal.clone() as Calendar

            while (!drawCal.after(today)) {
                val month = drawCal.get(Calendar.MONTH)
                val dayOfWeek = drawCal.get(Calendar.DAY_OF_WEEK) - 1

                if (dayOfWeek == 0) {
                    currentX += cellSize + spacing
                }
                
                // Split logic: If month changes, skip a full column width (monthGap)
                if (month != prevMonth && prevMonth != -1) {
                    currentX += monthGap
                    // Draw label in the gap
                    val monthName = drawCal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.US)
                    canvas.drawText(monthName ?: "", currentX, height - 5f, textPaint)
                }
                prevMonth = month

                val dateKey = String.format(
                    "%d-%02d-%02d",
                    drawCal.get(Calendar.YEAR),
                    drawCal.get(Calendar.MONTH) + 1,
                    drawCal.get(Calendar.DAY_OF_MONTH)
                )

                val count = data[dateKey] ?: 0
                paint.color = getStreakColor(count)
                val top = dayOfWeek * (cellSize + spacing)
                
                canvas.drawRoundRect(
                    currentX, top, 
                    currentX + cellSize, top + cellSize, 
                    4f, 4f, 
                    paint
                )

                drawCal.add(Calendar.DAY_OF_YEAR, 1)
            }
            return bitmap
        }

        private fun getStreakColor(count: Int): Int {
            return when {
                count == 0 -> Color.parseColor("#2D333B") // Dark Gray
                count <= 2 -> Color.parseColor("#0E4429") // Dark Green
                count <= 5 -> Color.parseColor("#006D32") // Medium Green
                count <= 10 -> Color.parseColor("#26A641") // Bright Green
                else -> Color.parseColor("#39D353")       // Neon Green
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
