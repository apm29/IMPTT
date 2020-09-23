package com.imptt.apm29.rtc

import android.content.Context
import org.webrtc.*
import org.webrtc.PeerConnection.IceServer
import org.webrtc.PeerConnection.RTCConfiguration
import org.webrtc.PeerConnectionFactory.InitializationOptions
import java.nio.ByteBuffer


/**
 *  author : ciih
 *  date : 2020/9/22 5:11 PM
 *  description :
 */
class ImPeerConnection : CustomPeerConnectionObserver, CustomDataChannelObserver, CustomSdpObserver {

    private lateinit var peerConnection: PeerConnection
    private var dataChannel: DataChannel? = null

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
        peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, this)?: throw IllegalAccessException("建立连接失败")
        //2.创建数据通道
        val dcInit = DataChannel.Init()
        dataChannel = peerConnection.createDataChannel("1", dcInit)
        dataChannel?.registerObserver(this)
    }

    //2.创建offer
    fun createOffer() {
        peerConnection.createOffer(this, MediaConstraints())
    }

    //3.创建answer
    fun createAnswer() {
        peerConnection.createAnswer(this, MediaConstraints())
    }

    fun setRemoteAnswer(sdp: SessionDescription) {
        peerConnection.setRemoteDescription(this,sdp)
    }

    fun sendData(message:String){
        val buffer = ByteBuffer.wrap(message.toByteArray())
        dataChannel?.send(DataChannel.Buffer(buffer,false))
    }

    fun close() {
        dataChannel?.close()
        peerConnection.close()
    }

    //每当WebRTC生成Ice Candidates时，我们都将调用此函数，而我们需要与其他用户共享IceCandidate
    override fun onIceCandidate(candidate: IceCandidate?) {
        super.onIceCandidate(candidate)
        peerConnection.addIceCandidate(candidate)
    }

    //当从另一个对等体收到消息时将被调用
    override fun onMessage(buffer: DataChannel.Buffer?) {
        super.onMessage(buffer)
        // will be called when message is received from another peer
    }

    //创建offer后，我们需要将sessionDescription对象设置为localDescription，
    // 并且sessionDescription.description（它是一个字符串）需要由网络上的另一个对等方共享
    //另一方面，另一个对等方必须将sessionDescription.description（它是一个字符串）添加为remoteDescription，如下面的代码所示。
    //peerConnection.setRemoteDescription(new CustomSdpObserver(), new SessionDescription(SessionDescription.Type.OFFER, sessionDescription));
    // sessionDescription is the same string sent over the network
    //After the answer is created, we need to set sessionDescription object as localDescription and sessionDescription.description (which is a string) needs to be shared by another peer over the network.
    //On the other side, another peer has to add sessionDescription.description (which is a string) as remoteDescription like the below code.
    //创建answer后，我们需要将sessionDescription对象设置为localDescription，
    // 并且sessionDescription.description（它是一个字符串）需要由网络上的另一个对等方共享。
    //另一方面，另一个对等方必须将sessionDescription.description（它是一个字符串）添加为remoteDescription，如下面的代码所示。
    //peerConnection.setRemoteDescription(new CustomSdpObserver(), new SessionDescription(SessionDescription.Type.ANSWER, sessionDescription));
    // sessionDescription is the same string sent over the network
    override fun onCreateSuccess(sessionDescription: SessionDescription?) {
        super.onCreateSuccess(sessionDescription)
        peerConnection.setLocalDescription(this, sessionDescription)
    }
}