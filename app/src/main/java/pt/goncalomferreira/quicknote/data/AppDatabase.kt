package pt.goncalomferreira.quicknote.data

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import pt.goncalomferreira.quicknote.model.Note

// Base de dados Room da aplicacao.
@Database(
    entities = [Note::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao

    companion object {

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE `notes` ADD COLUMN `remoteId` INTEGER DEFAULT NULL")
                connection.execSQL("ALTER TABLE `notes` ADD COLUMN `ownerEmail` TEXT NOT NULL DEFAULT '${Note.LEGACY_OWNER}'")
                connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_notes_ownerEmail_remoteId` ON `notes` (`ownerEmail`, `remoteId`)")
            }
        }

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
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
