package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.config.DeploymentConfig
import com.example.security.BiometricAuthManager
import com.example.viewmodel.GovernanceProposal
import com.example.viewmodel.GovernanceViewModel
import com.example.viewmodel.ProposalStatus

@Composable
fun GovernanceScreen(
    modifier: Modifier = Modifier,
    viewModel: GovernanceViewModel = viewModel(),
    activity: FragmentActivity? = null
) {
    val context = LocalContext.current
    val currentActivity = activity ?: (context as? FragmentActivity)
    val biometricManager = remember(currentActivity) {
        currentActivity?.let { BiometricAuthManager(it) }
    }

    val proposals by viewModel.proposals.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val actionMessage by viewModel.actionMessage.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var newTarget by remember { mutableStateOf(DeploymentConfig.OPERATIONS_SAFE_ADDRESS) }
    var newCalldata by remember { mutableStateOf("0x") }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(actionMessage) {
        actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Proposal")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Screen Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        "Governance & Timelock",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "48-Hour OpenZeppelin Timelock Controller",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        "48h Delay",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Timelock Controller Overview Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.HourglassTop,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Timelock: ${DeploymentConfig.TIMELOCK_CONTROLLER_ADDRESS.take(10)}...${DeploymentConfig.TIMELOCK_CONTROLLER_ADDRESS.takeLast(6)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Guarantees community review period prior to Safe execution",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Active Proposals (${proposals.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(proposals) { proposal ->
                    ProposalItemCard(
                        proposal = proposal,
                        onExecute = {
                            // Require biometric authorization before executing ready proposal
                            if (biometricManager != null) {
                                biometricManager.authenticate(
                                    title = "Authorize Proposal Execution",
                                    subtitle = proposal.title,
                                    description = "Biometrics required to trigger Timelock execute()."
                                ) { result ->
                                    if (result is BiometricAuthManager.AuthResult.Success) {
                                        viewModel.executeProposal(proposal.id)
                                    }
                                }
                            } else {
                                viewModel.executeProposal(proposal.id)
                            }
                        }
                    )
                }
            }
        }
    }

    // Schedule Proposal Dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Schedule Timelock Proposal") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Proposal Title / Action") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newTarget,
                        onValueChange = { newTarget = it },
                        label = { Text("Target Safe / Contract") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newCalldata,
                        onValueChange = { newCalldata = it },
                        label = { Text("Encoded Calldata (Hex)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        "Note: Scheduling enters a mandatory 48-hour timelock delay before hardware execution.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            // Biometric verification before proposal scheduling
                            if (biometricManager != null) {
                                biometricManager.authenticate(
                                    title = "Authorize Proposal Scheduling",
                                    subtitle = newTitle,
                                    description = "Verify identity to schedule on-chain Timelock proposal."
                                ) { result ->
                                    if (result is BiometricAuthManager.AuthResult.Success) {
                                        viewModel.initiateProposal(
                                            title = newTitle,
                                            target = newTarget,
                                            dataHex = newCalldata
                                        )
                                        showCreateDialog = false
                                        newTitle = ""
                                    }
                                }
                            } else {
                                viewModel.initiateProposal(
                                    title = newTitle,
                                    target = newTarget,
                                    dataHex = newCalldata
                                )
                                showCreateDialog = false
                                newTitle = ""
                            }
                        }
                    }
                ) {
                    Text("Verify & Schedule")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ProposalItemCard(
    proposal: GovernanceProposal,
    onExecute: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    proposal.id,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                // Status chip
                val (chipBg, chipText, statusLabel) = when (proposal.status) {
                    ProposalStatus.READY_FOR_EXECUTION -> Triple(
                        Color(0xFF00F5A0).copy(alpha = 0.18f),
                        Color(0xFF00F5A0),
                        "Ready to Execute"
                    )
                    ProposalStatus.SCHEDULED_PENDING_DELAY -> Triple(
                        Color(0xFFF59E0B).copy(alpha = 0.18f),
                        Color(0xFFF59E0B),
                        "Pending 48h Delay"
                    )
                    ProposalStatus.EXECUTED -> Triple(
                        Color(0xFF00E5FF).copy(alpha = 0.18f),
                        Color(0xFF00E5FF),
                        "Executed"
                    )
                    ProposalStatus.CANCELLED -> Triple(
                        Color(0xFFF43F5E).copy(alpha = 0.18f),
                        Color(0xFFF43F5E),
                        "Cancelled"
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = chipBg
                ) {
                    Text(
                        statusLabel,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = chipText,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                proposal.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                proposal.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Target: ${proposal.targetAddress.take(8)}...${proposal.targetAddress.takeLast(4)}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    "Signers: ${proposal.approvalsReceived}/${proposal.approvalsRequired}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (proposal.status == ProposalStatus.READY_FOR_EXECUTION) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onExecute,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Authorize & Execute (Biometrics)")
                }
            }
        }
    }
}
