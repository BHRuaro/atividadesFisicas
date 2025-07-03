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
import br.edu.atividadesfisicas.auth.LoginActivity
import br.edu.atividadesfisicas.auth.PerfilUsuario
import br.edu.atividadesfisicas.grupo.GruposActivity
import br.edu.atividadesfisicas.monitor.MonitorActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import ranking.RankingActivity

class MainActivity : AppCompatActivity() {

    private lateinit var btnMonitor: Button
    private lateinit var btnRanking: Button
    private lateinit var tvWelcome: TextView
    private lateinit var tvPoints: TextView
    private lateinit var tvRankingPreview: TextView

    private var pontuacaoListener: ListenerRegistration? = null
    private var rankingListener: ListenerRegistration? = null

    companion object {
        private const val TAG = "MainActivity"
        private const val USUARIO_DEFAULT = "Usuário"
        private const val RANKING_PREVIEW_LIMIT = 5L
        private val POSITION_EMOJIS = mapOf(
            1 to "🥇",
            2 to "🥈",
            3 to "🥉"
        )
        private const val DEFAULT_EMOJI = "🏆"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        setupClickListeners()
        loadUserData()
        setupListeners()
    }

    private fun initializeViews() {
        tvWelcome = findViewById(R.id.tvWelcome)
        tvPoints = findViewById(R.id.tvPoints)
        tvRankingPreview = findViewById(R.id.tvRankingPreview)
        btnRanking = findViewById(R.id.btnRanking)
        btnMonitor = findViewById(R.id.btnMonitor)
    }

    private fun setupClickListeners() {
        btnRanking.setOnClickListener { navigateToRanking() }
        btnMonitor.setOnClickListener { navigateToMonitor() }
        
        findViewById<ImageView>(R.id.ivQuestion).setOnClickListener { 
            showPointsInfo() 
        }
        
        findViewById<LinearLayout>(R.id.logoutContainer).setOnClickListener { 
            showLogoutDialog() 
        }
    }

    private fun loadUserData() {
        loadUserInfo()
        setupPontuacaoListener()
        setupRankingListener()
    }

    private fun setupListeners() {
        setupPontuacaoListener()
        setupRankingListener()
    }

    private fun navigateToRanking() {
        startActivity(Intent(this, RankingActivity::class.java))
    }

    private fun navigateToMonitor() {
        startActivity(Intent(this, MonitorActivity::class.java))
    }

    private fun setupRankingListener() {
        val db = Firebase.firestore

        rankingListener = db.collection("usuarios")
            .orderBy("pontuacao", Query.Direction.DESCENDING)
            .limit(RANKING_PREVIEW_LIMIT)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    updateRankingPreviewError()
                    return@addSnapshotListener
                }

                val topUsers = snapshot?.documents?.mapNotNull { document ->
                    document.toObject(PerfilUsuario::class.java)
                } ?: emptyList()

                runOnUiThread {
                    updateRankingPreview(topUsers)
                }
            }
    }

    private fun updateRankingPreviewError() {
        runOnUiThread {
            tvRankingPreview.text = "Erro ao carregar ranking"
        }
    }

    private fun updateRankingPreview(users: List<PerfilUsuario>) {
        if (users.isEmpty()) {
            tvRankingPreview.text = "Nenhum usuário encontrado"
            return
        }

        val rankingText = buildRankingText(users)
        tvRankingPreview.text = rankingText
    }

    private fun buildRankingText(users: List<PerfilUsuario>): String {
        return users.mapIndexed { index, user ->
            val position = index + 1
            val firstName = user.nome.split(" ").firstOrNull() ?: USUARIO_DEFAULT
            val emoji = POSITION_EMOJIS[position] ?: DEFAULT_EMOJI
            
            "$position. $firstName $emoji — ${user.pontuacao} pts"
        }.joinToString("\n")
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Sair")
            .setMessage("Tem certeza que deseja sair da sua conta?")
            .setPositiveButton("Sair") { _, _ -> performLogout() }
            .setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun performLogout() {
        LoginActivity.currentUserProfile = null
        FirebaseAuth.getInstance().signOut()
        
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun loadUserInfo() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            redirectToLogin()
            return
        }

        LoginActivity.currentUserProfile?.let { profile ->
            updateUserInterface(profile)
            return
        }

        fetchUserFromFirestore(currentUser.uid)
    }

    private fun redirectToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun fetchUserFromFirestore(uid: String) {
        val db = Firebase.firestore
        db.collection("usuarios").document(uid)
            .get()
            .addOnSuccessListener { document ->
                handleUserDocumentSuccess(document)
            }
            .addOnFailureListener {
                useDefaultUserInfo()
            }
    }

    private fun handleUserDocumentSuccess(document: com.google.firebase.firestore.DocumentSnapshot) {
        if (document.exists()) {
            val profile = document.toObject(PerfilUsuario::class.java)
            profile?.let {
                LoginActivity.currentUserProfile = it
                updateUserInterface(it)
            }
        } else {
            useDefaultUserInfo()
        }
    }

    private fun useDefaultUserInfo() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val displayName = currentUser?.displayName ?: USUARIO_DEFAULT
        val firstName = displayName.split(" ").firstOrNull() ?: USUARIO_DEFAULT
        
        tvWelcome.text = "Olá, $firstName!"
        tvPoints.text = "Sua pontuação total: 0 pts 🏆"
    }

    private fun setupPontuacaoListener() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val db = Firebase.firestore

        pontuacaoListener = db.collection("usuarios")
            .document(currentUser.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                snapshot?.let { document ->
                    handlePointsUpdate(document)
                }
            }
    }

    private fun handlePointsUpdate(document: com.google.firebase.firestore.DocumentSnapshot) {
        if (document.exists()) {
            val profile = document.toObject(PerfilUsuario::class.java)
            profile?.let {
                LoginActivity.currentUserProfile = it
                runOnUiThread {
                    tvPoints.text = "Sua pontuação total: ${it.pontuacao} pts 🏆"
                }
            }
        }
    }

    private fun updateUserInterface(profile: PerfilUsuario) {
        val firstName = profile.nome.split(" ").firstOrNull() ?: USUARIO_DEFAULT
        tvWelcome.text = "Olá, $firstName!"
        tvPoints.text = "Sua pontuação total: ${profile.pontuacao} pts 🏆"
    }

    fun verGrupos(view: View) {
        startActivity(Intent(this, GruposActivity::class.java))
    }

    private fun showPointsInfo() {
        AlertDialog.Builder(this)
            .setTitle("Como os pontos funcionam?")
            .setMessage(
                "A cada passo dado, você ganha pontos automaticamente!\n\n" +
                        "Esses pontos são contabilizados para todos os grupos que você participa " +
                        "e também somam na sua pontuação do ranking global.\n\n" +
                        "Continue se movimentando! 🏃‍♀️🔥"
            )
            .setPositiveButton("Entendi") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    override fun onResume() {
        super.onResume()
        loadUserInfo()
    }

    override fun onDestroy() {
        super.onDestroy()
        pontuacaoListener?.remove()
        rankingListener?.remove()
    }
}