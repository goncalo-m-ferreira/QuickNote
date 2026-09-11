package pt.goncalomferreira.quicknote.network

import pt.goncalomferreira.quicknote.model.Note
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object ApiNoteMapper {

    fun toEntity(apiNote: ApiNote, ownerEmail: String, localId: Long = 0): Note? {
        val remoteId = apiNote.id ?: return null
        return Note(
            id = localId,
            title = apiNote.title ?: "",
            content = apiNote.content ?: "",
            createdAt = parseIsoDate(apiNote.createdAt),
            updatedAt = parseIsoDate(apiNote.updatedAt),
            remoteId = remoteId,
            ownerEmail = ownerEmail
        )
    }

    fun parseIsoDate(dateStr: String?): Long {
        if (dateStr.isNullOrBlank()) return System.currentTimeMillis()

        val patterns = arrayOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ssX",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd HH:mm:ss"
        )

        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val date = sdf.parse(dateStr)
                if (date != null) {
                    return date.time
                }
            } catch (_: Exception) {
                // Tenta o proximo padrao
            }
        }
        return System.currentTimeMillis()
    }
}
