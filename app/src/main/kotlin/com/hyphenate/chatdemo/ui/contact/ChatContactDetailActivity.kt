package com.hyphenate.chatdemo.ui.contact

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.hyphenate.chatdemo.R
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.hyphenate.chatdemo.DemoHelper
import com.hyphenate.chatdemo.callkit.CallKitManager
import com.hyphenate.chatdemo.common.DemoConstant
import com.hyphenate.chatdemo.common.PresenceCache
import com.hyphenate.chatdemo.common.room.entity.parse
import com.hyphenate.chatdemo.common.room.extensions.parseToDbBean
import com.hyphenate.chatdemo.interfaces.IPresenceResultView
import com.hyphenate.chatdemo.utils.EasePresenceUtil
import com.hyphenate.chatdemo.viewmodel.ProfileInfoViewModel
import com.hyphenate.chatdemo.viewmodel.PresenceViewModel
import com.hyphenate.easeui.EaseIM
import com.hyphenate.easeui.common.ChatClient
import com.hyphenate.easeui.common.ChatLog
import com.hyphenate.easeui.common.ChatPresence
import com.hyphenate.easeui.common.ChatUserInfoType
import com.hyphenate.easeui.common.bus.EaseFlowBus
import com.hyphenate.easeui.common.extensions.catchChatException
import com.hyphenate.easeui.common.extensions.toProfile
import com.hyphenate.easeui.feature.contact.EaseContactDetailsActivity
import com.hyphenate.easeui.model.EaseEvent
import com.hyphenate.easeui.model.EaseMenuItem
import com.hyphenate.easeui.widget.EaseArrowItemView
import kotlinx.coroutines.launch


class ChatContactDetailActivity:EaseContactDetailsActivity(), IPresenceResultView {
    private lateinit var model: ProfileInfoViewModel
    private lateinit var presenceModel: PresenceViewModel
    private val remarkItem: EaseArrowItemView by lazy { findViewById(R.id.item_remark) }

    companion object {
        private const val TAG = "ChatContactDetailActivity"
        private const val REQUEST_UPDATE_REMARK = 100
        private const val RESULT_UPDATE_REMARK = "result_update_remark"
    }

    private val launcherToUpdateRemark: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result -> onActivityResult(result, REQUEST_UPDATE_REMARK) }

    override fun initView() {
        super.initView()
        model = ViewModelProvider(this)[ProfileInfoViewModel::class.java]
        presenceModel = ViewModelProvider(this)[PresenceViewModel::class.java]
        presenceModel.attachView(this)
        updateUserInfo()
    }

    override fun initListener() {
        super.initListener()
        remarkItem.setOnClickListener{
            user?.let {
                launcherToUpdateRemark.launch(ChatContactRemarkActivity.createIntent(mContext,it.userId))
            }
        }
        binding.tvNumber
    }

    override fun initEvent() {
        super.initEvent()
        EaseFlowBus.with<EaseEvent>(EaseEvent.EVENT.UPDATE.name).register(this) {
            if (it.isPresenceChange ) {
                updatePresence()
            }
        }
    }

    override fun initData() {
        super.initData()
        user?.let {
            presenceModel.fetchChatPresence(mutableListOf(it.userId))
        }
        lifecycleScope.launch {
            user?.let { user->
                model.fetchUserInfoAttribute(listOf(user.userId), listOf(ChatUserInfoType.NICKNAME, ChatUserInfoType.AVATAR_URL))
                    .catchChatException {
                        ChatLog.e("ContactDetail", "fetchUserInfoAttribute error: ${it.description}")
                    }
                    .collect {
                        it[user.userId]?.parseToDbBean()?.let {u->
                            u.parse().apply {
                                remark = ChatClient.getInstance().contactManager().fetchContactFromLocal(id)?.remark
                                EaseIM.updateUsersInfo(mutableListOf(this))
                                DemoHelper.getInstance().getDataModel().insertUser(this)
                            }
                            updateUserInfo()
                            notifyUpdateRemarkEvent()
                        }
                    }
            }
        }
    }

    private fun updateUserInfo() {
        DemoHelper.getInstance().getDataModel().getUser(user?.userId)?.let {
            binding.epPresence.setUserAvatarData(it.parse())
            binding.tvName.text = it.parse().getRemarkOrName()
            binding.tvNumber.text = it.userId
            remarkItem.setContent(it.remark)
        }
    }

    override fun getDetailItem(): MutableList<EaseMenuItem>? {
        val list = super.getDetailItem()
        val audioItem = EaseMenuItem(
            title = getString(R.string.detail_item_audio),
            resourceId = R.drawable.ease_phone_pick,
            menuId = R.id.contact_item_audio_call,
            titleColor = ContextCompat.getColor(this, com.hyphenate.easeui.R.color.ease_color_primary),
            order = 2
        )

        val videoItem = EaseMenuItem(
            title = getString(R.string.detail_item_video),
            resourceId = R.drawable.ease_video_camera,
            menuId = R.id.contact_item_video_call,
            titleColor = ContextCompat.getColor(this, com.hyphenate.easeui.R.color.ease_color_primary),
            order = 3
        )
        list?.add(audioItem)
        list?.add(videoItem)
        return list
    }

    override fun onMenuItemClick(item: EaseMenuItem?, position: Int): Boolean {
        item?.let {
            when(item.menuId){
                R.id.contact_item_audio_call -> {
                    CallKitManager.startSingleAudioCall(user?.userId)
                    return true
                }
                R.id.contact_item_video_call -> {
                    CallKitManager.startSingleVideoCall(user?.userId)
                    return true
                }
                else -> {
                    return super.onMenuItemClick(item, position)
                }
            }
        }
        return false
    }

    private fun onActivityResult(result: ActivityResult, requestCode: Int) {
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            when (requestCode) {
                REQUEST_UPDATE_REMARK ->{
                    data?.let {
                        if (it.hasExtra(RESULT_UPDATE_REMARK)){
                            notifyUpdateRemarkEvent()
                            updateUserInfo()
                        }
                    }
                }
                else -> {}
            }
        }
    }

    private fun notifyUpdateRemarkEvent() {
        EaseFlowBus.with<EaseEvent>(EaseEvent.EVENT.UPDATE + EaseEvent.TYPE.CONTACT + DemoConstant.EVENT_UPDATE_USER_SUFFIX)
            .post(lifecycleScope, EaseEvent(DemoConstant.EVENT_UPDATE_USER_SUFFIX, EaseEvent.TYPE.CONTACT, user?.userId))
    }

    private fun updatePresence(){
        val map = PresenceCache.getPresenceInfo
        user?.let { user->
            map.let {
                binding.epPresence.setUserAvatarData(user.toProfile(),EasePresenceUtil.getPresenceIcon(mContext,it[user.userId]))
            }
        }
    }

    override fun fetchChatPresenceSuccess(presence: MutableList<ChatPresence>) {
        ChatLog.e(TAG,"fetchChatPresenceSuccess $presence")
        updatePresence()
    }

    override fun fetchChatPresenceFail(code: Int, error: String) {
        ChatLog.e(TAG,"fetchChatPresenceFail $code $error")
    }

}