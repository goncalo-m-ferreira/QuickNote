package pt.goncalomferreira.quicknote

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import pt.goncalomferreira.quicknote.data.AppDatabase
import pt.goncalomferreira.quicknote.model.Note

class NoteEditActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_note_edit)

        // Ajusta o conteudo da Activity as barras do sistema.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }

        // Referencias aos campos utilizados para criar uma nota.
        val editTextTitulo = findViewById<EditText>(R.id.editTextTitulo)
        val editTextConteudo = findViewById<EditText>(R.id.editTextConteudo)
        val buttonGuardar = findViewById<Button>(R.id.buttonGuardar)

        // Obtem o DAO atraves da instancia unica da base de dados Room.
        val noteDao = AppDatabase
            .getDatabase(applicationContext)
            .noteDao()

        val noteId = intent.getLongExtra("NOTE_ID", -1L)
        var existingNote: Note? = null

        // Verifica se o editor foi aberto para criar ou editar uma nota.
        if (noteId != -1L) {
            buttonGuardar.isEnabled = false

            lifecycleScope.launch {
                existingNote = noteDao.getById(noteId)

                val note = existingNote

                if (note == null) {
                    Toast.makeText(
                        this@NoteEditActivity,
                        "Nota não encontrada.",
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()
                    return@launch
                }

                editTextTitulo.setText(note.title)
                editTextConteudo.setText(note.content)

                findViewById<TextView>(R.id.textViewEditorTitulo).text = "Editar nota"

                buttonGuardar.isEnabled = true
            }
        }

        buttonGuardar.setOnClickListener {

            val titulo = editTextTitulo.text.toString().trim()
            val conteudo = editTextConteudo.text.toString().trim()

            // Valida os dados antes de permitir a gravacao da nota.
            if (titulo.isEmpty()) {
                editTextTitulo.error = "Introduza um título."
                return@setOnClickListener
            }

            if (conteudo.isEmpty()) {
                editTextConteudo.error = "Introduza o conteúdo da nota."
                return@setOnClickListener
            }

            // Atualiza a nota existente ou cria uma nova nota.
            lifecycleScope.launch {
                val note = existingNote

                if (note == null) {
                    noteDao.insert(
                        Note(
                            title = titulo,
                            content = conteudo
                        )
                    )
                } else {
                    noteDao.update(
                        note.copy(
                            title = titulo,
                            content = conteudo,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }

                Toast.makeText(
                    this@NoteEditActivity,
                    "Nota guardada.",
                    Toast.LENGTH_SHORT
                ).show()

                finish()
            }
        }
    }
}