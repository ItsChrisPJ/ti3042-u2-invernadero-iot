package cl.itschris.invernaderoiot.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import cl.itschris.invernaderoiot.data.models.SensorReading
import cl.itschris.invernaderoiot.data.models.User
import cl.itschris.invernaderoiot.security.SecurityUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Base de Datos Local SQLite para el Sistema Invernadero IoT.
 * 
 * Tributa a los Criterios de Evaluación:
 * - 2.1.2: Almacenamiento local de datos capturados por sensores sin dependencia de internet.
 * - 2.1.3: Norma ISO 27400: Almacenamiento seguro de credenciales con Hash SHA-256 + Salt.
 */
class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val TAG = "DatabaseHelper"
        private const val DATABASE_NAME = "invernadero_iot.db"
        private const val DATABASE_VERSION = 1

        // Tabla Usuarios
        const val TABLE_USERS = "users"
        const val COL_USER_ID = "id"
        const val COL_USER_USERNAME = "username"
        const val COL_USER_PASSWORD_HASH = "password_hash"
        const val COL_USER_SALT = "salt"
        const val COL_USER_FULL_NAME = "full_name"
        const val COL_USER_ROLE = "role"
        const val COL_USER_CREATED_AT = "created_at"

        // Tabla Mediciones de Sensores (DHT11 + Actuadores)
        const val TABLE_READINGS = "sensor_readings"
        const val COL_READING_ID = "id"
        const val COL_READING_TIMESTAMP = "timestamp"
        const val COL_READING_TEMP = "temperature"
        const val COL_READING_HUMIDITY = "humidity"
        const val COL_READING_FAN_STATUS = "fan_status"
        const val COL_READING_ALERT_ACTIVE = "alert_active"
        const val COL_READING_MODE = "mode"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createUsersTable = """
            CREATE TABLE $TABLE_USERS (
                $COL_USER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_USER_USERNAME TEXT UNIQUE NOT NULL,
                $COL_USER_PASSWORD_HASH TEXT NOT NULL,
                $COL_USER_SALT TEXT NOT NULL,
                $COL_USER_FULL_NAME TEXT NOT NULL,
                $COL_USER_ROLE TEXT NOT NULL,
                $COL_USER_CREATED_AT TEXT NOT NULL
            );
        """.trimIndent()

        val createReadingsTable = """
            CREATE TABLE $TABLE_READINGS (
                $COL_READING_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_READING_TIMESTAMP TEXT NOT NULL,
                $COL_READING_TEMP REAL NOT NULL,
                $COL_READING_HUMIDITY REAL NOT NULL,
                $COL_READING_FAN_STATUS INTEGER NOT NULL,
                $COL_READING_ALERT_ACTIVE INTEGER NOT NULL,
                $COL_READING_MODE TEXT NOT NULL
            );
        """.trimIndent()

        db.execSQL(createUsersTable)
        db.execSQL(createReadingsTable)

        Log.d(TAG, "Tablas SQLite creadas con éxito")

        // Precarga de usuarios de demostración (Contraseñas con Hash SHA-256 + Salt según ISO 27400)
        seedInitialUsers(db)
        seedSampleReadings(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_READINGS")
        onCreate(db)
    }

    private fun seedInitialUsers(db: SQLiteDatabase) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val now = dateFormat.format(Date())

        // Usuario 1: Administrador / Docente
        val saltAdmin = SecurityUtils.generateSalt()
        val hashAdmin = SecurityUtils.hashPassword("admin1234", saltAdmin)
        val adminValues = ContentValues().apply {
            put(COL_USER_USERNAME, "admin")
            put(COL_USER_PASSWORD_HASH, hashAdmin)
            put(COL_USER_SALT, saltAdmin)
            put(COL_USER_FULL_NAME, "Administrador IoT")
            put(COL_USER_ROLE, "Supervisor")
            put(COL_USER_CREATED_AT, now)
        }
        db.insert(TABLE_USERS, null, adminValues)

        // Usuario 2: Agricultor de campo
        val saltAgri = SecurityUtils.generateSalt()
        val hashAgri = SecurityUtils.hashPassword("iot2026", saltAgri)
        val agriValues = ContentValues().apply {
            put(COL_USER_USERNAME, "agricultor")
            put(COL_USER_PASSWORD_HASH, hashAgri)
            put(COL_USER_SALT, saltAgri)
            put(COL_USER_FULL_NAME, "Juan Pérez - Agricultor")
            put(COL_USER_ROLE, "Operador Agrícola")
            put(COL_USER_CREATED_AT, now)
        }
        db.insert(TABLE_USERS, null, agriValues)

        Log.d(TAG, "Usuarios demo iniciales precargados (admin / agricultor)")
    }

    private fun seedSampleReadings(db: SQLiteDatabase) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val currentTime = System.currentTimeMillis()

        val sampleData = listOf(
            Triple(23.5, 65.0, false),
            Triple(25.8, 62.0, false),
            Triple(29.2, 58.0, true),
            Triple(30.4, 55.0, true),
            Triple(26.1, 60.0, false)
        )

        sampleData.forEachIndexed { index, (temp, hum, fan) ->
            val timestamp = dateFormat.format(Date(currentTime - (index * 600000))) // Hace 10, 20... min
            val values = ContentValues().apply {
                put(COL_READING_TIMESTAMP, timestamp)
                put(COL_READING_TEMP, temp)
                put(COL_READING_HUMIDITY, hum)
                put(COL_READING_FAN_STATUS, if (fan) 1 else 0)
                put(COL_READING_ALERT_ACTIVE, if (temp > 28.0) 1 else 0)
                put(COL_READING_MODE, "AUTOMATICO")
            }
            db.insert(TABLE_READINGS, null, values)
        }
        Log.d(TAG, "Lecturas de telemetría de prueba precargadas")
    }

    // ==========================================
    // OPERACIONES DE USUARIOS (AUTENTICACIÓN)
    // ==========================================

    fun registerUser(username: String, passwordPlain: String, fullName: String, role: String): Boolean {
        val cleanUser = SecurityUtils.sanitizeInput(username).lowercase()
        val cleanName = SecurityUtils.sanitizeInput(fullName)

        if (cleanUser.isBlank() || passwordPlain.isBlank() || cleanName.isBlank()) {
            return false
        }

        val salt = SecurityUtils.generateSalt()
        val passwordHash = SecurityUtils.hashPassword(passwordPlain, salt)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        val values = ContentValues().apply {
            put(COL_USER_USERNAME, cleanUser)
            put(COL_USER_PASSWORD_HASH, passwordHash)
            put(COL_USER_SALT, salt)
            put(COL_USER_FULL_NAME, cleanName)
            put(COL_USER_ROLE, role)
            put(COL_USER_CREATED_AT, dateFormat.format(Date()))
        }

        val db = writableDatabase
        return try {
            val result = db.insertOrThrow(TABLE_USERS, null, values)
            result != -1L
        } catch (e: Exception) {
            Log.e(TAG, "Error registrando usuario: ${e.message}")
            false
        }
    }

    fun authenticateUser(username: String, passwordPlain: String): Pair<Boolean, User?> {
        val cleanUser = SecurityUtils.sanitizeInput(username).lowercase()
        val db = readableDatabase

        val cursor: Cursor = db.query(
            TABLE_USERS,
            arrayOf(COL_USER_ID, COL_USER_USERNAME, COL_USER_PASSWORD_HASH, COL_USER_SALT, COL_USER_FULL_NAME, COL_USER_ROLE, COL_USER_CREATED_AT),
            "$COL_USER_USERNAME = ?",
            arrayOf(cleanUser),
            null, null, null
        )

        cursor.use {
            if (it.moveToFirst()) {
                val salt = it.getString(it.getColumnIndexOrThrow(COL_USER_SALT))
                val expectedHash = it.getString(it.getColumnIndexOrThrow(COL_USER_PASSWORD_HASH))

                if (SecurityUtils.verifyPassword(passwordPlain, salt, expectedHash)) {
                    val user = User(
                        id = it.getLong(it.getColumnIndexOrThrow(COL_USER_ID)),
                        username = it.getString(it.getColumnIndexOrThrow(COL_USER_USERNAME)),
                        fullName = it.getString(it.getColumnIndexOrThrow(COL_USER_FULL_NAME)),
                        role = it.getString(it.getColumnIndexOrThrow(COL_USER_ROLE)),
                        createdAt = it.getString(it.getColumnIndexOrThrow(COL_USER_CREATED_AT))
                    )
                    return Pair(true, user)
                }
            }
        }
        return Pair(false, null)
    }

    // ==========================================
    // OPERACIONES DE MEDICIONES (TELEMETRÍA IOT)
    // ==========================================

    fun insertReading(reading: SensorReading): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_READING_TIMESTAMP, reading.timestamp)
            put(COL_READING_TEMP, reading.temperature)
            put(COL_READING_HUMIDITY, reading.humidity)
            put(COL_READING_FAN_STATUS, if (reading.fanStatus) 1 else 0)
            put(COL_READING_ALERT_ACTIVE, if (reading.alertActive) 1 else 0)
            put(COL_READING_MODE, reading.mode)
        }
        return db.insert(TABLE_READINGS, null, values)
    }

    fun getAllReadings(): List<SensorReading> {
        val list = mutableListOf<SensorReading>()
        val db = readableDatabase
        val cursor: Cursor = db.query(
            TABLE_READINGS,
            null, null, null, null, null,
            "$COL_READING_ID DESC"
        )

        cursor.use {
            while (it.moveToNext()) {
                val reading = SensorReading(
                    id = it.getLong(it.getColumnIndexOrThrow(COL_READING_ID)),
                    timestamp = it.getString(it.getColumnIndexOrThrow(COL_READING_TIMESTAMP)),
                    temperature = it.getDouble(it.getColumnIndexOrThrow(COL_READING_TEMP)),
                    humidity = it.getDouble(it.getColumnIndexOrThrow(COL_READING_HUMIDITY)),
                    fanStatus = it.getInt(it.getColumnIndexOrThrow(COL_READING_FAN_STATUS)) == 1,
                    alertActive = it.getInt(it.getColumnIndexOrThrow(COL_READING_ALERT_ACTIVE)) == 1,
                    mode = it.getString(it.getColumnIndexOrThrow(COL_READING_MODE))
                )
                list.add(reading)
            }
        }
        return list
    }

    fun clearAllReadings(): Int {
        val db = writableDatabase
        return db.delete(TABLE_READINGS, null, null)
    }

    fun preloadDemoReadings() {
        val db = writableDatabase
        seedSampleReadings(db)
    }
}
