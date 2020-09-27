package com.imptt.apm29.rtc

import com.google.gson.Gson
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
class ImWebSocketClient {

    private lateinit var webSocketClient: WebSocketClient


    data class WsMessage(
        val type: String,
        val from: String,
    )

    interface OnWsMessageObserver{
        fun onWebSocketMessage(message:String)
    }

    var onWsMessage:OnWsMessageObserver? = null

    companion object {
        const val REGISTER = "register"
        const val OFFER = "offer"
        const val CALL = "call"
        const val IN_CALL = "in_call"
        const val CANDIDATE = "candidate"
        const val SUCCESS_CODE = 1
    }

    private val userId = Random().nextInt()
    private var registered: Boolean = false
    private val connected:Boolean
        get() {
            return checkInitialized() && webSocketClient.isOpen
        }

    fun connect( connectedCallback:(()->Unit)? = null ) {
        if (checkInitialized() && connected) {
            if(!registered){
                doRegister()
            }
            return
        } else if (checkInitialized() && !connected) {
            println("reconnect")
            webSocketClient.reconnect()
        } else {
            webSocketClient = object :
                WebSocketClient(URI.create("ws://192.168.10.185:8080/websocket/${userId}")) {
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