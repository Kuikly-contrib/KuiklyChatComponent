package com.kuikly.kuiklychat

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.reactive.handler.*
import com.tencent.kuikly.core.timer.setTimeout
import com.kuikly.kuiklychat.base.BasePager
import com.tencent.kuiklybase.chat.*

/**
 * 聊天组件 Demo 页面
 *
 * 展示 ChatSession 组件的开箱即用体验 + 各种自定义能力。
 * 当前激活的是「背景图 + 自定义气泡颜色」示例，其他示例见注释。
 */
@Page("chat", supportInLocal = true)
internal class ChatDemoPage : BasePager() {

    // Pager 层声明 observableList
    var messageList by observableList<ChatMessage>()

    override fun created() {
        super.created()
        messageList.addAll(createInitialMessages())
    }

    override fun body(): ViewBuilder {
        val ctx = this
        val chatTitle = pageData.params.optString("chatTitle").ifEmpty { "KuiklyChat" }
        return {

            // ============================
            // 当前激活：背景图 + 自定义气泡颜色
            // ============================
            ChatSession({ ctx.messageList }) {
                title = chatTitle
                showBackButton = true
                autoScrollToBottom = true

                //  聊天背景图
                backgroundImage = "https://picsum.photos/800/1600"

                //  自定义气泡颜色
                primaryColor = 0xFF6C5CE7          // 自己的气泡渐变起始色
                primaryGradientEndColor = 0xFFA29BFE // 自己的气泡渐变结束色
                otherBubbleColor = 0xFFF5F0FF       // 对方气泡背景（淡紫色）
                otherTextColor = 0xFF2D3436          // 对方文字颜色
                selfTextColor = 0xFFFFFFFF           // 自己文字颜色

                onBackClick = {
                    ctx.acquireModule<RouterModule>(RouterModule.MODULE_NAME).closePage()
                }
                onSendMessage = { text ->
                    ctx.onSendMessage(text)
                }
            }

            // ============================
            // 示例 1：最简用法（开箱即用）
            // ============================
            // ChatSession({ ctx.messageList }) {
            //     title = chatTitle
            //     showBackButton = true
            //     autoScrollToBottom = true
            //     onBackClick = {
            //         ctx.acquireModule<RouterModule>(RouterModule.MODULE_NAME).closePage()
            //     }
            //     onSendMessage = { text -> ctx.onSendMessage(text) }
            // }

            // ============================
            // 示例 2：隐藏头像 + 隐藏昵称（简洁 1v1 聊天风格）
            // ============================
            // ChatSession({ ctx.messageList }) {
            //     title = chatTitle
            //     showAvatar = false
            //     showSenderName = false
            //     onSendMessage = { text -> ctx.onSendMessage(text) }
            // }

            // ============================
            // 示例 3：自定义气泡渲染（Slot 模式）
            // ============================
            // ChatSession({ ctx.messageList }) {
            //     title = chatTitle
            //     onSendMessage = { text -> ctx.onSendMessage(text) }
            //     messageBubble = { container, message, config ->
            //         container.ChatBubble {
            //             attr {
            //                 content = message.content
            //                 isSelf = message.isSelf
            //                 avatarUrl = message.senderAvatar
            //                 senderName = if (!message.isSelf) message.senderName else ""
            //                 primaryColor = 0xFFFF6B6B
            //                 primaryGradientEndColor = 0xFFEE5A24
            //             }
            //         }
            //     }
            // }

            // ============================
            // 示例 4：自定义输入栏（Slot 模式）
            // ============================
            // ChatSession({ ctx.messageList }) {
            //     title = chatTitle
            //     onSendMessage = { text -> ctx.onSendMessage(text) }
            //     inputBar = { container, onSend ->
            //         container.ChatInputBar {
            //             attr {
            //                 placeholder = "说点什么吧..."
            //                 primaryColor = 0xFFFF6B6B
            //                 primaryGradientEndColor = 0xFFEE5A24
            //                 sendButtonText = "GO"
            //             }
            //             event { onSendMessage = onSend }
            //         }
            //     }
            // }

            // ============================
            // 示例 5：只读模式（隐藏输入栏，查看聊天记录）
            // ============================
            // ChatSession({ ctx.messageList }) {
            //     title = "聊天记录"
            //     showInputBar = false
            // }
        }
    }


    private fun onSendMessage(text: String) {
        val userMessage = ChatMessageHelper.createTextMessage(
            content = text,
            isSelf = true,
            senderName = "我",
            status = MessageStatus.SENT
        )
        messageList.add(userMessage)

        // 模拟自动回复（延迟 1 秒）
        setTimeout(1000) {
            messageList.add(createAutoReply(text))
        }
    }

    private fun createInitialMessages(): List<ChatMessage> {
        return listOf(
            ChatMessageHelper.createSystemMessage("以下是新的聊天"),
            ChatMessageHelper.createTextMessage(
                content = "你好！欢迎使用 KuiklyChat 💬",
                isSelf = false,
                senderName = "小助手",
                senderAvatar = ASSISTANT_AVATAR
            ),
            ChatMessageHelper.createTextMessage(
                content = "这是一个基于 KuiklyUI 框架构建的聊天组件示例",
                isSelf = false,
                senderName = "小助手",
                senderAvatar = ASSISTANT_AVATAR
            ),
            ChatMessageHelper.createTextMessage(
                content = "你好！这个聊天界面看起来不错 👍",
                isSelf = true,
                senderName = "我"
            ),
            ChatMessageHelper.createTextMessage(
                content = "支持背景图、自定义气泡颜色、隐藏头像等功能哦～",
                isSelf = false,
                senderName = "小助手",
                senderAvatar = ASSISTANT_AVATAR
            ),
            ChatMessageHelper.createTextMessage(
                content = "Kuikly 跨端框架真的很强大！",
                isSelf = true,
                senderName = "我"
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
            senderAvatar = ASSISTANT_AVATAR
        )
    }

    companion object {
        const val ASSISTANT_AVATAR =
            "https://vfiles.gtimg.cn/wuji_dashboard/wupload/xy/starter/62394e19.png"
    }
}
