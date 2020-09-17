package com.imptt.apm29.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query

/**
 *  author : ciih
 *  date : 2020/9/15 9:14 AM
 *  description :
 */
@Dao
interface MessageDao {

    @Query("SELECT * FROM t_message WHERE receiver_id = :receiverId")
    fun getAllMessageToReceiverId(receiverId:Long):LiveData<List<Message>>
}