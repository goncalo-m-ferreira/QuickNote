package pt.goncalomferreira.quicknote

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class LoginActivity : AppCompatActivity() {

    private lateinit var editTextLoginEmail: EditText
    private lateinit var editTextLoginPassword: EditText
    private lateinit var buttonLogin: Button
    private lateinit var buttonGoToRegister: Button
    private lateinit var buttonLoginAbout: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

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

        // Validação local no clique do botão Entrar
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

            // Não autentica nem navega para a MainActivity nesta fase
        }
    }
}
