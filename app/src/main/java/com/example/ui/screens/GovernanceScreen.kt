package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.config.DeploymentConfig
import com.example.repository.SafeSnapshot
import com.example.repository.TokenSnapshot
import com.example.ui.components.AddressRow
import com.example.ui.components.LoadingCard
import com.example.ui.components.NotDeployedCard
import com.example.ui.components.UnavailableCard
import com.example.ui.components.openUrl
import com.example.ui.theme.NegativeRed
import com.example.ui.theme.PositiveGreen
import com.example.ui.theme.WarningAmber
import com.example.viewmodel.LoadState

/**
 * Governance = the Safe multi-sig that owns DCDN.
 *
 * The app shows the live Safe configuration and links out to Safe{Wallet} for proposing, signing and
 * executing transactions. It never builds or signs Safe transactions itself: Safe{Wallet} already does
 * this securely (hardware wallet support, simulation, the owners' confirmation queue).
 *
 * Replaces the old screen, which showed hard-coded demo proposals against a Timelock that was never deployed.
 */
@Composable
fun GovernanceScreen(
    safeState: LoadState<SafeSnapshot>,
    tokenState: LoadState<TokenSnapshot>,
    modifier: Modifier = Modifier,
    network: DeploymentConfig.NetworkConfig = DeploymentConfig.currentNetwork,
    onRetry: () -> Unit = {}
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        item {
            Column {
                Text("Governance", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                Text(
                    "Safe multi-sig that owns the DCDN contract",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        when (safeState) {
            LoadState.Loading -> item { LoadingCard("Safe") }
            LoadState.NotDeployed -> item { NotDeployedCard(network.name) }
            is LoadState.Unavailable -> item { UnavailableCard("Safe data", safeState.reason, onRetry) }
            is LoadState.Ready -> {
                val safe = safeState.data
                item { SafeCard(safe, network) }
                item { OwnershipCheckCard(safe, tokenState) }
            }
        }

        if (network.ownerSafe != null) {
            item {
                Card(shape = RoundedCornerShape(16.dp), border = CardDefaults.outlinedCardBorder()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Propose & sign in Safe{Wallet}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Admin actions (add/remove minter, change dev fund, limits) are Safe transactions to the DCDN " +
                                "contract. Create them with Transaction Builder, then each owner signs in the queue.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        DeploymentConfig.safeHomeUrl(network)?.let { url ->
                            Button(onClick = { openUrl(context, url) }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Open Safe")
                            }
                        }
                        DeploymentConfig.safeQueueUrl(network)?.let { url ->
                            OutlinedButton(onClick = { openUrl(context, url) }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.PendingActions, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Pending transactions")
                            }
                        }
                        DeploymentConfig.safeTxBuilderUrl(network)?.let { url ->
                            OutlinedButton(onClick = { openUrl(context, url) }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Build, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("New transaction (Transaction Builder)")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SafeCard(safe: SafeSnapshot, network: DeploymentConfig.NetworkConfig) {
    Card(shape = RoundedCornerShape(16.dp), border = CardDefaults.outlinedCardBorder()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    "${safe.threshold}-of-${safe.owners.size} Safe",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text("v${safe.version} · nonce ${safe.nonce}", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (safe.threshold < 2) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Testnet setup: 1 signature can run admin actions. Mainnet must use the 3-of-5 hardware-key Safe.",
                    style = MaterialTheme.typography.bodySmall,
                    color = WarningAmber
                )
            }
            Spacer(Modifier.height(8.dp))
            AddressRow("Safe address", safe.address, DeploymentConfig.explorerAddressUrl(safe.address, network))
            safe.owners.forEachIndexed { i, owner ->
                AddressRow("Owner ${i + 1}", owner, DeploymentConfig.explorerAddressUrl(owner, network))
            }
        }
    }
}

@Composable
private fun OwnershipCheckCard(safe: SafeSnapshot, tokenState: LoadState<TokenSnapshot>) {
    val token = (tokenState as? LoadState.Ready)?.data ?: return
    val ownedBySafe = token.owner.equals(safe.address, ignoreCase = true)
    Card(shape = RoundedCornerShape(16.dp), border = CardDefaults.outlinedCardBorder()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (ownedBySafe) Icons.Default.VerifiedUser else Icons.Default.GppBad,
                contentDescription = null,
                tint = if (ownedBySafe) PositiveGreen else NegativeRed
            )
            Spacer(Modifier.width(12.dp))
            Text(
                if (ownedBySafe) "DCDN owner() is this Safe. Admin functions need Safe signatures."
                else "Warning: DCDN owner() is ${token.owner}, not this Safe.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
