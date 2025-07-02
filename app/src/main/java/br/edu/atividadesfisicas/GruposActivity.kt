package br.edu.atividadesfisicas

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class GruposActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grupos)

        val ivQuestion = findViewById<ImageView>(R.id.ivQuestion)
        ivQuestion.setOnClickListener {
            mostrarInfoPontuacao()
        }
    }

    private fun mostrarInfoPontuacao() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Como os pontos funcionam?")
        builder.setMessage(
            "A cada passo dado, você ganha pontos automaticamente!\n\n" +
                    "Esses pontos são contabilizados para todos os grupos que você participa " +
                    "e também somam na sua pontuação do ranking global.\n\n" +
                    "Continue se movimentando! 🏃‍♀️🔥"
        )
        builder.setPositiveButton("Entendi") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    fun criarGrupo(view: View) {
        val intent = Intent(this, CriarGruposActivity::class.java)
        startActivity(intent)
    }
}