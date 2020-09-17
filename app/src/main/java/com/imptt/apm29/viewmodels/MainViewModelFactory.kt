package com.imptt.apm29.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.imptt.apm29.data.MessageRepository
import com.imptt.apm29.lifecycle.ServicePTTBinderProxy

/**
 *  author : ciih
 *  date : 2020/9/11 1:13 PM
 *  description :
 */
@Suppress("UNCHECKED_CAST")
class MainViewModelFactory(
    private val proxy: ServicePTTBinderProxy,
    private val messageRepository: MessageRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return MainViewModel(proxy,messageRepository) as T
    }
}