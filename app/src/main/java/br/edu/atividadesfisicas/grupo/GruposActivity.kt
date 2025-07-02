package br.edu.atividadesfisicas.grupo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
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
    private lateinit var gruposAdapter: GruposAdapter

    private var convitesListener: ListenerRegistration? = null
    private var gruposListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grupos)

        val ivQuestion = findViewById<ImageView>(R.id.ivQuestion)
        ivQuestion.setOnClickListener {
            mostrarInfoPontuacao()
        }

        btnVerificarConvites = findViewById<Button>(R.id.btnConvites)
        btnVerificarConvites.setOnClickListener {
            val intent = Intent(this, SolicitacoesPendentesActivity::class.java)
            startActivity(intent)
        }

        rvGrupos = findViewById<RecyclerView>(R.id.rvGrupos)
        setupRecyclerView()

        setupConvitesListener()
        setupGruposListener()
    }

    private fun setupRecyclerView() {
        gruposAdapter = GruposAdapter { grupo ->
            onGroupClick(grupo)
        }

        rvGrupos.apply {
            layoutManager = LinearLayoutManager(this@GruposActivity)
            adapter = gruposAdapter
        }
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
                    gruposAdapter.updateGrupos(grupos)
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

    fun onGroupClick(group: Grupo) {
        val intent = Intent(this, GrupoDetailActivity::class.java)
        intent.putExtra("EXTRA_GROUP_ID", group.id)
        startActivity(intent)
    }

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
