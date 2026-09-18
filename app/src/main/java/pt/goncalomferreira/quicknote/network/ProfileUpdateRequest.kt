package pt.goncalomferreira.quicknote.network

import com.google.gson.annotations.SerializedName

data class ProfileUpdateRequest(
    @SerializedName("displayName") val displayName: String
)
