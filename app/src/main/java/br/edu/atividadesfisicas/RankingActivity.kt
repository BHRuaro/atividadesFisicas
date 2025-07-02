package br.edu.atividadesfisicas

import android.os.Bundle
import android.util.Log
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class RankingActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: RankingAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var etBuscar: EditText

    private val listaUsuarios = mutableListOf<PerfilUsuario>()
    private var listaOriginal = mutableListOf<PerfilUsuario>()

    private var lastVisible: DocumentSnapshot? = null
    private var isLoading = false

    companion object {
        private const val INITIAL_LOAD_SIZE = 30L
        private const val PAGE_SIZE = 15L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ranking)

        db = Firebase.firestore

        recyclerView = findViewById(R.id.recyclerViewUsers)
        etBuscar = findViewById(R.id.etBuscarRanking)

        layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager

        adapter = RankingAdapter(listaUsuarios)
        recyclerView.adapter = adapter

        setupScrollListener()
        fetchInitialUsers()

        etBuscar.addTextChangedListener {
            val texto = it.toString()
            filtrarUsuarios(texto)
        }
    }

    private fun fetchInitialUsers() {
        isLoading = true
        db.collection("usuarios")
            .orderBy("pontuacao", Query.Direction.DESCENDING)
            .limit(INITIAL_LOAD_SIZE)
            .get()
            .addOnSuccessListener { documents ->
                val userList = documents.toObjects(PerfilUsuario::class.java)
                listaUsuarios.clear()
                listaUsuarios.addAll(userList)
                listaOriginal = userList.toMutableList()
                adapter.notifyDataSetChanged()
                lastVisible = documents.documents.lastOrNull()
                isLoading = false
            }
            .addOnFailureListener { e ->
                Log.e("RankingActivity", "Erro na busca inicial", e)
                isLoading = false
            }
    }

    private fun fetchMoreUsers() {
        if (isLoading || lastVisible == null) return

        isLoading = true
        db.collection("usuarios")
            .orderBy("pontuacao", Query.Direction.DESCENDING)
            .startAfter(lastVisible!!)
            .limit(PAGE_SIZE)
            .get()
            .addOnSuccessListener { documents ->
                val novos = documents.toObjects(PerfilUsuario::class.java)
                listaUsuarios.addAll(novos)
                listaOriginal.addAll(novos)
                adapter.notifyItemRangeInserted(listaUsuarios.size - novos.size, novos.size)
                lastVisible = documents.documents.lastOrNull()
                isLoading = false
            }
            .addOnFailureListener { e ->
                Log.e("RankingActivity", "Erro ao buscar mais usuários", e)
                isLoading = false
            }
    }

    private fun filtrarUsuarios(texto: String) {
        val filtrados = if (texto.isBlank()) {
            listaOriginal
        } else {
            listaOriginal.filter {
                it.nome.contains(texto, ignoreCase = true) ||
                        it.email.contains(texto, ignoreCase = true)
            }
        }

        listaUsuarios.clear()
        listaUsuarios.addAll(filtrados)
        adapter.notifyDataSetChanged()
    }

    private fun setupScrollListener() {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0 && !isLoading) {
                    val visibleCount = layoutManager.childCount
                    val totalCount = layoutManager.itemCount
                    val firstVisible = layoutManager.findFirstVisibleItemPosition()
                    if ((visibleCount + firstVisible) >= totalCount) {
                        fetchMoreUsers()
                    }
                }
            }
        })
    }
}