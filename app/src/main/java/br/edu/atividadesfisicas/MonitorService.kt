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
import android.os.Handler
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore // Import FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore // Import ktx extension
import com.google.firebase.ktx.Firebase // Import Firebase instance

class MonitorService : Service(), SensorEventListener {

    private val TAG = "StepCounterService"
    private val CHANNEL_ID = "StepCounterServiceChannel"
    private val binder = LocalBinder()

    private var sensorManager: SensorManager? = null
    private var accelerometerSensor: Sensor? = null

    private var stepCounterSensor: Sensor? = null
    private var initialStepCount: Int = 0

    private var totalSteps = 0
    private var listener: StepCounterListener? = null

    //flag de autorizacao para o pedometro
    private var permissionToPedometro : Boolean = false

    // Variáveis para a lógica de detecção de passos com acelerômetro, método 2
    private val ACCELERATION_THRESHOLD = 3f // For absolute acceleration magnitude (m/s^2)
    private val STEP_DETECTION_THRESHOLD = 1f // Threshold for the change in acceleration magnitude (adjust carefully!)
    private val TIME_BETWEEN_STEPS_MS = 300L // Minimum time between detected steps to avoid double countingA

    private var lastAccelerationMagnitude: Float = 0f
    private var lastStepTime: Long = 0L

    // Firebase related variables
    private var baseSteps: Int? = null
    private lateinit var firestore: FirebaseFirestore // Use Firestore
    private lateinit var auth: FirebaseAuth
    private lateinit var handler : Handler
    private val SAVE_INTERVAL_MS: Long = 5 * 1000L // Salvar dados a cada 1 minuto
    // Declare saveStepsRunnable as lateinit var, also to be initialized in onCreate
    private lateinit var saveStepsRunnable: Runnable

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

        // Inicializar Firebase
        firestore = Firebase.firestore // Inicializa o Firestore
        auth = FirebaseAuth.getInstance()

        // Initialize saveStepsRunnable HERE, after handler is initialized
        handler = Handler(mainLooper) // <-- This must come before saveStepsRunnable
        saveStepsRunnable = object : Runnable { // <-- CHANGE: Initialize runnable here
            override fun run() {
                saveStepsToFirestore()
                handler.postDelayed(this, SAVE_INTERVAL_MS)
            }
        }

        // Configurar notificação para Foreground Service
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Contador de Passos")
            .setContentText("Contando seus passos em segundo plano.")
            .setSmallIcon(android.R.drawable.ic_menu_directions)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        startForeground(1, notification)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager?.let {
            stepCounterSensor = it.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
            if (stepCounterSensor != null && permissionToPedometro) {
                Log.d(TAG, "Sensor TYPE_STEP_COUNTER disponível. Usando-o.")
                it.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL)
            } else {
                Log.w(TAG, "Sensor TYPE_STEP_COUNTER não disponível. Tentando TYPE_ACCELEROMETER.")
                accelerometerSensor = it.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                if (accelerometerSensor == null) {
                    Log.e(TAG, "Nenhum sensor de passo ou acelerômetro disponível no dispositivo. Parando serviço.")
                    stopSelf()
                } else {
                    it.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME)
                    Log.d(TAG, "Sensor do acelerômetro registrado.")
                }
            }
        }

        // Inicia o salvamento periódico para o Firestore
        handler.post(saveStepsRunnable)
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
        intent?.let{
            permissionToPedometro = it.getBooleanExtra("PERMISSION", false)
            baseSteps = it.getIntExtra("LAST_STEPS", 0)
        }
        Log.d(TAG, "onStartCommand: Serviço iniciado.")
        // START_STICKY é bom para serviços que rodam indefinidamente.
        return START_STICKY
    }

    // registra o evento de nova informação do sensor, usando pedometro primeiro e acelerometro se pedometro nao disponivel
    override fun onSensorChanged(event: SensorEvent?) {
        when (event?.sensor?.type) {
            Sensor.TYPE_STEP_COUNTER -> {
                val currentSensorSteps = event.values[0].toInt()
                Log.d(TAG, "TYPE_STEP_COUNTER - Leitura bruta: $currentSensorSteps")

                if (initialStepCount == 0) {
                    initialStepCount = currentSensorSteps
                    Log.d(TAG, "TYPE_STEP_COUNTER - Initializando initialStepCount com: $initialStepCount")
                }

                totalSteps = currentSensorSteps - initialStepCount
                Log.d(TAG, "Passos (TYPE_STEP_COUNTER): $totalSteps (Bruto: $currentSensorSteps - Inicial: $initialStepCount)")

                listener?.onStepCountChanged(totalSteps)
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
                    totalSteps++
                    Log.d(TAG, "Passo detectado (Acelerômetro)! Total: $totalSteps, Magnitude: $currentAccelerationMagnitude")
                    listener?.onStepCountChanged(totalSteps)
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

        // Obtém uma referência ao documento do usuário no Firestore
        val userDocRef = firestore.collection("usuarios").document(userId)

        // Cria um mapa com os dados que você quer atualizar
        // Assumindo que 'pontuacao' é o campo que armazena os passos totais ou uma pontuação baseada neles
        totalSteps += baseSteps!!
        baseSteps = LoginActivity.currentUserProfile?.pontuacao
        Log.d("D", "BASE: $baseSteps")
        val updates = hashMapOf<String, Any>(
            "pontuacao" to totalSteps // Atualiza o campo 'pontuacao' com o totalSteps + baseSteps que estavam no banco
        )

        userDocRef.update(updates)
            .addOnSuccessListener {
                Log.d(TAG, "Passos salvos no Firestore: $totalSteps para o usuário $userId")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Erro ao salvar passos no Firestore para o usuário $userId: ${e.message}")
            }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { }

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