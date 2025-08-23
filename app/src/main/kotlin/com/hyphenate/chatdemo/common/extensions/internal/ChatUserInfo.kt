package com.hyphenate.chatdemo.common.extensions.internal

import com.hyphenate.callkit.bean.CallKitUserInfo
import com.hyphenate.easeui.common.ChatUserInfo
import com.hyphenate.easeui.model.ChatUIKitProfile

internal fun ChatUserInfo.toProfile(): ChatUIKitProfile {
    return ChatUIKitProfile(
        id = userId,
        name = nickname,
        avatar = avatarUrl
    )
}
internal fun ChatUserInfo.toCallKitUserInfo(): CallKitUserInfo {
    return CallKitUserInfo(
        userId = userId,
        nickName = nickname,
        avatar = avatarUrl
    )
}