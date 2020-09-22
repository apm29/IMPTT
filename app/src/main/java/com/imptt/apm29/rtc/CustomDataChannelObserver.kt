package com.imptt.apm29.rtc

import org.webrtc.DataChannel

/**
 *  author : ciih
 *  date : 2020/9/22 5:07 PM
 *  description :
 */
interface CustomDataChannelObserver : DataChannel.Observer {
    override fun onBufferedAmountChange(amount: Long) {
        println("CustomDataChannelObserver.onBufferedAmountChange")
        println("amount = [${amount}]")
    }

    override fun onStateChange() {
        println("CustomDataChannelObserver.onStateChange")
    }

    /**
     * 当从另一个对等体(peer)收到消息时将被调用
     * 当其他对等方发送任何数据时，将调用onMessage函数
     */
    override fun onMessage(buffer: DataChannel.Buffer?) {
        println("CustomDataChannelObserver.onMessage")
        println("buffer = [${buffer}]")
    }
}