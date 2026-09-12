package pt.goncalomferreira.quicknote

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import pt.goncalomferreira.quicknote.adapter.NoteAdapter
import pt.goncalomferreira.quicknote.auth.SessionManager
import pt.goncalomferreira.quicknote.data.AppDatabase
import pt.goncalomferreira.quicknote.data.NoteDao
import pt.goncalomferreira.quicknote.model.Note
import pt.goncalomferreira.quicknote.network.ApiClient
import pt.goncalomferreira.quicknote.network.ApiNoteMapper
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var noteDao: NoteDao
    private lateinit var textViewSemNotas: TextView
    private lateinit var recyclerViewNotas: RecyclerView
    private lateinit var editTextSearch: EditText
    private lateinit var noteAdapter: NoteAdapter
    private lateinit var buttonLogout: Button

    private var allNotes: List<Note> = emptyList()
    private var currentSearchQuery: String = ""
    private var isRefreshingNotes = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager(this)

        // Proteção da MainActivity: verifica se existe token guardado.
        if (sessionManager.getToken() == null) {
            redirectToLogin()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

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

        textViewSemNotas = findViewById(R.id.textViewSemNotas)
        recyclerViewNotas = findViewById(R.id.recyclerViewNotas)
        editTextSearch = findViewById(R.id.editTextSearch)
        buttonLogout = findViewById(R.id.buttonLogout)

        // Botão de Perfil no cabeçalho
        val buttonProfile = findViewById<View>(R.id.buttonProfile)
        buttonProfile?.setOnClickListener {
            showAccountDialog()
        }

        // Configuração do filtro de pesquisa local
        editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearchQuery = s?.toString()?.trim() ?: ""
                applyFilter()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Configuração do botão Terminar sessão
        buttonLogout.setOnClickListener {
            buttonLogout.isEnabled = false
            buttonLogout.text = getString(R.string.logging_out)

            val authHeader = sessionManager.getAuthorizationHeader()

            lifecycleScope.launch {
                if (authHeader != null) {
                    try {
                        ApiClient.apiService.logout(authHeader)
                    } catch (_: Exception) {
                        // Ignora falhas de rede no logout remoto (JWT é stateless)
                    }
                }

                redirectToLogin()
            }
        }

        // Configura a lista que apresenta as notas guardadas.
        noteAdapter = NoteAdapter { note ->
            val intent = Intent(this, NoteEditActivity::class.java).apply {
                putExtra("NOTE_ID", note.id)
            }
            startActivity(intent)
        }
        recyclerViewNotas.layoutManager = LinearLayoutManager(this)
        recyclerViewNotas.adapter = noteAdapter

        // Obtém o DAO através da instância única da base de dados Room.
        noteDao = AppDatabase
            .getDatabase(applicationContext)
            .noteDao()

        val buttonNovaNota = findViewById<View>(R.id.buttonNovaNota)

        // Abre o ecrã de criação de uma nova nota.
        buttonNovaNota.setOnClickListener {
            val intent = Intent(this, NoteEditActivity::class.java)
            startActivity(intent)
        }

        val buttonSobre = findViewById<View>(R.id.buttonSobre)

        // Abre o ecrã com as informações sobre a aplicação.
        buttonSobre.setOnClickListener {
            val intent = Intent(this, AboutActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showAccountDialog() {
        val email = sessionManager.getUserEmail()
        val displayEmail = if (!email.isNullOrBlank()) email else getString(R.string.authenticated_user)

        AlertDialog.Builder(this)
            .setTitle(R.string.account_dialog_title)
            .setMessage(displayEmail)
            .setPositiveButton(R.string.close, null)
            .show()
    }

    override fun onResume() {
        super.onResume()

        val authHeader = sessionManager.getAuthorizationHeader()
        if (authHeader == null) {
            redirectToLogin()
            return
        }

        loadAndSyncNotes(authHeader)
    }

    private fun loadAndSyncNotes(authHeader: String) {
        lifecycleScope.launch {
            var ownerEmail = sessionManager.getUserEmail()

            // Se o email do utilizador não estiver guardado na sessão, tenta obter via GET /users/me
            if (ownerEmail.isNullOrBlank()) {
                try {
                    val userResponse = ApiClient.apiService.getCurrentUser(authHeader)
                    if (userResponse.isSuccessful) {
                        val fetchedEmail = userResponse.body()?.user?.email
                        if (!fetchedEmail.isNullOrBlank()) {
                            sessionManager.saveUserEmail(fetchedEmail)
                            ownerEmail = fetchedEmail
                        } else {
                            return@launch
                        }
                    } else if (userResponse.code() == 401 || userResponse.code() == 403) {
                        redirectToLogin()
                        return@launch
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            getString(R.string.error_fetch_notes),
                            Toast.LENGTH_SHORT
                        ).show()
                        return@launch
                    }
                } catch (_: Exception) {
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.error_connection_showing_cache),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }
            }

            // 1. Carregar imediatamente o cache local do ownerEmail
            updateUIWithLocalCache(ownerEmail)

            // 2. Sincronizar com a API
            if (!isRefreshingNotes) {
                syncNotesFromApi(authHeader, ownerEmail)
            }
        }
    }

    private suspend fun updateUIWithLocalCache(ownerEmail: String) {
        allNotes = noteDao.getByOwnerEmail(ownerEmail)
        applyFilter()
    }

    private fun applyFilter() {
        if (allNotes.isEmpty()) {
            recyclerViewNotas.visibility = View.GONE
            textViewSemNotas.visibility = View.VISIBLE
            textViewSemNotas.text = getString(R.string.empty_notes_message)
            return
        }

        val filteredList = if (currentSearchQuery.isBlank()) {
            allNotes
        } else {
            allNotes.filter { note ->
                note.title.contains(currentSearchQuery, ignoreCase = true) ||
                        note.content.contains(currentSearchQuery, ignoreCase = true)
            }
        }

        if (filteredList.isEmpty()) {
            recyclerViewNotas.visibility = View.GONE
            textViewSemNotas.visibility = View.VISIBLE
            textViewSemNotas.text = getString(R.string.no_notes_found)
        } else {
            textViewSemNotas.visibility = View.GONE
            recyclerViewNotas.visibility = View.VISIBLE
            noteAdapter.submitList(filteredList)
        }
    }

    private suspend fun syncNotesFromApi(authHeader: String, ownerEmail: String) {
        isRefreshingNotes = true
        try {
            val response = ApiClient.apiService.getNotes(authHeader)

            if (response.isSuccessful) {
                val apiNotes = response.body()?.notes ?: emptyList()

                val convertedNotes = apiNotes.mapNotNull { apiNote ->
                    ApiNoteMapper.toEntity(apiNote, ownerEmail)
                }

                noteDao.replaceCacheForOwner(ownerEmail, convertedNotes)
                updateUIWithLocalCache(ownerEmail)
            } else {
                when (response.code()) {
                    401, 403 -> redirectToLogin()
                    else -> {
                        Toast.makeText(
                            this@MainActivity,
                            getString(R.string.error_fetch_notes),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        } catch (_: IOException) {
            Toast.makeText(
                this@MainActivity,
                getString(R.string.error_connection_showing_cache),
                Toast.LENGTH_SHORT
            ).show()
        } catch (_: Exception) {
            Toast.makeText(
                this@MainActivity,
                getString(R.string.error_fetch_notes),
                Toast.LENGTH_SHORT
            ).show()
        } finally {
            isRefreshingNotes = false
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
}
