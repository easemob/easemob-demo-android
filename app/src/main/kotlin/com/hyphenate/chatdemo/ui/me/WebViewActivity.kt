package com.hyphenate.chatdemo.ui.me

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.hyphenate.chatdemo.databinding.DemoActivityWebviewBinding
import com.hyphenate.easeui.base.EaseBaseActivity

class WebViewActivity : EaseBaseActivity<DemoActivityWebviewBinding>() {

    override fun getViewBinding(inflater: LayoutInflater): DemoActivityWebviewBinding? {
        return DemoActivityWebviewBinding.inflate(inflater)
    }
    companion object {
        private const val URL = "https://www.easemob.com/"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.let {
            val webSettings = it.wbView.settings
            webSettings.javaScriptEnabled = true
            it.wbView.webViewClient = WebViewClient()
            it.wbView.loadUrl(URL)
            it.titleBar.setNavigationOnClickListener{
                mContext.onBackPressed()
            }
        }
    }


}