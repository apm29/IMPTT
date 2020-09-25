package com.imptt.apm29.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import com.imptt.apm29.data.MessageRepository
import com.imptt.apm29.lifecycle.ServicePTTBinderProxy
import com.imptt.apm29.rtc.ImPeerConnection

class MainViewModel(private val context: Context,private val proxy: ServicePTTBinderProxy,private val messageRepository: MessageRepository) : ViewModel() {

    init {
        println("MainViewModel.created")
    }

    fun checkConnection(){
        proxy.checkConnection()
    }
    val peerConnection = ImPeerConnection()

    fun ensureCreate() {
        peerConnection.connectServer(context)
    }
}