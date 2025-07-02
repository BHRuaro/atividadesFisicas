data class Insignia(
    val id: String,
    val nome: String,
    val descricao: String,
    val iconeRes: Int,
    val cor: String,
    val raridade: RaridadeInsignia
)

enum class RaridadeInsignia(val cor: String) {
    BRONZE("#CD7F32"),
    PRATA("#C0C0C0"),
    OURO("#FFD700"),
    PLATINA("#E5E4E2"),
    DIAMANTE("#B9F2FF")
}

data class RecompensaDiaria(
    val dia: Int,
    val tipo: TipoRecompensa,
    val quantidade: Int,
    val resgatada: Boolean = false
)

sealed class TipoRecompensa {
    object Pontos : TipoRecompensa()
    object XP : TipoRecompensa()
    data class Insignia(val insigniaId: String) : TipoRecompensa()
    data class Conquista(val conquistaId: String) : TipoRecompensa()
}

// Sistema de recompensas diárias
object SistemaRecompensasDiarias {
    fun obterRecompensasDiarias(): List<RecompensaDiaria> = listOf(
        RecompensaDiaria(1, TipoRecompensa.Pontos, 100),
        RecompensaDiaria(2, TipoRecompensa.XP, 50),
        RecompensaDiaria(3, TipoRecompensa.Pontos, 150),
        RecompensaDiaria(4, TipoRecompensa.Insignia("madrugador"), 1),
        RecompensaDiaria(5, TipoRecompensa.Pontos, 200),
        RecompensaDiaria(6, TipoRecompensa.XP, 100),
        RecompensaDiaria(7, TipoRecompensa.Insignia("guerreiro_semanal"), 1) // Recompensa especial
    )
}