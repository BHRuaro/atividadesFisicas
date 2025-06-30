package br.edu.atividadesfisicas

import com.google.firebase.Timestamp

data class PerfilUsuario(
    var uid: String = "",
    var nome: String = "",
    var email: String = "",
    var pontuacao: Int = 0,
    var dataCadastro: Timestamp? = null
) {
    constructor() : this("", "", "", 0, null)
}
