package com.example.ui.components.main

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import com.example.data.model.KeystoreDetails
import com.example.ui.KeystoreViewModel
import com.example.ui.components.KeystoreDetailsSheet
import com.example.ui.components.UpdateDialog
import com.example.update.UpdateUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Host composable for global modal dialogs and bottom sheets (Keystore details & App updates).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainGlobalSheets(
    viewModel: KeystoreViewModel,
    selectedKeystoreForDetail: KeystoreDetails?,
    updateState: UpdateUiState,
    sheetState: SheetState,
    scope: CoroutineScope
) {
    // Modal details sheet
    selectedKeystoreForDetail?.let { details ->
        KeystoreDetailsSheet(
            details = details,
            sheetState = sheetState,
            onDismiss = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    viewModel.dismissKeystoreDetails()
                }
            }
        )
    }

    // Modal Update notification and download dialog
    (updateState as? UpdateUiState.Available)?.let { availableState ->
        UpdateDialog(
            viewModel = viewModel,
            updateState = availableState,
            onDismiss = {
                viewModel.dismissUpdate()
            }
        )
    }
}
