package br.edu.atividadesfisicas

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query

class RankingActivity : AppCompatActivity(){

    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: RankingAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutManager: LinearLayoutManager

    private var lastVisible: DocumentSnapshot? = null
    private var isLoading = false

    companion object {
        private const val INITIAL_LOAD_SIZE = 30L
        private const val PAGE_SIZE = 15L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = Firebase.firestore
        setContentView(R.layout.activity_ranking)

         recyclerView = findViewById(R.id.recyclerViewUsers)
        layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager
        setupScrollListener() // Configura o listener de rolagem
        fetchInitialUsers()
    }

    private fun fetchInitialUsers() {
        isLoading = true
        db.collection("usuarios")
            .orderBy("pontuacao", Query.Direction.DESCENDING)
            .limit(INITIAL_LOAD_SIZE)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val userList = documents.toObjects(PerfilUsuario::class.java)

                    lastVisible = documents.documents[documents.size() - 1]

                    adapter = RankingAdapter(userList.toMutableList())
                    recyclerView.adapter = adapter
                }
                isLoading = false
            }
            .addOnFailureListener { e ->
                Log.w("RankingActivity", "Erro na busca inicial", e)
                isLoading = false
            }
    }

    private fun fetchMoreUsers() {
        if (isLoading) return

        if (lastVisible == null) {
            Log.d("RankingActivity", "Chegou ao fim do ranking.")
            return
        }

        isLoading = true
        Log.d("RankingActivity", "Buscando mais usuários...")

        val query = db.collection("usuarios")
            .orderBy("pontuacao", Query.Direction.DESCENDING)
            .startAfter(lastVisible!!)
            .limit(PAGE_SIZE)

        query.get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val newUsers = documents.toObjects(PerfilUsuario::class.java)
                    adapter.addUsers(newUsers) // Adiciona os novos usuários ao adapter
                    lastVisible = documents.documents[documents.size() - 1]
                } else {
                    lastVisible = null
                    Log.d("RankingActivity", "Não há mais usuários para carregar.")
                }
                isLoading = false
            }
            .addOnFailureListener { e ->
                Log.w("RankingActivity", "Erro ao buscar mais usuários", e)
                isLoading = false
            }
    }

    private fun setupScrollListener() {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (dy > 0) {
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                    // Condição para carregar mais:
                    // Se os itens visíveis + a posição do primeiro item visível >= total de itens
                    // e não estiver carregando no momento.
                    if (!isLoading && (visibleItemCount + firstVisibleItemPosition) >= totalItemCount && firstVisibleItemPosition >= 0) {
                        fetchMoreUsers()
                    }
                }
            }
        })
    }
}