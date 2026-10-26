package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.config.DeploymentConfig
import com.example.repository.AddressLookup
import com.example.ui.components.LoadingCard
import com.example.ui.components.NotDeployedCard
import com.example.ui.components.UnavailableCard
import com.example.ui.components.formatTokens
import com.example.ui.components.openUrl
import com.example.ui.theme.NegativeRed
import com.example.ui.theme.PositiveGreen
import com.example.viewmodel.LoadState

/**
 * Read-only wallet lookup: paste any address to see its DCDN / ETH balance and its role in the deployment.
 *
 * Replaces the old "Hardware Key" screen. That screen's scan button never registered an NFC reader, so the
 * only working path was a simulated handshake that returned a hard-coded signer address and signature, and
 * the biometric step auto-approved when no activity was available. Treasury signing happens in Safe{Wallet},
 * which supports Ledger/Trezor directly. APDU helpers remain in security/NfcProtocolConstants.kt.
 */
@Composable
fun WalletLookupScreen(
    lookupState: LoadState<AddressLookup>?,
    onLookup: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    network: DeploymentConfig.NetworkConfig = DeploymentConfig.currentNetwork
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    var input by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        item {
            Column {
                Text("Wallet Lookup", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                Text(
                    "Check any address on ${network.name}. Read-only: this app never asks for keys.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (!network.isDeployed) {
            item { NotDeployedCard(network.name) }
            return@LazyColumn
        }

        item {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it.trim() },
                label = { Text("Address (0x…)") },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onLookup(input) }),
                trailingIcon = {
                    IconButton(onClick = { clipboard.getText()?.text?.trim()?.let { input = it } }) {
                        Icon(Icons.Default.ContentPaste, contentDescription = "Paste")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onLookup(input) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Look up")
                }
                OutlinedButton(onClick = { input = ""; onClear() }) { Text("Clear") }
            }
        }

        when (lookupState) {
            null -> Unit
            LoadState.Loading -> item { LoadingCard("address") }
            LoadState.NotDeployed -> item { NotDeployedCard(network.name) }
            is LoadState.Unavailable -> item { UnavailableCard("Lookup", lookupState.reason) }
            is LoadState.Ready -> item {
                val r = lookupState.data
                Card(shape = RoundedCornerShape(16.dp), border = CardDefaults.outlinedCardBorder()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(r.address, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                        Text("${formatTokens(r.dcdnBalance)} DCDN", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("${r.ethBalance.toPlainString()} ETH", style = MaterialTheme.typography.bodyMedium)
                        HorizontalDivider(Modifier.padding(vertical = 4.dp))
                        FlagRow("Contract account", r.isContract)
                        FlagRow("Safe owner (can sign admin actions)", r.isSafeOwner)
                        FlagRow("Authorized minter", r.isMinter)
                        FlagRow("Excluded from 1% max-wallet limit", r.isExcludedFromLimits)
                        TextButton(onClick = { openUrl(context, DeploymentConfig.explorerAddressUrl(r.address, network)) }) {
                            Text("View on explorer")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlagRow(label: String, value: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (value) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = null,
            tint = if (value) PositiveGreen else NegativeRed.copy(alpha = 0.6f),
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}
