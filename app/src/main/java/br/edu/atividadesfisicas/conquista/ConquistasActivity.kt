package br.edu.atividadesfisicas.conquista

import AdaptadorConquistas
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import br.edu.atividadesfisicas.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ConquistasActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var adaptador: AdaptadorConquistas
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var tvEmptyState: TextView
    private lateinit var tvEstatisticas: TextView
    
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val conquistasRepository = ConquistasRepository()
    
    private var conquistas = mutableListOf<ConquistaUsuario>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conquistas)
        
        supportActionBar?.title = "Conquistas"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        inicializarViews()
        configurarRecyclerView()
        configurarSwipeRefresh()
        carregarConquistas()
    }
    
    private fun inicializarViews() {
        recyclerView = findViewById(R.id.rvConquistas)
        progressBar = findViewById(R.id.progressBarConquistas)
        swipeRefresh = findViewById(R.id.swipeRefreshConquistas)
        tvEmptyState = findViewById(R.id.tvEmptyStateConquistas)
        tvEstatisticas = findViewById(R.id.tvEstatisticasConquistas)
    }
    
    private fun configurarRecyclerView() {
        adaptador = AdaptadorConquistas { conquista ->
            mostrarDetalhesConquista(conquista)
        }
        
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ConquistasActivity)
            adapter = adaptador
        }
    }
    
    private fun configurarSwipeRefresh() {
        swipeRefresh.setOnRefreshListener {
            carregarConquistas()
        }
        swipeRefresh.setColorSchemeResources(R.color.primary_green)
    }
    
    private fun carregarConquistas() {
        val usuarioId = auth.currentUser?.uid
        if (usuarioId == null) {
            Toast.makeText(this, "Usuário não logado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        showLoading(true)
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Carregar progresso do usuário do Firestore
                val progressoUsuario = conquistasRepository.carregarProgressoUsuario(usuarioId)
                
                // Combinar com definições de conquistas
                val conquistasComProgresso = DefinicoesConquista.conquistas.map { definicao ->
                    val progressoConquista = progressoUsuario[definicao.id]
                    
                    ConquistaUsuario(
                        conquista = definicao,
                        progresso = progressoConquista?.progresso ?: 0,
                        desbloqueadaEm = progressoConquista?.desbloqueadaEm,
                        visualizada = progressoConquista?.visualizada ?: false
                    )
                }
                
                withContext(Dispatchers.Main) {
                    conquistas.clear()
                    conquistas.addAll(conquistasComProgresso)
                    
                    // Ordenar: desbloqueadas primeiro, depois por progresso
                    conquistas.sortWith(compareByDescending<ConquistaUsuario> { it.estaDesbloqueada() }
                        .thenByDescending { it.calcularProgressoPercentual() }
                        .thenBy { it.conquista.raridade.ordinal })
                    
                    adaptador.atualizarConquistas(conquistas)
                    atualizarEstatisticas()
                    showLoading(false)
                    
                    // Verificar conquistas não visualizadas
                    verificarConquistasNaoVisualizadas()
                }
                
            } catch (e: Exception) {
                Log.e("ConquistasActivity", "Erro ao carregar conquistas", e)
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(
                        this@ConquistasActivity, 
                        "Erro ao carregar conquistas: ${e.message}", 
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    private fun mostrarDetalhesConquista(conquistaUsuario: ConquistaUsuario) {
        val dialog = DetalhesConquistaDialog(this, conquistaUsuario) { acao ->
            when (acao) {
                "compartilhar" -> compartilharConquista(conquistaUsuario)
                "marcar_visualizada" -> marcarComoVisualizada(conquistaUsuario)
            }
        }
        dialog.show()
    }
    
    private fun compartilharConquista(conquistaUsuario: ConquistaUsuario) {
        val conquista = conquistaUsuario.conquista
        val texto = if (conquistaUsuario.estaDesbloqueada()) {
            "🎉 Acabei de desbloquear a conquista '${conquista.titulo}' no app Atividades Físicas! " +
            "${conquista.descricao} (+${conquista.pontosRecompensa} pontos)"
        } else {
            "🎯 Estou trabalhando na conquista '${conquista.titulo}' no app Atividades Físicas! " +
            "Progresso: ${conquistaUsuario.calcularProgressoPercentual()}% - ${conquista.descricao}"
        }
        
        val intent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            putExtra(android.content.Intent.EXTRA_TEXT, texto)
            type = "text/plain"
        }
        
        startActivity(android.content.Intent.createChooser(intent, "Compartilhar conquista"))
    }
    
    private fun marcarComoVisualizada(conquistaUsuario: ConquistaUsuario) {
        val usuarioId = auth.currentUser?.uid ?: return
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                conquistasRepository.marcarConquistaComoVisualizada(usuarioId, conquistaUsuario.conquista.id)
                
                withContext(Dispatchers.Main) {
                    // Atualizar na lista local
                    val index = conquistas.indexOfFirst { it.conquista.id == conquistaUsuario.conquista.id }
                    if (index != -1) {
                        conquistas[index] = conquistas[index].copy(visualizada = true)
                        adaptador.notifyItemChanged(index)
                    }
                }
            } catch (e: Exception) {
                Log.e("ConquistasActivity", "Erro ao marcar como visualizada", e)
            }
        }
    }
    
    private fun verificarConquistasNaoVisualizadas() {
        val naoVisualizadas = conquistas.filter { 
            it.estaDesbloqueada() && !it.visualizada 
        }
        
        if (naoVisualizadas.isNotEmpty()) {
            // Mostrar primeiro diálogo de conquista não visualizada
            val primeira = naoVisualizadas.first()
            val dialog = ConquistaDesbloqueadaDialog(this, primeira.conquista)
            dialog.show()
            
            // Marcar como visualizada
            marcarComoVisualizada(primeira)
        }
    }
    
    private fun atualizarEstatisticas() {
        val desbloqueadas = conquistas.count { it.estaDesbloqueada() }
        val total = conquistas.size
        val pontosTotal = conquistas.filter { it.estaDesbloqueada() }
            .sumOf { it.conquista.pontosRecompensa }
        
        val estatisticas = "Desbloqueadas: $desbloqueadas/$total • Pontos: $pontosTotal"
        tvEstatisticas.text = estatisticas
        
        // Mostrar empty state se necessário
        if (conquistas.isEmpty()) {
            tvEmptyState.visibility = View.VISIBLE
            tvEmptyState.text = "Nenhuma conquista disponível"
        } else {
            tvEmptyState.visibility = View.GONE
        }
    }
    
    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        swipeRefresh.isRefreshing = false
    }
    
    override fun onResume() {
        super.onResume()
        // Recarregar para verificar novos progressos
        carregarConquistas()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}

