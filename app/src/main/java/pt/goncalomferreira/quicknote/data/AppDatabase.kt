package pt.goncalomferreira.quicknote.data

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import pt.goncalomferreira.quicknote.model.Note

// Base de dados Room da aplicacao. Atualmente contem apenas a entidade Note.
@Database(
    entities = [Note::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao

    companion object {

        // Mantem uma unica instancia da base de dados durante a execucao da aplicacao.
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Cria a base de dados apenas na primeira utilizacao e reutiliza-a nas seguintes.
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "quicknote_database"
                ).build()

                INSTANCE = instance
                instance
            }
        }
    }
}