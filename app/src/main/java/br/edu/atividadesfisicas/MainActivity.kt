package br.edu.atividadesfisicas

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private lateinit var btnMonitor : Button;
    private lateinit var btnRanking: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btnRanking = findViewById(R.id.btnRanking)
        btnRanking.setOnClickListener {
            verRanking()
        }

        val ivQuestion = findViewById<ImageView>(R.id.ivQuestion)
        ivQuestion.setOnClickListener {
            mostrarInfoPontuacao()
        }

        btnMonitor = findViewById(R.id.btnMonitor);

        btnMonitor.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                val intent = Intent(this@MainActivity, MonitorActivity::class.java)
                startActivity(intent)
            }
        });

    }
    fun verRanking() {
        val intent = Intent(this, RankingActivity ::class.java)
        startActivity(intent)

        btnMonitor = findViewById(R.id.btnMonitor);

        btnMonitor.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                val intent = Intent(this@MainActivity, MonitorActivity::class.java)
                startActivity(intent)
            }
        });
    }


    fun verGrupos(view: View) {
        val intent = Intent(this, GruposActivity::class.java)
        startActivity(intent)
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
}

