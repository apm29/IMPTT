package com.imptt.apm29.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.imptt.apm29.R

/**
 *  author : ciih
 *  date : 2020/9/11 11:15 AM
 *  description :
 */
object NotificationFactory {
    fun createNotification(context: Context, channelId: String, channelName: String): Notification {
        return builder(context, channelId, channelName)
            .setContentTitle(channelName)
            .setContentText(channelName)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setWhen(System.currentTimeMillis())
            .build()
    }

    private fun builder(
        context: Context,
        channelId: String,
        channelName: String
    ): Notification.Builder {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            Notification.Builder(context, channelId)
        } else {
            Notification.Builder(context)
        }
    }
}