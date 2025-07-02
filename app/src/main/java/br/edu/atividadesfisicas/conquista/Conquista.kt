import com.google.firebase.Timestamp

data class Conquista(
    val id: String,
    val titulo: String,
    val descricao: String,
    val iconeRes: Int,
    val categoria: CategoriaConquista,
    val raridade: RaridadeConquista,
    val requisito: RequisitoConquista,
    val pontosRecompensa: Int,
    val desbloqueadaEm: Timestamp? = null,
    val progresso: Int = 0
)

enum class CategoriaConquista(val displayName: String, val descricao: String) {
    PASSOS("Passos", "Conquistas relacionadas ao número de passos"),
    CONSISTENCIA("Consistência", "Conquistas de atividade regular e constante"),
    SOCIAL("Social", "Conquistas relacionadas a grupos e interações"),
    RANKING("Ranking", "Conquistas baseadas na posição no ranking"),
    DISTANCIA("Distância", "Conquistas baseadas na distância percorrida"),
    TEMPO("Tempo", "Conquistas relacionadas ao tempo de atividade"),
    HORARIO("Horário", "Conquistas baseadas no horário da atividade"),
    PONTOS("Pontos", "Conquistas baseadas no acúmulo de pontos"),
    ESPECIAL("Especial", "Conquistas especiais e eventos únicos"),
    NIVEL("Nível", "Conquistas relacionadas ao avanço de nível"),
    CONVITES("Convites", "Conquistas por enviar convites para amigos")
}

enum class RaridadeConquista(val pontosBase: Int, val multiplicador: Float, val cor: String) {
    COMUM(100, 1.0f, "#4CAF50"),
    RARA(250, 1.5f, "#2196F3"), 
    EPICA(500, 2.0f, "#FF9800"),
    LENDARIA(1000, 3.0f, "#9C27B0"),
    MITICA(2000, 5.0f, "#F44336")
}

sealed class RequisitoConquista {
    // Requisitos relacionados a passos
    data class PassosTotal(val meta: Int) : RequisitoConquista()
    data class PassosDiarios(val meta: Int, val diasConsecutivos: Int) : RequisitoConquista()
    data class PassosUnicoDia(val meta: Int) : RequisitoConquista()
    
    // Requisitos de sequência e consistência
    data class Sequencia(val dias: Int) : RequisitoConquista()
    data class SequenciaPerfeita(val dias: Int, val metaDiaria: Int) : RequisitoConquista()
    
    // Requisitos sociais
    data class GruposParticipando(val quantidade: Int) : RequisitoConquista()
    data class ConvitesEnviados(val quantidade: Int) : RequisitoConquista()
    data class ConvitesAceitos(val quantidade: Int) : RequisitoConquista()
    data class AmigosCadastrados(val quantidade: Int) : RequisitoConquista()
    
    // Requisitos de ranking
    data class PosicaoRanking(val posicao: Int, val dias: Int) : RequisitoConquista()
    data class TopRanking(val posicaoMaxima: Int, val vezes: Int) : RequisitoConquista()
    data class PontosRanking(val pontos: Int) : RequisitoConquista()
    
    // Requisitos de distância
    data class DistanciaTotal(val quilometros: Double) : RequisitoConquista()
    data class DistanciaDiaria(val quilometros: Double, val dias: Int) : RequisitoConquista()
    data class DistanciaUnicoDia(val quilometros: Double) : RequisitoConquista()
    
    // Requisitos de tempo
    data class TempoAtividade(val minutos: Int) : RequisitoConquista()
    data class TempoAtividadeDiaria(val minutos: Int, val dias: Int) : RequisitoConquista()
    data class TempoAtividadeTotal(val horas: Int) : RequisitoConquista()
    
    // Requisitos de horário
    data class AtividadeHorario(val horaInicio: Int, val horaFim: Int, val dias: Int) : RequisitoConquista()
    data class AtividadeMatinal(val horaLimite: Int, val dias: Int) : RequisitoConquista()
    data class AtividadeNoturna(val horaInicio: Int, val dias: Int) : RequisitoConquista()
    
    // Requisitos de pontos
    data class PontosTotal(val pontos: Int) : RequisitoConquista()
    data class PontosDiarios(val pontos: Int, val dias: Int) : RequisitoConquista()
    data class PontosConsecutivos(val pontos: Int, val diasConsecutivos: Int) : RequisitoConquista()
    
    // Requisitos de nível
    data class NivelAlcancado(val nivel: Int) : RequisitoConquista()
    data class SubirNiveis(val quantidade: Int, val periodo: Int) : RequisitoConquista()
    
    // Requisitos especiais
    data class Combinacao(val requisitos: List<RequisitoConquista>) : RequisitoConquista()
    data class EventoEspecial(val eventoId: String) : RequisitoConquista()
    data class PrimeiraVez(val acao: String) : RequisitoConquista()
    data class Meta(val tipo: String, val valor: Int, val periodo: Int) : RequisitoConquista()
    
    // Requisitos de frequência
    data class FrequenciaSemanal(val vezesNaSemana: Int, val semanas: Int) : RequisitoConquista()
    data class FrequenciaMensal(val vezesNoMes: Int, val meses: Int) : RequisitoConquista()
    
    // Requisitos de melhoria
    data class MelhoriaPercentual(val percentual: Double, val metrica: String) : RequisitoConquista()
    data class SuperarRecord(val metrica: String, val vezesConsecutivas: Int) : RequisitoConquista()
}

