package com.imptt.apm29.rtc

import android.media.AudioFormat
import android.os.Environment
import android.util.Log
import com.imptt.apm29.utilities.FileUtils
import org.webrtc.audio.JavaAudioDeviceModule
import org.webrtc.audio.JavaAudioDeviceModule.SamplesReadyCallback
import org.webrtc.voiceengine.WebRtcAudioRecord
import org.webrtc.voiceengine.WebRtcAudioRecord.WebRtcAudioRecordSamplesReadyCallback
import java.io.*
import java.util.*
import java.util.concurrent.ExecutorService

/**
 *  author : ciih
 *  date : 2020/9/28 10:39 AM
 *  description :
 */
/*
 *  Copyright 2018 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */
/**
 * Implements the AudioRecordSamplesReadyCallback interface and writes
 * recorded raw audio samples to an output file.
 */
class AudioSampleProcessor(executor: ExecutorService) :
    SamplesReadyCallback, WebRtcAudioRecordSamplesReadyCallback {
    private val lock = Any()
    private val executor: ExecutorService
    private var rawAudioFileOutputStream: OutputStream? = null
    private var isRunning = false
    private var fileSizeInBytes: Long = 0

    /**
     * Should be called on the same executor thread as the one provided at
     * construction.
     */
    fun start(): Boolean {
        Log.d(TAG, "start")
        if (!isExternalStorageWritable) {
            Log.e(TAG, "Writing to external media is not possible")
            return false
        }
        synchronized(lock) { isRunning = true }
        return true
    }

    /**
     * Should be called on the same executor thread as the one provided at
     * construction.
     */
    fun stop() {
        Log.d(TAG, "stop")
        synchronized(lock) {
            isRunning = false
            if (rawAudioFileOutputStream != null) {
                try {
                    rawAudioFileOutputStream!!.close()
                } catch (e: IOException) {
                    Log.e(
                        TAG,
                        "Failed to close file with saved input audio: $e"
                    )
                }
                rawAudioFileOutputStream = null
            }
            fileSizeInBytes = 0
        }
    }

    // Checks if external storage is available for read and write.
    private val isExternalStorageWritable: Boolean
        get() {
            val state = Environment.getExternalStorageState()
            return Environment.MEDIA_MOUNTED == state
        }

    // Utilizes audio parameters to create a file name which contains sufficient
    // information so that the file can be played using an external file player.
    // Example: /sdcard/recorded_audio_16bits_48000Hz_mono.pcm.
    var currentPath:String? = null
    private fun openRawAudioOutputFile(sampleRate: Int, channelCount: Int) {
        val fileName =
            "${FileUtils.currentAudioDir?.path}${File.separator}bits16_${sampleRate}Hz${if (channelCount == 1) "_mono" else "_stereo"}_${System.currentTimeMillis()}_self.pcm"
        val outputFile = File(fileName)
        try {
            rawAudioFileOutputStream = FileOutputStream(outputFile)
            currentPath = fileName
        } catch (e: FileNotFoundException) {
            Log.e(TAG, "Failed to open audio output file: " + e.message)
        }
        Log.d(
            TAG,
            "Opened file for recording: $fileName"
        )
    }

    // Called when new audio samples are ready.
    override fun onWebRtcAudioRecordSamplesReady(samples: WebRtcAudioRecord.AudioSamples) {
        onWebRtcAudioRecordSamplesReady(
            JavaAudioDeviceModule.AudioSamples(
                samples.audioFormat,
                samples.channelCount, samples.sampleRate, samples.data
            )
        )
    }

    // Called when new audio samples are ready.
    override fun onWebRtcAudioRecordSamplesReady(samples: JavaAudioDeviceModule.AudioSamples) {
        // The native audio layer on Android should use 16-bit PCM format.
        if (samples.audioFormat != AudioFormat.ENCODING_PCM_16BIT) {
            Log.e(TAG, "Invalid audio format")
            return
        }
        synchronized(lock) {

            // Abort early if stop() has been called.
            if (!isRunning) {
                return
            }
            // Open a new file for the first callback only since it allows us to add audio parameters to
            // the file name.
            if (rawAudioFileOutputStream == null) {
                openRawAudioOutputFile(samples.sampleRate, samples.channelCount)
                fileSizeInBytes = 0
            }
        }
        // Append the recorded 16-bit audio samples to the open output file.
        executor.execute {
            if (rawAudioFileOutputStream != null) {
                try {
                    // Set a limit on max file size. 58348800 bytes corresponds to
                    // approximately 10 minutes of recording in mono at 48kHz.
                    if (fileSizeInBytes < MAX_FILE_SIZE_IN_BYTES) {
                        // Writes samples.getData().length bytes to output stream.
                        rawAudioFileOutputStream!!.write(samples.data)
                        fileSizeInBytes += samples.data.size.toLong()
                    }
                } catch (e: IOException) {
                    Log.e(
                        TAG,
                        "Failed to write audio to file: " + e.message
                    )
                }
            }
        }
    }

    companion object {
        private const val TAG = "RecordedAudioToFile"
        private const val MAX_FILE_SIZE_IN_BYTES = 58348800L
    }

    init {
        Log.d(TAG, "ctor")
        this.executor = executor
    }
}