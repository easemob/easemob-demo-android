package com.hyphenate.chatdemo.callkit

import android.app.Application
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.hyphenate.chatdemo.DemoHelper
import com.hyphenate.chatdemo.R
import com.hyphenate.chatdemo.utils.ToastUtils
import com.hyphenate.callkit.CallKitClient
import com.hyphenate.callkit.CallKitConfig
import com.hyphenate.callkit.bean.CallEndReason
import com.hyphenate.callkit.bean.CallInfo
import com.hyphenate.callkit.bean.CallKitGroupInfo
import com.hyphenate.callkit.bean.CallKitUserInfo
import com.hyphenate.callkit.bean.CallType
import com.hyphenate.callkit.interfaces.CallInfoProvider
import com.hyphenate.callkit.interfaces.CallKitListener
import com.hyphenate.callkit.interfaces.OnValueSuccess
import com.hyphenate.chatdemo.common.extensions.internal.toCallKitUserInfo
import com.hyphenate.chatdemo.repository.ProfileInfoRepository
import com.hyphenate.easeui.common.ChatClient
import com.hyphenate.easeui.common.ChatException
import com.hyphenate.easeui.common.ChatLog
import com.hyphenate.easeui.common.ChatUserInfoType
import com.hyphenate.easeui.common.bus.ChatUIKitFlowBus
import com.hyphenate.easeui.common.dialog.SimpleListSheetDialog
import com.hyphenate.easeui.common.dialog.SimpleSheetType
import com.hyphenate.easeui.common.extensions.mainScope
import com.hyphenate.easeui.interfaces.SimpleListSheetItemClickListener
import com.hyphenate.easeui.model.ChatUIKitEvent
import com.hyphenate.easeui.model.ChatUIKitMenuItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object CallKitManager {

    private const val TAG = "callkitManager"

    /**
     * Whether it is a rtc call.
     */
    var isRtcCall = false

    /**
     * Rtc call type.
     */
    var rtcType = 0


    private val callKitListener by lazy {
        object : CallKitListener {
            override fun onEndCallWithReason(reason: CallEndReason, callInfo: CallInfo?) {
                ChatLog.d(TAG, "onEndCallWithReason:  reason: $reason, callInfo: $callInfo")
                // 刷新邀请消息UI展示
                ChatUIKitFlowBus.withStick<ChatUIKitEvent>(ChatUIKitEvent.EVENT.UPDATE.name)
                    .post(
                        DemoHelper.getInstance().context.mainScope(),
                        ChatUIKitEvent(null, ChatUIKitEvent.TYPE.MESSAGE,callInfo?.inviteMessage?.msgId)
                    )
            }

            override fun onCallError(
                errorType: CallKitClient.CallErrorType,
                errorCode: Int,
                description: String?,
            ) {
                ChatLog.e(
                    TAG,
                    "onCallError: errorCode: $errorCode, description: $description, errorType: $errorType "
                )
            }
        }
    }

    private val callInfoProvider by lazy { object : CallInfoProvider{
        override fun asyncFetchUsers(
            userIds: List<String>,
            onValueSuccess: OnValueSuccess<List<CallKitUserInfo>>
        ) {
            // fetch users from server and call call onValueSuccess.onSuccess(users) after successfully getting users
            CoroutineScope(Dispatchers.IO).launch {
                if (userIds.isEmpty()) {
                    onValueSuccess(mutableListOf())
                    return@launch
                }
                try {
                    val users = ProfileInfoRepository().getUserInfoAttribute(userIds, mutableListOf(ChatUserInfoType.NICKNAME, ChatUserInfoType.AVATAR_URL))
                    val callbackList = users.values.map { it.toCallKitUserInfo() }
                    if (callbackList.isNotEmpty()) {
                        DemoHelper.getInstance().getDataModel().insertCallUsers(callbackList)
                        DemoHelper.getInstance().getDataModel().updateCallUsersTimes(callbackList)
                    }
                    onValueSuccess(callbackList)
                }catch (e:ChatException){
                    ChatLog.e("fetchUsers", "fetchUsers error: ${e.description}")
                }
            }
        }

        override fun asyncFetchGroupInfo(
            groupId: String,
            onValueSuccess: OnValueSuccess<CallKitGroupInfo>
        ) {
            ChatClient.getInstance().groupManager().getGroup(groupId)?.let {
                onValueSuccess(CallKitGroupInfo(it.groupId, it.groupName, it.groupAvatar))
            }
        }

    } }

    fun init(context: Context) {
        // 初始化CallKit
        val config = CallKitConfig().apply {

            // 铃声文件配置示例：
            // 使用assets文件夹中的文件：
            outgoingRingFile = "assets://outgoing_ring.mp3"
            incomingRingFile = "assets://incoming_ring.mp3"
            dingRingFile = "assets://ding.mp3"

            // 使用res/raw文件夹中的文件：
//            ringFile = "raw://music.mp3" // 使用res/raw文件夹中的music.mp3作为铃声

            // 使用绝对路径：
            // ringFile = "/path/to/your/ringtone.mp3"
        }
        CallKitClient.init(context, config)

        CallKitClient.callKitListener=callKitListener
        CallKitClient.callInfoProvider=callInfoProvider


    }

    /**
     * Show single chat video call dialog.
     */
    fun showSelectDialog(context: Context, conversationId: String) {
        val context = (context as FragmentActivity)
        val mutableListOf = mutableListOf(
            ChatUIKitMenuItem(
                menuId = R.id.chat_video_call_voice,
                title = context.getString(R.string.voice_call),
                resourceId = R.drawable.phone_pick,
                titleColor = ContextCompat.getColor(
                    context,
                    com.hyphenate.easeui.R.color.ease_color_primary
                ),
                resourceTintColor = ContextCompat.getColor(
                    context,
                    com.hyphenate.easeui.R.color.ease_color_primary
                )
            ),
            ChatUIKitMenuItem(
                menuId = R.id.chat_video_call_video,
                title = context.getString(R.string.video_call),
                resourceId = R.drawable.video_camera,
                titleColor = ContextCompat.getColor(
                    context,
                    com.hyphenate.easeui.R.color.ease_color_primary
                ),
                resourceTintColor = ContextCompat.getColor(
                    context,
                    com.hyphenate.easeui.R.color.ease_color_primary
                )
            ),
        )
        val dialog = SimpleListSheetDialog(
            context = context,
            itemList = mutableListOf,
            type = SimpleSheetType.ITEM_LAYOUT_DIRECTION_START
        )
        dialog.setSimpleListSheetItemClickListener(object : SimpleListSheetItemClickListener {
            override fun onItemClickListener(position: Int, menu: ChatUIKitMenuItem) {
                dialog.dismiss()
                when (menu.menuId) {
                    R.id.chat_video_call_voice -> {
                        startSingleAudioCall(conversationId)
                    }

                    R.id.chat_video_call_video -> {
                        startSingleVideoCall(conversationId)
                    }

                    else -> {}
                }
            }
        })
        context.supportFragmentManager.let { dialog.show(it, "video_call_dialog") }
    }

    /**
     * Start single audio call.
     */
    fun startSingleAudioCall(conversationId: String) {
        CallKitClient.startSingleCall(
            CallType.SINGLE_VOICE_CALL, conversationId, null,
        )
    }

    /**
     * Start single video call.
     */
    fun startSingleVideoCall(conversationId: String) {
        CallKitClient.startSingleCall(CallType.SINGLE_VIDEO_CALL, conversationId, null)
    }

    /**
     * Start conference call.
     */
    fun startGroupCall( groupId: String) {
        CallKitClient.startGroupCall(groupId, null)
    }


}