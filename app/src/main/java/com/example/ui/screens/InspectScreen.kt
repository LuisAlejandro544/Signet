package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.Icon
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.KeystoreViewModel
import com.example.ui.screens.inspect.ApkMatcherSection
import com.example.ui.screens.inspect.KeystoreInspectorSection

@Composable
fun InspectScreen(
    viewModel: KeystoreViewModel,
    modifier: Modifier = Modifier
) {
    var activeSubTab by remember { mutableIntStateOf(0) } // 0: Inspeccionar Keystore, 1: Validar APK vs Keystore

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Mode Selector Tab
        TabRow(
            selectedTabIndex = activeSubTab,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = activeSubTab == 0,
                onClick = { activeSubTab = 0 },
                text = { Text("Inspeccionar Keystore") },
                icon = { Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = activeSubTab == 1,
                onClick = { activeSubTab = 1 },
                text = { Text("APK vs Keystore") },
                icon = { Icon(Icons.Default.Android, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        if (activeSubTab == 0) {
            KeystoreInspectorSection(viewModel = viewModel)
        } else {
            ApkMatcherSection(viewModel = viewModel)
        }
    }
}
