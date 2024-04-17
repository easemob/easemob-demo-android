package com.hyphenate.chatdemo.common.extensions.internal

import com.hyphenate.easeui.common.ChatGroup
import com.hyphenate.easeui.model.EaseGroupProfile

internal fun ChatGroup.parse(): EaseGroupProfile {
    return EaseGroupProfile(groupId, groupName, extension)
}