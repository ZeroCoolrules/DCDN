package com.example.config

/**
 * Deployment configuration for the DCDN (Dreamcadian) token.
 *
 * Every address in here is a real, verified on-chain deployment. Networks where DCDN is not
 * deployed yet have `dcdnToken = null`, and the UI shows "not deployed" instead of inventing data.
 *
 * History: the previous version of this file listed Arbitrum One / Ethereum / Sepolia addresses that
 * were not DCDN contracts at all (several were well-known third-party mainnet contracts, e.g. the
 * Uniswap V2 router, USDC, LINK and DAI). They were removed so nobody can send funds to them by mistake.
 */
object DeploymentConfig {

    data class NetworkConfig(
        val name: String,
        val chainId: Long,
        /** Public JSON-RPC endpoint (read-only calls only; the app never signs transactions). */
        val rpcUrl: String,
        /** Block explorer used for tx / address links. */
        val explorerUrl: String,
        /** Blockscout API v2 base URL, used for event history (no API key required). */
        val blockscoutApiUrl: String,
        /** Safe{Wallet} network prefix, e.g. "basesep" -> app.safe.global/home?safe=basesep:0x... */
        val safeNetworkPrefix: String,
        /** DCDN ERC-20 contract, or null if not deployed on this network. */
        val dcdnToken: String?,
        /** Safe multi-sig that owns the DCDN contract (the only account allowed to run admin functions). */
        val ownerSafe: String?,
        /** Token treasury (receives the 10% pre-mine). */
        val treasury: String?,
        /** Dev fund (receives the 2% dev share of mining rewards). */
        val devFund: String?,
        /** Block the token was deployed in. */
        val deployBlock: Long?
    ) {
        val isDeployed: Boolean get() = dcdnToken != null
    }

    /**
     * Base Sepolia testnet deployment, done 2026-09-26 with Hardhat Ignition.
     * Owner moved to the Safe on 2026-09-30 (tx 0x75c7c2c2bff82d47735b429b162d5f00b99f588cbb5299a0dacf6f28ab146d0c).
     */
    val BASE_SEPOLIA = NetworkConfig(
        name = "Base Sepolia",
        chainId = 84532L,
        rpcUrl = "https://sepolia.base.org",
        explorerUrl = "https://sepolia.basescan.org",
        blockscoutApiUrl = "https://base-sepolia.blockscout.com/api/v2",
        safeNetworkPrefix = "basesep",
        dcdnToken = "0x0602074976325a455BA3Fc53fD43294C5a3ABd66",
        ownerSafe = "0xd8cb95124fbc5611Ad834ac3efDff85048FA0724",
        treasury = "0x375CEf117533803cB287ad80Ddca179117e63BEE",
        devFund = "0x391F652bc3c948dBf7Bde95591eB9Dac13B18ea1",
        deployBlock = 47334963L
    )

    /**
     * Base mainnet. Not deployed yet: mainnet requires the external audit and the 3-of-5 Safe
     * (see GO-LIVE-RUNBOOK phases 3–5). Fill these in only from the real mainnet deployment.
     */
    val BASE_MAINNET = NetworkConfig(
        name = "Base",
        chainId = 8453L,
        rpcUrl = "https://mainnet.base.org",
        explorerUrl = "https://basescan.org",
        blockscoutApiUrl = "https://base.blockscout.com/api/v2",
        safeNetworkPrefix = "base",
        dcdnToken = null,
        ownerSafe = null,
        treasury = null,
        devFund = null,
        deployBlock = null
    )

    val ALL_NETWORKS = listOf(BASE_SEPOLIA, BASE_MAINNET)

    /** Active network. Base Sepolia until mainnet is live. */
    var currentNetwork: NetworkConfig = BASE_SEPOLIA

    const val SAFE_APP_URL = "https://app.safe.global"

    fun safeHomeUrl(net: NetworkConfig = currentNetwork): String? =
        net.ownerSafe?.let { "$SAFE_APP_URL/home?safe=${net.safeNetworkPrefix}:$it" }

    fun safeQueueUrl(net: NetworkConfig = currentNetwork): String? =
        net.ownerSafe?.let { "$SAFE_APP_URL/transactions/queue?safe=${net.safeNetworkPrefix}:$it" }

    fun safeTxBuilderUrl(net: NetworkConfig = currentNetwork): String? =
        net.ownerSafe?.let {
            "$SAFE_APP_URL/apps/open?safe=${net.safeNetworkPrefix}:$it&appUrl=https%3A%2F%2Fapps-portal.safe.global%2Ftx-builder"
        }

    fun explorerTxUrl(txHash: String, net: NetworkConfig = currentNetwork) = "${net.explorerUrl}/tx/$txHash"
    fun explorerAddressUrl(address: String, net: NetworkConfig = currentNetwork) = "${net.explorerUrl}/address/$address"
    fun explorerTokenUrl(net: NetworkConfig = currentNetwork) = net.dcdnToken?.let { "${net.explorerUrl}/token/$it" }
}
