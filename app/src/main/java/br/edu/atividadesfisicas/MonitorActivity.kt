package br.edu.atividadesfisicas;

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import br.edu.atividadesfisicas.MonitorService

class MonitorActivity : AppCompatActivity(), MonitorService.StepCounterListener {

    private var monitorService: MonitorService? = null
    private var isBound = false
    private var stepsTextView: TextView? = null
    private var permissionToPedometro : Boolean = false

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
    }

    private fun checkAndAskForPedometro ()
    {
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                // Your logic here to handle the permission result
                if (isGranted) {
                    permissionToPedometro = true;
                }
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
            serviceIntent.putExtra("LAST_STEPS", LoginActivity.currentUserProfile?.pontuacao)
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

    companion object {
        private const val TAG = "MonitorActivity"
    }
}