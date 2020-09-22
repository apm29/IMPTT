package com.imptt.apm29.rtc

import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

/**
 *  author : ciih
 *  date : 2020/9/22 5:04 PM
 *  description :
 */
interface CustomSDPObserver : SdpObserver {
    override fun onCreateSuccess(sessionDescription: SessionDescription?) {
        println("CustomSDPObserver.onCreateSuccess")
        println("sessionDescription = [${sessionDescription}]")
    }

    override fun onSetSuccess() {
       println("CustomSDPObserver.onSetSuccess")
    }

    override fun onCreateFailure(reason: String?) {
        println("CustomSDPObserver.onCreateFailure")
        println("reason = [${reason}]")
    }

    override fun onSetFailure(reason: String?) {
        println("CustomSDPObserver.onSetFailure")
        println("reason = [${reason}]")
    }
}