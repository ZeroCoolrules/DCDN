package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.config.DeploymentConfig
import com.example.ui.components.RechartsTokenFlowView
import com.example.ui.components.defaultHistoricalFlowData
import com.example.ui.theme.*

data class TokenFlowEvent(
    val id: String,
    val txHash: String,
    val title: String,
    val poolCategory: String,
    val amountDcdn: Double,
    val isOutflow: Boolean,
    val timestamp: String,
    val destination: String,
    val description: String,
    val network: String = "Arbitrum One"
)

enum class ChartEngine {
    RECHARTS,
    NATIVE_CANVAS
}

@Composable
fun TreasuryHistoryScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var selectedEngine by remember { mutableStateOf(ChartEngine.RECHARTS) }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedEventForDetail by remember { mutableStateOf<TokenFlowEvent?>(null) }
    val categories = listOf("All", "Emissions", "Operations", "Vesting", "Liquidity")

    val historyEvents = remember {
        listOf(
            TokenFlowEvent(
                id = "FLOW-104",
                txHash = "0x89abf19c3b8892147ef6b2668912e",
                title = "Quarterly Bandwidth Mining Payout",
                poolCategory = "Emissions",
                amountDcdn = 20000000.0,
                isOutflow = true,
                timestamp = "Sep 22, 2026 - 14:32 UTC",
                destination = DeploymentConfig.MINING_REWARDS_CONTRACT_ADDRESS,
                description = "Disbursed to 4,280 active edge nodes across 62 countries on Arbitrum."
            ),
            TokenFlowEvent(
                id = "FLOW-103",
                txHash = "0x44cd8912ea5091ff7820a400991a",
                title = "Operations Safe Tranche Replenishment",
                poolCategory = "Operations",
                amountDcdn = 500000.0,
                isOutflow = true,
                timestamp = "Sep 15, 2026 - 09:15 UTC",
                destination = DeploymentConfig.OPERATIONS_SAFE_ADDRESS,
                description = "Authorized 2-of-3 Safe transfer for global multi-cloud RPC relay nodes."
            ),
            TokenFlowEvent(
                id = "FLOW-102",
                txHash = "0x12fa9022bb76402377a0651ce887",
                title = "Team Vesting Wallet Unlock (Tranche #2)",
                poolCategory = "Vesting",
                amountDcdn = 4166666.0,
                isOutflow = true,
                timestamp = "Aug 30, 2026 - 18:00 UTC",
                destination = DeploymentConfig.TEAM_VESTING_VAULT_ADDRESS,
                description = "OpenZeppelin VestingWallet automatic release after cliff verification."
            ),
            TokenFlowEvent(
                id = "FLOW-101",
                txHash = "0x91bba107cf2289c0993077e5a240",
                title = "Protocol Inflow: Relay Network Licensing Fees",
                poolCategory = "Operations",
                amountDcdn = 1850000.0,
                isOutflow = false,
                timestamp = "Aug 12, 2026 - 22:45 UTC",
                destination = DeploymentConfig.TREASURY_COLD_SAFE_ADDRESS,
                description = "Commercial CDN enterprise license settlement routed into Cold Safe."
            ),
            TokenFlowEvent(
                id = "FLOW-100",
                txHash = "0x33ee01928045fbc4009800ff",
                title = "AMM Initial Liquidity Pool Seeding",
                poolCategory = "Liquidity",
                amountDcdn = 50000000.0,
                isOutflow = true,
                timestamp = "Jul 01, 2026 - 00:00 UTC",
                destination = DeploymentConfig.LIQUIDITY_SAFE_ADDRESS,
                description = "Paired with 15,000 ETH on Uniswap v3 full-range liquidity position."
            )
        )
    }

    val filteredEvents = remember(selectedCategory) {
        if (selectedCategory == "All") historyEvents
        else historyEvents.filter { it.poolCategory == selectedCategory }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        // Screen Header with Live Network Tag
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Treasury Flow Trends",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        "On-Chain Reserves & Disbursal Analytics",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(PositiveGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Arbitrum One",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }

        // Summary Metric Cards (Reserves & Quarterly Flow)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Total Reserves",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "423.5M DCDN",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = CyanPrimary
                        )
                        Text(
                            "~$127.05M USD (Floor: 400M)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(PositiveGreenContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = PositiveGreen
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Q3 Flow Delta",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "-22.8M DCDN",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = NegativeRed
                        )
                        Text(
                            "+18.5M Inflow / 41.3M Outflow",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Engine Toggle (Recharts vs Compose Canvas)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    selected = selectedEngine == ChartEngine.RECHARTS,
                    onClick = { selectedEngine = ChartEngine.RECHARTS },
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedEngine == ChartEngine.RECHARTS) MaterialTheme.colorScheme.primary else Color.Transparent,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.BarChart,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (selectedEngine == ChartEngine.RECHARTS) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Recharts Library",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedEngine == ChartEngine.RECHARTS) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    selected = selectedEngine == ChartEngine.NATIVE_CANVAS,
                    onClick = { selectedEngine = ChartEngine.NATIVE_CANVAS },
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedEngine == ChartEngine.NATIVE_CANVAS) MaterialTheme.colorScheme.primary else Color.Transparent,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ShowChart,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (selectedEngine == ChartEngine.NATIVE_CANVAS) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Compose Canvas",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedEngine == ChartEngine.NATIVE_CANVAS) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Active Visualization Component
        item {
            when (selectedEngine) {
                ChartEngine.RECHARTS -> {
                    RechartsTokenFlowView(
                        onDataPointSelected = { pt ->
                            Toast.makeText(context, "${pt.month}: ${pt.reserves}M DCDN (${pt.note})", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                ChartEngine.NATIVE_CANVAS -> {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.tertiaryContainer,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            "COMPOSE CANVAS",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Reserve Trajectory Curve",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    "2026 Trend",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyanPrimary
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            TreasuryFlowCanvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Jul (Genesis: 500M)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Aug (Vesting: 425M)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Sep (Current: 423.5M)", style = MaterialTheme.typography.labelSmall, color = CyanPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Category Filter Chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Historical Treasury Events",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }
        }

        // Filtered Token Flow Event Cards
        items(filteredEvents) { event ->
            FlowEventCard(
                event = event,
                onCardClick = { selectedEventForDetail = event },
                onCopyTx = {
                    clipboardManager.setText(AnnotatedString(event.txHash))
                    Toast.makeText(context, "Copied Tx Hash to clipboard", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    // Event Detail Dialog
    selectedEventForDetail?.let { event ->
        AlertDialog(
            onDismissRequest = { selectedEventForDetail = null },
            icon = {
                Icon(
                    if (event.isOutflow) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = if (event.isOutflow) NegativeRed else PositiveGreen
                )
            },
            title = {
                Text(event.title, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        color = if (event.isOutflow) NegativeRedContainer.copy(alpha = 0.3f) else PositiveGreenContainer.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${if (event.isOutflow) "Disbursal Outflow: -" else "Protocol Inflow: +"}${String.format("%,.0f", event.amountDcdn)} DCDN",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (event.isOutflow) NegativeRed else PositiveGreen
                        )
                    }

                    Text(
                        event.description,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Divider(color = MaterialTheme.colorScheme.outlineVariant)

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Category: ${event.poolCategory}", style = MaterialTheme.typography.labelMedium)
                        Text("Timestamp: ${event.timestamp}", style = MaterialTheme.typography.labelMedium)
                        Text("Network: ${event.network}", style = MaterialTheme.typography.labelMedium)
                        Text("Target: ${event.destination}", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                        Text("Tx Hash: ${event.txHash}", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = CyanPrimary)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { selectedEventForDetail = null }) {
                    Text("Close")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(event.txHash))
                        Toast.makeText(context, "Copied Tx Hash", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Copy Tx")
                }
            }
        )
    }
}

@Composable
fun TreasuryFlowCanvas(modifier: Modifier = Modifier) {
    val primaryColor = CyanPrimary
    val outlineColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Background grid lines
        val lineCount = 4
        for (i in 0..lineCount) {
            val y = height * (i / lineCount.toFloat())
            drawLine(
                color = outlineColor.copy(alpha = 0.4f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
        }

        // Curve data points normalized (0..1)
        val points = listOf(
            Offset(0f, height * 0.15f),
            Offset(width * 0.25f, height * 0.35f),
            Offset(width * 0.50f, height * 0.30f),
            Offset(width * 0.75f, height * 0.55f),
            Offset(width, height * 0.48f)
        )

        // Draw filled area under curve
        val fillPath = Path().apply {
            moveTo(0f, height)
            lineTo(points[0].x, points[0].y)
            for (i in 1 until points.size) {
                val prev = points[i - 1]
                val curr = points[i]
                val cX = (prev.x + curr.x) / 2f
                cubicTo(cX, prev.y, cX, curr.y, curr.x, curr.y)
            }
            lineTo(width, height)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(primaryColor.copy(alpha = 0.35f), Color.Transparent),
                startY = 0f,
                endY = height
            )
        )

        // Draw line curve
        val strokePath = Path().apply {
            moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size) {
                val prev = points[i - 1]
                val curr = points[i]
                val cX = (prev.x + curr.x) / 2f
                cubicTo(cX, prev.y, cX, curr.y, curr.x, curr.y)
            }
        }

        drawPath(
            path = strokePath,
            color = primaryColor,
            style = Stroke(width = 3.dp.toPx())
        )

        // Draw data points with glow
        points.forEach { pt ->
            drawCircle(
                color = primaryColor.copy(alpha = 0.3f),
                radius = 8.dp.toPx(),
                center = pt
            )
            drawCircle(
                color = Color.White,
                radius = 5.dp.toPx(),
                center = pt
            )
            drawCircle(
                color = primaryColor,
                radius = 3.5.dp.toPx(),
                center = pt
            )
        }
    }
}

@Composable
fun FlowEventCard(
    event: TokenFlowEvent,
    onCardClick: () -> Unit,
    onCopyTx: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (event.isOutflow) NegativeRedContainer.copy(alpha = 0.25f)
                        else PositiveGreenContainer.copy(alpha = 0.25f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (event.isOutflow) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = if (event.isOutflow) NegativeRed else PositiveGreen,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        event.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${if (event.isOutflow) "-" else "+"}${String.format("%,.0f", event.amountDcdn)} DCDN",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (event.isOutflow) NegativeRed else PositiveGreen
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    event.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        event.timestamp,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onCopyTx() }
                    ) {
                        Text(
                            "Tx: ${event.txHash.take(6)}...${event.txHash.takeLast(4)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = CyanPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy Hash",
                            modifier = Modifier.size(12.dp),
                            tint = CyanPrimary
                        )
                    }
                }
            }
        }
    }
}
