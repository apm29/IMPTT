package com.imptt.apm29.ui.main

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import com.imptt.apm29.R
import com.imptt.apm29.lifecycle.ServicePTTBinderProxy
import com.imptt.apm29.ui.AudioRecordActivity
import com.imptt.apm29.ui.web.WebViewActivity
import com.imptt.apm29.utilities.InjectUtils
import com.imptt.apm29.viewmodels.MainViewModel
import kotlinx.android.synthetic.main.main_activity.*
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import okio.ByteString
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {


    private val mainViewModel: MainViewModel by viewModels {
        InjectUtils.provideMainViewModelFactory(
            ServicePTTBinderProxy(this,this),
            this
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        mainViewModel.ensureCreate()
        cardView1.setOnClickListener {
            startActivity(Intent(this,AudioRecordActivity::class.java))
        }
        cardView2.setOnClickListener {
            startActivity(Intent(this,WebViewActivity::class.java))
        }

        val okHttpClient = OkHttpClient.Builder()
            .readTimeout(15, TimeUnit.SECONDS)//设置读取超时时间
            .writeTimeout(15, TimeUnit.SECONDS)//设置写的超时时间
            .connectTimeout(15, TimeUnit.SECONDS)//设置连接超时时间
            .retryOnConnectionFailure(true)//断线重连
            .addInterceptor(
                HttpLoggingInterceptor()
            )//添加拦截器
            .build()
        val request: Request = Request.Builder().url("ws://192.168.10.185:8080/ws/123").build()
        //创建webSocket
        val webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                super.onClosed(webSocket, code, reason)
                println("MainActivity.onClosed")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                super.onClosing(webSocket, code, reason)
                println("MainActivity.onClosing")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                super.onFailure(webSocket, t, response)
                println("webSocket = [${webSocket}], t = [${t}], response = [${response}]")
                t.printStackTrace()
                println("MainActivity.onFailure")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                super.onMessage(webSocket, text)
                println("MainActivity.onMessage $text")
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                super.onMessage(webSocket, bytes)
                println("MainActivity.onMessage ${bytes.utf8()}")
            }

            override fun onOpen(webSocket: WebSocket, response: Response) {
                super.onOpen(webSocket, response)
                println("MainActivity.onOpen")
            }
        })
        webSocket.send("123")
    }
}