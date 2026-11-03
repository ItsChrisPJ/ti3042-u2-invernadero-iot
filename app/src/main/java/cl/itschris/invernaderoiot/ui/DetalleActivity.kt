package cl.itschris.invernaderoiot.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import cl.itschris.invernaderoiot.databinding.ActivityDetalleBinding

/**
 * Actividad Secundaria para prueba de navegación e Intents (Clase 02).
 */
class DetalleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetalleBinding
    private val TAG = "CicloVida_Detalle"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityDetalleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        Log.d(TAG, "onCreate ejecutado en DetalleActivity")

        // Paso de datos mediante Intent (Clase 02)
        val recibido = intent.getStringExtra(MainActivity.EXTRA_TEXTO).orEmpty()
        binding.txtDetalle.text = if (recibido.isNotEmpty()) {
            "Dato recibido exitosamente:\n\n\"$recibido\""
        } else {
            "No se recibió ningún parámetro en el Intent"
        }

        binding.btnVolver.setOnClickListener {
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart ejecutado en DetalleActivity")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy ejecutado en DetalleActivity")
    }
}
