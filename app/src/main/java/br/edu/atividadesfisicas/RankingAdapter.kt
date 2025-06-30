package br.edu.atividadesfisicas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RankingAdapter(private val userList: MutableList<PerfilUsuario>) : RecyclerView.Adapter<RankingAdapter.UserViewHolder>() {

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val positionTextView: TextView = itemView.findViewById(R.id.textViewPosition)
        val nameTextView: TextView = itemView.findViewById(R.id.textViewName)
        val emailTextView: TextView = itemView.findViewById(R.id.textViewEmail)
        val scoreTextView: TextView = itemView.findViewById(R.id.textViewScore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_ranking, parent, false)
        return UserViewHolder(view)
    }


    override fun getItemCount(): Int {
        return userList.size
    }


    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val currentUser = userList[position]
        val rankingPosition = position + 1

        holder.positionTextView.text = "$rankingPosition." // Formata como "1.", "2.", etc.
        holder.nameTextView.text = currentUser.nome
        holder.emailTextView.text = currentUser.email
        holder.scoreTextView.text = currentUser.pontuacao.toString()
    }
    fun addUsers(newUsers: List<PerfilUsuario>) {
        val startPosition = userList.size
        userList.addAll(newUsers)
        notifyItemRangeInserted(startPosition, newUsers.size)
    }
}