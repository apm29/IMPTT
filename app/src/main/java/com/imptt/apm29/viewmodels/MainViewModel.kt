package com.imptt.apm29.viewmodels

import androidx.lifecycle.ViewModel
import com.imptt.apm29.data.MessageRepository
import com.imptt.apm29.lifecycle.ServicePTTBinderProxy

class MainViewModel(private val proxy: ServicePTTBinderProxy,private val messageRepository: MessageRepository) : ViewModel() {

    init {
        println("MainViewModel.created")
    }

    fun checkConnection(){
        proxy.checkConnection()
    }

    fun ensureCreate() {

    }
}