package br.edu.atividadesfisicas.monitor

import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import br.edu.atividadesfisicas.auth.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MonitorService : Service(), SensorEventListener {

    private val TAG = "StepCounterService"
    private val CHANNEL_ID = "StepCounterServiceChannel"
    private val binder = LocalBinder()

    private var sensorManager: SensorManager? = null
    private var accelerometerSensor: Sensor? = null

    private var stepCounterSensor: Sensor? = null
    private var initialStepCountForSensor: Int = 0

    private var currentSessionSteps = 0
    private var listener: StepCounterListener? = null

    private var permissionToPedometro : Boolean = false

    private val ACCELERATION_THRESHOLD = 3f
    private val STEP_DETECTION_THRESHOLD = 1f
    private val TIME_BETWEEN_STEPS_MS = 300L

    private var lastAccelerationMagnitude: Float = 0f
    private var lastStepTime: Long = 0L

    private var baseStepsFromProfile: Int = 0

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var handler : Handler
    private val SAVE_INTERVAL_MS: Long = 5 * 1000L
    private lateinit var saveStepsRunnable: Runnable

    companion object {
        const val ACTION_STOP_SERVICE = "br.edu.atividadesfisicas.ACTION_STOP_MONITOR_SERVICE"
        const val EXTRA_PERMISSION_PEDOMETRO = "PERMISSION"
    }

    interface StepCounterListener {
        fun onStepCountChanged(steps: Int)
    }

    inner class LocalBinder : Binder() {
        fun getService(): MonitorService = this@MonitorService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Serviço criado.")

        firestore = Firebase.firestore
        auth = FirebaseAuth.getInstance()

        // Inicializa baseStepsFromProfile com a pontuação do usuário logado
        // Isso assume que LoginActivity.currentUserProfile já foi preenchido.
        LoginActivity.Companion.currentUserProfile?.let {
            baseStepsFromProfile = it.pontuacao
            Log.d(TAG, "Base steps carregados do Perfil de Usuário: $baseStepsFromProfile")
        } ?: run {
            Log.w(TAG, "currentUserProfile é nulo. Iniciando baseStepsFromProfile com 0.")
            baseStepsFromProfile = 0
        }

        handler = Handler(mainLooper)
        saveStepsRunnable = object : Runnable {
            override fun run() {
                saveStepsToFirestore()
                handler.postDelayed(this, SAVE_INTERVAL_MS)
            }
        }

        val notification = createNotification()
        startForeground(1, notification)
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d(TAG, "Serviço vinculado.")
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "Serviço desvinculado.")
        return true
    }

    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)
        Log.d(TAG, "Serviço revinculado.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: Serviço iniciado/comando recebido.")

        if (intent?.action == ACTION_STOP_SERVICE) {
            Log.d(TAG, "ACTION_STOP_SERVICE recebida. Parando serviço.")
            stopSelf()
            return START_NOT_STICKY
        }

        intent?.let {
            val newPermissionToPedometro = it.getBooleanExtra(EXTRA_PERMISSION_PEDOMETRO, false)
            permissionToPedometro = newPermissionToPedometro
            Log.d(TAG, "Permissão para pedômetro no Intent: $permissionToPedometro")
        }


        if (sensorManager == null) {
            sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        }

        sensorManager?.let {
            it.unregisterListener(this)

            stepCounterSensor = it.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
            if (stepCounterSensor != null && permissionToPedometro) {
                Log.d(TAG, "Registrando Sensor TYPE_STEP_COUNTER.")
                it.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL)
            } else {
                Log.w(TAG, "TYPE_STEP_COUNTER não disponível ou sem permissão. Tentando TYPE_ACCELEROMETER.")
                accelerometerSensor = it.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                if (accelerometerSensor == null) {
                    Log.e(TAG, "Nenhum sensor de passo ou acelerômetro disponível. Parando serviço.")
                    stopSelf()
                } else {
                    it.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL)
                    Log.d(TAG, "Registrando Sensor do acelerômetro.")
                }
            }
        }

        // Garante que o handler esteja sempre postando o runnable para salvar
        handler.removeCallbacks(saveStepsRunnable)
        handler.post(saveStepsRunnable)

        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent?) {
        when (event?.sensor?.type) {
            Sensor.TYPE_STEP_COUNTER -> {
                val currentSensorReading = event.values[0].toInt()
                Log.d(TAG, "TYPE_STEP_COUNTER - Leitura bruta: $currentSensorReading")

                if (initialStepCountForSensor == 0) {
                    initialStepCountForSensor = currentSensorReading
                    Log.d(TAG, "TYPE_STEP_COUNTER - Initializando initialStepCountForSensor com: $initialStepCountForSensor")
                }

                currentSessionSteps = currentSensorReading - initialStepCountForSensor
                Log.d(TAG, "Passos (TYPE_STEP_COUNTER) nesta sessão: $currentSessionSteps")


                listener?.onStepCountChanged(currentSessionSteps)
            }
            Sensor.TYPE_ACCELEROMETER -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                val currentAccelerationMagnitude = Math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                val currentTime = System.currentTimeMillis()

                if (currentAccelerationMagnitude > ACCELERATION_THRESHOLD &&
                    (currentAccelerationMagnitude - lastAccelerationMagnitude) > STEP_DETECTION_THRESHOLD &&
                    (currentTime - lastStepTime) > TIME_BETWEEN_STEPS_MS
                ) {
                    currentSessionSteps++
                    Log.d(TAG, "Passo detectado (Acelerômetro) nesta sessão! Total: $currentSessionSteps, Magnitude: $currentAccelerationMagnitude")

                    listener?.onStepCountChanged(currentSessionSteps)
                    lastStepTime = currentTime
                }
                lastAccelerationMagnitude = currentAccelerationMagnitude
            }
        }
    }

    private fun saveStepsToFirestore() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.w(TAG, "Nenhum usuário logado. Não é possível salvar passos no Firestore.")
            return
        }

        val userDocRef = firestore.collection("usuarios").document(userId)

        // O valor a ser salvo no Firestore é a soma da base carregada do perfil
        // e dos passos acumulados na sessão atual.
        val totalStepsToSave = baseStepsFromProfile + currentSessionSteps
        Log.d(TAG, "Salvando no Firestore: BaseSteps(do perfil)=${baseStepsFromProfile} + SessionSteps(atuais)=${currentSessionSteps} = Total=${totalStepsToSave}")

        val updates = hashMapOf<String, Any>(
            "pontuacao" to totalStepsToSave
        )

        userDocRef.update(updates)
            .addOnSuccessListener {
                Log.d(TAG, "Passos salvos no Firestore com sucesso: $totalStepsToSave para o usuário $userId")
                // Não há necessidade de atualizar baseStepsFromProfile aqui, pois ele é a base inicial.
                // currentSessionSteps continua acumulando os passos desta sessão.
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Erro ao salvar passos no Firestore para o usuário $userId: ${e.message}")
            }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Serviço destruído.")
        sensorManager?.unregisterListener(this)
        if (::handler.isInitialized) {
            handler.removeCallbacks(saveStepsRunnable)
        }
        stopForeground(true)

        saveStepsToFirestore()
    }

    fun setStepCounterListener(listener: StepCounterListener?) {
        this.listener = listener
        this.listener?.onStepCountChanged(currentSessionSteps)
    }

    private fun createNotification(): Notification {
        createNotificationChannel()

        val stopSelfIntent = Intent(this, MonitorService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }

        val stopServicePendingIntent: PendingIntent =
            PendingIntent.getService(this, 0, stopSelfIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Contador de Passos")
            .setContentText("Contando seus passos em segundo plano.")
            .setSmallIcon(R.drawable.ic_menu_directions)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)

        notificationBuilder.addAction(
            R.drawable.ic_menu_close_clear_cancel,
            "Parar Contagem",
            stopServicePendingIntent
        )

        return notificationBuilder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Contador de Passos (Acelerômetro)"
            val description = "Notificação para serviço de contagem de passos em segundo plano com acelerômetro"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                this.description = description
            }
            val notificationManager: NotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}