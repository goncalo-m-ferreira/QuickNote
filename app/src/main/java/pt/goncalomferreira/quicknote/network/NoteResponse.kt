package pt.goncalomferreira.quicknote.network

import com.google.gson.annotations.SerializedName

data class NoteResponse(
    @SerializedName("message") val message: String? = null,
    @SerializedName("note") val note: ApiNote? = null
)
