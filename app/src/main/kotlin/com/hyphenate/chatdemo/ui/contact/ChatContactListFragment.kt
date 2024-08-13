package com.hyphenate.chatdemo.ui.contact

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
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
import com.hyphenate.easeui.feature.contact.EaseContactsListFragment
import com.hyphenate.easeui.model.EaseEvent
import com.hyphenate.easeui.model.EaseUser
import com.hyphenate.easeui.viewmodel.contacts.IContactListRequest

class ChatContactListFragment : EaseContactsListFragment() {

    private var contactViewModel: IContactListRequest? = null
    private var isFirstLoadData = false
    private val presenceViewModel by lazy { ViewModelProvider(this)[PresenceViewModel::class.java] }
    private val presenceController by lazy { PresenceController(mContext, presenceViewModel) }
    companion object{
        private val TAG = ChatContactListFragment::class.java.simpleName
    }

    override fun initView(savedInstanceState: Bundle?) {
        contactViewModel = ViewModelProvider(context as AppCompatActivity)[ChatContactViewModel::class.java]
        binding?.listContact?.setViewModel(contactViewModel)
        super.initView(savedInstanceState)
        binding?.titleContact?.let {
            it.setTitle("")
            it.setTitleEndDrawable(R.drawable.contact_title)
        }
        updateProfile(true)
    }

    override fun initData() {
        super.initData()
        EaseFlowBus.with<EaseEvent>(EaseEvent.EVENT.UPDATE + EaseEvent.TYPE.CONTACT + DemoConstant.EVENT_UPDATE_USER_SUFFIX).register(this) {
            if (it.isContactChange && it.message.isNullOrEmpty().not()) {
                binding?.listContact?.loadContactData(false)
            }
        }
        EaseFlowBus.with<EaseEvent>(EaseEvent.EVENT.UPDATE + EaseEvent.TYPE.CONTACT).register(this) {
            if (it.isContactChange && it.event == DemoConstant.EVENT_UPDATE_SELF) {
                updateProfile(true)
            }
        }
        EaseFlowBus.with<EaseEvent>(EaseEvent.EVENT.UPDATE.name).register(this) {
            if (it.isPresenceChange && it.message.equals(EaseIM.getCurrentUser()?.id) ) {
                updateProfile()
            }
        }
    }

    override fun initListener() {
        super.initListener()
        binding?.titleContact?.setLogoClickListener {
            EaseIM.getCurrentUser()?.id?.let {
                presenceController.showPresenceStatusDialog(PresenceCache.getUserPresence(it))
            }
        }
    }

    override fun setMenuItemClick(item: MenuItem): Boolean {
        when(item.itemId) {
            com.hyphenate.easeui.R.id.action_add_contact -> {
                dialogController.showAddContactDialog { content ->
                    if (content.isNotEmpty()) {
                        contactViewModel?.addContact(content)
                    }
                }
                return true
            }
            else -> return false
        }
    }

    override fun addContactFail(code: Int, error: String) {
       if (code == 404){
           mContext.showToast(error)
       }
    }

    private fun updateProfile(isRefreshAvatar:Boolean = false){
        binding?.titleContact?.let { titlebar->
            EaseIM.getConfig()?.avatarConfig?.setAvatarStyle(titlebar.getLogoView())
            EaseIM.getConfig()?.avatarConfig?.setStatusStyle(titlebar.getStatusView(),2.dpToPx(mContext),
                ContextCompat.getColor(mContext, com.hyphenate.easeui.R.color.ease_color_background))

            EaseIM.getCurrentUser()?.let { profile->
                val presence = PresenceCache.getUserPresence(profile.id)
                presence?.let {
                    val logoStatus = EasePresenceUtil.getPresenceIcon(mContext,it)
                    titlebar.setLogoStatusMargin(end = -1, bottom = -1)
                    titlebar.setLogoStatus(logoStatus)
                    titlebar.getStatusView().visibility = View.VISIBLE
                    titlebar.setLogoStatusSize(resources.getDimensionPixelSize(R.dimen.em_title_bar_status_icon_size))
                }
                ChatLog.e(TAG,"updateProfile ${profile.id} ${profile.name} ${profile.avatar}")
                if (isRefreshAvatar){
                    titlebar.setLogo(profile.avatar, com.hyphenate.easeui.R.drawable.ease_default_avatar, 32.dpToPx(mContext))
                }
                val layoutParams = titlebar.getLogoView()?.layoutParams as? ViewGroup.MarginLayoutParams
                layoutParams?.marginStart = 12.dpToPx(mContext)
                titlebar.getTitleView().let { text ->
                    text.text = ""
                }
            }
        }
    }

    override fun loadContactListSuccess(userList: MutableList<EaseUser>) {
        super.loadContactListSuccess(userList)
        if (!isFirstLoadData){
            fetchContactInfo(userList)
            isFirstLoadData = true
        }
    }

    override fun loadContactListFail(code: Int, error: String) {
        super.loadContactListFail(code, error)
        ChatLog.e(TAG,"loadContactListFail: $code $error")
    }

    class Builder:EaseContactsListFragment.Builder() {
        override fun build(): EaseContactsListFragment {
            if (customFragment == null) {
                customFragment = ChatContactListFragment()
            }
            if (customFragment is ChatContactListFragment){

            }
            return super.build()
        }
    }

}