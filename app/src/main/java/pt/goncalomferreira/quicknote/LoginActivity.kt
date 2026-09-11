package pt.goncalomferreira.quicknote

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import pt.goncalomferreira.quicknote.auth.SessionManager
import pt.goncalomferreira.quicknote.network.ApiClient
import pt.goncalomferreira.quicknote.network.AuthRequest
import java.io.IOException

class LoginActivity : AppCompatActivity() {

    private lateinit var editTextLoginEmail: EditText
    private lateinit var editTextLoginPassword: EditText
    private lateinit var buttonLogin: Button
    private lateinit var buttonGoToRegister: Button
    private lateinit var buttonLoginAbout: Button

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        sessionManager = SessionManager(this)

        // Ajusta o conteúdo da Activity às barras do sistema.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }

        editTextLoginEmail = findViewById(R.id.editTextLoginEmail)
        editTextLoginPassword = findViewById(R.id.editTextLoginPassword)
        buttonLogin = findViewById(R.id.buttonLogin)
        buttonGoToRegister = findViewById(R.id.buttonGoToRegister)
        buttonLoginAbout = findViewById(R.id.buttonLoginAbout)

        // Navegação para o ecrã de registo
        buttonGoToRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        // Navegação para o ecrã Sobre
        buttonLoginAbout.setOnClickListener {
            val intent = Intent(this, AboutActivity::class.java)
            startActivity(intent)
        }

        // Validação local e autenticação real no clique do botão Entrar
        buttonLogin.setOnClickListener {
            val email = editTextLoginEmail.text.toString().trim()
            val password = editTextLoginPassword.text.toString()

            editTextLoginEmail.error = null
            editTextLoginPassword.error = null

            var hasError = false

            if (email.isEmpty()) {
                editTextLoginEmail.error = getString(R.string.error_email_required)
                hasError = true
            }

            if (password.isEmpty()) {
                editTextLoginPassword.error = getString(R.string.error_password_required)
                hasError = true
            }

            if (hasError) {
                return@setOnClickListener
            }

            // Desativa botões e altera texto enquanto o pedido está em curso
            setControlsEnabled(false)
            buttonLogin.text = getString(R.string.logging_in)

            lifecycleScope.launch {
                try {
                    val response = ApiClient.apiService.login(
                        AuthRequest(email = email, password = password)
                    )

                    if (response.isSuccessful) {
                        val authResponse = response.body()
                        val token = authResponse?.token

                        if (!token.isNullOrBlank()) {
                            sessionManager.saveToken(token)

                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                            return@launch
                        } else {
                            Toast.makeText(
                                this@LoginActivity,
                                getString(R.string.error_unknown),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        val errorMsg = when (response.code()) {
                            400 -> getString(R.string.error_invalid_data)
                            401 -> getString(R.string.error_invalid_credentials)
                            403 -> getString(R.string.error_access_denied)
                            in 500..599 -> getString(R.string.error_server)
                            else -> getString(R.string.error_unknown)
                        }
                        Toast.makeText(this@LoginActivity, errorMsg, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: IOException) {
                    Toast.makeText(
                        this@LoginActivity,
                        getString(R.string.error_connection),
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (e: Exception) {
                    Toast.makeText(
                        this@LoginActivity,
                        getString(R.string.error_unknown),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                // Restaura o estado dos botões após erro
                setControlsEnabled(true)
                buttonLogin.text = getString(R.string.login_button)
            }
        }
    }

    private fun setControlsEnabled(enabled: Boolean) {
        buttonLogin.isEnabled = enabled
        buttonGoToRegister.isEnabled = enabled
        buttonLoginAbout.isEnabled = enabled
    }
}
