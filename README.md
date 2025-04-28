# 产品介绍

环信IM产品展示了怎么使用环信SDK创建一个完整的聊天APP。展示的功能包括：用户登录注册，添加好友，单聊，群聊，发送文字，表情，语音，图片，文件，地理位置等消息，以及实时音视频通话等。

其中音视频通话使用声网SDK实现。

# 产品体验

![](./image/demo.png)

## 开发环境要求

- Android Studio Flamingo | 2022.2.1 及以上
- Gradle 8.0 及以上
- targetVersion 33 及以上
- Android SDK API 21 及以上
- JDK 17 及以上

# 跑通Demo

1. [注册环信应用](https://doc.easemob.com/product/enable_and_configure_IM.html)

2. 将Appkey填入工程根目录下的`local.properties`文件中 格式如下：`APPKEY = 你申请的AppKey`


# ChatUIKit在Demo中的使用

## 1. 初始化

[详情参见](./app/src/main/kotlin/com/hyphenate/chatdemo/DemoApplication.kt) 中 `DemoHelper.getInstance().initSDK()`方法。

```Kotlin

DemoHelper.getInstance().initSDK()

```

## 2. 登录

[详情参见](./app/src/main/kotlin/com/hyphenate/chatdemo/viewmodel/EMClientRepository.kt) 中 `ChatUIKitClient.login`方法

```Kotlin

//使用id和密码登录
ChatUIKitClient.login(userName, pwd, 
    onSuccess = {}, 
    onError = { code, error -> 
        
    }
)

//使用ChatUIKitProfile用户协议对象 和 token 登录，注意token需要通过服务端生成
ChatUIKitClient.login(ChatUIKitProfile(userName), token, 
    onSuccess = {}, 
    onError = { code, error -> 
        
    }
)

```

## 3. Provider使用及其最佳示例用法

如果您的App中已经有完备的用户体系以及可供展示的用户信息（例如头像昵称等。）可以实现ChatUIKitUserProfileProvider协议来提供给UIKit要展示的数据。

3.1 [Provider初始化详情参见](./app/src/main/kotlin/com/hyphenate/chatdemo/uikit/UIKitManager.kt) `UIKitManager#addProviders`方法中的
设置 ChatUIKitClient.setUserProfileProvide 和 ChatUIKitClient.setGroupProfileProvider

3.2 实现Provider协议提供的方法参见下述示例代码

```Kotlin
ChatUIKitClient.setUserProfileProvider(object : ChatUIKitUserProfileProvider {
    // 同步获取用户信息
    override fun getUser(userId: String?): ChatUIKitProfile? {
        return DemoHelper.getInstance().getDataModel().getAllContacts()[userId]?.toProfile()
    }

    override fun fetchUsers(
        userIds: List<String>,
        onValueSuccess: OnValueSuccess<List<ChatUIKitProfile>>
    ) {
        // 用户可以根据userIds从自己服务器获取多个id的Profile信息 通过onValueSuccess()进行数据返回
        // 同时可以将获取到的信息更新本地
        // 更新db DemoHelper.getInstance().getDataModel().insertUsers()
        // 更新缓存 ChatUIKitClient.updateUsersInfo() 获取Profile时 UIKit会先从缓存中查询
    }
})
.setGroupProfileProvider(object : ChatUIKitGroupProfileProvider {
    // 同步获取群组信息
    override fun getGroup(id: String?): ChatUIKitGroupProfile? {
        ChatClient.getInstance().groupManager().getGroup(id)?.let {
            return ChatUIKitGroupProfile(it.groupId, it.groupName, it.extension)
        }
        return null
    }

    override fun fetchGroups(
        groupIds: List<String>,
        onValueSuccess: OnValueSuccess<List<ChatUIKitGroupProfile>>
    ) {
        // 用户可以根据groupIds从自己服务器获取多个id的ChatUIKitGroupProfile信息 通过onValueSuccess()进行数据返回
        // 同时可以将获取到的信息更新本地
        // 更新缓存 ChatUIKitClient.updateGroupInfo() 获取Profile时 UIKit会先从缓存中查询
    }
})
```

## 4.继承ChatUIKit中的类进行二次开发

4.1  举例：继承ChatUIKit中的UIKitChatActivity

```Kotlin

class ChatActivity: UIKitChatActivity() {
    override fun setChildSettings(builder: UIKitChatFragment.Builder) {
        super.setChildSettings(builder)
        // builder 中提供了一系列的配置 如不满足还可以 ChatFragment 继承 UIKitChatFragment 进行扩展
        builder.setCustomFragment(ChatFragment()).setCustomAdapter(CustomMessagesAdapter())
    }
}

```

4.2 页面跳转重定向 setCustomActivityRoute的使用方法参见下述示例代码 

[详情参见](./app/src/main/kotlin/com/hyphenate/chatdemo/uikit/UIKitManager.kt) `UIKitManager.addProviders`方法中的 `setCustomActivityRoute` 设置

```Kotlin
// 用于修改UIKit内部跳转进行重定向，跳转为自己的实现类
ChatUIKitClient.setCustomActivityRoute(object : ChatUIKitCustomActivityRoute {
    override fun getActivityRoute(intent: Intent): Intent? {
        intent.component?.className?.let {
            when(it) {
                UIKitChatActivity::class.java.name -> {
                    intent.setClass(context, ChatActivity::class.java)
                }
                else -> {
                    return intent
                }
            }
        }
        return intent
    }
})

```

# Demo设计
浏览器中打开如下链接
https://www.figma.com/community/file/1327193019424263350/chat-uikit-for-mobile


# 已知问题
1. 为方便开发者快速体验IM功能，本工程Demo默认使用开发者注册的用户名和密码直接登录。但此模式下手机验证码、用户图像和Callkit相关功能不可用。用户可直接使用[官网下载](https://www.easemob.com/download/demo)的apk,或者通过以下配置跑通工程Demo体验完整功能:

- 将[服务端源码](https://github.com/easemob/easemob-im-app-server/tree/dev-demo)部署后填入`local.properties`文件中 格式如下 `APP_SERVER_DOMAIN = xxx服务器域名或ip地址xx`，手机号验证码暂时可以跳过，可以使用手机号后六位当验证码，服务端中的Appkey 要跟客户端的Appkey保持一致。

    :::tip
    Appserver主要提供了手机号验证码登录接口以及上传用户头像的接口，此接口主要的职能是根据用户的信息注册并生成ChatUIKit登录所需的token或者使用已注册的用户信息生成ChatUIKit登录所需的token。
    :::

- 在`local.properties`文件中，填入`LOGIN_WITH_APPSERVER=true`



2. UserProvider以及GroupProvider需要用户自己实现，用于获取用户的展示信息以及群组的简要展示信息，如果不实现默认用id以及默认头像。
3. 换设备或者多设备登录，漫游的会话列表，环信SDK中没有本地存储的群头像名称等显示信息，需要用户使用Provider提供给UIKit才能正常显示。
4. 由于Provider的机制是停止滚动或者第一页不满10条数据时触发，所以更新会话列表以及联系人列表UI显示的昵称头像需要滑动后Provider提供给UIKit数据后，UIKit会刷新UI。

# Q&A

如有问题请联系环信技术支持或者发邮件到issue@easemob.com
