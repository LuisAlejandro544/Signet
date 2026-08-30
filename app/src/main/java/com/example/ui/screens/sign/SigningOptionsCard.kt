package com.example.ui.screens.sign

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.state.SignApkFormState

@Composable
fun SigningOptionsCard(
    formState: SignApkFormState,
    onUpdateForm: ((SignApkFormState) -> SignApkFormState) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_signing_options"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "3. Opciones de Firma y Optimización",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Scheme v1 Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Firma Esquema v1 (JAR)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Compatibilidad universal con versiones antiguas de Android.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = formState.signV1,
                    onCheckedChange = { v -> onUpdateForm { it.copy(signV1 = v) } },
                    modifier = Modifier.testTag("switch_sign_v1")
                )
            }

            // Scheme v2 Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Firma Esquema v2 (APK Signature Block)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Firma binaria criptográfica de bloque completo (Android 7.0+).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = formState.signV2,
                    onCheckedChange = { v -> onUpdateForm { it.copy(signV2 = v) } },
                    modifier = Modifier.testTag("switch_sign_v2")
                )
            }

            // Zipalign Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Optimización Zipalign (4 bytes)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Alinea datos sin comprimir para carga directa por mmap en RAM.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = formState.zipalign,
                    onCheckedChange = { v -> onUpdateForm { it.copy(zipalign = v) } },
                    modifier = Modifier.testTag("switch_zipalign")
                )
            }

            // Output filename
            OutlinedTextField(
                value = formState.outputFileName,
                onValueChange = { name -> onUpdateForm { it.copy(outputFileName = name) } },
                label = { Text("Nombre del archivo de salida") },
                placeholder = { Text("app-signed.apk") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_output_apk_filename"),
                singleLine = true
            )
        }
    }
}
