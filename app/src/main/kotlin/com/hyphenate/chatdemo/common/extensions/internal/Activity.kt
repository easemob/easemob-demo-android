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
import com.hyphenate.easeui.common.extensions.isAlertMessage

private const val SWINDLE_SOURCE_MSG_ID = "demo_swindle_source_msg_id"

internal fun Activity.makeTaskToFront() {
    (getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)
        ?.moveTaskToFront(taskId, ActivityManager.MOVE_TASK_WITH_HOME)
}

internal fun Context.insertSwindleMsg(message: ChatMessage?){
    if (message == null) return
    if (message.isAlertMessage()) return

    val conversation = ChatClient.getInstance().chatManager().getConversation(message.conversationId()) ?: return
    val sourceMsgId = message.msgId
    if (sourceMsgId.isNullOrEmpty().not()) {
        val hasInserted = conversation.allMessages.any {
            it.isAlertMessage() && it.ext()[SWINDLE_SOURCE_MSG_ID] == sourceMsgId
        }
        if (hasInserted) return
    }

    val baseMsgTime = message.msgTime.takeIf { it > 0 } ?: System.currentTimeMillis()
    val baseLocalTime = message.localTime().takeIf { it > 0 } ?: baseMsgTime
    val swindleMsg = ChatMessage.createSendMessage(ChatMessageType.CUSTOM).let {
        it.from = ChatClient.getInstance().currentUser
        it.to = message.conversationId()
        it.chatType = message.chatType
        it.msgTime = baseMsgTime + 1
        it.setLocalTime(baseLocalTime + 1)
        it.setIsChatThreadMessage(message.isChatThreadMessage)
        val body = ChatCustomMessageBody(ChatUIKitConstant.MESSAGE_CUSTOM_ALERT)
        mutableMapOf(
            ChatUIKitConstant.MESSAGE_CUSTOM_ALERT_CONTENT to getString(R.string.demo_swindle_message_content),
        ).let { map ->
            body.params = map
        }
        it.body = body
        sourceMsgId?.let { msgId -> it.setAttribute(SWINDLE_SOURCE_MSG_ID, msgId) }
        it.setStatus(ChatMessageStatus.SUCCESS)
        it
    }
    conversation.insertMessage(swindleMsg)
}
