package com.imptt.apm29.ui.ptt

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.imptt.apm29.R
import com.imptt.apm29.rtc.ImWebSocketClient
import com.permissionx.guolindev.PermissionX

class PttActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ptt)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        doRequestPermissions {
            println("permission requested")
        }
    }

    private fun doRequestPermissions(
        permissions: List<String> = arrayListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.MODIFY_AUDIO_SETTINGS
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