package com.imptt.apm29.utilities

import android.media.*
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.Socket
import java.util.*
import kotlin.concurrent.thread


/**
 *  author : ciih
 *  date : 2020/9/27 9:30 AM
 *  description :
 */
object SocketStreamUtils {
    private val minBufferSize by lazy {
        AudioRecord.getMinBufferSize(
            8000,
            AudioFormat.CHANNEL_CONFIGURATION_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
    }
    private val mAudioRecord:AudioRecord by lazy {
        AudioRecord(
            MediaRecorder.AudioSource.MIC,
            8000,
            AudioFormat.CHANNEL_CONFIGURATION_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBufferSize
        )
    }
    private  val mInBytes = ByteArray(minBufferSize)

    private lateinit var mSockOut:Socket
    private lateinit var mDataOutPut : DataOutputStream
    private var run:Boolean = true
    var mInQueue: LinkedList<ByteArray> = LinkedList<ByteArray>()

    fun startCall() {
        thread {
            mSockOut = Socket("192.168.10.106", 5566)
            mDataOutPut = DataOutputStream(mSockOut.getOutputStream())
            run = true
            var bytePack:ByteArray
            mAudioRecord.startRecording()
            while (run){
                mAudioRecord.read(mInBytes, 0, minBufferSize)
                bytePack = mInBytes.clone()
                if(mInQueue.size >= 2){
                    mDataOutPut.write(mInQueue.removeFirst(), 0, mInQueue.removeFirst().size)
                }
                mInQueue.add(bytePack)
            }
            mAudioRecord.stop()
        }
    }
}

object SocketReceiverUtils{
    private val mAudioTrack: AudioTrack by lazy {
        AudioTrack(
            AudioManager.STREAM_MUSIC, 8000,
            AudioFormat.CHANNEL_CONFIGURATION_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            mMinOutBufferSize,
            AudioTrack.MODE_STREAM
        )
    }
    private val mMinOutBufferSize: Int by lazy {
        AudioTrack.getMinBufferSize(
            8000,
            AudioFormat.CHANNEL_CONFIGURATION_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
    }
    lateinit var mOutBytes: ByteArray
    var run = false
    private lateinit var mInSocket: Socket
    private lateinit var mDataInput: DataInputStream
    fun startListen() {
        thread {
            try {
                mInSocket = Socket("192.168.10.190", 5566)
                mDataInput = DataInputStream(mInSocket.getInputStream())
                run = true
                mOutBytes = ByteArray(mMinOutBufferSize)

                var bytesPack: ByteArray
                mAudioTrack.play()
                while (run) {
                    try {
                        mDataInput.read(mOutBytes)
                        bytesPack = mOutBytes.clone()
                        mAudioTrack.write(bytesPack, 0, bytesPack.size)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                mAudioTrack.stop()
                try {
                    mDataInput.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


}