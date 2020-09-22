package com.imptt.apm29.webrtc

import android.content.Context
import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import okhttp3.OkHttpClient
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.*
import org.webrtc.PeerConnection.RTCConfiguration
import org.webrtc.audio.AudioDeviceModule
import org.webrtc.audio.JavaAudioDeviceModule
import org.webrtc.audio.JavaAudioDeviceModule.AudioRecordErrorCallback
import org.webrtc.audio.JavaAudioDeviceModule.AudioTrackErrorCallback
import org.webrtc.voiceengine.WebRtcAudioManager
import java.net.URISyntaxException
import java.security.SecureRandom
import java.util.*
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import kotlin.collections.HashMap

open class WebRtcClient constructor(
    private val appContext: Context,
    private val eglBase: EglBase,
    private val rtcAudioListener: RtcAudioListener,
    private val host: String,
    private val pcParams: PeerConnectionParameters
) {
    private val factory: PeerConnectionFactory by lazy {
        //创建webRtc连接工厂类

        //创建webRtc连接工厂类
        val encoderFactory: VideoEncoderFactory
        val decoderFactory: VideoDecoderFactory
        val enableH264HighProfile = "H264 High" == pcParams.videoCodec
        //音频模式
        //音频模式
        val adm: AudioDeviceModule = createJavaAudioDevice()
        //编解码模式【硬件加速，软编码】
        //编解码模式【硬件加速，软编码】
        if (pcParams.videoCodecHwAcceleration) {
            encoderFactory = DefaultVideoEncoderFactory(
                eglBase.eglBaseContext, true /* enableIntelVp8Encoder */, enableH264HighProfile
            )
            decoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)
        } else {
            encoderFactory = SoftwareVideoEncoderFactory()
            decoderFactory = SoftwareVideoDecoderFactory()
        }
        //PeerConnectionFactory.initialize
        //PeerConnectionFactory.initialize
        var fieldTrials = ""
        if (pcParams.videoFlexfecEnabled) {
            fieldTrials += VIDEO_FLEXFEC_FIELDTRIAL
        }
        fieldTrials += VIDEO_VP8_INTEL_HW_ENCODER_FIELDTRIAL
        if (pcParams.disableWebRtcAGCAndHPF) {
            fieldTrials += DISABLE_WEBRTC_AGC_FIELDTRIAL
        }
        //PeerConnectionFactory.initialize
        //PeerConnectionFactory.initialize
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(appContext)
                .setFieldTrials(fieldTrials)
                .setEnableInternalTracer(true)
                .createInitializationOptions()
        )
        //构建PeerConnectionFactory
        //构建PeerConnectionFactory
        val options = PeerConnectionFactory.Options()
        return@lazy PeerConnectionFactory.builder()
            .setOptions(options)
            .setAudioDeviceModule(adm)
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()
    }
    private val peers: HashMap<String, Peer> = HashMap()
    private val iceServers: LinkedList<PeerConnection.IceServer> = LinkedList()

    private val rtcConfig: PeerConnection.RTCConfiguration by lazy {
        val rtcConfig = RTCConfiguration(iceServers)
        // TCP candidates are only useful when connecting to a server that supports
        // ICE-TCP.
        // TCP candidates are only useful when connecting to a server that supports
        // ICE-TCP.
        rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED
        rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
        rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
        rtcConfig.continualGatheringPolicy =
            PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        // Use ECDSA encryption.
        // Use ECDSA encryption.
        rtcConfig.keyType = PeerConnection.KeyType.ECDSA
        // Enable DTLS for normal calls and disable for loopback calls.
        // Enable DTLS for normal calls and disable for loopback calls.
        rtcConfig.enableDtlsSrtp = !pcParams.loopback
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        return@lazy rtcConfig
    }
    private val audioConstraints: MediaConstraints  by lazy {
        // 音频约束

        // 音频约束
        val audioConstraints = MediaConstraints()
        // added for audio performance measurements
        // added for audio performance measurements
        if (pcParams.noAudioProcessing) {
            Log.d(TAG, "Disabling audio processing")
            audioConstraints.mandatory.add(
                MediaConstraints.KeyValuePair(AUDIO_ECHO_CANCELLATION_CONSTRAINT, "false")
            )
            audioConstraints.mandatory.add(
                MediaConstraints.KeyValuePair(AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT, "false")
            )
            audioConstraints.mandatory.add(
                MediaConstraints.KeyValuePair(AUDIO_HIGH_PASS_FILTER_CONSTRAINT, "false")
            )
            audioConstraints.mandatory.add(
                MediaConstraints.KeyValuePair(AUDIO_NOISE_SUPPRESSION_CONSTRAINT, "false")
            )
        }
        return@lazy audioConstraints
    }
    private val videoConstraints: MediaConstraints  by lazy {
        // 音频约束

        // 音频约束
        val audioConstraints = MediaConstraints()
        // added for audio performance measurements
        // added for audio performance measurements
        if (pcParams.noAudioProcessing) {
            Log.d(TAG, "Disabling audio processing")
            audioConstraints.mandatory.add(
                MediaConstraints.KeyValuePair(AUDIO_ECHO_CANCELLATION_CONSTRAINT, "false")
            )
            audioConstraints.mandatory.add(
                MediaConstraints.KeyValuePair(AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT, "false")
            )
            audioConstraints.mandatory.add(
                MediaConstraints.KeyValuePair(AUDIO_HIGH_PASS_FILTER_CONSTRAINT, "false")
            )
            audioConstraints.mandatory.add(
                MediaConstraints.KeyValuePair(AUDIO_NOISE_SUPPRESSION_CONSTRAINT, "false")
            )
        }
        return@lazy audioConstraints
    }
    private val sdpMediaConstraints: MediaConstraints by lazy {
        //SDP约束 createOffer  createAnswer

        //SDP约束 createOffer  createAnswer
        val sdpMediaConstraints = MediaConstraints()
        sdpMediaConstraints.mandatory.add(
            MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true")
        )
        sdpMediaConstraints.mandatory.add(
            MediaConstraints.KeyValuePair(
                "OfferToReceiveVideo", "true"
            )
        )
        sdpMediaConstraints.optional.add(
            MediaConstraints.KeyValuePair(
                "DtlsSrtpKeyAgreement",
                "true"
            )
        )
        return@lazy sdpMediaConstraints
    }
    private val localAudioSource: AudioSource = factory.createAudioSource(audioConstraints)
    private val localVideoSource: VideoSource = factory.createVideoSource(false)
    private val localAudioTrack: AudioTrack = factory.createAudioTrack("ARDAMSv0", localAudioSource);
    private val localVideoTrack: VideoTrack = factory.createVideoTrack("ARDAMSv0", localVideoSource);
    /** 信令服务器处理相关  */ //created [id,room,peers]
    private val createdListener = Emitter.Listener { args ->
        val data = args[0] as JSONObject
        Log.d(TAG, "created:$data")
        try {
            //设置socket id
            socketId = data.getString("id")
            //设置room id
            roomId = data.getString("room")
            //获取peer数据
            val peers = data.getJSONArray("peers")
            //根据回应peers 循环创建WebRtcPeerConnection，创建成功后发送offer消息 [from,to,room,sdp]
            for (i in 0 until peers.length()) {
                val otherPeer = peers.getJSONObject(i)
                val otherSocketId = otherPeer.getString("id")
                //创建WebRtcPeerConnection
                val pc: Peer = getOrCreateRtcConnect(otherSocketId)
                //设置offer
                pc.pc.createOffer(pc, sdpMediaConstraints)
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    //joined [id,room]
    private val joinedListener = Emitter.Listener { args ->
        val data = args[0] as JSONObject
        Log.d(TAG, "joined:$data")
        try {
            //获取新加入socketId
            val fromId = data.getString("id")
            //构建pcconnection
            getOrCreateRtcConnect(fromId)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    //offer [from,to,room,sdp]
    private val offerListener = Emitter.Listener { args ->
        val data = args[0] as JSONObject
        Log.d(TAG, "offer:$data")
        try {
            //获取id
            val fromId = data.getString("from")
            //获取peer
            val pc: Peer = getOrCreateRtcConnect(fromId)
            //构建RTCSessionDescription参数
            val sdp = SessionDescription(
                SessionDescription.Type.fromCanonicalForm("offer"),
                data.getString("sdp")
            )
            //设置远端setRemoteDescription
            pc.pc.setRemoteDescription(pc, sdp)
            //设置answer
            pc.pc.createAnswer(pc, sdpMediaConstraints)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    //answer [from,to,room,sdp]
    private val answerListener = Emitter.Listener { args ->
        val data = args[0] as JSONObject
        Log.d(TAG, "answer:$data")
        try {
            //获取id
            val fromId = data.getString("from")
            //获取peer
            val pc: Peer = getOrCreateRtcConnect(fromId)
            //构建RTCSessionDescription参数
            val sdp = SessionDescription(
                SessionDescription.Type.fromCanonicalForm("answer"),
                data.getString("sdp")
            )
            //设置远端setRemoteDescription
            pc.pc.setRemoteDescription(pc, sdp)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    //candidate [from,to,room,candidate[sdpMid,sdpMLineIndex,sdp]]
    private val candidateListener = Emitter.Listener { args ->
        val data = args[0] as JSONObject
        Log.d(TAG, "candidate:$data")
        try {
            //获取id
            val fromId = data.getString("from")
            //获取peer
            val pc: Peer = getOrCreateRtcConnect(fromId)
            //获取candidate
            val candidate = data.getJSONObject("candidate")
            val iceCandidate = IceCandidate(
                candidate.getString("sdpMid"),  //描述协议id
                candidate.getInt("sdpMLineIndex"),  //描述协议的行索引
                candidate.getString("sdp") //描述协议
            )

            //添加远端设备路由描述
            pc.pc.addIceCandidate(iceCandidate)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    //exit [from,room]
    private val exitListener = Emitter.Listener { args ->
        val data = args[0] as JSONObject
        Log.d(TAG, "exit:$data")
        try {
            //获取id
            val fromId = data.getString("from")
            //判断是否为当前连接
            val pc = peers[fromId]
            if (pc != null) {
                //peer关闭
                getOrCreateRtcConnect(fromId).pc.close()
                //删除peer对象
                peers.remove(fromId)
                //通知UI界面移除video
                rtcAudioListener.onRemoteStreamRemove(fromId)
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }
    private val client: Socket?  by lazy {
        //socket模式连接信令服务器

        try {
            //普通连接
            //client = IO.socket(host);

            //SSL加密连接
            val okHttpClient = OkHttpClient.Builder()
                .hostnameVerifier { _, _ -> true }
                .sslSocketFactory(getSSLSocketFactory(), TrustAllCerts())
                .build()
            IO.setDefaultOkHttpWebSocketFactory(okHttpClient)
            IO.setDefaultOkHttpCallFactory(okHttpClient)
            val opts = IO.Options()
            opts.callFactory = okHttpClient
            opts.webSocketFactory = okHttpClient
            IO.socket(host, opts).also { client->
                ////设置消息监听
                //created [id,room,peers]
                client.on("created", createdListener)
                //joined [id,room]
                client.on("joined", joinedListener)
                //offer [from,to,room,sdp]
                client.on("offer", offerListener)
                //answer [from,to,room,sdp]
                client.on("answer", answerListener)
                //candidate [from,to,room,candidate[sdpMid,sdpMLineIndex,sdp]]
                client.on("candidate", candidateListener)
                //exit [from,room]
                client.on("exit", exitListener)
                //开始连接
                client.connect()
            }
        } catch (e: URISyntaxException) {
            e.printStackTrace()
            null
        }
    }
    var socketId: String = ""
    var roomId: String = ""


    
    init {
        factory
        iceServers.add(PeerConnection.IceServer.builder("stun:stun.xten.com").createIceServer())
        rtcConfig
        audioConstraints
        sdpMediaConstraints
        client
    }

    //创建音频模式JavaAudioDevice
    private fun createJavaAudioDevice(): AudioDeviceModule {
        // Enable/disable OpenSL ES playback.
        if (!pcParams.useOpenSLES) {
            Log.w(TAG, "External OpenSLES ADM not implemented yet.")
            // TODO(magjed): Add support for external OpenSLES ADM.
        }

        // Set audio record error callbacks.
        val audioRecordErrorCallback: AudioRecordErrorCallback = object : AudioRecordErrorCallback {
            override fun onWebRtcAudioRecordInitError(errorMessage: String) {
                Log.e(TAG, "onWebRtcAudioRecordInitError: $errorMessage")
            }

            override fun onWebRtcAudioRecordStartError(
                errorCode: JavaAudioDeviceModule.AudioRecordStartErrorCode, errorMessage: String
            ) {
                Log.e(
                    TAG,
                    "onWebRtcAudioRecordStartError: $errorCode. $errorMessage"
                )
            }

            override fun onWebRtcAudioRecordError(errorMessage: String) {
                Log.e(TAG, "onWebRtcAudioRecordError: $errorMessage")
            }
        }
        val audioTrackErrorCallback: AudioTrackErrorCallback = object : AudioTrackErrorCallback {
            override fun onWebRtcAudioTrackInitError(errorMessage: String) {
                Log.e(TAG, "onWebRtcAudioTrackInitError: $errorMessage")
            }

            override fun onWebRtcAudioTrackStartError(
                errorCode: JavaAudioDeviceModule.AudioTrackStartErrorCode, errorMessage: String
            ) {
                Log.e(
                    TAG,
                    "onWebRtcAudioTrackStartError: $errorCode. $errorMessage"
                )
            }

            override fun onWebRtcAudioTrackError(errorMessage: String) {
                Log.e(TAG, "onWebRtcAudioTrackError: $errorMessage")
            }
        }
        return JavaAudioDeviceModule.builder(appContext) //.setSamplesReadyCallback(saveRecordedAudioToFile)
            .setUseHardwareAcousticEchoCanceler(!pcParams.disableBuiltInAEC)
            .setUseHardwareNoiseSuppressor(!pcParams.disableBuiltInNS)
            .setAudioRecordErrorCallback(audioRecordErrorCallback)
            .setAudioTrackErrorCallback(audioTrackErrorCallback)
            .createAudioDeviceModule()
    }


    //返回SSLSocketFactory 用于ssl连接
    private fun getSSLSocketFactory(): SSLSocketFactory {
        val ssfFactory: SSLSocketFactory
        try {
            val sc = SSLContext.getInstance("TLS")
            sc.init(null, arrayOf(TrustAllCerts()), SecureRandom())
            ssfFactory = sc.socketFactory
        } catch (e: Exception) {
            throw e
        }
        return ssfFactory
    }

    /** WebRtc相关  */ //构建webRtc连接并返回
    private fun getOrCreateRtcConnect(socketId: String): Peer {
        var pc = peers[socketId]
        if (pc == null) {
            //构建RTCPeerConnection PeerConnection相关回调进入Peer中
            pc = Peer(socketId, factory, rtcConfig, this@WebRtcClient)
            //设置本地数据流
            pc.pc.addTrack(localAudioTrack)
            //保存peer连接
            peers[socketId] = pc
        }
        return pc
    }

    companion object {
        val TAG: String? = WebRtcClient::class.java.canonicalName

        ////webRtc定义常量////
        const val AUDIO_ECHO_CANCELLATION_CONSTRAINT = "googEchoCancellation"
        const val AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT = "googAutoGainControl"
        const val AUDIO_HIGH_PASS_FILTER_CONSTRAINT = "googHighpassFilter"
        const val AUDIO_NOISE_SUPPRESSION_CONSTRAINT = "googNoiseSuppression"
        const val VIDEO_FLEXFEC_FIELDTRIAL =
            "WebRTC-FlexFEC-03-Advertised/Enabled/WebRTC-FlexFEC-03/Enabled/"
        const val VIDEO_VP8_INTEL_HW_ENCODER_FIELDTRIAL = "WebRTC-IntelVP8/Enabled/"
        const val DISABLE_WEBRTC_AGC_FIELDTRIAL = "WebRTC-Audio-MinimizeResamplingOnMobile/Enabled/"
        const val FONT_FACTING = 0
        const val BACK_FACING = 1
    }


    /** UI操作相关  */ //创建并加入
    fun createAndJoinRoom(roomId: String?) {
        //构建信令数据并发送
        try {
            val message = JSONObject()
            message.put("room", roomId)
            //向信令服务器发送信令
            sendMessage("createAndJoinRoom", message)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    //退出room
    fun exitRoom() {
        //信令服务器发送 exit [from room]
        try {
            val message = JSONObject()
            message.put("from", socketId)
            message.put("room", roomId)
            //向信令服务器发送信令
            sendMessage("exit", message)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        //循环遍历 peer关闭
        for (pc in peers.values) {
            pc.pc.close()
        }
        //数据重置
        socketId = ""
        roomId = ""
        peers.clear()
        //通知UI清空远端摄像头
        rtcAudioListener.clearRemoteAudio()
    }

    fun sendMessage(event: String, message: JSONObject) {
        client?.emit(event, message)
    }

    /**
     * 相机相关
     */
    private var type = FONT_FACTING
    private var localRender:VideoSink? = null
    private val cameraVideoCapture by lazy {
        var cameraname: String? = ""
        val camera1Enumerator = Camera1Enumerator()
        val deviceNames = camera1Enumerator.deviceNames
        if (type == FONT_FACTING) {
            //前置摄像头
            for (deviceName in deviceNames) {
                if (camera1Enumerator.isFrontFacing(deviceName)) {
                    cameraname = deviceName
                }
            }
        } else {
            //后置摄像头
            for (deviceName in deviceNames) {
                if (camera1Enumerator.isBackFacing(deviceName)) {
                    cameraname = deviceName
                }
            }
        }
        camera1Enumerator.createCapturer(cameraname, null).also {
            camera1Enumerator.createCapturer(cameraname, null)
            val surfaceTextureHelper =
                SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)
            it.initialize(
                surfaceTextureHelper,
                appContext,
                localVideoSource.capturerObserver
            )
            it.startCapture(
                pcParams.videoWidth,
                pcParams.videoHeight,
                pcParams.videoFps
            )
            localVideoTrack.setEnabled(true)
            localVideoTrack.addSink(localRender)
        }

    }
    //相机
    //启动设备视频并关联本地video
    open fun startCamera(localRender: VideoSink?, type: Int) {
        this.localRender = localRender
        this.type = type
        if (pcParams.videoCallEnabled) {
            cameraVideoCapture.startCapture(
                pcParams.videoWidth,
                pcParams.videoHeight,
                pcParams.videoFps
            )
        }
    }

    //切换摄像头
    fun switchCamera() {
        if (cameraVideoCapture != null) {
            cameraVideoCapture.switchCamera(null)
        }
    }

    //关闭摄像头
    fun closeCamera() {
        if (cameraVideoCapture != null) {
            try {
                cameraVideoCapture.stopCapture()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }


    //音频
    fun startPushToTalk(){
        val audioSource = factory.createAudioSource(MediaConstraints())
    }

    fun stopPushToTalk(){

    }
}
