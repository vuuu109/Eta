package fuck.andes.agent.overlay

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Check
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Sidebar
import top.yukonga.miuix.kmp.theme.MiuixTheme

// ── 颜色 ─────────────────────────────────────────────────────────────────────
// 与 SettingsScreen 的图标 tint 风格保持一致的语义色
private val StopColor = Color(0xFFFF453A)
private val SuccessColor = Color(0xFF34C759)

/**
 * Agent 运行状态浮窗主体。
 *
 * 展开态：标题 + 一句话状态 + 进度/结果指示 + 停止/收起按钮，顶部手柄可拖动。
 * 折叠态：小药丸，整条可拖动，点击展开恢复。
 *
 * 实际屏幕定位由 Agent Runtime 的 WindowManager LayoutParams 驱动，
 * [onDrag] 回调把指针位移转译为 LayoutParams 的 x/y 增量。
 */
@Composable
internal fun AgentOverlayContent(
    state: AgentOverlayState,
    collapsed: Boolean,
    onDrag: (dx: Float, dy: Float) -> Unit,
    onToggleCollapse: () -> Unit,
    onStop: () -> Unit,
) {
    if (collapsed) {
        CollapsedPill(state = state, onDrag = onDrag, onExpand = onToggleCollapse)
    } else {
        ExpandedCard(
            state = state,
            onDrag = onDrag,
            onToggleCollapse = onToggleCollapse,
            onStop = onStop,
        )
    }
}

@Composable
private fun ExpandedCard(
    state: AgentOverlayState,
    onDrag: (dx: Float, dy: Float) -> Unit,
    onToggleCollapse: () -> Unit,
    onStop: () -> Unit,
) {
    Card(modifier = Modifier.width(252.dp)) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            // ── 手柄行：拖动手柄 + 标题 + 停止 + 收起 ───────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.pointerInput(Unit) {
                        detectDragGestures { _, dragAmount -> onDrag(dragAmount.x, dragAmount.y) }
                    },
                ) {
                    Icon(
                        imageVector = MiuixIcons.Sidebar,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "小布 Agent",
                    modifier = Modifier.weight(1f),
                    color = MiuixTheme.colorScheme.onSurfaceContainer,
                    fontSize = MiuixTheme.textStyles.title3.fontSize,
                )
                if (state.phase == AgentOverlayPhase.RUNNING) {
                    IconButton(onClick = onStop) {
                        Icon(
                            imageVector = MiuixIcons.Close,
                            contentDescription = "停止",
                            modifier = Modifier.size(18.dp),
                            tint = StopColor,
                        )
                    }
                }
                IconButton(onClick = onToggleCollapse) {
                    Icon(
                        imageVector = MiuixIcons.More,
                        contentDescription = "收起",
                        modifier = Modifier.size(18.dp),
                        tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── 状态行：进度/结果指示 + 状态文案 ───────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusIndicator(state = state, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = state.statusText,
                    modifier = Modifier.weight(1f),
                    color = MiuixTheme.colorScheme.onSurface,
                    fontSize = MiuixTheme.textStyles.body2.fontSize,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (state.detailText.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = state.detailText,
                    modifier = Modifier.fillMaxWidth(),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    fontSize = MiuixTheme.textStyles.footnote1.fontSize,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun CollapsedPill(
    state: AgentOverlayState,
    onDrag: (dx: Float, dy: Float) -> Unit,
    onExpand: () -> Unit,
) {
    Card {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .pointerInput(Unit) {
                    detectDragGestures { _, dragAmount -> onDrag(dragAmount.x, dragAmount.y) }
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusIndicator(state = state, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            IconButton(onClick = onExpand) {
                Icon(
                    imageVector = MiuixIcons.More,
                    contentDescription = "展开",
                    modifier = Modifier.size(16.dp),
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
    }
}

@Composable
private fun StatusIndicator(state: AgentOverlayState, modifier: Modifier = Modifier) {
    when (state.phase) {
        AgentOverlayPhase.RUNNING -> InfiniteProgressIndicator(
            modifier = modifier,
            color = MiuixTheme.colorScheme.primary,
        )

        AgentOverlayPhase.FINISHED -> Icon(
            imageVector = MiuixIcons.Basic.Check,
            contentDescription = null,
            modifier = modifier,
            tint = SuccessColor,
        )

        AgentOverlayPhase.FAILED -> Icon(
            imageVector = MiuixIcons.Close,
            contentDescription = null,
            modifier = modifier,
            tint = StopColor,
        )
    }
}
