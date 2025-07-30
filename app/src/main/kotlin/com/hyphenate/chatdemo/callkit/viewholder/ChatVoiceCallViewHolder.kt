package com.hyphenate.chatdemo.callkit.viewholder

import android.view.View
import com.hyphenate.chatdemo.callkit.VideoCallActivity
import com.hyphenate.easecallkit.CallKitClient
import com.hyphenate.easecallkit.bean.CallType
import com.hyphenate.easecallkit.bean.Constant
import com.hyphenate.easeui.common.ChatMessage
import com.hyphenate.easeui.common.ChatMessageDirection
import com.hyphenate.easeui.feature.chat.viewholders.ChatUIKitRowViewHolder

class ChatVoiceCallViewHolder(itemView: View): ChatUIKitRowViewHolder(itemView) {

    override fun onBubbleClick(message: ChatMessage?) {
        super.onBubbleClick(message)
        message?.let {
            if (it.getIntAttribute(Constant.CALL_TYPE, 0) == CallType.SINGLE_VOICE_CALL.ordinal) {
                if (it.direct() == ChatMessageDirection.RECEIVE) {
                    // answer call
                    CallKitClient.startSingleCall(
                        CallType.SINGLE_VOICE_CALL, message.from, null,
                        VideoCallActivity::class.java
                    )
                } else {
                    // make call
                    CallKitClient.startSingleCall(
                        CallType.SINGLE_VOICE_CALL, message.to, null,
                        VideoCallActivity::class.java
                    )
                }
            } else {
                if (it.direct() == ChatMessageDirection.RECEIVE) {
                    // answer call
                    CallKitClient.startSingleCall(
                        CallType.SINGLE_VIDEO_CALL, message.from, null,
                        VideoCallActivity::class.java
                    )
                } else {
                    // make call
                    CallKitClient.startSingleCall(
                        CallType.SINGLE_VIDEO_CALL, message.to, null,
                        VideoCallActivity::class.java
                    )
                }
            }
        }
    }
}