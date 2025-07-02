import android.animation.ObjectAnimator
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import br.edu.atividadesfisicas.R

class AdaptadorConquistas(
    private val onConquistaClick: (Conquista) -> Unit
) : RecyclerView.Adapter<AdaptadorConquistas.ConquistaViewHolder>() {

    private var conquistas: List<Conquista> = emptyList()

    fun atualizarConquistas(novasConquistas: List<Conquista>) {
        conquistas = novasConquistas
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConquistaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_conquista, parent, false)
        return ConquistaViewHolder(view)
    }

    override fun onBindViewHolder(holder: ConquistaViewHolder, position: Int) {
        holder.bind(conquistas[position])
    }

    override fun getItemCount(): Int = conquistas.size

    inner class ConquistaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconeConquista: ImageView = itemView.findViewById(R.id.ivIconeConquista)
        private val tituloConquista: TextView = itemView.findViewById(R.id.tvTituloConquista)
        private val descricaoConquista: TextView = itemView.findViewById(R.id.tvDescricaoConquista)
        private val pontosConquista: TextView = itemView.findViewById(R.id.tvPontosConquista)
        private val progressoConquista: ProgressBar = itemView.findViewById(R.id.progressBarConquista)
        private val textoProgresso: TextView = itemView.findViewById(R.id.tvProgressoConquista)
        private val badgeRaridade: TextView = itemView.findViewById(R.id.tvBadgeRaridade)
        private val containerConquista: CardView = itemView.findViewById(R.id.cardConquista)
        private val overlayBloqueada: View = itemView.findViewById(R.id.overlayBloqueada)
        private val iconeBloqueada: ImageView = itemView.findViewById(R.id.ivIconeBloqueada)

        fun bind(conquista: Conquista) {
            tituloConquista.text = conquista.titulo
            descricaoConquista.text = conquista.descricao
            pontosConquista.text = "+${conquista.pontosRecompensa} pts"
            
            // Configurar raridade
            configurarRaridade(conquista.raridade)
            
            // Verificar se está desbloqueada
            val desbloqueada = conquista.desbloqueadaEm != null
            
            if (desbloqueada) {
                configurarConquistaDesbloqueada(conquista)
            } else {
                configurarConquistaBloqueada(conquista)
            }
            
            // Click listener
            itemView.setOnClickListener {
                onConquistaClick(conquista)
            }
        }
        
        private fun configurarRaridade(raridade: RaridadeConquista) {
            badgeRaridade.text = when (raridade) {
                RaridadeConquista.COMUM -> "COMUM"
                RaridadeConquista.RARA -> "RARA"
                RaridadeConquista.EPICA -> "ÉPICA"
                RaridadeConquista.LENDARIA -> "LENDÁRIA"
                RaridadeConquista.MITICA -> TODO()
            }
            
            val corRaridade = when (raridade) {
                RaridadeConquista.COMUM -> ContextCompat.getColor(itemView.context, R.color.primary_green)
                RaridadeConquista.RARA -> ContextCompat.getColor(itemView.context, R.color.primary_green)
                RaridadeConquista.EPICA -> ContextCompat.getColor(itemView.context, R.color.primary_green)
                RaridadeConquista.LENDARIA -> ContextCompat.getColor(itemView.context, R.color.primary_green)
                RaridadeConquista.MITICA -> TODO()
            }
            
            badgeRaridade.backgroundTintList = ColorStateList.valueOf(corRaridade)
            badgeRaridade.setTextColor(ContextCompat.getColor(itemView.context, R.color.black))
        }
        
        private fun configurarConquistaDesbloqueada(conquista: Conquista) {
            // Mostrar ícone colorido
            iconeConquista.setImageResource(conquista.iconeRes)
            iconeConquista.colorFilter = null
            iconeConquista.alpha = 1.0f
            
            // Ocultar overlay de bloqueio
            overlayBloqueada.visibility = View.GONE
            iconeBloqueada.visibility = View.GONE
            
            // Progresso completo
            progressoConquista.progress = 100
            textoProgresso.text = "Concluída!"
            textoProgresso.setTextColor(ContextCompat.getColor(itemView.context, R.color.primary_green))
            
            // Card com aparência normal
            containerConquista.alpha = 1.0f
            containerConquista.cardElevation = 8f
            
            // Animação de brilho para conquistas recentes
            if (conquistaRecente(conquista)) {
                adicionarAnimacaoBrilho()
            }
        }
        
        private fun configurarConquistaBloqueada(conquista: Conquista) {
            // Ícone em escala de cinza
            iconeConquista.setImageResource(conquista.iconeRes)
            iconeConquista.colorFilter = PorterDuffColorFilter(
                ContextCompat.getColor(itemView.context, R.color.white),
                PorterDuff.Mode.SRC_IN
            )
            iconeConquista.alpha = 0.5f
            
            // Mostrar overlay de bloqueio
            overlayBloqueada.visibility = View.VISIBLE
            iconeBloqueada.visibility = View.VISIBLE
            
            // Configurar progresso
            val progressoPercentual = calcularProgressoPercentual(conquista)
            progressoConquista.progress = progressoPercentual
            textoProgresso.text = "$progressoPercentual% concluído"
            textoProgresso.setTextColor(ContextCompat.getColor(itemView.context, R.color.white))
            
            // Card com aparência diminuída
            containerConquista.alpha = 0.7f
            containerConquista.cardElevation = 2f
        }
        
        private fun calcularProgressoPercentual(conquista: Conquista): Int {
            return when (val requisito = conquista.requisito) {
                is RequisitoConquista.PassosTotal -> {
                    minOf(100, (conquista.progresso * 100) / requisito.meta)
                }
                is RequisitoConquista.PassosDiarios -> {
                    minOf(100, (conquista.progresso * 100) / requisito.diasConsecutivos)
                }
                is RequisitoConquista.Sequencia -> {
                    minOf(100, (conquista.progresso * 100) / requisito.dias)
                }
                is RequisitoConquista.PosicaoRanking -> {
                    minOf(100, (conquista.progresso * 100) / requisito.dias)
                }
                is RequisitoConquista.GruposParticipando -> {
                    minOf(100, (conquista.progresso * 100) / requisito.quantidade)
                }
                is RequisitoConquista.ConvitesEnviados -> {
                    minOf(100, (conquista.progresso * 100) / requisito.quantidade)
                }
                is RequisitoConquista.AtividadeHorario -> {
                    minOf(100, (conquista.progresso * 100) / requisito.dias)
                }
                else -> 0
            }
        }
        
        private fun conquistaRecente(conquista: Conquista): Boolean {
            val agora = System.currentTimeMillis()
            val doisDiasAtras = agora - (2 * 24 * 60 * 60 * 1000) // 2 dias
            return conquista.desbloqueadaEm?.toDate()?.time ?: 0 > doisDiasAtras
        }
        
        private fun adicionarAnimacaoBrilho() {
            val animacao = ObjectAnimator.ofFloat(containerConquista, "alpha", 0.7f, 1.0f)
            animacao.duration = 1000
            animacao.repeatCount = 2
            animacao.repeatMode = ObjectAnimator.REVERSE
            animacao.start()
        }
    }
}