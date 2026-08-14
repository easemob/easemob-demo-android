package com.hyphenate.chatdemo.common.push.fcm

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.hyphenate.chat.EMClient
import com.hyphenate.chatdemo.DemoHelper
import com.hyphenate.chatdemo.common.AutoLoginManager

/**
 * If you want to show notifications on Android 13+, you need to do the following:
 * Android 13 introduces a new runtime permission for showing notifications.
 * This affects all apps running on Android 13 or higher that use FCM notifications.
 * By default, the FCM SDK (version 23.0.6 or higher) includes the POST_NOTIFICATIONS permission defined in the manifest.
 * However, your app will also need to request the runtime version of this permission via the constant, android.permission.POST_NOTIFICATIONS.
 * Your app will not be allowed to show notifications until the user has granted this permission.
 * To request the new runtime permission:
 * <pre>
 * // Declare the launcher at the top of your Activity/Fragment:
 * private val requestPermissionLauncher = registerForActivityResult(
 *     ActivityResultContracts.RequestPermission(),
 * ) { isGranted: Boolean ->
 *     if (isGranted) {
 *         // FCM SDK (and your app) can post notifications.
 *     } else {
 *         // TODO: Inform user that that your app will not show notifications.
 *     }
 * }
 *
 * private fun askNotificationPermission() {
 *     // This is only necessary for API level >= 33 (TIRAMISU)
 *     if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
 *         if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
 *             PackageManager.PERMISSION_GRANTED
 *         ) {
 *             // FCM SDK (and your app) can post notifications.
 *         } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
 *             // TODO: display an educational UI explaining to the user the features that will be enabled
 *             //       by them granting the POST_NOTIFICATION permission. This UI should provide the user
 *             //       "OK" and "No thanks" buttons. If the user selects "OK," directly request the permission.
 *             //       If the user selects "No thanks," allow the user to continue without notifications.
 *         } else {
 *             // Directly ask for the permission
 *             requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
 *         }
 *     }
 * }
 * </pre>
 */
class EMFCMMSGService : FirebaseMessagingService() {

    override fun onCreate() {
        super.onCreate()
        // 环信离线推送为 notification 类型消息：应用不在前台时 FCM SDK 会直接展示系统通知，
        // 不会回调 onMessageReceived。因此把自动登录挂在 Service 创建时机——只要 FCM
        // 有消息投递（含通知型）就触发登录，以便 SDK 拉取离线消息（含呼叫信令），拉起 telecom。
        AutoLoginManager.autoLoginAsync()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        if (remoteMessage.data.isNotEmpty()) {
            val message = remoteMessage.data["alert"]
            Log.d(TAG, "onMessageReceived: $message")
            // SDK 5.0 移除了自动登录：收到推送后先使用本地凭证自动登录，
            // 触发 SDK 拉取离线消息（含呼叫信令），进而拉起 telecom 来电流程。
            AutoLoginManager.autoLoginAsync()
            DemoHelper.getInstance().getNotifier()?.notify(message)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "onNewToken: $token")
        EMClient.getInstance().sendFCMTokenToServer(token)
    }

    companion object {
        private const val TAG = "EMFCMMSGService"
    }
}