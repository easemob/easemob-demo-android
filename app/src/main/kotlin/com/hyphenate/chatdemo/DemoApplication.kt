package com.hyphenate.chatdemo

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.hyphenate.chatdemo.base.UserActivityLifecycleCallbacks
import com.hyphenate.chatdemo.common.DemoConstant
import com.hyphenate.chatdemo.common.LanguageUtil
import com.hyphenate.easeui.EaseIM
import com.hyphenate.easeui.common.helper.EasePreferenceManager
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.ClassicsHeader
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import com.tencent.bugly.crashreport.CrashReport
import java.util.Locale

class DemoApplication: Application() {
    private val mLifecycleCallbacks = UserActivityLifecycleCallbacks()
    override fun onCreate() {
        super.onCreate()
        instance = this
        registerActivityLifecycleCallbacks()

        DemoHelper.getInstance().init(this)
        initSDK()
        initFeatureConfig()
        initBugly()
    }

    private fun initSDK() {
        if (DemoHelper.getInstance().getDataModel().isAgreeAgreement()) {
            DemoHelper.getInstance().initSDK()
        }
    }

    private fun initFeatureConfig(){
        // Call this method after EaseIM#init
        val isBlack = DemoHelper.getInstance().getDataModel().getBoolean(DemoConstant.IS_BLACK_THEME)
        AppCompatDelegate.setDefaultNightMode(if (isBlack) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)

        val enableTranslation = DemoHelper.getInstance().getDataModel().getBoolean(DemoConstant.FEATURES_TRANSLATION,true)
        val enableThread = DemoHelper.getInstance().getDataModel().getBoolean(DemoConstant.FEATURES_THREAD,true)
        val enableReaction = DemoHelper.getInstance().getDataModel().getBoolean(DemoConstant.FEATURES_REACTION,true)
        val targetLanguage = EasePreferenceManager.getInstance().getString(DemoConstant.TARGET_LANGUAGE)
        targetLanguage?.let {
            if (it.isNotEmpty()){
                LanguageUtil.changeLanguage(it)
            }
        }
        EaseIM.getConfig()?.chatConfig?.targetTranslationLanguage = Locale.getDefault().language
        EaseIM.getConfig()?.chatConfig?.enableTranslationMessage = enableTranslation
        EaseIM.getConfig()?.chatConfig?.enableChatThreadMessage = enableThread
        EaseIM.getConfig()?.chatConfig?.enableMessageReaction = enableReaction
    }

    private fun initBugly(){
        CrashReport.initCrashReport(applicationContext)
    }

    private fun registerActivityLifecycleCallbacks() {
        this.registerActivityLifecycleCallbacks(mLifecycleCallbacks)
    }

    fun getLifecycleCallbacks(): UserActivityLifecycleCallbacks {
        return mLifecycleCallbacks
    }

    /**
     * Set default settings for SmartRefreshLayout
     */

    init {
        SmartRefreshLayout.setDefaultRefreshHeaderCreator { context, layout ->
            ClassicsHeader(context)
        }
        SmartRefreshLayout.setDefaultRefreshFooterCreator { context, layout ->
            ClassicsFooter(context)
        }
    }

    companion object {
        private lateinit var instance: DemoApplication
        fun getInstance(): DemoApplication {
            return instance
        }
    }

}