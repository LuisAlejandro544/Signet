package com.example.ui.screens.generate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.KeyAlgorithm
import com.example.ui.FormState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GeneratePresetsSection(
    formState: FormState,
    onApplyPreset: (String) -> Unit,
    onFillQuickTest: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Plantillas Rápidas & Generación de Prueba",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = false,
                onClick = onFillQuickTest,
                label = { Text("Prueba Rápida Completa 🎲", fontWeight = FontWeight.Bold) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Casino,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    labelColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.testTag("chip_quick_test_profile")
            )
            FilterChip(
                selected = formState.alias == "key0" && formState.fileExtension == "jks",
                onClick = { onApplyPreset("release") },
                label = { Text("Release (.jks)") },
                leadingIcon = {
                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            )
            FilterChip(
                selected = formState.fileExtension == "pfx",
                onClick = { onApplyPreset("windows") },
                label = { Text("Windows (.pfx)") }
            )
            FilterChip(
                selected = formState.fileExtension == "p12",
                onClick = { onApplyPreset("p12") },
                label = { Text("Multiplataforma (.p12)") }
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
