# KuiklyChat

基于 [Kuikly](https://kuikly.tencent.com) 跨端框架构建的聊天消息展示组件，支持 Android、iOS、鸿蒙、H5 多端运行。

ChatSession 是一个纯消息展示容器，架构参考 [Stream Chat Compose SDK](https://getstream.io/chat/sdk/compose/) 设计：

- **Config 三层分组**：`theme {}` 主题 + `messageListOptions {}` 行为 + `slots {}` 渲染插槽（同时保持旧 API 向后兼容）
- **消息类型独立 Slot**：`textBubble` / `imageBubble` / `customBubble` 按类型精确替换（参考 Stream 的 attachmentFactories）
- **上下文感知渲染**：`MessageContext` 提供前后消息信息，支持连续消息合并头像、缩小间距（参考 Stream 的 MessageItemState）
- **图片消息内置渲染**：IMAGE 类型自动按比例缩放 + 圆角显示
- **输入栏原子化**：不内置输入栏，业务方自行组合（与 Stream Chat 的 MessageComposer 独立思路一致）

---

## 目录

- [快速接入](#快速接入)
  - [1. 添加 Maven 依赖](#1-添加-maven-依赖)
  - [2. 最简用法](#2-最简用法)
- [配置参数一览](#配置参数一览)
  - [导航栏配置](#导航栏配置)
  - [头像与昵称](#头像与昵称)
  - [主题色与气泡颜色](#主题色与气泡颜色)
  - [背景图](#背景图)
  - [气泡布局配置](#气泡布局配置)
  - [行为配置](#行为配置)
  - [时间分组配置](#时间分组配置)
  - [事件回调](#事件回调)
  - [Slot 自定义渲染](#slot-自定义渲染)
- [使用示例](#使用示例)
- [数据模型](#数据模型)
- [组件架构](#组件架构)
- [发布到 Maven](#发布到-maven)
- [注意事项](#注意事项)

---

## 快速接入

### 1. 添加 Maven 依赖

在项目的 `settings.gradle.kts` 中添加仓库：

```kotlin
dependencyResolutionManagement {
    repositories {
        // ... 其他仓库
        maven {
            url = uri("https://mirrors.tencent.com/nexus/repository/maven-tencent/")
        }
    }
}
```

在模块的 `build.gradle.kts` 中添加依赖：

```kotlin
kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                // KuiklyChat 聊天组件
                implementation("com.tencent.kuiklybase:KuiklyChat:{version}-{kotlinVersion}")
                // 例如：
                // implementation("com.tencent.kuiklybase:KuiklyChat:1.0.0-2.0.21")
            }
        }
    }
}
```

版本号规则：`{baseVersion}-{kotlinVersion}`，例如 `1.0.0-2.0.21`。鸿蒙平台使用 `1.0.0-2.0.21-KBA-010` 格式。

### 2. 最简用法

ChatSession 是一个纯消息展示容器，输入栏由业务自行组合：

```kotlin
import com.tencent.kuiklybase.chat.*

@Page("chat")
class MyChatPage : BasePager() {

    // 第 1 步：声明响应式消息列表
    var messageList by observableList<ChatMessage>()
    private var inputText = ""
    private lateinit var inputRef: ViewRef<InputView>

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    flex(1f)
                    flexDirection(FlexDirection.COLUMN)
                }

                // 第 2 步：放置 ChatSession（纯消息展示）
                ChatSession({ ctx.messageList }) {
                    title = "聊天"
                }

                // 第 3 步：业务自行实现输入栏
                Row {
                    attr { padding(8f, 12f, 8f, 12f); alignItemsCenter() }
                    Input {
                        ref { ctx.inputRef = it }
                        attr { flex(1f); height(36f); placeholder("输入消息...") }
                        event {
                            textDidChange { ctx.inputText = it.text }
                            inputReturn {
                                if (it.text.isNotBlank()) ctx.inputText = it.text
                                ctx.doSend()
                            }
                        }
                    }
                    View {
                        attr { minWidth(60f); height(36f); borderRadius(18f); allCenter() }
                        Text { attr { text("发送") } }
                        event { click { ctx.doSend() } }
                    }
                }
            }
        }
    }

    private fun doSend() {
        val text = inputText.trim()
        if (text.isNotEmpty()) {
            messageList.add(ChatMessageHelper.createTextMessage(content = text, isSelf = true))
            inputText = ""
            inputRef.view?.setText("")
        }
    }
}
```

为什么不内置输入栏？实际项目中输入栏是最重度定制的部分——语音输入、表情面板、图片/视频选择、@提及、AI 补全等，每个业务都不一样。ChatSession 只做消息展示，让业务方自由组合输入方式。

---

## 配置参数一览

所有配置通过 `ChatSession` 的尾部 lambda（`ChatSessionConfig`）设置。支持两种风格：

**新 DSL 风格（推荐，参考 Stream Chat 的 ChatTheme 分层）：**

```kotlin
ChatSession({ ctx.messageList }) {
    title = "聊天"
    selfAvatarUrl = "https://..."

    // 主题配置（颜色、尺寸、形状）
    theme {
        primaryColor = 0xFF6C5CE7
        primaryGradientEndColor = 0xFFA29BFE
        otherBubbleColor = 0xFFF5F0FF
    }

    // 消息列表行为配置
    messageListOptions {
        autoScrollToBottom = true
        enableMessageGrouping = true     // 连续同一发送者消息合并头像
        messageGroupingInterval = 2 * 60 * 1000L
    }

    // 渲染插槽配置
    slots {
        textBubble = { container, context, config -> ... }
        imageBubble = { container, context, config -> ... }
        helperContent = { container -> ... }
    }
}
```

**旧平铺风格（向后兼容）：**

```kotlin
ChatSession({ ctx.messageList }) {
    title = "聊天"
    primaryColor = 0xFF6C5CE7     // 等价于 theme { primaryColor = ... }
    autoScrollToBottom = true      // 等价于 messageListOptions { autoScrollToBottom = ... }
    messageBubble = { ... }        // 等价于 slots { messageBubble = ... }
}
```

### 导航栏配置

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `title` | `String` | `"聊天"` | 导航栏标题 |
| `showNavigationBar` | `Boolean` | `true` | 是否显示导航栏 |
| `showBackButton` | `Boolean` | `true` | 是否显示返回按钮 |

### 头像与昵称

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `selfAvatarUrl` | `String` | `""` | 自己的头像 URL（为空则使用默认头像） |
| `showAvatar` | `Boolean` | `true` | 是否显示头像 |
| `showSenderName` | `Boolean` | `true` | 是否显示发送者昵称 |
| `avatarRadius` | `Float` | `8f` | 头像圆角半径（20f = 圆形，8f = 微信风格圆角方形，0f = 方形） |

### 主题色与气泡颜色

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `primaryColor` | `Long` | `0xFF4F8FFF` | 主色（自己气泡渐变起始色、导航栏） |
| `primaryGradientEndColor` | `Long` | `0xFF6C5CE7` | 渐变结束色 |
| `backgroundColor` | `Long` | `0xFFF0F2F5` | 页面背景色（无背景图时生效） |
| `otherBubbleColor` | `Long` | `0xFFFFFFFF` | 对方消息气泡背景色 |
| `otherTextColor` | `Long` | `0xFF333333` | 对方消息文字颜色 |
| `selfTextColor` | `Long` | `0xFFFFFFFF` | 自己消息文字颜色 |

### 背景图

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `backgroundImage` | `String` | `""` | 聊天区域背景图 URL（设置后铺满消息列表区域） |

### 气泡布局配置

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `bubbleMaxWidthRatio` | `Float` | `0.65f` | 气泡最大宽度占屏幕宽度的比例 |
| `bubblePaddingH` | `Float` | `12f` | 气泡内水平 padding（单侧） |
| `bubblePaddingV` | `Float` | `10f` | 气泡内垂直 padding（单侧） |
| `messageFontSize` | `Float` | `15f` | 消息文字大小 |
| `messageLineHeight` | `Float` | `22f` | 消息行高 |
| `avatarSize` | `Float` | `40f` | 头像尺寸（宽高相同） |
| `rowPaddingV` | `Float` | `6f` | 消息行垂直 padding |
| `rowPaddingH` | `Float` | `12f` | 消息行水平 padding |
| `avatarBubbleGap` | `Float` | `8f` | 头像与气泡的间距 |

### 行为配置

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `autoScrollToBottom` | `Boolean` | `true` | 组件内部自动处理滚动（见下方策略），业务方只需 `add` 消息即可 |

**自动滚动策略：**

| 消息来源 | 策略 |
|----------|------|
| 自己发的消息（`isSelf = true`） | **必须**滚动到底部 |
| 他人的消息（`isSelf = false`） | 当前在底部（10px 容差）才滚动，不在底部则保持当前位置 |

业务方发消息后**不需要**手动调用滚动，组件内部自动处理：

```kotlin
// 自己发消息 → 只需 add，组件自动滚到底部
messageList.add(userMessage)

// 收到对方消息 → 只需 add，组件自动判断是否滚动
messageList.add(reply)
```

#### 暴露给业务方的滚动 API

`ChatSessionConfig` 在初始化时会自动设置以下方法引用，适用于特殊场景（如引用消息定位、搜索跳转等）：

| API | 类型 | 说明 |
|-----|------|------|
| `scrollToBottomAction` | `((animate: Boolean) -> Unit)?` | 手动滚动到底部（特殊场景使用） |
| `scrollToMessageAction` | `((messageId: String, animate: Boolean) -> Unit)?` | 滚动到指定消息，通过消息 ID 定位 |

```kotlin
// 滚动到某条引用消息
chatSessionConfig?.scrollToMessageAction?.invoke(targetMessage.id, true)

// 特殊场景手动滚到底部（如点击"回到底部"按钮）
chatSessionConfig?.scrollToBottomAction?.invoke(true)
```

### 时间分组配置

消息之间超过一定时间间隔，会自动插入时间分隔标签（如 "14:30"）。

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `showTimeGroup` | `Boolean` | `true` | 是否启用时间分组显示 |
| `timeGroupInterval` | `Long` | `300000`（5 分钟） | 时间分组间隔阈值（毫秒） |
| `timeFormatter` | `TimeFormatter?` | `null` | 自定义时间格式化器（默认显示 "HH:mm"） |

```kotlin
// 自定义时间格式化器
timeFormatter = { timestamp ->
    // 自定义格式化逻辑，返回字符串
    "下午 3:00"
}
```

### 事件回调

| 参数 | 类型 | 说明 |
|------|------|------|
| `onBackClick` | `() -> Unit` | 返回按钮点击（不设置则默认 `closePage()`） |
| `onMessageClick` | `(ChatMessage) -> Unit` | 消息气泡点击 |
| `onMessageLongPress` | `(ChatMessage) -> Unit` | 消息气泡长按 |
| `onResend` | `(ChatMessage) -> Unit` | 失败消息重发回调（设置后失败消息会显示红色重试按钮） |

### Slot 自定义渲染

Slot 是高级定制能力，允许替换组件的某个部分的默认渲染。参考 Stream Chat Compose SDK 的 Slot API 设计。

#### 消息类型独立 Slot（参考 Stream Chat 的 attachmentFactories）

| Slot | 类型 | 说明 |
|------|------|------|
| `textBubble` | `MessageBubbleSlot` | 替换**文本消息**气泡渲染 |
| `imageBubble` | `MessageBubbleSlot` | 替换**图片消息**渲染 |
| `customBubble` | `MessageBubbleSlot` | 替换**自定义消息**渲染（MessageType.CUSTOM） |
| `messageBubble` | `MessageBubbleSlot` | 替换**所有非系统消息**渲染（优先级最高，覆盖上面三个） |
| `systemMessage` | `SimpleBubbleSlot` | 替换系统消息渲染 |

**渲染优先级**：`messageBubble` > `textBubble` / `imageBubble` / `customBubble` > 默认渲染

#### 列表状态 Slot（参考 Stream Chat MessageList）

| Slot | 类型 | 说明 |
|------|------|------|
| `emptyContent` | `ViewSlot` | 消息列表为空时的占位渲染 |
| `loadingContent` | `ViewSlot` | 首次加载时的加载指示器 |
| `loadingMoreContent` | `ViewSlot` | 加载历史消息时的顶部加载指示器 |
| `helperContent` | `ViewSlot` | 列表辅助内容（如"滚动到底部"悬浮按钮） |

#### 导航栏 Slot

| Slot | 类型 | 说明 |
|------|------|------|
| `navigationBar` | `NavigationBarSlot` | 替换导航栏渲染 |
| `navigationBarTrailing` | `ViewSlot` | 导航栏右侧操作区域 |

Slot 类型定义：

```kotlin
// 消息气泡 Slot（带上下文，支持连续消息分组渲染）
typealias MessageBubbleSlot = (container: ViewContainer<*, *>, context: MessageContext, config: ChatSessionConfig) -> Unit

// 简单气泡 Slot（不含上下文，用于系统消息）
typealias SimpleBubbleSlot = (container: ViewContainer<*, *>, message: ChatMessage, config: ChatSessionConfig) -> Unit

// 导航栏 Slot
typealias NavigationBarSlot = (container: ViewContainer<*, *>, config: ChatSessionConfig) -> Unit

// 通用 Slot（导航栏右侧、空消息占位等）
typealias ViewSlot = (container: ViewContainer<*, *>) -> Unit
```

#### MessageContext 消息上下文

新版 Slot 提供 `MessageContext`，包含前后消息信息和分组状态（参考 Stream Chat 的 MessageItemState）：

```kotlin
data class MessageContext(
    val message: ChatMessage,           // 当前消息
    val previousMessage: ChatMessage?,  // 上一条消息
    val nextMessage: ChatMessage?,      // 下一条消息
    val index: Int,                     // 当前索引
    val isFirstInGroup: Boolean,        // 是否为分组第一条（显示发送者名称）
    val isLastInGroup: Boolean          // 是否为分组最后一条（显示头像）
)
```

利用分组信息可以实现「连续同一发送者的消息合并头像、缩小间距」效果。

---

## 使用示例

### 示例 1：开箱即用 + 业务自定义输入栏

ChatSession 只负责消息展示，输入栏由业务自行组合：

```kotlin
View {
    attr { flex(1f); flexDirection(FlexDirection.COLUMN) }

    // 消息展示区
    ChatSession({ ctx.messageList }) {
        title = "客服助手"
    }

    // 业务自定义输入栏（可自由添加语音、表情、图片等按钮）
    Row {
        attr { padding(8f, 12f, 8f, 12f); alignItemsCenter() }
        View { /* 语音按钮 */ }
        Input {
            attr { flex(1f); placeholder("输入消息...") }
            event { inputReturn { ctx.doSend(it.text) } }
        }
        View { /* 表情按钮 */ }
        View { /* 附件按钮 */ }
        View { /* 发送按钮 */
            event { click { ctx.doSend() } }
        }
    }
}
```

### 示例 2：背景图 + 自定义气泡颜色

```kotlin
ChatSession({ ctx.messageList }) {
    title = chatTitle

    // 聊天背景图
    backgroundImage = "https://example.com/chat-bg.jpg"

    // 自定义气泡颜色
    primaryColor = 0xFF6C5CE7            // 自己气泡渐变起始色
    primaryGradientEndColor = 0xFFA29BFE  // 自己气泡渐变结束色
    otherBubbleColor = 0xFFF5F0FF         // 对方气泡（淡紫色）
    otherTextColor = 0xFF2D3436           // 对方文字颜色
    selfTextColor = 0xFFFFFFFF            // 自己文字颜色
}
```

### 示例 3：隐藏头像与昵称（简洁 1v1 聊天）

```kotlin
ChatSession({ ctx.messageList }) {
    title = "与 Alice 的对话"
    showAvatar = false       // 隐藏头像
    showSenderName = false   // 隐藏昵称
}
```

### 示例 4：按消息类型自定义渲染（Slot）

新版支持按消息类型精确替换渲染（参考 Stream Chat 的 attachmentFactories）：

```kotlin
ChatSession({ ctx.messageList }) {
    title = chatTitle

    // 方式 1：按类型独立替换（推荐）
    slots {
        // 只自定义文本气泡
        textBubble = { container, context, config ->
            val msg = context.message
            container.ChatBubble {
                attr {
                    content = msg.content
                    isSelf = msg.isSelf
                    // 利用 context 的分组信息
                    showAvatar = context.isLastInGroup
                    senderName = if (context.isFirstInGroup) msg.senderName else ""
                }
            }
        }

        // 自定义图片消息
        imageBubble = { container, context, config ->
            // 自定义图片渲染逻辑
        }
    }

    // 方式 2：统一替换所有消息（旧方式兼容）
    // messageBubble = { container, context, config -> ... }
}
```

注意：Slot 中的 `container` 参数是 `vfor` 内部容器的引用，必须通过 `container.XXX { }` 添加子组件，不要使用外层的 `this@List`。

### 示例 5：只读模式（查看聊天记录）

不需要输入栏，ChatSession 本身就是只读的消息展示：

```kotlin
ChatSession({ ctx.messageList }) {
    title = "聊天记录"
    showBackButton = true
}
```

### 示例 6：消息重发

```kotlin
ChatSession({ ctx.messageList }) {
    title = chatTitle

    // 设置 onResend 后，发送失败的消息会显示红色重试按钮
    onResend = { failedMessage ->
        // 业务处理重发逻辑
        ctx.resendMessage(failedMessage)
    }
}
```

### 示例 7：自定义空消息占位

```kotlin
ChatSession({ ctx.messageList }) {
    title = "新对话"

    // 自定义空消息提示
    emptyView = { container ->
        container.Text {
            attr {
                text("快来打个招呼吧")
                fontSize(16f)
                color(Color(0xFF999999))
            }
        }
    }
}
```

### 示例 8：滚动到指定消息

通过 `scrollToMessageAction` 可以滚动到任意消息位置，适用于引用消息定位、搜索结果跳转等场景：

```kotlin
var chatSessionConfig: ChatSessionConfig? = null

ChatSession({ ctx.messageList }) {
    title = chatTitle
    ctx.chatSessionConfig = this
}

// 滚动到指定消息（带动画）
chatSessionConfig?.scrollToMessageAction?.invoke(targetMessage.id, true)

// 发消息后手动滚到底部
chatSessionConfig?.scrollToBottomAction?.invoke(true)
```

---

## 数据模型

### ChatMessage

聊天消息的数据模型：

```kotlin
data class ChatMessage(
    val id: String,                              // 消息唯一 ID
    val content: String,                         // 消息内容（文本或图片 URL）
    val isSelf: Boolean,                         // 是否为自己发送
    val type: MessageType = MessageType.TEXT,     // 消息类型
    val status: MessageStatus = MessageStatus.SENT, // 发送状态
    val senderName: String = "",                 // 发送者名称
    val senderAvatar: String = "",               // 发送者头像 URL
    val timestamp: Long = 0L,                    // 时间戳（毫秒），用于时间分组
    val extra: Map<String, String> = emptyMap()  // 扩展数据
)
```

设置 `timestamp` 字段后会自动启用时间分组功能。`timestamp` 为 0 时不参与时间分组。

### MessageType

```kotlin
enum class MessageType {
    TEXT,    // 文本消息
    IMAGE,   // 图片消息（内置渲染：自动缩放 + 圆角）
    SYSTEM,  // 系统消息（如时间提示、通知）
    CUSTOM   // 自定义消息（需设置 customBubble Slot 渲染）
}
```

### MessageStatus

```kotlin
enum class MessageStatus {
    SENDING,  // 发送中（显示 "发送中..."）
    SENT,     // 已发送（不显示状态）
    FAILED,   // 发送失败（显示 "发送失败，点击重试" + 红色重发按钮）
    READ      // 已读（显示 "已读"）
}
```

### ChatMessageHelper 工具类

提供快捷创建消息的方法：

```kotlin
// 创建文本消息（支持时间戳）
val msg = ChatMessageHelper.createTextMessage(
    content = "Hello!",
    isSelf = true,
    senderName = "我",
    senderAvatar = "https://example.com/avatar.png",
    status = MessageStatus.SENT,
    timestamp = System.currentTimeMillis()  // 设置时间戳以启用时间分组
)

// 创建图片消息
val imgMsg = ChatMessageHelper.createImageMessage(
    imageUrl = "https://example.com/photo.jpg",
    isSelf = false,
    senderName = "小助手",
    width = 300,
    height = 200
)

// 创建系统消息
val sysMsg = ChatMessageHelper.createSystemMessage("以下是新的聊天")
```

---

## 组件架构

### 架构对标 Stream Chat Compose SDK

| Stream Chat 概念 | ChatSession 对应 |
|---|---|
| `ChatTheme`（全局主题注入） | `theme {}` 主题配置分组 |
| `MessageList` 行为参数 | `messageListOptions {}` 行为配置分组 |
| Slot APIs（itemContent / emptyContent / loadingContent / helperContent） | `slots {}` 渲染插槽分组 |
| `attachmentFactories`（按消息类型分发渲染） | `textBubble` / `imageBubble` / `customBubble` 独立 Slot |
| `MessageItemState`（含上下文信息） | `MessageContext`（前后消息、分组状态） |
| `MessageComposer`（独立输入栏组件） | 输入栏外置，业务方自行组合 |
| `MessagesScreen`（Screen 级封装） | `ChatSession`（Screen 级 DSL 函数） |

### 源码结构

```
KuiklyChat/
  src/commonMain/kotlin/com/tencent/kuiklybase/chat/
    ChatMessage.kt          -- 数据模型 + Config 三层分组(ThemeOptions/MessageListOptions/SlotOptions)
                               + MessageContext 上下文 + Slot 类型定义
    ChatSessionView.kt      -- ChatSession DSL 入口 + 消息类型分发渲染 + 默认气泡/图片渲染
    ChatBubbleView.kt       -- 文本消息气泡组件 + 系统消息组件
    ChatNavigationBarView.kt -- 导航栏组件
```

### 组件层级关系

```
页面布局（由业务方组合）
  |-- ChatSession (纯消息展示容器)
  |     |-- ChatNavigationBar (或自定义 navigationBar Slot)
  |     |-- 消息区域 (含背景图层)
  |     |     |-- Image (backgroundImage，绝对定位铺满)
  |     |     |-- List
  |     |     |     |-- vforLazy(messageList)
  |     |     |           |-- View (itemRoot)
  |     |     |                 |-- 构建 MessageContext（前后消息、分组状态）
  |     |     |                 |-- [SYSTEM] → systemMessage Slot / 默认 ChatSystemMessage
  |     |     |                 |-- [TEXT]   → messageBubble > textBubble > 默认 ChatBubble
  |     |     |                 |-- [IMAGE]  → messageBubble > imageBubble > 默认图片渲染
  |     |     |                 |-- [CUSTOM] → messageBubble > customBubble > 占位提示
  |     |     |-- helperContent Slot (悬浮在列表上层)
  |
  |-- 业务自定义输入栏 (由业务方自行实现)
```

### Config 三层分组

```
ChatSessionConfig
  ├── theme: ChatThemeOptions          -- 颜色、尺寸、形状（对应 Stream ChatTheme）
  ├── messageListOptions: MessageListOptions  -- 滚动、加载、分组行为
  ├── slots: ChatSlotOptions           -- 所有可替换渲染插槽
  ├── 导航栏配置 (title, showNavigationBar, showBackButton)
  ├── 头像配置 (selfAvatarUrl)
  ├── 事件回调 (onBackClick, onMessageClick, onMessageLongPress, onResend, onAvatarClick)
  └── 向后兼容属性 (primaryColor → theme.primaryColor 等代理)
```

---

## 发布到 Maven

项目内置了发布脚本 `publish-maven.sh`，支持一键发布到 Maven 仓库：

```bash
# 发布到远程 Maven 仓库
./publish-maven.sh -v 1.0.0

# 发布 SNAPSHOT 版本
./publish-maven.sh -v 1.0.0 -s true

# 发布到本地 Maven 仓库（调试用）
./publish-maven.sh -v 1.0.0 -l true

# 指定 Kotlin 版本
./publish-maven.sh -v 1.0.0 -k 2.0.21

# 查看完整参数
./publish-maven.sh -h
```

发布配置（在 `gradle.properties` 中）：

```properties
# 版本号
mavenVersion=1.0.0
# Group ID
GROUP_ID=com.tencent.kuiklybase
# Kotlin 版本列表
KOTLIN_VERSION_LIST=2.0.21
# 鸿蒙 Kotlin 版本列表
OHOS_KOTLIN_VERSION_LIST=2.0.21-KBA-010
# Maven 仓库地址
MAVEN_REPO_URL=https://mirrors.tencent.com/repository/maven/kuikly-open/
```

发布后，其他项目通过以下 Maven 坐标引用：

```
com.tencent.kuiklybase:KuiklyChat:{version}-{kotlinVersion}
```

---

## 注意事项

### 1. 每个页面只放一个 ChatSession

`ChatSession` 内部包含完整的消息列表和状态管理。如果在同一个 `body()` 中放置多个 `ChatSession` 并共享同一个 `messageList`，会导致 `vfor` 响应式更新冲突，引发闪退。

```kotlin
// 错误：同一页面放两个 ChatSession 共享列表
ChatSession({ ctx.messageList }) { ... }
ChatSession({ ctx.messageList }) { ... }  // 会闪退

// 正确：每个页面只放一个 ChatSession
ChatSession({ ctx.messageList }) { ... }
```

### 2. Slot 中使用 container 参数添加子组件

在 `messageBubble` 等 Slot 回调中，必须通过 `container` 参数来添加子组件，不要使用外层作用域的引用：

```kotlin
// 正确
messageBubble = { container, message, config ->
    container.ChatBubble { ... }
}

// 错误：使用外层引用，会导致 vfor 子节点检测失败
messageBubble = { container, message, config ->
    this@List.ChatBubble { ... }  // 闪退
}
```

### 3. 消息列表必须是 ObservableList

`ChatSession` 的第一个参数是 `() -> ObservableList<ChatMessage>` 类型。必须使用 Kuikly 的 `observableList` 声明，以便触发响应式更新：

```kotlin
// 正确
var messageList by observableList<ChatMessage>()

// 错误：普通列表不会触发 UI 更新
val messageList = mutableListOf<ChatMessage>()
```

### 4. 颜色值格式

所有颜色参数使用 `Long` 类型的 ARGB 格式，需要包含 Alpha 通道：

```kotlin
primaryColor = 0xFF4F8FFF   // 正确：包含 FF（不透明）
primaryColor = 0x4F8FFF     // 错误：缺少 Alpha 通道
```

### 5. 时间分组需要设置 timestamp

只有 `timestamp > 0` 的消息才参与时间分组计算。如果消息没有设置 `timestamp`（默认 0），则不会显示时间标签：

```kotlin
// 会参与时间分组
ChatMessageHelper.createTextMessage(
    content = "Hello",
    isSelf = true,
    timestamp = System.currentTimeMillis()
)

// 不会显示时间标签（timestamp 为 0）
ChatMessageHelper.createTextMessage(
    content = "Hello",
    isSelf = true
)
```

### 6. ChatSession 与输入栏的布局组合

ChatSession 会占据 `flex(1f)` 的空间，输入栏放在它下方。需要用一个纵向的 `View` 容器包裹：

```kotlin
View {
    attr {
        flex(1f)
        flexDirection(FlexDirection.COLUMN)
    }
    // ChatSession 占据剩余空间
    ChatSession({ ctx.messageList }) { ... }
    // 输入栏固定在底部
    MyInputBar { ... }
}
```

### 7. 自动滚动行为

组件内部已自动处理所有滚动逻辑，业务方**不需要**手动调用 `scrollToBottomAction`：

- **自己发的消息**（`isSelf = true`）→ 必须滚动到底部
- **他人的消息**（`isSelf = false`）→ 当前在底部才滚动，不在底部则保持位置

```kotlin
// 自己发消息 → 只需 add，组件自动滚到底部
messageList.add(userMessage)

// 收到对方消息 → 只需 add，组件自动判断
messageList.add(reply)
// 不需要写任何滚动逻辑！
```

组件内部通过 `scrollEnd` 事件维护 `atBottom` 状态（10px 容差），新消息到达时根据 `isSelf` 自动决定是否滚动。

---

## License

MIT License
