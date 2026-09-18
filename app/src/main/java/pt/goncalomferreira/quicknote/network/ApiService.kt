package pt.goncalomferreira.quicknote.network

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

interface ApiService {

    @POST("auth/register")
    suspend fun register(
        @Body request: AuthRequest
    ): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(
        @Body request: AuthRequest
    ): Response<AuthResponse>

    @POST("auth/logout")
    suspend fun logout(
        @Header("Authorization") token: String
    ): Response<MessageResponse>

    @GET("users/me")
    suspend fun getCurrentUser(
        @Header("Authorization") token: String
    ): Response<UserResponse>

    @PATCH("users/me")
    suspend fun updateCurrentUser(
        @Header("Authorization") token: String,
        @Body request: ProfileUpdateRequest
    ): Response<UserResponse>

    @GET("notes")
    suspend fun getNotes(
        @Header("Authorization") token: String
    ): Response<NotesResponse>

    @POST("notes")
    suspend fun createNote(
        @Header("Authorization") token: String,
        @Body request: NoteRequest
    ): Response<NoteResponse>

    @PUT("notes/{id}")
    suspend fun updateNote(
        @Header("Authorization") token: String,
        @Path("id") id: Long,
        @Body request: NoteRequest
    ): Response<NoteResponse>

    @DELETE("notes/{id}")
    suspend fun deleteNote(
        @Header("Authorization") token: String,
        @Path("id") id: Long
    ): Response<MessageResponse>

    @Multipart
    @PUT("notes/{id}/photo")
    suspend fun uploadNotePhoto(
        @Header("Authorization") token: String,
        @Path("id") id: Long,
        @Part photo: MultipartBody.Part
    ): Response<MessageResponse>

    @GET("notes/{id}/photo")
    suspend fun getNotePhoto(
        @Header("Authorization") token: String,
        @Path("id") id: Long
    ): Response<ResponseBody>

    @DELETE("notes/{id}/photo")
    suspend fun deleteNotePhoto(
        @Header("Authorization") token: String,
        @Path("id") id: Long
    ): Response<MessageResponse>
}
