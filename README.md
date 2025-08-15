# 环信即时通讯 IM Demo

环信即时通讯 IM Demo 提供用户登录、单聊、群组、子区、消息(文字、表情、语音、视频、图片、文件等)发送及管理、会话管理、好友管理、用户属性、用户在线状态（Presence）以及实时音视频通话等功能。

## Demo 体验

你可以进入 [环信官网](https://www.easemob.com/download/demo) 体验即时通讯 IM Demo。
注意：Demo里登录时获取验证码功能在模拟器上不支持，请使用真机。

## 快速跑通 Demo 源码

### 开发环境要求

- 推荐 Android Studio Meerkat | 2024.3.1 Patch 2及以上
- 推荐 Gradle 8.0 及以上
- targetVersion 33 及以上
- Android SDK API 21 及以上
- JDK 17 及以上

### 跑通步骤

1. [创建应用](https://doc.easemob.com/product/enable_and_configure_IM.html#%E5%88%9B%E5%BB%BA%E5%BA%94%E7%94%A8)。
2. [获取应用的 App Key](https://doc.easemob.com/product/enable_and_configure_IM.html#%E8%8E%B7%E5%8F%96%E7%8E%AF%E4%BF%A1%E5%8D%B3%E6%97%B6%E9%80%9A%E8%AE%AF-im-%E7%9A%84%E4%BF%A1%E6%81%AF)。
3. [创建用户](https://doc.easemob.com/product/enable_and_configure_IM.html#%E5%88%9B%E5%BB%BA-im-%E7%94%A8%E6%88%B7)。
4. [下载即时通讯 IM Demo 项目源码](https://github.com/easemob/easemob-demo-android)。
5. 下载完毕，打开 Android Studio，点击 **File > Open**，打开已下载到本地的 Demo (`easemob-demo-android`) 工程根目录即可。
6. 将你的应用的 App Key 填入 Demo 工程根目录下的 `local.properties` 文件，格式为 `APPKEY = 你申请的appkey`。
7. 编译运行项目。
8. 使用注册的用户 ID 和密码登录。

### App Server

为方便开发者快速体验即时通讯 IM 功能，跑通本工程 Demo 源码默认使用开发者注册的用户 ID 和密码直接登录，不需要依赖部署服务端 App Server。但是在此模式下，手机验证码、用户头像和 EaseCallKit 实时音视频等相关功能不可用，你可以通过部署 App Server 完整体验这些功能。

App Server 为 Demo 提供以下功能：

- 通过手机号获取验证码。
- 通过手机号和验证码返回环信用户 ID 和环信用户 Token。
- 上传头像并返回地址。
- 根据用户的信息生成 [EaseCallKit](https://doc.easemob.com/document/android/easecallkit.html) 登录所需的 Token。
- 获取音视频通话时环信用户 ID 和 Agora UID 的映射关系。

你通过以下步骤部署 App Server：

1. 部署 App Server。详见 [服务端源码](https://github.com/easemob/easemob-im-app-server/tree/dev-demo)。
2. 在 Demo 工程根目录下 `local.properties` 文件中，开发者在这里应该根据自己部署的 App Server 替换填写以下配置属性：

```gradle
# App Server 服务器域名或 IP 地址
APP_SERVER_DOMAIN=xxx.xxx.com

# App Server 用户管理 URL 路径
APP_BASE_USER=/inside/app/user

# App Server 群组管理 URL 路径
APP_BASE_GROUP=/inside/app/group

# App Server 登录管理 URL 路径
APP_SERVER_LOGIN=/login/V2

# App Server 上传用户图像 URL 路径
APP_UPLOAD_AVATAR=/avatar/upload

# App Server 群图像 URL 路径
APP_GROUP_AVATAR=/avatarurl

# 从服务端拉取 Callkit 登录使用的 RTC Token URL 路径
APP_RTC_TOKEN_URL=/inside/token/rtc/channel

# RTC APP ID（https://doc.easemob.com/document/android/easecallkit.html）
RTC_APPID=xxxxxxxxxxxxxxxxxxxxxxx

# 获取 RTC UID 和环信用户名映射关系的 URL 路径
APP_RTC_CHANNEL_MAPPER_URL=/inside/agora/channel/mapper

# 获取验证码时与 App Server 加密参数所用的 AES 密钥
SECRET_KEY=xxxxxxxxxxxxxxxxxxxxxxx
```
3. 在 Demo 工程根目录下 `local.properties` 文件中，填入 `LOGIN_WITH_APPSERVER = true`，即通知 Demo 工程需要启用 App Server，体验完整功能。
4. (可选) 离线推送相关配置。

从各厂商申请对应推送相关 Appkey/AppSecret/AppID 后，填入 Demo 工程根目录下 `local.properties` 文件中，可使用各厂商推送功能。

```gradle
MEIZU_PUSH_APPKEY=xxxxxxxxxxxxxxxxxxxxxxx
MEIZU_PUSH_APPID=xxxxxxxxxxxxxxxxxxxxxxx
OPPO_PUSH_APPKEY=xxxxxxxxxxxxxxxxxxxxxxx
OPPO_PUSH_APPSECRET=xxxxxxxxxxxxxxxxxxxxxxx
VIVO_PUSH_APPID=xxxxxxxxxxxxxxxxxxxxxxx
VIVO_PUSH_APPKEY=xxxxxxxxxxxxxxxxxxxxxxx
MI_PUSH_APPKEY=xxxxxxxxxxxxxxxxxxxxxxx
MI_PUSH_APPID=xxxxxxxxxxxxxxxxxxxxxxx
FCM_SENDERID=xxxxxxxxxxxxxxxxxxxxxxx
HONOR_PUSH_APPID=xxxxxxxxxxxxxxxxxxxxxxx
```

**服务端中的 App Key 要跟客户端的 App Key 保持一致。**

## Demo 项目结构

### Demo 架构
```
└── com
    └── hyphenate
        └── chatdemo
            ├── DemoApplication.kt  //程序入口
            ├── DemoHelper.kt   //app 帮助类
            ├── MainActivity.kt //主页面
            ├── base    //包含一些基类
            │   ├── ActivityState.kt
            │   ├── BaseDialogFragment.kt   //一些弹窗基类
            │   ├── BaseInitActivity.kt //activity 基类
            │   ├── ErrorCode.kt    //一些常用的错误码
            │   └── UserActivityLifecycleCallbacks.kt
            ├── bean    //一些序列化 bean 类
            ├── callkit
            │   ├── CallKitActivityLifecycleCallback.kt //callkit 中 activity 的生命周期监听回调类
            │   ├── CallKitManager.kt   //callkit 管理类
            │   ├── CallUserInfo.kt
            │   ├── ConferenceInviteActivity.kt
            │   ├── ConferenceInviteAdapter.kt
            │   ├── ConferenceInviteFragment.kt
            │   ├── ConferenceMemberSelectViewHolder.kt
            │   ├── MultipleVideoActivity.kt    //多人音视频页面
            │   ├── VideoCallActivity.kt    //单人音视频页面
            │   ├── extensions  //callkit 一些扩展函数类
            │   ├── viewholder  //单聊、群聊 call 消息提醒类型适配器，包含一些事件处理
            │   └── views   //单聊、群聊 call 消息提醒类型布局
            ├── common  //app 的一些的公共类
            │  
            ├── controller  
            │   └── PresenceController.kt   //presence 相关的管理类
            ├── interfaces  //包含一些接口标准类
            ├── repository  // app 的数据仓库
            ├── ui
            │   ├── chat
            │   │   ├── ChatActivity.kt //单群聊聊天页面 activity
            │   │   ├── ChatFragment.kt //单群聊聊天页面 fragment
            │   │   └── CustomMessagesAdapter.kt    //自定义消息适配器
            │   ├── contact
            │   │   ├── ChatContactCheckActivity.kt     //检查是否是联系人页面
            │   │   ├── ChatContactDetailActivity.kt    //联系人详情页面
            │   │   ├── ChatContactListFragment.kt      //联系人列表页面
            │   │   ├── ChatContactRemarkActivity.kt    //联系人(好友)备注页面
            │   │   └── ChatNewRequestActivity.kt       //联系人页面新请求 item
            │   ├── conversation
            │   │   └── ConversationListFragment.kt //会话列表页面
            │   ├── group
            │   │   ├── ChatCreateGroupActivity.kt  //创建群组页面
            │   │   └── ChatGroupDetailActivity.kt  //群组详情页面
            │   ├── login
            │   │   ├── LoginActivity.kt    //登录页面 activity
            │   │   ├── LoginFragment.kt    //登录页面 fragment
            │   │   ├── ServerSetFragment.kt
            │   │   └── SplashActivity.kt   //启动页
            │   └── me //我的界面里相关按钮对应的页面
            │       ├── AboutActivity.kt    
            │       ├── AboutMeFragment.kt   //关于我页面
            │       ├── CurrencyActivity.kt     //通用设置页面，设置暗黑模式、语言、样式等
            │       ├── EditUserNicknameActivity.kt // 修改用户昵称页面
            │       ├── FeaturesActivity.kt
            │       ├── LanguageSettingActivity.kt
            │       ├── NotifyActivity.kt
            │       ├── StyleSettingActivity.kt
            │       ├── UserInformationActivity.kt
            │       ├── WebViewActivity.kt
            │       └── controller
            ├── uikit
            │   └── UIKitManager.kt //UIKit 管理类
            ├── utils   //工具类
            └── viewmodel //包含一些 ViewModel 类
```

### 核心类

| 模块               | 描述   | 
| :------------------- | :----- |
| DemoHelper               | 环信（Demo）全局帮助类，主要功能为初始化 IM SDK，初始化 [ChatUIKit](https://doc.easemob.com/uikit/chatuikit/android/chatuikit_overview.html) 、[EaseCallKit](https://doc.easemob.com/document/android/easecallkit.html) 相关及注册对话类型等。  | 
| ConversationListFragment   | 继承自 [ChatUIKit](https://doc.easemob.com/uikit/chatuikit/android/chatuikit_overview.html) 中的 `ChatUIKitConversationListFragment`，展示当前用户的所有会话，包含单聊和群组聊天（不包括聊天室），并且提供会话搜索、删除、置顶和免打扰功能  | 
| ChatActivity 及 ChatFragment  | `ChatActivity` 继承自 [ChatUIKit](https://doc.easemob.com/uikit/chatuikit/android/chatuikit_overview.html) 中的 `UIKitChatActivity`，主要进行了权限的请求，比如相机权限，语音权限等。`ChatFragment` 继承自 [ChatUIKit](https://doc.easemob.com/uikit/chatuikit/android/chatuikit_overview.html) 中的 `UIKitChatFragment`，该页面提供如下功能：1、发送和接收消息, 包括文本、表情、图片、语音、视频、文件和名片消息。2、对消息进行复制、引用、撤回、删除、编辑、重新发送和审核。3、清除本地消息。  | 
| ChatContactListFragment                | 继承自 [ChatUIKit](https://doc.easemob.com/uikit/chatuikit/android/chatuikit_overview.html) 中的 `ChatUIKitContactsListFragment`，用于展示通讯录列表，包括联系人搜索，添加联系人，好友申请列表入口，群组列表入口，联系人列表。 |
| ChatGroupDetailActivity  | 继承自 [ChatUIKit](https://doc.easemob.com/uikit/chatuikit/android/chatuikit_overview.html) 中的 `ChatUIKitGroupDetailActivity`，实现了如下功能：群成员管理，群属性管理，上传共享文件，设置消息免打扰,解散或者退出群组等。   | 

### 核心模块

| 模块               | 描述   | 
| :------------------- | :----- |
| 聊天模块    | 展示如何依赖 [ChatUIKit](https://doc.easemob.com/uikit/chatuikit/android/chatuikit_overview.html) 实现聊天页面，如何发送消息、消息管理、扩展消息类型及如何增加扩展菜单等的逻辑。    | 
| 会话列表模块 | 展示如何依赖 [ChatUIKit](https://doc.easemob.com/uikit/chatuikit/android/chatuikit_overview.html) 实现会话列表的逻辑及实现系统消息的具体逻辑。   | 
| 联系人模块  | 展示如何依赖 [ChatUIKit](https://doc.easemob.com/uikit/chatuikit/android/chatuikit_overview.html) 实现联系人列表的逻辑。   | 
| 我的模块  | 账户管理、用户状态管理及 App 的一些功能样式设置    | 
| 开发者模块  | 展示 IM SDK 提供的一些常规的开发者可以设置的功能。   | 

### 单群聊 ChatUIKit

Demo 里会话列表、聊天界面、联系人列表及后续界面均继承或直接使用环信 ChatUIKit 里提供的模块。

关于 ChatUIKit 详情，请参见 [环信官网 ChatUIKit 文档](https://doc.easemob.com/uikit/chatuikit/android/chatuikit_overview.html)。

### 音视频通话 EaseCallKit 库

Demo 里音视频页面继承自环信 EaseCallKit 提供的单人/多人音视频聊天模块。

关于音视频通话 EaseCallKit 库，请参见 [环信官网 EaseCallKit 文档](https://doc.easemob.com/document/android/easecallkit.html)。

## Demo 设计

关于 Demo 的设计，详见 [设计文档](https://www.figma.com/community/file/1327193019424263350/chat-uikit-for-mobile) 。

## 已知问题

1. UserProvider 以及 GroupProvider 需要用户自己实现，用于获取用户的展示信息以及群组的简要展示信息，如果不实现默认用 ID 以及默认头像。
2. 换设备或者多设备登录，漫游的会话列表，环信 SDK 中没有本地存储的群头像名称等显示信息，需要用户使用 Provider 提供给 UIKit 才能正常显示。
3. 由于 Provider 的机制是停止滚动或者第一页不满 10 条数据时触发，所以更新会话列表以及联系人列表 UI 显示的昵称头像需要滑动后 Provider 提供给 UIKit 数据后，UIKit会刷新 UI。

## Q&A

如有问题请联系环信技术支持或者发邮件到 [issue@easemob.com](mailto:issue@easemob.com)。