package com.hyphenate.chatdemo.ui.login

import android.content.Intent
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.text.Editable
import android.text.InputType
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.TextUtils
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.util.Base64
import android.util.Log
import android.util.TypedValue
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.inputmethod.EditorInfo
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.CompoundButton
import android.widget.TextView
import android.widget.TextView.GONE
import android.widget.TextView.OnEditorActionListener
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.text.clearSpans
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.hyphenate.chatdemo.BuildConfig
import com.hyphenate.chatdemo.DemoHelper
import com.hyphenate.chatdemo.MainActivity
import com.hyphenate.chatdemo.R
import com.hyphenate.chatdemo.common.CustomCountDownTimer
import com.hyphenate.chatdemo.common.DemoConstant
import com.hyphenate.chatdemo.common.PreferenceManager
import com.hyphenate.chatdemo.common.dialog.SimpleDialog
import com.hyphenate.chatdemo.common.extensions.internal.changePwdDrawable
import com.hyphenate.chatdemo.common.extensions.internal.clearEditTextListener
import com.hyphenate.chatdemo.common.extensions.internal.showRightDrawable
import com.hyphenate.chatdemo.databinding.DemoFragmentLoginBinding
import com.hyphenate.chatdemo.utils.AESEncryptor
import com.hyphenate.chatdemo.utils.PhoneNumberUtils
import com.hyphenate.chatdemo.utils.ToastUtils.showToast
import com.hyphenate.chatdemo.viewmodel.LoginFragmentViewModel
import com.hyphenate.easeui.base.ChatUIKitBaseFragment
import com.hyphenate.easeui.common.ChatClient
import com.hyphenate.easeui.common.ChatError
import com.hyphenate.easeui.common.bus.ChatUIKitFlowBus
import com.hyphenate.easeui.common.extensions.catchChatException
import com.hyphenate.easeui.common.extensions.hideSoftKeyboard
import com.hyphenate.util.EMLog
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Locale
import javax.crypto.spec.SecretKeySpec

