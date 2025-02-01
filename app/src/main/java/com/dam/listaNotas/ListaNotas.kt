package com.dam.listaNotas

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ListaNotas : AppCompatActivity() {

    // Criação das variaveis
    private val originalNotaLista = ArrayList<Nota>() // Lista de objetos Nota
    private val notaLista = ArrayList<Nota>() // Lista de objetos Nota
    private lateinit var adapter: ListaNotasAdapter
    private lateinit var ListaDeNotas: RecyclerView
    private lateinit var searchBar: SearchView
    private lateinit var fab: FloatingActionButton
    private var index: Int = 0
    private var sp: MinhaSharedPreferences = MinhaSharedPreferences()
    private var api: API = API()
    private lateinit var apagaTudo: ImageButton
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var utilizadorEmail: String
    private lateinit var utilizadorToken: String
    private lateinit var utilizadorNome: String
    private lateinit var utilizadorImagemPerfil: String
    private var sync: Sincronizar = Sincronizar()
    private val handler = android.os.Handler()
    private val delay: Long = 3000 // 3 segundos
    private var isDialogShowing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_notas)

        // Pedir permissões
        if (checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED ||
            checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED ||
            checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_DENIED
        ) {
            val permission = arrayOf(
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.READ_MEDIA_IMAGES
            )
            requestPermissions(permission, 112)
        }

        //inicialização das variaveis
        sp.init(this)
        sync.init(this)
        ListaDeNotas = findViewById(R.id.note_list_recyclerview)
        fab = findViewById(R.id.Adicionar)
        searchBar = findViewById(R.id.searchBar)
        apagaTudo = findViewById(R.id.apagarTudo)
        utilizadorEmail = UtilizadorManager.buscarEMAIL().toString()
        utilizadorNome = UtilizadorManager.buscarUserName().toString()
        TokenManager.init(this)
        utilizadorToken = TokenManager.buscarToken().toString()

        // Flag que é inicializada a true para verificar se o utilizador tem internet
        sp.marcarFlag("internet", true)

        // Configuração do layout e adapter para a RecyclerView
        val layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        // Define o layout manager da RecyclerView
        ListaDeNotas.layoutManager = layoutManager
        // Adiciona um espaçamento decorativo à RecyclerView
        ListaDeNotas.addItemDecoration(DecoracaoEspacoItem(this))
        // Inicializa o adapter e define o comportamento do clique no item
        adapter = ListaNotasAdapter(notaLista, this) { clickedNote ->
            // Prepara e inicia uma nova atividade ao clicar no item
            val intent = Intent(this, RascunhoNota::class.java)
            // Indice da nota selecionada
            index = notaLista.indexOf(clickedNote)
            intent.putExtra("objeto", index)
            startActivity(intent)

        }
        // Define o adapter na RecyclerView
        ListaDeNotas.adapter = adapter

        // Função de configuração do DrawerLayout
        setupDrawerLayout()


        lifecycleScope.launch(Dispatchers.Main) {

            // Condicao para verificar se o utilizador está logado
            if (!utilizadorEmail.isEmpty()) {
                // Flag que verifica se é para buscar as Notas
                if (sp.buscarFlag("buscar")) {
                    // Buscar Notas da API
                    val notas = api.buscarNotasAPI("${TokenManager.buscarToken()}", this@ListaNotas)
                    // Guardar Notas
                    sp.salvarNotas(notas)
                    sp.salvarNotasAPISP(notas)
                    // Atualizar flag
                    sp.marcarFlag("buscar", false)
                    // Guardar o total de Notas
                    sp.setTotal(notas.size)
                    delay(1000)
                }
            }
        }
    }
}