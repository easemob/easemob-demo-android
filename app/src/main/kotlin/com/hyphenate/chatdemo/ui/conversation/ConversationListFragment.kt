package com.hyphenate.chatdemo.ui.conversation

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.hyphenate.chatdemo.DemoHelper
import com.hyphenate.chatdemo.R
import com.hyphenate.chatdemo.common.DemoConstant
import com.hyphenate.chatdemo.common.PresenceCache
import com.hyphenate.chatdemo.controller.PresenceController
import com.hyphenate.chatdemo.utils.EasePresenceUtil
import com.hyphenate.chatdemo.viewmodel.ChatContactViewModel
import com.hyphenate.chatdemo.viewmodel.PresenceViewModel
import com.hyphenate.easeui.EaseIM
import com.hyphenate.easeui.common.ChatLog
import com.hyphenate.easeui.common.bus.EaseFlowBus
import com.hyphenate.easeui.common.extensions.dpToPx
import com.hyphenate.easeui.common.extensions.showToast
import com.hyphenate.easeui.configs.setAvatarStyle
import com.hyphenate.easeui.configs.setStatusStyle
import com.hyphenate.easeui.feature.conversation.EaseConversationListFragment
import com.hyphenate.easeui.model.EaseConversation
import com.hyphenate.easeui.model.EaseEvent

class ConversationListFragment: EaseConversationListFragment() {

    private var isFirstLoadData = false
    private val chatContactViewModel by lazy { ViewModelProvider(this)[ChatContactViewModel::class.java] }
    private val presenceViewModel by lazy { ViewModelProvider(this)[PresenceViewModel::class.java] }
    private val presenceController by lazy { PresenceController(mContext,presenceViewModel) }

    override fun initData() {
        super.initData()
        initEventBus()
    }

    override fun initView(savedInstanceState: Bundle?) {
        super.initView(savedInstanceState)

        binding?.titleConversations?.let {
            EaseIM.getConfig()?.avatarConfig?.setAvatarStyle(it.getLogoView())
            EaseIM.getConfig()?.avatarConfig?.setStatusStyle(it.getStatusView(),2.dpToPx(mContext),
                ContextCompat.getColor(mContext, com.hyphenate.easeui.R.color.ease_color_background))
            updateProfile()
            it.setTitleEndDrawable(R.drawable.conversation_title)
        }
    }

    private fun initEventBus() {
        EaseFlowBus.with<EaseEvent>(EaseEvent.EVENT.UPDATE + EaseEvent.TYPE.CONTACT).register(this) {
            if (it.isContactChange && it.event == DemoConstant.EVENT_UPDATE_SELF) {
                updateProfile()
            }
        }

        EaseFlowBus.with<EaseEvent>(EaseEvent.EVENT.UPDATE.name).register(this) {
            if (it.isPresenceChange && it.message.equals(EaseIM.getCurrentUser()?.id) ) {
                updateProfile()
            }
        }

        EaseFlowBus.with<EaseEvent>(EaseEvent.EVENT.UPDATE + EaseEvent.TYPE.CONTACT + DemoConstant.EVENT_UPDATE_USER_SUFFIX).register(this) {
            if (it.isContactChange && it.message.isNullOrEmpty().not()) {
                binding?.listConversation?.notifyDataSetChanged()
            }
        }
        EaseFlowBus.with<EaseEvent>(EaseEvent.EVENT.ADD.name).register(viewLifecycleOwner) {
            if (it.isContactChange) {
                refreshData()
            }
        }
    }

    override fun initListener() {
        super.initListener()
        binding?.titleConversations?.setLogoClickListener {
            EaseIM.getCurrentUser()?.id?.let {
                presenceController.showPresenceStatusDialog(PresenceCache.getUserPresence(it))
            }
        }
    }

    private fun updateProfile(){
        binding?.titleConversations?.let { titlebar->
            EaseIM.getCurrentUser()?.let { profile->
                val presence = PresenceCache.getUserPresence(profile.id)
                presence?.let {
                    val logoStatus = EasePresenceUtil.getPresenceIcon(mContext,it)
                    titlebar.setLogoStatusMargin(end = -1, bottom = -1)
                    titlebar.setLogoStatus(logoStatus)
                    titlebar.getStatusView().visibility = View.VISIBLE
                    titlebar.setLogoStatusSize(resources.getDimensionPixelSize(R.dimen.em_title_bar_status_icon_size))
                }
                ChatLog.e("ConversationListFragment","updateProfile ${profile.id} ${profile.name} ${profile.avatar}")
                titlebar.setLogo(profile.avatar, com.hyphenate.easeui.R.drawable.ease_default_avatar, 32.dpToPx(mContext))
                val layoutParams = titlebar.getLogoView()?.layoutParams as? ViewGroup.MarginLayoutParams
                layoutParams?.marginStart = 12.dpToPx(mContext)
                titlebar.getTitleView().let { text ->
                    text.text = ""
                }
            }
        }
    }

    override fun loadConversationListSuccess(userList: List<EaseConversation>) {
        if (!isFirstLoadData){
            fetchFirstVisibleData()
            isFirstLoadData = true
        }
    }

    private fun fetchFirstVisibleData(){
        binding?.listConversation?.let { layout->
            (layout.conversationList.layoutManager as? LinearLayoutManager)?.let { manager->
                layout.post {
                    val firstVisibleItemPosition = manager.findFirstVisibleItemPosition()
                    val lastVisibleItemPosition = manager.findLastVisibleItemPosition()
                    val visibleList = layout.getListAdapter()?.mData?.filterIndexed { index, _ ->
                        index in firstVisibleItemPosition..lastVisibleItemPosition
                    }
                    val fetchList = visibleList?.filter { conv ->
                        val u = DemoHelper.getInstance().getDataModel().getUser(conv.conversationId)
                        (u == null || u.updateTimes == 0) && (u?.name.isNullOrEmpty() || u?.avatar.isNullOrEmpty())
                    }
                    fetchList?.let {
                        layout.fetchConvUserInfo(it)
                    }
                }
            }
        }
    }

    override fun defaultActionMoreDialog() {
        dialogController.showMoreDialog { content ->
            if (content.isNotEmpty()) {
                chatContactViewModel.addContact(content)
            }
        }
    }

    override fun addContactFail(code: Int, error: String) {
        ChatLog.e("ConversationListFragment","ConversationListFragment addContactFail $code $error")
        if (code == 404){
            mContext.showToast(error)
        }
    }


}