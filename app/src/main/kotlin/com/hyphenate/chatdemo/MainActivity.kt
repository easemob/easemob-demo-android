package com.hyphenate.chatdemo

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationItemView
import com.google.android.material.bottomnavigation.BottomNavigationMenuView
import com.google.android.material.navigation.NavigationBarView
import com.hyphenate.chatdemo.base.BaseInitActivity
import com.hyphenate.chatdemo.common.DemoConstant
import com.hyphenate.chatdemo.databinding.ActivityMainBinding
import com.hyphenate.chatdemo.ui.conversation.ConversationListFragment
import com.hyphenate.chatdemo.interfaces.IMainResultView
import com.hyphenate.chatdemo.ui.me.AboutMeFragment
import com.hyphenate.chatdemo.ui.contact.ChatContactListFragment
import com.hyphenate.chatdemo.viewmodel.MainViewModel
import com.hyphenate.chatdemo.viewmodel.ProfileInfoViewModel
import com.hyphenate.easeui.ChatUIKitClient
import com.hyphenate.easeui.common.ChatError
import com.hyphenate.easeui.common.ChatLog
import com.hyphenate.easeui.common.ChatMessage
import com.hyphenate.easeui.common.ChatUIKitConstant
import com.hyphenate.easeui.common.bus.ChatUIKitFlowBus
import com.hyphenate.easeui.common.extensions.catchChatException
import com.hyphenate.easeui.common.extensions.showToast
import com.hyphenate.easeui.feature.conversation.ChatUIKitConversationListFragment
import com.hyphenate.easeui.interfaces.ChatUIKitContactListener
import com.hyphenate.easeui.interfaces.ChatUIKitMessageListener
import com.hyphenate.easeui.interfaces.OnEventResultListener
import com.hyphenate.easeui.model.ChatUIKitEvent
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


