package br.edu.atividadesfisicas.grupo

import com.google.firebase.Timestamp

data class Grupo(
    val nome: String = "",
    val descricao: String = "",
    val membros: List<String> = listOf(),
    val criadoEm: Timestamp = Timestamp.now(),
    val criadoPorUid: String = ""
)
{
    constructor() : this("", "", listOf(), Timestamp.now(), "")
}