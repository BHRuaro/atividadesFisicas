package br.edu.atividadesfisicas

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import br.edu.atividadesfisicas.grupo.GruposActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {

    private lateinit var btnMonitor : Button
    private lateinit var btnRanking: Button
    private lateinit var tvWelcome: TextView
    private lateinit var tvPoints: TextView
    private lateinit var tvRankingPreview: TextView

    private var pontuacaoListener: ListenerRegistration? = null
    private var rankingListener: ListenerRegistration? = null

    companion object {
        private const val USUARIO = "Usuário"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar views
        tvWelcome = findViewById(R.id.tvWelcome)
        tvPoints = findViewById(R.id.tvPoints)
        tvRankingPreview = findViewById(R.id.tvRankingPreview)

        btnRanking = findViewById(R.id.btnRanking)
        btnRanking.setOnClickListener {
            verRanking()
        }

        val ivQuestion = findViewById<ImageView>(R.id.ivQuestion)
        ivQuestion.setOnClickListener {
            mostrarInfoPontuacao()
        }

        val logoutContainer = findViewById<LinearLayout>(R.id.logoutContainer)
        logoutContainer.setOnClickListener {
            mostrarDialogoLogout()
        }

        btnMonitor = findViewById(R.id.btnMonitor)

        btnMonitor.setOnClickListener {
            val intent = Intent(this@MainActivity, MonitorActivity::class.java)
            startActivity(intent)
        }

        // Carregar informações do usuário
        carregarInfoUsuario()
        setupPontuacaoListener()
        setupRankingListener()
    }

    private fun setupRankingListener() {
        val db = Firebase.firestore

        rankingListener = db.collection("usuarios")
            .orderBy("pontuacao", Query.Direction.DESCENDING)
            .limit(5)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    runOnUiThread {
                        tvRankingPreview.text = "Erro ao carregar ranking"
                    }
                    return@addSnapshotListener
                }

                val topUsuarios = snapshot?.documents?.mapNotNull { document ->
                    document.toObject(PerfilUsuario::class.java)
                } ?: emptyList()

                runOnUiThread {
                    atualizarRankingPreview(topUsuarios)
                }
            }
    }

    private fun atualizarRankingPreview(usuarios: List<PerfilUsuario>) {
        if (usuarios.isEmpty()) {
            tvRankingPreview.text = "Nenhum usuário encontrado"
            return
        }

        val rankingText = StringBuilder()
        usuarios.forEachIndexed { index, usuario ->
            val posicao = index + 1
            val primeiroNome = usuario.nome.split(" ").firstOrNull() ?: "Usuário"
            val emoji = when (posicao) {
                1 -> "🥇"
                2 -> "🥈" 
                3 -> "🥉"
                else -> "🏆"
            }
            
            rankingText.append("$posicao. $primeiroNome $emoji — ${usuario.pontuacao} pts")
            if (index < usuarios.size - 1) {
                rankingText.append("\n")
            }
        }

        tvRankingPreview.text = rankingText.toString()
    }

    private fun mostrarDialogoLogout() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Sair")
        builder.setMessage("Tem certeza que deseja sair da sua conta?")
        builder.setPositiveButton("Sair") { _, _ ->
            realizarLogout()
        }
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    private fun realizarLogout() {
        // Limpar o perfil do usuário armazenado
        LoginActivity.currentUserProfile = null

        // Fazer logout do Firebase Auth
        FirebaseAuth.getInstance().signOut()

        // Redirecionar para a tela de login
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
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
        val intent = Intent(this, RankingActivity::class.java)
        startActivity(intent)
    }

    fun verGrupos(view: View) {
        val intent = Intent(this, GruposActivity::class.java)
        startActivity(intent)
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

    override fun onResume() {
        super.onResume()
        // Recarregar as informações do usuário quando voltar para a tela
        carregarInfoUsuario()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Limpar os listeners para evitar vazamentos de memória
        pontuacaoListener?.remove()
        rankingListener?.remove()
    }
}