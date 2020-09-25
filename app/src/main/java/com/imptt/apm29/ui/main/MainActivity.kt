package com.imptt.apm29.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.imptt.apm29.R
import com.imptt.apm29.lifecycle.ServicePTTBinderProxy
import com.imptt.apm29.ui.ptt.AudioRecordActivity
import com.imptt.apm29.ui.ptt.PttActivity
import com.imptt.apm29.ui.web.WebViewActivity
import com.imptt.apm29.utilities.InjectUtils
import com.imptt.apm29.viewmodels.MainViewModel
import kotlinx.android.synthetic.main.main_activity.*


class MainActivity : AppCompatActivity() {


    private val mainViewModel: MainViewModel by viewModels {
        InjectUtils.provideMainViewModelFactory(
            ServicePTTBinderProxy(this, this),
            this
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        mainViewModel.ensureCreate()
        cardView1.setOnClickListener {
            startActivity(Intent(this, AudioRecordActivity::class.java))
        }
        cardView2.setOnClickListener {
            startActivity(Intent(this, WebViewActivity::class.java).apply {
                putExtra("url", "http://jwttest.ciih.net/#/Home")
            })
        }
        cardView3.setOnClickListener {
            startActivity(Intent(this, WebViewActivity::class.java).apply {
                putExtra("url", "http://jwttest.ciih.net/#/todayDuty")
            })
        }
        cardView4.setOnClickListener {
            startActivity(Intent(this, WebViewActivity::class.java).apply {
                putExtra("url", "http://jwttest.ciih.net/#/Home")
            })
        }
        cardView1.visibility = View.GONE
        cardView4.visibility = View.GONE
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        startActivity(Intent(this, PttActivity::class.java))
        return super.onOptionsItemSelected(item)
    }
}