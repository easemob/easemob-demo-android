package com.hyphenate.chatdemo.ui.chat

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import coil.load
import com.hyphenate.chatdemo.R
import com.hyphenate.chatdemo.callkit.CallKitManager
import com.hyphenate.chatdemo.common.DemoConstant
import com.hyphenate.chatdemo.common.MenuFilterHelper
import com.hyphenate.chatdemo.common.PresenceCache
import com.hyphenate.chatdemo.interfaces.IPresenceRequest
import com.hyphenate.chatdemo.interfaces.IPresenceResultView
import com.hyphenate.chatdemo.utils.EasePresenceUtil
import com.hyphenate.chatdemo.viewmodel.PresenceViewModel
import com.hyphenate.easeui.ChatUIKitClient
import com.hyphenate.easeui.common.ChatMessage
import com.hyphenate.easeui.common.ChatPresence
import com.hyphenate.easeui.common.bus.ChatUIKitFlowBus
import com.hyphenate.easeui.feature.chat.UIKitChatFragment
import com.hyphenate.easeui.feature.chat.enums.ChatUIKitType
import com.hyphenate.easeui.feature.chat.widgets.ChatUIKitLayout
import com.hyphenate.easeui.menu.chat.ChatUIKitChatMenuHelper
import com.hyphenate.easeui.model.ChatUIKitEvent
import java.util.Locale


class ChatFragment: UIKitChatFragment() , IPresenceResultView {
    private var presenceViewModel: IPresenceRequest? = null
    override fun initView(savedInstanceState: Bundle?) {
        super.initView(savedInstanceState)
        binding?.titleBar?.inflateMenu(com.hyphenate.chatdemo.R.menu.demo_chat_menu)
        updatePresence()
        setFraudLayoutInChatFragemntHead()
    }

    override fun initEventBus() {
        super.initEventBus()
        ChatUIKitFlowBus.with<ChatUIKitEvent>(ChatUIKitEvent.EVENT.UPDATE + ChatUIKitEvent.TYPE.CONTACT + DemoConstant.EVENT_UPDATE_USER_SUFFIX).register(this) {
            if (it.isContactChange && it.message.isNullOrEmpty().not()) {
                val userId = it.message
                if (chatType == ChatUIKitType.SINGLE_CHAT && userId == conversationId) {
                    setDefaultHeader(true)
                }
                binding?.layoutChat?.chatMessageListLayout?.refreshMessages()
            }
        }
        ChatUIKitFlowBus.with<ChatUIKitEvent>(ChatUIKitEvent.EVENT.UPDATE.name).register(this) {
            if (it.isPresenceChange && it.message.equals(conversationId) ) {
                updatePresence()
            }
        }
    }

    override fun initViewModel() {
        super.initViewModel()
        presenceViewModel = ViewModelProvider(this)[PresenceViewModel::class.java]
        presenceViewModel?.attachView(this)
    }

    override fun initData() {
        super.initData()
        conversationId?.let {
            if (it != ChatUIKitClient.getCurrentUser()?.id){
                presenceViewModel?.fetchChatPresence(mutableListOf(it))
                presenceViewModel?.subscribePresences(mutableListOf(it))
            }
        }
    }

    override fun setMenuItemClick(item: MenuItem): Boolean {
        when(item.itemId) {
            com.hyphenate.chatdemo.R.id.chat_menu_video_call -> {
                showVideoCall()
                return true
            }
        }
        return super.setMenuItemClick(item)
    }

    private fun showVideoCall() {
        if (chatType == ChatUIKitType.SINGLE_CHAT) {
            conversationId?.let {
                CallKitManager.showSelectDialog(mContext, it)
            }
        } else {
            conversationId?.let {
                CallKitManager.startGroupCall(it)
            }

        }
    }

    override fun onPreMenu(helper: ChatUIKitChatMenuHelper?, message: ChatMessage?) {
        super.onPreMenu(helper, message)
        MenuFilterHelper.filterMenu(helper, message)
    }

