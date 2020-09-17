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
interface UserDao {
    @Query("SELECT * FROM t_im_user")
    fun getAllUsers():LiveData<List<User>>
}