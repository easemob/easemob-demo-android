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
    
    private var wakeLock: PowerManager.WakeLock? = null
    
    companion object {
        private const val REQUEST_OVERLAY_PERMISSION = 201
        private const val REQUEST_NOTIFICATION_PERMISSION = 202
        
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
        
        // 设置锁屏显示相关配置
        setupLockScreenDisplay()
        
        initIntent(intent)
        initView(savedInstanceState)
        initListener()
        initData()
    }


    /**
     * 设置锁屏显示相关配置
     */
    private fun setupLockScreenDisplay() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 -> {
                // Android 8.1+ 使用新API
                setShowWhenLocked(true)
                setTurnScreenOn(true)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                // Android 8.0
                window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
                window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
            }
            else -> {
                // Android 8.0以下
                window.addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                )
            }
        }

        // 请求解锁键盘锁 - 但不要强制解锁，否则会造成显示在锁屏上方失败
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 对于锁屏通话，我们不强制解锁，而是在锁屏上显示
            if (!keyguardManager.isKeyguardSecure) {
                // 只有在没有安全锁屏时才请求解锁
                keyguardManager.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
                    override fun onDismissSucceeded() {
                        super.onDismissSucceeded()
                        com.hyphenate.callkit.utils.ChatLog.d(TAG, "Keyguard dismissed successfully")
                    }

                    override fun onDismissError() {
                        super.onDismissError()
                        com.hyphenate.callkit.utils.ChatLog.w(TAG, "Failed to dismiss keyguard - showing on lockscreen")
                    }
                })
            } else {
                com.hyphenate.callkit.utils.ChatLog.d(TAG, "Secure keyguard detected - showing on lockscreen without unlock")
            }

        }

        
        // 获取唤醒锁
        acquireWakeLock()
        
        // 检查系统窗口权限（Android 6.0+）
        checkSystemWindowPermission()
        
        // 检查通知权限（Android 13+）
        checkNotificationPermission()
    }
    
    /**
     * 获取唤醒锁
     */
    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "ChatDemo::WakeLock"
        )
        wakeLock?.acquire(10 * 60 * 1000L) // 10分钟超时
    }
    
    /**
     * 检查系统窗口权限
     */
    private fun checkSystemWindowPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                ChatLog.w("BaseInitActivity", "System overlay permission not granted")
                // 可以选择是否要求用户授权，这里暂时不强制要求
            }
        }
    }
    
    /**
     * 检查通知权限
     */
    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, 
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS), 
                    REQUEST_NOTIFICATION_PERMISSION)
            }
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            REQUEST_NOTIFICATION_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    ChatLog.d("BaseInitActivity", "Notification permission granted")
                } else {
                    ChatLog.w("BaseInitActivity", "Notification permission denied")
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // 释放唤醒锁
        releaseWakeLock()
    }
    
    /**
     * 释放唤醒锁
     */
    private fun releaseWakeLock() {
        wakeLock?.let { lock ->
            if (lock.isHeld) {
                lock.release()
                ChatLog.d("BaseInitActivity", "WakeLock released")
            }
        }
        wakeLock = null
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