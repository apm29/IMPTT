package com.imptt.apm29.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import java.lang.UnsupportedOperationException

class IMService : Service() {

    companion object {
        const val NOTIFICATION_ID = 2938
        const val NOTIFICATION_CHANNEL_ID = "对讲服务通知"
        const val NOTIFICATION_CHANNEL_NAME = "对讲服务通知"
    }

    override fun onBind(intent: Intent): IBinder {
        return ServicePTTBinder()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(
            NOTIFICATION_ID, NotificationFactory.createNotification(
                this,
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME
            )
        )
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
    }
}
