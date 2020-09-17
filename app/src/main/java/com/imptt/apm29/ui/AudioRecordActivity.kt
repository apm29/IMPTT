package com.imptt.apm29.ui

import android.Manifest
import android.os.Bundle
import android.os.Environment
import android.view.Display
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.imptt.apm29.R
import com.imptt.apm29.lifecycle.ServicePTTBinderProxy
import com.imptt.apm29.utilities.InjectUtils
import com.imptt.apm29.utilities.RecordUtilities
import com.imptt.apm29.viewmodels.MainViewModel
import com.permissionx.guolindev.PermissionX
import kotlinx.android.synthetic.main.activity_audio_record.*
import java.io.File


class AudioRecordActivity : AppCompatActivity() {


    private val mainViewModel: MainViewModel by viewModels {
        InjectUtils.provideMainViewModelFactory(
            ServicePTTBinderProxy(this, this),
            this
        )
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_record)

        mainViewModel.ensureCreate()
        buttonRecord.setOnClickListener {
            val recording =
                RecordUtilities.getInstance().stopOrStartRecord(this@AudioRecordActivity)
            imageRecord.setImageResource(if (recording) android.R.drawable.ic_media_pause else android.R.drawable.ic_btn_speak_now)
            listFiles()
        }
        RecordUtilities.getInstance().mOnVolumeChangeListener =
            object : RecordUtilities.OnVolumeChange {
                override fun change(percent: Float) {
                    rhythmView.setPerHeight(percent)
                }

                override fun onRhythmStateChange(display: Boolean) {
                    rhythmView.visibility = if (display) View.VISIBLE else View.INVISIBLE
                }
            }
        doRequestPermissions()
    }

    private fun doRequestPermissions(
        permissions: List<String> = arrayListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    ) {
        PermissionX.init(this).permissions(
            permissions
        )
            .explainReasonBeforeRequest()
            .onExplainRequestReason { scope, deniedList ->
                scope.showRequestReasonDialog(
                    deniedList,
                    "App运行需要获取手机内部存储权限以及录音权限！",
                    "好的",
                    "取消"
                )
            }
            .onForwardToSettings { scope, deniedList ->
                scope.showForwardToSettingsDialog(
                    deniedList,
                    "请到设置中心打开所需的权限",
                    "好的",
                    "取消"
                )
            }
            .request { allGranted, _, deniedList ->
                if (allGranted) {
                    listFiles()
                } else {
                    doRequestPermissions(deniedList)
                }
            }
    }

    private fun listFiles() {
        val listFiles =
            File(RecordUtilities.getInstance().getDefaultAudioDirectory(this)).listFiles()
                ?: arrayOf()

        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerView.adapter = object : RecyclerView.Adapter<AudioFileViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AudioFileViewHolder {
                val view: View =
                    LayoutInflater.from(parent.context).inflate(R.layout.audio_item, parent, false)
                return AudioFileViewHolder(view)
            }

            override fun onBindViewHolder(holder: AudioFileViewHolder, position: Int) {
                val file = listFiles[position]
                holder.textViewFileLocation.text = file.absolutePath
                holder.imageViewPlay.setOnClickListener {
                    RecordUtilities.getInstance().play(file)
                }
                holder.imageViewDelete.setOnClickListener {
                    RecordUtilities.getInstance().delete(file)
                    listFiles()
                }
            }

            override fun getItemCount(): Int {
                return listFiles.size
            }
        }

    }

    class AudioFileViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textViewFileLocation: TextView = this.itemView.findViewById(R.id.textViewFileLocation)
        val imageViewPlay: ImageView = this.itemView.findViewById(R.id.imageViewPlay)
        val imageViewDelete: ImageView = this.itemView.findViewById(R.id.imageViewDelete)
    }

}