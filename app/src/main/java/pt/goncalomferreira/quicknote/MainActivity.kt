package pt.goncalomferreira.quicknote

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
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
import pt.goncalomferreira.quicknote.network.ApiClient
import pt.goncalomferreira.quicknote.network.ApiNoteMapper
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var noteDao: NoteDao
    private lateinit var textViewSemNotas: TextView
    private lateinit var recyclerViewNotas: RecyclerView
    private lateinit var noteAdapter: NoteAdapter
    private lateinit var textViewUser: TextView
    private lateinit var buttonLogout: Button

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

        // Ajusta o conteúdo da Activity às barras do sistema.
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

        textViewSemNotas = findViewById(R.id.textViewSemNotas)
        recyclerViewNotas = findViewById(R.id.recyclerViewNotas)
        textViewUser = findViewById(R.id.textViewUser)
        buttonLogout = findViewById(R.id.buttonLogout)

        // Indicação visível do utilizador autenticado
        val email = sessionManager.getUserEmail()
        if (!email.isNullOrBlank()) {
            textViewUser.text = getString(R.string.session_user, email)
        } else {
            textViewUser.text = getString(R.string.authenticated_user)
        }

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

        val buttonNovaNota = findViewById<Button>(R.id.buttonNovaNota)

        // Abre o ecrã de criação de uma nova nota.
        buttonNovaNota.setOnClickListener {
            val intent = Intent(this, NoteEditActivity::class.java)
            startActivity(intent)
        }

        val buttonSobre = findViewById<Button>(R.id.buttonSobre)

        // Abre o ecrã com as informações sobre a aplicação.
        buttonSobre.setOnClickListener {
            val intent = Intent(this, AboutActivity::class.java)
            startActivity(intent)
        }
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

            // Se o email do utilizador nao estiver guardado na sessao, tenta obter via GET /users/me
            if (ownerEmail.isNullOrBlank()) {
                try {
                    val userResponse = ApiClient.apiService.getCurrentUser(authHeader)
                    if (userResponse.isSuccessful) {
                        val fetchedEmail = userResponse.body()?.user?.email
                        if (!fetchedEmail.isNullOrBlank()) {
                            sessionManager.saveUserEmail(fetchedEmail)
                            ownerEmail = fetchedEmail
                            textViewUser.text = getString(R.string.session_user, ownerEmail)
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
            } else {
                textViewUser.text = getString(R.string.session_user, ownerEmail)
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
        val notas = noteDao.getByOwnerEmail(ownerEmail)

        if (notas.isEmpty()) {
            recyclerViewNotas.visibility = View.GONE
            textViewSemNotas.visibility = View.VISIBLE
        } else {
            textViewSemNotas.visibility = View.GONE
            recyclerViewNotas.visibility = View.VISIBLE

            noteAdapter.submitList(notas)
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
