package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.config.DeploymentConfig
import com.example.repository.AddressLookup
import com.example.repository.ChainEvent
import com.example.repository.ExplorerRepository
import com.example.repository.SafeSnapshot
import com.example.repository.TokenSnapshot
import com.example.repository.WalletRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Load state for a piece of on-chain data. There is no "fallback" state: data is either real or unavailable. */
sealed interface LoadState<out T> {
    data object Loading : LoadState<Nothing>
    data class Ready<T>(val data: T) : LoadState<T>
    data class Unavailable(val reason: String) : LoadState<Nothing>
    data object NotDeployed : LoadState<Nothing>
}

/**
 * Single source of truth for every screen. Shared at activity scope so all tabs show the same snapshot.
 * Replaces the old GovernanceViewModel, which held hard-coded demo proposals.
 */
class TreasuryViewModel(
    private val network: DeploymentConfig.NetworkConfig = DeploymentConfig.currentNetwork,
    private val chain: WalletRepository = WalletRepository(network),
    private val explorer: ExplorerRepository = ExplorerRepository(network)
) : ViewModel() {

    private val _token = MutableStateFlow<LoadState<TokenSnapshot>>(LoadState.Loading)
    val token: StateFlow<LoadState<TokenSnapshot>> = _token.asStateFlow()

    private val _safe = MutableStateFlow<LoadState<SafeSnapshot>>(LoadState.Loading)
    val safe: StateFlow<LoadState<SafeSnapshot>> = _safe.asStateFlow()

    private val _events = MutableStateFlow<LoadState<List<ChainEvent>>>(LoadState.Loading)
    val events: StateFlow<LoadState<List<ChainEvent>>> = _events.asStateFlow()

    private val _lookup = MutableStateFlow<LoadState<AddressLookup>?>(null)
    val lookup: StateFlow<LoadState<AddressLookup>?> = _lookup.asStateFlow()

    val networkConfig: DeploymentConfig.NetworkConfig get() = network

    init {
        refresh()
    }

    fun refresh() {
        if (!network.isDeployed) {
            _token.value = LoadState.NotDeployed
            _safe.value = LoadState.NotDeployed
            _events.value = LoadState.NotDeployed
            return
        }
        _token.value = LoadState.Loading
        _safe.value = LoadState.Loading
        _events.value = LoadState.Loading
        viewModelScope.launch { _token.value = chain.loadTokenSnapshot().toLoadState() }
        viewModelScope.launch { _safe.value = chain.loadSafeSnapshot().toLoadState() }
        viewModelScope.launch { _events.value = explorer.loadEvents().toLoadState() }
    }

    fun lookupAddress(address: String) {
        val trimmed = address.trim()
        if (!WalletRepository.isValidAddress(trimmed)) {
            _lookup.value = LoadState.Unavailable("Enter a full 0x address (42 characters).")
            return
        }
        _lookup.value = LoadState.Loading
        val owners = (_safe.value as? LoadState.Ready)?.data?.owners.orEmpty()
        viewModelScope.launch { _lookup.value = chain.lookupAddress(trimmed, owners).toLoadState() }
    }

    fun clearLookup() {
        _lookup.value = null
    }

    private fun <T> Result<T>.toLoadState(): LoadState<T> = fold(
        onSuccess = { LoadState.Ready(it) },
        onFailure = { LoadState.Unavailable(it.message ?: it::class.java.simpleName) }
    )
}
