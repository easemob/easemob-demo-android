package com.hyphenate.chatdemo.repository

import com.hyphenate.chatdemo.DemoApplication
import com.hyphenate.chatdemo.BuildConfig
import com.hyphenate.chatdemo.DemoHelper
import com.hyphenate.chatdemo.R
import com.hyphenate.chatdemo.base.ErrorCode
import com.hyphenate.chatdemo.bean.LoginResult
import com.hyphenate.cloud.HttpClientManager
import com.hyphenate.easeui.ChatUIKitClient
import com.hyphenate.easeui.common.ChatClient
import com.hyphenate.easeui.common.ChatError
import com.hyphenate.easeui.common.ChatException
import com.hyphenate.easeui.common.ChatLog
import com.hyphenate.easeui.common.ChatValueCallback
import com.hyphenate.easeui.common.impl.OnError
import com.hyphenate.easeui.common.impl.OnSuccess
import com.hyphenate.easeui.model.ChatUIKitProfile
import com.hyphenate.easeui.model.ChatUIKitUser
import com.hyphenate.exceptions.HyphenateException
import com.hyphenate.util.EMLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * As the repository of ChatClient, handles ChatClient related logic
 */
class EMClientRepository: BaseRepository() {

    companion object {
        private const val LOGIN_URL = BuildConfig.APP_SERVER_PROTOCOL + "://" + BuildConfig.APP_SERVER_DOMAIN +
                BuildConfig.APP_BASE_USER + BuildConfig.APP_SERVER_LOGIN
        private const val SEND_SMS_URL = BuildConfig.APP_SERVER_PROTOCOL + "://" + BuildConfig.APP_SERVER_DOMAIN +
                BuildConfig.APP_SEND_SMS_FROM_SERVER
        private const val CANCEL_ACCOUNT = BuildConfig.APP_SERVER_PROTOCOL + "://" + BuildConfig.APP_SERVER_DOMAIN +
                BuildConfig.APP_BASE_USER
    }

    /**
     * 登录过后需要加载的数据
     * @return
     */
    suspend fun loadAllInfoFromHX(): Boolean =
        withContext(Dispatchers.IO) {
            suspendCoroutine { continuation ->
                ChatLog.e("login info","isLoggedInBefore ${ChatClient.getInstance().isLoggedInBefore} - autoLogin ${ChatClient.getInstance().options.autoLogin}")
                if (ChatClient.getInstance().isLoggedInBefore && ChatClient.getInstance().options.autoLogin) {
                    loadAllConversationsAndGroups()
                    continuation.resume(true)
                } else {
                    continuation.resumeWithException(ChatException(ErrorCode.EM_NOT_LOGIN, ""))
                }
            }
        }

    /**
     * 从本地数据库加载所有的对话及群组
     */
    private fun loadAllConversationsAndGroups() {
        // 从本地数据库加载所有的对话及群组
        ChatClient.getInstance().chatManager().loadAllConversations()
        ChatClient.getInstance().groupManager().loadAllGroups()
    }

    /**
     * 登录到服务器，可选择密码登录或者token登录
     * @param userName
     * @param pwd 登录凭证。[isTokenFlag] 为 `true` 时为 chat token；为 `false` 时为账号密码，
     *            此时先用密码换取 token，再用 token 登录。
     * @param isTokenFlag
     * @return
     */
    suspend fun loginToServer(
        userName: String,
        pwd: String,
        isTokenFlag: Boolean
    ): ChatUIKitUser =
        withContext(Dispatchers.IO) {
            // 在换取 token / 登录前先确定 AppKey，保证 token 请求命中正确的 REST 服务器。
            if (ChatClient.getInstance().isLoggedIn.not()) {
                if (DemoHelper.getInstance().getDataModel().isCustomSetEnable()) {
                    DemoHelper.getInstance().getDataModel().getCustomAppKey()?.let {
                        if (it.isNotEmpty()) {
                            ChatClient.getInstance().changeAppkey(it)
                        }else{
                            ChatClient.getInstance().options.enableDNSConfig(true)
                            ChatClient.getInstance().changeAppkey(BuildConfig.APPKEY)
                        }
                    }
                } else {
                    ChatClient.getInstance().changeAppkey(BuildConfig.APPKEY)
                }
            }
            // 密码登录场景：先通过 {restBaseUrl}/token 用密码换取 chat token，仅用于debug，生产环境应该从业务服务器获取token
            // 不再把原始密码传给 SDK，登录统一走 token 通道。
            val token = if (isTokenFlag) pwd else fetchTokenForUser(userName, pwd)
            suspendCoroutine { continuation ->
                ChatUIKitClient.login(ChatUIKitProfile(userName), token, onSuccess = {
                    successForCallBack(continuation)
                }, onError = { code, error ->
                    if(code == ChatError.USER_ALREADY_LOGIN){
                        if (ChatUIKitClient.getCurrentUser()?.id == userName){
                            successForCallBack(continuation)
                        }else{
                            ChatUIKitClient.logout(true)
                            continuation.resumeWithException(ChatException(code, error))
                        }
                    }else{
                        continuation.resumeWithException(ChatException(code, error))
                    }
                })
            }
        }

