package com.hyphenate.chatdemo.common

import com.hyphenate.chatdemo.DemoHelper
import com.hyphenate.chatdemo.repository.EMClientRepository
import com.hyphenate.easeui.ChatUIKitClient
import com.hyphenate.easeui.common.ChatClient
import com.hyphenate.easeui.common.ChatException
import com.hyphenate.util.EMLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 使用本地保存的账号凭证自动登录的统一入口。
 *
 * SDK 5.0 移除了初始化后的自动登录，因此闪屏页启动与收到 FCM 推送时都通过此处登录。
 * 登录成功后 SDK 会自动拉取离线消息（含呼叫信令），从而触发 telecom 来电流程。
 */
object AutoLoginManager {

    private const val TAG = "AutoLoginManager"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val loginMutex = Mutex()

    /**
     * 是否存在可用于自动登录的本地凭证。
     */
    fun hasSavedAccount(): Boolean {
        val dataModel = DemoHelper.getInstance().getDataModel()
        return dataModel.getLoginUserName().isNotBlank() && dataModel.getLoginToken().isNotBlank()
    }

    /**
     * 使用本地保存的账号与 token 登录，成功后初始化本地数据库。
     *
     * 登录失败不会清除本地凭证（可能是网络异常等临时失败，下次仍可重试），
     * 需要清理凭证的场景（如闪屏页）由调用方根据返回值自行处理。
     *
     * @return true 登录成功或已登录同一账号；false 无本地凭证、SDK 未初始化或登录失败
     */
    suspend fun loginWithSavedAccount(): Boolean {
        if (DemoHelper.getInstance().isSDKInited().not()) {
            EMLog.e(TAG, "SDK is not inited, skip auto login.")
            return false
        }
        val dataModel = DemoHelper.getInstance().getDataModel()
        val userName = dataModel.getLoginUserName()
        val token = dataModel.getLoginToken()
        if (userName.isBlank() || token.isBlank()) {
            EMLog.d(TAG, "No saved account, skip auto login.")
            return false
        }
        return loginMutex.withLock {
            // 已登录同一账号时无需重复登录
            if (ChatClient.getInstance().isLoggedIn && ChatUIKitClient.getCurrentUser()?.id == userName) {
                EMLog.d(TAG, "Already logged in as $userName, skip auto login.")
                true
            } else {
                try {
                    EMClientRepository().loginToServer(userName, token, true)
                    dataModel.initDb()
                    EMLog.d(TAG, "Auto login success: $userName")
                    true
                } catch (e: ChatException) {
                    EMLog.e(TAG, "Auto login failed: ${e.errorCode} ${e.description}")
                    false
                }
            }
        }
    }

    /**
     * 非挂起版本的自动登录，供没有协程作用域的场景使用（如 FCM 推送 Service）。
     */
    fun autoLoginAsync() {
        scope.launch {
            loginWithSavedAccount()
        }
    }
}
