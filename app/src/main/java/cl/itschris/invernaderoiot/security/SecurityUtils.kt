package cl.itschris.invernaderoiot.security

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Utilidades de Seguridad bajo el Estándar ISO/IEC 27400 para Aplicaciones IoT y Entornos OT.
 * 
 * Principios aplicados:
 * 1. Protección de Credenciales: Contraseñas almacenadas exclusivamente con Hash criptográfico
 *    SHA-256 enriquecido con Salt aleatorio de 16 bytes, evitando texto plano en SQLite.
 * 2. Integridad de Datos: Hashing unidireccional y validación estricta de entradas.
 * 3. Principio de Mínimo Privilegio: Ningún dato sensible o credencial es expuesto en logs.
 */
object SecurityUtils {

    private const val HASH_ALGORITHM = "SHA-256"
    private const val SALT_BYTES = 16

    /**
     * Genera un Salt criptográficamente seguro usando SecureRandom.
     */
    fun generateSalt(): String {
        val random = SecureRandom()
        val salt = ByteArray(SALT_BYTES)
        random.nextBytes(salt)
        return bytesToHex(salt)
    }

    /**
     * Genera un hash SHA-256 combinando la contraseña en texto plano y su salt único.
     */
    fun hashPassword(password: String, salt: String): String {
        val digest = MessageDigest.getInstance(HASH_ALGORITHM)
        val combined = "$salt:$password"
        val hashBytes = digest.digest(combined.toByteArray(Charsets.UTF_8))
        return bytesToHex(hashBytes)
    }

    /**
     * Verifica si una contraseña coincide con el hash esperado utilizando el salt almacenado.
     */
    fun verifyPassword(password: String, salt: String, expectedHash: String): Boolean {
        val calculatedHash = hashPassword(password, salt)
        return calculatedHash.equals(expectedHash, ignoreCase = true)
    }

    /**
     * Sanitiza cadenas de entrada para prevenir inyecciones o caracteres de control.
     */
    fun sanitizeInput(input: String): String {
        return input.trim().replace("'", "").replace(";", "").replace("\"", "")
    }

    /**
     * Convierte un arreglo de bytes en representación hexadecimal legible.
     */
    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = "0123456789abcdef"
        val result = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val i = b.toInt() and 0xFF
            result.append(hexChars[i shr 4])
            result.append(hexChars[i and 0x0F])
        }
        return result.toString()
    }
}
