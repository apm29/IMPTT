package com.imptt.apm29.lifecycle

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.imptt.apm29.IServicePTT
import com.imptt.apm29.service.IMService
import com.imptt.apm29.service.ServicePTTBinder

/**
 *  author : ciih
 *  date : 2020/9/11 11:34 AM
 *  description :
 */
class ServicePTTBinderProxy(
    private val lifecycleOwner: LifecycleOwner, private val context: Context
) : LifecycleObserver, ServiceConnection {

    init {
        lifecycleOwner.lifecycle.removeObserver(this)
        lifecycleOwner.lifecycle.addObserver(this)
        println("ServicePTTBinderProxy.created")
    }

    private var mIBinder: IBinder? = null
    private val serviceIntent: Intent by lazy {
        Intent(context, IMService::class.java)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun onCreate() {
        println("ServiceBinderProxy.onCreate")
        context.startService(serviceIntent)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onStart() {
        println("ServiceBinderProxy.onStart")
        context.bindService(serviceIntent, this, Context.BIND_AUTO_CREATE)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume() {
        println("ServiceBinderProxy.onResume")
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause() {
        println("ServiceBinderProxy.onPause")
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStop() {
        println("ServiceBinderProxy.onStop")
        context.unbindService(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        println("ServiceBinderProxy.onDestroy")
        lifecycleOwner.lifecycle.removeObserver(this)
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        println("ServiceBinderProxy.onServiceConnected")
        println(service)
        mIBinder = service
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        println("ServiceBinderProxy.onServiceDisconnected")
        mIBinder = null
    }

    fun checkConnection() {
        mIBinder?.let {
           val bind =  IServicePTT.Stub.asInterface(it)
            bind?.basicTypes(0,101L,false,0.4f,0.5,"Hello")
        }?: println("Service is not connected")
    }

}