// Extensões úteis para verificação de progresso
fun RequisitoConquista.calcularProgressoPercentual(progressoAtual: Int): Int {
    return when (this) {
        is RequisitoConquista.PassosTotal -> 
            minOf(100, (progressoAtual * 100) / meta)
        is RequisitoConquista.PassosDiarios -> 
            minOf(100, (progressoAtual * 100) / diasConsecutivos)
        is RequisitoConquista.PassosUnicoDia -> 
            minOf(100, (progressoAtual * 100) / meta)
        is RequisitoConquista.Sequencia -> 
            minOf(100, (progressoAtual * 100) / dias)
        is RequisitoConquista.SequenciaPerfeita -> 
            minOf(100, (progressoAtual * 100) / dias)
        is RequisitoConquista.GruposParticipando -> 
            minOf(100, (progressoAtual * 100) / quantidade)
        is RequisitoConquista.ConvitesEnviados -> 
            minOf(100, (progressoAtual * 100) / quantidade)
        is RequisitoConquista.ConvitesAceitos -> 
            minOf(100, (progressoAtual * 100) / quantidade)
        is RequisitoConquista.AmigosCadastrados -> 
            minOf(100, (progressoAtual * 100) / quantidade)
        is RequisitoConquista.PosicaoRanking -> 
            minOf(100, (progressoAtual * 100) / dias)
        is RequisitoConquista.TopRanking -> 
            minOf(100, (progressoAtual * 100) / vezes)
        is RequisitoConquista.PontosRanking -> 
            minOf(100, (progressoAtual * 100) / pontos)
        is RequisitoConquista.DistanciaTotal -> 
            minOf(100, (progressoAtual * 100) / (quilometros * 1000).toInt())
        is RequisitoConquista.DistanciaDiaria -> 
            minOf(100, (progressoAtual * 100) / dias)
        is RequisitoConquista.DistanciaUnicoDia -> 
            minOf(100, (progressoAtual * 100) / (quilometros * 1000).toInt())
        is RequisitoConquista.TempoAtividade -> 
            minOf(100, (progressoAtual * 100) / minutos)
        is RequisitoConquista.TempoAtividadeDiaria -> 
            minOf(100, (progressoAtual * 100) / dias)
        is RequisitoConquista.TempoAtividadeTotal -> 
            minOf(100, (progressoAtual * 100) / (horas * 60))
        is RequisitoConquista.AtividadeHorario -> 
            minOf(100, (progressoAtual * 100) / dias)
        is RequisitoConquista.AtividadeMatinal -> 
            minOf(100, (progressoAtual * 100) / dias)
        is RequisitoConquista.AtividadeNoturna -> 
            minOf(100, (progressoAtual * 100) / dias)
        is RequisitoConquista.PontosTotal -> 
            minOf(100, (progressoAtual * 100) / pontos)
        is RequisitoConquista.PontosDiarios -> 
            minOf(100, (progressoAtual * 100) / dias)
        is RequisitoConquista.PontosConsecutivos -> 
            minOf(100, (progressoAtual * 100) / diasConsecutivos)
        is RequisitoConquista.NivelAlcancado -> 
            minOf(100, (progressoAtual * 100) / nivel)
        is RequisitoConquista.SubirNiveis -> 
            minOf(100, (progressoAtual * 100) / quantidade)
        is RequisitoConquista.FrequenciaSemanal -> 
            minOf(100, (progressoAtual * 100) / (vezesNaSemana * semanas))
        is RequisitoConquista.FrequenciaMensal -> 
            minOf(100, (progressoAtual * 100) / (vezesNoMes * meses))
        is RequisitoConquista.MelhoriaPercentual -> 
            minOf(100, (progressoAtual * 100) / percentual.toInt())
        is RequisitoConquista.SuperarRecord -> 
            minOf(100, (progressoAtual * 100) / vezesConsecutivas)
        is RequisitoConquista.Meta -> 
            minOf(100, (progressoAtual * 100) / valor)
        else -> 0 // Para requisitos complexos como Combinacao, EventoEspecial, PrimeiraVez
    }
}

fun RequisitoConquista.getDescricaoDetalhada(): String {
    return when (this) {
        is RequisitoConquista.PassosTotal -> 
            "Dar $meta passos no total"
        is RequisitoConquista.PassosDiarios -> 
            "Dar $meta passos por dia durante $diasConsecutivos dias consecutivos"
        is RequisitoConquista.PassosUnicoDia -> 
            "Dar $meta passos em um único dia"
        is RequisitoConquista.Sequencia -> 
            "Manter atividade por $dias dias consecutivos"
        is RequisitoConquista.SequenciaPerfeita -> 
            "Atingir $metaDiaria passos por dia durante $dias dias consecutivos"
        is RequisitoConquista.GruposParticipando -> 
            "Participar de $quantidade grupos diferentes"
        is RequisitoConquista.ConvitesEnviados -> 
            "Enviar $quantidade convites para amigos"
        is RequisitoConquista.AtividadeHorario -> 
            "Fazer atividade entre ${horaInicio}h e ${horaFim}h por $dias dias"
        is RequisitoConquista.PontosTotal -> 
            "Acumular $pontos pontos no total"
        is RequisitoConquista.NivelAlcancado -> 
            "Alcançar o nível $nivel"
        else -> "Requisito especial"
    }
}