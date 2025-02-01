package com.dam.quicknote.autenticacao

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dam.quicknote.PaginaInicial
import com.dam.quicknote.R
import com.dam.quicknote.storage.API
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class Registar : AppCompatActivity() {

    // Variáveis de layout
    private lateinit var registoEmail: EditText
    private lateinit var registoPassword: EditText
    private lateinit var registoButton: Button
    private lateinit var voltarBtn: ImageButton
    private lateinit var mudarParaLoginButton: TextView
    private lateinit var api : API
    val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\$"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registar)

        // inicia a variavel
        api = API()

        // inicializa sharedPreferences dos objetos
        UtilizadorManager.init(applicationContext)
        TokenManager.init(applicationContext)

        // ID's dos elementos
        voltarBtn = findViewById(R.id.voltar)
        registoEmail = findViewById(R.id.registoEmail)
        registoPassword = findViewById(R.id.registoPassword)
        registoButton = findViewById(R.id.registoButton)
        mudarParaLoginButton = findViewById(R.id.mudarParaLoginButton)

        // botão para voltar atrás
        voltarBtn.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                val intent = Intent(this@Registar, PaginaInicial::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                startActivity(intent)
                finish()
            }
        }

        // botão para fazer o registo na app
        registoButton.setOnClickListener{
            // obtem o email
            val email = registoEmail.text.toString().trim()
            // obtem a password
            val password = registoPassword.text.toString().trim()
            // confirmar se o email é válido
            if(!isValidEmail(email)){
                // aparece uma mensagem a dizer que o email é inválido
                Toast.makeText(this@Registar, "Email inválido", Toast.LENGTH_SHORT).show()
            } else {
                // faz o registo com o email e password fornecidos
                api.registarUtilizadorAPI(email, password, this@Registar)
            }
            // esconde o teclado
            val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(registoPassword.getWindowToken(), 0)
        }

        // muda para a página Registar
        mudarParaLoginButton.setOnClickListener {
            startActivity(Intent(this@Registar, Login::class.java))
            finish()
        }

    }

    // função que confirma se o email é válido
    fun isValidEmail(email: String): Boolean {
        return email.matches(emailRegex.toRegex())
    }
}