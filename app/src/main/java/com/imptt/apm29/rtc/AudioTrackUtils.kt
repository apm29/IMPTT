package com.imptt.apm29.rtc

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager.AUDIO_SESSION_ID_GENERATE
import android.media.AudioTrack
import android.media.audiofx.Equalizer
import android.media.audiofx.Visualizer
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.File
import java.io.FileInputStream


/**
 *  author : ciih
 *  date : 2020/9/28 1:37 PM
 *  description :
 */
object AudioTrackUtils {
    private const val sampleRate = 48000
    private fun createAudioTrack(): AudioTrack {
        val sampleRate = AudioTrack.getNativeOutputSampleRate(AudioTrack.MODE_STREAM)
        println("sampleRate = $sampleRate")
        val format = AudioFormat.Builder()
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .setSampleRate(sampleRate)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .build()
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            AudioTrack.Builder()
                .setAudioAttributes(
                    attributes
                )
                .setAudioFormat(format)
                .build()
        } else {
            AudioTrack(
                attributes, format, AudioTrack.getMinBufferSize(
                    sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ),
                AudioTrack.MODE_STREAM, AUDIO_SESSION_ID_GENERATE
            )
        }
    }

    var audioTrack: AudioTrack = createAudioTrack()
    private val mHandler: Handler = Handler(Looper.getMainLooper())

    interface OnVolumeChange {
        fun change(percent: Float)
        fun onRhythmStateChange(display: Boolean)
    }

    var onVolumeChange: OnVolumeChange? = null

    fun playFile(file: File) {
        onVolumeChange?.onRhythmStateChange(true)
        audioTrack.stop()
        audioTrack.release()
        audioTrack = createAudioTrack()
        audioTrack.setVolume(1.0f)
        val tempBuffer = ByteArray(sampleRate * 1)
        var readCount: Int
        val dis = FileInputStream(file)
        //声音可视化
        while (dis.available() > 0) {
            readCount = dis.read(tempBuffer)
            if (readCount == AudioTrack.ERROR_INVALID_OPERATION || readCount == AudioTrack.ERROR_BAD_VALUE) {
                continue
            }
            if (readCount != 0 && readCount != -1) {
                audioTrack.play()
                val count = audioTrack.write(tempBuffer, 0, readCount)
                onVolumeChange?.change(count / sampleRate * 1.0f)
            }
        }
        onVolumeChange?.onRhythmStateChange(false)
    }

    /**
     * 可见数字音频文件大小的计算公式为:数据量Byte=
    采样频率Hz
    ×（采样位数/8）
    × 声道数
    × 时间s[例]如果采样频率为44.1kHz，分辨率为16位，立体声，录音时
    间为10s，符合CD音质的声音文件的大小是多少？
    根据计算公式：
    数据量Byte= 44100Hz×(16/8)×2×10s=1764KByte

    PCM文件大小=采样率采样时间采样位深/8*通道数（Bytes）
     */
    fun getFileDuration(file: File): Double {
        println(file.length())
        //一路16bitPCM 48000Hz
        return file.length() / 1.0 / (16 / 8) / sampleRate * 1000
    }
}