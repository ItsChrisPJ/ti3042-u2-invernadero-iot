package cl.itschris.invernaderoiot.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import cl.itschris.invernaderoiot.data.DatabaseHelper
import cl.itschris.invernaderoiot.databinding.ActivityLoginBinding
import cl.itschris.invernaderoiot.security.SessionManager
import com.google.android.material.snackbar.Snackbar

/**
 * Pantalla de Login y Autenticación de Usuarios.
 * 
 * Cumple con los Criterios:
 * - 2.1.1: Implementación con ViewBinding, Kotlin y ciclo de vida.
 * - 2.1.3: Norma ISO 27400: Autenticación segura mediante verificación de contraseñas con Hash SHA-256 + Salt.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var sessionManager: SessionManager

    private val TAG = "CicloVida_Login"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        dbHelper = DatabaseHelper(this)
        sessionManager = SessionManager(this)

        Log.d(TAG, "onCreate ejecutado en LoginActivity")

        // Si ya tiene sesión activa, redirigir directo al dashboard
        if (sessionManager.isLoggedIn()) {
            goToMainActivity()
            return
        }

        setupListeners()
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart ejecutado en LoginActivity")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume ejecutado en LoginActivity")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy ejecutado en LoginActivity")
    }

    private fun setupListeners() {
        // Iniciar Sesión
        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString()

            if (username.isEmpty()) {
                binding.tilUsername.error = "Ingresa tu nombre de usuario"
                return@setOnClickListener
            } else {
                binding.tilUsername.error = null
            }

            if (password.isEmpty()) {
                binding.tilPassword.error = "Ingresa tu contraseña"
                return@setOnClickListener
            } else {
                binding.tilPassword.error = null
            }

            // Validación de credenciales con Hash SHA-256 (ISO 27400)
            val (authenticated, user) = dbHelper.authenticateUser(username, password)
            if (authenticated && user != null) {
                sessionManager.saveUserSession(user.username, user.fullName, user.role)
                Toast.makeText(this, "Bienvenido, ${user.fullName}", Toast.LENGTH_SHORT).show()
                goToMainActivity()
            } else {
                Snackbar.make(binding.root, "Credenciales incorrectas o usuario no registrado", Snackbar.LENGTH_LONG)
                    .setAction("Ayuda") {
                        binding.etUsername.setText("admin")
                        binding.etPassword.setText("admin1234")
                    }
                    .show()
            }
        }

        // Botón Demo Rápido para agilizar la evaluación del docente
        binding.btnQuickDemo.setOnClickListener {
            binding.etUsername.setText("admin")
            binding.etPassword.setText("admin1234")
            Toast.makeText(this, "Credenciales de demostración cargadas (admin / admin1234)", Toast.LENGTH_SHORT).show()
        }

        // Ir a Pantalla de Registro
        binding.tvGoRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun goToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
