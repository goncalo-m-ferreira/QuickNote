package com.dam.quicknote.listaNotas

import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.dam.listaNotas.Acerca
import com.dam.listaNotas.Definicoes
import com.dam.listaNotas.PaginaInicial
import com.dam.listaNotas.R
import com.dam.autenticacao.TokenManager
import com.dam.autenticacao.UtilizadorManager
import com.dam.listaNotas.models.Nota
import com.dam.listaNotas.storage.API
import com.dam.listaNotas.storage.MinhaSharedPreferences
import com.dam.listaNotas.storage.Sincronizar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
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

    // Função com evento para procura Notas
    private fun barraPesquisa() {
        searchBar.clearFocus()
        // Evento para ao escrever na searchView serem mostradas as Notas correspondentes ao texto inserido
        searchBar.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(p0: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(texto: String): Boolean {
                // Chama a função para procurar Notas
                procurarNota(texto)
                return true
            }
        })
    }

    // Função para procurar Notas na search bar
    private fun procurarNota(query: String) {
        // Lista de Notas que vai ser atualizada
        val procListaNota = ArrayList<Nota>()
        procListaNota.clear()

        // Percorre a lista de Notas e verifica se o titulo contem o texto inserido na search bar
        for (nota in originalNotaLista) {
            // Se contem adiciona à lista de Notas que vai ser atualizada
            if (nota.titulo.lowercase().contains(query.lowercase())) {
                procListaNota.add(nota)
            }
        }
        // Se a lista de Notas que vai ser atualizada estiver vazia mostra uma mensagem
        if (procListaNota.isEmpty()) {
            notaLista.clear()
            Toast.makeText(this, "Não existe", Toast.LENGTH_SHORT).show()
            // Se não estiver vazia atualiza a lista de Notas
        } else {
            notaLista.clear()
            notaLista.addAll(procListaNota)
        }
        adapter.notifyDataSetChanged()
    }

    // Evento para apagar todas as Notas ao carregar no botão
    private fun eventoApagatuTudo() {
        apagaTudo.setOnClickListener {
            // Chama a função para apagar todas as notas com um AlertDialog
            deleteAll()
        }
    }

    // Função para apagar todas as notas com a ajuda do AlertDialog
    private fun deleteAll() {
        // Construção do AlertDialog usando padrão Builder - this referencia o contexto
        AlertDialog.Builder(this)
            // Título
            .setTitle("Apagar tudo")
            // Mensagem
            .setMessage("Tem a certeza que quer apagar as Notas todas?")
            // Cria e prepara o botão para responder ao click
            .setPositiveButton("Sim", DialogInterface.OnClickListener { dialog, id ->
                // Apaga todas as Notas
                sp.apagarTudo(notaLista)
                notaLista.addAll(sp.getNotas())
                adapter.notifyDataSetChanged()
            })
            // Cria e prepara o botão para responder ao click
            .setNegativeButton(
                "Não",
                DialogInterface.OnClickListener { dialog, id -> dialog.cancel() })
            // Faz a construção do AlertDialog com todas as configurações
            .create()
            // Exibe
            .show()
    }
    // ----------------------------------------------------------------- Menu ---------------------------------------------------------------------------

    // Função para configurar o DrawerLayout
    private fun setupDrawerLayout() {
        // Criação do DrawerLayout
        drawerLayout = findViewById(R.id.drawer_layout)
        // Botão para abrir o DrawerLayout
        val menuBtn = findViewById<ImageButton>(R.id.btnMenu)

        // Evento ao carregar no botão que abre o DrawerLayout
        menuBtn.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
        // Criação do ActionBarDrawerToggle
        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        // Adiciona o toggle ao DrawerLayout
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Criação do NavigationView
        val navView: NavigationView = findViewById(R.id.nav_view)
        setupNavigationView(navView)
    }
    // Função para configurar o NavigationView
    private fun setupNavigationView(navView: NavigationView) {

        // Inicialização das variaveis
        val navigationView: NavigationView = findViewById(R.id.nav_view)
        val headerView = navigationView.getHeaderView(0)
        val nome = headerView.findViewById<TextView>(R.id.nome)
        val loginMenuItem = navView.menu.findItem(R.id.nav_login)
        val ImagemPerfil = headerView.findViewById<ImageView>(R.id.fotoPerfil)

        // Evento que verifica qual o item do menu que foi selecionado e executa a ação correspondente
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                // Botão para ir para a pagina RascunhoNota para criar uma nova Nota
                R.id.nav_home -> {
                    CoroutineScope(Dispatchers.Main).launch {
                        val intent = Intent(this@ListaNotas, RascunhoNota::class.java)
                        // Variavel index é metida a -1 para evidenciar que não foi escolhido nenhuma na Nota
                        index=-1
                        intent.putExtra("objeto",index)
                        startActivity(intent)
                    }
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true

                }
                // Botão para ir para a pagina Definições
                R.id.nav_settings -> {
                    CoroutineScope(Dispatchers.Main).launch {
                        val intent = Intent(this@ListaNotas, Definicoes::class.java)
                        startActivity(intent)
                    }
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                // Botão para ir para a pagina Acerca
                R.id.nav_about -> {
                    CoroutineScope(Dispatchers.Main).launch {
                        val intent = Intent(this@ListaNotas, Acerca::class.java)
                        startActivity(intent)
                    }
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                else -> false
            }
        }
        // Condição para verificar se o utilizador está logado
        if(!utilizadorEmail.isEmpty()){
            // Cofiguração das opções exclusivas para o utilizador logado
            nome.text= UtilizadorManager.buscarUserName().toString()
            loginMenuItem.setIcon(getResources().getDrawable(R.drawable.login))
            loginMenuItem.setTitle("Sair")
            // Condição para verificar se o utilizador tem imagem de perfil
            if(UtilizadorManager.buscarImagemPerfil() != null){
                ImagemPerfil?.setImageURI(Uri.parse(UtilizadorManager.buscarImagemPerfil()))
            }
            // Evento ao carregar na opção "Sair"
            loginMenuItem.setOnMenuItemClickListener{
                // Sinconizar Notas com a API
                sync.sync(this)
                // Atualizar flags
                sp.marcarFlag("buscar", true)
                sp.marcarFlag("logado", false)
                // Fazer logout online
                api.logoutUtilizadorAPI(utilizadorToken, utilizadorEmail, this)

                drawerLayout.closeDrawer(GravityCompat.START)
                true
            }
        }else{
            // Cofiguração das opções exclusivas para o utilizador offline
            nome.text= "Convidado"
            loginMenuItem.setIcon(getResources().getDrawable(R.drawable.logout))
            loginMenuItem.setTitle("Entrar/Registar")
            // Evento ao carregar na opção "Entrar/Registar"
            loginMenuItem.setOnMenuItemClickListener{
                // Mudar de atividade para a pagina PaginaInicial
                startActivity(Intent(this, PaginaInicial::class.java))
                finish()
                drawerLayout.closeDrawer(GravityCompat.START)
                true
            }
        }


    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    // Função onResume para atualizar o DrawerLayout
    override fun onResume() {
        super.onResume()
        setupDrawerLayout()
    }

}