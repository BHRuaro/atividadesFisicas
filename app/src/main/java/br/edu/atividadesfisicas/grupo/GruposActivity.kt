package br.edu.atividadesfisicas.grupo

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
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

    private var convitesListener: ListenerRegistration? = null
    private var gruposListener: ListenerRegistration? = null
    private var allGroups: List<Grupo> = emptyList()

    companion object {
        private const val TAG = "GruposActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            Log.d(TAG, "Iniciando onCreate")
            setContentView(R.layout.activity_grupos)
            Log.d(TAG, "Layout carregado com sucesso")
            
            initializeViews()
            setupRecyclerView()
            setupSearchListener()
            setupConvitesListener()
            setupGruposListener()
            
            Log.d(TAG, "onCreate finalizado com sucesso")
        } catch (e: Exception) {
            Log.e(TAG, "Erro no onCreate: ${e.message}", e)
            handleLayoutError(e)
        }
    }

    private fun initializeViews() {
        try {
            Log.d(TAG, "Inicializando views")
            
            // Inicializar views com verificação de existência
            etSearchGroup = findViewById<EditText>(R.id.etSearchGroup).apply {
                Log.d(TAG, "EditText etSearchGroup inicializado")
            }
            
            tvNoGroups = findViewById<TextView>(R.id.tvNoGroups).apply {
                Log.d(TAG, "TextView tvNoGroups inicializado")
            }

            val ivQuestion = findViewById<ImageView>(R.id.ivQuestion)
            ivQuestion?.setOnClickListener {
                mostrarInfoPontuacao()
            } ?: Log.w(TAG, "ImageView ivQuestion não encontrado")

            btnVerificarConvites = findViewById<Button>(R.id.btnConvites).apply {
                setOnClickListener {
                    val intent = Intent(this@GruposActivity, SolicitacoesPendentesActivity::class.java)
                    startActivity(intent)
                }
                Log.d(TAG, "Button btnConvites inicializado")
            }

            rvGrupos = findViewById<RecyclerView>(R.id.rvGrupos).apply {
                Log.d(TAG, "RecyclerView rvGrupos inicializado")
            }
            
            Log.d(TAG, "Todas as views inicializadas com sucesso")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao inicializar views: ${e.message}", e)
            throw e
        }
    }

    private fun setupRecyclerView() {
        try {
            Log.d(TAG, "Configurando RecyclerView")
            
            gruposAdapter = GruposAdapter { grupo ->
                Log.d(TAG, "Clique no grupo: ${grupo.nome}")
                abrirDetalhesGrupo(grupo)
            }

            rvGrupos.apply {
                layoutManager = LinearLayoutManager(this@GruposActivity)
                adapter = gruposAdapter
            }
            
            Log.d(TAG, "RecyclerView configurado com sucesso")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao configurar RecyclerView: ${e.message}", e)
            Toast.makeText(this, "Erro ao configurar lista de grupos", Toast.LENGTH_SHORT).show()
        }
    }

    private fun abrirDetalhesGrupo(grupo: Grupo) {
        try {
            Log.d(TAG, "Abrindo detalhes do grupo: ${grupo.id}")
            val intent = Intent(this, GrupoDetailActivity::class.java)
            intent.putExtra("EXTRA_GROUP_ID", grupo.id)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao abrir detalhes do grupo: ${e.message}", e)
            Toast.makeText(this, "Erro ao abrir grupo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSearchListener() {
        try {
            Log.d(TAG, "Configurando listener de busca")
            
            etSearchGroup.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    val searchText = s.toString().trim()
                    Log.d(TAG, "Texto de busca alterado: '$searchText'")
                    filtrarGrupos(searchText)
                }
            })
            
            Log.d(TAG, "Listener de busca configurado com sucesso")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao configurar listener de busca: ${e.message}", e)
        }
    }

    private fun filtrarGrupos(searchText: String) {
        try {
            val gruposFiltrados = if (searchText.isEmpty()) {
                allGroups
            } else {
                allGroups.filter { grupo ->
                    grupo.nome.contains(searchText, ignoreCase = true) ||
                            grupo.descricao.contains(searchText, ignoreCase = true)
                }
            }

            Log.d(TAG, "Filtrados ${gruposFiltrados.size} grupos de ${allGroups.size} total")
            updateUI(gruposFiltrados)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao filtrar grupos: ${e.message}", e)
            updateUI(allGroups) // Fallback para mostrar todos os grupos
        }
    }

    private fun updateUI(grupos: List<Grupo>) {
        try {
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
            Log.d(TAG, "UI atualizada com ${grupos.size} grupos")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao atualizar UI: ${e.message}", e)
            Toast.makeText(this, "Erro ao atualizar lista", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupConvitesListener() {
        try {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                Log.w(TAG, "Usuário não autenticado, não configurando listener de convites")
                return
            }

            Log.d(TAG, "Configurando listener de convites para usuário: ${currentUser.uid}")
            val db = Firebase.firestore

            convitesListener = db.collection("convites")
                .whereEqualTo("destinatarioUid", currentUser.uid)
                .whereEqualTo("status", StatusConvite.PENDENTE.name)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Erro no listener de convites: ${error.message}", error)
                        return@addSnapshotListener
                    }

                    val count = snapshot?.size() ?: 0
                    Log.d(TAG, "Convites pendentes: $count")
                    
                    runOnUiThread {
                        try {
                            btnVerificarConvites.text = if (count > 0) {
                                "Verificar Convites 🔴 $count"
                            } else {
                                "Verificar Convites"
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Erro ao atualizar botão de convites: ${e.message}", e)
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao configurar listener de convites: ${e.message}", e)
        }
    }

    private fun setupGruposListener() {
        try {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                Log.w(TAG, "Usuário não autenticado, não configurando listener de grupos")
                return
            }

            Log.d(TAG, "Configurando listener de grupos para usuário: ${currentUser.uid}")
            val db = Firebase.firestore

            gruposListener = db.collection("grupos")
                .whereArrayContains("membros", currentUser.uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Erro no listener de grupos: ${error.message}", error)
                        return@addSnapshotListener
                    }

                    val grupos = snapshot?.documents?.mapNotNull { document ->
                        try {
                            // Criar uma nova instância do Grupo com o ID do documento
                            val grupoData = document.data
                            if (grupoData != null) {
                                Grupo(
                                    id = document.id,
                                    nome = grupoData["nome"] as? String ?: "",
                                    descricao = grupoData["descricao"] as? String ?: "",
                                    membros = grupoData["membros"] as? List<String> ?: listOf(),
                                    criadoEm = grupoData["criadoEm"] as? com.google.firebase.Timestamp 
                                        ?: com.google.firebase.Timestamp.now(),
                                    criadoPorUid = grupoData["criadoPorUid"] as? String ?: ""
                                )
                            } else {
                                null
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Erro ao converter documento para Grupo: ${e.message}", e)
                            null
                        }
                    } ?: emptyList()

                    Log.d(TAG, "Carregados ${grupos.size} grupos")

                    runOnUiThread {
                        try {
                            allGroups = grupos
                            // Aplicar filtro atual
                            val searchText = etSearchGroup.text.toString().trim()
                            filtrarGrupos(searchText)
                        } catch (e: Exception) {
                            Log.e(TAG, "Erro ao atualizar grupos na UI: ${e.message}", e)
                        }
                    }
                }
    } catch (e: Exception) {
        Log.e(TAG, "Erro ao configurar listener de grupos: ${e.message}", e)
    }
}

    private fun mostrarInfoPontuacao() {
        try {
            Log.d(TAG, "Mostrando informações de pontuação")
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
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao mostrar diálogo de informações: ${e.message}", e)
            Toast.makeText(this, "Erro ao exibir informações", Toast.LENGTH_SHORT).show()
        }
    }

    fun criarGrupo(view: View) {
        try {
            Log.d(TAG, "Navegando para criar grupo")
            val intent = Intent(this, CriarGruposActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao navegar para criar grupo: ${e.message}", e)
            Toast.makeText(this, "Erro ao abrir tela de criação", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleLayoutError(e: Exception) {
        Log.e(TAG, "Erro crítico no layout", e)
        
        // Tentar carregar um layout alternativo simples
        try {
            setContentView(android.R.layout.activity_list_item)
            Toast.makeText(this, "Erro ao carregar interface. Verifique os recursos.", Toast.LENGTH_LONG).show()
        } catch (fallbackError: Exception) {
            Log.e(TAG, "Erro no layout de fallback também", fallbackError)
            Toast.makeText(this, "Erro crítico na interface", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            Log.d(TAG, "onResume - reconfigurando listeners")
            setupConvitesListener()
            setupGruposListener()
        } catch (e: Exception) {
            Log.e(TAG, "Erro no onResume: ${e.message}", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            Log.d(TAG, "onDestroy - removendo listeners")
            convitesListener?.remove()
            gruposListener?.remove()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao remover listeners: ${e.message}", e)
        }
    }
}