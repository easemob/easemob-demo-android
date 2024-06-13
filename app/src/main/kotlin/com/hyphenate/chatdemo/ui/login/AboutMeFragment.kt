package com.hyphenate.chatdemo.ui.login

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.hyphenate.chatdemo.DemoApplication
import com.hyphenate.chatdemo.DemoHelper
import com.hyphenate.chatdemo.R
import com.hyphenate.chatdemo.common.DemoConstant
import com.hyphenate.chatdemo.databinding.DemoFragmentAboutMeBinding
import com.hyphenate.chatdemo.ui.me.CurrencyActivity
import com.hyphenate.chatdemo.ui.me.NotifyActivity
import com.hyphenate.chatdemo.ui.me.UserInformationActivity
import com.hyphenate.chatdemo.viewmodel.LoginViewModel
import com.hyphenate.easeui.EaseIM
import com.hyphenate.easeui.base.EaseBaseFragment
import com.hyphenate.easeui.common.ChatClient
import com.hyphenate.easeui.common.ChatLog
import com.hyphenate.easeui.common.ChatPresence
import com.hyphenate.easeui.common.bus.EaseFlowBus
import com.hyphenate.easeui.common.dialog.CustomDialog
import com.hyphenate.easeui.common.extensions.catchChatException
import com.hyphenate.easeui.common.extensions.dpToPx
import com.hyphenate.easeui.common.utils.EasePresenceUtil
import com.hyphenate.easeui.configs.setStatusStyle
import com.hyphenate.easeui.feature.chat.interfaces.IPresenceResultView
import com.hyphenate.easeui.model.EaseEvent
import com.hyphenate.easeui.viewmodel.presence.EasePresenceViewModel
import com.hyphenate.easeui.viewmodel.presence.IPresenceRequest
import com.hyphenate.easeui.widget.EasePresenceView
import kotlinx.coroutines.launch

