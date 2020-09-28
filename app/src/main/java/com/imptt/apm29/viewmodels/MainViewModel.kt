package com.imptt.apm29.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import com.imptt.apm29.api.Api
import com.imptt.apm29.api.FileDetail
import com.imptt.apm29.api.RetrofitManager
import com.imptt.apm29.data.MessageRepository
import com.imptt.apm29.lifecycle.ServicePTTBinderProxy
import com.imptt.apm29.rtc.CustomPeerConnectionObserver
import com.imptt.apm29.rtc.ImPeerConnection
import io.reactivex.functions.Consumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

class MainViewModel(private val context: Context,private val proxy: ServicePTTBinderProxy,private val messageRepository: MessageRepository) : ViewModel(),
    CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main
    private val threadContext: CoroutineContext =
        Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    init {
        println("MainViewModel.created")
    }

    fun checkConnection(inCallObserver: CustomPeerConnectionObserver?=null){
        peerConnection.connectServer(context,inCallObserver)
    }


    fun uploadFile(currentPath: String?) {
        currentPath?.run {
            val file = File(currentPath)
            val apiKt = RetrofitManager.getInstance().retrofit.create(Api::class.java)
            launch(threadContext){
                val fileDetail = apiKt.uploadFile(
                    MultipartBody
                        .Builder()
                        .addFormDataPart(
                            "file",
                            file.name,
                            RequestBody.create("multipart/form-data".toMediaTypeOrNull(), file)
                        )
                        .build()
                ).data
                peerConnection.sendAudioFileData(fileDetail)
            }

        }
    }

    val peerConnection = ImPeerConnection()

}