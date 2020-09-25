package com.imptt.apm29.ui.ptt.ui.channel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ChannelViewModel : ViewModel() {

    var channels: MutableLiveData<ArrayList<ChannelFragment.Channel>> = MutableLiveData()

    fun getChannel(){
        val data = arrayListOf<ChannelFragment.Channel>()
        data.add(ChannelFragment.Channel("204频道"))
        data.add(ChannelFragment.Channel("206频道"))
        channels.value = data
    }

}