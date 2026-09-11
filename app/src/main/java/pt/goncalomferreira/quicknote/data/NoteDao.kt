package pt.goncalomferreira.quicknote.data

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import pt.goncalomferreira.quicknote.model.Note

// Define as operacoes de acesso a tabela de notas.
@Dao
interface NoteDao {

    // Insere uma nova nota e devolve o identificador gerado.
    @Insert
    suspend fun insert(note: Note): Long

    // Atualiza uma nota existente na base de dados.
    @Update
    suspend fun update(note: Note)

    // Obtem todas as notas, mostrando primeiro as alteradas mais recentemente.
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    suspend fun getAll(): List<Note>

    // Obtem uma nota especifica atraves do seu identificador.
    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Note?
}