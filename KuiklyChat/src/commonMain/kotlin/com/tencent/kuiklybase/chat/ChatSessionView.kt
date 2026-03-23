package com.tencent.kuiklybase.chat

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.reactive.collection.ObservableList
import com.tencent.kuikly.core.reactive.handler.*
import com.tencent.kuikly.core.views.*

// ============================
// ChatSession DSL 扩展函数
// ============================

/**
 * ChatSession - 完整聊天会话的 DSL 扩展函数
 *
 * 开箱即用的聊天界面，同时支持通过 Slot 深度定制。
 *
 * ## 最简用法
 * ```kotlin
 * ChatSession({ ctx.messageList }) {
 *     title = "聊天"
 *     onSendMessage = { text -> ctx.messageList.add(...) }
 * }
 * ```
 *
 * ## 设置背景图 + 自定义气泡颜色
 * ```kotlin
 * ChatSession({ ctx.messageList }) {
 *     backgroundImage = "https://example.com/chat-bg.jpg"
 *     otherBubbleColor = 0xFFF5E6CC
 *     onSendMessage = { text -> ... }
 * }
 * ```
 *
 * ## 自定义气泡
 * ```kotlin
 * ChatSession({ ctx.messageList }) {
 *     onSendMessage = { text -> ... }
 *     messageBubble = { container, message, config ->
 *         container.MyCustomBubble {
 *             attr { content = message.content }
 *         }
 *     }
 * }
 * ```
 */
fun ViewContainer<*, *>.ChatSession(
    messageList: () -> ObservableList<ChatMessage>,
    config: ChatSessionConfig.() -> Unit
) {
    val cfg = ChatSessionConfig().apply(config)
    // 用于持有 List 引用以实现自动滚动
    var listViewRef: ViewRef<ListView<*, *>>? = null

    // ========== 根容器 ==========
    View {
        attr {
            flex(1f)
            backgroundColor(Color(cfg.backgroundColor))
            flexDirection(FlexDirection.COLUMN)
        }

        // ========== 顶部导航栏 ==========
        if (cfg.showNavigationBar) {
            if (cfg.navigationBar != null) {
                // Slot: 自定义导航栏
                cfg.navigationBar!!.invoke(this@View, cfg)
            } else {
                // 默认导航栏
                ChatNavigationBar {
                    attr {
                        title = cfg.title
                        showBackButton = cfg.showBackButton
                        primaryColor = cfg.primaryColor
                        primaryGradientEndColor = cfg.primaryGradientEndColor
                    }
                    event {
                        onBackClick = cfg.onBackClick
                        onTrailingClick = null  // 导航栏右侧通过 slot 实现
                    }
                }
                // 导航栏右侧操作按钮 Slot（叠加在导航栏上方）
                // 注意：这里不使用 slot 叠加方式，而是通过 ChatNavigationBar 的 trailing slot 传入
            }
        }

        // ========== 消息列表区域（含背景图） ==========
        View messageArea@{
            attr {
                flex(1f)
            }

            // 背景图层（绝对定位铺满）
            if (cfg.backgroundImage.isNotEmpty()) {
                Image {
                    attr {
                        positionAbsolute()
                        top(0f)
                        left(0f)
                        right(0f)
                        bottom(0f)
                        src(cfg.backgroundImage)
                        resizeCover()
                    }
                }
            }

            // 消息列表
            List {
                // 保存 List 引用
                ref { listViewRef = it }

                attr {
                    flex(1f)
                    flexDirection(FlexDirection.COLUMN)
                }

                // 自动滚动到底部：监听内容尺寸变化
                if (cfg.autoScrollToBottom) {
                    event {
                        contentSizeChanged { _, contentHeight ->
                            val viewHeight = listViewRef?.view?.flexNode?.layoutFrame?.height ?: 0f
                            val scrollY = contentHeight - viewHeight
                            if (scrollY > 0) {
                                listViewRef?.view?.setContentOffset(0f, scrollY, true)
                            }
                        }
                    }
                }

                // vfor 闭包内必须有且仅有一个子节点，
                // 用 View 包一层确保满足约束，slot 的 container 传内部上下文而非 this@List。
                vfor(messageList) { message ->
                    View itemRoot@{
                        if (message.type == MessageType.SYSTEM) {
                            // ---- 系统消息 ----
                            if (cfg.systemMessage != null) {
                                cfg.systemMessage!!.invoke(this@itemRoot, message, cfg)
                            } else {
                                ChatSystemMessage {
                                    attr {
                                        this.message = message.content
                                    }
                                }
                            }
                        } else {
                            // ---- 普通消息气泡 ----
                            if (cfg.messageBubble != null) {
                                cfg.messageBubble!!.invoke(this@itemRoot, message, cfg)
                            } else {
                                ChatBubble {
                                    attr {
                                        content = message.content
                                        isSelf = message.isSelf
                                        avatarUrl = message.senderAvatar
                                        selfAvatarUrl = cfg.selfAvatarUrl
                                        senderName = if (!message.isSelf && cfg.showSenderName) message.senderName else ""
                                        primaryColor = cfg.primaryColor
                                        primaryGradientEndColor = cfg.primaryGradientEndColor
                                        otherBubbleColor = cfg.otherBubbleColor
                                        otherTextColor = cfg.otherTextColor
                                        selfTextColor = cfg.selfTextColor
                                        showAvatar = cfg.showAvatar
                                        status = message.status
                                    }
                                    event {
                                        onClick = {
                                            cfg.onMessageClick?.invoke(message)
                                        }
                                        onLongPress = {
                                            cfg.onMessageLongPress?.invoke(message)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 底部留白
                View {
                    attr {
                        height(8f)
                    }
                }
            }

            // 空消息占位（绝对定位居中）
            if (messageList().isEmpty()) {
                View emptyContainer@{
                    attr {
                        positionAbsolute()
                        top(0f)
                        left(0f)
                        right(0f)
                        bottom(0f)
                        allCenter()
                    }
                    if (cfg.emptyView != null) {
                        cfg.emptyView!!.invoke(this@emptyContainer)
                    } else {
                        // 默认空消息占位
                        Text {
                            attr {
                                text("暂无消息")
                                fontSize(14f)
                                color(Color(0xFF999999))
                            }
                        }
                    }
                }
            }
        }

        // ========== 底部输入栏 ==========
        if (cfg.showInputBar) {
            if (cfg.inputBar != null) {
                // Slot: 自定义输入栏
                cfg.inputBar!!.invoke(this@View) { text ->
                    cfg.onSendMessage?.invoke(text)
                }
            } else {
                // 默认输入栏
                ChatInputBar {
                    attr {
                        placeholder = cfg.inputPlaceholder
                        primaryColor = cfg.primaryColor
                        primaryGradientEndColor = cfg.primaryGradientEndColor
                        showSendButton = cfg.showSendButton
                        sendButtonText = cfg.sendButtonText
                    }
                    event {
                        onSendMessage = { text ->
                            cfg.onSendMessage?.invoke(text)
                        }
                    }
                }
            }
        }
    }
}