class LoginFragment : ChatUIKitBaseFragment<DemoFragmentLoginBinding>(), View.OnClickListener,
    TextWatcher,
    CompoundButton.OnCheckedChangeListener, OnEditorActionListener {
    private var mUserPhone: String? = null
    private var mCode: String? = null
    private lateinit var mFragmentViewModel: LoginFragmentViewModel
    private var clear: Drawable? = null
    private var eyeOpen: Drawable? = null
    private var eyeClose: Drawable? = null
    private val mHits = LongArray(COUNT)
    private var isDeveloperMode = false
    private var isShowingDialog = false
    private var countDownTimer: CustomCountDownTimer? = null
    private val VERIFY_CODE_URL = "https://downloadsdk.easesdk.com/downloads/IMDemo/sms/index.html"

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): DemoFragmentLoginBinding? {
        return DemoFragmentLoginBinding.inflate(inflater)
    }

    override fun initListener() {
        super.initListener()
        binding?.run {
            etLoginPhone.addTextChangedListener(this@LoginFragment)
            etLoginCode.addTextChangedListener(this@LoginFragment)
            tvVersion.setOnClickListener(this@LoginFragment)
            btnLogin.setOnClickListener(this@LoginFragment)
            tvGetCode.setOnClickListener(this@LoginFragment)
            tvLoginDeveloper.setOnClickListener(this@LoginFragment)
            cbSelect.setOnCheckedChangeListener(this@LoginFragment)
            etLoginCode.setOnEditorActionListener(this@LoginFragment)
            etLoginPhone.clearEditTextListener()
            root.setOnClickListener {
                mContext.hideSoftKeyboard()
                makeVerifyCodeWebViewVisible(false)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        mFragmentViewModel = ViewModelProvider(this)[LoginFragmentViewModel::class.java]
    }

    override fun initView(savedInstanceState: Bundle?) {
        super.initView(savedInstanceState)
        setupWebView()
    }

    override fun initData() {
        super.initData()
        binding?.run {
            etLoginPhone.setText(ChatClient.getInstance().currentUser)
            tvVersion.text = "V${ChatClient.VERSION}"
            tvAgreement.text = spannable
            tvAgreement.movementMethod = LinkMovementMethod.getInstance()
            tvAgreement.setHintTextColor(Color.TRANSPARENT)
        }
        eyeClose = ContextCompat.getDrawable(mContext, R.drawable.d_pwd_hide)
        eyeOpen = ContextCompat.getDrawable(mContext, R.drawable.d_pwd_show)
        clear = ContextCompat.getDrawable(mContext, R.drawable.d_clear)
        binding?.etLoginPhone?.showRightDrawable(clear)
        isDeveloperMode = DemoHelper.getInstance().getDataModel().isDeveloperMode()
        resetView(isDeveloperMode)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.tv_version -> {
                System.arraycopy(mHits, 1, mHits, 0, mHits.size - 1)
                mHits[mHits.size - 1] = SystemClock.uptimeMillis()
                if (mHits[0] >= SystemClock.uptimeMillis() - DURATION && !isShowingDialog) {
                    isShowingDialog = true
                    showOpenDeveloperDialog()
                }
            }

            R.id.btn_login -> {
                mContext?.hideSoftKeyboard()
                loginToServer()
            }

            R.id.tv_get_code -> {
                getVerificationCode()
            }

            R.id.tv_login_developer -> {
                ChatUIKitFlowBus.with<String>(DemoConstant.SKIP_DEVELOPER_CONFIG)
                    .post(lifecycleScope, LoginFragment::class.java.simpleName)
            }
        }
    }

    private fun loginToServer() {
        if (isDeveloperMode) {
            if (TextUtils.isEmpty(mUserPhone) || TextUtils.isEmpty(mCode)) {
                showToast(mContext!!.getString(R.string.em_login_btn_info_incomplete))
                return
            }
            if (binding?.cbSelect?.isChecked == false) {
                showToast(mContext!!.getString(R.string.em_login_not_select_agreement))
                return
            }
            lifecycleScope.launch {
                mFragmentViewModel.login(mUserPhone!!, mCode!!)
                    .onStart { showLoading(true) }
                    .onCompletion { dismissLoading() }
                    .catchChatException { e ->
                        if (e.errorCode == ChatError.USER_AUTHENTICATION_FAILED) {
                            showToast(R.string.demo_error_user_authentication_failed)
                        } else {
                            showToast(e.description)
                        }
                    }
                    .stateIn(
                        lifecycleScope,
                        SharingStarted.WhileSubscribed(stopTimeoutMillis),
                        null
                    )
                    .collect {
                        if (it != null) {
                            DemoHelper.getInstance().getDataModel().initDb()
                            startActivity(Intent(mContext, MainActivity::class.java))
                            mContext.finish()
                        }
                    }
            }
        } else {
            if (mUserPhone.isNullOrEmpty()) {
                showToast(mContext.getString(R.string.em_login_phone_empty))
                return
            }
            if (!PhoneNumberUtils.isPhoneNumber(mUserPhone)) {
                showToast(mContext.getString(R.string.em_login_phone_illegal))
                return
            }
            if (mCode.isNullOrEmpty()) {
                showToast(R.string.em_login_code_empty)
                return
            }
            if (!PhoneNumberUtils.isNumber(mCode)) {
                showToast(mContext.getString(R.string.em_login_illegal_code))
                return
            }
            if (binding?.cbSelect?.isChecked == false) {
                showToast(mContext.getString(R.string.em_login_not_select_agreement))
                return
            }
            lifecycleScope.launch {
                mFragmentViewModel.loginFromAppServer(mUserPhone!!, mCode!!)
                    .onStart {
                        showLoading(true)
                    }
                    .onCompletion {
                        dismissLoading()
                    }
                    .catchChatException { e ->
                        if (e.errorCode == ChatError.USER_AUTHENTICATION_FAILED) {
                            showToast(R.string.demo_error_user_authentication_failed)
                        } else {
                            showToast(e.description)
                        }
                    }
                    .stateIn(
                        lifecycleScope,
                        SharingStarted.WhileSubscribed(stopTimeoutMillis),
                        null
                    )
                    .collect {
                        if (it != null) {
                            DemoHelper.getInstance().getDataModel().initDb()
                            startActivity(Intent(mContext, MainActivity::class.java))
                            mContext.finish()
                        }
                    }
            }

        }
    }

    private fun getVerificationCode() {
        if (mUserPhone.isNullOrEmpty()) {
            showToast(mContext!!.getString(R.string.em_login_phone_empty))
            return
        }
        if (!PhoneNumberUtils.isPhoneNumber(mUserPhone)) {
            showToast(mContext!!.getString(R.string.em_login_phone_illegal))
            return
        }
        binding?.tvGetCode?.let {
            countDownTimer = CustomCountDownTimer(
                it,
                60000,
                1000
            )
        }
        mContext.hideSoftKeyboard()
        showVerifyCodeWebView(mUserPhone)
    }

    private fun showOpenDeveloperDialog() {
        SimpleDialog.Builder(mContext)
            .setTitle(
                if (isDeveloperMode) getString(R.string.server_close_develop_mode) else getString(
                    R.string.server_open_develop_mode
                )
            )
            .setPositiveButton {
                isDeveloperMode = !isDeveloperMode
                DemoHelper.getInstance().getDataModel()?.setDeveloperMode(isDeveloperMode)
                binding?.etLoginPhone?.setText("")
                resetView(isDeveloperMode)
            }
            .setOnDismissListener {
                isShowingDialog = false
            }
            .setCanceledOnTouchOutside(false)
            .build()
            .show()
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    override fun afterTextChanged(s: Editable) {
        binding?.run {
            mUserPhone = etLoginPhone.text.toString().trim { it <= ' ' }
            mCode = etLoginCode.text.toString().trim { it <= ' ' }
            etLoginPhone.showRightDrawable(clear)
            if (isDeveloperMode) {
                etLoginCode.showRightDrawable(eyeClose)
            } else {
                if (!PhoneNumberUtils.isPhoneNumber(mUserPhone)) {
                    makeVerifyCodeWebViewVisible(false)
                }
            }
            setButtonEnable(!TextUtils.isEmpty(mUserPhone) && !TextUtils.isEmpty(mCode))
        }
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        when (buttonView.id) {
            R.id.cb_select -> setButtonEnable(
                !TextUtils.isEmpty(mUserPhone) && !TextUtils.isEmpty(
                    mCode
                ) && isChecked
            )
        }
    }

    private fun setButtonEnable(enable: Boolean) {
        binding?.run {
            btnLogin.isEnabled = enable
            if (etLoginCode.hasFocus()) {
                makeVerifyCodeWebViewVisible(false)
                etLoginCode.imeOptions =
                    if (enable) EditorInfo.IME_ACTION_DONE else EditorInfo.IME_ACTION_PREVIOUS
            } else if (etLoginPhone.hasFocus()) {
                etLoginCode.imeOptions =
                    if (enable) EditorInfo.IME_ACTION_DONE else EditorInfo.IME_ACTION_NEXT
            }
        }
    }

    private var spannable: SpannableString? = null
        private get() {
            if (field == null) {
                field = createSpannable()
            }
            return field
        }

    private fun createSpannable(): SpannableString {
        val language =
            PreferenceManager.getValue(DemoConstant.APP_LANGUAGE, Locale.getDefault().language)
        val isZh = language.startsWith("zh")
        val spanStr = SpannableString(getString(R.string.em_login_agreement))
        var start1 = 29
        var end1 = 45
        var start2 = 50
        var end2 = spanStr.length
        if (isZh) {
            start1 = 5
            end1 = 13
            start2 = 14
            end2 = spanStr.length
        }
        //设置下划线
        //spanStr.setSpan(new UnderlineSpan(), 3, 7, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spanStr.setSpan(object : MyClickableSpan() {
            override fun onClick(widget: View) {
                jumpToAgreement()
            }
        }, start1, end1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spanStr.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(mContext, com.hyphenate.easeui.R.color.ease_color_primary)),
            start1,
            end1,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        //spanStr.setSpan(new UnderlineSpan(), 10, 14, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spanStr.setSpan(object : MyClickableSpan() {
            override fun onClick(widget: View) {
                jumpToProtocol()
            }
        }, start2, end2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spanStr.setSpan(
            ForegroundColorSpan(
                ContextCompat.getColor(
                    mContext,
                    com.hyphenate.easeui.R.color.ease_color_primary
                )
            ),
            start2,
            end2,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return spanStr
    }

    override fun onEditorAction(v: TextView, actionId: Int, event: KeyEvent?): Boolean {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            if (!TextUtils.isEmpty(mUserPhone) && !TextUtils.isEmpty(mCode)) {
                mContext.hideSoftKeyboard()
                loginToServer()
                return true
            }
        }
        return false
    }

    private fun resetView(isDeveloperMode: Boolean) {
        binding?.run {
            etLoginCode.setText("")
            if (isDeveloperMode) {
                etLoginPhone.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL
                etLoginCode.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                etLoginCode.setHint(R.string.em_login_password_hint)
                etLoginPhone.setHint(R.string.em_login_name_hint)
                tvGetCode.visibility = View.GONE
                tvLoginDeveloper.visibility = View.VISIBLE
                etLoginCode.changePwdDrawable(
                    eyeOpen,
                    eyeClose,
                    null,
                    null,
                    null
                )
            } else {
                etLoginPhone.inputType = InputType.TYPE_CLASS_PHONE
                etLoginCode.inputType = InputType.TYPE_CLASS_NUMBER
                etLoginCode.setHint(R.string.em_login_input_verification_code)
                etLoginPhone.setHint(R.string.register_phone_number)
                tvGetCode.visibility = View.VISIBLE
                tvLoginDeveloper.visibility = View.GONE
                etLoginCode.showRightDrawable(null)
                etLoginCode.clearEditTextListener()
                DemoHelper.getInstance().getDataModel().enableCustomSet(false)
                DemoHelper.getInstance().getDataModel().setDeveloperMode(false)
//                ChatClient.getInstance().options.enableDNSConfig(true)
            }
        }
    }

    private fun jumpToAgreement() {
        val uri = Uri.parse("http://www.easemob.com/agreement")
        val it = Intent(Intent.ACTION_VIEW, uri)
        startActivity(it)
    }

    private fun jumpToProtocol() {
        val uri = Uri.parse("http://www.easemob.com/protocol")
        val it = Intent(Intent.ACTION_VIEW, uri)
        startActivity(it)
    }

    private fun setupWebView() {
        binding?.webView?.settings?.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_NO_CACHE
        }

        // Kotlin代码动态设置
        binding?.webView?.apply {
            addJavascriptInterface(CaptchaJsInterface(), "android")
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            // 设置背景色，避免显示旧内容
            setBackgroundColor(ContextCompat.getColor(context, android.R.color.white))
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, 4f.dp)
                }
            }
            clipToOutline = true
        }

        binding?.webView?.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                // 页面开始加载时显示进度条
                if (binding?.progressBar?.visibility == VISIBLE) {
                    binding?.progressBar?.visibility = VISIBLE
                }
            }
            
            override fun onPageFinished(view: WebView?, url: String?) {
                binding?.progressBar?.visibility = GONE
            }

            @RequiresApi(Build.VERSION_CODES.M)
            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                handleError(errorInfo = "WEBVIEW_ERROR: ${error?.description}")
            }

            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                url?.let {
                    view?.loadUrl(url)
                    return true
                }
                return super.shouldOverrideUrlLoading(view, url)
            }
        }
    }

    private fun showVerifyCodeWebView(mUserPhone: String?) {
        makeVerifyCodeWebViewVisible(true)
        binding?.webView?.loadUrl("$VERIFY_CODE_URL?telephone=$mUserPhone")
    }

    private fun makeVerifyCodeWebViewVisible(visible: Boolean) {
        binding?.run {
            if (visible) {
                progressBar.visibility = VISIBLE
                webView.visibility = VISIBLE
            } else {
                progressBar.visibility = GONE
                webView.visibility = GONE
                // 隐藏时加载空白页面，清除视觉内容
                webView.post {
                    webView.loadUrl("about:blank")
                }
            }
        }
    }

    inner class CaptchaJsInterface {

        @JavascriptInterface
        fun encryptData(param: String) {
            // 1. 准备密钥
            val keyBytes = Base64.decode(BuildConfig.SECRET_KEY, Base64.DEFAULT)
            val secretKey = SecretKeySpec(keyBytes, "AES")

            val encryptedData = AESEncryptor.encrypt(param,secretKey)

            // 2. 切换到主线程回调JS
            binding?.webView?.post {
                val jsCallback = "window.encryptCallback('${encryptedData}')"
                binding?.webView?.evaluateJavascript(jsCallback, null)
                Log.d(TAG, "encryptData: $jsCallback")
            }
        }

        @JavascriptInterface
        fun getVerifyResult(verifyResult: String) {
            binding?.progressBar?.post {
                makeVerifyCodeWebViewVisible(false)
            }
            EMLog.d(TAG, "verifyResult = " + verifyResult)
            var code = -1;
            try {
                val json = JSONObject(verifyResult)
                code = json.getInt("code")
                if (code == 200) {
                    handleSuccess()
                } else {
                    val errorInfo = json.getString("errorInfo")
                    handleError(code, errorInfo)
                }

            } catch (e: Exception) {
                handleError(code, "PARSE_ERROR: ${e.message}")
            }
        }
    }

    private fun handleSuccess() {
        binding?.webView?.post {
            countDownTimer?.start()
            showToast(R.string.em_login_post_code)
        }
    }


    private fun handleError(code: Int = -1, errorInfo: String = "") {
        EMLog.e(TAG, "code = $code" + ",errorInfo = " + errorInfo)
        binding?.webView?.post {
            showToast(errorInfo)
        }
    }


    private abstract inner class MyClickableSpan : ClickableSpan() {
        override fun updateDrawState(ds: TextPaint) {
            super.updateDrawState(ds)
            ds.color = ContextCompat.getColor(mContext, R.color.transparent)
            ds.clearShadowLayer()
        }
    }

    override fun onDestroyView() {
        spannable?.clearSpans()
        spannable = null
        binding?.webView?.apply {
            stopLoading()
            destroy()
        }
        super.onDestroyView()
    }

    val Float.dp: Float
        get() = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            this,
            resources.displayMetrics
        )

    companion object {
        private const val TAG = "LoginFragment"
        private const val COUNT: Int = 5
        private const val DURATION = (3 * 1000).toLong()
        private const val stopTimeoutMillis: Long = 5000
    }
}