package com.guardsms.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val grouped = mutableMapOf<String, StringBuilder>()

        for (smsMsg in messages) {
            val sender = smsMsg.displayOriginatingAddress ?: "Unknown"
            grouped.getOrPut(sender) { StringBuilder() }.append(smsMsg.messageBody)
        }

        for ((sender, body) in grouped) {
            Timber.d("SMS received from $sender: ${body.take(50)}")
            val serviceIntent = Intent(context, SmsAnalysisService::class.java).apply {
                putExtra(SmsAnalysisService.EXTRA_SENDER, sender)
                putExtra(SmsAnalysisService.EXTRA_BODY, body.toString())
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}
