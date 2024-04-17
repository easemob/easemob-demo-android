package com.hyphenate.chatdemo.ui.chat

import android.os.Bundle
import android.view.MenuItem
import com.hyphenate.chatdemo.R
import com.hyphenate.chatdemo.callkit.CallKitManager
import com.hyphenate.chatdemo.common.DemoConstant
import com.hyphenate.chatdemo.common.MenuFilterHelper
import com.hyphenate.easeui.common.ChatMessage
import com.hyphenate.easeui.common.bus.EaseFlowBus
import com.hyphenate.easeui.feature.chat.EaseChatFragment
import com.hyphenate.easeui.feature.chat.enums.EaseChatType
import com.hyphenate.easeui.menu.chat.EaseChatMenuHelper
import com.hyphenate.easeui.model.EaseEvent

class ChatFragment: EaseChatFragment() {

    override fun initView(savedInstanceState: Bundle?) {
        super.initView(savedInstanceState)
        binding?.titleBar?.inflateMenu(R.menu.demo_chat_menu)
    }

    override fun initEventBus() {
        super.initEventBus()
        EaseFlowBus.with<EaseEvent>(EaseEvent.EVENT.UPDATE + EaseEvent.TYPE.CONTACT + DemoConstant.EVENT_UPDATE_USER_SUFFIX).register(this) {
            if (it.isContactChange && it.message.isNullOrEmpty().not()) {
                val userId = it.message
                if (chatType == EaseChatType.SINGLE_CHAT && userId == conversationId) {
                    setDefaultHeader(true)
                }
                binding?.layoutChat?.chatMessageListLayout?.refreshMessages()
            }
        }
    }

    override fun setMenuItemClick(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.chat_menu_video_call -> {
                showVideoCall()
                return true
            }
        }
        return super.setMenuItemClick(item)
    }

    private fun showVideoCall() {
        if (chatType == EaseChatType.SINGLE_CHAT) {
            CallKitManager.showSelectDialog(mContext, conversationId)
        } else {
            CallKitManager.startConferenceCall(mContext, conversationId)
        }
    }

    override fun onPreMenu(helper: EaseChatMenuHelper?, message: ChatMessage?) {
        super.onPreMenu(helper, message)
        MenuFilterHelper.filterMenu(helper, message)
    }
}