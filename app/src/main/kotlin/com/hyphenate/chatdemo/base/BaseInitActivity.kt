package com.hyphenate.chatdemo.base

import android.Manifest
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.WindowManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.viewbinding.ViewBinding
import com.hyphenate.easeui.base.ChatUIKitBaseActivity
import com.hyphenate.easeui.common.ChatLog

abstract class BaseInitActivity<B : ViewBinding> : ChatUIKitBaseActivity<B>() {

    private val TAG= "BaseInitActivity"
    companion object {
        /**
         * 创建适用于锁屏显示的 Intent
         * 从后台 Service 启动 Activity 时使用此方法
         */
        fun createLockScreenIntent(context: Context, activityClass: Class<out BaseInitActivity<*>>): Intent {
            return Intent(context, activityClass).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Android 8.0+ 从后台启动 Activity 需要额外的标志
                    addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                }
            }
        }
        
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initIntent(intent)
        initView(savedInstanceState)
        initListener()
        initData()
    }

    /**
     * init intent
     * @param intent
     */
    protected open fun initIntent(intent: Intent?) {}

    /**
     * init view
     * @param savedInstanceState
     */
    protected open fun initView(savedInstanceState: Bundle?) {}

    /**
     * init listener
     */
    protected open fun initListener() {}

    /**
     * init data
     */
    protected open fun initData() {}
}