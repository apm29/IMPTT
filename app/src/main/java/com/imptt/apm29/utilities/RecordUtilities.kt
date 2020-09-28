package com.imptt.apm29.utilities

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.media.audiofx.Equalizer
import android.media.audiofx.Visualizer
import android.media.audiofx.Visualizer.OnDataCaptureListener
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.File
import java.util.*


/**
 *  author : ciih
 *  date : 2020/9/11 4:41 PM
 *  description :
 */
class RecordUtilities private constructor() {
    companion object : ISingleton<RecordUtilities>() {
        override fun createInstance(): RecordUtilities {
            return RecordUtilities()
        }
    }

    /**
     * MediaRecorder recorder = new MediaRecorder();
    recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
    recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
    recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
    recorder.setOutputFile(PATH_NAME);
    recorder.prepare();
    recorder.start();   // Recording is now started
    ...
    recorder.stop();
    recorder.reset();   // You can reuse the object by going back to setAudioSource() step
    recorder.release(); // Now the object cannot be reused
     */
    private var recorder: MediaRecorder = MediaRecorder()

    private val mHandler: Handler = Handler(Looper.getMainLooper())

    enum class RecordState {
        IDLE, RECORDING
    }

    private var recorderState: RecordState = RecordState.IDLE

    val recording: Boolean
        get() = recorderState == RecordState.RECORDING
    var currentPath: String? = null
    fun startRecord(context: Context) {
        println("RecordUtilities.startRecord")
        if (recorderState == RecordState.RECORDING) {
            return
        }
        println("RecordUtilities.startRecord:Record")
        val path: String = getDefaultAudioDirectory(context) + "/${System.currentTimeMillis()}_self.m4a"
        currentPath = path
        try {
            if (recording) {
                recorder.stop()
                recorder.reset()
                recorder.release()
            }
            recorder = MediaRecorder()
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            recorder.setOutputFile(path)
            recorder.prepare()
            recorder.start()
            recorderState = RecordState.RECORDING
            displayRhythm(true)
            callbackVolume()
        } catch (e: Exception) {
            Log.e("Record Exception", e.toString())
            recorderState = RecordState.IDLE
        }
    }

    fun getDefaultAudioDirectory(context: Context): String {
        return context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.absolutePath
            ?: throw IllegalAccessException("NO EXTERNAL FILE DIRECTORY")
    }

    fun clearAudioDirectory(context: Context) {
        context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.deleteRecursively()
    }

    fun stopOrStartRecord(context: Context): Boolean {
        println("recorderState = $recorderState")
        if (recording) {
            stopRecord()
        } else {
            startRecord(context)
        }
        return recording
    }


    fun stopRecord() {
        println("RecordUtilities.stopRecord")
        println("RecordUtilities.stopRecord:Stop")
        recorderState = try {
            recorder.stop()
            recorder.reset()
            RecordState.IDLE
        } catch (e: Exception) {
            Log.e("Record Exception", e.toString())
            RecordState.IDLE
        }
        displayRhythm(false)
    }


    private var player: MediaPlayer = MediaPlayer()

    enum class PlayState {
        IDLE, PLAYING
    }

    private var playState: PlayState = PlayState.IDLE
    private val playing: Boolean
        get() = playState == PlayState.PLAYING

    fun play(file: File) {
        if (playing) {
            player.stop()
            player.reset()
            player.release()
        }
        player = MediaPlayer()
        player.setDataSource(file.absolutePath)
        player.isLooping = false
        player.prepare()
        player.start()
        playState = PlayState.PLAYING

        val mEqualizer = Equalizer(0, player.audioSessionId)
        mEqualizer.enabled = true
        val audioOutput = Visualizer(player.audioSessionId) // get output audio stream

        audioCallback(audioOutput)
        displayRhythm(true)
        player.setOnCompletionListener {
            player.reset()
            player.release()
            audioOutput.release()
            displayRhythm(false)
            playState = PlayState.IDLE
        }

    }

    private fun audioCallback(
        audioOutput: Visualizer,
    ) {
        val rate = Visualizer.getMaxCaptureRate()
        var intensity = 0F
        var lastSendTime = 0L
        audioOutput.setDataCaptureListener(object : OnDataCaptureListener {
            override fun onWaveFormDataCapture(
                visualizer: Visualizer,
                waveform: ByteArray,
                samplingRate: Int
            ) {
                intensity = (waveform[0].toFloat() + 128f) / 256
                mOnVolumeChangeListener?.let { mOnVolumeChangeListener ->
                    if (System.currentTimeMillis() - lastSendTime > 500) {
                        mHandler.post { mOnVolumeChangeListener.change(intensity * .3F) }
                        lastSendTime = System.currentTimeMillis()
                    }
                }
            }

            override fun onFftDataCapture(
                visualizer: Visualizer,
                fft: ByteArray,
                samplingRate: Int
            ) {
            }
        }, rate, true, false) // waveform not freq data

        Log.d("rate", Visualizer.getMaxCaptureRate().toString())
        audioOutput.enabled = true
    }

    private fun displayRhythm(display: Boolean) {
        mOnVolumeChangeListener?.let { mOnVolumeChangeListener ->
            mHandler.post { mOnVolumeChangeListener.onRhythmStateChange(display) }
        }
    }

    private val mTimer = Timer()
    var mOnVolumeChangeListener: OnVolumeChange? = null

    /**
     * 每隔1秒回调一次音量
     */
    private fun callbackVolume() {
        mTimer.schedule(object : TimerTask() {
            override fun run() {
                if (recording) {
                    val per: Float
                    per = try {
                        //获取音量大小
                        recorder.maxAmplitude / 32767f //最大32767
                    } catch (e: IllegalStateException) {
                        e.printStackTrace()
                        Math.random().toFloat()
                    }
                    mOnVolumeChangeListener?.let { mOnVolumeChangeListener ->
                        mHandler.post { mOnVolumeChangeListener.change(per) }
                    }

                }
            }
        }, 0, 500)
    }

    fun delete(file: File?) {
        file?.let {
            if (file.exists() && file.isFile) {
                file.delete()
            }
        }
    }

    interface OnVolumeChange {
        fun change(percent: Float)
        fun onRhythmStateChange(display: Boolean)
    }

    private var mediaPlayer = MediaPlayer()

    fun getFileDuration(context: Context,file: String): Int {
        if (recording) {
            return -1
        }
        var duration: Int
        try {
            mediaPlayer.reset()
            mediaPlayer.setDataSource(context,Uri.fromFile(File(file)))
            mediaPlayer.prepare()
            duration = mediaPlayer.duration
        } catch (e: Exception) {
            duration = -1
            mediaPlayer = MediaPlayer()
            e.printStackTrace()
        }
        return duration
    }
}