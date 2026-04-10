package com.tencent.kuiklybase.chat.ai

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.reactive.handler.*
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.*

// ============================
// AI 打字效果指示器组件
// ============================

/**
 * ChatAiTypingIndicatorView - AI 打字效果指示器组件
 *
 * 参考 Stream Chat Android 的 AiTypingIndicator 实现，
 * 在消息列表中显示 AI 正在生成回复的状态指示器。
 *
 * 特性：
 * - 文字标签（如 "AI 正在思考..."）
 * - 三个带延迟缩放动画的圆点（波浪式跳动）
 * - 整体微光闪烁效果（模拟 Shimmer）
 *
 * 使用方式：
 * ```
 * ChatAiTypingIndicator {
 *     attr {
 *         text = "AI 正在思考..."
 *         dotColor = 0xFF999999
 *         textColor = 0xFF666666
 *         textFontSize = 14f
 *         dotSize = 8f
 *     }
 * }
 * ```
 */
class ChatAiTypingIndicatorView : ComposeView<ChatAiTypingIndicatorAttr, ComposeEvent>() {
    override fun createAttr(): ChatAiTypingIndicatorAttr = ChatAiTypingIndicatorAttr()
    override fun createEvent(): ComposeEvent = ComposeEvent()

    // ---- 动画状态 ----

    /**
     * 三个圆点各自的动画阶段
     * 每个圆点在 0→1→0 之间循环，通过延迟错开形成波浪效果
     * dotPhase 取值 0~5，每个圆点在不同阶段激活：
     *   dot0: phase 0,1 激活
     *   dot1: phase 1,2 激活
     *   dot2: phase 2,3 激活
     */
    private var dotPhase by observable(0)

    /**
     * 微光闪烁阶段（0 或 1 交替），控制整体 opacity 在 0.6~1.0 之间渐变
     */
    private var shimmerPhase by observable(0)

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    flexDirectionRow()
                    alignItems(FlexAlign.CENTER)
                    padding(
                        ctx.attr.paddingVertical,
                        ctx.attr.paddingHorizontal,
                        ctx.attr.paddingVertical,
                        ctx.attr.paddingHorizontal
                    )
                    // 微光闪烁效果：整体 opacity 在 0.6~1.0 之间渐变
                    val shimmerOpacity = if (ctx.shimmerPhase == 0) 1f else 0.55f
                    opacity(shimmerOpacity)
                    animate(Animation.easeInOut(0.8f), ctx.shimmerPhase)
                }

                // ===== 文字标签 =====
                if (ctx.attr.text.isNotEmpty()) {
                    Text {
                        attr {
                            text(ctx.attr.text)
                            fontSize(ctx.attr.textFontSize)
                            color(Color(ctx.attr.textColor))
                            fontWeightMedium()
                        }
                    }

                    // 文字与圆点之间的间距
                    View {
                        attr {
                            width(ctx.attr.dotSpacing)
                        }
                    }
                }

                // ===== 三个跳动圆点 =====
                for (i in 0..2) {
                    View {
                        attr {
                            val dotSize = ctx.attr.dotSize
                            size(dotSize, dotSize)
                            borderRadius(dotSize / 2f)
                            backgroundColor(Color(ctx.attr.dotColor))
                            if (i < 2) {
                                marginRight(ctx.attr.dotGap)
                            }

                            // 波浪式缩放动画：
                            // 每个圆点根据 dotPhase 判断是否处于激活状态
                            // dot0 在 phase=0 时激活
                            // dot1 在 phase=1 时激活
                            // dot2 在 phase=2 时激活
                            val isActive = (ctx.dotPhase % 3) == i
                            if (isActive) {
                                opacity(1f)
                                transform(Scale(1.4f, 1.4f))
                            } else {
                                // 非激活圆点保持较小且半透明
                                val minScale = 0.55f
                                opacity(0.5f)
                                transform(Scale(minScale, minScale))
                            }
                            animate(Animation.easeInOut(0.25f), ctx.dotPhase)
                        }
                    }
                }
            }
        }
    }

    override fun viewDidLayout() {
        super.viewDidLayout()
        // 启动圆点波浪动画
        startDotAnimation()
        // 启动微光闪烁动画
        startShimmerAnimation()
    }

    /**
     * 圆点波浪动画：每 200ms 切换一次 dotPhase（0→1→2→0 循环）
     */
    private fun startDotAnimation() {
        setTimeout(DOT_ANIMATION_INTERVAL) {
            dotPhase = (dotPhase + 1) % 3
            startDotAnimation()
        }
    }

    /**
     * 微光闪烁动画：每 800ms 切换一次 shimmerPhase（0↔1 交替）
     */
    private fun startShimmerAnimation() {
        setTimeout(SHIMMER_ANIMATION_INTERVAL) {
            shimmerPhase = if (shimmerPhase == 0) 1 else 0
            startShimmerAnimation()
        }
    }

    companion object {
        /** 圆点动画切换间隔（毫秒），对应参考代码中的 delayUnit = 200 */
        private const val DOT_ANIMATION_INTERVAL = 200
        /** 微光闪烁切换间隔（毫秒） */
        private const val SHIMMER_ANIMATION_INTERVAL = 800
    }
}

// ============================
// Attr 属性类
// ============================

class ChatAiTypingIndicatorAttr : ComposeAttr() {
    /** 显示的文字标签（如 "AI 正在思考..."、"正在生成回复..."） */
    var text: String by observable("AI 正在思考...")
    /** 文字颜色 */
    var textColor: Long by observable(0xFF666666)
    /** 文字字号 */
    var textFontSize: Float by observable(14f)
    /** 圆点颜色 */
    var dotColor: Long by observable(0xFF999999)
    /** 单个圆点直径 */
    var dotSize: Float by observable(8f)
    /** 圆点之间的间距 */
    var dotGap: Float by observable(4f)
    /** 文字与圆点之间的间距 */
    var dotSpacing: Float by observable(6f)
    /** 容器上下内边距 */
    var paddingVertical: Float by observable(10f)
    /** 容器左右内边距 */
    var paddingHorizontal: Float by observable(16f)
}

// ============================
// 扩展函数（注册到 DSL）
// ============================

/**
 * AI 打字效果指示器
 *
 * 在消息列表中显示 AI 正在生成回复的状态，包含文字标签和三个波浪式跳动圆点。
 *
 * @param init 组件配置 Lambda
 *
 * 示例：
 * ```kotlin
 * ChatAiTypingIndicator {
 *     attr {
 *         text = "AI 正在思考..."
 *         dotColor = 0xFF4F8FFF
 *         textColor = 0xFF666666
 *     }
 * }
 * ```
 */
fun ViewContainer<*, *>.ChatAiTypingIndicator(init: ChatAiTypingIndicatorView.() -> Unit) {
    addChild(ChatAiTypingIndicatorView(), init)
}
