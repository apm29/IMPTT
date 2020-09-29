package com.imptt.apm29.ui.ptt.ui.channel

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.imptt.apm29.R
import com.imptt.apm29.ui.ptt.AudioRecordActivity
import com.imptt.apm29.utilities.FileUtils
import kotlinx.android.synthetic.main.fragment_channel.*

class ChannelFragment : Fragment() {

    private lateinit var channelViewModel: ChannelViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        channelViewModel =
            ViewModelProviders.of(this).get(ChannelViewModel::class.java)
        return inflater.inflate(R.layout.fragment_channel, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        channelViewModel.channels.observe(requireActivity()){
            init(it)
        }
        channelViewModel.getChannel()
    }

    private fun init(data:ArrayList<Channel>){

        channelList.adapter = object : RecyclerView.Adapter<VH>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
                return VH(layoutInflater.inflate(R.layout.channel_item,parent,false))
            }

            override fun onBindViewHolder(holder: VH, position: Int) {
                holder.textViewChannelName.text = data[position].name
                holder.imageViewChannelIcon.setImageResource(data[position].icon)
                holder.itemView.setOnClickListener {
                    val intent = Intent(requireContext(), AudioRecordActivity::class.java)
                    intent.putExtra("title",data[position].name)
                    intent.putExtra("audioDir",if(data[position].name.contains("用户"))FileUtils.audioDirUser else FileUtils.audioDirChannel)
                    startActivity(intent)
                }

            }

            override fun getItemCount(): Int {
                return  data.size
            }
        }
        channelList.layoutManager = LinearLayoutManager(requireContext())
    }
    data class Channel(
        val name:String,
        val icon:Int = R.mipmap.img_address_group_icon
    )
    class VH(view: View) : RecyclerView.ViewHolder(view){
        val textViewChannelName: TextView = view.findViewById(R.id.textViewChannelName)
        val imageViewChannelIcon: ImageView = view.findViewById(R.id.imageViewChannelIcon)
        val buttonListenChannel: View = view.findViewById(R.id.buttonListenChannel)
        val buttonTalkChannel: View = view.findViewById(R.id.buttonTalkChannel)
    }
}