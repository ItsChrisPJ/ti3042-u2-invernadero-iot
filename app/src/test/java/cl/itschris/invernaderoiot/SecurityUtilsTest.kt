package cl.itschris.invernaderoiot

import cl.itschris.invernaderoiot.security.SecurityUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias para verificar la seguridad criptográfica bajo ISO 27400.
 */
class SecurityUtilsTest {

    @Test
    fun testGenerateSaltIsRandomAndUnique() {
        val salt1 = SecurityUtils.generateSalt()
        val salt2 = SecurityUtils.generateSalt()

        assertEquals(32, salt1.length) // 16 bytes en hexadecimal = 32 caracteres
        assertEquals(32, salt2.length)
        assertNotEquals("Dos salts consecutivos no deben ser iguales", salt1, salt2)
    }

    @Test
    fun testHashPasswordGeneratesValidSha256() {
        val salt = "abcdef1234567890abcdef1234567890"
        val password = "mipasswordseguro123"

        val hash1 = SecurityUtils.hashPassword(password, salt)
        val hash2 = SecurityUtils.hashPassword(password, salt)

        assertEquals("El hash debe ser determinista para la misma sal y contraseña", hash1, hash2)
        assertEquals(64, hash1.length) // SHA-256 en hexadecimal = 64 caracteres
    }

    @Test
    fun testVerifyPasswordSuccess() {
        val salt = SecurityUtils.generateSalt()
        val password = "adminPassword123"
        val hash = SecurityUtils.hashPassword(password, salt)

        val isValid = SecurityUtils.verifyPassword(password, salt, hash)
        assertTrue("La verificación debe ser exitosa con la contraseña correcta", isValid)
    }

    @Test
    fun testVerifyPasswordFailureWithWrongPassword() {
        val salt = SecurityUtils.generateSalt()
        val password = "adminPassword123"
        val hash = SecurityUtils.hashPassword(password, salt)

        val isValid = SecurityUtils.verifyPassword("wrongPassword", salt, hash)
        assertFalse("La verificación debe fallar con una contraseña errónea", isValid)
    }

    @Test
    fun testSanitizeInput() {
        val dirty = "  admin'; DROP TABLE users; --  "
        val clean = SecurityUtils.sanitizeInput(dirty)
        assertFalse(clean.contains("'"))
        assertFalse(clean.contains(";"))
    }
}
