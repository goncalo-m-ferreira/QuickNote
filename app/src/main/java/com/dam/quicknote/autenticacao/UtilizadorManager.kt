package com.dam.quicknote.autenticacao

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.dam.quicknote.models.LoginResponse
import org.json.JSONException

object UtilizadorManager {
    // Keys dos valores do utilizador presente no ficheiro
    private const val ID_KEY: String = "ID_KEY"
    private const val EMAIL_KEY: String = "EMAIL_KEY"
    private const val DATA_KEY: String = "DATA_KEY"
    private const val NAME_KEY: String = "NOMES_KEY"
    private const val IMAGEM_KEY: String = "IMAGEM_KEY"

    private lateinit var sharedPreferences: SharedPreferences

    // inicar o sharedPreferences
    fun init(context: Context){
        sharedPreferences = context.getSharedPreferences("Utilizador1", Context.MODE_PRIVATE)

    }

    // guarda o utilizador no ficheiro
    fun guardarUtilizador(id: String, email: String, data: String){
        var editor = sharedPreferences.edit()
        editor.putString(ID_KEY, id)
        editor.putString(EMAIL_KEY, email)
        editor.putString(DATA_KEY, data)
        editor.apply()
        if (buscarUserName() == null) {
            setUserName(email)
        }

    }

    // buscar o valor do EMAIL do utilizador ao ficheiro
    fun buscarID(): String? {
        return sharedPreferences.getString(ID_KEY, "")
    }

    // buscar o valor do EMAIL do utilizador ao ficheiro
    fun buscarEMAIL(): String? {
        return sharedPreferences.getString(EMAIL_KEY, "")
    }

    // buscar o valor da DATA do utilizador ao ficheiro
    fun buscarDATA(): String? {
        return sharedPreferences.getString(DATA_KEY, "")
    }

    // apaga o utilizador no ficheiro
    fun apagarUtilizador() {
        var editor = sharedPreferences.edit()
        editor.remove(ID_KEY)
        editor.remove(EMAIL_KEY)
        editor.remove(DATA_KEY)
        editor.apply()
    }

    // buscar lista de nomes ao ficheiro
    fun buscarUserNameList(): String? {
        return sharedPreferences.getString(NAME_KEY, "")
    }

    // buscar o valor da Nome do utilizador ao ficheiro
    fun buscarUserName(): String? {
        val email = buscarEMAIL().toString()
        // buscar lista de nomes ao ficheiro
        val currentUserName = buscarUserNameList()?.split(",")?.toMutableList()

        // Se o utilizador tiver um nome associado
        if (currentUserName?.contains(email) == true) {
            val index = currentUserName.indexOf(email)
            // retorna o nome do utilizador caso exista
            return if (index != -1) currentUserName.getOrNull(index + 1) else null
        }

        return null
    }

    // guardar o valor do Nome do utilizador no ficheiro
    fun setUserName(name: String) {
        val editor = sharedPreferences.edit()
        val email = buscarEMAIL().toString()
        // buscar lista de nomes ao ficheiro
        val currentUserName = buscarUserNameList()?.split(",")?.toMutableList()
        // Se o utilizador tiver um nome associado
        if (currentUserName != null) {
            val index = currentUserName.indexOf(email)
            // Se o index for diferente de -1 significa que o email existe na lista
            if (index != -1) {
                // Atualizar o nome existente
                currentUserName[index + 1] = name
            } else {
                // Adicionar um novo nome e o email do utilizador
                currentUserName.add(email)
                currentUserName.add(name)
            }

            editor.putString(NAME_KEY, currentUserName.joinToString(",")).apply()
        }
    }

    // apagar o valor do Nome do utilizador no ficheiro
    fun apagarUserName() {
        var editor = sharedPreferences.edit()
        val email = buscarEMAIL().toString()
        val lista = buscarUserNameList()?.split(",")?.toMutableList()
        if (lista != null) {
            val index = lista.indexOf(email)

            if (index != -1) {
                lista.removeAt(index)
                lista.removeAt(index+1)
            }

            editor.putString(NAME_KEY, lista.joinToString(",")).apply()
        }
    }

    // guarda o utilizador obtido da resposta
    fun getUserFromResponse(response: LoginResponse?){
        response?.let {
            try {
                val id = it.id
                val email = it.email
                val data = it.data

                guardarUtilizador(id, email, data)
                Log.e("Utilizador", "$id, $email, $data")
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
    }

    // buscar lista de imagens ao ficheiro
    fun buscarImagemPerfilList(): String? {
        return sharedPreferences.getString(IMAGEM_KEY, "")
    }

    // buscar o valor da imagem do utilizador ao ficheiro
    fun buscarImagemPerfil(): String? {
        val email = buscarEMAIL().toString()
        val currentUserName = buscarImagemPerfilList()?.split(",")?.toMutableList()
        // Se o utilizador tiver uma imagem associada
        if (currentUserName?.contains(email) == true) {
            val index = currentUserName.indexOf(email)
            // retorna a imagem do utilizador caso exista
            return if (index != -1) currentUserName.getOrNull(index + 1) else null
        }

        return null
    }

    // guardar o valor da imagem do utilizador no ficheiro
    fun setImagemPerfil(imagem: String) {
        val editor = sharedPreferences.edit()
        val email = buscarEMAIL().toString()
        // buscar lista de imagens ao ficheiro
        val currentUserName = buscarImagemPerfilList()?.split(",")?.toMutableList()

        // Se o utilizador tiver uma imagem
        if (currentUserName != null) {
            val index = currentUserName.indexOf(email)

            if (index != -1) {
                // Atualizar a imagem existente
                currentUserName[index + 1] = imagem
            } else {
                // Adicionar uma nova imagem para o email do utilizador
                currentUserName.add(email)
                currentUserName.add(imagem)
            }

            editor.putString(IMAGEM_KEY, currentUserName.joinToString(",")).apply()
        }
    }

    // apagar o valor da imagem do utilizador no ficheiro
    fun apagarImagemPerfil() {
        var editor = sharedPreferences.edit()
        val email = buscarEMAIL().toString()
        // buscar lista de imagens ao ficheiro
        val lista = buscarImagemPerfilList()?.split(",")?.toMutableList()
        // Se o utilizador tiver uma imagem
        if(buscarImagemPerfil() != null){
            // se a lista não for nula
            if (lista != null) {
                // buscar o index do email do utilizador
                val index = lista.indexOf(email)

                // se o index for diferente de -1 significa que o email existe na lista
                if (index != -1) {
                    // remover o email e a imagem
                    lista.removeAt(index)
                    lista.removeAt(index+1)
                }
                editor.putString(IMAGEM_KEY, lista.joinToString(",")).apply()
            }
        }
    }

}