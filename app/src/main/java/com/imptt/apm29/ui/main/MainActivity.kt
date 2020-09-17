package com.imptt.apm29.ui.main

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import com.imptt.apm29.R
import com.imptt.apm29.lifecycle.ServicePTTBinderProxy
import com.imptt.apm29.ui.AudioRecordActivity
import com.imptt.apm29.ui.web.WebViewActivity
import com.imptt.apm29.utilities.InjectUtils
import com.imptt.apm29.viewmodels.MainViewModel
import kotlinx.android.synthetic.main.main_activity.*

class MainActivity : AppCompatActivity() {


    private val mainViewModel: MainViewModel by viewModels {
        InjectUtils.provideMainViewModelFactory(
            ServicePTTBinderProxy(this,this),
            this
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        mainViewModel.ensureCreate()
        cardView1.setOnClickListener {
            startActivity(Intent(this,AudioRecordActivity::class.java))
        }
        cardView2.setOnClickListener {
            startActivity(Intent(this,WebViewActivity::class.java))
        }
    }
}