package com.dam.listaNotas

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dam.listaNotas.R
import com.dam.listaNotas.models.Nota

class ListaNotasAdapter(private val notas: ArrayList<Nota>, private val context: Context,  private val itemClickListener: (Nota) -> Unit) :
    RecyclerView.Adapter<ListaNotasAdapter.ViewHolder>() {

    // Define o layout de cada item da lista
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val nota = notas[position]
        holder?.let {
            it.bindView(nota)
        }
    }

    // Cria um ViewHolder para cada item da lista
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.nota_item, parent, false)
        view.width
        return ViewHolder(view)
    }

    // Retorna o número de itens da lista da RecyclerView
    override fun getItemCount(): Int {
        return notas.size
    }

    // Define o ViewHolder para cada item da lista da RecyclerView e define o OnClickListener
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        // Define os componentes do layout de cada item da lista
        fun bindView(nota: Nota) {
            val title: TextView = itemView.findViewById(R.id.notaItemTitulo)
            val description: TextView = itemView.findViewById(R.id.notaItemDescricao)
            title.text = nota.titulo
            description.text = nota.descricao
        }
        init {
            // Define o OnClickListener para cada item da lista
            itemView.setOnClickListener {
                val position = adapterPosition
                // Verifica se a posição é válida
                if (position != RecyclerView.NO_POSITION) {
                    // Retorna a nota da posição clicada
                    val note = notas[position]
                    itemClickListener(note)
                }
            }
        }
    }
}