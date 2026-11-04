package cl.itschris.invernaderoiot.network

import android.os.Handler
import android.os.Looper
import android.util.Log
import cl.itschris.invernaderoiot.data.models.GreenhouseTelemetry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Gestor Singleton del Servidor IoT del Invernadero.
 * 
 * Permite que el servidor socket permanezca activo en segundo plano dentro del dispositivo,
 * permitiendo tanto la interconexión entre 2 teléfonos distintos como la prueba local
 * (loopback 127.0.0.1) al navegar entre las pantallas de la aplicación.
 */
object GreenhouseServerManager {

    private const val TAG = "GreenhouseServerManager"
    private var socketServer: GreenhouseSocketServer? = null

    // Estado del Invernadero compartido
    var currentTemp: Double = 24.5
    var currentHumidity: Double = 62.0
    var isFanOn: Boolean = false
    var isAutoMode: Boolean = true

    // Callbacks para la actividad del Nodo si está abierta
    var onNodeStateChanged: (() -> Unit)? = null
    var onLogListener: ((String) -> Unit)? = null
    var onClientStatusListener: ((Boolean, String) -> Unit)? = null

    private val handler = Handler(Looper.getMainLooper())
    private var isLoopRunning = false

    private val telemetryRunnable = object : Runnable {
        override fun run() {
            if (!isServerRunning()) return

            // Automatización: Encender ventilador si T > 28°C
            if (isAutoMode) {
                if (currentTemp > 28.0 && !isFanOn) {
                    isFanOn = true
                    log("Auto: Temperatura > 28°C -> Activando ventilador")
                    onNodeStateChanged?.invoke()
                } else if (currentTemp <= 25.0 && isFanOn) {
                    isFanOn = false
                    log("Auto: Temperatura normalizada -> Apagando ventilador")
                    onNodeStateChanged?.invoke()
                }
            }

            val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val telemetry = GreenhouseTelemetry(
                temperature = currentTemp,
                humidity = currentHumidity,
                fanStatus = isFanOn,
                alertActive = currentTemp > 28.0,
                mode = if (isAutoMode) "AUTOMATICO" else "MANUAL",
                timestamp = dateFormat.format(Date())
            )

            socketServer?.broadcastTelemetry(telemetry)
            handler.postDelayed(this, 1500) // Telemetría periódica cada 1.5s
        }
    }

    fun startServer(port: Int = 8080) {
        if (socketServer?.isRunning == true) {
            log("Servidor ya está activo en puerto $port")
            return
        }

        socketServer = GreenhouseSocketServer(
            port = port,
            onLogMessage = { msg -> log(msg) },
            onClientStatusChange = { connected, ip ->
                onClientStatusListener?.invoke(connected, ip)
            },
            onCommandReceived = { cmd -> handleCommand(cmd) }
        )

        socketServer?.startServer()

        if (!isLoopRunning) {
            isLoopRunning = true
            handler.post(telemetryRunnable)
        }
    }

    fun stopServer() {
        isLoopRunning = false
        handler.removeCallbacks(telemetryRunnable)
        socketServer?.stopServer()
        socketServer = null
        log("Servidor detenido")
    }

    fun isServerRunning(): Boolean {
        return socketServer?.isRunning == true
    }

    fun handleCommand(cmd: String) {
        when {
            cmd == "CMD:FAN_ON" -> {
                isFanOn = true
                isAutoMode = false
                log("Comando Remoto: VENTILADOR ENCENDIDO")
            }
            cmd == "CMD:FAN_OFF" -> {
                isFanOn = false
                isAutoMode = false
                log("Comando Remoto: VENTILADOR APAGADO")
            }
            cmd == "CMD:MODE_AUTO" -> {
                isAutoMode = true
                log("Comando Remoto: MODO AUTOMÁTICO")
            }
            cmd == "CMD:MODE_MANUAL" -> {
                isAutoMode = false
                log("Comando Remoto: MODO MANUAL")
            }
            cmd == "CMD:TOGGLE_MODE" -> {
                isAutoMode = !isAutoMode
                log("Comando Remoto: MODO ${if (isAutoMode) "AUTOMÁTICO" else "MANUAL"}")
            }
        }
        onNodeStateChanged?.invoke()
    }

    private fun log(msg: String) {
        Log.d(TAG, msg)
        onLogListener?.invoke(msg)
    }
}
