package br.edu.atividadesfisicas.grupo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.edu.atividadesfisicas.R

interface OnGroupClickListener {
    fun onGroupClick(group: Grupo) // A função será chamada com o objeto do grupo que foi clicado
}

class GrupoListAdapter (
    private val listener: OnGroupClickListener,
    private val grupoList: List<Grupo>) : RecyclerView.Adapter<GrupoListAdapter.UserViewHolder>() {

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.textViewGroupName)
        val descriptionTextView: TextView = itemView.findViewById(R.id.textViewGroupDescription)
        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onGroupClick(grupoList[position])
                }
            }
    }
}

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_grupos, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: UserViewHolder,
        position: Int
    ) {
        val currentGroup = grupoList[position]

        holder.nameTextView.text = currentGroup.nome
        holder.descriptionTextView.text = currentGroup.descricao
    }

    override fun getItemCount(): Int {
        return grupoList.size
    }
}