package br.edu.atividadesfisicas.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import br.edu.atividadesfisicas.conviteGrupo.SolicitacoesPendentesActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FirebaseMessagingService: FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        if (remoteMessage.data["type"] == "group_invite") {
            mostrarNotificacaoConvite(
                remoteMessage.notification?.title ?: "Convite para Grupo",
                remoteMessage.notification?.body ?: "Você recebeu um convite"
            )
        }
    }

    private fun mostrarNotificacaoConvite(titulo: String, corpo: String) {
        val intent = Intent(this, SolicitacoesPendentesActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, "convites_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(titulo)
            .setContentText(corpo)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "convites_channel",
                "Convites de Grupo",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        salvarTokenNoFirestore(token)
    }

    private fun salvarTokenNoFirestore(token: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(currentUser.uid)
                .update("fcmToken", token)
        }
    }
}