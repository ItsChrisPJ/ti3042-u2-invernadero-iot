package cl.itschris.invernaderoiot.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import cl.itschris.invernaderoiot.data.DatabaseHelper
import cl.itschris.invernaderoiot.databinding.ActivityHistorialBinding
import cl.itschris.invernaderoiot.ui.adapters.HistorialAdapter

/**
 * Pantalla de Historial de Mediciones Almacenadas en SQLite.
 * 
 * Cumple con el Criterio 2.1.2:
 * - Persistencia local y consulta de datos capturados por los sensores (DHT11 y ventilador).
 * - Funcionamiento 100% offline y sin necesidad de servicios en la nube externos.
 */
class HistorialActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistorialBinding
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var adapter: HistorialAdapter

    private val TAG = "CicloVida_Historial"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityHistorialBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        dbHelper = DatabaseHelper(this)
        Log.d(TAG, "onCreate ejecutado en HistorialActivity")

        setupRecyclerView()
        setupListeners()
        loadData()
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart ejecutado en HistorialActivity")
        loadData()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy ejecutado en HistorialActivity")
    }

    private fun setupRecyclerView() {
        adapter = HistorialAdapter(emptyList())
        binding.rvReadings.layoutManager = LinearLayoutManager(this)
        binding.rvReadings.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Cargar datos de prueba para demostración rápida en clase
        binding.btnPreloadSample.setOnClickListener {
            dbHelper.preloadDemoReadings()
            loadData()
            Toast.makeText(this, "Lecturas de prueba agregadas a SQLite", Toast.LENGTH_SHORT).show()
        }

        // Limpiar base de datos
        binding.btnClearHistory.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Limpiar Base de Datos")
                .setMessage("¿Deseas eliminar todo el historial de mediciones de SQLite?")
                .setPositiveButton("Eliminar") { _, _ ->
                    val deleted = dbHelper.clearAllReadings()
                    loadData()
                    Toast.makeText(this, "Se eliminaron $deleted registros", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun loadData() {
        val readings = dbHelper.getAllReadings()
        adapter.updateList(readings)

        if (readings.isEmpty()) {
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.tvTotalReadings.text = "0"
            binding.tvAvgTemp.text = "--.- °C"
            binding.tvAvgHum.text = "--.- %"
        } else {
            binding.tvEmptyState.visibility = View.GONE
            binding.tvTotalReadings.text = readings.size.toString()

            val avgTemp = readings.map { it.temperature }.average()
            val avgHum = readings.map { it.humidity }.average()

            binding.tvAvgTemp.text = "${String.format("%.1f", avgTemp)} °C"
            binding.tvAvgHum.text = "${String.format("%.1f", avgHum)} %"
        }
    }
}
