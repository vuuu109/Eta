package fuck.andes.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R as LucideR
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.AddCircle
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Send
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AgentChatInputBar(
    input: String,
    isStreaming: Boolean,
    thinkingEnabled: Boolean,
    onInputChange: (String) -> Unit,
    onThinkingChange: (Boolean) -> Unit,
    onSend: () -> Unit,
    onAttach: () -> Unit,
    onScreenContext: () -> Unit,
    onVoice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextField(
        value = input,
        onValueChange = onInputChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        label = "输入任务",
        useLabelAsPlaceholder = true,
        singleLine = false,
        maxLines = 4,
        leadingIcon = {
            IconButton(onClick = onAttach) {
                Icon(
                    imageVector = MiuixIcons.AddCircle,
                    contentDescription = "附件",
                )
            }
        },
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onThinkingChange(!thinkingEnabled) },
                    enabled = !isStreaming,
                ) {
                    Icon(
                        painter = painterResource(LucideR.drawable.lucide_ic_sparkles),
                        contentDescription = if (thinkingEnabled) "关闭思考" else "开启思考",
                        tint = if (thinkingEnabled) {
                            MiuixTheme.colorScheme.primary
                        } else {
                            MiuixTheme.colorScheme.onSurfaceVariantActions
                        },
                    )
                }
                IconButton(
                    onClick = onSend,
                    enabled = input.isNotBlank() && !isStreaming,
                ) {
                    if (isStreaming) {
                        Icon(
                            imageVector = MiuixIcons.Close,
                            contentDescription = "停止",
                        )
                    } else {
                        Icon(
                            imageVector = MiuixIcons.Send,
                            contentDescription = "发送",
                        )
                    }
                }
            }
        },
    )
}
