package com.imptt.apm29.data

import com.imptt.apm29.utilities.IParamSingleton

/**
 *  author : ciih
 *  date : 2020/9/15 9:22 AM
 *  description :
 */
class MessageRepository private constructor(private val messageDao: MessageDao) {

    init {
        println("MessageRepository.created")
    }


    companion object : IParamSingleton<MessageRepository, MessageDao>() {
        override fun createInstance(param: MessageDao): MessageRepository {
            return MessageRepository(param)
        }
    }

    fun getMessagesToSomebody(receiverId:Long) = messageDao.getAllMessageToReceiverId(receiverId)
}