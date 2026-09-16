package pt.goncalomferreira.quicknote

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import pt.goncalomferreira.quicknote.auth.SessionManager
import pt.goncalomferreira.quicknote.data.AppDatabase
import pt.goncalomferreira.quicknote.data.NoteDao
import pt.goncalomferreira.quicknote.model.Note
import pt.goncalomferreira.quicknote.network.ApiClient
import pt.goncalomferreira.quicknote.network.ApiNoteMapper
import pt.goncalomferreira.quicknote.network.NoteRequest
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

class NoteEditActivity : AppCompatActivity() {

    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var editTextConteudo: EditText
    private lateinit var editTextTitulo: EditText
    private lateinit var buttonGuardar: Button
    private lateinit var buttonEliminar: Button
    private lateinit var buttonDitado: Button

    private lateinit var buttonCamera: MaterialButton
    private lateinit var cardPhotoPreview: MaterialCardView
    private lateinit var imageViewPhotoPreview: ImageView
    private lateinit var buttonRemovePhoto: MaterialButton

    private lateinit var sessionManager: SessionManager
    private lateinit var noteDao: NoteDao

    private var existingNote: Note? = null

    // Photo state management
    private var pendingPhotoBytes: ByteArray? = null
    private var hasExistingRemotePhoto: Boolean = false
    private var photoMarkedForDeletion: Boolean = false
    private var hasPendingPhoto: Boolean = false

    companion object {
        private const val KEY_HAS_PENDING_PHOTO = "KEY_HAS_PENDING_PHOTO"
        private const val KEY_PHOTO_MARKED_FOR_DELETION = "KEY_PHOTO_MARKED_FOR_DELETION"
    }

    private fun getTempPhotoUri(): Uri {
        val tempFile = File(cacheDir, "temp_note_photo.jpg")
        return FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            tempFile
        )
    }

    private fun processPendingPhoto() {
        val uri = getTempPhotoUri()
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Read EXIF orientation
                var orientation = ExifInterface.ORIENTATION_NORMAL
                contentResolver.openInputStream(uri)?.use { stream ->
                    val exif = ExifInterface(stream)
                    orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                }

                // Decode original bitmap
                val originalBitmap = contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }

                if (originalBitmap != null) {
                    // Apply EXIF rotation
                    val rotatedBitmap = when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(originalBitmap, 90f)
                        ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(originalBitmap, 180f)
                        ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(originalBitmap, 270f)
                        else -> originalBitmap
                    }

                    // Scale down after rotation
                    val resizedBitmap = scaleBitmapDown(rotatedBitmap, 1024)

                    // Compress to JPEG 80%
                    val byteArrayOutputStream = ByteArrayOutputStream()
                    resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
                    val bytes = byteArrayOutputStream.toByteArray()

                    pendingPhotoBytes = bytes
                    hasPendingPhoto = true
                    photoMarkedForDeletion = false

                    withContext(Dispatchers.Main) {
                        imageViewPhotoPreview.setImageBitmap(resizedBitmap)
                        cardPhotoPreview.visibility = View.VISIBLE
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@NoteEditActivity,
                        getString(R.string.error_unknown),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

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

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            processPendingPhoto()
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

        if (savedInstanceState != null) {
            hasPendingPhoto = savedInstanceState.getBoolean(KEY_HAS_PENDING_PHOTO, false)
            photoMarkedForDeletion = savedInstanceState.getBoolean(KEY_PHOTO_MARKED_FOR_DELETION, false)
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

        buttonCamera = findViewById(R.id.buttonCamera)
        cardPhotoPreview = findViewById(R.id.cardPhotoPreview)
        imageViewPhotoPreview = findViewById(R.id.imageViewPhotoPreview)
        buttonRemovePhoto = findViewById(R.id.buttonRemovePhoto)

        if (hasPendingPhoto) {
            processPendingPhoto()
        }

        buttonDitado.setOnClickListener {
            checkSpeechAndStart()
        }

        buttonCamera.setOnClickListener {
            try {
                val uri = getTempPhotoUri()
                takePictureLauncher.launch(uri)
            } catch (_: Exception) {
                Toast.makeText(this, getString(R.string.error_unknown), Toast.LENGTH_SHORT).show()
            }
        }

        buttonRemovePhoto.setOnClickListener {
            pendingPhotoBytes = null
            hasPendingPhoto = false
            if (hasExistingRemotePhoto) {
                photoMarkedForDeletion = true
                hasExistingRemotePhoto = false
            }
            imageViewPhotoPreview.setImageDrawable(null)
            cardPhotoPreview.visibility = View.GONE
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

                val remoteId = loadedNote.remoteId
                if (remoteId != null && loadedNote.ownerEmail != Note.LEGACY_OWNER) {
                    if (!hasPendingPhoto && !photoMarkedForDeletion) {
                        loadRemotePhoto(authHeader, remoteId)
                    }
                }

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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_HAS_PENDING_PHOTO, hasPendingPhoto)
        outState.putBoolean(KEY_PHOTO_MARKED_FOR_DELETION, photoMarkedForDeletion)
    }

    private fun loadRemotePhoto(authHeader: String, remoteId: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = ApiClient.apiService.getNotePhoto(authHeader, remoteId)
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody != null) {
                        val inputStream = responseBody.byteStream()
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        inputStream.close()
                        if (bitmap != null) {
                            hasExistingRemotePhoto = true
                            withContext(Dispatchers.Main) {
                                imageViewPhotoPreview.setImageBitmap(bitmap)
                                cardPhotoPreview.visibility = View.VISIBLE
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                // Keep cardPhotoPreview hidden if no photo or error
            }
        }
    }

    private suspend fun syncPhotoAfterSave(authHeader: String, remoteId: Long) {
        if (photoMarkedForDeletion) {
            try {
                val response = ApiClient.apiService.deleteNotePhoto(authHeader, remoteId)
                if (response.isSuccessful || response.code() == 404) {
                    photoMarkedForDeletion = false
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@NoteEditActivity,
                            getString(R.string.error_photo_delete),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@NoteEditActivity,
                        getString(R.string.error_photo_delete),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } else if (pendingPhotoBytes != null) {
            val bytes = pendingPhotoBytes!!
            try {
                val mediaType = MediaType.parse("image/jpeg")
                val requestBody = RequestBody.create(mediaType, bytes)
                val part = MultipartBody.Part.createFormData("photo", "photo.jpg", requestBody)
                val response = ApiClient.apiService.uploadNotePhoto(authHeader, remoteId, part)

                if (response.isSuccessful) {
                    hasPendingPhoto = false
                    pendingPhotoBytes = null
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@NoteEditActivity,
                            getString(R.string.error_photo_upload),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@NoteEditActivity,
                        getString(R.string.error_photo_upload),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated != bitmap) {
            bitmap.recycle()
        }
        return rotated
    }

    private fun scaleBitmapDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap

        val ratio = width.toFloat() / height.toFloat()
        val targetWidth: Int
        val targetHeight: Int

        if (width > height) {
            targetWidth = maxDimension
            targetHeight = (maxDimension / ratio).toInt()
        } else {
            targetHeight = maxDimension
            targetWidth = (maxDimension * ratio).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    private fun setUiEnabled(enabled: Boolean) {
        buttonGuardar.isEnabled = enabled
        buttonEliminar.isEnabled = enabled
        buttonDitado.isEnabled = enabled
        buttonCamera.isEnabled = enabled
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
                        val remoteId = apiNote?.id
                        if (remoteId != null) {
                            syncPhotoAfterSave(authHeader, remoteId)
                        }

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
                        syncPhotoAfterSave(authHeader, remoteId)

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
