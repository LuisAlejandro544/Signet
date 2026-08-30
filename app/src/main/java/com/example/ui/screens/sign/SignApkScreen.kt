package com.example.ui.screens.sign

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.KeystoreViewModel
import com.example.ui.state.ApkSigningUiState

@Composable
fun SignApkScreen(
    viewModel: KeystoreViewModel,
    onNavigateToInspectWithApk: (ByteArray, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val formState by viewModel.signApkFormState.collectAsState()
    val savedKeystores by viewModel.savedKeystores.collectAsState()
    val signingState by viewModel.apkSigningState.collectAsState()

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Firmar APK con Signet",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Firma cualquier APK usando tus Keystores guardados o un archivo externo (.jks, .p12, .keystore) con soporte multi-esquema v1 + v2 + v3 y optimización Zipalign.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // 1. Select APK
        SelectApkCard(
            formState = formState,
            onApkSelected = { name, size, bytes, ctx ->
                viewModel.selectApkForSigning(name, size, bytes, ctx)
            },
            onClearApk = {
                viewModel.clearSelectedApkForSigning()
            }
        )

        // 2. Select Keystore
        SelectKeystoreForSigningCard(
            formState = formState,
            savedKeystores = savedKeystores,
            onSelectSavedKeystore = { ks ->
                viewModel.selectSavedKeystoreForSigning(ks)
            },
            onSetExternalKeystore = { name, bytes ->
                viewModel.setExternalKeystoreForSigning(name, bytes)
            },
            onUpdateForm = { transform ->
                viewModel.updateSignApkForm(transform)
            }
        )

        // 3. Signing & Zipalign Options
        SigningOptionsCard(
            formState = formState,
            onUpdateForm = { transform ->
                viewModel.updateSignApkForm(transform)
            }
        )

        // Sign Button (disabled while signing)
        val isSigning = signingState is ApkSigningUiState.Signing
        val canSign = formState.apkBytes != null && !isSigning

        Button(
            onClick = { viewModel.signApk(context) },
            enabled = canSign,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("btn_start_sign_apk")
        ) {
            Icon(
                imageVector = Icons.Default.Draw,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isSigning) "Firmando APK..." else "Firmar y Optimizar APK",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // 4. Progress / Error / Result
        SigningResultCard(
            signingState = signingState,
            onInstallApk = { file ->
                viewModel.installSignedApk(context, file)
            },
            onInspectApk = { bytes, name ->
                onNavigateToInspectWithApk(bytes, name)
            },
            onReset = {
                viewModel.resetSigningState()
            }
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}
