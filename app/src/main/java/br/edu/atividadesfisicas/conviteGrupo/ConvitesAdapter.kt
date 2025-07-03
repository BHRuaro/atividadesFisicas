package br.edu.atividadesfisicas.conviteGrupo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.edu.atividadesfisicas.R
import java.text.SimpleDateFormat
import java.util.*

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
        val descricao: TextView = itemView.findViewById(R.id.tvDescricao)
        val nomeRemetente: TextView = itemView.findViewById(R.id.tvRemetente)
        val data: TextView = itemView.findViewById(R.id.tvData)
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
        holder.descricao.text = convite.descricaoGrupo
        holder.nomeRemetente.text = "👤 Convite de: ${convite.remetenteNome}"
        
        
        val dataFormatada = formatarDataConvite(convite.criadoEm.toDate())
        holder.data.text = "📅 $dataFormatada"

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
    
    private fun formatarDataConvite(data: Date): String {
        val agora = Calendar.getInstance()
        val dataConvite = Calendar.getInstance().apply { time = data }
        
        val diferencaMillis = agora.timeInMillis - dataConvite.timeInMillis
        val diferencaMinutos = diferencaMillis / (1000 * 60)
        val diferencaHoras = diferencaMillis / (1000 * 60 * 60)
        val diferencaDias = diferencaMillis / (1000 * 60 * 60 * 24)
        
        return when {
            diferencaMinutos < 1 -> "Agora"
            diferencaMinutos < 60 -> "${diferencaMinutos}min atrás"
            diferencaHoras < 24 -> "${diferencaHoras}h atrás"
            diferencaDias < 7 -> "${diferencaDias}d atrás"
            else -> {
                val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                formato.format(data)
            }
        }
    }
}