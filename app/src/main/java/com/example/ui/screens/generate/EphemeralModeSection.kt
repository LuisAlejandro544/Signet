package com.example.ui.screens.generate

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.FormState

@Composable
fun EphemeralModeSection(
    formState: FormState,
    onToggleEphemeral: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val isEphemeral = formState.isEphemeral
    val cardBorderColor by animateColorAsState(
        targetValue = if (isEphemeral) {
            MaterialTheme.colorScheme.tertiary
        } else {
            MaterialTheme.colorScheme.outlineVariant
        },
        label = "ephemeralBorderColor"
    )
    val cardContainerColor by animateColorAsState(
        targetValue = if (isEphemeral) {
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "ephemeralContainerColor"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = cardContainerColor),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, cardBorderColor, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isEphemeral) Icons.Default.VisibilityOff else Icons.Outlined.DeleteSweep,
                            contentDescription = null,
                            tint = if (isEphemeral) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Modo Efímero (Sin Rastro)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isEphemeral) "Generar solo en memoria RAM (Zero-Footprint)" else "Guardar copia cifrada en la app",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = isEphemeral,
                    onCheckedChange = onToggleEphemeral,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onTertiary,
                        checkedTrackColor = MaterialTheme.colorScheme.tertiary
                    ),
                    modifier = Modifier.testTag("switch_ephemeral_mode")
                )
            }

            if (isEphemeral) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "La clave se procesará exclusivamente en la memoria para que descargues el ZIP, exportes el JKS o copies el Base64. Al cerrar la hoja de detalles o la app, desaparecerá por completo sin dejar rastro en la base de datos.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}
