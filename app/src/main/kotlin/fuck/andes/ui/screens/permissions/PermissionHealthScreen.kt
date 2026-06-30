package fuck.andes.ui.screens.permissions

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fuck.andes.ui.components.ArrowItem
import fuck.andes.ui.components.PrefDivider
import fuck.andes.ui.components.TintedIcon
import fuck.andes.ui.components.color
import fuck.andes.ui.components.label
import fuck.andes.ui.model.PermissionHealthAction
import fuck.andes.ui.model.PermissionHealthItemUi
import fuck.andes.ui.model.PermissionHealthUiState
import fuck.andes.ui.model.PermissionStatusUi
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Check
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Lock
import top.yukonga.miuix.kmp.icon.extended.Report
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun PermissionHealthScreen(
    state: PermissionHealthUiState,
    onAction: (PermissionHealthAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 12.dp),
    ) {
        item(key = "title") {
            SmallTitle("权限与状态")
        }
        item(key = "card") {
            Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                state.items.forEachIndexed { index, item ->
                    PermissionItemRow(
                        item = item,
                        onActionClick = { onAction(PermissionHealthAction.OpenItemAction(item.id)) },
                    )
                    if (index < state.items.lastIndex) {
                        PrefDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionItemRow(
    item: PermissionHealthItemUi,
    onActionClick: () -> Unit,
) {
    val icon = when (item.status) {
        PermissionStatusUi.Available -> MiuixIcons.Basic.Check
        PermissionStatusUi.Warning -> MiuixIcons.Report
        PermissionStatusUi.Missing -> MiuixIcons.Close
        PermissionStatusUi.Disabled -> MiuixIcons.Lock
    }
    val tint = item.status.color()
    ArrowItem(
        title = item.title,
        summary = item.summary,
        startAction = { TintedIcon(icon = icon, tint = tint) },
        endActions = {
            Text(
                text = item.status.label(),
                fontSize = MiuixTheme.textStyles.body2.fontSize,
                color = item.status.color(),
            )
        },
        onClick = onActionClick,
    )
}
