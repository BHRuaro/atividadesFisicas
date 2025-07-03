package br.edu.atividadesfisicas.grupo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.edu.atividadesfisicas.auth.PerfilUsuario
import br.edu.atividadesfisicas.R
import ranking.RankingAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

private const val USUARIO_NAO_AUTENTICADO = "Erro: Usuário não autenticado"

private const val SAIR_DO_GRUPO = "Sair do Grupo"

class GrupoDetailActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RankingAdapter
    private lateinit var groupNameTextView: TextView
    private lateinit var groupDescriptionTextView: TextView
    private lateinit var etBuscarMembro: EditText
    private lateinit var btnSairGrupo: Button
    private lateinit var btnEditarNome: ImageView
    private lateinit var btnEditarDescricao: ImageView
    
    private var currentGroupId: String? = null
    private var currentGroup: Grupo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grupo_detail)

        db = Firebase.firestore
        recyclerView = findViewById(R.id.recyclerViewUsers)
        groupNameTextView = findViewById(R.id.tvGroupName)
        groupDescriptionTextView = findViewById(R.id.tvGroupDescription)
        etBuscarMembro = findViewById(R.id.etBuscarMembro)
        btnSairGrupo = findViewById(R.id.btnSairGrupo)
        btnEditarNome = findViewById(R.id.btnEditarNome)
        btnEditarDescricao = findViewById(R.id.btnEditarDescricao)
        
        recyclerView.layoutManager = LinearLayoutManager(this)

        currentGroupId = intent.getStringExtra("EXTRA_GROUP_ID")

        if (currentGroupId != null) {
            fetchGroupMembers(currentGroupId!!)
        } else {
            Toast.makeText(this, "Erro: ID do grupo não encontrado.", Toast.LENGTH_LONG).show()
            finish()
        }
        
        setupSearchListener()
        setupSairGrupoButton()
        setupEditButtons()
    }

    private fun setupEditButtons() {
        btnEditarNome.setOnClickListener {
            mostrarDialogoEdicaoNome()
        }
        
        btnEditarDescricao.setOnClickListener {
            mostrarDialogoEdicaoDescricao()
        }
    }

    private fun mostrarDialogoEdicaoNome() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, USUARIO_NAO_AUTENTICADO, Toast.LENGTH_SHORT).show()
            return
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Editar Nome do Grupo")
        
        val input = EditText(this)
        input.setText(currentGroup?.nome ?: "")
        input.hint = "Digite o novo nome do grupo"
        input.setSingleLine(true)
        
        builder.setView(input)
        
        builder.setPositiveButton("Salvar") { _, _ ->
            val novoNome = input.text.toString().trim()
            if (novoNome.isNotEmpty()) {
                atualizarNomeGrupo(novoNome)
            } else {
                Toast.makeText(this, "O nome não pode estar vazio", Toast.LENGTH_SHORT).show()
            }
        }
        
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }
        
        val dialog = builder.create()
        dialog.show()
        
        
        input.requestFocus()
        input.selectAll()
    }

    private fun mostrarDialogoEdicaoDescricao() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, USUARIO_NAO_AUTENTICADO, Toast.LENGTH_SHORT).show()
            return
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Editar Descrição do Grupo")
        
        val input = EditText(this)
        input.setText(currentGroup?.descricao ?: "")
        input.hint = "Digite a nova descrição do grupo"
        input.maxLines = 4
        input.minLines = 2
        
        builder.setView(input)
        
        builder.setPositiveButton("Salvar") { _, _ ->
            val novaDescricao = input.text.toString().trim()
            if (novaDescricao.isNotEmpty()) {
                atualizarDescricaoGrupo(novaDescricao)
            } else {
                Toast.makeText(this, "A descrição não pode estar vazia", Toast.LENGTH_SHORT).show()
            }
        }
        
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }
        
        val dialog = builder.create()
        dialog.show()
        
        
        input.requestFocus()
        input.selectAll()
    }

    private fun atualizarNomeGrupo(novoNome: String) {
        val groupId = currentGroupId ?: return
        
        
        val nomeOriginal = groupNameTextView.text
        groupNameTextView.text = "Atualizando..."
        
        db.collection("grupos").document(groupId)
            .update("nome", novoNome)
            .addOnSuccessListener {
                Log.d("GrupoDetailActivity", "Nome do grupo atualizado com sucesso")
                groupNameTextView.text = novoNome
                currentGroup = currentGroup?.copy(nome = novoNome)
                Toast.makeText(this, "Nome do grupo atualizado!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Log.e("GrupoDetailActivity", "Erro ao atualizar nome do grupo", exception)
                groupNameTextView.text = nomeOriginal
                Toast.makeText(this, "Erro ao atualizar nome. Tente novamente.", Toast.LENGTH_LONG).show()
            }
    }

    private fun atualizarDescricaoGrupo(novaDescricao: String) {
        val groupId = currentGroupId ?: return
        
        
        val descricaoOriginal = groupDescriptionTextView.text
        groupDescriptionTextView.text = "Atualizando..."
        
        db.collection("grupos").document(groupId)
            .update("descricao", novaDescricao)
            .addOnSuccessListener {
                Log.d("GrupoDetailActivity", "Descrição do grupo atualizada com sucesso")
                groupDescriptionTextView.text = novaDescricao
                currentGroup = currentGroup?.copy(descricao = novaDescricao)
                Toast.makeText(this, "Descrição do grupo atualizada!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Log.e("GrupoDetailActivity", "Erro ao atualizar descrição do grupo", exception)
                groupDescriptionTextView.text = descricaoOriginal
                Toast.makeText(this, "Erro ao atualizar descrição. Tente novamente.", Toast.LENGTH_LONG).show()
            }
    }

    private fun setupSearchListener() {
        etBuscarMembro.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString() ?: ""
                if (::adapter.isInitialized) {
                    adapter.filterUsers(query)
                }
            }
        })
    }

    private fun setupSairGrupoButton() {
        btnSairGrupo.setOnClickListener {
            mostrarDialogoConfirmacaoSaida()
        }
    }

    private fun mostrarDialogoConfirmacaoSaida() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, USUARIO_NAO_AUTENTICADO, Toast.LENGTH_SHORT).show()
            return
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle(SAIR_DO_GRUPO)
        builder.setMessage("Tem certeza que deseja sair do grupo \"${currentGroup?.nome}\"?\n\nVocê perderá acesso a todas as informações do grupo e precisará ser convidado novamente para participar.")
        
        builder.setPositiveButton(SAIR_DO_GRUPO) { _, _ ->
            sairDoGrupo()
        }
        
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }
        
        
        val dialog = builder.create()
        dialog.show()
        
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(
            resources.getColor(android.R.color.holo_red_dark, theme)
        )
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(
            resources.getColor(android.R.color.darker_gray, theme)
        )
    }

    private fun sairDoGrupo() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val groupId = currentGroupId
        
        if (currentUser == null || groupId == null) {
            Toast.makeText(this, "Erro interno. Tente novamente.", Toast.LENGTH_SHORT).show()
            return
        }

        
        btnSairGrupo.isEnabled = false
        btnSairGrupo.text = "Saindo..."

        
        db.collection("grupos").document(groupId)
            .update("membros", FieldValue.arrayRemove(currentUser.uid))
            .addOnSuccessListener {
                Log.d("GrupoDetailActivity", "Usuário removido do grupo com sucesso")
                
                Toast.makeText(this, "Você saiu do grupo com sucesso", Toast.LENGTH_SHORT).show()
                
                
                verificarGrupoVazio(groupId)
                
                
                finish()
            }
            .addOnFailureListener { exception ->
                Log.e("GrupoDetailActivity", "Erro ao sair do grupo", exception)
                
                
                btnSairGrupo.isEnabled = true
                btnSairGrupo.text = SAIR_DO_GRUPO
                
                Toast.makeText(this, "Erro ao sair do grupo. Tente novamente.", Toast.LENGTH_LONG).show()
            }
    }

    private fun verificarGrupoVazio(groupId: String) {
        db.collection("grupos").document(groupId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val membros = document["membros"] as? List<String> ?: emptyList()
                    
                    if (membros.isEmpty()) {
                        
                        Log.d("GrupoDetailActivity", "Grupo $groupId está vazio, removendo...")
                        removerGrupoVazio(groupId)
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("GrupoDetailActivity", "Erro ao verificar se grupo está vazio", exception)
            }
    }

    private fun removerGrupoVazio(groupId: String) {
        db.collection("grupos").document(groupId)
            .delete()
            .addOnSuccessListener {
                Log.d("GrupoDetailActivity", "Grupo vazio removido com sucesso")
            }
            .addOnFailureListener { exception ->
                Log.e("GrupoDetailActivity", "Erro ao remover grupo vazio", exception)
            }
    }

    private fun fetchGroupMembers(groupId: String) {
        db.collection("grupos").document(groupId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val group = document.toObject(Grupo::class.java)?.copy(id = document.id)
                    currentGroup = group
                    
                    groupNameTextView.text = group?.nome
                    groupDescriptionTextView.text = group?.descricao

                    val memberUids = document["membros"] as? List<String>

                    if (!memberUids.isNullOrEmpty()) {
                        fetchMembersRanking(memberUids)
                        
                        
                        val currentUser = FirebaseAuth.getInstance().currentUser
                        if (currentUser != null && memberUids.contains(currentUser.uid)) {
                            btnSairGrupo.visibility = android.view.View.VISIBLE
                            
                            btnEditarNome.visibility = android.view.View.VISIBLE
                            btnEditarDescricao.visibility = android.view.View.VISIBLE
                        } else {
                            btnSairGrupo.visibility = android.view.View.GONE
                            btnEditarNome.visibility = android.view.View.GONE
                            btnEditarDescricao.visibility = android.view.View.GONE
                        }
                    } else {
                        Log.d("GroupDetailActivity", "Este grupo não tem membros.")
                        adapter = RankingAdapter(mutableListOf())
                        recyclerView.adapter = adapter
                        btnSairGrupo.visibility = android.view.View.GONE
                        btnEditarNome.visibility = android.view.View.GONE
                        btnEditarDescricao.visibility = android.view.View.GONE
                    }
                } else {
                    Toast.makeText(this, "Grupo não encontrado", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { exception ->
                Log.e("GroupDetailActivity", "Erro ao buscar detalhes do grupo", exception)
                Toast.makeText(this, "Erro ao carregar grupo", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun fetchMembersRanking(memberUids: List<String>) {
        db.collection("usuarios")
            .whereIn(FieldPath.documentId(), memberUids)
            .orderBy("pontuacao", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val userList = documents.toObjects(PerfilUsuario::class.java)
                adapter = RankingAdapter(userList.toMutableList())
                recyclerView.adapter = adapter
            }
            .addOnFailureListener { exception ->
                Log.e("GroupDetailActivity", "Erro ao buscar ranking dos membros", exception)
                Toast.makeText(this, "Erro ao carregar membros do grupo", Toast.LENGTH_SHORT).show()
            }
    }
}