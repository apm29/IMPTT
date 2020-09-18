package com.imptt.apm29.ui.channel

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.imptt.apm29.R
import com.permissionx.guolindev.PermissionX
import kotlinx.android.synthetic.main.activity_channel_list.*

class ChannelListActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_channel_list)

        doRequestPermissions(arrayListOf(Manifest.permission.RECORD_AUDIO)){
            init()
        }
    }

    private fun init(){
        val data = arrayListOf<Channel>()
        data.add(Channel("204频道"))
        data.add(Channel("206频道"))
        channelList.adapter = object : RecyclerView.Adapter<VH>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
                return VH(layoutInflater.inflate(R.layout.channel_item,parent,false))
            }

            override fun onBindViewHolder(holder: VH, position: Int) {
                holder.textViewChannelName.text = data[position].name
            }

            override fun getItemCount(): Int {
                return  data.size
            }
        }
        channelList.layoutManager = LinearLayoutManager(this)
    }
    data class Channel(
        val name:String,
    )
    class VH(view: View) :RecyclerView.ViewHolder(view){
        val textViewChannelName: TextView = view.findViewById(R.id.textViewChannelName)
    }

    private fun doRequestPermissions(
        permissions: List<String> = arrayListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ),
        onSuccess:()->Unit
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
                    onSuccess.invoke()
                } else {
                    doRequestPermissions(deniedList,onSuccess)
                }
            }
    }
}