package com.hyphenate.chatdemo

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.invalidateOptionsMenu
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationItemView
import com.google.android.material.bottomnavigation.BottomNavigationMenuView
import com.google.android.material.navigation.NavigationBarView
import com.hyphenate.callkit.CallKitClient
import com.hyphenate.chatdemo.base.BaseInitActivity
import com.hyphenate.chatdemo.common.DemoConstant
import com.hyphenate.chatdemo.databinding.ActivityMainBinding
import com.hyphenate.chatdemo.ui.conversation.ConversationListFragment
import com.hyphenate.chatdemo.interfaces.IMainResultView
import com.hyphenate.chatdemo.ui.me.AboutMeFragment
import com.hyphenate.chatdemo.ui.contact.ChatContactListFragment
import com.hyphenate.chatdemo.viewmodel.MainViewModel
import com.hyphenate.chatdemo.viewmodel.ProfileInfoViewModel
import com.hyphenate.callkit.telecom.IncomingCallService
import com.hyphenate.callkit.telecom.PhoneAccountHelper
import com.hyphenate.callkit.telecom.PhoneAccountHelper.registerPhoneAccount
import com.hyphenate.callkit.telecom.PhoneAccountHelper.showPhoneAccountEnableGuide
import com.hyphenate.callkit.telecom.VoipConnectionService
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
    private val TAG= "MainActivity"
    /**
     * The clipboard manager.
     */
    private var mConversationListFragment: Fragment? = null
    private var mContactFragment:Fragment? = null
    private var mAboutMeFragment:Fragment? = null
    private var mCurrentFragment: Fragment? = null
    private val badgeMap = mutableMapOf<Int, TextView>()

    private var hasCheckedAccount=false
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
            val intent = BaseInitActivity.createLockScreenIntent(context, MainActivity::class.java)
            context.startActivity(intent)
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
        // 请求必要权限
        requestPermissions()
    }

    fun checkPhoneAccount() {
        val status = PhoneAccountHelper.getPhoneAccountStatus(this)
        when {
            !status.isSupported -> {
                ChatLog.e(TAG, "Telecom not supported on this device")
            }
            !status.isRegistered -> {
                ChatLog.e(TAG, "PhoneAccount not registered, registering now")
                registerPhoneAccount(this)
                if (!hasCheckedAccount){
                    hasCheckedAccount=true
                    binding.root.postDelayed({
                        checkPhoneAccount()
                    }, 2000)
                }
            }
            !status.isEnabled -> {
                ChatLog.e(TAG, "PhoneAccount registered but not enabled, showing enable button")
                // 自动显示引导（可选，根据需要启用）
                showPhoneAccountEnableGuide()
            }
            else -> {
                ChatLog.e(TAG, "PhoneAccount is ready")
            }
        }
    }

    /**
     * 显示PhoneAccount启用引导
     */
    private fun showPhoneAccountEnableGuide() {
        showPhoneAccountEnableGuide(this) { enabled ->
            if (enabled) {
                ChatLog.d(TAG, "PhoneAccount enabled successfully")
                Toast.makeText(this, getString(R.string.demo_check_voip), Toast.LENGTH_LONG).show()
            } else {
                ChatLog.d(TAG, "PhoneAccount still not enabled")
            }
        }
    }

    private  val PERMISSION_REQUEST_CODE = 1001

    private fun getRequiredPermissions() = arrayOf(
            android.Manifest.permission.MANAGE_OWN_CALLS,
            android.Manifest.permission.READ_PHONE_STATE,
            android.Manifest.permission.READ_PHONE_NUMBERS,
            android.Manifest.permission.CALL_PHONE,
            android.Manifest.permission.MODIFY_PHONE_STATE,
            android.Manifest.permission.POST_NOTIFICATIONS
        )

    private fun requestPermissions() {

        val permissionsToRequest = mutableListOf<String>()
        for (permission in getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(permission)
                ChatLog.d(TAG, "Permission not granted: $permission")
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ChatLog.d(TAG, "Requesting permissions: ${permissionsToRequest.joinToString()}")
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        } else {
            ChatLog.d(TAG, "All permissions already granted")
            checkPhoneAccount()
        }
    }

    private fun checkPermissions(): Boolean {
        for (permission in getRequiredPermissions()) {
            val permissionCheck = ContextCompat.checkSelfPermission(this, permission)
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (!checkPermissions()){
                ChatLog.e(TAG, "Required permissions not granted")
            }
            checkPhoneAccount()
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
        CallKitClient.cleanUp()
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


