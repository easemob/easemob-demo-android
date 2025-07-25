package com.hyphenate.chatdemo.common.extensions.internal

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import com.hyphenate.easeui.common.ChatCustomMessageBody
import com.hyphenate.chatdemo.R
import com.hyphenate.easeui.common.ChatClient
import com.hyphenate.easeui.common.ChatMessage
import com.hyphenate.easeui.common.ChatMessageStatus
import com.hyphenate.easeui.common.ChatMessageType
import com.hyphenate.easeui.common.ChatUIKitConstant

internal fun Activity.makeTaskToFront() {
    (getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)
        ?.moveTaskToFront(taskId, ActivityManager.MOVE_TASK_WITH_HOME)
}

internal fun Activity.insertSwindleMsg(message: ChatMessage?){
    val swindleMsg = ChatMessage.createSendMessage(ChatMessageType.CUSTOM).let {
        it.from = ChatClient.getInstance().currentUser
        it.to = message?.conversationId()
        it.chatType = message?.chatType
        val body = ChatCustomMessageBody(ChatUIKitConstant.MESSAGE_CUSTOM_ALERT)
        mutableMapOf(
            ChatUIKitConstant.MESSAGE_CUSTOM_ALERT_CONTENT to getString(R.string.demo_swindle_message_content),
        ).let { map ->
            body.params = map
        }
        it.body = body
        it.setStatus(ChatMessageStatus.SUCCESS)
        it
    }
    ChatClient.getInstance().chatManager().
    getConversation(message?.conversationId())
        .insertMessage(swindleMsg)
}