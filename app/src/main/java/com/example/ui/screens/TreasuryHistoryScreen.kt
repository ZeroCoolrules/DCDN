package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.config.DeploymentConfig
import com.example.repository.ChainEvent
import com.example.repository.EventCategory
import com.example.ui.components.LoadingCard
import com.example.ui.components.NotDeployedCard
import com.example.ui.components.UnavailableCard
import com.example.ui.components.formatTokens
import com.example.ui.components.openUrl
import com.example.ui.components.shortAddress
import com.example.ui.theme.*
import com.example.viewmodel.LoadState

/**
 * Real event history of the DCDN contract (transfers, mints, burns, mining, admin changes).
 * Replaces the old screen, which showed invented payouts and a chart of made-up monthly reserves.
 */
@Composable
fun TreasuryHistoryScreen(
    state: LoadState<List<ChainEvent>>,
    modifier: Modifier = Modifier,
    network: DeploymentConfig.NetworkConfig = DeploymentConfig.currentNetwork,
    onRetry: () -> Unit = {}
) {
    val context = LocalContext.current
    var filter by remember { mutableStateOf<EventCategory?>(null) }
    val filters = listOf<Pair<String, EventCategory?>>(
        "All" to null,
        "Transfers" to EventCategory.TRANSFER,
        "Mining" to EventCategory.MINING,
        "Admin" to EventCategory.ADMIN
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        item {
            Column {
                Text("On-chain History", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                Text(
                    "Every event emitted by the DCDN contract on ${network.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filters) { (label, category) ->
                    FilterChip(selected = filter == category, onClick = { filter = category }, label = { Text(label) })
                }
            }
        }

        when (state) {
            LoadState.Loading -> item { LoadingCard("event history") }
            LoadState.NotDeployed -> item { NotDeployedCard(network.name) }
            is LoadState.Unavailable -> item { UnavailableCard("Event history", state.reason, onRetry) }
            is LoadState.Ready -> {
                val shown = state.data.filter { filter == null || it.category == filter }
                if (shown.isEmpty()) {
                    item {
                        Text("No events in this category yet.", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    item {
                        Text("${shown.size} event${if (shown.size == 1) "" else "s"}", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    items(shown, key = { "${it.txHash}-${it.logIndex}" }) { event ->
                        EventCard(event, onOpen = { openUrl(context, DeploymentConfig.explorerTxUrl(event.txHash, network)) })
                    }
                }
            }
        }
    }
}

@Composable
private fun EventCard(event: ChainEvent, onOpen: () -> Unit) {
    val (icon, tint) = when {
        event.title == "Mint" -> Icons.Default.AddCircle to PositiveGreen
        event.title == "Burn" -> Icons.Default.LocalFireDepartment to NegativeRed
        event.category == EventCategory.TRANSFER -> Icons.Default.SwapHoriz to CyanPrimary
        event.category == EventCategory.MINING -> Icons.Default.Memory to MintSecondary
        else -> Icons.Default.AdminPanelSettings to VioletTertiary
    }
    Card(
        onClick = onOpen,
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = tint)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(event.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    event.amount?.let {
                        Text("${formatTokens(it)} DCDN", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = tint)
                    }
                }
                val parties = listOfNotNull(
                    event.from?.takeIf { it != com.example.repository.ExplorerRepository.ZERO_ADDRESS }?.let { "from ${shortAddress(it)}" },
                    event.to?.takeIf { it != com.example.repository.ExplorerRepository.ZERO_ADDRESS }?.let { "to ${shortAddress(it)}" }
                ).joinToString("  ")
                if (parties.isNotEmpty()) {
                    Text(parties, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                }
                Text(
                    listOfNotNull(event.timestamp?.let(::formatTimestamp), "block ${event.blockNumber}", "tx ${shortAddress(event.txHash)}")
                        .joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = "Open in explorer", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** "2026-09-26T15:56:54.000000Z" -> "2026-09-26 15:56 UTC" */
internal fun formatTimestamp(iso: String): String =
    if (iso.length >= 16) "${iso.substring(0, 10)} ${iso.substring(11, 16)} UTC" else iso
