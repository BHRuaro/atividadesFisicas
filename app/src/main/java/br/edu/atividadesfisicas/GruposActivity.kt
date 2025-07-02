package br.edu.atividadesfisicas

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import br.edu.atividadesfisicas.conviteGrupo.SolicitacoesPendentesActivity
import br.edu.atividadesfisicas.conviteGrupo.StatusConvite
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class GruposActivity : AppCompatActivity() {

    private lateinit var btnVerificarConvites: Button
    private var convitesListener: ListenerRegistration? = null

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

        // Adicionar listener para contar convites pendentes
        setupConvitesListener()
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

    private fun mostrarInfoPontuacao() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
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

    override fun onResume() {
        super.onResume()
        setupConvitesListener() // Reconfigurar o listener quando voltar à tela
    }

    override fun onDestroy() {
        super.onDestroy()
        convitesListener?.remove() // Limpar o listener
    }
}