    /**
     * 使用用户名 + 密码换取 chat token。
     *
     * 该实现参考 emclient-linux 中 `EMConfigManager::fetchTokenForUser`：
     * 向 `{restBaseUrl}/token` 发起 POST 请求，body 为 `grant_type=password`，
     * 成功后从响应 JSON 中读取 `access_token`。
     *
     * 注意：此方式需要客户端持有原始密码并直接访问 chat REST 服务，仅适用于示例/测试场景；
     * 生产环境推荐由 app server 代理完成密码到 token 的换取（见 [loginFromServer]）。
     */
    private fun fetchTokenForUser(userName: String, password: String): String {
        if (userName.isEmpty() || password.isEmpty()) {
            throw ChatException(ChatError.INVALID_PARAM, "username or password is empty")
        }
        val baseUrl = ChatClient.getInstance().chatConfigPrivate?.getBaseUrl(true, false)
        if (baseUrl.isNullOrEmpty()) {
            throw ChatException(ChatError.SERVER_NOT_REACHABLE, "no matching rest base url")
        }
        val url = "$baseUrl/token"
        EMLog.d("fetchTokenForUser url : ", url)
        try {
            val headers: MutableMap<String, String> = HashMap()
            headers["Content-Type"] = "application/json"
            val request = JSONObject()
            request.putOpt("grant_type", "password")
            request.putOpt("username", userName)
            request.putOpt("password", password)
            val response = HttpClientManager.httpExecute(
                url,
                headers,
                request.toString(),
                HttpClientManager.Method_POST
            )
            val code = response.code
            val responseInfo = response.content
            when (code) {
                200 -> {
                    val token = JSONObject(responseInfo).optString("access_token")
                    if (token.isNullOrEmpty()) {
                        throw ChatException(ChatError.SERVER_UNKNOWN_ERROR, "access_token is empty")
                    }
                    return token
                }
                400 -> throw ChatException(ChatError.USER_AUTHENTICATION_FAILED, responseInfo)
                404 -> throw ChatException(ChatError.USER_NOT_FOUND, responseInfo)
                else -> throw ChatException(ChatError.SERVER_NOT_REACHABLE, responseInfo)
            }
        } catch (e: ChatException) {
            throw e
        } catch (e: Exception) {
            throw ChatException(ChatError.NETWORK_ERROR, e.message)
        }
    }

    /**
     * 退出登录
     * @param unbindDeviceToken
     * @return
     */
    suspend fun logout(unbindDeviceToken: Boolean): Int =
        withContext(Dispatchers.IO) {
            suspendCoroutine { continuation ->
                ChatUIKitClient.logout(unbindDeviceToken, onSuccess = {
                    DemoHelper.getInstance().getDataModel().setCurrentPhoneNumber("")
                    continuation.resume(ChatError.EM_NO_ERROR)
                }, onError = { code, error ->
                    continuation.resumeWithException(ChatException(code, error))
                })
            }
        }

    private fun successForCallBack(continuation: Continuation<ChatUIKitUser>) {
        // get current user id
        val currentUser = ChatClient.getInstance().currentUser
        val user = ChatUIKitUser(currentUser)
        continuation.resume(user)

        // ** manually load all local groups and conversation
        loadAllConversationsAndGroups()
    }

