package pt.goncalomferreira.quicknote

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import pt.goncalomferreira.quicknote.data.AppDatabase
import pt.goncalomferreira.quicknote.model.Note

class NoteEditActivity : AppCompatActivity() {

    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var editTextConteudo: EditText

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startDictation()
        } else {
            Toast.makeText(
                this,
                getString(R.string.microphone_permission_required),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

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
        editTextConteudo = findViewById(R.id.editTextConteudo)
        val buttonGuardar = findViewById<Button>(R.id.buttonGuardar)
        val buttonEliminar = findViewById<Button>(R.id.buttonEliminar)
        val buttonDitado = findViewById<Button>(R.id.buttonDitado)

        buttonDitado.setOnClickListener {
            checkSpeechAndStart()
        }

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

    private fun checkSpeechAndStart() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(
                this,
                getString(R.string.speech_not_available),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startDictation()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startDictation() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(
                this,
                getString(R.string.speech_not_available),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        if (isFinishing || isDestroyed) return
                        Toast.makeText(
                            this@NoteEditActivity,
                            getString(R.string.listening),
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}

                    override fun onError(error: Int) {
                        if (isFinishing || isDestroyed) return
                        Toast.makeText(
                            this@NoteEditActivity,
                            getString(R.string.speech_recognition_error),
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    override fun onResults(results: Bundle?) {
                        if (isFinishing || isDestroyed) return
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val recognizedText = matches[0]
                            appendRecognizedText(recognizedText)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-PT")
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            if (!isFinishing && !isDestroyed) {
                Toast.makeText(
                    this,
                    getString(R.string.speech_recognition_error),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun appendRecognizedText(recognizedText: String) {
        if (recognizedText.isBlank()) return

        val existingText = editTextConteudo.text.toString()

        val newText = if (existingText.isEmpty()) {
            recognizedText
        } else if (existingText.endsWith(" ") || existingText.endsWith("\n")) {
            existingText + recognizedText
        } else {
            "$existingText $recognizedText"
        }

        editTextConteudo.setText(newText)
        editTextConteudo.setSelection(editTextConteudo.text.length)
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}