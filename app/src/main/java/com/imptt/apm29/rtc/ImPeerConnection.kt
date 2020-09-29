package com.imptt.apm29.rtc

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.google.gson.Gson
import com.imptt.apm29.api.Api
import com.imptt.apm29.api.FileDetail
import com.imptt.apm29.api.RetrofitManager
import com.imptt.apm29.utilities.FileUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.webrtc.*
import org.webrtc.PeerConnection.IceServer
import org.webrtc.PeerConnection.RTCConfiguration
import org.webrtc.PeerConnectionFactory.InitializationOptions
import org.webrtc.audio.AudioDeviceModule
import org.webrtc.audio.JavaAudioDeviceModule
import java.io.File
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext


/**
 *  author : ciih
 *  date : 2020/9/22 5:11 PM
 *  description :
 */
class ImPeerConnection : CustomPeerConnectionObserver, CustomDataChannelObserver, CustomSdpObserver,
    ImWebSocketClient.OnWsMessageObserver, CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO
    private lateinit var peerConnection: PeerConnection
    private lateinit var dataChannel: DataChannel
    private val streamList: ArrayList<String> = arrayListOf()
    private val iceServers: ArrayList<IceServer> by lazy {
        val arr = arrayListOf<IceServer>()
        arr.add(
            IceServer.builder("stun:stun2.1.google.com:19302")
                .createIceServer()
        )
        arr.add(
            IceServer.builder("turn:numb.viagenie.ca")
                .setUsername("webrtc@live.com").setPassword("muazkh").createIceServer()
        )
        arr
    }
    private val eglBase: EglBase by lazy { EglBase.create() }
    private val mHandler: Handler = Handler(Looper.getMainLooper())
    private val webSocketClient: ImWebSocketClient by lazy {
        ImWebSocketClient.getInstance()
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
            .setAudioDecoderFactoryFactory(
                BuiltinAudioDecoderFactoryFactory()
            )
            .setAudioDeviceModule(
                createJavaAudioDevice()
            )
            .setOptions(options)
            .createPeerConnectionFactory()
    }
    lateinit var context: Context

    var inCallObserver: CustomPeerConnectionObserver? = null
    var audioDir: File? = null
    fun connectServer(
        context: Context,
        audioDir: File?,
        inCallObserver: CustomPeerConnectionObserver? = null
    ) {
        this.audioDir = audioDir
        audioSampleProcessor.audioDir = audioDir
        this.inCallObserver = inCallObserver
        webSocketClient.onWsMessage = this
        this.context = context
        webSocketClient.connect()
    }

    private val executor = Executors.newSingleThreadExecutor()
    val audioSampleProcessor: AudioSampleProcessor by lazy {
        AudioSampleProcessor(executor)
    }

    //1.初始化
    private fun initialize(send: Boolean) {
        //初始化选项
        val initializationOptions: InitializationOptions = InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(initializationOptions)
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
        startCaptureLocalAudio(send)
    }

    //2.创建offer
    private fun createOffer() {
        val mediaConstraints = MediaConstraints()
        mediaConstraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        peerConnection.createOffer(this, mediaConstraints)
    }

    override fun onIceConnectionChange(iceConnectionState: PeerConnection.IceConnectionState?) {
        super.onIceConnectionChange(iceConnectionState)
        mHandler.post {
            peerConnectionObserver?.onIceConnectionChange(iceConnectionState)
            if (!calling) {
                inCallObserver?.onIceConnectionChange(iceConnectionState)
            }
        }
        if (iceConnectionState == PeerConnection.IceConnectionState.CONNECTED) {
            mHandler.post {
                Toast.makeText(context, "链接建立", Toast.LENGTH_SHORT).show()
            }
        } else if (iceConnectionState == PeerConnection.IceConnectionState.DISCONNECTED) {
            mHandler.post {
                Toast.makeText(context, "链接断开", Toast.LENGTH_SHORT).show()
            }
        }
    }


    //3.创建answer
    private fun createAnswer() {
        peerConnection.createAnswer(this, MediaConstraints())
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

    private var localTrackId = AUDIO_TRACK_ID + "${Random().nextInt()}"

    private fun startCaptureLocalAudio(send: Boolean) {
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
        val audioSource = peerConnectionFactory.createAudioSource(audioConstraints)
        //重新生成trackId
        localTrackId = AUDIO_TRACK_ID + "${Random().nextInt()}"
        val audioTrack = peerConnectionFactory.createAudioTrack(localTrackId, audioSource)
        val localMediaStream = peerConnectionFactory.createLocalMediaStream(LOCAL_AUDIO_STREAM)
        localMediaStream.addTrack(audioTrack)
        audioTrack.setVolume(VOLUME)
        if (send) {
            try {
                peerConnection.addTrack(audioTrack, streamList)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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


    private var peerConnectionObserver: CustomPeerConnectionObserver? = null
    private var calling = false

    interface FileObserver {
        fun onFileChange()
    }

    var fileObserver: FileObserver? = null

    /**
     * call
     */
    fun call(
        peerConnectionObserver: CustomPeerConnectionObserver? = null,
        muted: () -> Unit
    ) {
        if (mutedByServer) {
            muted.invoke()
            return
        }
        val text = Gson().toJson(
            mapOf(
                "type" to ImWebSocketClient.CALL,
            )
        )
        this.peerConnectionObserver = peerConnectionObserver
        Log.e(TAG, " call: $text")
        webSocketClient.send(text)
        initialize(true)
        calling = true
        audioSampleProcessor.start()
    }

    //关闭P2Pa
    fun stopCall() {
        if (!calling) {
            return
        }
        audioSampleProcessor.stop()
        calling = false
        peerConnectionObserver?.onIceConnectionChange(PeerConnection.IceConnectionState.DISCONNECTED)
        peerConnectionObserver = null
        peerConnection.dispose()
    }

    override fun onAddStream(stream: MediaStream?) {
        super.onAddStream(stream)
        Log.e(TAG, "OnAddStream:$stream")
        val audioTracks: List<AudioTrack> = stream?.audioTracks ?: arrayListOf()
        audioTracks.forEach {
            println("AudioTrack = ${it.id()}--${it}")
            println("localTrackId = $localTrackId")
            if (it.id() === localTrackId) {
                it.setVolume(0.0)
            }
        }
    }

    companion object Constant {
        const val TAG = "ImPeerConnection"
        const val VOLUME: Double = 30.0
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
                ImWebSocketClient.REGISTER -> {
                    //call()
                    val text = Gson().toJson(
                        mapOf(
                            "type" to ImWebSocketClient.GET,
                        )
                    )
                    webSocketClient.send(text)
                }
                ImWebSocketClient.GET -> {
                    val value = result.getJSONObject("value")
                    mutedByServer = value.getBoolean("mute")
                    val count = value.getInt("count")
                    mHandler.post {
                        channelMuteObserver?.onMuteChange(mutedByServer)
                        Toast.makeText(context, "当前在线人数$count", Toast.LENGTH_SHORT).show()
                    }
                }
                ImWebSocketClient.AUDIO_FILE -> {
                    val value = result.getJSONObject("value")
                    val path = value.getString("filePath")
                    launch {
                        val responseBody =
                            RetrofitManager.getInstance().retrofit.create(Api::class.java)
                                .downloadFile(path)
                        FileUtils.writeResponseBodyToDisk(
                            responseBody, File(
                                audioDir,
                                "${System.currentTimeMillis()}.pcm"
                            )
                        )
                        mHandler.post {
                            fileObserver?.onFileChange()
                        }
                    }
                }
                ImWebSocketClient.IN_CALL -> {
                    if (mute) {
                        return
                    }
                    initialize(false)
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
                ImWebSocketClient.MUTE -> {
                    mutedByServer = result.getBoolean("value")
                    mHandler.post {
                        channelMuteObserver?.onMuteChange(mutedByServer)
                    }
                }
            }

        }
    }

    fun muteChannel() {
        val text = Gson().toJson(
            mapOf(
                "type" to ImWebSocketClient.MUTE,
            )
        )
        webSocketClient.send(text)
    }

    fun sendAudioFileData(file: FileDetail) {
        val text = Gson().toJson(
            mapOf(
                "type" to ImWebSocketClient.AUDIO_FILE,
                "value" to file
            )
        )
        webSocketClient.send(text)
    }

    interface ChannelMuteObserver {
        fun onMuteChange(muted: Boolean)
    }

    var channelMuteObserver: ChannelMuteObserver? = null


    //创建音频模式JavaAudioDevice
    private fun createJavaAudioDevice(): AudioDeviceModule {
        // Set audio record error callbacks.
        val audioRecordErrorCallback: JavaAudioDeviceModule.AudioRecordErrorCallback = object :
            JavaAudioDeviceModule.AudioRecordErrorCallback {
            override fun onWebRtcAudioRecordInitError(errorMessage: String) {
                Log.e(TAG, "onWebRtcAudioRecordInitError: $errorMessage")
            }

            override fun onWebRtcAudioRecordStartError(
                errorCode: JavaAudioDeviceModule.AudioRecordStartErrorCode, errorMessage: String
            ) {
                Log.e(
                    TAG, "onWebRtcAudioRecordStartError: $errorCode. $errorMessage"
                )
            }

            override fun onWebRtcAudioRecordError(errorMessage: String) {
                Log.e(TAG, "onWebRtcAudioRecordError: $errorMessage")
            }
        }
        val audioTrackErrorCallback: JavaAudioDeviceModule.AudioTrackErrorCallback = object :
            JavaAudioDeviceModule.AudioTrackErrorCallback {
            override fun onWebRtcAudioTrackInitError(errorMessage: String) {
                Log.e(TAG, "onWebRtcAudioTrackInitError: $errorMessage")
            }

            override fun onWebRtcAudioTrackStartError(
                errorCode: JavaAudioDeviceModule.AudioTrackStartErrorCode, errorMessage: String
            ) {
                Log.e(
                    TAG, "onWebRtcAudioTrackStartError: $errorCode. $errorMessage"
                )
            }

            override fun onWebRtcAudioTrackError(errorMessage: String) {
                Log.e(TAG, "onWebRtcAudioTrackError: $errorMessage")
            }
        }
        return JavaAudioDeviceModule.builder(context)
            .setSamplesReadyCallback(audioSampleProcessor)
            .setUseHardwareAcousticEchoCanceler(true)
            .setUseHardwareNoiseSuppressor(true)
            .setAudioRecordErrorCallback(audioRecordErrorCallback)
            .setAudioTrackErrorCallback(audioTrackErrorCallback)
            .createAudioDeviceModule()
    }

    //静音
    private var mute: Boolean = false
    var mutedByServer: Boolean = false
    fun toggleMute(): Boolean {
        mute = !mute
        return mute
    }
}