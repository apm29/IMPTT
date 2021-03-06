package com.imptt.apm29.webrtc

/**
 * PeerConnection连接参数
 * Created by chengshaobo on 2018/10/24.
 */
class PeerConnectionParameters(
    val videoCallEnabled: Boolean,
    val loopback: Boolean,
    val tracing: Boolean,
    val videoWidth: Int,
    val videoHeight: Int,
    val videoFps: Int,
    val videoMaxBitrate: Int,
    val videoCodec: String,
    val videoCodecHwAcceleration: Boolean,
    val videoFlexfecEnabled: Boolean,
    val audioStartBitrate: Int,
    val audioCodec: String,
    val noAudioProcessing: Boolean,
    val aecDump: Boolean,
    val saveInputAudioToFile: Boolean,
    val useOpenSLES: Boolean,
    val disableBuiltInAEC: Boolean,
    val disableBuiltInAGC: Boolean,
    val disableBuiltInNS: Boolean,
    val disableWebRtcAGCAndHPF: Boolean,
    val enableRtcEventLog: Boolean
)