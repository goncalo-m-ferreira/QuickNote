package com.dam.quicknote

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.dam.quicknote.autenticacao.TokenManager
import com.dam.quicknote.autenticacao.UtilizadorManager
import com.dam.quicknote.listaNotas.ListaNotas
import com.dam.quicknote.models.Nota
import com.dam.quicknote.storage.API
import com.dam.quicknote.storage.MinhaSharedPreferences
import com.dam.quicknote.storage.Sincronizar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileDescriptor
import java.io.IOException


class Definicoes : AppCompatActivity() {

    private lateinit var nomePerfil : TextView
    private lateinit var utilizadorEmail : String
    private lateinit var nomeUtilizador: TextInputEditText
    private lateinit var passwordUtilizador: TextInputEditText
    private lateinit var mudarpass : TextView
    private lateinit var voltarBtn: ImageButton
    private lateinit var btnNome : ImageButton
    private lateinit var btnPassword : ImageButton
    private lateinit var acerca : TextView
    private lateinit var api : API
    private lateinit var btnApagaConta : Button
    private lateinit var utilizadorNome : String
    private lateinit var mudarNomeUtilizador : TextView
    private var notas = ArrayList<Nota>()
    private var sync : Sincronizar = Sincronizar()
    private var sp : MinhaSharedPreferences = MinhaSharedPreferences()
    private var frame: ImageView? = null
    private var imageUri: Uri? = null
    private val RESULT_LOAD_IMAGE = 123
    private val IMAGE_CAPTURE_CODE = 654


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_definicoes)

        //inicialização das variaveis
        voltarBtn = findViewById(R.id.voltar)
        nomePerfil = findViewById(R.id.nome)
        nomeUtilizador = findViewById(R.id.edNome)
        passwordUtilizador = findViewById(R.id.edPassword)
        btnNome = findViewById(R.id.btNome)
        btnPassword = findViewById(R.id.btnPassword)
        acerca = findViewById(R.id.defAcerca)
        utilizadorEmail = UtilizadorManager.buscarEMAIL().toString()
        utilizadorNome = UtilizadorManager.buscarUserName().toString()
        mudarNomeUtilizador = findViewById(R.id.chNome)
        mudarpass = findViewById(R.id.chPassword)
        btnApagaConta = findViewById(R.id.apagaConta)
        frame= findViewById(R.id.fotoPerfil)
        api= API()
        sp.init(this)
        sync.init(this)

        // Verifica se o utilizador está logado
        if(utilizadorEmail.isNotEmpty()){
            // Cofiguração das opções exclusivas para o utilizador logado
            nomePerfil.text = utilizadorNome
            nomeUtilizador.setText(utilizadorNome)
            if(UtilizadorManager.buscarImagemPerfil() != null){
                frame?.setImageURI(Uri.parse(UtilizadorManager.buscarImagemPerfil()))
            }
        }else{
            // Cofiguração das opções exclusivas para o utilizador offline
            nomePerfil.text = "Convidado"
            nomeUtilizador.visibility = View.GONE
            mudarNomeUtilizador.visibility = View.GONE
            passwordUtilizador.visibility = View.GONE
            mudarpass.visibility = View.GONE
            btnPassword.visibility = View.GONE
            btnNome.visibility = View.GONE
            btnApagaConta.visibility = View.GONE
            frame?.setImageResource(R.drawable.png)
            frame?.isEnabled = false
        }

        // Evento ao carregar no botão para voltar para a lista de notas
        voltarBtn.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                startActivity(Intent(this@Definicoes, ListaNotas::class.java))
                finish()
            }
        }

        // Evento ao carregar no botão para mudar o nome de utilizador
        btnNome.setOnClickListener {
            mudarNome()
        }

        // Evento ao carregar no botão para mudar a password
        btnPassword.setOnClickListener {
            mudarPassword()
        }

        // Evento ao carregar no botão para ir para a pagina acerca
        acerca.setOnClickListener {
            startActivity(Intent(this, Acerca::class.java))
            finish()
        }

        // Evento ao carregar no botão para apagar a conta
        btnApagaConta.setOnClickListener {
            apagarConta()
        }

        // Verifica se a permissão para usar a camera foi concedida
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

        // Evento ao carregar na imagem para mudar a foto de perfil e abrir a camera
        frame?.setOnLongClickListener {
            openCamera()
            true
        }

        // Evento ao carregar na imagem para mudar a foto de perfil e abrir a galeria
        frame?.setOnClickListener {
            openGallery()
        }

    }

    // Função para mudar o nome de utilizador
    private fun mudarNome(){
        // Construção do AlertDialog usando padrão Builder - this referencia o contexto
        AlertDialog.Builder(this)
            // Título
            .setTitle("Mudar nome de utilizador")
            // Mensagem
            .setMessage("Tem a certeza que quer mudar o nome de utilizador?")
            // Cria e prepara o botão para responder ao click
            .setPositiveButton("Sim", DialogInterface.OnClickListener { dialog, id ->
                UtilizadorManager.setUserName(nomeUtilizador.text.toString())
                nomePerfil.text = UtilizadorManager.buscarUserName().toString();
            })
            // Cria e prepara o botão para responder ao click
            .setNegativeButton("Não", DialogInterface.OnClickListener { dialog, id ->
                dialog.cancel()})
            // Faz a construção do AlertDialog com todas as configurações
            .create()
            // Exibe
            .show()
    }

    // Função para mudar a password
    private fun mudarPassword(){
        if(api.internetConectada(this)) {
            // Construção do AlertDialog usando padrão Builder - this referencia o contexto
            AlertDialog.Builder(this)
                // Título
                .setTitle("Mudar password")
                // Mensagem
                .setMessage("Tem a certeza que quer mudar a password?")
                // Cria e prepara o botão para responder ao click
                .setPositiveButton("Sim", DialogInterface.OnClickListener { dialog, id ->
                    api.atualizarUtilizadorAPI(
                        TokenManager.buscarToken().toString(),
                        UtilizadorManager.buscarID().toString(),
                        utilizadorEmail,
                        passwordUtilizador.text.toString(),
                        this
                    )
                })
                // Cria e prepara o botão para responder ao click
                .setNegativeButton("Não", DialogInterface.OnClickListener { dialog, id ->
                    dialog.cancel()
                })
                // Faz a construção do AlertDialog com todas as configurações
                .create()
                // Exibe
                .show()
        } else {
            AlertDialog.Builder(this)
                // Título
                .setTitle("Sem Conexão")
                // Mensagem
                .setMessage("Não tem conexão á internet, logo não pode mudar a password.")
                // Cria e prepara o botão para responder ao click
                .setPositiveButton("OK", DialogInterface.OnClickListener { dialog, id ->
                    dialog.cancel()
                })
                // Faz a construção do AlertDialog com todas as configurações
                .create()
                // Exibe
                .show()
        }
    }

    // Função para apagar a conta
    private fun apagarConta(){
        if(api.internetConectada(this)) {
            // Construção do AlertDialog usando padrão Builder - this referencia o contexto
            AlertDialog.Builder(this)
                // Título
                .setTitle("Apagar conta")
                // Mensagem
                .setMessage("Tem a certeza que quer apagar a sua conta?")
                // Cria e prepara o botão para responder ao click
                .setPositiveButton("Sim", DialogInterface.OnClickListener { dialog, id ->
                    notas.addAll(sp.getNotas())
                    sp.apagarTudo(notas)
                    sync.sync(this)
                    api.apagarUtilizadorAPI(
                        TokenManager.buscarToken().toString(),
                        UtilizadorManager.buscarID().toString(),
                        this
                    )
                    UtilizadorManager.apagarUtilizador()
                    UtilizadorManager.apagarUserName()
                    UtilizadorManager.apagarImagemPerfil()
                    TokenManager.apagarToken()
                    sp.marcarFlag("buscar", true)
                    sp.marcarFlag("logado", false)
                    startActivity(Intent(this@Definicoes, PaginaInicial::class.java))
                    finish()
                })
                // Cria e prepara o botão para responder ao click
                .setNegativeButton(
                    "Cancelar",
                    DialogInterface.OnClickListener { dialog, id -> dialog.cancel() })
                // Faz a construção do AlertDialog com todas as configurações
                .create()
                // Exibe
                .show()
        } else {
            AlertDialog.Builder(this)
                // Título
                .setTitle("Sem Conexão")
                // Mensagem
                .setMessage("Não tem conexão á internet, logo não pode apagar a conta.")
                // Cria e prepara o botão para responder ao click
                .setPositiveButton("OK", DialogInterface.OnClickListener { dialog, id ->
                    dialog.cancel()
                })
                // Faz a construção do AlertDialog com todas as configurações
                .create()
                // Exibe
                .show()
        }
    }
    private fun openGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(galleryIntent, RESULT_LOAD_IMAGE)
    }
    private fun openCamera() {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "New Picture")
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera")
        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        startActivityForResult(cameraIntent, IMAGE_CAPTURE_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_CAPTURE_CODE && resultCode == Activity.RESULT_OK) {
            val bitmap = uriToBitmap(imageUri!!)
            frame?.setImageBitmap(bitmap)
            // Obtém o nome do arquivo da Uri
            val imageName = getFileName(imageUri!!)
        }
        if (requestCode == RESULT_LOAD_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            imageUri = data.data
            val bitmap = uriToBitmap(imageUri!!)
            frame?.setImageBitmap(bitmap)
            // Obtém o nome do arquivo da Uri
            val imageName = getFileName(imageUri!!)
        }
        UtilizadorManager.setImagemPerfil(imageUri.toString())
    }


    @SuppressLint("Range")
    fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
            } finally {
                cursor!!.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result!!.lastIndexOf('/')
            if (cut != -1) {
                result = result.substring(cut + 1)
            }
        }
        return result
    }

    private fun uriToBitmap(selectedFileUri: Uri): Bitmap? {
        try {
            val parcelFileDescriptor = contentResolver.openFileDescriptor(selectedFileUri, "r")
            val fileDescriptor: FileDescriptor = parcelFileDescriptor!!.fileDescriptor
            val image = BitmapFactory.decodeFileDescriptor(fileDescriptor)
            parcelFileDescriptor.close()
            return image
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }
}
