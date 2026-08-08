package com.mrkuzumi.polish

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

class PolishReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val dateStr = intent.getStringExtra("date") ?: return
        val notifId = intent.getIntExtra("id", 0)
        val channelId = "polish_reminder"

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("该磨剑了！")
            .setContentText("$dateStr 的磨剑日到了，记得准时磨剑！(^^ゞ")
            .setStyle(NotificationCompat.BigTextStyle().bigText("$dateStr 的磨剑日到了！\n点此打开磨剑记录今天份的练习。"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(channelId, notifId, notification)
    }
}
