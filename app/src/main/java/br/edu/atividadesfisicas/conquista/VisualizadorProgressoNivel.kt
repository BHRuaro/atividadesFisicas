import android.R.attr.orientation
import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import br.edu.atividadesfisicas.R

// Componente de nível na MainActivity
class VisualizadorProgressoNivel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    
    private val textoNivel: TextView
    private val textoTitulo: TextView
    private val barraProgresso: ProgressBar
    private val textoXP: TextView
    
    init {
        orientation = VERTICAL
        inflate(context, R.layout.visualizador_progresso_nivel, this)
        
        textoNivel = findViewById(R.id.tvNivel)
        textoTitulo = findViewById(R.id.tvTituloNivel)
        barraProgresso = findViewById(R.id.barraProgressoXP)
        textoXP = findViewById(R.id.tvXP)
    }
    
    fun atualizarNivel(nivelUsuario: NivelUsuario) {
        textoNivel.text = "Nível ${nivelUsuario.nivelAtual}"
        textoTitulo.text = NivelUsuario.obterTituloNivel(nivelUsuario.nivelAtual)
        barraProgresso.progress = (nivelUsuario.progresso * 100).toInt()
        textoXP.text = "${nivelUsuario.xpAtual}/${nivelUsuario.xpProximoNivel} XP"
    }
}