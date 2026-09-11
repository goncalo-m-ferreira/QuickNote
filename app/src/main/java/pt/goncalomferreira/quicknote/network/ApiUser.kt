package pt.goncalomferreira.quicknote.network

import com.google.gson.annotations.SerializedName

data class ApiUser(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null
)
