package com.imptt.apm29.receiver

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.imptt.apm29.service.IMService

class BootstrapReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if(intent.action === Manifest.permission.RECEIVE_BOOT_COMPLETED){
            context.startService(
                Intent(context,IMService::class.java)
            )
        }
    }
}
