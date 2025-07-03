package ranking

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.edu.atividadesfisicas.auth.PerfilUsuario
import br.edu.atividadesfisicas.R
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class RankingActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: RankingAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var etBuscar: EditText

    private val userList = mutableListOf<PerfilUsuario>()
    private val originalUserList = mutableListOf<PerfilUsuario>()

    private var lastVisible: DocumentSnapshot? = null
    private var isLoading = false

    companion object {
        private const val TAG = "RankingActivity"
        private const val INITIAL_LOAD_SIZE = 30L
        private const val PAGE_SIZE = 15L
        private const val LOAD_MORE_THRESHOLD = 5
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ranking)

        initializeFirestore()
        initializeViews()
        setupRecyclerView()
        setupScrollListener()
        setupSearchListener()
        fetchInitialUsers()
    }

    private fun initializeFirestore() {
        db = Firebase.firestore
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.recyclerViewUsers)
        etBuscar = findViewById(R.id.etBuscarRanking)
        layoutManager = LinearLayoutManager(this)
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = layoutManager
        adapter = RankingAdapter(userList)
        recyclerView.adapter = adapter
    }

    private fun setupSearchListener() {
        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                val searchText = s?.toString()?.trim() ?: ""
                adapter.filterUsers(searchText)
            }
        })
    }

    private fun fetchInitialUsers() {
        if (isLoading) return
        
        isLoading = true
        db.collection("usuarios")
            .orderBy("pontuacao", Query.Direction.DESCENDING)
            .limit(INITIAL_LOAD_SIZE)
            .get()
            .addOnSuccessListener { documents ->
                handleInitialUsersSuccess(documents)
            }
            .addOnFailureListener { exception ->
                handleUsersError("Erro na busca inicial", exception)
            }
    }

    private fun handleInitialUsersSuccess(documents: QuerySnapshot) {
        val newUsers = documents.toObjects(PerfilUsuario::class.java)
        
        clearUserLists()
        addUsersToLists(newUsers)
        adapter.updateUserList(newUsers)
        
        lastVisible = documents.documents.lastOrNull()
        isLoading = false
        
        Log.d(TAG, "Carregados ${newUsers.size} usuários iniciais")
    }

    private fun clearUserLists() {
        userList.clear()
        originalUserList.clear()
    }

    private fun addUsersToLists(users: List<PerfilUsuario>) {
        userList.addAll(users)
        originalUserList.addAll(users)
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
                handleMoreUsersSuccess(documents)
            }
            .addOnFailureListener { exception ->
                handleUsersError("Erro ao buscar mais usuários", exception)
            }
    }

    private fun handleMoreUsersSuccess(documents: QuerySnapshot) {
        val newUsers = documents.toObjects(PerfilUsuario::class.java)
        
        if (newUsers.isNotEmpty()) {
            originalUserList.addAll(newUsers)
            adapter.addUsers(newUsers)
            lastVisible = documents.documents.lastOrNull()
            Log.d(TAG, "Carregados mais ${newUsers.size} usuários")
        } else {
            Log.d(TAG, "Não há mais usuários para carregar")
            lastVisible = null
        }
        
        isLoading = false
    }

    private fun handleUsersError(message: String, exception: Exception) {
        Log.e(TAG, message, exception)
        isLoading = false
    }

    private fun setupScrollListener() {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (shouldLoadMoreUsers(dy)) {
                    fetchMoreUsers()
                }
            }
        })
    }

    private fun shouldLoadMoreUsers(dy: Int): Boolean {
        if (dy <= 0 || isLoading) return false
        
        val visibleCount = layoutManager.childCount
        val totalCount = layoutManager.itemCount
        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        
        return (visibleCount + firstVisible) >= totalCount - LOAD_MORE_THRESHOLD
    }
}