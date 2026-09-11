package pt.goncalomferreira.quicknote.network

import com.google.gson.annotations.SerializedName

data class AuthResponse(
    @SerializedName("message") val message: String? = null,
    @SerializedName("user") val user: ApiUser? = null,
    @SerializedName("token") val token: String? = null
)
