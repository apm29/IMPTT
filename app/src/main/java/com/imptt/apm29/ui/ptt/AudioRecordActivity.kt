package com.imptt.apm29.ui.ptt

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.imptt.apm29.R
import com.imptt.apm29.lifecycle.ServicePTTBinderProxy
import com.imptt.apm29.utilities.InjectUtils
import com.imptt.apm29.utilities.RecordUtilities
import com.imptt.apm29.utilities.getTimeFormatText
import com.imptt.apm29.viewmodels.MainViewModel
import com.imptt.apm29.widget.PttButton
import com.permissionx.guolindev.PermissionX
import kotlinx.android.synthetic.main.activity_audio_record.*
import java.io.File
import java.util.*


@Deprecated("NOT USED FOR PTT")
class AudioRecordActivity : AppCompatActivity() {


    private val mainViewModel: MainViewModel by viewModels {
        InjectUtils.provideMainViewModelFactory(
            ServicePTTBinderProxy(this, this),
            this
        )
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.chat_room_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        RecordUtilities.getInstance().clearAudioDirectory(this)
        listFiles()
        return super.onOptionsItemSelected(item)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_record)
        mainViewModel.ensureCreate()
        buttonRecord.pttButtonState = object :PttButton.PttButtonState{
            override fun onPressDown() {
                super.onPressDown()
                val recording =
                    RecordUtilities.getInstance().stopOrStartRecord(this@AudioRecordActivity)
                imageRecord.setImageResource(if (recording) R.mipmap.img_mine_audio_pause else R.mipmap.img_talk)
                if (!recording) {
                    listFiles()
                }
            }

            override fun onPressUp() {
                super.onPressUp()
                val recording =
                    RecordUtilities.getInstance().stopOrStartRecord(this@AudioRecordActivity)
                imageRecord.setImageResource(if (recording) R.mipmap.img_mine_audio_pause else R.mipmap.img_talk)
                if (!recording) {
                    listFiles()
                }
            }
        }
        buttonCall.pttButtonState = object :PttButton.PttButtonState{
            override fun onPressDown() {
                super.onPressDown()
                mainViewModel.peerConnection.call()
            }

            override fun onPressUp() {
                super.onPressUp()
                mainViewModel.peerConnection.stopCall()
            }
        }
        textViewMute.setOnClickListener {
            val mute = mainViewModel.peerConnection.toggleMute()
            textViewMute.text= if(mute) "解除禁言" else "禁言"
        }
//        buttonCall.visibility = View.INVISIBLE
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
        Arrays.sort(listFiles) { a, b ->
            return@sort (a.lastModified() - b.lastModified()).toInt()
        }
        val recordUtilities = RecordUtilities.getInstance()
        val fileInfoList: List<FileInfo> = listFiles.map {
            val length = recordUtilities.getFileDuration(it.absolutePath) / 1000
            val duration = "${length}秒"
            return@map FileInfo(
                duration,
                it,
                length
            )
        }
        if (recyclerView.adapter == null) {
            recyclerView.layoutManager =
                LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
            recyclerView.adapter = MessageAdapter(fileInfoList)
        } else {
            (recyclerView.adapter as? MessageAdapter)?.changeData(fileInfoList)
        }
        recyclerView.scrollToPosition(fileInfoList.size)
    }

    class MessageAdapter(private var listFiles: List<FileInfo>) :
        RecyclerView.Adapter<AudioFileViewHolder>() {

        fun changeData(listFiles: List<FileInfo>) {
            this.listFiles = listFiles
            notifyDataSetChanged()
        }

        companion object {
            const val ItemTypeFooter = 90
        }

        override fun getItemViewType(position: Int): Int {
            if (position == listFiles.size) {
                return ItemTypeFooter
            }
            return super.getItemViewType(position)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AudioFileViewHolder {
            val view: View = if (viewType == ItemTypeFooter) {
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.audio_footer_item, parent, false)
            } else {
                LayoutInflater.from(parent.context).inflate(R.layout.audio_item, parent, false)
            }
            return AudioFileViewHolder(view)
        }

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: AudioFileViewHolder, position: Int) {
            if (getItemViewType(position) == ItemTypeFooter) {
                return
            }
            val file = listFiles[position].file
            val duration = listFiles[position].duration
            val length = listFiles[position].length
            holder.textViewFileLocation?.text = duration
            holder.imageViewPlay?.setOnClickListener {
                RecordUtilities.getInstance().play(file)
            }
            holder.textViewCreateTime?.text =
                getTimeFormatText((file.lastModified() / 1000).toInt())
        }

        override fun getItemCount(): Int {
            return listFiles.size + 1
        }

        override fun getItemId(position: Int): Long {
            if (getItemViewType(position) == ItemTypeFooter) {
                return -100
            }
            return listFiles[position].file.absolutePath.hashCode().toLong()
        }
    }

    data class FileInfo(
        val duration: String,
        val file: File,
        val length: Int
    )

    class AudioFileViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textViewFileLocation: TextView? = this.itemView.findViewById(R.id.textViewFileLocation)
        val imageViewPlay: ImageView? = this.itemView.findViewById(R.id.imageViewPlay)
        val textViewCreateTime: TextView? = this.itemView.findViewById(R.id.textViewCreateTime)
        val linearLayoutMessage: LinearLayout? = this.itemView.findViewById(R.id.linearLayoutMessage)
    }

}