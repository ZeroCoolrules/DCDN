package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TokenAllocation
import com.example.ui.theme.*

// Rich color palette for allocation segments
val AllocationColors = listOf(
    Color(0xFF00E5FF), // Cyan (Node Operators - 40%)
    Color(0xFF00F5A0), // Mint (Ecosystem - 15%)
    Color(0xFF8B5CF6), // Violet (Core Team - 15%)
    Color(0xFF38BDF8), // Sky Blue (Treasury - 10%)
    Color(0xFFF59E0B), // Amber (Strategic Backers - 10%)
    Color(0xFFEC4899), // Pink (Public Sale - 5%)
    Color(0xFF14B8A6)  // Teal (DEX Liquidity - 5%)
)

@Composable
fun TokenAllocationScreen(modifier: Modifier = Modifier) {
    val allocations = remember {
        listOf(
            TokenAllocation("Node Operators", 40.0, "400M", "60mo Decaying", "Bandwidth Mining & Edge Relays"),
            TokenAllocation("Ecosystem Grants", 15.0, "150M", "36mo Linear", "Developer Grants & Hackathons"),
            TokenAllocation("Core Team", 15.0, "150M", "36mo Linear", "Core Contributor Locking (1yr cliff)"),
            TokenAllocation("Protocol Treasury", 10.0, "100M", "48mo Linear", "Timelock Controlled Reserves"),
            TokenAllocation("Strategic Backers", 10.0, "100M", "18mo Linear", "Capital Backing & Seed Investors"),
            TokenAllocation("Public Sale", 5.0, "50M", "3mo Vesting", "Community IDO Distribution"),
            TokenAllocation("DEX Liquidity", 5.0, "50M", "Immediate", "Uniswap v3 Protocol Owned Liquidity")
        )
    }

    var selectedIndex by remember { mutableIntStateOf(-1) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Tokenomics & Allocation",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        "DCDN Genesis Token Distribution & Vesting",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "1,000,000,000 DCDN",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // High Level Metrics Row (Circulating vs Locked)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Circulating Supply", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("245.0M DCDN", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MintSecondary)
                        Text("24.5% in circulation", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Locked in Escrow", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("755.0M DCDN", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CyanPrimary)
                        Text("75.5% timelock/vesting", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Interactive Donut Chart
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Supply Breakdown Visualization",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .size(220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 36.dp.toPx()
                            val chartSize = size.minDimension - strokeWidth
                            val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

                            var currentStartAngle = -90f
                            allocations.forEachIndexed { index, alloc ->
                                val sweepAngle = (alloc.allocationPercent / 100.0 * 360f).toFloat()
                                val isSelected = selectedIndex == index
                                val color = AllocationColors[index % AllocationColors.size]

                                drawArc(
                                    color = if (selectedIndex == -1 || isSelected) color else color.copy(alpha = 0.3f),
                                    startAngle = currentStartAngle,
                                    sweepAngle = sweepAngle - 2f, // slight gap
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = Size(chartSize, chartSize),
                                    style = Stroke(
                                        width = if (isSelected) strokeWidth * 1.15f else strokeWidth,
                                        cap = StrokeCap.Round
                                    )
                                )
                                currentStartAngle += sweepAngle
                            }
                        }

                        // Central Hub Stat
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (selectedIndex >= 0 && selectedIndex < allocations.size) {
                                val selected = allocations[selectedIndex]
                                Text(
                                    "${selected.allocationPercent}%",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = AllocationColors[selectedIndex % AllocationColors.size]
                                )
                                Text(
                                    selected.totalTokens,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    selected.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            } else {
                                Text(
                                    "1.0 Billion",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = CyanPrimary
                                )
                                Text(
                                    "Total Max Supply",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "$300M FDV",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MintSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Horizontal Quick-Filter Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(allocations.indices.toList()) { idx ->
                            val alloc = allocations[idx]
                            val isSelected = selectedIndex == idx
                            val color = AllocationColors[idx % AllocationColors.size]

                            Surface(
                                selected = isSelected,
                                onClick = {
                                    selectedIndex = if (isSelected) -1 else idx
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (isSelected) BorderStroke(1.dp, color) else null
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "${alloc.category} (${alloc.allocationPercent}%)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Category Cards List
        item {
            Text(
                "Allocation Pools & Vesting Schedules",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(allocations.indices.toList()) { index ->
            val allocation = allocations[index]
            val isSelected = selectedIndex == index
            val color = AllocationColors[index % AllocationColors.size]

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        selectedIndex = if (isSelected) -1 else index
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) color.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                ),
                border = if (isSelected) BorderStroke(1.5.dp, color) else CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                allocation.category,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = color.copy(alpha = 0.15f)
                        ) {
                            Text(
                                "${allocation.allocationPercent}%",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = color
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Tokens: ${allocation.totalTokens} DCDN",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Schedule: ${allocation.vestingSchedule}",
                            style = MaterialTheme.typography.bodySmall,
                            color = CyanPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Progress Bar representing %
                    LinearProgressIndicator(
                        progress = { (allocation.allocationPercent / 40.0).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = color,
                        trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        allocation.purpose,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
