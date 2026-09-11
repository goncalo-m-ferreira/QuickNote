package pt.goncalomferreira.quicknote

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import pt.goncalomferreira.quicknote.adapter.NoteAdapter
import pt.goncalomferreira.quicknote.data.AppDatabase
import pt.goncalomferreira.quicknote.data.NoteDao

class MainActivity : AppCompatActivity() {

    private lateinit var noteDao: NoteDao
    private lateinit var textViewSemNotas: TextView
    private lateinit var recyclerViewNotas: RecyclerView
    private lateinit var noteAdapter: NoteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

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

        textViewSemNotas = findViewById(R.id.textViewSemNotas)
        recyclerViewNotas = findViewById(R.id.recyclerViewNotas)

        // Configura a lista que apresenta as notas guardadas.
        // Abre a nota selecionada e envia o seu identificador para o editor.
        noteAdapter = NoteAdapter { note ->
            val intent = Intent(this, NoteEditActivity::class.java).apply {
                putExtra("NOTE_ID", note.id)
            }
            startActivity(intent)
        }
        recyclerViewNotas.layoutManager = LinearLayoutManager(this)
        recyclerViewNotas.adapter = noteAdapter

        // Obtem o DAO atraves da instancia unica da base de dados Room.
        noteDao = AppDatabase
            .getDatabase(applicationContext)
            .noteDao()

        val buttonNovaNota = findViewById<Button>(R.id.buttonNovaNota)

        // Abre o ecra de criacao de uma nova nota.
        buttonNovaNota.setOnClickListener {
            val intent = Intent(this, NoteEditActivity::class.java)
            startActivity(intent)
        }

        val buttonSobre = findViewById<Button>(R.id.buttonSobre)

        // Abre o ecra com as informacoes sobre a aplicacao.
        buttonSobre.setOnClickListener {
            val intent = Intent(this, AboutActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()

        // Atualiza a lista sempre que o utilizador regressa a este ecra.
        lifecycleScope.launch {
            val notas = noteDao.getAll()

            if (notas.isEmpty()) {
                recyclerViewNotas.visibility = View.GONE
                textViewSemNotas.visibility = View.VISIBLE
            } else {
                textViewSemNotas.visibility = View.GONE
                recyclerViewNotas.visibility = View.VISIBLE

                noteAdapter.submitList(notas)
            }
        }
    }
}