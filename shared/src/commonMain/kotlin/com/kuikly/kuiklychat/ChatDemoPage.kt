package com.kuikly.kuiklychat

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.reactive.handler.*
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.*
import com.tencent.kuikly.core.views.layout.Row
import com.kuikly.kuiklychat.base.BasePager
import com.tencent.kuiklybase.chat.*

/**
 * 聊天组件 Demo 页面
 *
 * 展示 ChatSession 组件的完整功能：
 * - theme {} DSL 配置主题
 * - 消息分组（连续同一发送者合并头像）
 * - 图片消息
 * - 文本消息
 * - 系统消息
 */
@Page("chat", supportInLocal = true)
internal class ChatDemoPage : BasePager() {

    var messageList by observableList<ChatMessage>()

    // 输入框
    private var currentInputText: String = ""
    private lateinit var inputViewRef: ViewRef<InputView>

    // 持有 ChatSession 配置引用
    private var chatSessionConfig: ChatSessionConfig? = null

    override fun created() {
        super.created()
        messageList.addAll(createInitialMessages())
    }

    override fun body(): ViewBuilder {
        val ctx = this
        val chatTitle = pageData.params.optString("chatTitle").ifEmpty { "KuiklyChat" }
        return {
            View {
                attr {
                    flex(1f)
                    flexDirection(FlexDirection.COLUMN)
                }

                // ============================
                // ChatSession - 消息列表容器
                // ============================
                ChatSession({ ctx.messageList }) {
                    title = chatTitle
                    showBackButton = true
                    selfAvatarUrl = USER_AVATAR

                    // ---- 主题配置（新 DSL 方式，参考 Stream Chat 的 ChatTheme） ----
                    theme {
                        primaryColor = 0xFF6C5CE7
                        primaryGradientEndColor = 0xFFA29BFE
                        otherBubbleColor = 0xFFF5F0FF
                        otherTextColor = 0xFF2D3436
                        selfTextColor = 0xFFFFFFFF
                    }

                    // ---- 消息列表行为配置 ----
                    messageListOptions {
                        autoScrollToBottom = true
                        showTimeGroup = true
                        timeGroupInterval = 3 * 60 * 1000L
                        // 启用消息分组（连续同一发送者的消息合并头像、缩小间距）
                        enableMessageGrouping = true
                        messageGroupingInterval = 2 * 60 * 1000L
                    }

                    // ---- 事件回调 ----
                    onBackClick = {
                        ctx.acquireModule<RouterModule>(RouterModule.MODULE_NAME).closePage()
                    }
                    onMessageClick = { message ->
                        // 处理消息点击
                    }
                    onMessageLongPress = { message ->
                        // 处理消息长按
                    }
                    onResend = { message ->
                        // 业务处理重发逻辑
                    }

                    // 保存配置引用
                    ctx.chatSessionConfig = this
                }


                View {
                    attr {
                        backgroundColor(Color(0xFFF8F8F8))
                        border(Border(0.5f, BorderStyle.SOLID, Color(0xFFE0E0E0)))
                    }
                    Row {
                        attr {
                            padding(8f, 12f, 8f, 12f)
                            alignItems(FlexAlign.CENTER)
                        }

                        // 输入框
                        View {
                            attr {
                                flex(1f)
                                height(36f)
                                backgroundColor(Color.WHITE)
                                borderRadius(18f)
                                border(Border(0.5f, BorderStyle.SOLID, Color(0xFFE0E0E0)))
                                flexDirectionRow()
                                alignItemsCenter()
                            }
                            Input {
                                ref { ctx.inputViewRef = it }
                                attr {
                                    flex(1f)
                                    height(36f)
                                    fontSize(15f)
                                    color(Color(0xFF333333))
                                    placeholder("输入消息...")
                                    placeholderColor(Color(0xFFBBBBBB))
                                    marginLeft(14f)
                                    marginRight(14f)
                                    returnKeyTypeSend()
                                }
                                event {
                                    textDidChange { params ->
                                        ctx.currentInputText = params.text
                                    }
                                    inputReturn { params ->
                                        if (params.text.isNotBlank()) {
                                            ctx.currentInputText = params.text
                                        }
                                        ctx.sendMessage()
                                    }
                                }
                            }
                        }

                        // 发送按钮
                        View {
                            attr {
                                minWidth(60f)
                                height(36f)
                                marginLeft(4f)
                                borderRadius(18f)
                                backgroundLinearGradient(
                                    Direction.TO_RIGHT,
                                    ColorStop(Color(0xFF6C5CE7), 0f),
                                    ColorStop(Color(0xFFA29BFE), 1f)
                                )
                                allCenter()
                                padding(0f, 12f, 0f, 12f)
                            }
                            Text {
                                attr {
                                    text("发送")
                                    fontSize(14f)
                                    fontWeightMedium()
                                    color(Color.WHITE)
                                }
                            }
                            event {
                                click {
                                    ctx.sendMessage()
                                }
                            }
                        }
                    }

                    // 底部安全区域占位
                    View {
                        attr {
                            height(ctx.pagerData.safeAreaInsets.bottom)
                            backgroundColor(Color(0xFFF8F8F8))
                        }
                    }
                }
            }
        }
    }


