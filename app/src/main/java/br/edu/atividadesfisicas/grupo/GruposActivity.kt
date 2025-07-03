package br.edu.atividadesfisicas.grupo

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.edu.atividadesfisicas.R
import br.edu.atividadesfisicas.conviteGrupo.SolicitacoesPendentesActivity
import br.edu.atividadesfisicas.conviteGrupo.StatusConvite
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class GruposActivity : AppCompatActivity() {

    private lateinit var btnVerificarConvites: Button
    private lateinit var rvGrupos: RecyclerView
    private lateinit var etSearchGroup: EditText
    private lateinit var tvNoGroups: TextView
    private lateinit var gruposAdapter: GruposAdapter
    private lateinit var badgeContainer: FrameLayout
    private lateinit var tvBadgeNumber: TextView

    private var convitesListener: ListenerRegistration? = null
    private var gruposListener: ListenerRegistration? = null
    private var allGroups: List<Grupo> = emptyList()

    companion object {
        private const val TAG = "GruposActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            setContentView(R.layout.activity_grupos)
            initializeViews()
            setupRecyclerView()
            setupSearchListener()
            setupConvitesListener()
            setupGruposListener()
        } catch (e: Exception) {
            handleLayoutError(e)
        }
    }

    private fun initializeViews() {
        btnVerificarConvites = findViewById(R.id.btnConvites) 
        rvGrupos = findViewById(R.id.rvGrupos)
        etSearchGroup = findViewById(R.id.etSearchGroup)
        tvNoGroups = findViewById(R.id.tvNoGroups)
        badgeContainer = findViewById(R.id.badgeContainer)
        tvBadgeNumber = findViewById(R.id.tvBadgeNumber)

        btnVerificarConvites.setOnClickListener {
            val intent = Intent(this, SolicitacoesPendentesActivity::class.java)
            startActivity(intent)
        }

        val ivQuestion = findViewById<ImageView>(R.id.ivQuestion)
        ivQuestion.setOnClickListener {
            mostrarInfoPontuacao()
        }
    }

    private fun setupRecyclerView() {
        gruposAdapter = GruposAdapter(mutableListOf()) { grupo ->
            abrirDetalhesGrupo(grupo)
        }
        rvGrupos.adapter = gruposAdapter
        rvGrupos.layoutManager = LinearLayoutManager(this)
    }

    private fun abrirDetalhesGrupo(grupo: Grupo) {
        val intent = Intent(this, GrupoDetailActivity::class.java)
        intent.putExtra("EXTRA_GROUP_ID", grupo.id)
        startActivity(intent)
    }

    private fun setupSearchListener() {
        etSearchGroup.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filtrarGrupos(s?.toString() ?: "")
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
        } else {
            rvGrupos.visibility = View.VISIBLE
            tvNoGroups.visibility = View.GONE
        }
        gruposAdapter.updateGrupos(grupos)
    }

    private fun setupConvitesListener() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val db = Firebase.firestore

        Log.d(TAG, "Configurando listener de convites para usuário: ${currentUser.uid}")

        convitesListener = db.collection("convites")
            .whereEqualTo("destinatarioUid", currentUser.uid)
            .whereEqualTo("status", StatusConvite.PENDENTE.name)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Erro ao escutar convites", error)
                    return@addSnapshotListener
                }

                val count = snapshot?.size() ?: 0
                Log.d(TAG, "Número de convites pendentes encontrados: $count")
                
                if (snapshot != null) {
                    Log.d(TAG, "Documentos de convites:")
                    for (document in snapshot.documents) {
                        Log.d(TAG, "Convite: ${document.id}")
                        Log.d(TAG, "Status: ${document.getString("status")}")
                        Log.d(TAG, "uidConvidado: ${document.getString("uidConvidado")}")
                        Log.d(TAG, "destinatarioUid: ${document.getString("destinatarioUid")}")
                    }
                }
            
                updateBadge(count)
            }
    }

    private fun updateBadge(count: Int) {
        Log.d(TAG, "Atualizando badge com count: $count")
        
        runOnUiThread {
            if (count > 0) {
                badgeContainer.visibility = View.VISIBLE
                tvBadgeNumber.text = if (count > 99) "99+" else count.toString()
                Log.d(TAG, "Badge visível com texto: ${tvBadgeNumber.text}")
            } else {
                badgeContainer.visibility = View.GONE
                Log.d(TAG, "Badge oculto")
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
                    Log.e(TAG, "Erro ao escutar grupos", error)
                    return@addSnapshotListener
                }

                val grupos = snapshot?.documents?.mapNotNull { document ->
                    document.toObject(Grupo::class.java)?.copy(id = document.id)
                } ?: emptyList()

                allGroups = grupos
                updateUI(grupos)
            }
    }

    private fun mostrarInfoPontuacao() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Como os grupos funcionam?")
        builder.setMessage(
            "Nos grupos você pode:\n\n" +
                    "• Ver o ranking dos membros\n" +
                    "• Competir com amigos\n" +
                    "• Acompanhar o progresso de todos\n" +
                    "• Editar nome e descrição do grupo\n\n" +
                    "Crie grupos ou aguarde convites! 🏆"
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

    private fun handleLayoutError(e: Exception) {
        Log.e(TAG, "Erro ao inflar layout", e)
        Toast.makeText(this, "Erro ao carregar a tela. Tente novamente.", Toast.LENGTH_LONG).show()
        finish()
    }

    override fun onResume() {
        super.onResume()
        if (convitesListener == null) {
            setupConvitesListener()
        }
        if (gruposListener == null) {
            setupGruposListener()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        convitesListener?.remove()
        gruposListener?.remove()
    }
}