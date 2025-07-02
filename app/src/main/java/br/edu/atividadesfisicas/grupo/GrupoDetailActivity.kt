package br.edu.atividadesfisicas.grupo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.edu.atividadesfisicas.PerfilUsuario
import br.edu.atividadesfisicas.R
import br.edu.atividadesfisicas.RankingAdapter
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class GrupoDetailActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RankingAdapter
    private lateinit var groupNameTextView: TextView
    private lateinit var groupDescriptionTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grupo_detail)

        db = Firebase.firestore
        recyclerView = findViewById(R.id.recyclerViewUsers)
        groupNameTextView = findViewById(R.id.tvGroupName)
        groupDescriptionTextView = findViewById(R.id.tvGroupDescription)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val groupId = intent.getStringExtra("EXTRA_GROUP_ID")

        if (groupId != null) {
            fetchGroupMembers(groupId)
        } else {
            Toast.makeText(this, "Erro: ID do grupo não encontrado.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun fetchGroupMembers(groupId: String) {
        db.collection("grupos").document(groupId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val group = document.toObject(Grupo::class.java)
                    groupNameTextView.text = group?.nome
                    groupDescriptionTextView.text = group?.descricao

                    val memberUids = document.get("membros") as? List<String>

                    if (memberUids != null && memberUids.isNotEmpty()) {
                        fetchMembersRanking(memberUids)
                    } else {

                        Log.d("GroupDetailActivity", "Este grupo não tem membros.")
                        recyclerView.adapter = RankingAdapter(mutableListOf())
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("GroupDetailActivity", "Erro ao buscar detalhes do grupo", exception)
            }
    }

    // ETAPA 2: Buscar os perfis dos usuários membros e ordenar por pontuação
    private fun fetchMembersRanking(memberUids: List<String>) {
        db.collection("usuarios")

            .whereIn(FieldPath.documentId(), memberUids)
            .orderBy("pontuacao", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val userList = documents.toObjects(PerfilUsuario::class.java)
                adapter = RankingAdapter(userList.toMutableList())
                recyclerView.adapter = adapter
            }
            .addOnFailureListener { exception ->
                Log.e("GroupDetailActivity", "Erro ao buscar ranking dos membros", exception)
            }
    }
}