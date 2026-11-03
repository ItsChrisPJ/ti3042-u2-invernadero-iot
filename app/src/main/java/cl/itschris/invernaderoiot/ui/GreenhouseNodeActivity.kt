package cl.itschris.invernaderoiot.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import cl.itschris.invernaderoiot.R
import cl.itschris.invernaderoiot.data.models.GreenhouseTelemetry
import cl.itschris.invernaderoiot.databinding.ActivityGreenhouseNodeBinding
import cl.itschris.invernaderoiot.network.GreenhouseSocketServer
import cl.itschris.invernaderoiot.network.NetworkUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dispositivo 1: Nodo Sensor Invernadero (Servidor IoT).
 * 
 * Simula el microcontrolador ubicado en el invernadero agrícola:
 * - Emula sensor DHT11 (Temperatura y Humedad).
 * - Controla el actuador de enfriamiento (Ventilador).
 * - Difunde la telemetría periódicamente por sockets Wi-Fi a los paneles de agricultores conectados.
 * - Recibe comandos de actuación remota desde el Dispositivo 2.
 */
class GreenhouseNodeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGreenhouseNodeBinding
    private var socketServer: GreenhouseSocketServer? = null

    private var currentTemp = 24.5
    private var currentHumidity = 62.0
    private var isFanOn = false
    private var isAutoMode = true

    private val handler = Handler(Looper.getMainLooper())
    private val broadcastIntervalMs = 2000L // Transmisión cada 2 segundos

    private val TAG = "CicloVida_Node"

    private val broadcastRunnable = object : Runnable {
        override fun run() {
            // Lógica de automatización: Si está en modo automático y la temperatura supera 28°C, activar ventilador
            if (isAutoMode) {
                if (currentTemp > 28.0 && !isFanOn) {
                    isFanOn = true
                    appendConsoleLog("Auto: Temperatura > 28°C -> Activando ventilador")
                } else if (currentTemp <= 25.0 && isFanOn) {
                    isFanOn = false
                    appendConsoleLog("Auto: Temperatura normalizada -> Apagando ventilador")
                }
            }

            updateUI()

            // Transmitir telemetría al agricultor conectado
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

            handler.postDelayed(this, broadcastIntervalMs)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityGreenhouseNodeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        Log.d(TAG, "onCreate ejecutado en GreenhouseNodeActivity")

        initServer()
        setupListeners()
        updateUI()
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart ejecutado en GreenhouseNodeActivity")
        handler.post(broadcastRunnable)
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop ejecutado en GreenhouseNodeActivity")
        handler.removeCallbacks(broadcastRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy ejecutado en GreenhouseNodeActivity")
        socketServer?.stopServer()
    }

    private fun initServer() {
        val localIp = NetworkUtils.getLocalIpAddress()
        binding.tvServerIpAddress.text = "$localIp:8080"

        socketServer = GreenhouseSocketServer(
            port = 8080,
            onLogMessage = { msg -> runOnUiThread { appendConsoleLog(msg) } },
            onClientStatusChange = { connected, ip ->
                runOnUiThread {
                    if (connected) {
                        binding.tvServerStatus.text = "Cliente conectado: $ip"
                        binding.tvServerStatus.setTextColor(ContextCompat.getColor(this, R.color.status_connected))
                    } else {
                        binding.tvServerStatus.text = "Servidor activo - Esperando cliente..."
                        binding.tvServerStatus.setTextColor(ContextCompat.getColor(this, R.color.greenhouse_primary_dark))
                    }
                }
            },
            onCommandReceived = { cmd -> runOnUiThread { handleRemoteCommand(cmd) } }
        )

        socketServer?.startServer()
        binding.tvServerStatus.text = "Servidor activo en $localIp:8080"
    }

    private fun handleRemoteCommand(cmd: String) {
        when {
            cmd == "CMD:FAN_ON" -> {
                isFanOn = true
                isAutoMode = false // Pasa a manual al recibir comando manual
                appendConsoleLog(">> Remoto: Forzar VENTILADOR ENCENDIDO")
                updateUI()
            }
            cmd == "CMD:FAN_OFF" -> {
                isFanOn = false
                isAutoMode = false
                appendConsoleLog(">> Remoto: Forzar VENTILADOR APAGADO")
                updateUI()
            }
            cmd == "CMD:MODE_AUTO" -> {
                isAutoMode = true
                appendConsoleLog(">> Remoto: Establecer modo AUTOMÁTICO")
                updateUI()
            }
            cmd == "CMD:MODE_MANUAL" -> {
                isAutoMode = false
                appendConsoleLog(">> Remoto: Establecer modo MANUAL")
                updateUI()
            }
            cmd == "CMD:TOGGLE_MODE" -> {
                isAutoMode = !isAutoMode
                appendConsoleLog(">> Remoto: Alternar modo a ${if (isAutoMode) "AUTOMÁTICO" else "MANUAL"}")
                updateUI()
            }
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnToggleServer.setOnClickListener {
            socketServer?.stopServer()
            initServer()
            appendConsoleLog("Servidor reiniciado manualmente")
        }

        // Control manual de temperatura
        binding.btnTempPlus.setOnClickListener {
            currentTemp = (currentTemp + 1.0).coerceAtMost(50.0)
            updateUI()
        }
        binding.btnTempMinus.setOnClickListener {
            currentTemp = (currentTemp - 1.0).coerceAtLeast(0.0)
            updateUI()
        }

        // Control manual de humedad
        binding.btnHumPlus.setOnClickListener {
            currentHumidity = (currentHumidity + 5.0).coerceAtMost(100.0)
            updateUI()
        }
        binding.btnHumMinus.setOnClickListener {
            currentHumidity = (currentHumidity - 5.0).coerceAtLeast(10.0)
            updateUI()
        }

        // Escenarios Rápidos
        binding.btnSimulateHeatAlert.setOnClickListener {
            currentTemp = 32.5
            currentHumidity = 45.0
            appendConsoleLog("Simulación: Onda de calor aplicada (32.5°C)")
            updateUI()
        }

        binding.btnSimulateOptimal.setOnClickListener {
            currentTemp = 22.0
            currentHumidity = 65.0
            appendConsoleLog("Simulación: Clima óptimo restaurado (22.0°C)")
            updateUI()
        }
    }

    private fun updateUI() {
        binding.tvNodeTemp.text = "${String.format("%.1f", currentTemp)} °C"
        binding.tvNodeHumidity.text = "${String.format("%.1f", currentHumidity)} %"

        // Color temperatura
        val tempColor = when {
            currentTemp >= 28.0 -> ContextCompat.getColor(this, R.color.temp_hot)
            currentTemp <= 18.0 -> ContextCompat.getColor(this, R.color.temp_cold)
            else -> ContextCompat.getColor(this, R.color.temp_optimal)
        }
        binding.tvNodeTemp.setTextColor(tempColor)

        // Estado del ventilador
        if (isFanOn) {
            binding.tvNodeFanStatus.text = "VENTILADOR: ENCENDIDO"
            binding.tvNodeFanStatus.setBackgroundColor(ContextCompat.getColor(this, R.color.fan_active))
        } else {
            binding.tvNodeFanStatus.text = "VENTILADOR: APAGADO"
            binding.tvNodeFanStatus.setBackgroundColor(ContextCompat.getColor(this, R.color.fan_inactive))
        }

        // Modo
        binding.tvNodeMode.text = if (isAutoMode) {
            "Modo: AUTOMÁTICO (Enciende ventilador si T > 28°C)"
        } else {
            "Modo: MANUAL (Control directo desde panel agricultor)"
        }
    }

    private fun appendConsoleLog(message: String) {
        val currentText = binding.tvNodeConsole.text.toString()
        val lines = currentText.lines().takeLast(15).toMutableList()
        lines.add("> $message")
        binding.tvNodeConsole.text = lines.joinToString("\n")
    }
}
