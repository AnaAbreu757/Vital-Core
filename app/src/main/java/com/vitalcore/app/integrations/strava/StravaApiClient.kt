package com.vitalcore.app.integrations.strava

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import javax.inject.Inject

data class StravaActivity(
    val id: Long,
    val name: String,
    val type: String,
    val startDate: Instant,
    val elapsedSeconds: Long,
    val averageHeartRate: Int?,
    val maxHeartRate: Int?,
    val calories: Double?,
    val distanceMeters: Double?,
)

/** Thin wrapper over the public Strava REST API (https://developers.strava.com/docs/reference/). Read-only. */
class StravaApiClient @Inject constructor() {

    suspend fun fetchRecentActivities(accessToken: String, afterEpochSeconds: Long, perPage: Int = 50): List<StravaActivity> =
        withContext(Dispatchers.IO) {
            val url = URL("https://www.strava.com/api/v3/athlete/activities?after=$afterEpochSeconds&per_page=$perPage")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer $accessToken")
                connectTimeout = 15_000
                readTimeout = 20_000
            }
            val code = connection.responseCode
            if (code !in 200..299) {
                val err = connection.errorStream?.bufferedReader()?.readText() ?: "HTTP $code"
                throw RuntimeException("Strava API error: $err")
            }
            val body = connection.inputStream.bufferedReader().readText()
            val array = Json.parseToJsonElement(body).jsonArray
            array.map { element ->
                val obj = element.jsonObject
                StravaActivity(
                    id = obj["id"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L,
                    name = obj["name"]?.jsonPrimitive?.content ?: "Strava activity",
                    type = obj["type"]?.jsonPrimitive?.content ?: "Workout",
                    startDate = obj["start_date"]?.jsonPrimitive?.content?.let { Instant.parse(it) } ?: Instant.now(),
                    elapsedSeconds = obj["elapsed_time"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L,
                    averageHeartRate = obj["average_heartrate"]?.jsonPrimitive?.content?.toDoubleOrNull()?.toInt(),
                    maxHeartRate = obj["max_heartrate"]?.jsonPrimitive?.content?.toDoubleOrNull()?.toInt(),
                    calories = obj["calories"]?.jsonPrimitive?.content?.toDoubleOrNull(),
                    distanceMeters = obj["distance"]?.jsonPrimitive?.content?.toDoubleOrNull(),
                )
            }
        }
}
