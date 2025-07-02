package br.edu.atividadesfisicas

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import androidx.appcompat.app.AlertDialog
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import androidx.core.widget.addTextChangedListener
import br.edu.atividadesfisicas.conviteGrupo.ConviteGrupo
import br.edu.atividadesfisicas.conviteGrupo.StatusConvite
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth


class CriarGruposActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: UsuarioAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutManager: LinearLayoutManager

    private val listaUsuarios = mutableListOf<PerfilUsuario>()
    private val usuariosSelecionados = mutableSetOf<String>()

    private var lastVisible: DocumentSnapshot? = null
    private var isLoading = false

    companion object {
        private const val INITIAL_LOAD_SIZE = 30L
        private const val PAGE_SIZE = 15L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_criar_grupos)
        db = Firebase.firestore

        recyclerView = findViewById(R.id.rvUsuarios)
        layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager

        adapter = UsuarioAdapter(listaUsuarios, usuariosSelecionados)
        recyclerView.adapter = adapter

        setupScrollListener()
        fetchInitialUsers()

        findViewById<EditText>(R.id.etBuscarUsuario).addTextChangedListener {
            filtrarUsuarios(it.toString())
        }

        findViewById<Button>(R.id.btnSalvar).setOnClickListener {
            salvarGrupo()
        }

        val ivQuestion = findViewById<ImageView>(R.id.ivQuestion)
        ivQuestion.setOnClickListener { mostrarInfoPontuacao() }
    }

    private fun fetchInitialUsers() {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        
        isLoading = true
        db.collection("usuarios")
            .orderBy("pontuacao", Query.Direction.DESCENDING)
            .limit(INITIAL_LOAD_SIZE)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    listaUsuarios.clear()
                    val usuarios = result.toObjects(PerfilUsuario::class.java)
                        .filter { it.uid != currentUserUid } // Filtra o usuário atual
                    listaUsuarios.addAll(usuarios)
                    lastVisible = result.documents[result.size() - 1]
                    adapter.notifyDataSetChanged()
                }
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    private fun fetchMoreUsers() {
        if (isLoading || lastVisible == null) return
        
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        isLoading = true
        db.collection("usuarios")
            .orderBy("pontuacao", Query.Direction.DESCENDING)
            .startAfter(lastVisible!!)
            .limit(PAGE_SIZE)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    val novosUsuarios = result.toObjects(PerfilUsuario::class.java)
                        .filter { it.uid != currentUserUid } // Filtra o usuário atual
                    listaUsuarios.addAll(novosUsuarios)
                    adapter.notifyItemRangeInserted(
                        listaUsuarios.size - novosUsuarios.size,
                        novosUsuarios.size
                    )
                    lastVisible = result.documents[result.size() - 1]
                } else {
                    lastVisible = null
                }
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    private fun setupScrollListener() {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(rv, dx, dy)
                if (dy > 0 && !isLoading && layoutManager.findLastVisibleItemPosition() >= listaUsuarios.size - 3) {
                    fetchMoreUsers()
                }
            }
        })
    }

    private fun filtrarUsuarios(texto: String) {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        
        val filtrados = listaUsuarios.filter {
            it.uid != currentUserUid && // Sempre filtra o usuário atual
            (it.nome.contains(texto, ignoreCase = true) || it.email.contains(texto, ignoreCase = true))
        }
        adapter.atualizarLista(filtrados)
    }

    private fun salvarGrupo() {
        val nomeGrupo = findViewById<EditText>(R.id.etNomeGrupo).text.toString().trim()
        val descricaoGrupo = findViewById<EditText>(R.id.etDescricaoGrupo).text.toString().trim()

        if (nomeGrupo.isEmpty() || usuariosSelecionados.isEmpty()) {
            Toast.makeText(this, "Preencha o nome e selecione membros", Toast.LENGTH_SHORT).show()
            return
        }

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Toast.makeText(this, "Usuário não autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        val grupo = Grupo(
            nome = nomeGrupo,
            descricao = descricaoGrupo,
            membros = listOf(user.uid),
            criadoEm = Timestamp.now(),
            criadoPorUid = user.uid
        )

        db.collection("grupos")
            .add(grupo)
            .addOnSuccessListener { grupoRef ->
                enviarConvites(grupoRef.id, nomeGrupo, descricaoGrupo, user.uid)
                Toast.makeText(this, "Grupo criado com sucesso!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao salvar grupo", Toast.LENGTH_SHORT).show()
            }
    }

    private fun mostrarInfoPontuacao() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Como os pontos funcionam?")
        builder.setMessage(
            "A cada passo dado, você ganha pontos automaticamente!\n\n" +
                    "Esses pontos contam para todos os grupos que você participa " +
                    "e para o ranking global.\n\n" +
                    "Continue se movimentando! 🏃‍♀️🔥"
        )
        builder.setPositiveButton("Entendi") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    private fun enviarConvites(
        grupoId: String,
        nomeGrupo: String,
        descricaoGrupo: String,
        remetenteUid: String
    ) {
        db.collection("usuarios").document(remetenteUid).get()
            .addOnSuccessListener { remetenteDoc ->
                val remetenteNome = remetenteDoc.getString("nome") ?: "Usuário"
                usuariosSelecionados.forEach { destinatarioUid ->
                    val convite = ConviteGrupo(
                        grupoId = grupoId,
                        nomeGrupo = nomeGrupo,
                        descricaoGrupo = descricaoGrupo,
                        remetenteUid = remetenteUid,
                        remetenteNome = remetenteNome,
                        destinatarioUid = destinatarioUid,
                        status = StatusConvite.PENDENTE
                    )

                    db.collection("convites")
                        .add(convite)
                }
            }
    }
}