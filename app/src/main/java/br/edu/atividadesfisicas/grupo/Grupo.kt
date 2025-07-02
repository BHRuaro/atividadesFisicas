package br.edu.atividadesfisicas.grupo

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Grupo(
    @DocumentId
    val id: String = "",
    val nome: String = "",
    val descricao: String = "",
    val membros: List<String> = listOf(),
    val criadoEm: Timestamp = Timestamp.Companion.now(),
    val criadoPorUid: String = ""
)
{
    constructor() : this("","", "", listOf(), Timestamp.Companion.now(), "")
}