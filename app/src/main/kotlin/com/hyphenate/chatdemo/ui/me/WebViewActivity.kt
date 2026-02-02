package com.hyphenate.chatdemo.ui.me

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.webkit.WebViewClient
import com.hyphenate.chatdemo.R
import com.hyphenate.chatdemo.databinding.DemoActivityWebviewBinding
import com.hyphenate.easeui.base.ChatUIKitBaseActivity

class WebViewActivity : ChatUIKitBaseActivity<DemoActivityWebviewBinding>() {
    private var url = "https://www.easemob.com/"

    override fun getViewBinding(inflater: LayoutInflater): DemoActivityWebviewBinding? {
        return DemoActivityWebviewBinding.inflate(inflater)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if ( intent.hasExtra(LOAD_TYPE) ){
            val type =  WebViewLoadType.from(intent.getIntExtra(LOAD_TYPE,0))
            when (type) {
                WebViewLoadType.LocalHtml -> {
                    binding.titleBar.setTitle(getString(R.string.about_privacy_policy))
                    url = "file:///android_asset/privacy_protocol.html"
                }
                WebViewLoadType.PrivacyPolicy -> {
                    binding.titleBar.setTitle(getString(R.string.main_about_me_privacy_policy))
                    url = "https://www.easemob.com/demo/privacy-policy"
                }
                WebViewLoadType.ThirdPartyDataSharing -> {
                    binding.titleBar.setTitle(getString(R.string.main_about_me_third_party_data))
                    url = "https://www.easemob.com/demo/third-party-sharing"
                }
                WebViewLoadType.PersonalDataCollection -> {
                    binding.titleBar.setTitle(getString(R.string.main_about_me_personal_data_collection))
                    // Load local HTML file with parameters
                    val username = intent.getStringExtra(PARAM_USERNAME) ?: ""
                    val phone = intent.getStringExtra(PARAM_PHONE) ?: ""
                    val device = intent.getStringExtra(PARAM_DEVICE) ?: ""
                    val avatar = intent.getStringExtra(PARAM_AVATAR) ?: ""
                    
                    // Build URL with query parameters
                    url = "file:///android_asset/person-info.html?username=$username&phone=$phone&device=$device&avatar=$avatar"
                }
                WebViewLoadType.TermsOfService -> {
                    binding.titleBar.setTitle(getString(R.string.main_about_me_terms_of_service))
                    url = "https://www.easemob.com/demo/agreement"
                }
                else -> {
                    url = "https://www.easemob.com/"
                }
            }
        }

        binding.let {
            val webSettings = it.wbView.settings
            webSettings.javaScriptEnabled = true
            it.wbView.webViewClient = WebViewClient()
            it.wbView.loadUrl(url)
            it.titleBar.setNavigationOnClickListener{
                mContext.onBackPressed()
            }
        }
    }

    companion object {
        private const val LOAD_TYPE = "webView_load_type"
        private const val PARAM_USERNAME = "param_username"
        private const val PARAM_PHONE = "param_phone"
        private const val PARAM_DEVICE = "param_device"
        private const val PARAM_AVATAR = "param_avatar"
        
        fun actionStart(context: Context, type: WebViewLoadType) {
            Intent(context, WebViewActivity::class.java).apply {
                putExtra(LOAD_TYPE, type.ordinal)
                context.startActivity(this)
            }
        }
        
        fun actionStartWithParams(
            context: Context, 
            type: WebViewLoadType,
            username: String = "",
            phone: String = "",
            device: String = "",
            avatar: String = ""
        ) {
            Intent(context, WebViewActivity::class.java).apply {
                putExtra(LOAD_TYPE, type.ordinal)
                putExtra(PARAM_USERNAME, username)
                putExtra(PARAM_PHONE, phone)
                putExtra(PARAM_DEVICE, device)
                putExtra(PARAM_AVATAR, avatar)
                context.startActivity(this)
            }
        }
    }
}

enum class WebViewLoadType(val value:Int){
    RemoteUrl(0),
    LocalHtml(1),
    PrivacyPolicy(2),
    ThirdPartyDataSharing(3),
    PersonalDataCollection(4),
    TermsOfService(5);

    companion object {
        fun from(value: Int): WebViewLoadType {
            val types = WebViewLoadType.values()
            val length = types.size
            for (i in 0 until length) {
                val type = types[i]
                if (type.value == value) {
                    return type
                }
            }
            return RemoteUrl
        }
    }
}