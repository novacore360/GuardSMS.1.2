package com.guardsms.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.guardsms.GuardSMSApp
import com.guardsms.R
import com.guardsms.data.repository.GuardRepository
import com.guardsms.domain.model.MessageStatus
import com.guardsms.domain.model.ThreatLevel
import com.guardsms.presentation.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class SmsAnalysisService : Service() {

    @Inject
    lateinit var repository: GuardRepository

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val sender = intent?.getStringExtra(EXTRA_SENDER) ?: return START_NOT_STICKY
        val body = intent.getStringExtra(EXTRA_BODY) ?: return START_NOT_STICKY

        startForeground(NOTIFICATION_ID_PROGRESS, buildProgressNotification())

        scope.launch {
            try {
                val isContact = repository.isContact(sender)
                val senderName = if (isContact) getSenderName(sender) else null
                val result = repository.saveMessage(sender, body, senderName, isContact)
                result.onSuccess { msg ->
                    sendStatusNotification(msg.sender, msg.senderName, msg.status, msg.threatLevel, msg.id)
                }.onFailure { e ->
                    Timber.e(e, "Failed to save/analyze message")
                }
            } catch (e: Exception) {
                Timber.e(e, "SMS analysis error")
            } finally {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf(startId)
            }
        }

        return START_NOT_STICKY
    }

    private fun buildProgressNotification(): android.app.Notification {
        return NotificationCompat.Builder(this, GuardSMSApp.CHANNEL_SERVICE)
            .setSmallIcon(R.drawable.ic_shield_check)
            .setContentTitle("GuardSMS")
            .setContentText("Analyzing incoming message…")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
    }

    private fun getSenderName(phone: String): String? {
        // In production, look up from contacts ContentProvider
        return null
    }

    private fun sendStatusNotification(
        sender: String,
        senderName: String?,
        status: String,
        threatLevel: String,
        messageId: String
    ) {
        val nm = getSystemService(NotificationManager::class.java)
        val displayName = senderName ?: sender

        val tapIntent = Intent(this, MainActivity::class.java).apply {
            putExtra("message_id", messageId)
            putExtra("open_tab", "messages")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, messageId.hashCode(), tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val (title, text, channel, priority, iconRes) = when {
            status == MessageStatus.BLOCKED.name -> Notification(
                "⛔ Message Blocked",
                "Blocked from $displayName — known malicious domain",
                GuardSMSApp.CHANNEL_ALERT,
                NotificationCompat.PRIORITY_MAX,
                R.drawable.ic_shield_alert
            )
            status == MessageStatus.FLAGGED.name && threatLevel == ThreatLevel.HIGH.name ||
            threatLevel == ThreatLevel.CRITICAL.name -> Notification(
                "🚨 Suspicious Message",
                "From $displayName — possible phishing or scam",
                GuardSMSApp.CHANNEL_ALERT,
                NotificationCompat.PRIORITY_HIGH,
                R.drawable.ic_shield_alert
            )
            status == MessageStatus.FLAGGED.name -> Notification(
                "⚠️ Possible Threat",
                "Message from $displayName flagged for review",
                GuardSMSApp.CHANNEL_ALERT,
                NotificationCompat.PRIORITY_DEFAULT,
                R.drawable.ic_shield_warning
            )
            else -> Notification(
                "✅ Safe Message",
                "Message from $displayName — no threats detected",
                GuardSMSApp.CHANNEL_SAFE,
                NotificationCompat.PRIORITY_LOW,
                R.drawable.ic_shield_check
            )
        }

        val notification = NotificationCompat.Builder(this, channel)
            .setSmallIcon(iconRes)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(priority)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        nm.notify(messageId.hashCode(), notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private data class Notification(
        val title: String,
        val text: String,
        val channel: String,
        val priority: Int,
        val iconRes: Int
    )

    companion object {
        const val EXTRA_SENDER = "extra_sender"
        const val EXTRA_BODY = "extra_body"
        private const val NOTIFICATION_ID_PROGRESS = 1001
    }
}
