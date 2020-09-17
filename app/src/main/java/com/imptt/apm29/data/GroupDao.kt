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
interface GroupDao {
    @Query("SELECT * FROM t_im_group")
    fun getAllGroups():LiveData<List<Group>>
}