class MainActivity : BaseInitActivity<ActivityMainBinding>(), NavigationBarView.OnItemSelectedListener,
    OnEventResultListener, IMainResultView {
    override fun getViewBinding(inflater: LayoutInflater): ActivityMainBinding? {
        return ActivityMainBinding.inflate(inflater)
    }
    /**
     * The clipboard manager.
     */
    private var mConversationListFragment: Fragment? = null
    private var mContactFragment:Fragment? = null
    private var mAboutMeFragment:Fragment? = null
    private var mCurrentFragment: Fragment? = null
    private val badgeMap = mutableMapOf<Int, TextView>()
    private val mainViewModel: MainViewModel by lazy {
        ViewModelProvider(this)[MainViewModel::class.java]
    }
    private val mProfileViewModel: ProfileInfoViewModel by lazy {
        ViewModelProvider(this)[ProfileInfoViewModel::class.java]
    }

    private val chatMessageListener = object : ChatUIKitMessageListener() {
        override fun onMessageReceived(messages: MutableList<ChatMessage>?) {
            mainViewModel.getUnreadMessageCount()
        }
    }

    companion object {
        fun actionStart(context: Context) {
            Intent(context, MainActivity::class.java).apply {
                context.startActivity(this)
            }
        }
    }

    override fun initView(savedInstanceState: Bundle?) {
        super.initView(savedInstanceState)
        binding.navView.itemIconTintList = null
        mainViewModel.getUnreadMessageCount()
        mainViewModel.getRequestUnreadCount()
        switchToHome()
        checkIfShowSavedFragment(savedInstanceState)
        addTabBadge()
    }

    override fun initListener() {
        super.initListener()
        binding.navView.setOnItemSelectedListener(this)
        ChatUIKitClient.addEventResultListener(this)
        ChatUIKitClient.addChatMessageListener(chatMessageListener)
        ChatUIKitClient.addContactListener(contactListener)
    }

    override fun initData() {
        super.initData()
        mainViewModel.attachView(this)
        synchronizeProfile()
        ChatUIKitFlowBus.with<ChatUIKitEvent>(ChatUIKitEvent.EVENT.ADD.name).register(this){
            // check unread message count
            mainViewModel.getUnreadMessageCount()
        }
        ChatUIKitFlowBus.with<ChatUIKitEvent>(ChatUIKitEvent.EVENT.REMOVE.name).register(this){
            // check unread message count
            mainViewModel.getUnreadMessageCount()
        }
        ChatUIKitFlowBus.with<ChatUIKitEvent>(ChatUIKitEvent.EVENT.DESTROY.name).register(this){
            // check unread message count
            mainViewModel.getUnreadMessageCount()
        }
        ChatUIKitFlowBus.with<ChatUIKitEvent>(ChatUIKitEvent.EVENT.LEAVE.name).register(this){
            // check unread message count
            mainViewModel.getUnreadMessageCount()
        }
        ChatUIKitFlowBus.with<ChatUIKitEvent>(ChatUIKitEvent.EVENT.UPDATE.name).register(this){
            // check unread message count
            mainViewModel.getUnreadMessageCount()
        }
        ChatUIKitFlowBus.withStick<ChatUIKitEvent>(ChatUIKitEvent.EVENT.UPDATE.name).register(this){
            // check unread message count
            mainViewModel.getUnreadMessageCount()
        }
        ChatUIKitFlowBus.with<ChatUIKitEvent>(ChatUIKitEvent.EVENT.UPDATE.name).register(this) {
            if (it.isNotifyChange) {
                mainViewModel.getRequestUnreadCount()
            }
        }
        ChatUIKitFlowBus.with<ChatUIKitEvent>(ChatUIKitEvent.EVENT.ADD.name).register(this) {
            if (it.isNotifyChange) {
                mainViewModel.getRequestUnreadCount()
            }
        }
        ChatUIKitFlowBus.with<ChatUIKitEvent>(ChatUIKitEvent.EVENT.ADD + ChatUIKitEvent.TYPE.CONVERSATION).register(this) {
            if (it.isConversationChange) {
                mainViewModel.getUnreadMessageCount()
            }
        }
    }

    private fun switchToHome() {
        if (mConversationListFragment == null) {
            mConversationListFragment = ChatUIKitConversationListFragment.Builder()
                .useTitleBar(true)
                .enableTitleBarPressBack(false)
                .useSearchBar(true)
                .setCustomFragment(ConversationListFragment())
                .build()
        }
        mConversationListFragment?.let {
            replace(it, "conversation")
        }
    }

    private fun switchToContacts() {
        if (mContactFragment == null) {
            mContactFragment = ChatContactListFragment.Builder()
                .useTitleBar(true)
                .useSearchBar(true)
                .enableTitleBarPressBack(false)
                .setHeaderItemVisible(true)
                .build()
        }
        mContactFragment?.let {
            replace(it, "contact")
        }
    }

    private fun switchToAboutMe() {
        if (mAboutMeFragment == null) {
            mAboutMeFragment = AboutMeFragment()
        }
        mAboutMeFragment?.let {
            replace(it, "me")
        }
    }

    override fun onDestroy() {
        ChatUIKitClient.removeEventResultListener(this)
        ChatUIKitClient.removeContactListener(contactListener)
        ChatUIKitClient.removeChatMessageListener(chatMessageListener)
        super.onDestroy()
    }

    private fun replace(fragment: Fragment, tag: String) {
        if (mCurrentFragment !== fragment) {
            val t = supportFragmentManager.beginTransaction()
            mCurrentFragment?.let {
                t.hide(it)
            }
            mCurrentFragment = fragment
            if (!fragment.isAdded) {
                t.add(R.id.fl_main_fragment, fragment, tag).show(fragment).commit()
            } else {
                t.show(fragment).commit()
            }
        }
    }

    /**
     * 用于展示是否已经存在的Fragment
     * @param savedInstanceState
     */
    private fun checkIfShowSavedFragment(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            val tag = savedInstanceState.getString("tag")
            if (!tag.isNullOrEmpty()) {
                val fragment = supportFragmentManager.findFragmentByTag(tag)
                if (fragment is Fragment) {
                    replace(fragment, tag)
                }
            }
        }
    }

    @SuppressLint("RestrictedApi")
    private fun addTabBadge() {
        (binding.navView.getChildAt(0) as? BottomNavigationMenuView)?.let { menuView->
            val childCount = menuView.childCount
            for (i in 0 until childCount) {
                val itemView = menuView.getChildAt(i) as BottomNavigationItemView
                val badge = LayoutInflater.from(this).inflate(R.layout.demo_badge_home, menuView, false)
                badgeMap[i] = badge.findViewById(R.id.tv_main_home_msg)
                itemView.addView(badge)
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        var showNavigation = false
        when (item.itemId) {
            R.id.em_main_nav_home -> {
                switchToHome()
                showNavigation = true
            }

            R.id.em_main_nav_friends -> {
                switchToContacts()
                showNavigation = true
            }

            R.id.em_main_nav_me -> {
                switchToAboutMe()
                showNavigation = true
            }
        }
        invalidateOptionsMenu()
        return showNavigation
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (mCurrentFragment != null) {
            outState.putString("tag", mCurrentFragment!!.tag)
        }
    }

    override fun onEventResult(function: String, errorCode: Int, errorMessage: String?) {
        when(function){
            ChatUIKitConstant.API_ASYNC_ADD_CONTACT -> {
                if (errorCode == ChatError.EM_NO_ERROR){
                    runOnUiThread{
                        showToast(R.string.em_main_add_contact_success)
                    }
                }else{
                    runOnUiThread{
                        showToast(errorMessage.toString())
                    }
                }
            }
            else -> {}
        }
    }

    override fun getUnreadCountSuccess(count: String?) {
        if (count.isNullOrEmpty()) {
            badgeMap[0]?.text = ""
            badgeMap[0]?.visibility = View.GONE
        } else {
            badgeMap[0]?.text = count
            badgeMap[0]?.visibility = View.VISIBLE
        }
    }

    override fun getRequestUnreadCountSuccess(count: String?) {
        if (count.isNullOrEmpty()) {
            badgeMap[1]?.text = ""
            badgeMap[1]?.visibility = View.GONE
        } else {
            badgeMap[1]?.text = count
            badgeMap[1]?.visibility = View.VISIBLE
        }
    }

    private val contactListener = object : ChatUIKitContactListener() {
        override fun onContactInvited(username: String?, reason: String?) {
            mainViewModel.getRequestUnreadCount()
        }
    }

    private fun synchronizeProfile(){
        lifecycleScope.launch {
            mProfileViewModel.synchronizeProfile()
                .onCompletion { dismissLoading() }
                .catchChatException { e ->
                    ChatLog.e("MainActivity", " synchronizeProfile fail error message = " + e.description)
                }
                .stateIn(lifecycleScope, SharingStarted.WhileSubscribed(5000), null)
                .collect {
                    ChatLog.e("MainActivity","synchronizeProfile result $it")
                    it?.let {
                        ChatUIKitFlowBus.with<ChatUIKitEvent>(ChatUIKitEvent.EVENT.UPDATE + ChatUIKitEvent.TYPE.CONTACT)
                            .post(lifecycleScope, ChatUIKitEvent(DemoConstant.EVENT_UPDATE_SELF, ChatUIKitEvent.TYPE.CONTACT))
                    }
                }
        }

    }

}


