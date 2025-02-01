package com.dam.quicknote.autenticacao

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.dam.quicknote.PaginaInicial
import com.dam.quicknote.R
import com.dam.quicknote.storage.API
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class Login : AppCompatActivity() {

    // Variáveis de layout
    private lateinit var voltarBtn: ImageButton
    private lateinit var loginEmail: EditText
    private lateinit var loginPassword: EditText
    private lateinit var loginButton: Button
    private lateinit var mudarPagina: TextView
    private lateinit var api : API

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // inicia a variavel
        api = API()

        // inicializa sharedPreferences dos objetos
        UtilizadorManager.init(applicationContext)
        TokenManager.init(applicationContext)

        // ID's dos elementos
        voltarBtn = findViewById(R.id.voltar)
        loginEmail = findViewById(R.id.loginEmail)
        loginPassword = findViewById(R.id.loginPassword)
        loginButton = findViewById(R.id.loginButton)
        mudarPagina = findViewById(R.id.mudarPagina)

        // botão para voltar a atrás
        voltarBtn.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                val intent = Intent(this@Login, PaginaInicial::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                startActivity(intent)
                finish()
            }
        }

        // botão para fazer login na app
        loginButton.setOnClickListener {
            // obtem o email
            val email = loginEmail.text.toString().trim()
            // obtem a pasword
            val password = loginPassword.text.toString().trim()
            // faz o login com email e password fornecidos
            api.loginUtilizadorAPI(email, password, this@Login)

            // esconde o teclado
            val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(loginPassword.getWindowToken(), 0)
        }

        // muda para a pagina Login
        mudarPagina.setOnClickListener  {
            startActivity(Intent(this@Login, Registar::class.java))
            finish()
        }
    }
}