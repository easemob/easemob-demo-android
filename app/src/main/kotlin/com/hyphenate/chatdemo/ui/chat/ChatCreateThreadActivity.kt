package com.hyphenate.chatdemo.ui.chat

import com.hyphenate.chatdemo.DemoHelper
import com.hyphenate.chatdemo.common.DemoConstant
import com.hyphenate.easeui.feature.thread.ChatUIKitCreateThreadActivity
import com.hyphenate.easeui.feature.thread.fragment.ChatUIKitCreateThreadFragment

class ChatCreateThreadActivity: ChatUIKitCreateThreadActivity() {

    override fun setChildSettings(builder: ChatUIKitCreateThreadFragment.Builder) {
        super.setChildSettings(builder)
        builder.sendMessageByOriginalImage(
            DemoHelper.getInstance().getDataModel().getBoolean(DemoConstant.FEATURES_SEND_ORIGINAL_IMAGE, false)
        )
    }
}
