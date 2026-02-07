package com.leetcode.tracker.api

import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.util.Calendar
import java.util.concurrent.TimeUnit

class LeetCodeApi {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    
    fun getUserSubmissions(username: String): Map<String, Int>? {
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
            
            // FIXED: Use MediaType.parse instead of extension function
            val mediaType = MediaType.parse("application/json; charset=utf-8")
            
            // FIXED: Use RequestBody.create instead of extension function
            val body = RequestBody.create(mediaType, json.toString())
            
            val request = Request.Builder()
                .url("https://leetcode.com/graphql")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Referer", "https://leetcode.com")
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                // FIXED: Use .body() method to avoid "package-private" access error
                val responseBody = response.body()?.string()
                
                if (responseBody != null) {
                    val jsonResponse = gson.fromJson(responseBody, JsonObject::class.java)
                    val submissionCalendar = jsonResponse
                        .getAsJsonObject("data")
                        ?.getAsJsonObject("matchedUser")
                        ?.get("submissionCalendar")
                        ?.asString
                    
                    return parseSubmissionCalendar(submissionCalendar)
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