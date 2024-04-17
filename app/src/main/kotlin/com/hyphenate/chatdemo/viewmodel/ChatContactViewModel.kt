package com.hyphenate.chatdemo.viewmodel

import androidx.lifecycle.viewModelScope
import com.hyphenate.chatdemo.DemoHelper
import com.hyphenate.chatdemo.common.room.entity.parse
import com.hyphenate.easeui.EaseIM
import com.hyphenate.easeui.common.ChatClient
import com.hyphenate.easeui.common.extensions.catchChatException
import com.hyphenate.easeui.common.extensions.toUser
import com.hyphenate.easeui.common.helper.ContactSortedHelper
import com.hyphenate.easeui.model.EaseProfile
import com.hyphenate.easeui.model.EaseUser
import com.hyphenate.easeui.model.setUserInitialLetter
import com.hyphenate.easeui.viewmodel.contacts.EaseContactListViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatContactViewModel: EaseContactListViewModel() {
     private var contactRepository = ChatContactRepository()

    override fun loadData(fetchServerData: Boolean){
        viewModelScope.launch {
            if (fetchServerData || !EaseIM.isLoadedContactFromServer()) {
                flow {
                    emit(contactRepository.loadLocalContact())
                }
                .flatMapConcat {
                    val sortedList = it.map {
                            user -> user.setUserInitialLetter()
                            user
                        }
                        .let {list ->
                            ContactSortedHelper.sortedList(list).toMutableList()
                        }
                    view?.loadContactListSuccess(sortedList)
                    flow {
                        emit(contactRepository.loadData())
                    }
                }
            } else {
                flow {
                    emit(contactRepository.loadLocalContact())
                }
            }
                .catchChatException { e ->
                    view?.loadContactListFail(e.errorCode, e.description)
                }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis), null)
                .collect {
                    val data = it?.map { user->
                        val contactInfo = ChatClient.getInstance().contactManager().fetchContactFromLocal(user.userId)
                        val profile = DemoHelper.getInstance().getDataModel().getUser(user.userId)?.parse()?: EaseProfile(user.userId)
                        contactInfo?.let { contact->
                            if (contact.remark.isNotEmpty()){
                                profile.remark = contact.remark
                            }
                        }
                        DemoHelper.getInstance().getDataModel().insertUser(profile,false)
                        profile
                    }
                    data?.let { it1 ->
                        EaseIM.updateUsersInfo(it1)
                    }
                    val result = data?.map { it.toUser() }
                    result?.map {
                        it.setUserInitialLetter()
                    }
                    result?.let {
                        val sortedList = ContactSortedHelper.sortedList(it)
                        view?.loadContactListSuccess(sortedList.toMutableList())
                    }
                }
        }
    }

    override fun fetchContactInfo(contactList: List<EaseUser>) {
        contactList?.filter {
            val user = DemoHelper.getInstance().getDataModel().getUser(it.userId)
            (user == null || user.updateTimes == 0) &&
                    (it.nickname.isNullOrEmpty() || it.avatar.isNullOrEmpty())
        }?.apply {
            if (this.isNotEmpty()) {
                super.fetchContactInfo(this)
            }
        }
    }

}