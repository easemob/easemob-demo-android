package com.hyphenate.chatdemo.common

import android.text.TextUtils
import com.hyphenate.easecallkit.bean.Constant
import com.hyphenate.easeui.R
import com.hyphenate.easeui.common.ChatMessage
import com.hyphenate.easeui.common.ChatMessageType
import com.hyphenate.easeui.menu.chat.ChatUIKitChatMenuHelper

object MenuFilterHelper {
    fun filterMenu(helper: ChatUIKitChatMenuHelper?, message: ChatMessage?){
        message?.let {
            when(it.type){
                ChatMessageType.TXT ->{
                    if (it.ext().containsKey(Constant.CALL_MSG_TYPE)){
                        val msgType = it.getStringAttribute(Constant.CALL_MSG_TYPE,"")
                        if (TextUtils.equals(msgType, Constant.CALL_MSG_INFO)) {
                            helper?.setAllItemsVisible(false)
                            helper?.clearTopView()
                            helper?.findItemVisible(R.id.action_chat_delete,true)
                        }
                    }
                }
                else -> {}
            }
        }
    }
}