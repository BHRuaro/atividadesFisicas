package br.edu.atividadesfisicas.conviteGrupo

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.edu.atividadesfisicas.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class SolicitacoesPendentesActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ConvitesAdapter
    private lateinit var tvEmptyState: TextView
    private val listaConvites = mutableListOf<ConviteGrupo>()
    private var firestoreListener: ListenerRegistration? = null

    companion object {
        private const val TAG = "SolicitacoesPendentes"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_solicitacoes_pendentes)

        db = Firebase.firestore
        tvEmptyState = findViewById(R.id.tvEmptyState)
        setupRecyclerView()
        carregarConvitesPendentes()


        findViewById<ImageView>(R.id.ivQuestion).setOnClickListener {
            mostrarInfoConvites()
        }
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.rvConvites)

        val itemClickListener = object : ConvitesAdapter.OnItemClickListener {
            override fun onAceitarClick(convite: ConviteGrupo) {
                Log.d(TAG, "Aceitar convite clicado para: ${convite.nomeGrupo}")
                aceitarConvite(convite)
            }

            override fun onRecusarClick(convite: ConviteGrupo) {
                Log.d(TAG, "Recusar convite clicado para: ${convite.nomeGrupo}")
                recusarConvite(convite)
            }
        }

        adapter = ConvitesAdapter(listaConvites, itemClickListener)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        updateEmptyState()
    }

    private fun carregarConvitesPendentes() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.w(TAG, "Usuário não está logado")
            return
        }

        Log.d(TAG, "Carregando convites para usuário: ${currentUser.uid}")


        firestoreListener?.remove()


        firestoreListener = db.collection("convites")
            .whereEqualTo("destinatarioUid", currentUser.uid)
            .whereEqualTo("status", StatusConvite.PENDENTE.name)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Erro ao escutar mudanças nos convites", e)
                    return@addSnapshotListener
                }

                Log.d(TAG, "Recebida atualização do Firestore. Documentos: ${snapshot?.size()}")

                val novosConvites = mutableListOf<ConviteGrupo>()
                snapshot?.documents?.forEach { doc ->
                    try {
                        val convite = doc.toObject(ConviteGrupo::class.java)
                        convite?.let {
                            it.id = doc.id
                            novosConvites.add(it)
                            Log.d(TAG, "Convite carregado: ${it.nomeGrupo} - Status: ${it.status}")
                        }
                    } catch (ex: Exception) {
                        Log.e(TAG, "Erro ao converter documento em ConviteGrupo", ex)
                    }
                }


                novosConvites.sortByDescending { it.criadoEm }


                listaConvites.clear()
                listaConvites.addAll(novosConvites)

                Log.d(TAG, "Lista atualizada com ${listaConvites.size} convites")

                runOnUiThread {
                    updateEmptyState()
                    adapter.notifyDataSetChanged()
                }
            }
    }

    private fun updateEmptyState() {
        if (listaConvites.isEmpty()) {
            recyclerView.visibility = View.GONE
            tvEmptyState.visibility = View.VISIBLE
            Log.d(TAG, "Mostrando estado vazio")
        } else {
            recyclerView.visibility = View.VISIBLE
            tvEmptyState.visibility = View.GONE
            Log.d(TAG, "Mostrando lista com ${listaConvites.size} itens")
        }
    }

    private fun aceitarConvite(convite: ConviteGrupo) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        Log.d(TAG, "Iniciando processo de aceitar convite: ${convite.id}")


        val position = listaConvites.indexOf(convite)
        if (position != -1) {
            listaConvites.removeAt(position)
            adapter.notifyItemRemoved(position)
            updateEmptyState()
        }

        db.runTransaction { transaction ->

            val conviteRef = db.collection("convites").document(convite.id)
            transaction.update(conviteRef, "status", StatusConvite.ACEITO.name)


            val grupoRef = db.collection("grupos").document(convite.grupoId)
            transaction.update(
                grupoRef,
                "membros",
                com.google.firebase.firestore.FieldValue.arrayUnion(currentUser.uid)
            )
        }.addOnSuccessListener {
            Log.d(TAG, "Convite aceito com sucesso: ${convite.id}")
            Toast.makeText(
                this,
                "Convite aceito! Você agora faz parte do grupo.",
                Toast.LENGTH_SHORT
            ).show()
        }.addOnFailureListener { exception ->
            Log.e(TAG, "Erro ao aceitar convite: ${convite.id}", exception)
            Toast.makeText(
                this,
                "Erro ao aceitar convite: ${exception.message}",
                Toast.LENGTH_SHORT
            ).show()


            if (position != -1) {
                listaConvites.add(position, convite)
                adapter.notifyItemInserted(position)
                updateEmptyState()
            }
        }
    }

    private fun recusarConvite(convite: ConviteGrupo) {
        Log.d(TAG, "Iniciando processo de recusar convite: ${convite.id}")


        val position = listaConvites.indexOf(convite)
        if (position != -1) {
            listaConvites.removeAt(position)
            adapter.notifyItemRemoved(position)
            updateEmptyState()
        }

        db.collection("convites").document(convite.id)
            .update("status", StatusConvite.RECUSADO.name)
            .addOnSuccessListener {
                Log.d(TAG, "Convite recusado com sucesso: ${convite.id}")
                Toast.makeText(this, "Convite recusado", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Erro ao recusar convite: ${convite.id}", exception)
                Toast.makeText(
                    this,
                    "Erro ao recusar convite: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()


                if (position != -1) {
                    listaConvites.add(position, convite)
                    adapter.notifyItemInserted(position)
                    updateEmptyState()
                }
            }
    }

    private fun mostrarInfoConvites() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Como funcionam os convites?")
        builder.setMessage(
            "Quando alguém te convida para um grupo, você recebe uma notificação!\n\n" +
                    "Aqui você pode aceitar ou recusar os convites pendentes.\n\n" +
                    "Ao aceitar, você fará parte do grupo e seus pontos contarão para o ranking do grupo! 🏃‍♀️🔥"
        )
        builder.setPositiveButton("Entendi") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        firestoreListener?.remove()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "Activity resumida, recarregando dados")
        carregarConvitesPendentes()
    }
}