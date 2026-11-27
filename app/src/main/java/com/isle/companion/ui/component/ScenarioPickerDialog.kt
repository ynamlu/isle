package com.isle.companion.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.isle.companion.data.local.entity.CharacterEntity
import com.isle.companion.domain.scenario.Scenario
import com.isle.companion.ui.theme.IsleRadius
import com.isle.companion.ui.theme.IsleSpacing
import com.isle.companion.ui.theme.relationColor

@Composable
internal fun ScenarioPickerDialog(
    scenario: Scenario,
    characters: List<CharacterEntity>,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    IsleDialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colors.primaryContainer.copy(alpha = 0.56f),
            shape = IsleRadius.field
        ) {
            Row(
                modifier = Modifier.padding(IsleSpacing.md),
                horizontalArrangement = Arrangement.spacedBy(IsleSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(scenario.emoji, style = MaterialTheme.typography.headlineSmall)
                Column(verticalArrangement = Arrangement.spacedBy(IsleSpacing.xs)) {
                    Text(scenario.title, style = MaterialTheme.typography.titleMedium)
                    Text(scenario.subtitle, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
            }
        }

        Text("想和谁聊？", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        if (characters.isEmpty()) {
            Text("先创建一个角色，再开始场景。", style = MaterialTheme.typography.bodyMedium)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(IsleSpacing.sm)) {
                characters.forEach { character ->
                    Surface(
                        onClick = { onSelect(character.id) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = IsleRadius.field,
                        color = colors.surface.copy(alpha = 0.7f)
                    ) {
                        Row(
                            modifier = Modifier.heightIn(min = 56.dp).padding(IsleSpacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(IsleSpacing.md)
                        ) {
                            CharacterAvatar(
                                avatarPath = character.avatarPath,
                                name = character.name,
                                size = 36.dp,
                                ringColor = relationColor(character.relationType)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(character.name, style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    when (character.relationType) {
                                        "bestie" -> "闺蜜"
                                        "teacher" -> "老师"
                                        "friend" -> "朋友"
                                        "family" -> "家人"
                                        else -> "自定义"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant
                                )
                            }
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
                                tint = colors.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("取消") }
    }
}
