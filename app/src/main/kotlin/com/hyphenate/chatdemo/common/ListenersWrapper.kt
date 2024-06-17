package com.hyphenate.chatdemo.common

import android.content.Intent
import com.hyphenate.chatdemo.DemoApplication
import com.hyphenate.chatdemo.DemoHelper
import com.hyphenate.chatdemo.ui.login.LoginActivity
import com.hyphenate.easeui.EaseIM
import com.hyphenate.easeui.common.ChatClient
import com.hyphenate.easeui.common.ChatGroup
import com.hyphenate.easeui.common.ChatLog
import com.hyphenate.easeui.common.ChatLoginExtensionInfo
import com.hyphenate.easeui.common.ChatMessage
import com.hyphenate.easeui.common.ChatPresence
import com.hyphenate.easeui.common.ChatPresenceListener
import com.hyphenate.easeui.common.bus.EaseFlowBus
import com.hyphenate.easeui.common.extensions.ioScope
import com.hyphenate.easeui.common.extensions.mainScope
import com.hyphenate.easeui.common.impl.ValueCallbackImpl
import com.hyphenate.easeui.interfaces.EaseConnectionListener
import com.hyphenate.easeui.interfaces.EaseMessageListener
import com.hyphenate.easeui.model.EaseEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object ListenersWrapper {
    private var isLoadGroupList = false

    private val connectListener by lazy {
        object : EaseConnectionListener() {
            override fun onConnected() {
                // do something
                CoroutineScope(Dispatchers.IO).launch {
                    val groups = ChatClient.getInstance().groupManager().allGroups
                    if (isLoadGroupList.not() && groups.isEmpty()) {
                        ChatClient.getInstance().groupManager().asyncGetJoinedGroupsFromServer(ValueCallbackImpl<List<ChatGroup>>(onSuccess = {
                            isLoadGroupList = true
                            if (it.isEmpty().not()) {
                                EaseFlowBus.with<EaseEvent>(EaseEvent.EVENT.UPDATE.name)
                                    .post(DemoHelper.getInstance().context.ioScope(),
                                        EaseEvent(EaseEvent.EVENT.UPDATE.name, EaseEvent.TYPE.GROUP))
                            }
                        }, onError = {_,_ ->

                        }))
                    }
                }

            }

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
        EaseIM.logout(unbindPushToken,
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

    private val messageListener by lazy { object : EaseMessageListener(){
        override fun onMessageReceived(messages: MutableList<ChatMessage>?) {
            super.onMessageReceived(messages)
            if (DemoHelper.getInstance().getDataModel().isAppPushSilent()) {
                return
            }
            // do something
            messages?.forEach { message ->

                if (EaseIM.checkMutedConversationList(message.conversationId())) {
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
        val enablePresences = EaseIM.getConfig()?.presencesConfig?.enablePresences ?: false
        if (enablePresences){
            presences?.forEach { presence->
                PresenceCache.insertPresences(presence.publisher,presence)
                EaseIM.getContext()?.let {
                    EaseFlowBus.with<EaseEvent>(EaseEvent.EVENT.UPDATE.name)
                        .post(it.mainScope(), EaseEvent(EaseEvent.EVENT.UPDATE.name, EaseEvent.TYPE.PRESENCE,presence.publisher))
                }
            }
        }
    }

    fun registerListeners() {
        // register connection listener
        EaseIM.addConnectionListener(connectListener)
        EaseIM.addChatMessageListener(messageListener)
        EaseIM.addPresenceListener(presenceListener)
    }
}