// Data class para conquista com progresso do usuário
data class ConquistaUsuario(
    val conquista: Conquista,
    val progresso: Int = 0,
    val desbloqueadaEm: com.google.firebase.Timestamp? = null,
    val visualizada: Boolean = false
) {
    fun estaDesbloqueada(): Boolean = desbloqueadaEm != null
    
    fun calcularProgressoPercentual(): Int {
        return if (estaDesbloqueada()) 100 
        else conquista.requisito.calcularProgressoPercentual(progresso)
    }
    
    fun getDescricaoProgresso(): String {
        return if (estaDesbloqueada()) {
            "Desbloqueada em ${android.text.format.DateFormat.format("dd/MM/yyyy", desbloqueadaEm!!.toDate())}"
        } else {
            val percentual = calcularProgressoPercentual()
            "Progresso: $percentual% ($progresso/${getMetaRequisito()})"
        }
    }
    
    private fun getMetaRequisito(): Int {
        return when (val req = conquista.requisito) {
            is RequisitoConquista.PassosTotal -> req.meta
            is RequisitoConquista.PassosDiarios -> req.diasConsecutivos
            is RequisitoConquista.Sequencia -> req.dias
            is RequisitoConquista.GruposParticipando -> req.quantidade
            is RequisitoConquista.PontosTotal -> req.pontos
            is RequisitoConquista.AtividadeHorario -> req.dias
            else -> 100
        }
    }
}