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
 * 展示 ChatSession 组件的开箱即用体验：
 * - Pager 层声明 observableList
 * - created() 中加载初始数据
 * - body() 中使用 ChatSession({ messageList }) { ... } 一行搞定
 */
@Page("chat", supportInLocal = true)
internal class ChatDemoPage : BasePager() {

    // Pager 层声明 observableList
    var messageList by observableList<ChatMessage>()

    override fun created() {
        super.created()
        // 在 Pager.created() 中加载初始数据
        messageList.addAll(createInitialMessages())
    }

    override fun body(): ViewBuilder {
        val ctx = this
        val chatTitle = pageData.params.optString("chatTitle").ifEmpty { "KuiklyChat" }
        return {
            // ChatSession 是纯 DSL 扩展函数，vfor 直接在此上下文中展开
            // 响应式依赖收集直接绑定到 Pager 层的 observableList
            ChatSession({ ctx.messageList }) {
                title = chatTitle
                showBackButton = true
                primaryColor = 0xFF4F8FFF
                primaryGradientEndColor = 0xFF6C5CE7
                onBackClick = {
                    ctx.acquireModule<RouterModule>(RouterModule.MODULE_NAME).closePage()
                }
                onSendMessage = { text ->
                    ctx.onSendMessage(text)
                }
            }
        }
    }

    // ============================
    // 业务逻辑
    // ============================

    private fun onSendMessage(text: String) {
        // 添加用户消息
        messageList.add(
            ChatMessageHelper.createTextMessage(
                content = text,
                isSelf = true,
                senderName = "我"
            )
        )

        // 模拟自动回复（延迟 1 秒）
        setTimeout(1000) {
            messageList.add(createAutoReply(text))
        }
    }

    private fun createInitialMessages(): List<ChatMessage> {
        return listOf(
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
                content = "支持文本消息发送和接收，还有系统提示消息哦～",
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
