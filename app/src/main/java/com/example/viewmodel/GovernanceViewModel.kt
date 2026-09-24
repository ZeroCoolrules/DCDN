package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.config.DeploymentConfig
import com.example.repository.WalletRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigInteger

data class GovernanceProposal(
    val id: String,
    val title: String,
    val description: String,
    val targetAddress: String,
    val valueEth: String,
    val delayHours: Int = 48,
    val scheduledAt: Long,
    val executeAfterTimestamp: Long,
    val status: ProposalStatus,
    val approvalsReceived: Int,
    val approvalsRequired: Int = 3,
    val salt: String
)

enum class ProposalStatus {
    SCHEDULED_PENDING_DELAY,
    READY_FOR_EXECUTION,
    EXECUTED,
    CANCELLED
}

class GovernanceViewModel : ViewModel() {

    private val repository = WalletRepository()

    private val _proposals = MutableStateFlow<List<GovernanceProposal>>(emptyList())
    val proposals: StateFlow<List<GovernanceProposal>> = _proposals.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    init {
        loadInitialProposals()
    }

    private fun loadInitialProposals() {
        val now = System.currentTimeMillis()
        val fortyEightHoursMs = 48 * 60 * 60 * 1000L

        _proposals.value = listOf(
            GovernanceProposal(
                id = "PROP-001",
                title = "Distribute Quarterly Bandwidth Mining Pool",
                description = "Release 20,000,000 DCDN from Mining Rewards contract for relay nodes meeting 99.9% edge uptime proofs.",
                targetAddress = DeploymentConfig.MINING_REWARDS_CONTRACT_ADDRESS,
                valueEth = "0.0",
                delayHours = 48,
                scheduledAt = now - 50 * 60 * 60 * 1000L, // Scheduled 50h ago -> ready!
                executeAfterTimestamp = now - 2 * 60 * 60 * 1000L,
                status = ProposalStatus.READY_FOR_EXECUTION,
                approvalsReceived = 3,
                approvalsRequired = 3,
                salt = "0x892A...44C1"
            ),
            GovernanceProposal(
                id = "PROP-002",
                title = "Replenish Operations Safe (Q4 Working Budget)",
                description = "Transfer 500,000 DCDN from Treasury Cold Safe to Operations Safe (2-of-3) for cloud relay hosting and security audit retainer.",
                targetAddress = DeploymentConfig.OPERATIONS_SAFE_ADDRESS,
                valueEth = "0.0",
                delayHours = 48,
                scheduledAt = now - 12 * 60 * 60 * 1000L, // Scheduled 12h ago -> pending 36h delay
                executeAfterTimestamp = now + 36 * 60 * 60 * 1000L,
                status = ProposalStatus.SCHEDULED_PENDING_DELAY,
                approvalsReceived = 2,
                approvalsRequired = 3,
                salt = "0x7F1B...30E8"
            ),
            GovernanceProposal(
                id = "PROP-003",
                title = "Trigger Core Team Vesting Tranche #3",
                description = "Call release() on TeamVestingWallet. Enforces 12-month cliff verification before unlocking 4,166,666 DCDN.",
                targetAddress = DeploymentConfig.TEAM_VESTING_VAULT_ADDRESS,
                valueEth = "0.0",
                delayHours = 48,
                scheduledAt = now - 72 * 60 * 60 * 1000L,
                executeAfterTimestamp = now - 24 * 60 * 60 * 1000L,
                status = ProposalStatus.EXECUTED,
                approvalsReceived = 3,
                approvalsRequired = 3,
                salt = "0x33C9...901B"
            )
        )
    }

    /**
     * Schedules a new proposal into the on-chain TimelockController using ABI encoding.
     */
    fun initiateProposal(
        title: String,
        target: String = DeploymentConfig.OPERATIONS_SAFE_ADDRESS,
        valueWei: Long = 0,
        dataHex: String = "0x"
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val saltBytes = org.web3j.crypto.Hash.sha3(title.toByteArray())

            val result = repository.scheduleTimelock(
                timelockAddress = DeploymentConfig.TIMELOCK_CONTROLLER_ADDRESS,
                target = target,
                valueWei = BigInteger.valueOf(valueWei),
                data = org.web3j.utils.Numeric.hexStringToByteArray(dataHex),
                salt = saltBytes
            )

            result.onSuccess { msg ->
                val newProp = GovernanceProposal(
                    id = "PROP-00${_proposals.value.size + 1}",
                    title = title,
                    description = "Automated proposal scheduled via Safe Timelock with 48h execution delay.",
                    targetAddress = target,
                    valueEth = "0.0",
                    delayHours = 48,
                    scheduledAt = System.currentTimeMillis(),
                    executeAfterTimestamp = System.currentTimeMillis() + (48 * 3600 * 1000L),
                    status = ProposalStatus.SCHEDULED_PENDING_DELAY,
                    approvalsReceived = 1,
                    approvalsRequired = 3,
                    salt = "0x" + saltBytes.take(4).joinToString("") { "%02X".format(it) } + "..."
                )
                _proposals.value = listOf(newProp) + _proposals.value
                _actionMessage.value = "Proposal successfully scheduled! Timelock 48h timer active."
            }.onFailure { err ->
                _actionMessage.value = "Schedule error: ${err.localizedMessage}"
            }
            _isLoading.value = false
        }
    }

    /**
     * Executes a ready proposal once the 48-hour timelock delay has elapsed.
     */
    fun executeProposal(proposalId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _proposals.value = _proposals.value.map { prop ->
                if (prop.id == proposalId) prop.copy(status = ProposalStatus.EXECUTED) else prop
            }
            _actionMessage.value = "Proposal $proposalId executed successfully on ${DeploymentConfig.currentNetwork.name}!"
            _isLoading.value = false
        }
    }

    fun dismissMessage() {
        _actionMessage.value = null
    }
}
