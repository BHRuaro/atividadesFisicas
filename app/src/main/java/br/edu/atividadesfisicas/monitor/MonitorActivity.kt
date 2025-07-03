package br.edu.atividadesfisicas.monitor

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import br.edu.atividadesfisicas.auth.LoginActivity
import br.edu.atividadesfisicas.R

class MonitorActivity : AppCompatActivity(), MonitorService.StepCounterListener {

    private var monitorService: MonitorService? = null
    private var isBound = false
    private var stepsTextView: TextView? = null
    private var permissionToPedometro : Boolean = false
    private lateinit var btnParar: TextView

    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder: MonitorService.LocalBinder = service as MonitorService.LocalBinder
            monitorService = binder.getService()
            isBound = true
            Log.d(TAG, "Serviço vinculado com sucesso.")
            monitorService!!.setStepCounterListener(this@MonitorActivity)
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
            Log.d(TAG, "Serviço desvinculado.")
            monitorService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_monitor)
        stepsTextView = findViewById<TextView>(R.id.stepsTextView)

        // Apenas verifica se o acelerômetro está disponível
        checkAccelerometerAvailability()
        startAndBindService() // Inicia e vincula o serviço diretamente

        btnParar = findViewById(R.id.btnParar)
        btnParar.setOnClickListener {
            pararMonitoramento()
        }

        val ivQuestion = findViewById<ImageView>(R.id.ivQuestion)
        ivQuestion.setOnClickListener {
            mostrarInfoPontuacao()
        }

    }

    private fun checkAccelerometerAvailability() {
        val sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) == null) {
            Toast.makeText(this, "Sensor de Acelerômetro não disponível.", Toast.LENGTH_LONG).show()
            Log.e(TAG, "Sensor de Acelerômetro não disponível.")
        }
    }

    private fun startAndBindService() {

        val serviceIntent = Intent(
            this,
            MonitorService::class.java
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, serviceIntent)
        } else {
            startService(serviceIntent)
        }

        bindService(serviceIntent, connection, BIND_AUTO_CREATE)

    }

    override fun onStart() {

        super.onStart()

        if (!isBound) {
            val serviceIntent = Intent(
                this,
                MonitorService::class.java
            )
            serviceIntent.putExtra("PERMISSION", permissionToPedometro)
            serviceIntent.putExtra("LAST_STEPS", LoginActivity.Companion.currentUserProfile?.pontuacao)
            bindService(serviceIntent, connection, BIND_AUTO_CREATE)
        } else {
            if (monitorService != null) {
                monitorService!!.setStepCounterListener(this)
            }
        }

    }

    override fun onStop() {

        super.onStop()

        if (isBound) {
            if (monitorService != null) {
                monitorService!!.setStepCounterListener(null)
            }
            unbindService(connection)
            isBound = false
            Log.d(TAG, "Activity desvinculada do serviço.") //para debug
        }

    }

    override fun onDestroy() {

        super.onDestroy()
        Log.d(TAG, "MonitorActivity destruída.") //para debug

    }

    override fun onStepCountChanged(steps: Int) {
        runOnUiThread {
            stepsTextView!!.text = "Passos: $steps"
            Log.d(TAG, "UI atualizada com passos: $steps")
        }
    }

    private fun pararMonitoramento() {
        if (isBound) {
            unbindService(connection)
            isBound = false
            Log.d(TAG, "Serviço desvinculado manualmente.")
        }
        val serviceIntent = Intent(this, MonitorService::class.java)
        stopService(serviceIntent)
        Toast.makeText(this, "Monitoramento encerrado", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun mostrarInfoPontuacao() {
        val builder = AlertDialog.Builder(this)
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

    companion object {
        private const val TAG = "MonitorActivity"
    }
}