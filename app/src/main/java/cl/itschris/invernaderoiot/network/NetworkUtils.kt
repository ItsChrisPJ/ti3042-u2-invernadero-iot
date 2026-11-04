package cl.itschris.invernaderoiot.network

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

/**
 * Utilidades para Conectividad Inalámbrica (Wi-Fi y Bluetooth).
 * Tributa al Criterio 2.1.2 y 2.1.4: Gestión de interfaces inalámbricas e interconexión móvil.
 */
object NetworkUtils {

    private const val TAG = "NetworkUtils"

    /**
     * Obtiene la dirección IPv4 local del dispositivo en la red Wi-Fi o Hotspot actual.
     * Prioriza estrictamente las interfaces de red local (Wi-Fi 'wlan', Hotspot 'ap', Ethernet 'eth')
     * por sobre interfaces de datos celulares ('ccmni', 'rmnet') que usan IPs de operadora no accesibles localmente.
     */
    fun getLocalIpAddress(): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())

            // Prioridad 1: Interfaces Wi-Fi (wlan) o Hotspot/Zona Portátil (ap, softap, rndis, eth)
            for (iface in interfaces) {
                val name = iface.name.lowercase()
                if (name.startsWith("wlan") || name.startsWith("ap") || name.startsWith("softap") || name.startsWith("eth") || name.startsWith("rndis")) {
                    for (address in Collections.list(iface.inetAddresses)) {
                        if (!address.isLoopbackAddress && address is Inet4Address) {
                            val ip = address.hostAddress
                            if (!ip.isNullOrBlank()) {
                                Log.d(TAG, "IP Wi-Fi/Hotspot detectada en ${iface.name}: $ip")
                                return ip
                            }
                        }
                    }
                }
            }

            // Prioridad 2: Cualquier interfaz con rango de red de área local típico (192.168.x.x)
            for (iface in interfaces) {
                val name = iface.name.lowercase()
                if (!name.startsWith("rmnet") && !name.startsWith("ccmni") && !name.startsWith("dummy")) {
                    for (address in Collections.list(iface.inetAddresses)) {
                        if (!address.isLoopbackAddress && address is Inet4Address) {
                            val ip = address.hostAddress
                            if (!ip.isNullOrBlank() && ip.startsWith("192.168.")) {
                                Log.d(TAG, "IP 192.168.x detectada en ${iface.name}: $ip")
                                return ip
                            }
                        }
                    }
                }
            }

            // Prioridad 3: Cualquier otra interfaz que no sea datos celulares de operadora
            for (iface in interfaces) {
                val name = iface.name.lowercase()
                if (!name.startsWith("rmnet") && !name.startsWith("ccmni") && !name.startsWith("dummy")) {
                    for (address in Collections.list(iface.inetAddresses)) {
                        if (!address.isLoopbackAddress && address is Inet4Address) {
                            val ip = address.hostAddress
                            if (!ip.isNullOrBlank()) {
                                Log.d(TAG, "IP local alternativa en ${iface.name}: $ip")
                                return ip
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo IP local: ${e.message}")
        }
        return "127.0.0.1"
    }

    /**
     * Obtiene el nombre (SSID) de la red Wi-Fi a la que está conectado el dispositivo.
     */
    @Suppress("DEPRECATION")
    fun getWifiSSID(context: Context): String {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val info = wifiManager?.connectionInfo
            val ssid = info?.ssid ?: "No disponible"
            if (ssid == "<unknown ssid>" || ssid == "\"<unknown ssid>\"") {
                "Wi-Fi Conectado (Activar Ubicación para ver SSID)"
            } else {
                ssid.replace("\"", "")
            }
        } catch (e: Exception) {
            "Desconocido / Sin conexión Wi-Fi"
        }
    }

    /**
     * Obtiene la lista de nombres y direcciones MAC de dispositivos Bluetooth vinculados.
     */
    @SuppressLint("MissingPermission")
    fun getPairedBluetoothDevices(): List<String> {
        val list = mutableListOf<String>()
        val adapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        if (adapter != null && adapter.isEnabled) {
            val bonded = adapter.bondedDevices
            if (!bonded.isNullOrEmpty()) {
                for (device in bonded) {
                    list.add("${device.name ?: "Desconocido"} (${device.address})")
                }
            }
        }
        return list
    }

    /**
     * Verifica si el adaptador Bluetooth está disponible y habilitado.
     */
    fun isBluetoothAvailable(): Boolean {
        val adapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        return adapter != null && adapter.isEnabled
    }
}
