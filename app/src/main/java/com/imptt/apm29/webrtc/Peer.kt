package com.imptt.apm29.webrtc

import android.util.Log
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.*

/**
 *  author : ciih
 *  date : 2020/9/18 3:27 PM
 *  description :
 */
class Peer constructor(
    private val id: String,
    factory: PeerConnectionFactory,
    rctConfig: PeerConnection.RTCConfiguration,
    private val webRtcClient: WebRtcClient
):SdpObserver,PeerConnection.Observer {
    
    companion object{
        private val TAG = Peer::class.java.canonicalName
    }

    val pc:PeerConnection = factory.createPeerConnection(rctConfig, this)!!

    /**SdpObserver是来回调sdp是否创建(offer,answer)成功，是否设置描述成功(local,remote）的接口**/
    //Create{Offer,Answer}成功回调
    override fun onCreateSuccess(sdp: SessionDescription) {
        val type: String = sdp.type.canonicalForm()
        Log.d(TAG, "onCreateSuccess $type")
        //设置本地LocalDescription
        //设置本地LocalDescription
        pc.setLocalDescription(this@Peer, sdp)
        //构建信令数据
        //构建信令数据
        try {
            val message = JSONObject()
            message.put("from", webRtcClient.socketId)
            message.put("to", id)
            message.put("room", webRtcClient.roomId)
            message.put("sdp", sdp.description)
            //向信令服务器发送信令
            webRtcClient.sendMessage(type, message)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }
    //Set{Local,Remote}Description()成功回调
    override fun onSetSuccess() {
       
    }
    //Create{Offer,Answer}失败回调
    override fun onCreateFailure(p0: String?) {
       
    }
    //Set{Local,Remote}Description()失败回调
    override fun onSetFailure(p0: String?) {
       
    }

    /**SdpObserver是来回调sdp是否创建(offer,answer)成功，是否设置描述成功(local,remote）的接口**/
    //信令状态改变时候触发
    override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
       
    }
    //IceConnectionState连接状态改变时候触发
    override fun onIceConnectionChange(iceConnectionState: PeerConnection.IceConnectionState?) {
        Log.d(TAG, "onIceConnectionChange $iceConnectionState")
        if (iceConnectionState == PeerConnection.IceConnectionState.DISCONNECTED) {
            /** ice连接中断处理  */
        }
    }
    //IceConnectionState连接接收状态改变
    override fun onIceConnectionReceivingChange(p0: Boolean) {
       
    }
    //IceConnectionState网络信息获取状态改变
    override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
       
    }
    //新ice地址被找到触发
    override fun onIceCandidate(iceCandidate: IceCandidate) {
        Log.d(TAG, "onIceCandidate " + iceCandidate.sdpMid)
        try {
            //构建信令数据
            val message = JSONObject()
            message.put("from", webRtcClient.socketId)
            message.put("to", id)
            message.put("room", webRtcClient.roomId)
            //candidate参数
            val candidate = JSONObject()
            candidate.put("sdpMid", iceCandidate.sdpMid)
            candidate.put("sdpMLineIndex", iceCandidate.sdpMLineIndex)
            candidate.put("sdp", iceCandidate.sdp)
            message.put("candidate", candidate)
            //向信令服务器发送信令
            webRtcClient.sendMessage("candidate", message)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }
    //ice地址被移除掉触发
    override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {
       
    }
    //Peer连接远端音视频数据到达时触发 注：用onTrack回调代替
    override fun onAddStream(mediaStream: MediaStream) {
        Log.d(TAG, "onAddStream " + mediaStream.id)
    }
    //Peer连接远端音视频数据移除时触发
    override fun onRemoveStream(mediaStream: MediaStream) {
        Log.d(TAG, "onRemoveStream " + mediaStream.id)
    }
    //Peer连接远端开启数据传输通道时触发
    override fun onDataChannel(dataChannel: DataChannel?) {
       
    }
    //通道交互协议需要重新协商时触发
    override fun onRenegotiationNeeded() {
       
    }
    //Triggered when a new track is signaled by the remote peer, as a result of setRemoteDescription.
    override fun onAddTrack(rtpReceiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {
       
    }

    override fun onTrack(transceiver: RtpTransceiver?) {
        val track = transceiver!!.receiver.track()
        Log.d(TAG, "onTrack " + track!!.id())
        if (track is VideoTrack) {
            //webRtcClient.getRtcListener().onAddRemoteStream(id, track as VideoTrack?)
        }
    }
}