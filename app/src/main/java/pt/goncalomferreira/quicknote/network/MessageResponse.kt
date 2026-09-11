package pt.goncalomferreira.quicknote.network

import com.google.gson.annotations.SerializedName

data class MessageResponse(
    @SerializedName("message") val message: String? = null,
    @SerializedName("error") val error: String? = null
)
