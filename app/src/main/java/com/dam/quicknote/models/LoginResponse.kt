package com.dam.quicknote.models

data class LoginResponse(
    val id: String,
    val data: String,
    val email: String,
    val token: String,
)
