package com.imptt.apm29.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 *  author : ciih
 *  date : 2020/9/15 9:11 AM
 *  description :
 */
@Entity(tableName = "t_message")
data class Message(
    @PrimaryKey @ColumnInfo(name = "id") val messageId: Long,
    val description: String,
    @ColumnInfo(name = "created_time")
    val createdTime: Long,
    @ColumnInfo(name = "sender_id")
    val senderId: Long,
    @ColumnInfo(name = "receiver_id")
    val receiverId: Long
)