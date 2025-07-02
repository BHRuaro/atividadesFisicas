package br.edu.atividadesfisicas.grupo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.edu.atividadesfisicas.R
import com.google.firebase.firestore.FirebaseFirestore
import br.edu.atividadesfisicas.conviteGrupo.SolicitacoesPendentesActivity
import br.edu.atividadesfisicas.conviteGrupo.StatusConvite
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class GruposActivity : AppCompatActivity(), OnGroupClickListener {

    private lateinit var btnVerificarConvites: Button
    private lateinit var rvGrupos: RecyclerView
    private lateinit var etSearchGroup: EditText
    private lateinit var tvNoGroups: TextView
    private lateinit var gruposAdapter: GruposAdapter

    private lateinit var adapter: GrupoListAdapter
    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private var convitesListener: ListenerRegistration? = null
    private var gruposListener: ListenerRegistration? = null
    private var allGroups: List<Grupo> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grupos)

        // Inicializar views
        etSearchGroup = findViewById(R.id.etSearchGroup)
        tvNoGroups = findViewById(R.id.tvNoGroups)

        val ivQuestion = findViewById<ImageView>(R.id.ivQuestion)
        ivQuestion.setOnClickListener {
            mostrarInfoPontuacao()
        }
        db = Firebase.firestore
        recyclerView = findViewById(R.id.rvGrupos)
        recyclerView.layoutManager = LinearLayoutManager(this)
        fetchGrupos()

        btnVerificarConvites = findViewById<Button>(R.id.btnConvites)
        btnVerificarConvites.setOnClickListener {
            val intent = Intent(this, SolicitacoesPendentesActivity::class.java)
            startActivity(intent)
        }

        rvGrupos = findViewById<RecyclerView>(R.id.rvGrupos)
        setupRecyclerView()
        setupSearchListener()

        setupConvitesListener()
        setupGruposListener()
    }

    private fun setupRecyclerView() {
        gruposAdapter = GruposAdapter { _ ->
            // Implementar acesso a tela de detalhe do grupo
        }

        rvGrupos.apply {
            layoutManager = LinearLayoutManager(this@GruposActivity)
            adapter = gruposAdapter
        }
    }

    private fun setupSearchListener() {
        etSearchGroup.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val searchText = s.toString().trim()
                filtrarGrupos(searchText)
            }
        })
    }

    private fun filtrarGrupos(searchText: String) {
        val gruposFiltrados = if (searchText.isEmpty()) {
            allGroups
        } else {
            allGroups.filter { grupo ->
                grupo.nome.contains(searchText, ignoreCase = true) ||
                        grupo.descricao.contains(searchText, ignoreCase = true)
            }
        }

        updateUI(gruposFiltrados)
    }

    private fun updateUI(grupos: List<Grupo>) {
        if (grupos.isEmpty()) {
            rvGrupos.visibility = View.GONE
            tvNoGroups.visibility = View.VISIBLE

            // Personalizar mensagem baseada no texto de busca
            val searchText = etSearchGroup.text.toString().trim()
            tvNoGroups.text = if (searchText.isEmpty()) {
                "Você ainda não participa de nenhum grupo 😔\n\nCrie um novo grupo ou aguarde convites!"
            } else {
                "Nenhum grupo encontrado para \"$searchText\" 😔\n\nTente buscar por um nome diferente ou crie um novo grupo!"
            }
        } else {
            rvGrupos.visibility = View.VISIBLE
            tvNoGroups.visibility = View.GONE
        }

        gruposAdapter.updateGrupos(grupos)
    }

    private fun setupConvitesListener() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val db = Firebase.firestore

        convitesListener = db.collection("convites")
            .whereEqualTo("destinatarioUid", currentUser.uid)
            .whereEqualTo("status", StatusConvite.PENDENTE.name)
            .addSnapshotListener { snapshot, _ ->
                val count = snapshot?.size() ?: 0
                runOnUiThread {
                    btnVerificarConvites.text = if (count > 0) {
                        "Verificar Convites 🔴 $count"
                    } else {
                        "Verificar Convites"
                    }
                }
            }
    }

    private fun setupGruposListener() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val db = Firebase.firestore

        gruposListener = db.collection("grupos")
            .whereArrayContains("membros", currentUser.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Tratar erro se necessário
                    return@addSnapshotListener
                }

                val grupos = snapshot?.documents?.mapNotNull { document ->
                    document.toObject(Grupo::class.java)
                } ?: emptyList()

                runOnUiThread {
                    allGroups = grupos
                    // Aplicar filtro atual
                    val searchText = etSearchGroup.text.toString().trim()
                    filtrarGrupos(searchText)
                }
            }
    }

    private fun mostrarInfoPontuacao() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Como os pontos funcionam?")
        builder.setMessage(
            "A cada passo dado, você ganha pontos automaticamente!\n\n" +
                    "Esses pontos são contabilizados para todos os grupos que você participa " +
                    "e também somam na sua pontuação do ranking global.\n\n" +
                    "Continue se movimentando! 🏃‍♀️🔥"
        )
        builder.setPositiveButton("Entendi") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    fun criarGrupo(view: View) {
        val intent = Intent(this, CriarGruposActivity::class.java)
        startActivity(intent)
    }
    private fun fetchGrupos() {
        db.collection("grupos")
            .get()
            .addOnSuccessListener { documents ->
                val grupoList = mutableListOf<Grupo>()
                for (document in documents) {
                    val grupo = document.toObject(Grupo::class.java)
                    grupoList.add(grupo)
                }
                if (grupoList.isNotEmpty()) {
                    adapter = GrupoListAdapter(this ,grupoList)
                    recyclerView.adapter = adapter
                }

            }
            .addOnFailureListener { e ->
                Log.w("RankingActivity", "Erro na busca inicial", e)

            }

    }

    override fun onGroupClick(group: Grupo) {
        val intent = Intent(this, GrupoDetailActivity::class.java)
        intent.putExtra("EXTRA_GROUP_ID", group.id)
        startActivity(intent)

    override fun onResume() {
        super.onResume()
        setupConvitesListener() // Reconfigurar o listener quando voltar à tela
        setupGruposListener() // Reconfigurar o listener dos grupos
    }

    override fun onDestroy() {
        super.onDestroy()
        convitesListener?.remove() // Limpar o listener
        gruposListener?.remove() // Limpar o listener dos grupos
    }
}