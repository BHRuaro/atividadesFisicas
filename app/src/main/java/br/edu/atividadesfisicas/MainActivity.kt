package br.edu.atividadesfisicas

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import br.edu.atividadesfisicas.grupo.GruposActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

private const val USUARIO = "Usuário"

class MainActivity : AppCompatActivity() {

    private lateinit var btnMonitor : Button;
    private lateinit var btnRanking: Button
    private lateinit var tvWelcome: TextView
    private lateinit var tvPoints: TextView

    private var pontuacaoListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar views
        tvWelcome = findViewById(R.id.tvWelcome)
        tvPoints = findViewById(R.id.tvPoints)

        btnRanking = findViewById(R.id.btnRanking)
        btnRanking.setOnClickListener {
            verRanking()
        }

        val ivQuestion = findViewById<ImageView>(R.id.ivQuestion)
        ivQuestion.setOnClickListener {
            mostrarInfoPontuacao()
        }

        btnMonitor = findViewById(R.id.btnMonitor);

        btnMonitor.setOnClickListener {
            val intent = Intent(this@MainActivity, MonitorActivity::class.java)
            startActivity(intent)
        };

        // Carregar informações do usuário
        carregarInfoUsuario()
        setupPontuacaoListener()
    }

    private fun carregarInfoUsuario() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            // Primeiro, verifica se já temos o perfil carregado no LoginActivity
            LoginActivity.currentUserProfile?.let { perfil ->
                atualizarInterface(perfil)
                return
            }

            // Se não tiver, busca no Firestore
            val db = Firebase.firestore
            db.collection("usuarios").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val perfil = document.toObject(PerfilUsuario::class.java)
                        perfil?.let {
                            // Atualiza o perfil no companion object para uso futuro
                            LoginActivity.currentUserProfile = it
                            atualizarInterface(it)
                        }
                    } else {
                        // Se não encontrar o documento, usa o nome do Firebase Auth
                        val nomeDisplay = currentUser.displayName ?: USUARIO
                        val primeiroNome = nomeDisplay.split(" ").firstOrNull() ?: USUARIO
                        tvWelcome.text = "Olá, $primeiroNome!"
                        tvPoints.text = "Sua pontuação total: 0 pts 🏆"
                    }
                }
                .addOnFailureListener {
                    // Em caso de erro, usa o nome do Firebase Auth
                    val nomeDisplay = currentUser.displayName ?: USUARIO
                    val primeiroNome = nomeDisplay.split(" ").firstOrNull() ?: USUARIO
                    tvWelcome.text = "Olá, $primeiroNome!"
                    tvPoints.text = "Sua pontuação total: 0 pts 🏆"
                }
        } else {
            // Usuário não logado, redirecionar para login
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun setupPontuacaoListener() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val db = Firebase.firestore

        // Listener reativo para a pontuação do usuário
        pontuacaoListener = db.collection("usuarios")
            .document(currentUser.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                snapshot?.let { document ->
                    if (document.exists()) {
                        val perfil = document.toObject(PerfilUsuario::class.java)
                        perfil?.let {
                            // Atualiza o perfil armazenado
                            LoginActivity.currentUserProfile = it
                            // Atualiza apenas a pontuação na interface
                            runOnUiThread {
                                tvPoints.text = "Sua pontuação total: ${it.pontuacao} pts 🏆"
                            }
                        }
                    }
                }
            }
    }

    private fun atualizarInterface(perfil: PerfilUsuario) {
        val primeiroNome = perfil.nome.split(" ").firstOrNull() ?: USUARIO
        tvWelcome.text = "Olá, $primeiroNome!"
        tvPoints.text = "Sua pontuação total: ${perfil.pontuacao} pts 🏆"
    }

    fun verRanking() {
        val intent = Intent(this, RankingActivity ::class.java)
        startActivity(intent)

        btnMonitor = findViewById(R.id.btnMonitor);

        btnMonitor.setOnClickListener {
            val intent = Intent(this@MainActivity, MonitorActivity::class.java)
            startActivity(intent)
        };
    }


    fun verGrupos(view: View) {
        val intent = Intent(this, GruposActivity::class.java)
        startActivity(intent)
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

    override fun onResume() {
        super.onResume()
        // Recarregar as informações do usuário quando voltar para a tela
        carregarInfoUsuario()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Limpar o listener para evitar vazamentos de memória
        pontuacaoListener?.remove()
    }
}
