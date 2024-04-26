package com.hyphenate.chatdemo.ui.contact

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.hyphenate.chatdemo.DemoHelper
import com.hyphenate.chatdemo.R
import com.hyphenate.chatdemo.common.DemoConstant
import com.hyphenate.chatdemo.viewmodel.ChatContactViewModel
import com.hyphenate.easeui.common.ChatLog
import com.hyphenate.easeui.common.bus.EaseFlowBus
import com.hyphenate.easeui.feature.contact.EaseContactsListFragment
import com.hyphenate.easeui.model.EaseEvent
import com.hyphenate.easeui.model.EaseUser
import com.hyphenate.easeui.viewmodel.contacts.IContactListRequest

class ChatContactListFragment : EaseContactsListFragment() {

    private var contactViewModel: IContactListRequest? = null
    private var isFirstLoadData = false
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
    }

    override fun initData() {
        super.initData()
        EaseFlowBus.with<EaseEvent>(EaseEvent.EVENT.UPDATE + EaseEvent.TYPE.CONTACT + DemoConstant.EVENT_UPDATE_USER_SUFFIX).register(this) {
            if (it.isContactChange && it.message.isNullOrEmpty().not()) {
                binding?.listContact?.loadContactData(false)
            }
        }
    }

    override fun loadContactListSuccess(userList: MutableList<EaseUser>) {
        super.loadContactListSuccess(userList)
        if (!isFirstLoadData){
            fetchFirstVisibleData()
            isFirstLoadData = true
        }
    }

    override fun loadContactListFail(code: Int, error: String) {
        super.loadContactListFail(code, error)
        ChatLog.e(TAG,"loadContactListFail: $code $error")
    }

    private fun fetchFirstVisibleData(){
        binding?.listContact?.let {
            (it.rvContactList.layoutManager as? LinearLayoutManager)?.let { manager->
                it.post {
                    val firstVisibleItemPosition = manager.findFirstVisibleItemPosition()
                    val lastVisibleItemPosition = manager.findLastVisibleItemPosition()
                    val visibleList = it.getListAdapter()?.mData?.filterIndexed { index, _ ->
                        index in firstVisibleItemPosition..lastVisibleItemPosition
                    }
                    val fetchList = visibleList?.filter { user ->
                        val u = DemoHelper.getInstance().getDataModel().getUser(user.userId)
                        (u == null || u.updateTimes == 0) && (u?.name.isNullOrEmpty() || u?.avatar.isNullOrEmpty())
                    }
                    fetchList?.let {
                        contactViewModel?.fetchContactInfo(fetchList)
                    }
                }
            }
        }
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