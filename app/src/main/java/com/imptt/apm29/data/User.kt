package com.imptt.apm29.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 *  author : ciih
 *  date : 2020/9/15 10:05 AM
 *  description :
 */
@Entity(tableName = "t_im_user")
data class User(
    @PrimaryKey @ColumnInfo(name = "id") val userId:Long,
    @ColumnInfo(name = "user_name") val userName:String
)