package com.leetcode.tracker.api

import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Calendar
import java.util.concurrent.TimeUnit

data class LeetCodeUserData(
    val totalSolved: Int,
    val submissionCalendar: Map<String, Int>
)

class LeetCodeApi {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    
    fun getUserSubmissions(username: String): LeetCodeUserData? {
        try {
            val query = """
                {
                    matchedUser(username: "$username") {
                        submitStats {
                            acSubmissionNum {
                                difficulty
                                count
                                submissions
                            }
                        }
                        submissionCalendar
                    }
                }
            """.trimIndent()
            
            val json = JsonObject().apply {
                addProperty("query", query)
            }
            
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = json.toString().toRequestBody(mediaType)
            
            val request = Request.Builder()
                .url("https://leetcode.com/graphql")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Referer", "https://leetcode.com")
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                
                if (responseBody != null) {
                    val jsonResponse = gson.fromJson(responseBody, JsonObject::class.java)
                    val matchedUser = jsonResponse.getAsJsonObject("data")?.getAsJsonObject("matchedUser")
                    
                    if (matchedUser == null || matchedUser.isJsonNull) {
                        return null
                    }

                    // 1. Extract Calendar
                    val calendarJson = matchedUser.get("submissionCalendar")?.asString
                    val calendarMap = parseSubmissionCalendar(calendarJson)

                    // 2. Extract Actual Total Solved (All difficulties)
                    var totalSolved = 0
                    val submitStats = matchedUser.getAsJsonObject("submitStats")
                    val acSubmissionNum = submitStats?.getAsJsonArray("acSubmissionNum")
                    
                    acSubmissionNum?.forEach { element ->
                        val obj = element.asJsonObject
                        if (obj.get("difficulty").asString == "All") {
                            totalSolved = obj.get("count").asInt
                        }
                    }
                    
                    return LeetCodeUserData(totalSolved, calendarMap)
                }
            }
            
            return null
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    private fun parseSubmissionCalendar(calendarJson: String?): Map<String, Int> {
        if (calendarJson == null) return emptyMap()
        
        val result = mutableMapOf<String, Int>()
        
        try {
            val calendarData = gson.fromJson(calendarJson, JsonObject::class.java)
            
            calendarData.entrySet().forEach { entry ->
                val timestamp = entry.key.toLong()
                val count = entry.value.asInt
                
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = timestamp * 1000
                
                val dateKey = String.format(
                    "%d-%02d-%02d",
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH) + 1,
                    calendar.get(Calendar.DAY_OF_MONTH)
                )
                
                result[dateKey] = count
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return result
    }
}
