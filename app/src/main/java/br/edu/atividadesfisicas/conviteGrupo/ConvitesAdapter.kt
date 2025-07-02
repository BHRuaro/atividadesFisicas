package br.edu.atividadesfisicas.conviteGrupo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.edu.atividadesfisicas.R

class ConvitesAdapter(
    private val convites: MutableList<ConviteGrupo>,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<ConvitesAdapter.ConviteViewHolder>() {

    interface OnItemClickListener {
        fun onAceitarClick(convite: ConviteGrupo)
        fun onRecusarClick(convite: ConviteGrupo)
    }

    inner class ConviteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nomeGrupo: TextView = itemView.findViewById(R.id.tvNomeGrupo)
        val nomeRemetente: TextView = itemView.findViewById(R.id.tvRemetente)
        val btnAceitar: Button = itemView.findViewById(R.id.btnAceitar)
        val btnRecusar: Button = itemView.findViewById(R.id.btnRecusar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConviteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_convite, parent, false)
        return ConviteViewHolder(view)
    }

    override fun getItemCount() = convites.size

    override fun onBindViewHolder(holder: ConviteViewHolder, position: Int) {
        val convite = convites[position]
        holder.nomeGrupo.text = convite.nomeGrupo
        holder.nomeRemetente.text = "de: ${convite.remetenteNome}"

        holder.btnAceitar.setOnClickListener {
            holder.btnAceitar.isEnabled = false
            holder.btnRecusar.isEnabled = false

            listener.onAceitarClick(convite)
        }

        holder.btnRecusar.setOnClickListener {
            holder.btnAceitar.isEnabled = false
            holder.btnRecusar.isEnabled = false

            listener.onRecusarClick(convite)
        }
    }
}

