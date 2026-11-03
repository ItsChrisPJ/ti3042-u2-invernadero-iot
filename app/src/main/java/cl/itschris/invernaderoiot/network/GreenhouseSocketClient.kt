package cl.itschris.invernaderoiot.network

import android.util.Log
import cl.itschris.invernaderoiot.data.models.GreenhouseTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Cliente de Sockets Inalámbricos para el Dispositivo 2 (Panel de Control del Agricultor).
 * 
 * Tributa a los Criterios:
 * - 2.1.2: Conexión inalámbrica a dispositivo IoT remoto.
 * - 2.1.4: Demuestra interconexión y envío de comandos bidireccionales.
 */
class GreenhouseSocketClient(
    private val onConnectionStateChange: (Boolean, String) -> Unit,
    private val onTelemetryReceived: (GreenhouseTelemetry) -> Unit,
    private val onLogMessage: (String) -> Unit
) {

    private val TAG = "GreenhouseClient"
    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private var clientJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    @Volatile
    var isConnected = false
        private set

    fun connect(host: String, port: Int = 8080) {
        if (isConnected) return

        clientJob = scope.launch {
            try {
                onLogMessage("Conectando inalámbricamente a $host:$port...")
                val newSocket = Socket()
                newSocket.connect(InetSocketAddress(host, port), 5000)

                socket = newSocket
                writer = PrintWriter(newSocket.getOutputStream(), true)
                isConnected = true
                onConnectionStateChange(true, "Conectado a $host:$port")
                onLogMessage("¡Conexión establecida con Nodo Invernadero!")

                val reader = BufferedReader(InputStreamReader(newSocket.getInputStream()))
                while (isActive && isConnected && newSocket.isConnected && !newSocket.isClosed) {
                    val line = reader.readLine() ?: break
                    if (line.isNotBlank()) {
                        val telemetry = GreenhouseTelemetry.fromJson(line)
                        if (telemetry != null) {
                            onTelemetryReceived(telemetry)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error de conexión socket: ${e.message}")
                onLogMessage("Error de conexión: ${e.message}")
            } finally {
                disconnect()
            }
        }
    }

    fun sendCommand(command: String) {
        scope.launch {
            try {
                if (isConnected && writer != null) {
                    writer?.println(command)
                    Log.d(TAG, "Comando enviado: $command")
                    onLogMessage("Comando enviado: $command")
                } else {
                    onLogMessage("No se puede enviar comando: Sin conexión")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error enviando comando: ${e.message}")
                onLogMessage("Error enviando comando: ${e.message}")
            }
        }
    }

    fun disconnect() {
        if (!isConnected && socket == null) return
        isConnected = false
        try {
            writer?.close()
            socket?.close()
            socket = null
            writer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error cerrando socket cliente: ${e.message}")
        }
        clientJob?.cancel()
        onConnectionStateChange(false, "Desconectado")
        onLogMessage("Desconectado del Nodo Invernadero")
    }
}
