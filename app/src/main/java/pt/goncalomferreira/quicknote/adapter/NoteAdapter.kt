package pt.goncalomferreira.quicknote.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import pt.goncalomferreira.quicknote.R
import pt.goncalomferreira.quicknote.model.Note

// Liga a lista de notas aos cartoes apresentados no RecyclerView.
class NoteAdapter : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    private var notes: List<Note> = emptyList()

    // Guarda as referencias aos elementos visuais de cada nota.
    class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.textViewNoteTitle)
        val content: TextView = itemView.findViewById(R.id.textViewNoteContent)
    }

    // Cria o layout visual de cada item da lista.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_note, parent, false)

        return NoteViewHolder(view)
    }

    // Preenche cada cartao com os dados da nota correspondente.
    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]

        holder.title.text = note.title
        holder.content.text = note.content
    }

    override fun getItemCount(): Int {
        return notes.size
    }

    // Atualiza os dados apresentados pelo RecyclerView.
    fun submitList(newNotes: List<Note>) {
        notes = newNotes
        notifyDataSetChanged()
    }
}