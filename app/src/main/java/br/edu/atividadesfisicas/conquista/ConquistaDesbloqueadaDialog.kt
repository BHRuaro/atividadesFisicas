package br.edu.atividadesfisicas.conquista

import Conquista
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import br.edu.atividadesfisicas.R

class ConquistaDesbloqueadaDialog(
    context: Context,
    private val conquista: Conquista
) : Dialog(context, R.style.Theme_Transparent) {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialogo_conquista_desbloqueada)
        
        // Tornar fundo transparente
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        
        configurarElementos()
        configurarRaridade()
        configurarAnimacoes()
        configurarBotoes()
    }
    
    private fun configurarElementos() {
        findViewById<TextView>(R.id.tvTituloConquista)?.text = conquista.titulo
        findViewById<TextView>(R.id.tvDescricaoConquista)?.text = conquista.descricao
        findViewById<TextView>(R.id.tvPontosConquista)?.text = "+${conquista.pontosRecompensa} pts"
        
        // Configurar ícone da conquista
        findViewById<ImageView>(R.id.ivIconeConquista)?.apply {
            try {
                setImageResource(conquista.iconeRes)
            } catch (e: Exception) {
                // Usar ícone padrão se houver erro
                setImageResource(R.drawable.baseline_trophy_24)
            }
        }
        
        // Configurar categoria
        findViewById<TextView>(R.id.tvCategoriaConquista)?.apply {
            text = conquista.categoria.displayName
        }
    }
    
    private fun configurarRaridade() {
        val badgeRaridade = findViewById<TextView>(R.id.tvBadgeRaridade)
        val cartaoConquista = findViewById<CardView>(R.id.cartaoConquista)
        
        // Configurar texto da raridade
        badgeRaridade?.text = when (conquista.raridade) {
            RaridadeConquista.COMUM -> "COMUM"
            RaridadeConquista.RARA -> "RARA"
            RaridadeConquista.EPICA -> "ÉPICA"
            RaridadeConquista.LENDARIA -> "LENDÁRIA"
            RaridadeConquista.MITICA -> "MÍTICA"
        }
        
        // Configurar cores baseadas na raridade
        val corRaridade = when (conquista.raridade) {
            RaridadeConquista.COMUM -> 
                ContextCompat.getColor(context, R.color.raridade_comum)
            RaridadeConquista.RARA -> 
                ContextCompat.getColor(context, R.color.raridade_rara)
            RaridadeConquista.EPICA -> 
                ContextCompat.getColor(context, R.color.raridade_epica)
            RaridadeConquista.LENDARIA -> 
                ContextCompat.getColor(context, R.color.raridade_lendaria)
            RaridadeConquista.MITICA -> 
                ContextCompat.getColor(context, R.color.primary_green)
        }
        
        badgeRaridade?.setBackgroundColor(corRaridade)
        
        // Configurar borda do cartão baseada na raridade
        cartaoConquista?.setCardBackgroundColor(
            ContextCompat.getColor(context, R.color.background_dark)
        )
    }
    
    private fun configurarAnimacoes() {
        val cartaoView = findViewById<CardView>(R.id.cartaoConquista)
        val iconeView = findViewById<ImageView>(R.id.ivIconeConquista)
        val overlayBrilho = findViewById<TextView>(R.id.tvBrilhoEfeito)
        
        // Animação inicial do cartão
        cartaoView?.apply {
            alpha = 0f
            scaleX = 0.3f
            scaleY = 0.3f
            
            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(600)
                .setInterpolator(OvershootInterpolator(1.2f))
                .start()
        }
        
        // Efeito de rotação no ícone
        iconeView?.postDelayed({
            iconeView.animate()
                .rotationBy(360f)
                .setDuration(1000)
                .start()
        }, 300)
        
        // Efeito de brilho (se disponível)
        overlayBrilho?.apply {
            alpha = 0f
            animate()
                .alpha(0.8f)
                .setDuration(500)
                .withEndAction {
                    animate()
                        .alpha(0f)
                        .setDuration(500)
                        .start()
                }
                .start()
        }
        
        // Vibração de celebração baseada na raridade
        when (conquista.raridade) {
            RaridadeConquista.LENDARIA, RaridadeConquista.MITICA -> {
                // Animação extra para raridades altas
                cartaoView?.postDelayed({
                    cartaoView.animate()
                        .scaleX(1.05f)
                        .scaleY(1.05f)
                        .setDuration(200)
                        .withEndAction {
                            cartaoView.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(200)
                                .start()
                        }
                        .start()
                }, 800)
            }
            else -> { /* Animação padrão já aplicada */ }
        }
    }
    
    private fun configurarBotoes() {
        findViewById<Button>(R.id.btnFecharConquista)?.setOnClickListener {
            dismiss()
        }
        
        findViewById<Button>(R.id.btnCompartilhar)?.setOnClickListener {
            compartilharConquista()
        }
    }
    
    private fun compartilharConquista() {
        // Implementar compartilhamento da conquista
        // Por exemplo, compartilhar nas redes sociais ou copiar para clipboard
        dismiss()
    }
    
    override fun show() {
        super.show()
        // Garantir que o diálogo seja modal e capture eventos de toque
        setCancelable(true)
        setCanceledOnTouchOutside(false)
    }
}