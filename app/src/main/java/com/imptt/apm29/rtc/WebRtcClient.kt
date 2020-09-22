package com.imptt.apm29.rtc

import android.content.Context
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import org.webrtc.PeerConnection.IceServer
import org.webrtc.PeerConnection.RTCConfiguration
import org.webrtc.PeerConnectionFactory
import org.webrtc.PeerConnectionFactory.InitializationOptions


/**
 *  author : ciih
 *  date : 2020/9/22 5:11 PM
 *  description :
 */
class WebRtcClient :CustomPeerConnectionObserver,CustomDataChannelObserver{

    private var peerConnection:PeerConnection? = null
    private var dataChannel:DataChannel? = null

    //1.初始化
    fun initialize(context: Context) {
        //初始化选项
        val initializationOptions: InitializationOptions = InitializationOptions.builder(context)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(initializationOptions)
        val options = PeerConnectionFactory.Options()

        val peerConnectionFactory =
            PeerConnectionFactory.builder().setOptions(options).createPeerConnectionFactory()
        val iceServers: ArrayList<IceServer> = arrayListOf()
        //stun 服务器
        iceServers.add(
            IceServer.builder("stun:stun2.1.google.com:19302")
                .createIceServer()
        )
        //turn 服务器
        iceServers.add(
            IceServer.builder("turn:numb.viagenie.ca")
                .setUsername("webrtc@live.com").setPassword("muazkh").createIceServer()
        )
        val rtcConfig = RTCConfiguration(iceServers)

        //1.创建对等连接
        peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, this)
        if(peerConnection==null){
            throw IllegalAccessException("建立连接失败")
        }
        //2.创建数据通道
        val dcInit = DataChannel.Init()
        dataChannel = peerConnection?.createDataChannel("1", dcInit)
        dataChannel?.registerObserver(this)
    }

    //2.创建offer
    fun createOffer(){

    }

    //每当WebRTC生成Ice Candidates时，我们都将调用此函数，而我们需要与其他用户共享IceCandidate
    override fun onIceCandidate(candidate: IceCandidate?) {
        super.onIceCandidate(candidate)
        peerConnection?.addIceCandidate(candidate)
    }

    //当从另一个对等体收到消息时将被调用
    override fun onMessage(buffer: DataChannel.Buffer?) {
        super.onMessage(buffer)
        // will be called when message is received from another peer
    }
}