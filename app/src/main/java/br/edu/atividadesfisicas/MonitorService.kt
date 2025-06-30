package br.edu.atividadesfisicas

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class MonitorService : Service(), SensorEventListener {

    private val TAG = "StepCounterService"
    private val CHANNEL_ID = "StepCounterServiceChannel"
    private val binder = LocalBinder()

    private var sensorManager: SensorManager? = null
    private var accelerometerSensor: Sensor? = null
    private var stepCounterSensor: Sensor? = null
    private var totalSteps = 0
    private var listener: StepCounterListener? = null

    // Variáveis para a lógica de detecção de passos com acelerômetro, método 2
    private val ACCELERATION_THRESHOLD = 10.0f // For absolute acceleration magnitude (m/s^2)
    private val STEP_DETECTION_THRESHOLD = 6f // Threshold for the change in acceleration magnitude (adjust carefully!)
    private val TIME_BETWEEN_STEPS_MS = 300L // Minimum time between detected steps to avoid double countingA

    private var lastAccelerationMagnitude: Float = 0f
    private var lastStepTime: Long = 0L

    // Variáveis para a lógica de detecção de passos com acelerômetro, método 1
    private var lastTime: Long = 0
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private val SHAKE_THRESHOLD = 1450 // Limiar de "sacudida" para detectar um passo (ajuste este valor!)

    // Interface para comunicação com a Activity
    interface StepCounterListener {
        fun onStepCountChanged(steps: Int)
    }

    inner class LocalBinder : Binder() {
        fun getService(): MonitorService = this@MonitorService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Serviço criado.")
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Contador de Passos") // Generic title
            .setContentText("Contando seus passos em segundo plano.")
            .setSmallIcon(android.R.drawable.ic_menu_directions)
            .build()
        startForeground(1, notification)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager?.let {
            stepCounterSensor = it.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
            if (stepCounterSensor != null) {
                Log.d(TAG, "Sensor TYPE_STEP_COUNTER disponível. Usando-o.")
                it.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL)
                // When using TYPE_STEP_COUNTER, totalSteps is the cumulative count from boot
                // You'll need to store the initial value and subtract it to get steps for your session.
                // Or use TYPE_STEP_DETECTOR.
            } else {
                Log.w(TAG, "Sensor TYPE_STEP_COUNTER não disponível. Tentando TYPE_ACCELEROMETER.")
                accelerometerSensor = it.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                if (accelerometerSensor == null) {
                    Log.e(TAG, "Nenhum sensor de passo ou acelerômetro disponível.")
                    // Consider stopping the service or informing the user
                } else {
                    it.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME)
                    Log.d(TAG, "Sensor do acelerômetro registrado.")
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d(TAG, "Serviço vinculado.")
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "Serviço desvinculado.")
        // Retorne true se você quiser que onRebind seja chamado se a Activity tentar vincular novamente.
        return true
    }

    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)
        Log.d(TAG, "Serviço revinculado.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: Serviço iniciado.")
        // START_STICKY é bom para serviços que rodam indefinidamente.
        return START_STICKY
    }

    //forma que leva em consideração apenas o limiar de diferença entre duas medições
    /*override fun onSensorChanged(event: SensorEvent?) {
            if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
                val currentTime = System.currentTimeMillis()
                // A cada 100ms (0.1 segundos) ou mais, processamos os dados
                if ((currentTime - lastTime) > 100) {
                    val diffTime = (currentTime - lastTime)

                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]

                    // Calcular a magnitude da aceleração ou a mudança em relação ao estado anterior
                    // Uma forma simples de detectar "movimento":
                    val speed = Math.abs(x + y + z - lastX - lastY - lastZ) / diffTime * 10000

                    // Log.d(TAG, "Speed: $speed, X:$x, Y:$y, Z:$z")

                    if (speed > SHAKE_THRESHOLD) {
                        totalSteps++
                        Log.d(TAG, "Passo detectado! Total: $totalSteps")

                        // Notifica a Activity (se houver um listener)
                        listener?.onStepCountChanged(totalSteps)
                    }

                    lastX = x
                    lastY = y
                    lastZ = z
                    lastTime = currentTime
                }
            }
    }*/

    //forma calculando comprimento do vetor de acelaração, precisa de ajustes
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            // Calculate the magnitude of the total acceleration vector (Comprimento)
            val currentAccelerationMagnitude = Math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()

            val currentTime = System.currentTimeMillis()

            // Basic peak detection: look for a significant increase in magnitude
            // followed by a decrease, and ensure enough time has passed since the last step.
            if (currentAccelerationMagnitude > ACCELERATION_THRESHOLD &&
                (currentAccelerationMagnitude - lastAccelerationMagnitude) > STEP_DETECTION_THRESHOLD &&
                (currentTime - lastStepTime) > TIME_BETWEEN_STEPS_MS
            ) {
                totalSteps++
                Log.d(TAG, "Passo detectado! Total: $totalSteps, Magnitude: $currentAccelerationMagnitude")
                listener?.onStepCountChanged(totalSteps)
                lastStepTime = currentTime // Update the last step time
            }

            lastAccelerationMagnitude = currentAccelerationMagnitude // Update for next comparison
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Nada a fazer aqui na maioria dos casos
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Serviço destruído.")
        sensorManager?.unregisterListener(this) // Importante desregistrar o listener!
        stopForeground(true) // Remove a notificação foreground
    }

    // Método para a Activity definir o listener
    fun setStepCounterListener(listener: StepCounterListener?) {
        this.listener = listener
        // Se a Activity se conectar, envie o valor atual imediatamente
        this.listener?.onStepCountChanged(totalSteps)
    }

    // Método para a Activity obter os passos atuais diretamente
    fun getCurrentSteps(): Int {
        return totalSteps
    }

    // Cria o canal de notificação para Android Oreo (API 26) e superior
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Contador de Passos (Acelerômetro)"
            val description = "Notificação para serviço de contagem de passos em segundo plano com acelerômetro"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                this.description = description
            }
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}