package pt.goncalomferreira.quicknote.network

import com.google.gson.annotations.SerializedName

data class NotesResponse(
    @SerializedName("notes") val notes: List<ApiNote>? = null
)
