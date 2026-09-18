package pt.goncalomferreira.quicknote

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import pt.goncalomferreira.quicknote.auth.SessionManager
import pt.goncalomferreira.quicknote.network.ApiClient
import pt.goncalomferreira.quicknote.network.ProfileUpdateRequest

class AccountActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_account)

        // Ajusta o conteúdo da Activity às barras do sistema preservando as margens da aplicação.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val density = resources.displayMetrics.density
            val basePaddingHorizontal = (20 * density).toInt()
            val basePaddingVertical = (16 * density).toInt()

            v.setPadding(
                systemBars.left + basePaddingHorizontal,
                systemBars.top + basePaddingVertical,
                systemBars.right + basePaddingHorizontal,
                systemBars.bottom + basePaddingVertical
            )

            insets
        }

        // Botão voltar no cabeçalho
        val buttonBack = findViewById<View>(R.id.buttonBack)
        buttonBack?.setOnClickListener {
            finish()
        }

        val sessionManager = SessionManager(this)
        val authHeader = sessionManager.getAuthorizationHeader()
        if (authHeader.isNullOrBlank()) {
            finish()
            return
        }

        val editTextDisplayName = findViewById<EditText>(R.id.editTextDisplayName)
        val textViewEmail = findViewById<TextView>(R.id.textViewEmail)
        val buttonSave = findViewById<MaterialButton>(R.id.buttonSave)

        // Carregar inicialmente dados locais do SessionManager
        val localEmail = sessionManager.getUserEmail()
        val localDisplayName = sessionManager.getUserDisplayName()
        if (!localEmail.isNullOrBlank()) {
            textViewEmail.text = localEmail
        }
        if (!localDisplayName.isNullOrBlank()) {
            editTextDisplayName.setText(localDisplayName)
        }

        // Fazer GET /users/me para obter dados atualizados do servidor
        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.getCurrentUser(authHeader)
                if (response.isSuccessful) {
                    val user = response.body()?.user
                    if (user != null) {
                        val email = user.email.orEmpty()
                        val displayName = user.displayName.orEmpty()

                        if (email.isNotBlank()) {
                            sessionManager.saveUserEmail(email)
                            textViewEmail.text = email
                        }
                        sessionManager.saveUserDisplayName(displayName)
                        editTextDisplayName.setText(displayName)
                    }
                } else if (response.code() == 401 || response.code() == 403) {
                    finish()
                } else {
                    Toast.makeText(
                        this@AccountActivity,
                        R.string.error_fetch_account,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (_: Exception) {
                Toast.makeText(
                    this@AccountActivity,
                    R.string.error_fetch_account,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Botão Guardar alterações
        buttonSave.setOnClickListener {
            val input = editTextDisplayName.text?.toString().orEmpty()
            val trimmed = input.trim()

            if (trimmed.length < 2 || trimmed.length > 20) {
                editTextDisplayName.error = getString(R.string.error_display_name_length)
                return@setOnClickListener
            }

            editTextDisplayName.error = null
            buttonSave.isEnabled = false

            lifecycleScope.launch {
                try {
                    val response = ApiClient.apiService.updateCurrentUser(
                        authHeader,
                        ProfileUpdateRequest(trimmed)
                    )
                    if (response.isSuccessful) {
                        val updatedUser = response.body()?.user
                        val finalDisplayName = updatedUser?.displayName ?: trimmed
                        sessionManager.saveUserDisplayName(finalDisplayName)
                        editTextDisplayName.setText(finalDisplayName)
                        Toast.makeText(
                            this@AccountActivity,
                            R.string.profile_updated,
                            Toast.LENGTH_SHORT
                        ).show()
                    } else if (response.code() == 401 || response.code() == 403) {
                        finish()
                    } else {
                        Toast.makeText(
                            this@AccountActivity,
                            R.string.error_update_profile,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (_: Exception) {
                    Toast.makeText(
                        this@AccountActivity,
                        R.string.error_update_profile,
                        Toast.LENGTH_SHORT
                    ).show()
                } finally {
                    buttonSave.isEnabled = true
                }
            }
        }
    }
}
