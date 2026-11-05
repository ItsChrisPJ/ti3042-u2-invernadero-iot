package cl.itschris.invernaderoiot.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import cl.itschris.invernaderoiot.R
import cl.itschris.invernaderoiot.data.DatabaseHelper
import cl.itschris.invernaderoiot.data.models.GreenhouseTelemetry
import cl.itschris.invernaderoiot.data.models.SensorReading
import cl.itschris.invernaderoiot.databinding.ActivityGreenhouseMonitorBinding
import cl.itschris.invernaderoiot.network.GreenhouseServerManager
import cl.itschris.invernaderoiot.network.GreenhouseSocketClient
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dispositivo 2: Panel de Control del Agricultor (Cliente IoT).
 * 
 * Se conecta inalámbricamente al Dispositivo 1 (Nodo Invernadero):
 * - Monitorea en tiempo real Temperatura y Humedad del DHT11.
 * - Alerta visual de calor si la temperatura supera 28°C.
 * - Envía comandos remotos de control para activar o desactivar el ventilador.
 * - Permite persistir lecturas de telemetría en la base de datos local SQLite.
 */
class GreenhouseMonitorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGreenhouseMonitorBinding
    private var socketClient: GreenhouseSocketClient? = null
    private lateinit var dbHelper: DatabaseHelper

    private var latestTelemetry: GreenhouseTelemetry? = null
    private val TAG = "CicloVida_Monitor"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityGreenhouseMonitorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        dbHelper = DatabaseHelper(this)
        Log.d(TAG, "onCreate ejecutado en GreenhouseMonitorActivity")

        initClient()
        setupListeners()
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart ejecutado en GreenhouseMonitorActivity")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy ejecutado en GreenhouseMonitorActivity")
        socketClient?.disconnect()
    }

    private fun initClient() {
        socketClient = GreenhouseSocketClient(
            onConnectionStateChange = { connected, msg ->
                runOnUiThread {
                    binding.tvConnectionStatus.text = "Estado: $msg"
                    if (connected) {
                        binding.tvConnectionStatus.setTextColor(ContextCompat.getColor(this, R.color.hk_accent_greenpath))
                        binding.btnToggleConnection.text = "DESCONECTAR DEL INVERNADERO"
                        binding.btnToggleConnection.setBackgroundResource(R.drawable.bg_secondary_button)
                        Toast.makeText(this, "¡Conectado exitosamente!", Toast.LENGTH_SHORT).show()
                    } else {
                        binding.tvConnectionStatus.setTextColor(ContextCompat.getColor(this, R.color.hk_accent_grimm))
                        binding.btnToggleConnection.text = "FORJAR VÍNCULO CON INVERNADERO"
                        binding.btnToggleConnection.setBackgroundResource(R.drawable.bg_soul_button)
                    }
                }
            },
            onTelemetryReceived = { telemetry ->
                runOnUiThread {
                    latestTelemetry = telemetry
                    updateTelemetryUI(telemetry)
                }
            },
            onLogMessage = { logMsg ->
                Log.d(TAG, "Log Cliente: $logMsg")
            }
        )
    }

    private fun updateTelemetryUI(t: GreenhouseTelemetry) {
        binding.tvMonitorTemp.text = "${String.format("%.1f", t.temperature)} °C"
        binding.tvMonitorHumidity.text = "${String.format("%.1f", t.humidity)} %"
        binding.tvLastUpdate.text = "Última actualización: ${t.timestamp} (Modo: ${t.mode})"

        // Alerta visual de calor
        if (t.alertActive || t.temperature >= 28.0) {
            binding.tvAlertBanner.visibility = View.VISIBLE
            binding.tvMonitorTemp.setTextColor(ContextCompat.getColor(this, R.color.temp_hot))
        } else {
            binding.tvAlertBanner.visibility = View.GONE
            val tempColor = if (t.temperature <= 18.0) {
                ContextCompat.getColor(this, R.color.temp_cold)
            } else {
                ContextCompat.getColor(this, R.color.temp_optimal)
            }
            binding.tvMonitorTemp.setTextColor(tempColor)
        }

        // Estado del ventilador remoto
        if (t.fanStatus) {
            binding.tvMonitorFanStatus.text = "ACTIVO (ENCENDIDO)"
            binding.tvMonitorFanStatus.setTextColor(ContextCompat.getColor(this, R.color.hk_accent_lifeblood_glow))
        } else {
            binding.tvMonitorFanStatus.text = "INACTIVO (APAGADO)"
            binding.tvMonitorFanStatus.setTextColor(ContextCompat.getColor(this, R.color.hk_text_lore))
        }
        if (binding.switchMonitorFan.isChecked != t.fanStatus) {
            binding.switchMonitorFan.isChecked = t.fanStatus
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Loopback rápido
        binding.btnUseLoopback.setOnClickListener {
            binding.etIpAddress.setText("127.0.0.1")
            Toast.makeText(this, "IP configurada en 127.0.0.1 para pruebas locales", Toast.LENGTH_SHORT).show()
        }

        // Conectar / Desconectar
        binding.btnToggleConnection.setOnClickListener {
            if (socketClient?.isConnected == true) {
                socketClient?.disconnect("Desconectado por el usuario")
            } else {
                val rawInput = binding.etIpAddress.text.toString().trim()
                if (rawInput.isEmpty()) {
                    binding.tilIpAddress.error = "Ingresa la IP del Nodo Invernadero"
                    return@setOnClickListener
                } else {
                    binding.tilIpAddress.error = null
                }

                // Extraer host y puerto en caso de que el usuario haya escrito "IP:8080"
                var host = rawInput
                var port = 8080
                if (host.contains(":")) {
                    val parts = host.split(":")
                    host = parts[0].trim()
                    parts.getOrNull(1)?.trim()?.toIntOrNull()?.let { port = it }
                }

                // Autoinicio de servidor en loopback si está probando en el mismo equipo
                if ((host == "127.0.0.1" || host == "localhost") && !GreenhouseServerManager.isServerRunning()) {
                    GreenhouseServerManager.startServer(port)
                }

                socketClient?.connect(host, port)
            }
        }

        // Control remoto mediante Switch (Criterio 2.1.1.2)
        binding.switchMonitorFan.setOnCheckedChangeListener { _, isChecked ->
            if (socketClient?.isConnected == true) {
                val cmd = if (isChecked) "CMD:FAN_ON" else "CMD:FAN_OFF"
                socketClient?.sendCommand(cmd)
            }
        }

        // Control remoto: Encender Ventilador
        binding.btnTurnFanOn.setOnClickListener {
            if (socketClient?.isConnected == true) {
                socketClient?.sendCommand("CMD:FAN_ON")
                Toast.makeText(this, "Comando enviado: Encender Ventilador", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Primero conecta al Nodo Invernadero", Toast.LENGTH_SHORT).show()
            }
        }

        // Control remoto: Apagar Ventilador
        binding.btnTurnFanOff.setOnClickListener {
            if (socketClient?.isConnected == true) {
                socketClient?.sendCommand("CMD:FAN_OFF")
                Toast.makeText(this, "Comando enviado: Apagar Ventilador", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Primero conecta al Nodo Invernadero", Toast.LENGTH_SHORT).show()
            }
        }

        // Alternar Modo Auto / Manual
        binding.btnToggleAutoMode.setOnClickListener {
            if (socketClient?.isConnected == true) {
                socketClient?.sendCommand("CMD:TOGGLE_MODE")
                Toast.makeText(this, "Comando enviado: Alternar Modo", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Primero conecta al Nodo Invernadero", Toast.LENGTH_SHORT).show()
            }
        }

        // Guardar Lectura en SQLite (Criterio 2.1.2)
        binding.btnSaveReadingSqlite.setOnClickListener {
            val telemetry = latestTelemetry
            if (telemetry == null) {
                Toast.makeText(this, "Aún no se ha recibido telemetría para guardar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val reading = SensorReading(
                timestamp = dateFormat.format(Date()),
                temperature = telemetry.temperature,
                humidity = telemetry.humidity,
                fanStatus = telemetry.fanStatus,
                alertActive = telemetry.alertActive,
                mode = telemetry.mode
            )

            val id = dbHelper.insertReading(reading)
            if (id != -1L) {
                Toast.makeText(this, "Lectura guardada en SQLite (Registro #$id)", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Error al guardar en SQLite", Toast.LENGTH_SHORT).show()
            }
        }

        // Ver Historial SQLite
        binding.btnGoHistory.setOnClickListener {
            val intent = Intent(this, HistorialActivity::class.java)
            startActivity(intent)
        }
    }
}
