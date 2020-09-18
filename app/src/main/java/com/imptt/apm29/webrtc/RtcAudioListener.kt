package com.imptt.apm29.webrtc

import android.media.AudioTrack

/**
 *  author : ciih
 *  date : 2020/9/18 3:50 PM
 *  description :
 */
interface RtcAudioListener {
    fun onRemoteStreamAdd(peerId: String, audioTrack: AudioTrack)
    fun onRemoteStreamRemove(peerId: String)
    fun clearRemoteAudio()
}