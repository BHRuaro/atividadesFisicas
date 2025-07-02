data class NivelUsuario(
    val nivelAtual: Int,
    val xpAtual: Int,
    val xpProximoNivel: Int,
    val xpTotal: Int
) {
    val progresso: Float = xpAtual.toFloat() / xpProximoNivel.toFloat()
    
    companion object {
        fun calcularNivel(xpTotal: Int): NivelUsuario {
            var nivel = 1
            var xpNecessario = 1000 // XP base para nível 2
            var xpAcumulado = 0
            
            while (xpTotal >= xpAcumulado + xpNecessario) {
                xpAcumulado += xpNecessario
                nivel++
                xpNecessario = (xpNecessario * 1.2).toInt() // 20% de aumento por nível
            }
            
            val xpAtual = xpTotal - xpAcumulado
            val xpProximoNivel = xpNecessario
            
            return NivelUsuario(nivel, xpAtual, xpProximoNivel, xpTotal)
        }
        
        fun obterTituloNivel(nivel: Int): String = when {
            nivel < 5 -> "Iniciante 🌱"
            nivel < 10 -> "Caminhante 🚶"
            nivel < 20 -> "Corredor 🏃"
            nivel < 35 -> "Atleta ⚡"
            nivel < 50 -> "Campeão 🏆"
            nivel < 75 -> "Lenda 🌟"
            else -> "Imortal 👑"
        }
    }
}

// Sistema de recompensas por XP
object RecompensasXP {
    fun obterXPPorPassos(passos: Int): Int = passos / 10 // 1 XP a cada 10 passos
    fun obterXPPorConquista(raridade: RaridadeConquista): Int = raridade.pontosBase / 4
    fun obterXPPorMetaDiaria(): Int = 50
    fun obterXPPorAtividadeGrupo(): Int = 25
    fun obterXPPorMelhoriaRanking(): Int = 30
}