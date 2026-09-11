package pt.goncalomferreira.quicknote.network

import com.google.gson.annotations.SerializedName

data class UserResponse(
    @SerializedName("user") val user: ApiUser
)
