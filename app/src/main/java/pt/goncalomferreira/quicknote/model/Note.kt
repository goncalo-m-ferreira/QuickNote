package pt.goncalomferreira.quicknote.model

import androidx.room3.Entity
import androidx.room3.PrimaryKey

// Entidade Room que representa uma nota armazenada localmente.
@Entity(tableName = "notes")
data class Note(
    // O identificador e gerado automaticamente pelo Room.
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)