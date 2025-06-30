package br.edu.atividadesfisicas

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
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
    }
}