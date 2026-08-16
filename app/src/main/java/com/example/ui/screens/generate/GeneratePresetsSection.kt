package com.example.ui.screens.generate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.KeyAlgorithm
import com.example.ui.FormState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GeneratePresetsSection(
    formState: FormState,
    onApplyPreset: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Plantillas Rápidas",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = formState.alias == "key0" && formState.algorithm == KeyAlgorithm.RSA_2048,
                onClick = { onApplyPreset("release") },
                label = { Text("Release Estándar") },
                leadingIcon = {
                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            )
            FilterChip(
                selected = formState.alias == "upload",
                onClick = { onApplyPreset("upload") },
                label = { Text("Upload Key") }
            )
            FilterChip(
                selected = formState.algorithm == KeyAlgorithm.RSA_4096,
                onClick = { onApplyPreset("rsa4096") },
                label = { Text("RSA 4096 Bits") }
            )
        }
    }
}
