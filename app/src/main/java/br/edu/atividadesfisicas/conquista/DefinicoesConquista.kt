import br.edu.atividadesfisicas.R

object DefinicoesConquista {
    val conquistas = listOf(
        Conquista(
            id = "primeiros_passos",
            titulo = "Primeiros Passos",
            descricao = "Dê seus primeiros 1.000 passos",
            iconeRes = R.drawable.baseline_footprint_24,
            categoria = CategoriaConquista.PASSOS,
            raridade = RaridadeConquista.COMUM,
            requisito = RequisitoConquista.PassosTotal(1000),
            pontosRecompensa = 100
        ),
        
        Conquista(
            id = "caminhante_diario",
            titulo = "Caminhante Diário",
            descricao = "Atinja 10.000 passos por dia durante 7 dias seguidos",
            iconeRes = R.drawable.baseline_run_circle_24,
            categoria = CategoriaConquista.CONSISTENCIA,
            raridade = RaridadeConquista.RARA,
            requisito = RequisitoConquista.PassosDiarios(10000, 7),
            pontosRecompensa = 500
        ),
        
        Conquista(
            id = "explorador",
            titulo = "Explorador",
            descricao = "Caminhe por 30 dias consecutivos",
            iconeRes = R.drawable.baseline_trending_up_24,
            categoria = CategoriaConquista.CONSISTENCIA,
            raridade = RaridadeConquista.EPICA,
            requisito = RequisitoConquista.Sequencia(30),
            pontosRecompensa = 750
        ),
        
        Conquista(
            id = "maratonista",
            titulo = "Maratonista",
            descricao = "Atinja 50.000 passos em um único dia",
            iconeRes = R.drawable.baseline_local_fire_department_24,
            categoria = CategoriaConquista.PASSOS,
            raridade = RaridadeConquista.LENDARIA,
            requisito = RequisitoConquista.PassosTotal(50000),
            pontosRecompensa = 1000
        ),
        
        Conquista(
            id = "social_butterfly",
            titulo = "Borboleta Social",
            descricao = "Participe de 3 grupos diferentes",
            iconeRes = R.drawable.baseline_groups_24,
            categoria = CategoriaConquista.SOCIAL,
            raridade = RaridadeConquista.RARA,
            requisito = RequisitoConquista.GruposParticipando(3),
            pontosRecompensa = 400
        ),
        
        Conquista(
            id = "campeao",
            titulo = "Campeão",
            descricao = "Fique em 1º lugar no ranking por 7 dias",
            iconeRes = R.drawable.baseline_trophy_24,
            categoria = CategoriaConquista.RANKING,
            raridade = RaridadeConquista.LENDARIA,
            requisito = RequisitoConquista.PosicaoRanking(1, 7),
            pontosRecompensa = 1500
        ),
        
        Conquista(
            id = "estrela_matinal",
            titulo = "Estrela Matinal",
            descricao = "Caminhe antes das 7h por 14 dias",
            iconeRes = R.drawable.baseline_schedule_24,
            categoria = CategoriaConquista.HORARIO,
            raridade = RaridadeConquista.EPICA,
            requisito = RequisitoConquista.AtividadeHorario(6, 7, 14),
            pontosRecompensa = 800
        ),
        
        Conquista(
            id = "colecionador_estrelas",
            titulo = "Colecionador de Estrelas",
            descricao = "Ganhe 10.000 pontos total",
            iconeRes = R.drawable.baseline_star_24,
            categoria = CategoriaConquista.PONTOS,
            raridade = RaridadeConquista.EPICA,
            requisito = RequisitoConquista.PontosTotal(10000),
            pontosRecompensa = 1000
        )
    )
}