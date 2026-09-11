package pt.goncalomferreira.quicknote.data

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
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

    // Elimina uma nota existente da base de dados.
    @Delete
    suspend fun delete(note: Note)

    // Obtem todas as notas, mostrando primeiro as alteradas mais recentemente.
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    suspend fun getAll(): List<Note>

    // Obtem uma nota especifica atraves do seu identificador.
    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Note?

    // Obtem todas as notas de um determinado utilizador (ownerEmail), ordenadas pela mais recente.
    @Query("SELECT * FROM notes WHERE ownerEmail = :ownerEmail ORDER BY updatedAt DESC")
    suspend fun getByOwnerEmail(ownerEmail: String): List<Note>

    // Obtem uma nota especifica de um utilizador atraves do id remoto da API.
    @Query("SELECT * FROM notes WHERE ownerEmail = :ownerEmail AND remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(ownerEmail: String, remoteId: Long): Note?

    // Elimina todas as notas de cache de um determinado utilizador.
    @Query("DELETE FROM notes WHERE ownerEmail = :ownerEmail")
    suspend fun deleteByOwnerEmail(ownerEmail: String)

    // Insere ou substitui varias notas no cache.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notes: List<Note>)
}
