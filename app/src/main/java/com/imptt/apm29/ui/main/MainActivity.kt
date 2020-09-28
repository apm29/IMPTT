package com.imptt.apm29.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.imptt.apm29.R
import com.imptt.apm29.ui.ptt.PttActivity
import com.imptt.apm29.ui.web.WebViewActivity
import com.imptt.apm29.utilities.FileUtils
import kotlinx.android.synthetic.main.main_activity.*


class MainActivity : AppCompatActivity() {



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        cardView1.setOnClickListener {
            startActivity(Intent(this, PttActivity::class.java))
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
                putExtra("url", "http://jwttest.ciih.net/#/cuttingEdgeNews")
            })
        }
        FileUtils.initialize(this)
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