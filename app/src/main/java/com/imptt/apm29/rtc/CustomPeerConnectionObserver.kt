package com.imptt.apm29.rtc

import org.webrtc.*


/**
 *  author : ciih
 *  date : 2020/9/22 5:08 PM
 *  description :
 */
interface CustomPeerConnectionObserver : PeerConnection.Observer {
    override fun onSignalingChange(state: PeerConnection.SignalingState?) {
        println("CustomPeerConnectionObserver.onSignalingChange")
        println("state = [${state}]")
    }

    /**
     * onIceConnectionChange: This function will be called whenever the connection state is changed.
     * It can be used to check the status of peer to peer connection.
     *
     * onIceConnectionChange：每当连接状态更改时，将调用此函数。它可用于检查对等连接的状态。
     */
    override fun onIceConnectionChange(iceConnectionState: PeerConnection.IceConnectionState?) {
        println("CustomPeerConnectionObserver.onIceConnectionChange")
        println("state = [${iceConnectionState}]")
        if (iceConnectionState == PeerConnection.IceConnectionState.CONNECTED) {
            // when connection is established
            println("链接建立")
        }
        if (iceConnectionState == PeerConnection.IceConnectionState.CLOSED || iceConnectionState == PeerConnection.IceConnectionState.FAILED || iceConnectionState == PeerConnection.IceConnectionState.DISCONNECTED) {
            // when connection is failed or closed
            println("链接断开、失败")
        }
    }

    override fun onIceConnectionReceivingChange(receiving: Boolean) {
        println("CustomPeerConnectionObserver.onIceConnectionReceivingChange")
        println("receiving = [${receiving}]")
    }

    override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {
        println("CustomPeerConnectionObserver.onIceGatheringChange")
        println("state = [${state}]")
    }

    /**
     * This function will be called whenever WebRTC will generate Ice Candidates and we need to share the IceCandidate with another user
     * 每当WebRTC生成Ice Candidates时，我们都将调用此函数，而我们需要与其他用户共享IceCandidate
     *
     * Now you must be wondering about how to send this custom object over the network. IceCandidate consist of three different values:
     * 现在，您一定想知道如何通过网络发送此自定义对象。 IceCandidate由三个不同的值组成：
     *
     * iceCandidate.sdp; // string value (which you can share over network)
     * iceCandidate.sdpMid; // String value (which is shareable)
     * iceCandidate.sdpMLineIndex; //Integer value(which is also shareable)
     *
     * 另一方面，需要添加其他应用发送的IceCandidate，并且可以使用以下代码添加它。
     * peerConnection.addIceCandidate(new IceCandidate(sdpmid, sdpmlineIndex, candidate));
     */
    override fun onIceCandidate(candidate: IceCandidate?) {
        println("CustomPeerConnectionObserver.onIceCandidate")
        println("candidate = [${candidate}]")
    }

    override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {
        println("CustomPeerConnectionObserver.onIceCandidatesRemoved")
        println("candidates = [${candidates}]")
    }

    override fun onAddStream(stream: MediaStream?) {
        println("CustomPeerConnectionObserver.onAddStream")
        println("stream = [${stream}]")
    }

    override fun onRemoveStream(stream: MediaStream?) {
        println("CustomPeerConnectionObserver.onRemoveStream")
        println("stream = [${stream}]")
    }

    /**
     * onDataChannel: This function will be called when another peer will create a data channel.
     * After the data channel is created, we can send string data by using the below code.
     * 当另一个对等方创建数据通道时，将调用此函数。创建数据通道后，我们可以使用以下代码发送字符串数据。
     * ByteBuffer buffer = ByteBuffer.wrap(message.getBytes()); // message is string
     * dataChannel.send(new DataChannel.Buffer(buffer, false));
     */
    override fun onDataChannel(channel: DataChannel?) {
        println("CustomPeerConnectionObserver.onDataChannel")
        println("channel = [${channel}]")
    }


    override fun onRenegotiationNeeded() {
        println("CustomPeerConnectionObserver.onRenegotiationNeeded")
    }

    override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
        println("CustomPeerConnectionObserver.onAddTrack")
        println("receiver = [${receiver}], streams = [${streams}]")
    }
}