package com.imptt.apm29.api

import androidx.lifecycle.MutableLiveData
import io.reactivex.Single
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Url

/**
 *  author : ciih
 *  date : 2020/9/28 2:45 PM
 *  description :
 */
interface Api{
    /**
     * pic - image
     */
    @POST("/business/upload/uploadFile")
    suspend fun uploadFile(
        @Body image: MultipartBody
    ): BaseResponse<FileDetail>


    //文件下载
    @GET
    suspend fun downloadFile(
        @Url fileUrl: String
    ): ResponseBody

}