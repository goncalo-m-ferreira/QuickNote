package pt.goncalomferreira.quicknote

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
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
        val buttonEliminar = findViewById<Button>(R.id.buttonEliminar)

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
                        getString(R.string.note_not_found),
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()
                    return@launch
                }

                editTextTitulo.setText(note.title)
                editTextConteudo.setText(note.content)

                findViewById<TextView>(R.id.textViewEditorTitulo).text =
                    getString(R.string.editor_edit_note_title)
                buttonEliminar.visibility = View.VISIBLE

                buttonGuardar.isEnabled = true
            }
        }

        // Pede confirmacao antes de eliminar definitivamente a nota.
        buttonEliminar.setOnClickListener {
            val note = existingNote ?: return@setOnClickListener

            AlertDialog.Builder(this)
                .setTitle(R.string.delete_note)
                .setMessage(R.string.delete_note_confirmation)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete_note) { _, _ ->
                    lifecycleScope.launch {
                        noteDao.delete(note)

                        Toast.makeText(
                            this@NoteEditActivity,
                            getString(R.string.note_deleted),
                            Toast.LENGTH_SHORT
                        ).show()

                        finish()
                    }
                }
                .show()
        }

        buttonGuardar.setOnClickListener {

            val titulo = editTextTitulo.text.toString().trim()
            val conteudo = editTextConteudo.text.toString().trim()

            // Valida os dados antes de permitir a gravacao da nota.
            if (titulo.isEmpty()) {
                editTextTitulo.error = getString(R.string.note_title_required)
                return@setOnClickListener
            }

            if (conteudo.isEmpty()) {
                editTextConteudo.error = getString(R.string.note_content_required)
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
                    getString(R.string.note_saved),
                    Toast.LENGTH_SHORT
                ).show()

                finish()
            }
        }
    }
}