package com.imptt.apm29.utilities

import android.content.Context
import com.imptt.apm29.data.IMPTTDatabase
import com.imptt.apm29.data.MessageRepository
import com.imptt.apm29.lifecycle.ServicePTTBinderProxy
import com.imptt.apm29.viewmodels.MainViewModelFactory

/**
 *  author : ciih
 *  date : 2020/9/11 1:16 PM
 *  description :
 */
object InjectUtils {

    private fun getPlantRepository(context: Context): MessageRepository {
        return MessageRepository.getInstance(
            IMPTTDatabase.getInstance(context.applicationContext).messageDao()
        )
    }

    fun provideMainViewModelFactory(proxy: ServicePTTBinderProxy,context:Context): MainViewModelFactory {
        return MainViewModelFactory(proxy, getPlantRepository(context))
    }

}