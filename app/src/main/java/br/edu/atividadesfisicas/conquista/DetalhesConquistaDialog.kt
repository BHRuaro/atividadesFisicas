package br.edu.atividadesfisicas.conquista

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import br.edu.atividadesfisicas.R

class DetalhesConquistaDialog(
    context: Context,
    private val conquistaUsuario: ConquistaUsuario,
    private val onAcao: (String) -> Unit
) : Dialog(context, R.style.Theme_Transparent) {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialogo_detalhes_conquista)
        
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        
        configurarElementos()
        configurarProgresso()
        configurarBotoes()
    }
    
    private fun configurarElementos() {
        val conquista = conquistaUsuario.conquista
        
        findViewById<TextView>(R.id.tvTituloConquistaDetalhes)?.text = conquista.titulo
        findViewById<TextView>(R.id.tvDescricaoConquistaDetalhes)?.text = conquista.descricao
        findViewById<TextView>(R.id.tvCategoriaConquistaDetalhes)?.text = conquista.categoria.displayName
        findViewById<TextView>(R.id.tvPontosConquistaDetalhes)?.text = "${conquista.pontosRecompensa} pontos"
        
        // Configurar ícone
        findViewById<ImageView>(R.id.ivIconeConquistaDetalhes)?.apply {
            setImageResource(conquista.iconeRes)
        }
        
        // Configurar raridade
        configurarRaridade()
        
        // Configurar status
        configurarStatus()
        
        // Configurar requisitos
        configurarRequisitos()
    }
    
    private fun configurarRaridade() {
        val conquista = conquistaUsuario.conquista
        val tvRaridade = findViewById<TextView>(R.id.tvRaridadeConquistaDetalhes)
        val cartao = findViewById<CardView>(R.id.cartaoDetalhesConquista)
        
        tvRaridade?.text = when (conquista.raridade) {
            RaridadeConquista.COMUM -> "COMUM"
            RaridadeConquista.RARA -> "RARA"
            RaridadeConquista.EPICA -> "ÉPICA"
            RaridadeConquista.LENDARIA -> "LENDÁRIA"
            RaridadeConquista.MITICA -> "MÍTICA"
        }
        
        val cor = when (conquista.raridade) {
            RaridadeConquista.COMUM -> R.color.raridade_comum
            RaridadeConquista.RARA -> R.color.raridade_rara
            RaridadeConquista.EPICA -> R.color.raridade_epica
            RaridadeConquista.LENDARIA -> R.color.raridade_lendaria
            RaridadeConquista.MITICA -> R.color.primary_green
        }
        
        tvRaridade?.setBackgroundColor(ContextCompat.getColor(context, cor))
    }
    
    private fun configurarStatus() {
        val tvStatus = findViewById<TextView>(R.id.tvStatusConquista)
        val ivStatus = findViewById<ImageView>(R.id.ivStatusConquista)
        
        if (conquistaUsuario.estaDesbloqueada()) {
            tvStatus?.text = "✅ DESBLOQUEADA"
            tvStatus?.setTextColor(ContextCompat.getColor(context, R.color.primary_green))
            ivStatus?.setImageResource(R.drawable.baseline_trophy_24)
            ivStatus?.setColorFilter(ContextCompat.getColor(context, R.color.primary_green))
        } else {
            tvStatus?.text = "🔒 BLOQUEADA"
            tvStatus?.setTextColor(ContextCompat.getColor(context, R.color.text_gray))
            ivStatus?.setImageResource(R.drawable.ic_lock_24)
            ivStatus?.setColorFilter(ContextCompat.getColor(context, R.color.text_gray))
        }
    }
    
    private fun configurarProgresso() {
        val progressBar = findViewById<ProgressBar>(R.id.progressBarConquistaDetalhes)
        val tvProgresso = findViewById<TextView>(R.id.tvProgressoConquistaDetalhes)
        val tvDescricaoProgresso = findViewById<TextView>(R.id.tvDescricaoProgressoDetalhes)
        
        val percentual = conquistaUsuario.calcularProgressoPercentual()
        
        progressBar?.progress = percentual
        tvProgresso?.text = "$percentual%"
        tvDescricaoProgresso?.text = conquistaUsuario.getDescricaoProgresso()
        
        if (conquistaUsuario.estaDesbloqueada()) {
            progressBar?.progressTintList = ContextCompat.getColorStateList(context, R.color.primary_green)
        } else {
            progressBar?.progressTintList = ContextCompat.getColorStateList(context, R.color.text_gray)
        }
    }
    
    private fun configurarRequisitos() {
        val tvRequisitos = findViewById<TextView>(R.id.tvRequisitosConquista)
        val tvDicas = findViewById<TextView>(R.id.tvDicasConquista)
        
        tvRequisitos?.text = conquistaUsuario.conquista.requisito.getDescricaoDetalhada()
        
        // Adicionar dicas baseadas no tipo de requisito
        val dicas = when (conquistaUsuario.conquista.requisito) {
            is RequisitoConquista.PassosTotal -> 
                "💡 Dica: Caminhe regularmente para acumular passos"
            is RequisitoConquista.PassosDiarios -> 
                "💡 Dica: Estabeleça uma rotina diária de caminhada"
            is RequisitoConquista.Sequencia -> 
                "💡 Dica: Mantenha consistência - não pule nenhum dia"
            is RequisitoConquista.GruposParticipando -> 
                "💡 Dica: Procure grupos na aba 'Grupos' e participe"
            is RequisitoConquista.AtividadeHorario -> 
                "💡 Dica: Configure lembretes para se exercitar no horário"
            else -> "💡 Dica: Continue se exercitando para desbloquear"
        }
        
        tvDicas?.text = dicas
    }
    
    private fun configurarBotoes() {
        val btnCompartilhar = findViewById<Button>(R.id.btnCompartilharDetalhes)
        val btnFechar = findViewById<Button>(R.id.btnFecharDetalhes)
        val btnMarcarVisualizada = findViewById<Button>(R.id.btnMarcarVisualizada)
        
        btnCompartilhar?.setOnClickListener {
            onAcao("compartilhar")
            dismiss()
        }
        
        btnFechar?.setOnClickListener {
            dismiss()
        }
        
        // Mostrar botão "Marcar como visualizada" apenas para conquistas desbloqueadas não visualizadas
        if (conquistaUsuario.estaDesbloqueada() && !conquistaUsuario.visualizada) {
            btnMarcarVisualizada?.visibility = View.VISIBLE
            btnMarcarVisualizada?.setOnClickListener {
                onAcao("marcar_visualizada")
                dismiss()
            }
        } else {
            btnMarcarVisualizada?.visibility = View.GONE
        }
    }
}