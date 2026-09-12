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
import pt.goncalomferreira.quicknote.auth.SessionManager
import pt.goncalomferreira.quicknote.data.AppDatabase
import pt.goncalomferreira.quicknote.data.NoteDao
import pt.goncalomferreira.quicknote.model.Note
import pt.goncalomferreira.quicknote.network.ApiClient
import pt.goncalomferreira.quicknote.network.ApiNoteMapper
import pt.goncalomferreira.quicknote.network.NoteRequest
import java.io.IOException

class NoteEditActivity : AppCompatActivity() {

    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var editTextConteudo: EditText
    private lateinit var editTextTitulo: EditText
    private lateinit var buttonGuardar: Button
    private lateinit var buttonEliminar: Button
    private lateinit var buttonDitado: Button

    private lateinit var sessionManager: SessionManager
    private lateinit var noteDao: NoteDao

    private var existingNote: Note? = null

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

        sessionManager = SessionManager(this)

        val authHeader = sessionManager.getAuthorizationHeader()
        val ownerEmail = sessionManager.getUserEmail()

        if (authHeader == null || ownerEmail.isNullOrBlank()) {
            redirectToLogin()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_note_edit)

        // Ajusta o conteúdo da Activity às barras do sistema preservando as margens da aplicação.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val density = resources.displayMetrics.density
            val basePaddingHorizontal = (20 * density).toInt()
            val basePaddingVertical = (16 * density).toInt()

            v.setPadding(
                systemBars.left + basePaddingHorizontal,
                systemBars.top + basePaddingVertical,
                systemBars.right + basePaddingHorizontal,
                systemBars.bottom + basePaddingVertical
            )

