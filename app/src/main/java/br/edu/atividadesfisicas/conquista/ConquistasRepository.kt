package br.edu.atividadesfisicas.conquista

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class ConquistasRepository {
    
    private val firestore = FirebaseFirestore.getInstance()
    
    companion object {
        private const val COLLECTION_USUARIOS = "usuarios"
        private const val FIELD_CONQUISTAS = "conquistas"
        private const val FIELD_ESTATISTICAS = "estatisticas"
    }
    
    /**
     * Carrega o progresso de todas as conquistas do usuário
     */
    suspend fun carregarProgressoUsuario(usuarioId: String): Map<String, ProgressoConquista> {
        return try {
            val documento = firestore.collection(COLLECTION_USUARIOS)
                .document(usuarioId)
                .get()
                .await()
            
            val conquistas = documento.get("$FIELD_CONQUISTAS") as? Map<String, Any> ?: emptyMap()
            
            conquistas.mapValues { (_, valor) ->
                val dados = valor as Map<String, Any>
                ProgressoConquista(
                    progresso = (dados["progresso"] as? Long)?.toInt() ?: 0,
                    desbloqueadaEm = dados["desbloqueadaEm"] as? Timestamp,
                    visualizada = dados["visualizada"] as? Boolean ?: false,
                    ultimaAtualizacao = dados["ultimaAtualizacao"] as? Timestamp ?: Timestamp.now()
                )
            }
        } catch (e: Exception) {
            Log.e("ConquistasRepository", "Erro ao carregar progresso", e)
            emptyMap()
        }
    }
    
    /**
     * Atualiza o progresso de uma conquista específica
     */
    suspend fun atualizarProgressoConquista(
        usuarioId: String, 
        conquistaId: String, 
        novoProgresso: Int,
        desbloquear: Boolean = false
    ) {
        try {
            val dadosConquista = mutableMapOf<String, Any>(
                "progresso" to novoProgresso,
                "ultimaAtualizacao" to Timestamp.now()
            )
            
            if (desbloquear) {
                dadosConquista["desbloqueadaEm"] = Timestamp.now()
                dadosConquista["visualizada"] = false
            }
            
            val dados = mapOf(
                "$FIELD_CONQUISTAS.$conquistaId" to dadosConquista
            )
            
            firestore.collection(COLLECTION_USUARIOS)
                .document(usuarioId)
                .set(dados, SetOptions.merge())
                .await()
                
            Log.d("ConquistasRepository", "Progresso atualizado: $conquistaId = $novoProgresso")
            
        } catch (e: Exception) {
            Log.e("ConquistasRepository", "Erro ao atualizar progresso", e)
            throw e
        }
    }
    
    /**
     * Marca uma conquista como visualizada
     */
    suspend fun marcarConquistaComoVisualizada(usuarioId: String, conquistaId: String) {
        try {
            val dados = mapOf(
                "$FIELD_CONQUISTAS.$conquistaId.visualizada" to true
            )
            
            firestore.collection(COLLECTION_USUARIOS)
                .document(usuarioId)
                .set(dados, SetOptions.merge())
                .await()
                
        } catch (e: Exception) {
            Log.e("ConquistasRepository", "Erro ao marcar como visualizada", e)
            throw e
        }
    }
    
    /**
     * Atualiza múltiplas conquistas de uma vez
     */
    suspend fun atualizarMultiplasConquistas(
        usuarioId: String, 
        atualizacoes: Map<String, ProgressoConquista>
    ) {
        try {
            val dados = mutableMapOf<String, Any>()
            
            atualizacoes.forEach { (conquistaId, progresso) ->
                dados["$FIELD_CONQUISTAS.$conquistaId"] = mapOf(
                    "progresso" to progresso.progresso,
                    "desbloqueadaEm" to progresso.desbloqueadaEm,
                    "visualizada" to progresso.visualizada,
                    "ultimaAtualizacao" to progresso.ultimaAtualizacao
                )
            }
            
            firestore.collection(COLLECTION_USUARIOS)
                .document(usuarioId)
                .set(dados, SetOptions.merge())
                .await()
                
        } catch (e: Exception) {
            Log.e("ConquistasRepository", "Erro ao atualizar múltiplas conquistas", e)
            throw e
        }
    }
    
    /**
     * Sincroniza estatísticas do usuário
     */
    suspend fun sincronizarEstatisticas(usuarioId: String, estatisticas: EstatisticasUsuario) {
        try {
            val dados = mapOf(
                "$FIELD_ESTATISTICAS.passosTotal" to estatisticas.passosTotal,
                "$FIELD_ESTATISTICAS.distanciaTotal" to estatisticas.distanciaTotal,
                "$FIELD_ESTATISTICAS.tempoAtividadeTotal" to estatisticas.tempoAtividadeTotal,
                "$FIELD_ESTATISTICAS.diasConsecutivos" to estatisticas.diasConsecutivos,
                "$FIELD_ESTATISTICAS.groupsParticipando" to estatisticas.gruposParticipando,
                "$FIELD_ESTATISTICAS.pontosTotal" to estatisticas.pontosTotal,
                "$FIELD_ESTATISTICAS.nivel" to estatisticas.nivel,
                "$FIELD_ESTATISTICAS.ultimaAtualizacao" to Timestamp.now()
            )
            
            firestore.collection(COLLECTION_USUARIOS)
                .document(usuarioId)
                .set(dados, SetOptions.merge())
                .await()
                
        } catch (e: Exception) {
            Log.e("ConquistasRepository", "Erro ao sincronizar estatísticas", e)
            throw e
        }
    }
    
    /**
     * Carrega estatísticas do usuário
     */
    suspend fun carregarEstatisticas(usuarioId: String): EstatisticasUsuario? {
        return try {
            val documento = firestore.collection(COLLECTION_USUARIOS)
                .document(usuarioId)
                .get()
                .await()
            
            val stats = documento.get(FIELD_ESTATISTICAS) as? Map<String, Any>
            
            stats?.let {
                EstatisticasUsuario(
                    passosTotal = (it["passosTotal"] as? Long)?.toInt() ?: 0,
                    distanciaTotal = (it["distanciaTotal"] as? Double) ?: 0.0,
                    tempoAtividadeTotal = (it["tempoAtividadeTotal"] as? Long)?.toInt() ?: 0,
                    diasConsecutivos = (it["diasConsecutivos"] as? Long)?.toInt() ?: 0,
                    gruposParticipando = (it["groupsParticipando"] as? Long)?.toInt() ?: 0,
                    pontosTotal = (it["pontosTotal"] as? Long)?.toInt() ?: 0,
                    nivel = (it["nivel"] as? Long)?.toInt() ?: 1
                )
            }
        } catch (e: Exception) {
            Log.e("ConquistasRepository", "Erro ao carregar estatísticas", e)
            null
        }
    }
}

// Data classes para o repositório
data class ProgressoConquista(
    val progresso: Int = 0,
    val desbloqueadaEm: Timestamp? = null,
    val visualizada: Boolean = false,
    val ultimaAtualizacao: Timestamp = Timestamp.now()
)

data class EstatisticasUsuario(
    val passosTotal: Int = 0,
    val distanciaTotal: Double = 0.0,
    val tempoAtividadeTotal: Int = 0, // em minutos
    val diasConsecutivos: Int = 0,
    val gruposParticipando: Int = 0,
    val pontosTotal: Int = 0,
    val nivel: Int = 1
)