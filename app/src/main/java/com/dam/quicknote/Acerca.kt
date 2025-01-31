package com.dam.quicknote

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.dam.quicknote.listaNotas.ListaNotas
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar


class Acerca : AppCompatActivity() {

    // Criação das variaveis
    private lateinit var gitpagina : TextView
    private lateinit var voltarBtn: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_acerca)

        // Inicialização das variaveis
        voltarBtn = findViewById(R.id.voltar)
        gitpagina = findViewById(R.id.AcercaGit)

        // Evento para abrir o link do github do projeto
        gitpagina.setOnClickListener {
            val url = "https://github.com/goncalo-m-ferreira/QuickNote"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)
        }

        // Evento para voltar para a lista de notas
        voltarBtn.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                startActivity(Intent(this@Acerca, ListaNotas::class.java))
                finish()
            }
        }

        // Chama a função para definir o ano atual
        Copyright()
    }

    private fun Copyright() {
        val copyright = findViewById<TextView>(R.id.AcercaCopyright)
        @SuppressLint("DefaultLocale") val copyrightString =
            String.format("Copyright %d", Calendar.getInstance().get(Calendar.YEAR))
        copyright.setText(copyrightString)
        copyright.setGravity(Gravity.CENTER)
    }
}