class AboutMeFragment: EaseBaseFragment<DemoFragmentAboutMeBinding>(), View.OnClickListener,
    EasePresenceView.OnPresenceClickListener,IPresenceResultView{

    /**
     * The clipboard manager.
     */
    private val clipboard by lazy { mContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    private lateinit var loginViewModel: LoginViewModel
    private var presenceViewModel:IPresenceRequest? = null

    companion object{
        private val TAG = AboutMeFragment::class.java.simpleName
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): DemoFragmentAboutMeBinding {
        return DemoFragmentAboutMeBinding.inflate(inflater)
    }

    override fun initView(savedInstanceState: Bundle?) {
        super.initView(savedInstanceState)
        initPresence()
        initStatus()
        EaseIM.getConfig()?.presencesConfig?.enablePresences?.let {
             if (it){
                 binding?.itemPresence?.visibility = View.VISIBLE
             }else{
                 binding?.itemPresence?.visibility = View.GONE
             }
        }
    }
    override fun initViewModel() {
        super.initViewModel()
        loginViewModel = ViewModelProvider(this)[LoginViewModel::class.java]
        presenceViewModel = ViewModelProvider(this)[EasePresenceViewModel::class.java]
        presenceViewModel?.attachView(this)
    }

    override fun initListener() {
        super.initListener()
        binding?.run {
            epPresence.setOnPresenceClickListener(this@AboutMeFragment)
            tvNumber.setOnClickListener(this@AboutMeFragment)
            itemPresence.setOnClickListener(this@AboutMeFragment)
            itemInformation.setOnClickListener(this@AboutMeFragment)
            itemCurrency.setOnClickListener(this@AboutMeFragment)
            itemNotify.setOnClickListener(this@AboutMeFragment)
            itemPrivacy.setOnClickListener(this@AboutMeFragment)
            itemAbout.setOnClickListener(this@AboutMeFragment)
            aboutMeLogout.setOnClickListener(this@AboutMeFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    override fun initData() {
        super.initData()
        fetchCurrentPresence()
        initEvent()
    }

    private fun initEvent() {
        EaseFlowBus.with<EaseEvent>(EaseEvent.EVENT.UPDATE.name).register(this) {
            if (it.isPresenceChange ) {
                updatePresence()
            }
        }
        EaseFlowBus.with<EaseEvent>(EaseEvent.EVENT.UPDATE + EaseEvent.TYPE.CONTACT).register(this) {
            if (it.isContactChange && it.event == DemoConstant.EVENT_UPDATE_SELF) {
                updatePresence()
            }
        }
    }

    private fun initPresence(){
        binding?.run {
            var name:String? = ChatClient.getInstance().currentUser
            val id = getString(R.string.main_about_me_id,ChatClient.getInstance().currentUser)
            EaseIM.getConfig()?.avatarConfig?.setStatusStyle(epPresence.getStatusView(),4.dpToPx(mContext),
                ContextCompat.getColor(mContext, com.hyphenate.easeui.R.color.ease_color_background))
            epPresence.setPresenceStatusMargin(end = -3, bottom = -3)
            epPresence.setPresenceStatusSize(resources.getDimensionPixelSize(com.hyphenate.easeui.R.dimen.ease_contact_status_icon_size))

            val layoutParams = epPresence.getUserAvatar().layoutParams
            layoutParams.width = 100.dpToPx(mContext)
            layoutParams.height = 100.dpToPx(mContext)
            epPresence.getUserAvatar().layoutParams = layoutParams

            EaseIM.getCurrentUser()?.let {
                epPresence.setPresenceData(it)
                name = it.getRemarkOrName()
            }
            tvName.text = name
            tvNumber.text = id
        }
    }

    private fun updatePresence(){
        EaseIM.getCurrentUser()?.let { user->
            val presence = EaseIM.getUserPresence(user.id)
            presence?.let {
                binding?.epPresence?.setPresenceData(user,it)
                val subtitle = EasePresenceUtil.getPresenceString(mContext,it)
                binding?.itemPresence?.setContent(subtitle)
            }
            if (presence == null){
                binding?.epPresence?.setPresenceData(user)
            }
            binding?.tvName?.text = user.getNotEmptyName()
        }
    }

    private fun initStatus(){
        val isSilent = EaseIM.checkMutedConversationList(ChatClient.getInstance().currentUser)
        if (isSilent) {
            binding?.icNotice?.visibility = View.VISIBLE
        }else{
            binding?.icNotice?.visibility = View.GONE
        }
    }

    private fun logout() {
        lifecycleScope.launch {
            loginViewModel.logout()
                .catchChatException { e ->
                    ChatLog.e("logout", "logout failed: ${e.description}")
                }
                .collect {
                    DemoHelper.getInstance().getDataModel().clearCache()
                    DemoApplication.getInstance().getLifecycleCallbacks().skipToTarget(
                        LoginActivity::class.java)
                }
        }
    }

    private fun fetchCurrentPresence(){
        presenceViewModel?.fetchPresenceStatus(mutableListOf(ChatClient.getInstance().currentUser))
    }

    override fun onPresenceClick(v: View?) {

    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.item_presence -> {

            }
            R.id.item_information -> {
                startActivity(Intent(mContext, UserInformationActivity::class.java))
            }
            R.id.item_currency -> {
                startActivity(Intent(mContext, CurrencyActivity::class.java))
            }
            R.id.item_notify -> {
                startActivity(Intent(mContext, NotifyActivity::class.java))
            }
            R.id.item_privacy -> {

            }
            R.id.item_about -> {
                var clazz:Class<*>?
                try {
                    clazz  = Class.forName("com.hyphenate.chatdemo.EMActivity")
                }catch (e:Exception){
                    clazz = Class.forName("com.hyphenate.chatdemo.ui.me.AboutActivity")
                }
                startActivity(Intent(mContext, clazz))
            }
            R.id.about_me_logout -> {
                showLogoutDialog()
            }
            R.id.tv_number -> {
                val indexOfSpace = binding?.tvNumber?.text?.indexOf(":")
                indexOfSpace?.let {
                    if (indexOfSpace != -1) {
                        val substring = binding?.tvNumber?.text?.substring(indexOfSpace + 1)
                        clipboard.setPrimaryClip(
                            ClipData.newPlainText(
                                null,
                                substring
                            )
                        )
                    }
                }

            }
            else -> {}
        }
    }

    private fun showLogoutDialog(){
        val logoutDialog = CustomDialog(
            context = mContext,
            title = resources.getString(R.string.em_login_out_hint),
            isEditTextMode = false,
            onLeftButtonClickListener = {

            },
            onRightButtonClickListener = {
                logout()
            }
        )
        logoutDialog.show()
    }

    override fun fetchPresenceStatusSuccess(presence: MutableList<ChatPresence>) {
        updatePresence()
    }

    override fun fetchPresenceStatusFail(code: Int, message: String?) {

    }

}