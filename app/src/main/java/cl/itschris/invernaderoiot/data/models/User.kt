package cl.itschris.invernaderoiot.data.models

/**
 * Modelo de datos para representar a un usuario del sistema Invernadero IoT.
 * Cumple con ISO 27400: separación de datos de identidad y roles de acceso.
 */
data class User(
    val id: Long = 0,
    val username: String,
    val fullName: String,
    val role: String,
    val createdAt: String
)
