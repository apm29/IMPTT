package com.imptt.apm29.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 *  author : ciih
 *  date : 2020/9/15 10:08 AM
 *  description :
 */
@Entity(tableName = "t_im_group")
data class Group(
    @PrimaryKey @ColumnInfo(name = "id") val groupId:Long,
    @ColumnInfo(name = "group_name") val groupName:String
)