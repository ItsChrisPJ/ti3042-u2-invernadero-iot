package cl.itschris.invernaderoiot.data.models

import org.json.JSONObject

/**
 * Representa el paquete de telemetría transmitido inalámbricamente entre dispositivos Android.
 * Permite serializar y deserializar los datos de sensores DHT11 y estado de actuadores.
 */
data class GreenhouseTelemetry(
    val temperature: Double,
    val humidity: Double,
    val fanStatus: Boolean,
    val alertActive: Boolean,
    val mode: String,
    val timestamp: String
) {
    fun toJson(): String {
        val json = JSONObject()
        json.put("temp", temperature)
        json.put("hum", humidity)
        json.put("fan", fanStatus)
        json.put("alert", alertActive)
        json.put("mode", mode)
        json.put("time", timestamp)
        return json.toString()
    }

    companion object {
        fun fromJson(jsonStr: String): GreenhouseTelemetry? {
            return try {
                val json = JSONObject(jsonStr)
                GreenhouseTelemetry(
                    temperature = json.optDouble("temp", 0.0),
                    humidity = json.optDouble("hum", 0.0),
                    fanStatus = json.optBoolean("fan", false),
                    alertActive = json.optBoolean("alert", false),
                    mode = json.optString("mode", "AUTO"),
                    timestamp = json.optString("time", "")
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