    /**
     * Login to app server and get token.
     */
    suspend fun loginFromServer(userName: String, userPassword: String): LoginResult? =
        withContext(Dispatchers.IO) {
            suspendCoroutine { continuation ->
                loginFromAppServer(userName, userPassword, object : ChatValueCallback<LoginResult> {
                    override fun onSuccess(value: LoginResult?) {
                        DemoHelper.getInstance().getDataModel().setCurrentPhoneNumber(value?.phone)
                        continuation.resume(value)
                    }

                    override fun onError(code: Int, error: String?) {
                        continuation.resumeWithException(ChatException(code, error))
                    }
                })
            }
        }

    private fun loginFromAppServer(
        userName: String,
        userPassword: String,
        callBack: ChatValueCallback<LoginResult>
    ) {
        try {
            val headers: MutableMap<String, String> = HashMap()
            headers["Content-Type"] = "application/json"
            val request = JSONObject()
            request.putOpt("phoneNumber", userName)
            request.putOpt("smsCode", userPassword)
            val url: String = LOGIN_URL
            EMLog.d("LoginToAppServer url : ", url)
            val response = HttpClientManager.httpExecute(
                url,
                headers,
                request.toString(),
                HttpClientManager.Method_POST
            )
            val code = response.code
            val responseInfo = response.content
            if (code == 200) {
                EMLog.d("LoginToAppServer success : ", responseInfo)
                val `object` = JSONObject(responseInfo)
                val result = LoginResult()
                val phoneNumber = `object`.getString("phoneNumber")
                result.phone = phoneNumber
                result.token = `object`.getString("token")
                result.username = `object`.getString("chatUserName")
                result.code = code
                callBack.onSuccess(result)
            } else {
                if (responseInfo != null && responseInfo.isNotEmpty()) {
                    var errorInfo: String? = null
                    try {
                        val responseObject = JSONObject(responseInfo)
                        errorInfo = responseObject.getString("errorInfo")
                        if (errorInfo.contains("phone number illegal")) {
                            errorInfo = DemoApplication.getInstance().getString(R.string.em_login_phone_illegal)
                        } else if (errorInfo.contains("verification code error") || errorInfo.contains(
                                "send SMS to get mobile phone verification code"
                            )
                        ) {
                            errorInfo = DemoApplication.getInstance().getString(R.string.em_login_illegal_code)
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        errorInfo = responseInfo
                    }
                    callBack.onError(code, errorInfo)
                } else {
                    callBack.onError(code, responseInfo)
                }
            }
        } catch (e: Exception) {
            callBack.onError(ChatError.NETWORK_ERROR, e.message)
        }
    }

    /**
     * 注销账户
     * @return
     */
    suspend fun cancelAccount(): Int? =
        withContext(Dispatchers.IO) {
            suspendCoroutine { continuation ->
                cancelAccountFromServer(
                    onSuccess = {
                        continuation.resume(ChatError.EM_NO_ERROR)
                    },
                    onError = {code, error ->
                        continuation.resumeWithException(ChatException(code,error))
                    })
            }
        }

    private fun cancelAccountFromServer(onSuccess: OnSuccess, onError: OnError){
        try {
            val headers: MutableMap<String, String> = java.util.HashMap()
            headers["Content-Type"] = "application/json"
            headers["Authorization"] = "Bearer ${ChatClient.getInstance().accessToken}"
            val url = "$CANCEL_ACCOUNT/${DemoHelper.getInstance().getDataModel().getPhoneNumber()}"
            EMLog.d("cancelAccountFromServer url : ", url)
            val response =
                HttpClientManager.httpExecute(url, headers, null, HttpClientManager.Method_DELETE)
            val code = response.code
            val responseInfo = response.content
            EMLog.d("cancelAccountFromServer", "code:$code response:$responseInfo")
            if (code == 200) {
                onSuccess()
            } else {
                if (responseInfo != null && responseInfo.isNotEmpty()) {
                    val errorInfo = try {
                        val responseObject = JSONObject(responseInfo)
                        responseObject.getString("errorInfo")
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        responseInfo
                    }
                    onError(code, errorInfo)
                } else {
                    onError(code, responseInfo)
                }
            }
        } catch (e: java.lang.Exception) {
            onError(ChatError.NETWORK_ERROR, e.message)
        }
    }

}