package com.imptt.apm29.ui.ptt

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import com.imptt.apm29.lifecycle.ServicePTTBinderProxy
import com.imptt.apm29.rtc.AudioTrackUtils
import com.imptt.apm29.rtc.CustomPeerConnectionObserver
import com.imptt.apm29.rtc.ImPeerConnection
import com.imptt.apm29.utilities.FileUtils
import com.imptt.apm29.utilities.InjectUtils
import com.imptt.apm29.utilities.RecordUtilities
import com.imptt.apm29.utilities.getTimeFormatText
import com.imptt.apm29.viewmodels.MainViewModel
import com.imptt.apm29.widget.PttButton
import com.itsmartreach.libzm.ZmCmdLink
import com.permissionx.guolindev.PermissionX
import kotlinx.android.synthetic.main.activity_audio_record.*
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

    val mHandler = Handler(Looper.getMainLooper())

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.chat_room_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_clear -> {
                FileUtils.clearAudioDir(FileUtils.currentAudioDir)
                listFiles()
            }
            R.id.menu_refresh -> {
                listFiles()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    var user = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        user = title.contains("用户")
        FileUtils.currentAudioDir = if(user) FileUtils.audioDirUser else FileUtils.audioDirChannel
        buttonRecord.pttButtonState = object : PttButton.PttButtonState {
            override fun onPressDown() {
                super.onPressDown()
                mainViewModel.peerConnection.call(
                    object : CustomPeerConnectionObserver {
                        override fun onIceConnectionChange(iceConnectionState: PeerConnection.IceConnectionState?) {
                            if (iceConnectionState == PeerConnection.IceConnectionState.CONNECTED) {
                                cardRecord.visibility = View.VISIBLE
                                //RecordUtilities.getInstance()
                                //    .startRecord(this@AudioRecordActivity)
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
                //instance.stopRecord()
                imageRecord.setImageResource(if (instance.recording) R.mipmap.img_mine_audio_pause else R.mipmap.img_talk)
                if (!instance.recording) {
                    listFiles()
                }
                if(!mainViewModel.peerConnection.mutedByServer) {
                    mainViewModel.uploadFile()
                }
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
//        RecordUtilities.getInstance().mOnVolumeChangeListener =
//            object : RecordUtilities.OnVolumeChange {
//                override fun change(percent: Float) {
//                    rhythmView.setPerHeight(percent)
//                }
//
//                override fun onRhythmStateChange(display: Boolean) {
//                    rhythmView.visibility = if (display) View.VISIBLE else View.INVISIBLE
//                }
//            }
        rhythmLayout.visibility = View.GONE
//        AudioTrackUtils.onVolumeChange = object :AudioTrackUtils.OnVolumeChange{
//            override fun change(percent: Float) {
//                mHandler.post {
//                    println("onVolumeChange percent = [${percent}]")
//                    println("AudioRecordActivity.change ${Thread.currentThread()}")
//                    rhythmLayout.visibility = View.GONE
//                    rhythmLayout.visibility = View.VISIBLE
//                    rhythmView.setPerHeight(percent)
//                }
//            }
//
//            override fun onRhythmStateChange(display: Boolean) {
//                mHandler.post {
//                   println("onVolumeChange display = [${display}]")
//                   println("AudioRecordActivity.change ${Thread.currentThread()}")
//                    rhythmLayout.visibility = View.GONE
//                    rhythmLayout.visibility = if (display) View.VISIBLE else View.GONE
//               }
//            }
//        }



        doRequestPermissions()

        println("zmLink.isConnected:${zmLink.isConnected}")
    }

    private val audioManager :AudioManager by lazy {
        (getSystemService(Context.AUDIO_SERVICE) as AudioManager)
    }
    private val zmLink :ZmCmdLink by lazy {
        ZmCmdLink(this, object : ZmCmdLink.ZmEventListener {
            override fun onScoStateChanged(sco: Boolean) {
                println("AudioRecordActivity.onScoStateChanged")
                println("sco = [${sco}]")
//                if (sco) {
//                    println("enterSppStandbyMode")
//                    zmLink.enterSppStandbyMode()
//                } else {
//                    println("enterSppMode")
//                    zmLink.enterSppMode()
//                }
            }

            override fun onSppStateChanged(spp: Boolean) {
                println("AudioRecordActivity.onSppStateChanged")
                println("spp = [${spp}]")
//                if (!spp) {
//                    zmLink.enterSpeakMode()
//                }
                Toast.makeText(
                    this@AudioRecordActivity,
                    if (spp) "连接蓝牙肩咪成功" else "连接蓝牙肩咪失败",
                    Toast.LENGTH_SHORT
                ).show()
            }

            override fun onUserEvent(p0: ZmCmdLink.ZmUserEvent?) {
                println("AudioRecordActivity.onUserEvent")
                println("p0 = [${p0}]")
                if (p0 == ZmCmdLink.ZmUserEvent.zmEventPttPressed) {
                    audioManager.startBluetoothSco()
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
                            Toast.makeText(
                                this@AudioRecordActivity,
                                "禁言中，请联系管理员",
                                Toast.LENGTH_SHORT
                            )
                        makeText.view = layoutInflater.inflate(R.layout.mute_toast_layout, null)
                        makeText.setGravity(Gravity.CENTER, 0, 0)
                        makeText.show()
                    }
                } else if (p0 == ZmCmdLink.ZmUserEvent.zmEventPttReleased) {
                    audioManager.stopBluetoothSco()
                    val instance = RecordUtilities.getInstance()
                    mainViewModel.peerConnection.stopCall()
                    //instance.stopRecord()
                    imageRecord.setImageResource(if (instance.recording) R.mipmap.img_mine_audio_pause else R.mipmap.img_talk)
                    if (!instance.recording) {
                        listFiles()
                    }
                    if (!mainViewModel.peerConnection.mutedByServer) {
                        mainViewModel.uploadFile()
                    }
                }
            }

            override fun onBatteryLevelChanged(p0: Int) {
            }

            override fun onVolumeChanged(p0: Boolean) {
            }
        }, true)
    }

    override fun onDestroy() {
        super.onDestroy()
        zmLink.destroy()
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
            FileUtils.currentAudioDir?.listFiles()
                ?: arrayOf()
        Arrays.sort(listFiles) { a, b ->
            return@sort (a.lastModified() - b.lastModified()).toInt()
        }
        val fileInfoList: List<FileInfo> = listFiles.map {
            val length = AudioTrackUtils.getFileDuration(it) / 1000
            val duration = "${length.toInt()}秒"
            return@map FileInfo(
                duration,
                it,
                length.toInt()
            )
        }
        if (recyclerView.adapter == null) {
            recyclerView.layoutManager =
                LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
            recyclerView.adapter = MessageAdapter(fileInfoList,user)
        } else {
            (recyclerView.adapter as? MessageAdapter)?.changeData(fileInfoList)
        }
        recyclerView.scrollToPosition(fileInfoList.size)
    }

    class MessageAdapter(private var listFiles: List<FileInfo>,private val oneOnOne:Boolean) :
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
            if (getItemViewType(position) == ItemTypeOther && !oneOnOne ) {
                holder.imageViewAvatar?.setImageResource(
                    if((position % 3) == 1 || (position % 4) == 2)
                        R.mipmap.img_address_new_friend_icon
                    else
                        R.mipmap.ic_launcher_round
                )
            }
            val file = listFiles[position].file
            val duration = listFiles[position].duration
            val length = listFiles[position].length
            holder.textViewFileLocation?.text = duration
            holder.imageViewPlay?.setOnClickListener {
                AudioTrackUtils.playFile(file)
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
        val imageViewAvatar: ImageView? = this.itemView.findViewById(R.id.imageViewAvatar)
        val textViewCreateTime: TextView? = this.itemView.findViewById(R.id.textViewCreateTime)
        val linearLayoutMessage: LinearLayout? =
            this.itemView.findViewById(R.id.linearLayoutMessage)
    }

}