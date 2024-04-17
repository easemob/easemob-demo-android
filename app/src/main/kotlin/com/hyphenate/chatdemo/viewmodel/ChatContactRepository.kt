package com.hyphenate.chatdemo.viewmodel

import com.hyphenate.chatdemo.common.suspend.fetchResultContactsFromServer
import com.hyphenate.easeui.model.EaseUser
import com.hyphenate.easeui.repository.EaseContactListRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChatContactRepository: EaseContactListRepository() {

    /**
     * Load server contacts.
     */
    override suspend fun loadData():List<EaseUser> =
        withContext(Dispatchers.IO){
            chatContactManager.fetchResultContactsFromServer()
        }

}