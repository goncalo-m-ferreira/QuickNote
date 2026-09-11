package pt.goncalomferreira.quicknote

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class RegisterActivity : AppCompatActivity() {

    private lateinit var editTextRegisterEmail: EditText
    private lateinit var editTextRegisterPassword: EditText
    private lateinit var editTextRegisterConfirmPassword: EditText
    private lateinit var buttonRegister: Button
    private lateinit var buttonBackToLogin: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

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

        // Validação local no clique do botão Criar conta
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

            // Não comunica com a API nesta fase
        }
    }
}
