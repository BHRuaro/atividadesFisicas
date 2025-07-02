package br.edu.atividadesfisicas.grupo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.edu.atividadesfisicas.R

class GruposAdapter(
    private var grupos: MutableList<Grupo> = mutableListOf(),
    private val onGrupoClick: (Grupo) -> Unit = {}
) : RecyclerView.Adapter<GruposAdapter.GrupoViewHolder>() {

    class GrupoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNomeGrupo: TextView = itemView.findViewById(R.id.tvNomeGrupo)
        val tvDescricaoGrupo: TextView = itemView.findViewById(R.id.tvDescricaoGrupo)
        val tvMembrosCount: TextView = itemView.findViewById(R.id.tvMembrosCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GrupoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_grupo, parent, false)
        return GrupoViewHolder(view)
    }

    override fun onBindViewHolder(holder: GrupoViewHolder, position: Int) {
        val grupo = grupos[position]

        holder.tvNomeGrupo.text = grupo.nome
        holder.tvDescricaoGrupo.text = grupo.descricao
        holder.tvMembrosCount.text = "${grupo.membros.size} membros"

        holder.itemView.setOnClickListener {
            onGrupoClick(grupo)
        }
    }

    override fun getItemCount(): Int = grupos.size

    fun updateGrupos(novosGrupos: List<Grupo>) {
        grupos.clear()
        grupos.addAll(novosGrupos)
        notifyDataSetChanged()
    }
}