    private fun sendMessage() {
        val text = currentInputText.trim()
        if (text.isNotEmpty()) {
            val userMessage = ChatMessageHelper.createTextMessage(
                content = text,
                isSelf = true,
                senderName = "我",
                status = MessageStatus.SENT
            )
            messageList.add(userMessage)
            currentInputText = ""
            inputViewRef.view?.setText("")

            // 模拟自动回复（延迟 1 秒）
            setTimeout(1000) {
                val reply = createAutoReply(text)
                messageList.add(reply)
            }
        }
    }

    private fun createInitialMessages(): List<ChatMessage> {
        val baseTime = 1711267200000L
        return listOf(
            ChatMessageHelper.createSystemMessage("以下是新的聊天"),
            ChatMessageHelper.createTextMessage(
                content = "你好！欢迎使用 KuiklyChat",
                isSelf = false,
                senderName = "小助手",
                timestamp = baseTime
            ),
            ChatMessageHelper.createTextMessage(
                content = "这是一个基于 KuiklyUI 框架构建的聊天组件示例",
                isSelf = false,
                senderName = "小助手",
                timestamp = baseTime + 30_000L
            ),
            ChatMessageHelper.createTextMessage(
                content = "你好！这个聊天界面看起来不错 👍",
                isSelf = true,
                senderName = "我",
                timestamp = baseTime + 60_000L
            ),
            ChatMessageHelper.createTextMessage(
                content = "支持背景图、自定义气泡颜色、时间分组、消息重发等功能哦～",
                isSelf = false,
                senderName = "小助手",
                timestamp = baseTime + 5 * 60_000L
            ),
            // 连续消息分组 Demo：同一发送者的连续消息会合并头像
            ChatMessageHelper.createTextMessage(
                content = "现在还支持图片消息了！来看看 👇",
                isSelf = false,
                senderName = "小助手",
                timestamp = baseTime + 5 * 60_000L + 10_000L
            ),
            // 图片消息 Demo
            ChatMessageHelper.createImageMessage(
                imageUrl = "https://picsum.photos/600/400",
                isSelf = false,
                senderName = "小助手",
                width = 600,
                height = 400,
                timestamp = baseTime + 5 * 60_000L + 20_000L
            ),
            ChatMessageHelper.createTextMessage(
                content = "Kuikly 跨端框架真的很强大！",
                isSelf = true,
                senderName = "我",
                timestamp = baseTime + 6 * 60_000L
            ),
            // 自己发的图片
            ChatMessageHelper.createImageMessage(
                imageUrl = "https://picsum.photos/400/600",
                isSelf = true,
                senderName = "我",
                width = 400,
                height = 600,
                timestamp = baseTime + 6 * 60_000L + 10_000L
            )
        )
    }

    private fun createAutoReply(userMessage: String): ChatMessage {
        val replies = listOf(
            "收到你的消息：\"$userMessage\" 😊",
            "好的，我知道了～",
            "这是一条自动回复消息",
            "Kuikly 框架太好用了！",
            "你说的很有道理 👏",
            "让我想想... 🤔",
            "哈哈，有意思！😄"
        )
        return ChatMessageHelper.createTextMessage(
            content = replies.random(),
            isSelf = false,
            senderName = "小助手",
        )
    }

    companion object {
        const val USER_AVATAR =
            "https://picsum.photos/800/1600"
    }
}
