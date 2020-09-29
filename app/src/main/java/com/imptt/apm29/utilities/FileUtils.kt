package com.imptt.apm29.utilities

import android.content.Context
import android.os.Environment
import okhttp3.ResponseBody
import java.io.*

/**
 *  author : ciih
 *  date : 2020/9/28 1:30 PM
 *  description :
 */
object FileUtils {

    var audioDirChannel: File? = null
    var audioDirUser: File? = null

    fun initialize(context: Context) {
        audioDirChannel = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.absolutePath
                ?: throw IllegalAccessException("NO EXTERNAL FILE DIRECTORY")
        )
        audioDirUser = File(
            context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.absolutePath
                ?: throw IllegalAccessException("NO EXTERNAL FILE DIRECTORY")
        )
    }

    fun writeResponseBodyToDisk(body: ResponseBody, file: File) {
        try {
            var inputStream: InputStream? = null
            var outputStream: OutputStream? = null
            try {
                val fileReader = ByteArray(4096)
                inputStream = body.byteStream()
                outputStream = FileOutputStream(file)
                while (true) {
                    val read = inputStream.read(fileReader)
                    if (read == -1) {
                        break
                    }
                    outputStream.write(fileReader, 0, read)
                }
                outputStream.flush()
            } catch (e: IOException) {
            } finally {
                inputStream?.close()
                outputStream?.close()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    fun clearAudioDir(dir: File?) {
        if (dir?.isDirectory == true) {
            dir.deleteRecursively()
        }
    }

    var currentAudioDir:File? = null
}