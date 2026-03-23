package com.tencent.kuiklybase.chat

import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.reactive.collection.ObservableList

/**
 * 聊天消息类型
 */
enum class MessageType {
    TEXT,       // 文本消息
    IMAGE,      // 图片消息
    SYSTEM      // 系统消息（如时间提示、系统通知等）
}

/**
 * 消息发送状态
 */
enum class MessageStatus {
    SENDING,    // 发送中
    SENT,       // 已发送
    FAILED,     // 发送失败
    READ        // 已读
}

/**
 * 聊天消息数据模型
 *
 * @param id 消息唯一ID
 * @param content 消息内容（文本内容或图片URL）
 * @param isSelf 是否为自己发送的消息
 * @param type 消息类型
 * @param status 消息发送状态
 * @param senderName 发送者名称
 * @param senderAvatar 发送者头像URL
 * @param timestamp 消息时间戳（毫秒）
 * @param extra 扩展数据（用于自定义消息携带额外信息，如图片尺寸、链接等）
 */
data class ChatMessage(
    val id: String,
    val content: String,
    val isSelf: Boolean,
    val type: MessageType = MessageType.TEXT,
    val status: MessageStatus = MessageStatus.SENT,
    val senderName: String = "",
    val senderAvatar: String = "",
    val timestamp: Long = 0L,
    val extra: Map<String, String> = emptyMap()
)

// ============================
// Slot 类型定义
// ============================

/**
 * 消息气泡渲染 Slot
 *
 * 用于自定义单条消息的渲染方式。
 * - container: 父容器，在其中添加自定义组件
 * - message: 当前消息数据
 * - config: 当前聊天会话配置（可获取主题色等）
 */
typealias MessageBubbleSlot = (container: ViewContainer<*, *>, message: ChatMessage, config: ChatSessionConfig) -> Unit

/**
 * 输入栏渲染 Slot
 *
 * 用于完全替换默认输入栏。
 * - container: 父容器
 * - onSend: 发送消息回调，调用此函数将触发消息发送流程
 */
typealias InputBarSlot = (container: ViewContainer<*, *>, onSend: (String) -> Unit) -> Unit

/**
 * 导航栏渲染 Slot
 *
 * 用于完全替换默认导航栏。
 * - container: 父容器
 * - config: 当前聊天会话配置
 */
typealias NavigationBarSlot = (container: ViewContainer<*, *>, config: ChatSessionConfig) -> Unit

/**
 * 通用渲染 Slot（用于导航栏右侧按钮、空消息占位等轻量定制场景）
 * - container: 父容器
 */
typealias ViewSlot = (container: ViewContainer<*, *>) -> Unit

// ============================
// 聊天会话配置
// ============================

/**
 * ChatSession 的配置参数
 *
 * 基础配置直接通过属性设置；高级定制通过 Slot 函数替换默认渲染。
 *
 * ## 快速上手
 * ```kotlin
 * ChatSession({ ctx.messageList }) {
 *     title = "聊天"
 *     onSendMessage = { text -> ... }
 * }
 * ```
 *
 * ## 设置背景图
 * ```kotlin
 * ChatSession({ ctx.messageList }) {
 *     backgroundImage = "https://example.com/bg.jpg"
 *     onSendMessage = { text -> ... }
 * }
 * ```
 */
class ChatSessionConfig {
    // ---------- 导航栏配置 ----------
    var title: String = "聊天"
    var showNavigationBar: Boolean = true
    var showBackButton: Boolean = true

    // ---------- 输入栏配置 ----------
    var inputPlaceholder: String = "输入消息..."
    var showSendButton: Boolean = true
    /** 发送按钮文案（默认 "发送"） */
    var sendButtonText: String = "发送"
    /** 是否显示输入栏（设为 false 可用于只读聊天记录场景） */
    var showInputBar: Boolean = true

    // ---------- 头像配置 ----------
    var selfAvatarUrl: String = ""
    /** 是否显示头像（设为 false 适用于简洁的 1v1 聊天） */
    var showAvatar: Boolean = true
    /** 是否显示发送者昵称 */
    var showSenderName: Boolean = true

