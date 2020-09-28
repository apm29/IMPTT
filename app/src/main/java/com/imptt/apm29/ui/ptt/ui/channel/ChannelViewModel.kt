package com.imptt.apm29.ui.ptt.ui.channel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.imptt.apm29.R

class ChannelViewModel : ViewModel() {

    var channels: MutableLiveData<ArrayList<ChannelFragment.Channel>> = MutableLiveData()

    fun getChannel(){
        val data = arrayListOf<ChannelFragment.Channel>()
        data.add(ChannelFragment.Channel("204频道"))
        data.add(ChannelFragment.Channel("用户33012", R.mipmap.img_user_register_name))
        channels.value = data
    }

}