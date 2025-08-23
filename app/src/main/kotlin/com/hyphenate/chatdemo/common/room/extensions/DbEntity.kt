package com.hyphenate.chatdemo.common.room.extensions

import android.R.attr.name
import com.hyphenate.callkit.bean.CallKitUserInfo
import com.hyphenate.chatdemo.common.room.entity.DemoUser
import com.hyphenate.easeui.common.ChatUserInfo
import com.hyphenate.easeui.model.ChatUIKitProfile
import com.xiaomi.push.ca

internal fun ChatUIKitProfile.parseToDbBean() = DemoUser(id, name, avatar, remark)
internal fun CallKitUserInfo.parseToDbBean() = DemoUser(userId, nickName, avatar)

internal fun ChatUserInfo.parseToDbBean(): DemoUser {
    return DemoUser(
        userId = userId,
        name = nickname,
        avatar = avatarUrl
    )
}