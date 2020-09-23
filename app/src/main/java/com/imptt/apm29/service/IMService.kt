package com.imptt.apm29.service

import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.text.TextUtils
import android.view.KeyEvent
import android.widget.Toast
import androidx.media.session.MediaButtonReceiver
import com.imptt.apm29.R
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

    private val mMediaPlayer: MediaPlayer by lazy {
        MediaPlayer.create(this, R.raw.silence10sec)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(
            NOTIFICATION_ID, NotificationFactory.createNotification(
                this,
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME
            )
        )
        //直接创建，不需要设置setDataSource
        mMediaPlayer.isLooping = true
        mMediaPlayer.start()

        val mbr = ComponentName(packageName, MediaButtonReceiver::class.java.name)
        val mMediaSession = MediaSessionCompat(this, "mbr", mbr, null)
        //一定要设置
        //一定要设置
        mMediaSession.setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        )
        if (!mMediaSession.isActive()) {
            mMediaSession.setActive(true)
        }
        mMediaSession.setCallback(object : MediaSessionCompat.Callback() {

            override fun onMediaButtonEvent(intent: Intent): Boolean {
                println("intent = [${intent}]")
                val action = intent.action
                if (action != null) {
                    if (TextUtils.equals(action, Intent.ACTION_MEDIA_BUTTON)) {
                        val keyEvent = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
                        if (keyEvent != null) {
                            if (keyEvent.action == KeyEvent.ACTION_DOWN) {
                                val keyCode = keyEvent.keyCode
                                when (keyCode) {
                                    KeyEvent.KEYCODE_MEDIA_PLAY -> {
                                        Toast.makeText(
                                            this@IMService,
                                            "onReceive: 播放",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        startForeground(
                                            NOTIFICATION_ID, NotificationFactory.createNotification(
                                                this@IMService,
                                                NOTIFICATION_CHANNEL_ID,
                                                "$NOTIFICATION_CHANNEL_NAME：播放"
                                            )
                                        )
                                    }
                                    KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                                        Toast.makeText(
                                            this@IMService,
                                            "onReceive: 暂停",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        startForeground(
                                            NOTIFICATION_ID, NotificationFactory.createNotification(
                                                this@IMService,
                                                NOTIFICATION_CHANNEL_ID,
                                                "$NOTIFICATION_CHANNEL_NAME：暂停"
                                            )
                                        )
                                    }
                                    KeyEvent.KEYCODE_MEDIA_NEXT -> {
                                        Toast.makeText(
                                            this@IMService,
                                            "onReceive: 下一首",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        startForeground(
                                            NOTIFICATION_ID, NotificationFactory.createNotification(
                                                this@IMService,
                                                NOTIFICATION_CHANNEL_ID,
                                                "$NOTIFICATION_CHANNEL_NAME：下一首"
                                            )
                                        )
                                    }
                                    KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                                        Toast.makeText(
                                            this@IMService,
                                            "onReceive: 上一首",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        startForeground(
                                            NOTIFICATION_ID, NotificationFactory.createNotification(
                                                this@IMService,
                                                NOTIFICATION_CHANNEL_ID,
                                                "$NOTIFICATION_CHANNEL_NAME：上一首"
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                return super.onMediaButtonEvent(intent)
            }
        })
        mMediaSession.setActive(true)
        MediaButtonReceiver.handleIntent(mMediaSession, intent)


        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        mMediaPlayer.stop()
        mMediaPlayer.release()
        stopForeground(true)
    }
}
