package com.imptt.apm29.rtc

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import org.json.JSONObject
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
class ImPeerConnection : CustomPeerConnectionObserver, CustomDataChannelObserver, CustomSdpObserver,
    ImWebSocketClient.OnWsMessageObserver {

    private lateinit var peerConnection: PeerConnection
    private lateinit var dataChannel: DataChannel
    private val streamList: ArrayList<String> = arrayListOf()
    private val iceServers: ArrayList<IceServer> = arrayListOf()
    private val eglBase: EglBase by lazy { EglBase.create() }
    private val webSocketClient: ImWebSocketClient by lazy {
        ImWebSocketClient()
    }
    private val peerConnectionFactory: PeerConnectionFactory by lazy {
        val options = PeerConnectionFactory.Options()
        PeerConnectionFactory.builder()
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .setVideoEncoderFactory(
                DefaultVideoEncoderFactory(
                    eglBase.eglBaseContext,
                    true,
                    true
                )
            )
            .setOptions(options)
            .createPeerConnectionFactory()
    }

    fun connectServer(context: Context) {
        webSocketClient.onWsMessage = this
        webSocketClient.connect {
            initialize(context)
        }
    }


    //1.初始化
    private fun initialize(context: Context) {
        //初始化选项
        val initializationOptions: InitializationOptions = InitializationOptions.builder(context)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(initializationOptions)
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
            ?: throw IllegalAccessException(
                "建立连接失败"
            )
        //2.创建数据通道
        val dcInit = DataChannel.Init()
        dataChannel = peerConnection.createDataChannel("channel-dc-${this}", dcInit)
        dataChannel.registerObserver(this)
    }

    //2.创建offer
    private fun createOffer() {
        val mediaConstraints = MediaConstraints()
        mediaConstraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        peerConnection.createOffer(this, mediaConstraints)
    }

    //3.创建answer
    private fun createAnswer() {
        peerConnection.createAnswer(this, MediaConstraints())
    }

    fun setRemoteAnswer(sdp: SessionDescription) {
        peerConnection.setRemoteDescription(this, sdp)
    }

    fun sendData(message: String) {
        val buffer = ByteBuffer.wrap(message.toByteArray())
        dataChannel.send(DataChannel.Buffer(buffer, false))
    }

    fun close() {
        dataChannel.close()
        peerConnection.close()
    }

    //每当WebRTC生成Ice Candidates时，我们都将调用此函数，而我们需要与其他用户共享IceCandidate
    override fun onIceCandidate(candidate: IceCandidate?) {
        super.onIceCandidate(candidate)
        peerConnection.addIceCandidate(candidate)
        Log.d(TAG, "onIceCandidate : " + candidate?.sdp)
        Log.d(
            TAG,
            "onIceCandidate : sdpMid = " + candidate?.sdpMid + " sdpMLineIndex = " + candidate?.sdpMLineIndex
        )
        val text = Gson().toJson(
            mapOf(
                "type" to ImWebSocketClient.CANDIDATE,
                "candidate" to candidate
            )
        )
        Log.d(TAG, "setIceCandidate : $text")
        webSocketClient.send(text)
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
        val localDescription = peerConnection.localDescription
        val type = localDescription.type
        Log.e(TAG, "onCreateSuccess ==  type == $type")
        //接下来使用之前的WebSocket实例将offer发送给服务器
        if (type == SessionDescription.Type.OFFER) {
            //呼叫
            offer(sessionDescription)
        } else if (type == SessionDescription.Type.ANSWER) {
            //应答
            answer(sessionDescription)
        } else if (type == SessionDescription.Type.PRANSWER) {
            //再次应答
        }
    }

    /**
     * 创建本地音频
     */
    private fun startLocalAudioCapture() {
        //语音
        val audioConstraints = MediaConstraints()
        //回声消除
        audioConstraints.mandatory.add(
            MediaConstraints.KeyValuePair(
                "googEchoCancellation",
                "true"
            )
        )
        //自动增益
        audioConstraints.mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
        //高音过滤
        audioConstraints.mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
        //噪音处理
        audioConstraints.mandatory.add(
            MediaConstraints.KeyValuePair(
                "googNoiseSuppression",
                "true"
            )
        )
        val audioSource: AudioSource = peerConnectionFactory.createAudioSource(audioConstraints)
        val audioTrack = peerConnectionFactory.createAudioTrack(
            AUDIO_TRACK_ID,
            audioSource
        )
        val localMediaStream: MediaStream =
            peerConnectionFactory.createLocalMediaStream(LOCAL_AUDIO_STREAM)
        localMediaStream.addTrack(audioTrack)
        audioTrack.setVolume(VOLUME)
        peerConnection.addTrack(audioTrack, streamList)
        peerConnection.addStream(localMediaStream)
    }

    /**
     * 呼叫,发送SessionDescription
     *
     * @param sdpDescription
     */
    private fun offer(sdpDescription: SessionDescription?) {
        //把sdp发送给其他客户端
        val text = Gson().toJson(
            mapOf(
                "type" to ImWebSocketClient.OFFER,
                "sdp" to sdpDescription
            )
        )
        Log.e(TAG, " answer $text")
        webSocketClient.send(text)
    }

    /**
     * 应答,发送SessionDescription
     *
     * @param sdpDescription
     */
    private fun answer(sdpDescription: SessionDescription?) {
        val text = Gson().toJson(
            mapOf(
                "type" to ImWebSocketClient.OFFER,
                "sdp" to sdpDescription
            )
        )
        Log.e(TAG, " answer $text")
        webSocketClient.send(text)
    }


    /**
     * call
     */
    fun call() {
        val text = Gson().toJson(
            mapOf(
                "type" to ImWebSocketClient.CALL,
            )
        )
        Log.e(TAG, " call: $text")
        webSocketClient.send(text)
    }

    override fun onAddStream(stream: MediaStream?) {
        super.onAddStream(stream)
        val audioTracks: List<AudioTrack> = stream?.audioTracks?: arrayListOf()
        if (audioTracks.isNotEmpty()) {
            val audioTrack = audioTracks[0]
            audioTrack.setVolume(VOLUME)
        }
    }

    companion object Constant {
        const val TAG = "ImPeerConnection"
        const val VOLUME: Double = 1.0
        const val LOCAL_AUDIO_STREAM: String = "local_audio_stream_1"
        const val AUDIO_TRACK_ID: String = "audio_track_id_1"
    }

    override fun onWebSocketMessage(message: String) {
        val result = try {
            JSONObject(message)
        } catch (e: Exception) {
            e.printStackTrace()
            JSONObject()
        }
        val type = result.getString("type")
        val success = result.getInt("code") == ImWebSocketClient.SUCCESS_CODE
        if (success) {
            when (type) {
                ImWebSocketClient.IN_CALL -> {
                    createOffer()
                }
                ImWebSocketClient.OFFER -> {
                    //服务端 发送 接收方sdpAnswer
                    val sdpObject = result.getJSONObject("sdp")
                    val sessionDescription =
                        Gson().fromJson(sdpObject.toString(), SessionDescription::class.java)
                    peerConnection.setRemoteDescription(this, sessionDescription)
                    createAnswer()
                }
                ImWebSocketClient.CANDIDATE -> {
                    //服务端 发送 接收方sdpAnswer
                    val candidateObject = result.getJSONObject("candidate")
                    val iceCandidate =
                        Gson().fromJson(candidateObject.toString(), IceCandidate::class.java)
                    if (iceCandidate != null) {
                        peerConnection.addIceCandidate(iceCandidate)
                    }
                }
            }

        }
    }
}