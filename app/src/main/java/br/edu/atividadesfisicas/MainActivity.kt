package br.edu.atividadesfisicas

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
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
}