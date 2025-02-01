package com.dam.listaNotas

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
            // Limpar a lista de Notas
            originalNotaLista.clear()
            // Adicionar as Notas atualizadas à lista
            originalNotaLista.addAll(sp.getNotas())
            // Limpar a lista de Notas
            notaLista.clear()
            // Adicionar as Notas atualizadas à lista
            notaLista.addAll(sp.getNotas())

            // Notifica as mudanças da lista para o RecyclerView
            adapter.notifyDataSetChanged()

            // Função com evento de criar Nota
            botaoAdicionarNota()

            // Função com evento para procura Notas
            barraPesquisa()

            // Função com evento de apagar todas as Notas
            eventoApagatuTudo()

            // Condição para verificar se o utilizador está logado
            if (sp.buscarFlag("logado")) {
                // Sincronizar Notas
                if (sync.sync(this@ListaNotas)) {
                    delay(5000)
                    // Buscar Notas da API
                    val notas = api.buscarNotasAPI("${TokenManager.buscarToken()}", this@ListaNotas)
                    //  Guardar Notas
                    sp.salvarNotasAPISP(notas)
                    // Atualizar flag
                    sp.marcarFlag("logado", false)
                    // Guardar o total de Notas
                    sp.setTotal(notas.size)
                }
            }
            // Função que lida com a verificação da conexão á Internet
            checkInternet()

        }

    }

    // Função com evento de criar Nota
    private fun botaoAdicionarNota() {
        // Evento ao carregar no botão
        fab.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                // Muda de atividade
                val intent = Intent(this@ListaNotas, RascunhoNota::class.java)
                // Variavel index é metida a -1 para evidenciar que não foi escolhido nenhuma Nota
                index = -1
                intent.putExtra("objeto", index)
                startActivity(intent)
            }

        }
    }

    // faz check para saber se tem conexão á Internet
    private fun checkInternet() {
        // Criação do AlertDialog
        val builder = AlertDialog.Builder(this@ListaNotas)
        builder.setTitle("Sem Conexão á Internet!")
        builder.setMessage("Por favor conecte-se á Internet para poder usar a aplicação ou utilize o modo convidado")
        // esta opção faz um logout forçado offline e muda para modo offline pois não tem internet
        builder.setPositiveButton("Convidado") { dialog, _ ->
            dialog.dismiss()
            isDialogShowing = false
            // Apagar dados do utilizador
            UtilizadorManager.apagarUtilizador()
            // Apagar token
            TokenManager.apagarToken()
            // Atualizar flags
            sp.marcarFlag("buscar", true)
            sp.marcarFlag("logado", false)
            finish()
            startActivity(Intent(this@ListaNotas, ListaNotas::class.java))
        }
        // esta opção faz um logout forçado offline e sai da aplicação pois não tem internet
        builder.setNegativeButton("Sair") { dialog, _ ->
            // Apagar dados do utilizador
            UtilizadorManager.apagarUtilizador()
            // Apagar token
            TokenManager.apagarToken()
            // Atualizar flags
            sp.marcarFlag("buscar", true)
            sp.marcarFlag("logado", false)
            finishAffinity()
        }
        val dialog = builder.create()

        handler.postDelayed(object : Runnable {
            override fun run() {
                // Verifica se existe utilizador logado pois este utiliza modo online
                if (utilizadorEmail.isNotEmpty()) {
                    // Verifica se tem conexão á Internet
                    if (!api.internetConectada(this@ListaNotas) && !isDialogShowing) {
                        // Verifica se o AlertDialog está a ser mostrado através da flag
                        if (sp.buscarFlag("internet")) {
                            if (!dialog.isShowing) {
                                dialog.show()
                                isDialogShowing = true
                                // Atualizar flag
                                sp.marcarFlag("internet", false)
                            }
                        }
                        // Verifica se tem conexão á Internet e se o AlertDialog está a ser mostrado
                    } else if (api.internetConectada(this@ListaNotas) && isDialogShowing) {
                        dialog.dismiss()
                        isDialogShowing = false
                        // Atualizar flag caso seja falsa
                        if (!sp.buscarFlag("internet")) {
                            Toast.makeText(this@ListaNotas, "Tem internet", Toast.LENGTH_SHORT)
                                .show()
                        }
                        // Atualizar flag
                        sp.marcarFlag("internet", true)
                    }
                }
                handler.postDelayed(this, delay)
            }
        }, delay)
    }
}