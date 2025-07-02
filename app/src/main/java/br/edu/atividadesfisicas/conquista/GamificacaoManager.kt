class GerenciadorGamificacao {
    private val processadorConquistas = ProcessadorConquistas()
    private val gerenciadorDesafios = GerenciadorDesafios()
    private val sistemaRecompensas = SistemaRecompensas()
    
    suspend fun processarAtualizacaoPassos(idUsuario: String, novosPassos: Int, totalPassos: Int) {
        // Verificar conquistas
        val conquistasDesbloqueadas = processadorConquistas.verificarConquistas(idUsuario, novosPassos, totalPassos)
        
        // Atualizar XP
        val xpGanho = RecompensasXP.obterXPPorPassos(novosPassos)
        atualizarXPUsuario(idUsuario, xpGanho)
        
        // Verificar desafios
        gerenciadorDesafios.atualizarProgressoDesafio(idUsuario, novosPassos)
        
        // Processar recompensas
        conquistasDesbloqueadas.forEach { conquista ->
            sistemaRecompensas.concederRecompensa(idUsuario, conquista.pontosRecompensa, conquista.titulo)
        }
        
        // Notificar sobre novas conquistas
        if (conquistasDesbloqueadas.isNotEmpty()) {
            mostrarNotificacaoConquista(conquistasDesbloqueadas)
        }
    }
    
    private fun mostrarNotificacaoConquista(conquistas: List<Conquista>) {
        conquistas.forEach { conquista ->
            AjudanteNotificacao.mostrarConquistaDesbloqueada(conquista)
        }
    }
}