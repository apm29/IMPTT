package com.imptt.apm29.rtc

import com.google.gson.Gson
import com.imptt.apm29.utilities.ISingleton
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONObject
import java.net.URI
import java.util.*


/**
 *  author : ciih
 *  date : 2020/9/25 1:32 PM
 *  description :
 */
class ImWebSocketClient private constructor(){

    private lateinit var webSocketClient: WebSocketClient


    data class WsMessage(
        val type: String,
        val from: String,
    )

    interface OnWsMessageObserver{
        fun onWebSocketMessage(message:String)
        fun onWebSocketClose(reason:String)
    }

    var onWsMessage:OnWsMessageObserver? = null

    companion object : ISingleton<ImWebSocketClient>() {

        const val REGISTER = "register"
        const val OFFER = "offer"
        const val CALL = "call"
        const val MUTE = "mute"
        const val AUDIO_FILE = "audio_file"
        const val IN_CALL = "in_call"
        const val CANDIDATE = "candidate"
        const val GET = "get"
        const val SUCCESS_CODE = 1
        override fun createInstance(): ImWebSocketClient {
            return ImWebSocketClient()
        }
    }

    private val userId = Random().nextInt()
    private var registered: Boolean = false
    private val connected:Boolean
        get() {
            return checkInitialized() && webSocketClient.isOpen
        }

    fun connect( connectedCallback:(()->Unit)? = null ) {
        println("registered = $registered")
        println("connected = $connected")
        println("checkInitialized = ${checkInitialized()}")
        if (checkInitialized() && connected) {
            if(!registered){
                doRegister()
            }
            return
        } else if (checkInitialized() && !connected) {
            println("reconnect")
            webSocketClient.connect()
        } else {
            webSocketClient = object :
                WebSocketClient(URI.create("ws://jwttest.ciih.net/talk/websocket/${userId}")) {
                override fun onOpen(handshakedata: ServerHandshake) {
                    println(
                        "onOpen == Status == ${handshakedata.httpStatus} StatusMessage == ${handshakedata.httpStatusMessage}"
                    )
                    doRegister()
                }

                override fun onMessage(message: String) {
                    println("收到信令服务器消息：$message")
                    val result = try {
                        JSONObject(message)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        JSONObject()
                    }
                    val register = result.getString("type")
                    val success = result.getInt("code") == SUCCESS_CODE
                    println("type = $register")
                    println("success = $success")
                    if (register == REGISTER && success) {
                        registered = true
                        connectedCallback?.invoke()
                    }
                    onWsMessage?.onWebSocketMessage(message)
                }

                override fun onClose(code: Int, reason: String?, remote: Boolean) {
                    println("ImWebSocketClient.onClose")
                    println("code = [${code}], reason = [${reason}], remote = [${remote}]")
                    registered = false
                    onWsMessage?.onWebSocketClose(reason?:"信令服务关闭")
                }

                override fun onError(ex: Exception?) {
                    println("ex = [${ex}]")
                    ex?.printStackTrace()
                }

            }
            println("connect")
            webSocketClient.connect()
        }

    }

    private fun doRegister() {
        println("register")
        val model = WsMessage(
            type = REGISTER,
            from = userId.toString(),
        )
        send(Gson().toJson(model))
    }

    private fun checkInitialized(): Boolean {
        return this::webSocketClient.isInitialized
    }

    fun close() {
        if (connected) {
            webSocketClient.close()
        }
    }

    fun send(text: String) {
        if(connected) {
            webSocketClient.send(text)
        }
    }

}