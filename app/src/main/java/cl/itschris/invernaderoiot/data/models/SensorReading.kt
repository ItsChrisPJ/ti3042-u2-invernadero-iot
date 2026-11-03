package cl.itschris.invernaderoiot.data.models

/**
 * Modelo de datos para las lecturas de telemetría de sensores capturadas y almacenadas en SQLite.
 * Refleja las variables del Caso 1: Invernadero Inteligente (DHT11 + Actuador Ventilador).
 */
data class SensorReading(
    val id: Long = 0,
    val timestamp: String,
    val temperature: Double,
    val humidity: Double,
    val fanStatus: Boolean,
    val alertActive: Boolean,
    val mode: String
)
