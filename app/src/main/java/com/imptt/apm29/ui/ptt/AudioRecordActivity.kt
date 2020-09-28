package com.imptt.apm29.ui.ptt

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.imptt.apm29.R
import com.imptt.apm29.api.Api
import com.imptt.apm29.api.RetrofitManager
import com.imptt.apm29.lifecycle.ServicePTTBinderProxy
import com.imptt.apm29.rtc.CustomPeerConnectionObserver
import com.imptt.apm29.rtc.ImPeerConnection
import com.imptt.apm29.utilities.InjectUtils
import com.imptt.apm29.utilities.RecordUtilities
import com.imptt.apm29.utilities.getTimeFormatText
import com.imptt.apm29.viewmodels.MainViewModel
import com.imptt.apm29.widget.PttButton
import com.permissionx.guolindev.PermissionX
import kotlinx.android.synthetic.main.activity_audio_record.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.webrtc.PeerConnection
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
        when (item.itemId) {
            R.id.menu_clear -> {
                RecordUtilities.getInstance().clearAudioDirectory(this)
                listFiles()
            }
            R.id.menu_refresh -> {
                listFiles()
            }
        }
        return super.onOptionsItemSelected(item)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RecordUtilities.getInstance().clearAudioDirectory(this)
        setContentView(R.layout.activity_audio_record)
        mainViewModel.checkConnection(
            object : CustomPeerConnectionObserver {
                override fun onIceConnectionChange(iceConnectionState: PeerConnection.IceConnectionState?) {
                    if (iceConnectionState == PeerConnection.IceConnectionState.CONNECTED) {
                        cardPlaying.visibility = View.VISIBLE
                    } else if (iceConnectionState == PeerConnection.IceConnectionState.DISCONNECTED
                        || iceConnectionState == PeerConnection.IceConnectionState.CLOSED
                        || iceConnectionState == PeerConnection.IceConnectionState.FAILED
                    ) {
                        cardPlaying.visibility = View.GONE
                    }
                }
            }
        )
        title = intent.extras?.getString("title") ?: "Demo"
        buttonRecord.pttButtonState = object : PttButton.PttButtonState {
            override fun onPressDown() {
                super.onPressDown()
                mainViewModel.peerConnection.call(
                    object : CustomPeerConnectionObserver {
                        override fun onIceConnectionChange(iceConnectionState: PeerConnection.IceConnectionState?) {
                            if (iceConnectionState == PeerConnection.IceConnectionState.CONNECTED) {
                                cardRecord.visibility = View.VISIBLE
                                RecordUtilities.getInstance()
                                    .startRecord(this@AudioRecordActivity)
                                imageRecord.setImageResource(if (RecordUtilities.getInstance().recording) R.mipmap.img_mine_audio_pause else R.mipmap.img_talk)
                            } else if (iceConnectionState == PeerConnection.IceConnectionState.DISCONNECTED
                                || iceConnectionState == PeerConnection.IceConnectionState.CLOSED
                                || iceConnectionState == PeerConnection.IceConnectionState.FAILED
                            ) {
                                cardRecord.visibility = View.GONE
                            }
                        }
                    }
                ) {
                    val makeText =
                        Toast.makeText(this@AudioRecordActivity, "禁言中，请联系管理员", Toast.LENGTH_SHORT)
                    makeText.view = layoutInflater.inflate(R.layout.mute_toast_layout, null)
                    makeText.setGravity(Gravity.CENTER, 0, 0)
                    makeText.show()
                }
            }

            override fun onPressUp() {
                super.onPressUp()
                val instance = RecordUtilities.getInstance()
                mainViewModel.peerConnection.stopCall()
                instance.stopRecord()
                imageRecord.setImageResource(if (instance.recording) R.mipmap.img_mine_audio_pause else R.mipmap.img_talk)
                if (!instance.recording) {
                    listFiles()
                }
                mainViewModel.uploadFile(instance.currentPath)
            }
        }
        buttonCall.pttButtonState = object : PttButton.PttButtonState {
            override fun onPressDown() {
                super.onPressDown()
                mainViewModel.peerConnection.call(
                    object : CustomPeerConnectionObserver {
                        override fun onIceConnectionChange(iceConnectionState: PeerConnection.IceConnectionState?) {
                            if (iceConnectionState == PeerConnection.IceConnectionState.CONNECTED) {
                                cardRecord.visibility = View.VISIBLE
                            } else if (iceConnectionState == PeerConnection.IceConnectionState.DISCONNECTED
                                || iceConnectionState == PeerConnection.IceConnectionState.CLOSED
                                || iceConnectionState == PeerConnection.IceConnectionState.FAILED
                            ) {
                                cardRecord.visibility = View.GONE
                            }
                        }
                    }
                ) {
                    val makeText =
                        Toast.makeText(this@AudioRecordActivity, "禁言中，请联系管理员", Toast.LENGTH_SHORT)
                    makeText.view = layoutInflater.inflate(R.layout.mute_toast_layout, null)
                    makeText.setGravity(Gravity.CENTER, 0, 0)
                    makeText.show()
                }
            }

            override fun onPressUp() {
                super.onPressUp()
                mainViewModel.peerConnection.stopCall()
                listFiles()
            }
        }
        buttonCall.visibility = View.GONE
        textViewMute.text = if (mainViewModel.peerConnection.mutedByServer) "解除" else "禁言"
        mainViewModel.peerConnection.channelMuteObserver =
            object : ImPeerConnection.ChannelMuteObserver {
                override fun onMuteChange(muted: Boolean) {
                    textViewMute.text = if (muted) "解除" else "禁言"
                }
            }
        mainViewModel.peerConnection.fileObserver =
            object : ImPeerConnection.FileObserver {
                override fun onFileChange() {
                    listFiles()
                }
            }
        textViewMute.setOnClickListener {
            mainViewModel.peerConnection.muteChannel()
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
            val length = recordUtilities.getFileDuration(this, it.absolutePath) / 1000
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
            const val ItemTypeOther = 91
        }

        override fun getItemViewType(position: Int): Int {
            if (position == listFiles.size) {
                return ItemTypeFooter
            }
            if (!listFiles[position].file.name.contains("_self")) {
                return ItemTypeOther
            }
            return super.getItemViewType(position)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AudioFileViewHolder {
            val view: View = when (viewType) {
                ItemTypeFooter -> {
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.audio_footer_item, parent, false)
                }
                ItemTypeOther -> {
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.audio_item_other, parent, false)
                }
                else -> {
                    LayoutInflater.from(parent.context).inflate(R.layout.audio_item, parent, false)
                }
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
        val linearLayoutMessage: LinearLayout? =
            this.itemView.findViewById(R.id.linearLayoutMessage)
    }

}