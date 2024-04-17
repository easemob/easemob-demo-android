package com.hyphenate.chatdemo.common.suspend

import com.hyphenate.easeui.EaseIM
import com.hyphenate.easeui.common.ChatContactManager
import com.hyphenate.easeui.common.ChatException
import com.hyphenate.easeui.common.extensions.toUser
import com.hyphenate.easeui.common.impl.ValueCallbackImpl
import com.hyphenate.easeui.model.EaseUser
import com.hyphenate.easeui.provider.getSyncUser
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


/**
 * Suspend method for [ChatContactManager.fetchContactsFromServer()]
 * @return List<EaseUser> User Information List
 */
suspend fun ChatContactManager.fetchResultContactsFromServer():List<EaseUser>{
    return suspendCoroutine{ continuation ->
        asyncFetchAllContactsFromServer(ValueCallbackImpl(
            onSuccess = { value ->
                EaseIM.setLoadedContactFromServer()
                value?.let {
                    val list = it.map { contact ->
                        EaseIM.getUserProvider()?.getSyncUser(contact.username)?.toUser() ?: EaseUser(contact.username)
                    }
                    continuation.resume(list)
                }
            },
            onError = {code,message-> continuation.resumeWithException(ChatException(code, message)) }
        ))
    }
}