package cl.itschris.invernaderoiot.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import cl.itschris.invernaderoiot.R
import cl.itschris.invernaderoiot.databinding.ActivityGreenhouseNodeBinding
import cl.itschris.invernaderoiot.network.GreenhouseServerManager
import cl.itschris.invernaderoiot.network.NetworkUtils

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
    private val TAG = "CicloVida_Node"

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

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume ejecutado en GreenhouseNodeActivity")

        // Conectar callbacks con el ServerManager
        GreenhouseServerManager.onNodeStateChanged = {
            runOnUiThread { updateUI() }
        }
        GreenhouseServerManager.onLogListener = { msg ->
            runOnUiThread { appendConsoleLog(msg) }
        }
        GreenhouseServerManager.onClientStatusListener = { connected, ip ->
            runOnUiThread {
                if (connected) {
                    binding.tvServerStatus.text = "Cliente conectado: $ip"
                    binding.tvServerStatus.setTextColor(ContextCompat.getColor(this, R.color.status_connected))
                } else {
                    binding.tvServerStatus.text = "Servidor activo - Esperando cliente..."
                    binding.tvServerStatus.setTextColor(ContextCompat.getColor(this, R.color.greenhouse_primary_dark))
                }
            }
        }
        updateUI()
    }

    override fun onPause() {
        super.onPause()
        GreenhouseServerManager.onNodeStateChanged = null
        GreenhouseServerManager.onLogListener = null
        GreenhouseServerManager.onClientStatusListener = null
    }

    private fun initServer() {
        val localIp = NetworkUtils.getLocalIpAddress()
        binding.tvServerIpAddress.text = "$localIp:8080"

        GreenhouseServerManager.startServer(8080)
        binding.tvServerStatus.text = "Servidor activo en $localIp:8080"
        binding.tvServerStatus.setTextColor(ContextCompat.getColor(this, R.color.status_connected))
        binding.btnToggleServer.text = "Detener / Reiniciar Servidor"
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnToggleServer.setOnClickListener {
            if (GreenhouseServerManager.isServerRunning()) {
                GreenhouseServerManager.stopServer()
                binding.tvServerStatus.text = "Servidor detenido"
                binding.tvServerStatus.setTextColor(ContextCompat.getColor(this, R.color.temp_hot))
                binding.btnToggleServer.text = "Iniciar Servidor"
                appendConsoleLog("Servidor detenido manualmente")
            } else {
                initServer()
                appendConsoleLog("Servidor iniciado manualmente")
            }
        }

        // Control manual de temperatura
        binding.btnTempPlus.setOnClickListener {
            GreenhouseServerManager.currentTemp = (GreenhouseServerManager.currentTemp + 1.0).coerceAtMost(50.0)
            updateUI()
        }
        binding.btnTempMinus.setOnClickListener {
            GreenhouseServerManager.currentTemp = (GreenhouseServerManager.currentTemp - 1.0).coerceAtLeast(0.0)
            updateUI()
        }

        // Control manual de humedad
        binding.btnHumPlus.setOnClickListener {
            GreenhouseServerManager.currentHumidity = (GreenhouseServerManager.currentHumidity + 5.0).coerceAtMost(100.0)
            updateUI()
        }
        binding.btnHumMinus.setOnClickListener {
            GreenhouseServerManager.currentHumidity = (GreenhouseServerManager.currentHumidity - 5.0).coerceAtLeast(10.0)
            updateUI()
        }

        // Escenarios Rápidos
        binding.btnSimulateHeatAlert.setOnClickListener {
            GreenhouseServerManager.currentTemp = 32.5
            GreenhouseServerManager.currentHumidity = 45.0
            appendConsoleLog("Simulación: Onda de calor aplicada (32.5°C)")
            updateUI()
        }

        binding.btnSimulateOptimal.setOnClickListener {
            GreenhouseServerManager.currentTemp = 22.0
            GreenhouseServerManager.currentHumidity = 65.0
            appendConsoleLog("Simulación: Clima óptimo restaurado (22.0°C)")
            updateUI()
        }

        // Slider de temperatura (Criterio 2.1.1.2)
        binding.sliderNodeTemp.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                GreenhouseServerManager.currentTemp = value.toDouble()
                updateUI()
            }
        }

        // Switch de ventilador (Criterio 2.1.1.2)
        binding.switchNodeFan.setOnCheckedChangeListener { _, isChecked ->
            GreenhouseServerManager.isFanOn = isChecked
            updateUI()
        }
    }

    private fun updateUI() {
        val temp = GreenhouseServerManager.currentTemp
        val hum = GreenhouseServerManager.currentHumidity
        val fan = GreenhouseServerManager.isFanOn
        val auto = GreenhouseServerManager.isAutoMode

        binding.tvNodeTemp.text = "${String.format("%.1f", temp)} °C"
        binding.tvNodeHumidity.text = "${String.format("%.1f", hum)} %"

        // Actualizar Slider y Switch sin disparar bucles
        val clamped = temp.coerceIn(15.0, 45.0).toFloat()
        if (Math.abs(binding.sliderNodeTemp.value - clamped) > 0.4f) {
            binding.sliderNodeTemp.value = clamped
        }
        if (binding.switchNodeFan.isChecked != fan) {
            binding.switchNodeFan.isChecked = fan
        }

        // Color temperatura
        val tempColor = when {
            temp >= 28.0 -> ContextCompat.getColor(this, R.color.temp_hot)
            temp <= 18.0 -> ContextCompat.getColor(this, R.color.temp_cold)
            else -> ContextCompat.getColor(this, R.color.temp_optimal)
        }
        binding.tvNodeTemp.setTextColor(tempColor)

        // Estado del ventilador
        if (fan) {
            binding.tvNodeFanStatus.text = "VENTILADOR: ENCENDIDO"
            binding.tvNodeFanStatus.setBackgroundColor(ContextCompat.getColor(this, R.color.fan_active))
        } else {
            binding.tvNodeFanStatus.text = "VENTILADOR: APAGADO"
            binding.tvNodeFanStatus.setBackgroundColor(ContextCompat.getColor(this, R.color.fan_inactive))
        }

        // Modo
        binding.tvNodeMode.text = if (auto) {
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
