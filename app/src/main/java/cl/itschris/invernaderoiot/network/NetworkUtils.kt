package cl.itschris.invernaderoiot.network

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Utilidades para Conectividad Inalámbrica (Wi-Fi y Bluetooth).
 * Tributa al Criterio 2.1.2 y 2.1.4: Gestión de interfaces inalámbricas e interconexión móvil.
 */
object NetworkUtils {

    /**
     * Obtiene la dirección IPv4 local del dispositivo en la red Wi-Fi o Hotspot actual.
     * Esto permite al "Nodo Invernadero" mostrar su IP para que el "Monitor Agricultor" se conecte.
     */
    fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        val hostAddress = address.hostAddress
                        if (hostAddress != null && (hostAddress.startsWith("192.168.") || hostAddress.startsWith("10.") || hostAddress.startsWith("172."))) {
                            return hostAddress
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Fallback
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