    // ---------- 主题色配置 ----------
    /** 主色（用于发送按钮、自己的气泡、导航栏等） */
    var primaryColor: Long = 0xFF4F8FFF
    /** 渐变结束色 */
    var primaryGradientEndColor: Long = 0xFF6C5CE7
    /** 页面背景色（当没有设置背景图时生效） */
    var backgroundColor: Long = 0xFFF0F2F5
    /** 对方消息气泡背景色（默认白色） */
    var otherBubbleColor: Long = 0xFFFFFFFF
    /** 对方消息文字颜色 */
    var otherTextColor: Long = 0xFF333333
    /** 自己消息文字颜色 */
    var selfTextColor: Long = 0xFFFFFFFF

    // ---------- 背景图配置 ----------
    /** 聊天区域背景图 URL（设置后 backgroundColor 对消息列表区域不生效） */
    var backgroundImage: String = ""

    // ---------- 行为配置 ----------
    /** 新消息时是否自动滚动到底部 */
    var autoScrollToBottom: Boolean = true

    // ---------- 事件回调 ----------
    var onSendMessage: ((String) -> Unit)? = null
    var onBackClick: (() -> Unit)? = null
    var onMessageClick: ((ChatMessage) -> Unit)? = null
    var onMessageLongPress: ((ChatMessage) -> Unit)? = null

    // ---------- Slot: 自定义渲染 ----------

    /**
     * 自定义消息气泡渲染。设置此 Slot 后，所有非系统消息都将使用自定义渲染。
     *
     * 示例:
     * ```kotlin
     * messageBubble = { container, message, config ->
     *     container.MyCustomBubble {
     *         attr { content = message.content; isSelf = message.isSelf }
     *     }
     * }
     * ```
     */
    var messageBubble: MessageBubbleSlot? = null

    /**
     * 自定义系统消息渲染。
     */
    var systemMessage: MessageBubbleSlot? = null

    /**
     * 自定义输入栏渲染。设置此 Slot 后将完全替换默认输入栏。
     */
    var inputBar: InputBarSlot? = null

    /**
     * 自定义导航栏渲染。设置此 Slot 后将完全替换默认导航栏。
     */
    var navigationBar: NavigationBarSlot? = null

    /**
     * 导航栏右侧操作区域 Slot（如"更多"按钮、搜索按钮等）。
     *
     * 示例:
     * ```kotlin
     * navigationBarTrailing = { container ->
     *     container.Image {
     *         attr { size(24f, 24f); src("...") }
     *         event { click { /* 打开更多菜单 */ } }
     *     }
     * }
     * ```
     */
    var navigationBarTrailing: ViewSlot? = null

    /**
     * 消息列表为空时的占位渲染。
     *
     * 示例:
     * ```kotlin
     * emptyView = { container ->
     *     container.Text {
     *         attr { text("暂无消息，快来打个招呼吧～"); fontSize(14f) }
     *     }
     * }
     * ```
     */
    var emptyView: ViewSlot? = null
}

// ============================
// 工具类
// ============================

/**
 * 聊天消息工具类
 */
object ChatMessageHelper {
    private var messageIdCounter = 0

    /**
     * 生成消息唯一ID
     */
    fun generateId(): String {
        messageIdCounter++
        return "msg_${messageIdCounter}_${currentTimestamp()}"
    }

    private fun currentTimestamp(): Long {
        return messageIdCounter.toLong()
    }

    /**
     * 创建文本消息
     */
    fun createTextMessage(
        content: String,
        isSelf: Boolean,
        senderName: String = "",
        senderAvatar: String = "",
        status: MessageStatus = MessageStatus.SENT
    ): ChatMessage {
        return ChatMessage(
            id = generateId(),
            content = content,
            isSelf = isSelf,
            type = MessageType.TEXT,
            status = status,
            senderName = senderName,
            senderAvatar = senderAvatar,
            timestamp = 0L
        )
    }

    /**
     * 创建图片消息
     */
    fun createImageMessage(
        imageUrl: String,
        isSelf: Boolean,
        senderName: String = "",
        senderAvatar: String = "",
        width: Int = 0,
        height: Int = 0
    ): ChatMessage {
        return ChatMessage(
            id = generateId(),
            content = imageUrl,
            isSelf = isSelf,
            type = MessageType.IMAGE,
            senderName = senderName,
            senderAvatar = senderAvatar,
            timestamp = 0L,
            extra = mapOf("width" to width.toString(), "height" to height.toString())
        )
    }

    /**
     * 创建系统消息
     */
    fun createSystemMessage(content: String): ChatMessage {
        return ChatMessage(
            id = generateId(),
            content = content,
            isSelf = false,
            type = MessageType.SYSTEM,
            timestamp = 0L
        )
    }
}
