package ranking

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.edu.atividadesfisicas.auth.PerfilUsuario
import br.edu.atividadesfisicas.R

class RankingAdapter(private val originalUserList: MutableList<PerfilUsuario>) : RecyclerView.Adapter<RankingAdapter.UserViewHolder>() {

    // Lista que será modificada para filtros, mas mantém referência às posições originais
    private val displayedUserList = mutableListOf<PerfilUsuario>()
    
    // Mapa que guarda a posição original de cada usuário
    private val originalPositions = mutableMapOf<String, Int>()
    
    init {
        // Copia a lista original para a lista de exibição
        displayedUserList.addAll(originalUserList)
        
        // Mapeia as posições originais baseadas no ranking inicial (por pontuação)
        originalUserList.forEachIndexed { index, user ->
            originalPositions[user.uid] = index + 1
        }
    }

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
        return displayedUserList.size
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val currentUser = displayedUserList[position]
        
        // SEMPRE usa a posição original fixa do ranking inicial
        val originalPosition = originalPositions[currentUser.uid] ?: 1
        
        holder.positionTextView.text = originalPosition.toString()
        holder.nameTextView.text = currentUser.nome
        holder.emailTextView.text = currentUser.email
        holder.scoreTextView.text = currentUser.pontuacao.toString()
    }

    fun addUsers(newUsers: List<PerfilUsuario>) {
        val startPosition = originalUserList.size
        
        // Adiciona os novos usuários à lista original
        originalUserList.addAll(newUsers)
        
        // Mapeia as posições dos novos usuários
        newUsers.forEachIndexed { index, user ->
            originalPositions[user.uid] = startPosition + index + 1
        }
        
        // Adiciona à lista de exibição
        displayedUserList.addAll(newUsers)
        
        notifyItemRangeInserted(displayedUserList.size - newUsers.size, newUsers.size)
    }
    
    /**
     * Filtra os usuários mantendo sempre as posições originais do ranking
     */
    fun filterUsers(query: String) {
        displayedUserList.clear()
        
        if (query.isEmpty()) {
            // Se não há filtro, mostra todos os usuários
            displayedUserList.addAll(originalUserList)
        } else {
            // Filtra os usuários baseado no nome ou email
            val filteredUsers = originalUserList.filter { user ->
                user.nome.contains(query, ignoreCase = true) ||
                user.email.contains(query, ignoreCase = true)
            }
            displayedUserList.addAll(filteredUsers)
        }
        
        notifyDataSetChanged()
    }
    
    /**
     * Atualiza a lista completa mantendo as posições originais
     */
    fun updateUserList(newUserList: List<PerfilUsuario>) {
        originalUserList.clear()
        originalUserList.addAll(newUserList)
        
        displayedUserList.clear()
        displayedUserList.addAll(newUserList)
        
        // Remapeia as posições baseadas na nova ordem
        originalPositions.clear()
        newUserList.forEachIndexed { index, user ->
            originalPositions[user.uid] = index + 1
        }
        
        notifyDataSetChanged()
    }
}