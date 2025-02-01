package com.dam.quicknote.models

import com.google.gson.annotations.SerializedName

data class NotaWrapper(
    @SerializedName("nota") val nota: Nota
)
