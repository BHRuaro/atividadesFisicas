package br.edu.atividadesfisicas.conviteGrupo

import com.google.firebase.Timestamp

enum class StatusConvite {
    PENDENTE, ACEITO, RECUSADO
}

data class ConviteGrupo(
    var id: String = "",
    val grupoId: String = "",
    val nomeGrupo: String = "",
    val descricaoGrupo: String = "",
    val remetenteUid: String = "",
    val remetenteNome: String = "",
    val destinatarioUid: String = "",
    val status: StatusConvite = StatusConvite.PENDENTE,
    val criadoEm: Timestamp = Timestamp.now()
) {
    
    constructor() : this("", "", "", "", "", "", "", StatusConvite.PENDENTE, Timestamp.now())
}
