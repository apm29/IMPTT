package com.imptt.apm29.ui.web

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.imptt.apm29.R
import kotlinx.android.synthetic.main.activity_web_view.*


class WebViewActivity : AppCompatActivity() {
    val webViewClient :WebViewClient by lazy {
        WebViewClient()
    }
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_view)
        web.settings.javaScriptEnabled = true
        web.webViewClient =  webViewClient
        web.loadUrl(intent?.extras?.getString("url") ?: "http://ebasetest.ciih.net")
    }

    override fun onBackPressed() {
        if(web.canGoBack()){
            web.goBack()
        }else {
            super.onBackPressed()
        }
    }
}