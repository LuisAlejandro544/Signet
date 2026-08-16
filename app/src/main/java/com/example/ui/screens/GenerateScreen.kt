package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.GenerationUiState
import com.example.ui.KeystoreViewModel
import com.example.ui.screens.generate.GeneratePresetsSection
import com.example.ui.screens.generate.KeystoreCredentialsForm
import com.example.ui.screens.generate.KeystoreDnFields
import com.example.ui.screens.generate.KeystoreValiditySection

@Composable
fun GenerateScreen(
    viewModel: KeystoreViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val formState by viewModel.formState.collectAsState()
    val generationState by viewModel.generationState.collectAsState()

    val isGenerating = generationState is GenerationUiState.Generating

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Hero / Header Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Column {
                    Text(
                        text = "Generador de Keystores",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Crea claves criptográficas estándar (.jks / .keystore) para firmar tus APKs de Android y distribuirlas en Uptodown, web o tiendas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Error message banner if any
        if (generationState is GenerationUiState.Error) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = (generationState as GenerationUiState.Error).message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // Section: Quick Presets
        GeneratePresetsSection(
            formState = formState,
            onApplyPreset = { presetKey -> viewModel.applyPreset(presetKey) }
        )

        // Section 1: File & Credentials Form
        KeystoreCredentialsForm(
            formState = formState,
            onSetFileExtension = { ext -> viewModel.setFileExtension(ext) },
            onFileNameChange = { name -> viewModel.updateForm { it.copy(fileName = name) } },
            onStorePasswordChange = { newPwd ->
                viewModel.updateForm { s ->
                    s.copy(
                        storePassword = newPwd,
                        confirmPassword = if (s.storePassword == s.confirmPassword) newPwd else s.confirmPassword
                    )
                }
            },
            onConfirmPasswordChange = { confirm -> viewModel.updateForm { it.copy(confirmPassword = confirm) } },
            onToggleStorePasswordVisibility = {
                viewModel.updateForm { it.copy(isStorePasswordVisible = !it.isStorePasswordVisible) }
            },
            onGenerateRandomPassword = { length -> viewModel.generateRandomPassword(length) },
            onAliasChange = { alias -> viewModel.updateForm { it.copy(alias = alias) } },
            onToggleUseSamePassword = { checked -> viewModel.updateForm { it.copy(useSamePassword = checked) } },
            onKeyPasswordChange = { keyPwd -> viewModel.updateForm { it.copy(keyPassword = keyPwd) } },
            onToggleKeyPasswordVisibility = {
                viewModel.updateForm { it.copy(isKeyPasswordVisible = !it.isKeyPasswordVisible) }
            }
        )

        // Section 2: Cryptography & Validity Slider
        KeystoreValiditySection(
            formState = formState,
            onValidityYearsChange = { years -> viewModel.setValidityYears(years) },
            onAlgorithmChange = { alg -> viewModel.updateForm { it.copy(algorithm = alg) } }
        )

        // Section 3: Distinguished Name (X.500) Fields
        KeystoreDnFields(
            formState = formState,
            onToggleExpand = { viewModel.updateForm { it.copy(isAdvancedDnExpanded = !it.isAdvancedDnExpanded) } },
            onCommonNameChange = { cn -> viewModel.updateForm { it.copy(commonName = cn) } },
            onOrganizationChange = { org -> viewModel.updateForm { it.copy(organization = org) } },
            onOrganizationalUnitChange = { ou -> viewModel.updateForm { it.copy(organizationalUnit = ou) } },
            onLocalityChange = { loc -> viewModel.updateForm { it.copy(locality = loc) } },
            onStateChange = { st -> viewModel.updateForm { it.copy(state = st) } },
            onCountryCodeChange = { code -> viewModel.updateForm { it.copy(countryCode = code) } }
        )

        // Submit / Generate Button
        Button(
            onClick = {
                viewModel.generateKeystore(context)
            },
            enabled = !isGenerating,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("generate_keystore_submit_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isGenerating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.5.dp
                )
                Spacer(Modifier.width(12.dp))
                Text("Generando Claves y Certificado...", fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("Generar Archivo Keystore", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}