            insets
        }

        // Botão voltar no cabeçalho
        val buttonBack = findViewById<View>(R.id.buttonBack)
        buttonBack?.setOnClickListener {
            finish()
        }

        // Referências aos campos utilizados para criar/editar uma nota.
        editTextTitulo = findViewById(R.id.editTextTitulo)
        editTextConteudo = findViewById(R.id.editTextConteudo)
        buttonGuardar = findViewById(R.id.buttonGuardar)
        buttonEliminar = findViewById(R.id.buttonEliminar)
        buttonDitado = findViewById(R.id.buttonDitado)

        buttonDitado.setOnClickListener {
            checkSpeechAndStart()
        }

        // Obtém o DAO através da instância única da base de dados Room.
        noteDao = AppDatabase
            .getDatabase(applicationContext)
            .noteDao()

        val noteId = intent.getLongExtra("NOTE_ID", -1L)

        // Verifica se o editor foi aberto para criar ou editar uma nota.
        if (noteId != -1L) {
            buttonGuardar.isEnabled = false

            lifecycleScope.launch {
                val loadedNote = noteDao.getById(noteId)
                existingNote = loadedNote

                if (loadedNote == null) {
                    Toast.makeText(
                        this@NoteEditActivity,
                        getString(R.string.note_not_found),
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()
                    return@launch
                }

                editTextTitulo.setText(loadedNote.title)
                editTextConteudo.setText(loadedNote.content)

                findViewById<TextView>(R.id.textViewEditorTitulo).text =
                    getString(R.string.editor_edit_note_title)
                buttonEliminar.visibility = View.VISIBLE

                buttonGuardar.isEnabled = true
            }
        }

        // Pede confirmação antes de eliminar a nota na API e no Room.
        buttonEliminar.setOnClickListener {
            val note = existingNote ?: return@setOnClickListener

            if (note.remoteId == null || note.ownerEmail == Note.LEGACY_OWNER) {
                Toast.makeText(
                    this,
                    getString(R.string.error_legacy_note_operation),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            AlertDialog.Builder(this)
                .setTitle(R.string.delete_note)
                .setMessage(R.string.delete_note_confirmation)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete_note) { _, _ ->
                    deleteNoteFromApiAndRoom(authHeader, note)
                }
                .show()
        }

        buttonGuardar.setOnClickListener {
            val titulo = editTextTitulo.text.toString().trim()
            val conteudo = editTextConteudo.text.toString().trim()

            // Valida os dados antes de permitir a gravação da nota.
            if (titulo.isEmpty()) {
                editTextTitulo.error = getString(R.string.note_title_required)
                return@setOnClickListener
            }

            if (conteudo.isEmpty()) {
                editTextConteudo.error = getString(R.string.note_content_required)
                return@setOnClickListener
            }

            val note = existingNote
            if (note == null) {
                createNoteInApiAndRoom(authHeader, ownerEmail, titulo, conteudo)
            } else {
                if (note.remoteId == null || note.ownerEmail == Note.LEGACY_OWNER) {
                    Toast.makeText(
                        this,
                        getString(R.string.error_legacy_note_operation),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
                updateNoteInApiAndRoom(authHeader, ownerEmail, note, titulo, conteudo)
            }
        }
    }

    private fun setUiEnabled(enabled: Boolean) {
        buttonGuardar.isEnabled = enabled
        buttonEliminar.isEnabled = enabled
        buttonDitado.isEnabled = enabled
    }

    private fun createNoteInApiAndRoom(
        authHeader: String,
        ownerEmail: String,
        titulo: String,
        conteudo: String
    ) {
        setUiEnabled(false)

        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.createNote(
                    authHeader,
                    NoteRequest(titulo, conteudo)
                )

                if (response.isSuccessful) {
                    val apiNote = response.body()?.note
                    val noteToInsert = if (apiNote != null) {
                        ApiNoteMapper.toEntity(apiNote, ownerEmail)
                    } else null

                    if (noteToInsert != null) {
                        noteDao.insert(noteToInsert)
                        Toast.makeText(
                            this@NoteEditActivity,
                            getString(R.string.note_saved),
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    } else {
                        Toast.makeText(
                            this@NoteEditActivity,
                            getString(R.string.error_save_note),
                            Toast.LENGTH_SHORT
                        ).show()
                        setUiEnabled(true)
                    }
                } else {
                    when (response.code()) {
                        400 -> {
                            Toast.makeText(
                                this@NoteEditActivity,
                                getString(R.string.error_invalid_data),
                                Toast.LENGTH_SHORT
                            ).show()
                            setUiEnabled(true)
                        }
                        401, 403 -> redirectToLogin()
                        else -> {
                            Toast.makeText(
                                this@NoteEditActivity,
                                getString(R.string.error_server),
                                Toast.LENGTH_SHORT
                            ).show()
                            setUiEnabled(true)
                        }
                    }
                }
            } catch (_: IOException) {
                Toast.makeText(
                    this@NoteEditActivity,
                    getString(R.string.error_save_note_connection),
                    Toast.LENGTH_SHORT
                ).show()
                setUiEnabled(true)
            } catch (_: Exception) {
                Toast.makeText(
                    this@NoteEditActivity,
                    getString(R.string.error_save_note),
                    Toast.LENGTH_SHORT
                ).show()
                setUiEnabled(true)
            }
        }
    }

    private fun updateNoteInApiAndRoom(
        authHeader: String,
        ownerEmail: String,
        currentNote: Note,
        titulo: String,
        conteudo: String
    ) {
        val remoteId = currentNote.remoteId ?: return
        setUiEnabled(false)

        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.updateNote(
                    authHeader,
                    remoteId,
                    NoteRequest(titulo, conteudo)
                )

                if (response.isSuccessful) {
                    val apiNote = response.body()?.note
                    val noteToUpdate = if (apiNote != null) {
                        ApiNoteMapper.toEntity(apiNote, ownerEmail, localId = currentNote.id)
                    } else null

                    if (noteToUpdate != null) {
                        noteDao.update(noteToUpdate)
                        Toast.makeText(
                            this@NoteEditActivity,
                            getString(R.string.note_saved),
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    } else {
                        Toast.makeText(
                            this@NoteEditActivity,
                            getString(R.string.error_save_note),
                            Toast.LENGTH_SHORT
                        ).show()
                        setUiEnabled(true)
                    }
                } else {
                    when (response.code()) {
                        404 -> {
                            Toast.makeText(
                                this@NoteEditActivity,
                                getString(R.string.error_note_not_found_server),
                                Toast.LENGTH_SHORT
                            ).show()
                            noteDao.delete(currentNote)
                            finish()
                        }
                        400 -> {
                            Toast.makeText(
                                this@NoteEditActivity,
                                getString(R.string.error_invalid_data),
                                Toast.LENGTH_SHORT
                            ).show()
                            setUiEnabled(true)
                        }
                        401, 403 -> redirectToLogin()
                        else -> {
                            Toast.makeText(
                                this@NoteEditActivity,
                                getString(R.string.error_server),
                                Toast.LENGTH_SHORT
                            ).show()
                            setUiEnabled(true)
                        }
                    }
                }
            } catch (_: IOException) {
                Toast.makeText(
                    this@NoteEditActivity,
                    getString(R.string.error_save_note_connection),
                    Toast.LENGTH_SHORT
                ).show()
                setUiEnabled(true)
            } catch (_: Exception) {
                Toast.makeText(
                    this@NoteEditActivity,
                    getString(R.string.error_save_note),
                    Toast.LENGTH_SHORT
                ).show()
                setUiEnabled(true)
            }
        }
    }

    private fun deleteNoteFromApiAndRoom(authHeader: String, currentNote: Note) {
        val remoteId = currentNote.remoteId ?: return
        setUiEnabled(false)

        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.deleteNote(
                    authHeader,
                    remoteId
                )

                if (response.isSuccessful || response.code() == 404) {
                    noteDao.delete(currentNote)

                    val message = if (response.code() == 404) {
                        getString(R.string.error_note_not_found_server)
                    } else {
                        getString(R.string.note_deleted)
                    }

                    Toast.makeText(
                        this@NoteEditActivity,
                        message,
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()
                } else if (response.code() == 401 || response.code() == 403) {
                    redirectToLogin()
                } else {
                    Toast.makeText(
                        this@NoteEditActivity,
                        getString(R.string.error_delete_note),
                        Toast.LENGTH_SHORT
                    ).show()
                    setUiEnabled(true)
                }
            } catch (_: IOException) {
                Toast.makeText(
                    this@NoteEditActivity,
                    getString(R.string.error_delete_note_connection),
                    Toast.LENGTH_SHORT
                ).show()
                setUiEnabled(true)
            } catch (_: Exception) {
                Toast.makeText(
                    this@NoteEditActivity,
                    getString(R.string.error_delete_note),
                    Toast.LENGTH_SHORT
                ).show()
                setUiEnabled(true)
            }
        }
    }

    private fun redirectToLogin() {
        sessionManager.clearSession()
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
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
        } catch (_: Exception) {
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
