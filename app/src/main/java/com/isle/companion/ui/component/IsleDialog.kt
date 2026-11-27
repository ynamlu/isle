package com.isle.companion.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.isle.companion.ui.theme.IsleRadius
import com.isle.companion.ui.theme.IsleSpacing
import com.isle.companion.ui.theme.isleBackground

@Composable
internal fun IsleDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest, properties = properties) {
        Surface(shape = IsleRadius.cardLarge) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .isleBackground()
                    .verticalScroll(rememberScrollState())
                    .padding(IsleSpacing.xl),
                verticalArrangement = Arrangement.spacedBy(IsleSpacing.md),
                content = content
            )
        }
    }
}
