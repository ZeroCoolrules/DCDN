package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.config.DeploymentConfig
import com.example.repository.TokenSnapshot
import com.example.ui.components.AddressRow
import com.example.ui.components.LoadingCard
import com.example.ui.components.NotDeployedCard
import com.example.ui.components.UnavailableCard
import com.example.ui.components.formatTokens
import com.example.ui.components.formatTokensCompact
import com.example.ui.components.percentOf
import com.example.ui.theme.*
import com.example.viewmodel.LoadState
import java.math.BigDecimal

/** One slice of MAX_SUPPLY, computed from live balances. */
data class AllocationSlice(
    val label: String,
    val amount: BigDecimal,
    val description: String,
    val color: Color,
    val address: String? = null
)

/**
 * Splits MAX_SUPPLY into slices that add up exactly:
 * treasury + dev fund + owner Safe + other holders = totalSupply, and
 * totalSupply + pendingLockedRewards + remainingSupply = MAX_SUPPLY.
 */
fun buildAllocationSlices(s: TokenSnapshot): List<AllocationSlice> = listOfNotNull(
    AllocationSlice("Treasury", s.treasuryBalance, "Pre-mine held by the treasury wallet", CyanPrimary, s.treasury),
    AllocationSlice("Dev fund", s.devFundBalance, "2% share of mining rewards", VioletTertiary, s.devFund),
    s.ownerSafeBalance?.takeIf { it.signum() > 0 }?.let {
        AllocationSlice("Owner Safe", it, "Held by the multi-sig that owns the contract", InfoBlue, s.owner)
    },
    AllocationSlice("Other holders", s.otherHoldersBalance, "Circulating in all other wallets", MintSecondary),
    AllocationSlice("Locked rewards", s.pendingLockedRewards, "Mining rewards reserved for the 30-day lock", WarningAmber),
    AllocationSlice("Not yet mined", s.remainingSupply, "Mintable headroom left under MAX_SUPPLY", Color(0xFF475569))
)

@Composable
fun TokenAllocationScreen(
    state: LoadState<TokenSnapshot>,
    modifier: Modifier = Modifier,
    network: DeploymentConfig.NetworkConfig = DeploymentConfig.currentNetwork,
    onRetry: () -> Unit = {}
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        item {
            Column {
                Text("Supply & Allocation", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                Text(
                    "Live from the DCDN contract on ${network.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        when (state) {
            LoadState.Loading -> item { LoadingCard("token supply") }
            LoadState.NotDeployed -> item { NotDeployedCard(network.name) }
            is LoadState.Unavailable -> item { UnavailableCard("Token data", state.reason, onRetry) }
            is LoadState.Ready -> allocationContent(state.data, network)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.allocationContent(
    s: TokenSnapshot,
    network: DeploymentConfig.NetworkConfig
) {
    val slices = buildAllocationSlices(s)

    item {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard("Minted", "${formatTokensCompact(s.totalSupply)} ${s.symbol}",
                "%.2f%% of max supply".format(percentOf(s.totalSupply, s.maxSupply)), MintSecondary, Modifier.weight(1f))
            MetricCard("Burned", "${formatTokensCompact(s.totalBurned)} ${s.symbol}",
                "1% burn on transfers", NegativeRed, Modifier.weight(1f))
        }
    }

    item {
        var selectedIndex by remember { mutableIntStateOf(-1) }
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Max supply breakdown", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start)
                )
                Spacer(Modifier.height(16.dp))
                Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        val strokeWidth = 32.dp.toPx()
                        val chartSize = size.minDimension - strokeWidth
                        val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                        var start = -90f
                        slices.forEachIndexed { index, slice ->
                            val sweep = (percentOf(slice.amount, s.maxSupply) / 100.0 * 360.0).toFloat()
                            if (sweep <= 0f) return@forEachIndexed
                            val dim = selectedIndex != -1 && selectedIndex != index
                            drawArc(
                                color = if (dim) slice.color.copy(alpha = 0.25f) else slice.color,
                                startAngle = start,
                                sweepAngle = (sweep - 1.5f).coerceAtLeast(0.5f),
                                useCenter = false,
                                topLeft = topLeft,
                                size = Size(chartSize, chartSize),
                                style = Stroke(width = if (selectedIndex == index) strokeWidth * 1.15f else strokeWidth, cap = StrokeCap.Butt)
                            )
                            start += sweep
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val sel = slices.getOrNull(selectedIndex)
                        if (sel != null) {
                            Text("%.2f%%".format(percentOf(sel.amount, s.maxSupply)), style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold, color = sel.color)
                            Text(formatTokensCompact(sel.amount), style = MaterialTheme.typography.labelMedium)
                            Text(sel.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Text(formatTokensCompact(s.maxSupply), style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold, color = CyanPrimary)
                            Text("Max supply (${s.symbol})", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                slices.forEachIndexed { index, slice ->
                    val isSelected = selectedIndex == index
                    Surface(
                        onClick = { selectedIndex = if (isSelected) -1 else index },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) slice.color.copy(alpha = 0.15f) else Color.Transparent,
                        border = if (isSelected) BorderStroke(1.dp, slice.color) else null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(slice.color))
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(slice.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(slice.description, style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(formatTokens(slice.amount), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                Text("%.2f%%".format(percentOf(slice.amount, s.maxSupply)), style = MaterialTheme.typography.labelSmall,
                                    color = slice.color)
                            }
                        }
                    }
                }
            }
        }
    }

    item {
        Card(shape = RoundedCornerShape(16.dp), border = CardDefaults.outlinedCardBorder()) {
            Column(Modifier.padding(16.dp)) {
                Text("Contract", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${s.name} (${s.symbol}) · minting ${if (s.mintingRenounced) "renounced" else "active"}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                network.dcdnToken?.let { AddressRow("Token", it, DeploymentConfig.explorerTokenUrl(network)) }
                AddressRow("Owner", s.owner, DeploymentConfig.explorerAddressUrl(s.owner, network))
                AddressRow("Treasury", s.treasury, DeploymentConfig.explorerAddressUrl(s.treasury, network))
                AddressRow("Dev fund", s.devFund, DeploymentConfig.explorerAddressUrl(s.devFund, network))
            }
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, caption: String, accent: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = accent)
            Text(caption, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
