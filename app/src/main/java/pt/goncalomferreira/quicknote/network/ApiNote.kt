package pt.goncalomferreira.quicknote.network

import com.google.gson.annotations.SerializedName

data class ApiNote(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("user_id") val userId: Long? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("content") val content: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
)
