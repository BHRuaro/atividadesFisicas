package br.edu.atividadesfisicas.grupo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.edu.atividadesfisicas.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class GruposActivity : AppCompatActivity(), OnGroupClickListener {


    private lateinit var adapter: GrupoListAdapter
    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grupos)

        val ivQuestion = findViewById<ImageView>(R.id.ivQuestion)
        ivQuestion.setOnClickListener {
            mostrarInfoPontuacao()
        }
        db = Firebase.firestore
        recyclerView = findViewById(R.id.rvGrupos)
        recyclerView.layoutManager = LinearLayoutManager(this)
        fetchGrupos()
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
    }
}