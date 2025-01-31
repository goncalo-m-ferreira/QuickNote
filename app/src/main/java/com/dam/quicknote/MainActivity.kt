package com.dam.quicknote

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.dam.quicknote.autenticacao.Login
import com.dam.quicknote.autenticacao.Registar
import com.dam.quicknote.listaNotas.ListaNotas

class PaginaInicial : AppCompatActivity() {

    // Criação das variaveis
    private lateinit var btnEntrar : Button
    private lateinit var btnRegistar : Button
    private lateinit var convidado : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pagina_inicial)

        // Inicialização das variaveis
        btnRegistar = findViewById(R.id.registo)
        btnEntrar = findViewById(R.id.entrar)
        convidado = findViewById(R.id.convidado)

        // Evento para abrir a pagina de login
        btnEntrar.setOnClickListener {
            startActivity(Intent(this@PaginaInicial, Login::class.java))
        }

        // Evento para abrir a pagina de registo
        btnRegistar.setOnClickListener {
            startActivity(Intent(this@PaginaInicial, Registar::class.java))
        }

        // Evento para abrir a pagina de lista de notas
        convidado.setOnClickListener{
            startActivity(Intent(this@PaginaInicial, ListaNotas::class.java))
        }

    }
}