//FemaApiService.kt
package edu.msoe.gilla.finalapp

import okhttp3.OkHttpClient
import okhttp3.Request
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonArray
import java.net.URLEncoder

class FemaApiService {
    private val client = OkHttpClient()
    private val gson = Gson()


    fun fetchAlerts(stateFilter: String): List<MainActivity.Alert> {
        val request = Request.Builder()
            .url("https://www.fema.gov/api/open/v2/DisasterDeclarationsSummaries?\$top=100")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Unexpected code $response")

            val body = response.body?.string() ?: throw Exception("No response body")
            val json = gson.fromJson(body, JsonObject::class.java)

            val alerts = mutableListOf<MainActivity.Alert>()
            val disasters: JsonArray = json.getAsJsonArray("DisasterDeclarationsSummaries")

            for (item in disasters) {
                val obj = item.asJsonObject
                val state = obj.get("state")?.asString ?: continue

                if (state != stateFilter) continue // only include matching state

                val title = obj.get("incidentType")?.asString ?: "Unknown Incident"
                val description = obj.get("declarationTitle")?.asString ?: "No description"
                val area = obj.get("designatedArea")?.asString ?: ""
                val locationName = "$area, $state"
                val (latitude, longitude) = geocodeLocation(locationName)

                if (latitude != 0.0 && longitude != 0.0) {
                    alerts.add(
                        MainActivity.Alert(
                            title = "$title in $state",
                            description = description,
                            latitude = latitude,
                            longitude = longitude,
                            radiusKm = 50.0
                        )
                    )
                }
            }

            return alerts
        }
    }



    private fun geocodeLocation(locationName: String): Pair<Double, Double> {
        val encodedLocation = URLEncoder.encode(locationName, "UTF-8")
        val url = "https://nominatim.openstreetmap.org/search?q=$encodedLocation&format=json&limit=1"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "FemaApp/1.0") // Nominatim requires a User-Agent
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return 0.0 to 0.0

            val body = response.body?.string() ?: return 0.0 to 0.0
            val jsonArray = gson.fromJson(body, JsonArray::class.java)

            if (jsonArray.size() > 0) {
                val obj = jsonArray[0].asJsonObject
                val lat = obj.get("lat")?.asString?.toDoubleOrNull() ?: 0.0
                val lon = obj.get("lon")?.asString?.toDoubleOrNull() ?: 0.0
                return lat to lon
            }
        }
        return 0.0 to 0.0
    }
}
