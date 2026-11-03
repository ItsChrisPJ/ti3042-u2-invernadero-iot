package cl.itschris.invernaderoiot.ui

import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import cl.itschris.invernaderoiot.data.DatabaseHelper
import cl.itschris.invernaderoiot.databinding.ActivityRegisterBinding
import com.google.android.material.snackbar.Snackbar

/**
 * Pantalla de Registro de Nuevos Operadores.
 * 
 * Cumple con ISO 27400: Registro de identidades con cifrado de credenciales.
 */
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var dbHelper: DatabaseHelper
    private val TAG = "CicloVida_Register"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        dbHelper = DatabaseHelper(this)
        Log.d(TAG, "onCreate ejecutado en RegisterActivity")

        setupRoleSpinner()
        setupListeners()
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart ejecutado en RegisterActivity")
    }

    private fun setupRoleSpinner() {
        val roles = arrayOf("Operador Agrícola", "Supervisor de Invernadero", "Técnico de Campo")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, roles)
        binding.spnRole.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnRegister.setOnClickListener {
            val fullName = binding.etFullName.text.toString().trim()
            val username = binding.etUsername.text.toString().trim().lowercase()
            val role = binding.spnRole.selectedItem.toString()
            val password = binding.etPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()

            if (fullName.isEmpty()) {
                binding.tilFullName.error = "Ingresa tu nombre completo"
                return@setOnClickListener
            } else {
                binding.tilFullName.error = null
            }

            if (username.length < 3) {
                binding.tilUsername.error = "El usuario debe tener al menos 3 caracteres"
                return@setOnClickListener
            } else {
                binding.tilUsername.error = null
            }

            if (password.length < 4) {
                binding.tilPassword.error = "La contraseña debe tener al menos 4 caracteres"
                return@setOnClickListener
            } else {
                binding.tilPassword.error = null
            }

            if (password != confirmPassword) {
                binding.tilConfirmPassword.error = "Las contraseñas no coinciden"
                return@setOnClickListener
            } else {
                binding.tilConfirmPassword.error = null
            }

            // Registro en SQLite con Hash SHA-256 (ISO 27400)
            val success = dbHelper.registerUser(username, password, fullName, role)
            if (success) {
                Toast.makeText(this, "¡Usuario registrado con éxito!", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Snackbar.make(binding.root, "Error: El usuario ya existe o no se pudo registrar", Snackbar.LENGTH_LONG).show()
            }
        }

        binding.btnBackToLogin.setOnClickListener {
            finish()
        }
    }
}
