package com.hyphenate.chatdemo.repository

import com.hyphenate.easeui.common.ChatClient
import com.hyphenate.easeui.feature.invitation.helper.ChatUIKitNotificationMsgManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


/**
 * As the repository of ChatManager, handles ChatManager related logic
 */
class ChatClientRepository: BaseRepository() {

    /**
     * Get all unread message count.
     */
    suspend fun getAllUnreadMessageCount(): Int =
        withContext(Dispatchers.IO) {
            val systemConversation = ChatUIKitNotificationMsgManager.getInstance().getConversation()
            val systemUnread = systemConversation.unreadMsgCount
            val allUnread = ChatClient.getInstance().chatManager().unreadMessageCount
            allUnread - systemUnread
        }

    /**
     * Get all unread request count.
     */
    suspend fun getRequestUnreadCount():Int =
        withContext(Dispatchers.IO) {
            // 联系人 Tab 与 UIKit 联系人页共用 em_system 会话中的已读游标。
            ChatUIKitNotificationMsgManager.getInstance().getRequestUnreadCount()
        }

}
