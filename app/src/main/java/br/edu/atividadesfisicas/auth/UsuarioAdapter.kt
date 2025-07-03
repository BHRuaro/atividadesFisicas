package br.edu.atividadesfisicas.auth

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.edu.atividadesfisicas.R

class UsuarioAdapter(
    private var listaFiltrada: List<PerfilUsuario>,
    private val usuariosSelecionados: MutableSet<String>
) : RecyclerView.Adapter<UsuarioAdapter.UsuarioViewHolder>() {

    inner class UsuarioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nome: TextView = itemView.findViewById(R.id.textViewName)
        val email: TextView = itemView.findViewById(R.id.textViewEmail)
        val checkBox: CheckBox = itemView.findViewById(R.id.checkBoxSelecionado)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsuarioViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_usuario, parent, false)
        return UsuarioViewHolder(view)
    }

    override fun getItemCount(): Int = listaFiltrada.size

    override fun onBindViewHolder(holder: UsuarioViewHolder, position: Int) {
        val usuario = listaFiltrada[position]
        holder.nome.text = usuario.nome
        holder.email.text = usuario.email

        holder.checkBox.setOnCheckedChangeListener(null) // evita reaproveitamento errado
        holder.checkBox.isChecked = usuariosSelecionados.contains(usuario.uid)

        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                usuariosSelecionados.add(usuario.uid)
            } else {
                usuariosSelecionados.remove(usuario.uid)
            }
        }
    }

    fun atualizarLista(novaLista: List<PerfilUsuario>) {
        listaFiltrada = novaLista
        notifyDataSetChanged()
    }
}
