package cl.itschris.invernaderoiot.ui

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import cl.itschris.invernaderoiot.databinding.ActivityMainBinding
import cl.itschris.invernaderoiot.network.NetworkUtils
import cl.itschris.invernaderoiot.security.SessionManager
import com.google.android.material.snackbar.Snackbar

/**
 * Actividad Principal (Dashboard de Control y Navegación).
 * 
 * Integra los contenidos de las 3 Clases:
 * - Clase 01: Estructura del proyecto, ciclo de vida, principios de seguridad ISO 27400.
 * - Clase 02: Views, ViewBinding, navegación entre Activities con Intents y paso de parámetros.
 * - Clase 03: Permisos inalámbricos en tiempo de ejecución, consulta de Wi-Fi y Bluetooth.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sessionManager: SessionManager

    companion object {
        const val EXTRA_TEXTO = "EXTRA_TEXTO"
        private const val REQUEST_CODE_PERMISSIONS = 101
        private val TAG = "CicloVida_Main"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inicialización de ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sessionManager = SessionManager(this)

        // Verificación de sesión: si no ha iniciado sesión, volver a Login
        if (!sessionManager.isLoggedIn()) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        Log.d(TAG, "onCreate ejecutado en MainActivity")

        updateSessionUI()
        updateNetworkInfo()
        setupListeners()
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart ejecutado en MainActivity")
        updateNetworkInfo()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume ejecutado en MainActivity")
        updateNetworkInfo()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy ejecutado en MainActivity")
    }

    private fun updateSessionUI() {
        binding.tvWelcomeUser.text = "Hola, ${sessionManager.getCurrentFullName()}"
        binding.tvUserRole.text = "Rol: ${sessionManager.getCurrentRole()} (@${sessionManager.getCurrentUsername()})"
    }

    private fun updateNetworkInfo() {
        val wifiSsid = NetworkUtils.getWifiSSID(this)
        val localIp = NetworkUtils.getLocalIpAddress()

        binding.tvWifiStatus.text = "Wi-Fi: $wifiSsid"
        binding.tvLocalIp.text = "IP Local Asignada: $localIp"
    }

    private fun setupListeners() {
        // Cerrar Sesión
        binding.btnLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Cerrar Sesión")
                .setMessage("¿Estás seguro de que deseas cerrar tu sesión actual?")
                .setPositiveButton("Sí, Salir") { _, _ ->
                    sessionManager.logout()
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        // ==========================================
        // DEMO INTERCONEXIÓN 2 DISPOSITIVOS (CRITERIO 2.1.4)
        // ==========================================

        // Dispositivo 1: Nodo Invernadero (Servidor Sockets Wi-Fi)
        binding.btnOpenGreenhouseNode.setOnClickListener {
            val intent = Intent(this, GreenhouseNodeActivity::class.java)
            startActivity(intent)
        }

        // Dispositivo 2: Panel Agricultor (Cliente Sockets Wi-Fi)
        binding.btnOpenGreenhouseMonitor.setOnClickListener {
            val intent = Intent(this, GreenhouseMonitorActivity::class.java)
            startActivity(intent)
        }

        // Almacenamiento SQLite (Criterio 2.1.2)
        binding.btnOpenHistory.setOnClickListener {
            val intent = Intent(this, HistorialActivity::class.java)
            startActivity(intent)
        }

        // ==========================================
        // FUNCIONALIDADES ORIGINALES DE LA CLASE (CLASE 02 Y 03)
        // ==========================================

        // Prueba de paso de datos mediante Intent (Clase 02)
        binding.btnEnviar.setOnClickListener {
            val texto = binding.inputTexto.text.toString().ifBlank { "Telemetría Invernadero OK" }
            val intent = Intent(this, DetalleActivity::class.java).apply {
                putExtra(EXTRA_TEXTO, texto)
            }
            startActivity(intent)
        }

        // Solicitud de Permisos en Contexto (ISO 27400 - Mínimo Privilegio)
        binding.btnSolicitarPermiso.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("¿Por qué solicitamos estos permisos?")
                .setMessage("Para interconectar este teléfono con el Nodo Invernadero u otros dispositivos Android cercanos, requerimos permisos inalámbricos y de ubicación.")
                .setPositiveButton("Continuar") { _, _ ->
                    solicitarPermisosInalambricos()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        // Refrescar Wi-Fi (Clase 03)
        binding.btnNombreWifi.setOnClickListener {
            mostrarNombreWifi()
        }

        // Listar Dispositivos Bluetooth (Clase 03)
        binding.btnListarBluetooth.setOnClickListener {
            listarDispositivosBluetooth()
        }
    }

    private fun solicitarPermisosInalambricos() {
        val permissions = mutableListOf<String>()

        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
        }

        ActivityCompat.requestPermissions(
            this,
            permissions.toTypedArray(),
            REQUEST_CODE_PERMISSIONS
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            val anyGranted = grantResults.isNotEmpty() && grantResults.any { it == PackageManager.PERMISSION_GRANTED }
            if (anyGranted) {
                Toast.makeText(this, "Permisos inalámbricos concedidos correctamente", Toast.LENGTH_SHORT).show()
                updateNetworkInfo()
            } else {
                Snackbar.make(binding.root, "Permisos denegados. Algunas funciones inalámbricas estarán limitadas.", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun listarDispositivosBluetooth() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                solicitarPermisosInalambricos()
                Toast.makeText(this, "Se requiere permiso Bluetooth Connect en Android 12+", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val adapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        if (adapter == null) {
            Toast.makeText(this, "Este dispositivo no soporta Bluetooth", Toast.LENGTH_SHORT).show()
            return
        }

        val bonded = adapter.bondedDevices
        if (bonded.isNullOrEmpty()) {
            Toast.makeText(this, "No hay dispositivos Bluetooth vinculados", Toast.LENGTH_SHORT).show()
            Log.d("Bluetooth", "No hay dispositivos vinculados o Bluetooth desactivado")
        } else {
            val deviceNames = bonded.map { "${it.name ?: "Desconocido"} (${it.address})" }
            AlertDialog.Builder(this)
                .setTitle("Dispositivos Bluetooth Vinculados (${deviceNames.size})")
                .setItems(deviceNames.toTypedArray(), null)
                .setPositiveButton("Cerrar", null)
                .show()

            bonded.forEach { dev ->
                Log.d("Bluetooth", "Nombre: ${dev.name}, Dirección: ${dev.address}")
            }
        }
    }

    private fun mostrarNombreWifi() {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        @Suppress("DEPRECATION")
        val info = wifiManager.connectionInfo
        val nombreWifi = info?.ssid ?: "No disponible"

        Log.d("WiFi", "Conectado a: $nombreWifi")
        Toast.makeText(this, "Wi-Fi: $nombreWifi", Toast.LENGTH_SHORT).show()
        updateNetworkInfo()
    }
}
