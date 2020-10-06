package com.imptt.apm29.service

import android.app.Service
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothProfile
import android.content.*
import android.media.MediaPlayer
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.text.TextUtils
import android.view.KeyEvent
import android.widget.Toast
import androidx.media.session.MediaButtonReceiver
import com.imptt.apm29.R
import com.imptt.apm29.utilities.ByteUtils
import java.util.*
import kotlin.concurrent.thread


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
        //mMediaPlayer.start()

        val mbr = ComponentName(packageName, MediaButtonReceiver::class.java.name)
        val mMediaSession = MediaSessionCompat(this, "mbr", mbr, null)
        //一定要设置
        //一定要设置
        mMediaSession.setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS or
                    MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS
        )
        if (!mMediaSession.isActive) {
            mMediaSession.isActive = true
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
                                    else -> {
                                        Toast.makeText(
                                            this@IMService,
                                            "onReceive: $keyCode",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        }
                    }
                }
                return super.onMediaButtonEvent(intent)
            }
        })
        mMediaSession.isActive = true
        MediaButtonReceiver.handleIntent(mMediaSession, intent)


        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val intentFilter = IntentFilter()
        intentFilter.addAction("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED")
        intentFilter.addAction("android.bluetooth.a2dp.profile.action.PLAYING_STATE_CHANGED")
        this.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val state = intent?.getIntExtra("android.bluetooth.profile.extra.STATE", 0)
                println(intent?.action)
                println(state)
            }
        }, intentFilter)
        bluetoothAdapter.getProfileProxy(this, object : BluetoothProfile.ServiceListener {

            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                println("IMService.onServiceConnected")
                println("profile = [${profile}], proxy = [${proxy}]")
                val bluetoothA2dp = proxy as BluetoothA2dp
                val blueDevice = bluetoothA2dp.connectedDevices.firstOrNull {
                    println(it.address)
                    println(it.name)
                    println(it.type)
                    it.name.contains("zmic") || it.name.contains("智咪")
                }
                val gaiaUUID = UUID.fromString("A5E648B6-374D-422D-A7DF-92259D4E7817")

                val socket = blueDevice?.createRfcommSocketToServiceRecord(
                    gaiaUUID
                )
                thread {
                    bluetoothAdapter.cancelDiscovery()
                    socket?.connect()
                    val byteArray = ByteArray(1024)
                    val input = socket?.inputStream
                    while (true){
                        val length = input?.read(byteArray)
                        if(length==null || length<0){
                            continue
                        }
                        scanStream(byteArray,length)
                    }
                }
            }

            override fun onServiceDisconnected(profile: Int) {
                println("IMService.onServiceDisconnected")
                println("profile = [${profile}]")
            }


        }, BluetoothProfile.A2DP)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        mMediaPlayer.stop()
        mMediaPlayer.release()
        stopForeground(true)
    }

    private fun scanStream(data: ByteArray, length: Int) {
        println(ByteUtils.bytesToAscii(data))
    }
}
