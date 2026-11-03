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
import java.net.ServerSocket
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Servidor de Sockets Inalámbricos para el Dispositivo 1 (Nodo Sensor Invernadero).
 * 
 * Tributa a los Criterios:
 * - 2.1.2: Conexión inalámbrica por Sockets TCP en red local Wi-Fi / Hotspot.
 * - 2.1.4: Interconexión demostrable entre dos dispositivos móviles.
 */
class GreenhouseSocketServer(
    private val port: Int = 8080,
    private val onLogMessage: (String) -> Unit,
    private val onClientStatusChange: (Boolean, String) -> Unit,
    private val onCommandReceived: (String) -> Unit
) {

    private val TAG = "GreenhouseServer"
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    // Lista de clientes conectados concurrentemente
    private val connectedClients = mutableListOf<Socket>()

    @Volatile
    var isRunning = false
        private set

    fun startServer() {
        if (isRunning) return
        isRunning = true

        serverJob = scope.launch {
            try {
                serverSocket = ServerSocket(port)
                Log.d(TAG, "Servidor iniciado en puerto $port")
                onLogMessage("Servidor inalámbrico iniciado en puerto $port")

                while (isActive && isRunning) {
                    val clientSocket = serverSocket?.accept() ?: break
                    val clientIp = clientSocket.inetAddress.hostAddress ?: "Desconocido"
                    Log.d(TAG, "Cliente conectado desde: $clientIp")
                    onClientStatusChange(true, clientIp)
                    onLogMessage("Cliente conectado desde $clientIp")

                    synchronized(connectedClients) {
                        connectedClients.add(clientSocket)
                    }

                    // Lanzar corrutina para escuchar comandos del cliente conectado
                    launch {
                        handleClient(clientSocket, clientIp)
                    }
                }
            } catch (e: Exception) {
                if (isRunning) {
                    Log.e(TAG, "Error en el servidor: ${e.message}")
                    onLogMessage("Error del servidor: ${e.message}")
                }
            } finally {
                stopServer()
            }
        }
    }

    private fun handleClient(socket: Socket, clientIp: String) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            var line: String?
            while (isRunning && socket.isConnected && !socket.isClosed) {
                line = reader.readLine() ?: break
                val command = line.trim()
                if (command.isNotEmpty()) {
                    Log.d(TAG, "Comando recibido de $clientIp: $command")
                    onLogMessage("Comando recibido: $command")
                    onCommandReceived(command)
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Conexión con $clientIp finalizada: ${e.message}")
        } finally {
            synchronized(connectedClients) {
                connectedClients.remove(socket)
            }
            try {
                socket.close()
            } catch (_: Exception) {}
            onClientStatusChange(false, clientIp)
            onLogMessage("Cliente desconectado: $clientIp")
        }
    }

    /**
     * Difunde un paquete de telemetría a todos los clientes (Panel Agricultor) conectados.
     */
    fun broadcastTelemetry(telemetry: GreenhouseTelemetry) {
        val jsonPayload = telemetry.toJson()
        scope.launch {
            synchronized(connectedClients) {
                val iterator = connectedClients.iterator()
                while (iterator.hasNext()) {
                    val client = iterator.next()
                    try {
                        if (client.isConnected && !client.isClosed) {
                            val writer = PrintWriter(client.getOutputStream(), true)
                            writer.println(jsonPayload)
                        } else {
                            iterator.remove()
                        }
                    } catch (e: Exception) {
                        iterator.remove()
                    }
                }
            }
        }
    }

    fun stopServer() {
        isRunning = false
        try {
            synchronized(connectedClients) {
                connectedClients.forEach {
                    try { it.close() } catch (_: Exception) {}
                }
                connectedClients.clear()
            }
            serverSocket?.close()
            serverSocket = null
        } catch (e: Exception) {
            Log.e(TAG, "Error cerrando servidor: ${e.message}")
        }
        serverJob?.cancel()
        onLogMessage("Servidor inalámbrico detenido")
    }
}