    private fun updatePresence(){
        if (chatType == ChatUIKitType.SINGLE_CHAT){
            conversationId?.let {
                val presence = PresenceCache.getUserPresence(it)
                val logoStatus = EasePresenceUtil.getPresenceIcon(mContext,presence)
                val subtitle = EasePresenceUtil.getPresenceString(mContext,presence)
                binding?.run{
                    titleBar.setLogoStatusMargin(end = -1, bottom = -1)
                    titleBar.setLogoStatus(logoStatus)
                    titleBar.setSubtitle(subtitle)
                    titleBar.getStatusView().visibility = View.VISIBLE
                    titleBar.setLogoStatusSize(resources.getDimensionPixelSize(com.hyphenate.chatdemo.R.dimen.em_title_bar_status_icon_size))
                }
            }
        }
    }

    override fun onPeerTyping(action: String?) {
        if (TextUtils.equals(action, ChatUIKitLayout.ACTION_TYPING_BEGIN)) {
            binding?.titleBar?.setSubtitle(getString(com.hyphenate.easeui.R.string.alert_during_typing))
            binding?.titleBar?.visibility = View.VISIBLE
        } else if (TextUtils.equals(action, ChatUIKitLayout.ACTION_TYPING_END)) {
            updatePresence()
        }
    }


    override fun onDestroy() {
        conversationId?.let {
            if (it != ChatUIKitClient.getCurrentUser()?.id){
                presenceViewModel?.unsubscribePresences(mutableListOf(it))
            }
        }
        super.onDestroy()
    }

    override fun fetchChatPresenceSuccess(presence: MutableList<ChatPresence>) {
        updatePresence()
    }

    /**
     * 检测当前系统是否为中文
     */
    private fun isChineseLanguage(): Boolean {
        val locale = Locale.getDefault()
        return locale.language == "zh" || locale.country == "CN" || locale.country == "TW" || locale.country == "HK"
    }

    private fun setFraudLayoutInChatFragemntHead() {
        val messageListLayout = binding?.layoutChat?.chatMessageListLayout
        val listLayoutParent = (messageListLayout?.getParent()) as ViewGroup
        val view: View = LayoutInflater.from(mContext).inflate(R.layout.demo_chat_fraud, listLayoutParent, false)
        listLayoutParent.addView(view)
        listLayoutParent.post { messageListLayout.setPadding(0, view.measuredHeight, 0, 0) }

        val textView: TextView =view.findViewById(R.id.tv_fraud)
        val ivExit: ImageView =view.findViewById(R.id.iv_fraud_exit)

        val prefixText = getString(R.string.demo_chat_fraud_prefix)
        val clickableText = getString(R.string.demo_chat_fraud_report)
        val fullText = prefixText + clickableText

        val spannableStringBuilder = SpannableStringBuilder(fullText)

        // 设置“点我举报”文字颜色
        val colorSpan = ForegroundColorSpan(ContextCompat.getColor(mContext, com.hyphenate.chatdemo.R.color.demo_chat_fraud_text_report_color))
        spannableStringBuilder.setSpan(
            colorSpan,
            prefixText.length,
            fullText.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        // 设置“点我举报”点击事件
        val clickableSpan: ClickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                Toast.makeText(mContext, getString(R.string.demo_chat_fraud_report_toast), Toast.LENGTH_SHORT).show()
            }
            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false // 去掉下划线
            }
        }
        spannableStringBuilder.setSpan(
            clickableSpan,
            prefixText.length,
            fullText.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        textView.text = spannableStringBuilder
        textView.movementMethod = LinkMovementMethod.getInstance()

        ivExit.setOnClickListener{
            listLayoutParent.removeView(view)
            listLayoutParent.post { messageListLayout.setPadding(0,0, 0, 0) }
        }

        // 使用 ImageView 显示用户头像
        val imageView = ImageView(mContext).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            // 根据系统语言显示不同图片
            val backgroundImage = if (isChineseLanguage()) {
                R.drawable.demo_swindle_bg
            } else {
                R.drawable.demo_swindle_bg_en
            }
            setImageResource(backgroundImage)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
            }
        }
        //设置反诈背景
        binding?.layoutChat?.addView(imageView,0)
    }
}