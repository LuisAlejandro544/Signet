package com.example.ui.components.details

import android.content.Context
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.IntegrationInstructions
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crypto.SnippetGenerator
import com.example.data.model.KeystoreDetails

enum class CodeSnippetTab(
    val tabLabel: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    GRADLE_KTS("build.gradle.kts", Icons.Default.Code),
    GITHUB_ACTIONS("GitHub Actions (.yml)", Icons.Default.IntegrationInstructions),
    GRADLE_GROOVY("build.gradle (Groovy)", Icons.Default.Code),
    APKSIGNER("apksigner CLI", Icons.Default.Terminal),
    PEM_CERT("Certificado PEM", Icons.Default.Security)
}

@Composable
fun KeystoreCodeSnippetsCard(
    details: KeystoreDetails,
    context: Context,
    modifier: Modifier = Modifier
) {
    var selectedSnippetTab by remember { mutableStateOf(CodeSnippetTab.GRADLE_KTS) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Configuración de Firma & CI/CD",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Text(
                text = "Visualiza y copia directamente el código listo para Gradle, pipelines de GitHub Actions o comandos de terminal.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Horizontal FilterChips for Snippet Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CodeSnippetTab.values().forEach { tab ->
                    FilterChip(
                        selected = selectedSnippetTab == tab,
                        onClick = { selectedSnippetTab = tab },
                        label = { Text(tab.tabLabel, fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            // Active Snippet Content Resolver
            val currentSnippet = when (selectedSnippetTab) {
                CodeSnippetTab.GRADLE_KTS -> SnippetGenerator.generateGradleKtsSnippet(details.fileName, details.alias)
                CodeSnippetTab.GITHUB_ACTIONS -> SnippetGenerator.generateGitHubActionsWorkflow(details.fileName, details.alias)
                CodeSnippetTab.GRADLE_GROOVY -> SnippetGenerator.generateGradleGroovySnippet(details.fileName, details.alias)
                CodeSnippetTab.APKSIGNER -> SnippetGenerator.generateApksignerSnippet(details.fileName, details.alias)
                CodeSnippetTab.PEM_CERT -> details.certificatePem
            }

            val currentDescription = when (selectedSnippetTab) {
                CodeSnippetTab.GRADLE_KTS -> "Pega este bloque en 'app/build.gradle.kts'. Lee las contraseñas de variables de entorno para máxima seguridad."
                CodeSnippetTab.GITHUB_ACTIONS -> "Crea el archivo '.github/workflows/build-and-sign.yml' en tu repositorio y agrega el secreto 'KEYSTORE_BASE64' (Settings > Secrets)."
                CodeSnippetTab.GRADLE_GROOVY -> "Para proyectos con Groovy DSL tradicional (Flutter / React Native / Android heredado)."
                CodeSnippetTab.APKSIGNER -> "Comandos oficiales para alinear con zipalign y firmar APKs con soporte de firma v1, v2 y v3."
                CodeSnippetTab.PEM_CERT -> "Certificado público X.509 en formato PEM para consolas de APIs y proveedores de autenticación."
            }

            Text(
                text = currentDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
                lineHeight = 16.sp
            )

            // Code Viewer Box
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
            ) {
                Text(
                    text = currentSnippet,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .padding(12.dp)
                        .horizontalScroll(rememberScrollState())
                )
            }

            // Copy Snippet Action Button
            Button(
                onClick = {
                    DetailsActionUtils.copyToClipboard(context, selectedSnippetTab.tabLabel, currentSnippet)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("copy_snippet_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Copiar ${selectedSnippetTab.tabLabel}")
            }
        }
    }
}
