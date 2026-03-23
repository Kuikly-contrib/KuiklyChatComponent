package com.tencent.kuiklybase.chat

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.reactive.collection.ObservableList
import com.tencent.kuikly.core.reactive.handler.*
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.*

// ============================
// 聊天会话配置
// ============================

/**
 * ChatSession 的配置参数
 */
class ChatSessionConfig {
    // 导航栏配置
    var title: String = "聊天"
    var showNavigationBar: Boolean = true
    var showBackButton: Boolean = true

    // 输入栏配置
    var inputPlaceholder: String = "输入消息..."

    // 头像配置
    var selfAvatarUrl: String = ""

    // 主题色配置
    var primaryColor: Long = 0xFF4F8FFF
    var primaryGradientEndColor: Long = 0xFF6C5CE7
    var backgroundColor: Long = 0xFFF0F2F5

    // 事件回调
    var onSendMessage: ((String) -> Unit)? = null
    var onBackClick: (() -> Unit)? = null
    var onMessageLongPress: ((ChatMessage) -> Unit)? = null
}

// ============================
// ChatSession DSL 扩展函数
// ============================

/**
 * ChatSession - 完整聊天会话的 DSL 扩展函数

 */
fun ViewContainer<*, *>.ChatSession(
    messageList: () -> ObservableList<ChatMessage>,
    config: ChatSessionConfig.() -> Unit
) {
    val cfg = ChatSessionConfig().apply(config)

    // ========== 根容器 ==========
    View {
        attr {
            flex(1f)
            backgroundColor(Color(cfg.backgroundColor))
            flexDirection(FlexDirection.COLUMN)
        }

        // ========== 顶部导航栏 ==========
        if (cfg.showNavigationBar) {
            ChatNavigationBar {
                attr {
                    title = cfg.title
                    showBackButton = cfg.showBackButton
                    primaryColor = cfg.primaryColor
                    primaryGradientEndColor = cfg.primaryGradientEndColor
                }
                event {
                    onBackClick = cfg.onBackClick
                }
            }
        }

        // ========== 消息列表区域 ==========
        List {
            attr {
                flex(1f)
                flexDirection(FlexDirection.COLUMN)
            }

            // vfor 直接在 Pager body 上下文中执行（关键！）
            // 响应式依赖收集直接绑定到 Pager 层的 observableList
            vfor(messageList) { message ->
                if (message.type == MessageType.SYSTEM) {
                    ChatSystemMessage {
                        attr {
                            this.message = message.content
                        }
                    }
                } else {
                    ChatBubble {
                        attr {
                            content = message.content
                            isSelf = message.isSelf
                            avatarUrl = message.senderAvatar
                            selfAvatarUrl = cfg.selfAvatarUrl
                            senderName = if (!message.isSelf) message.senderName else ""
                            primaryColor = cfg.primaryColor
                            primaryGradientEndColor = cfg.primaryGradientEndColor
                        }
                        event {
                            onLongPress = {
                                cfg.onMessageLongPress?.invoke(message)
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

        // ========== 底部输入栏 ==========
        ChatInputBar {
            attr {
                placeholder = cfg.inputPlaceholder
                primaryColor = cfg.primaryColor
                primaryGradientEndColor = cfg.primaryGradientEndColor
            }
            event {
                onSendMessage = { text ->
                    cfg.onSendMessage?.invoke(text)
                }
            }
        }
    }
}
