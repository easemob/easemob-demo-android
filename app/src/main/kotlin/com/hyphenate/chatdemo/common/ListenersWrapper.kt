package com.hyphenate.chatdemo.common

import android.content.Intent
import com.hyphenate.callkit.bean.Constant
import com.hyphenate.chatdemo.DemoApplication
import com.hyphenate.chatdemo.DemoHelper
import com.hyphenate.chatdemo.common.extensions.internal.insertSwindleMsg
import com.hyphenate.chatdemo.ui.login.LoginActivity
import com.hyphenate.easeui.ChatUIKitClient
import com.hyphenate.easeui.common.ChatClient
import com.hyphenate.easeui.common.ChatLog
import com.hyphenate.easeui.common.ChatLoginExtensionInfo
import com.hyphenate.easeui.common.ChatMessage
import com.hyphenate.easeui.common.ChatPresence
import com.hyphenate.easeui.common.ChatPresenceListener
import com.hyphenate.easeui.common.bus.ChatUIKitFlowBus
import com.hyphenate.easeui.common.extensions.mainScope
import com.hyphenate.easeui.interfaces.ChatUIKitConnectionListener
import com.hyphenate.easeui.interfaces.ChatUIKitContactListener
import com.hyphenate.easeui.interfaces.ChatUIKitMessageListener
import com.hyphenate.easeui.model.ChatUIKitEvent

object ListenersWrapper {

    private val connectListener by lazy {
        object : ChatUIKitConnectionListener() {
            override fun onTokenExpired() {
                super.onTokenExpired()
                logout(false)
            }

            override fun onLogout(errorCode: Int, info: ChatLoginExtensionInfo?) {
                super.onLogout(errorCode, info)
                ChatLog.e("app","onLogout: $errorCode ${info?.deviceInfo} - ${info?.deviceExt}")
                logout()
            }
        }
    }

    private fun logout(unbindPushToken:Boolean = true){
        DemoHelper.getInstance().getDataModel().clearLoginToken()
        ChatUIKitClient.logout(unbindPushToken,
            onSuccess = {
                ChatLog.e("ListenersWrapper","logout success")
                DemoApplication.getInstance().getLifecycleCallbacks().activityList.forEach {
                    it.finish()
                }
                DemoApplication.getInstance().apply {
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }
            },
            onError = {code, error ->
                ChatLog.e("ListenersWrapper","logout error $code $error")
            }
        )
    }

    private val messageListener by lazy { object : ChatUIKitMessageListener(){
        override fun onMessageReceived(messages: MutableList<ChatMessage>?) {
            super.onMessageReceived(messages)
            messages?.forEach { message ->
                DemoHelper.getInstance().context.insertSwindleMsg(message)

                if (DemoHelper.getInstance().getDataModel().isAppPushSilent()) {
                    return@forEach
                }
                if (ChatUIKitClient.checkMutedConversationList(message.conversationId())) {
                    return@forEach
                }
                // 呼叫信令消息不弹普通消息通知：来电提醒由 callkit（telecom/悬浮窗/全屏通知）负责，
                // 且进程被杀时 FCM 推送已展示过一条通知，避免同一呼叫出现两条通知
                if (message.getStringAttribute(Constant.CALL_MSG_TYPE, "") == Constant.CALL_MSG_INFO) {
                    return@forEach
                }
                if (DemoApplication.getInstance().getLifecycleCallbacks().isFront.not()) {
                    DemoHelper.getInstance().getNotifier()?.notify(message)
                }
            }
        }
    } }

    private val presenceListener by lazy{
        ChatPresenceListener {
            defaultPresencesEvent(it)
        }
    }

    private fun defaultPresencesEvent(presences: MutableList<ChatPresence>?){
        presences?.forEach { presence->
            PresenceCache.insertPresences(presence.publisher,presence)
            ChatUIKitClient.getContext()?.let {
                ChatUIKitFlowBus.with<ChatUIKitEvent>(ChatUIKitEvent.EVENT.UPDATE.name)
                    .post(it.mainScope(), ChatUIKitEvent(ChatUIKitEvent.EVENT.UPDATE.name, ChatUIKitEvent.TYPE.PRESENCE,presence.publisher))
            }
        }
    }

    private val contactListener by lazy { object : ChatUIKitContactListener(){

        override fun onFriendRequestAccepted(username: String?) {
            val notifyMsg = LocalNotifyHelper.createContactNotifyMessage(username)
            notifyMsg?.let {
                ChatClient.getInstance().chatManager().saveMessage(notifyMsg)
                DemoHelper.getInstance().context.let {
                    ChatUIKitFlowBus.with<ChatUIKitEvent>(ChatUIKitEvent.EVENT.ADD.name)
                        .post(it.mainScope(), ChatUIKitEvent(ChatUIKitEvent.EVENT.ADD.name, ChatUIKitEvent.TYPE.CONTACT))
                }
            }
        }

        override fun onContactDeleted(username: String?) {
            LocalNotifyHelper.removeContactNotifyMessage(username)
        }
    } }

    fun registerListeners() {
        // register connection listener
        ChatUIKitClient.addConnectionListener(connectListener)
        ChatUIKitClient.addChatMessageListener(messageListener)
        ChatUIKitClient.addPresenceListener(presenceListener)
        ChatUIKitClient.addContactListener(contactListener)
    }
}
