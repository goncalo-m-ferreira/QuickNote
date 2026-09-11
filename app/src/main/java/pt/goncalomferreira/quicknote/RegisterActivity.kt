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

class RegisterActivity : AppCompatActivity() {

    private lateinit var editTextRegisterEmail: EditText
    private lateinit var editTextRegisterPassword: EditText
    private lateinit var editTextRegisterConfirmPassword: EditText
    private lateinit var buttonRegister: Button
    private lateinit var buttonBackToLogin: Button

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

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

        editTextRegisterEmail = findViewById(R.id.editTextRegisterEmail)
        editTextRegisterPassword = findViewById(R.id.editTextRegisterPassword)
        editTextRegisterConfirmPassword = findViewById(R.id.editTextRegisterConfirmPassword)
        buttonRegister = findViewById(R.id.buttonRegister)
        buttonBackToLogin = findViewById(R.id.buttonBackToLogin)

        // Regressa ao ecrã de Login
        buttonBackToLogin.setOnClickListener {
            finish()
        }

        // Validação local e registo real no clique do botão Criar conta
        buttonRegister.setOnClickListener {
            val email = editTextRegisterEmail.text.toString().trim()
            val password = editTextRegisterPassword.text.toString()
            val confirmPassword = editTextRegisterConfirmPassword.text.toString()

            editTextRegisterEmail.error = null
            editTextRegisterPassword.error = null
            editTextRegisterConfirmPassword.error = null

            var hasError = false

            if (email.isEmpty()) {
                editTextRegisterEmail.error = getString(R.string.error_email_required)
                hasError = true
            }

            if (password.isEmpty()) {
                editTextRegisterPassword.error = getString(R.string.error_password_required)
                hasError = true
            } else if (password.length < 6) {
                editTextRegisterPassword.error = getString(R.string.error_password_too_short)
                hasError = true
            }

            if (confirmPassword.isEmpty()) {
                editTextRegisterConfirmPassword.error = getString(R.string.error_confirm_password_required)
                hasError = true
            } else if (password.isNotEmpty() && password.length >= 6 && password != confirmPassword) {
                editTextRegisterConfirmPassword.error = getString(R.string.error_passwords_do_not_match)
                hasError = true
            }

            if (hasError) {
                return@setOnClickListener
            }

            // Desativa botões e altera texto enquanto o pedido está em curso
            setControlsEnabled(false)
            buttonRegister.text = getString(R.string.registering)

            lifecycleScope.launch {
                try {
                    val response = ApiClient.apiService.register(
                        AuthRequest(email = email, password = password)
                    )

                    if (response.isSuccessful) {
                        val authResponse = response.body()
                        val token = authResponse?.token

                        if (!token.isNullOrBlank()) {
                            sessionManager.saveToken(token)

                            val userEmail = authResponse.user?.email
                            if (!userEmail.isNullOrBlank()) {
                                sessionManager.saveUserEmail(userEmail)
                            }

                            val intent = Intent(this@RegisterActivity, MainActivity::class.java)
                            startActivity(intent)
                            finishAffinity()
                            return@launch
                        } else {
                            Toast.makeText(
                                this@RegisterActivity,
                                getString(R.string.error_unknown),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        val errorMsg = when (response.code()) {
                            400 -> getString(R.string.error_invalid_data)
                            409 -> getString(R.string.error_email_already_registered)
                            in 500..599 -> getString(R.string.error_server)
                            else -> getString(R.string.error_unknown)
                        }
                        Toast.makeText(this@RegisterActivity, errorMsg, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: IOException) {
                    Toast.makeText(
                        this@RegisterActivity,
                        getString(R.string.error_connection),
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (e: Exception) {
                    Toast.makeText(
                        this@RegisterActivity,
                        getString(R.string.error_unknown),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                // Restaura o estado dos botões após erro
                setControlsEnabled(true)
                buttonRegister.text = getString(R.string.register_title)
            }
        }
    }

    private fun setControlsEnabled(enabled: Boolean) {
        buttonRegister.isEnabled = enabled
        buttonBackToLogin.isEnabled = enabled
    }
}
