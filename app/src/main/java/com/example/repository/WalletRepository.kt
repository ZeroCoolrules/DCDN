package com.example.repository

import com.example.config.DeploymentConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Bool
import org.web3j.abi.datatypes.DynamicArray
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Type
import org.web3j.abi.datatypes.Utf8String
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.crypto.Keys
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.http.HttpService
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

/** Live DCDN token state read from the chain. All token amounts are in whole DCDN (18 decimals applied). */
data class TokenSnapshot(
    val name: String,
    val symbol: String,
    val maxSupply: BigDecimal,
    val totalSupply: BigDecimal,
    val totalBurned: BigDecimal,
    val remainingSupply: BigDecimal,
    val pendingLockedRewards: BigDecimal,
    val owner: String,
    val treasury: String,
    val devFund: String,
    val mintingRenounced: Boolean,
    val treasuryBalance: BigDecimal,
    val devFundBalance: BigDecimal,
    val ownerSafeBalance: BigDecimal?
) {
    /** Tokens held by everyone other than treasury / dev fund / owner Safe. */
    val otherHoldersBalance: BigDecimal
        get() = (totalSupply - treasuryBalance - devFundBalance - (ownerSafeBalance ?: BigDecimal.ZERO))
            .max(BigDecimal.ZERO)
}

/** Live Safe multi-sig state. */
data class SafeSnapshot(
    val address: String,
    val owners: List<String>,
    val threshold: Int,
    val nonce: BigInteger,
    val version: String
)

/** Result of looking up an arbitrary address against the DCDN deployment. */
data class AddressLookup(
    val address: String,
    val dcdnBalance: BigDecimal,
    val ethBalance: BigDecimal,
    val isMinter: Boolean,
    val isExcludedFromLimits: Boolean,
    val isSafeOwner: Boolean,
    val isContract: Boolean
)

/**
 * Read-only chain access for the DCDN token and its owner Safe.
 *
 * This app deliberately never holds a private key and never sends transactions. Admin actions on DCDN
 * must be proposed and signed in Safe{Wallet} by the Safe owners (see DeploymentConfig.safeQueueUrl).
 *
 * Every call returns a [Result]. On failure the UI shows "unavailable"; there are no fallback or
 * placeholder numbers anywhere in this class.
 */
class WalletRepository(
    private val network: DeploymentConfig.NetworkConfig = DeploymentConfig.currentNetwork
) {
    private val web3j: Web3j = Web3j.build(HttpService(network.rpcUrl))

    private fun requireToken(): String =
        network.dcdnToken ?: throw IllegalStateException("DCDN is not deployed on ${network.name}")

    // ------------------------------------------------------------------
    // Low-level eth_call helper
    // ------------------------------------------------------------------

    private fun call(to: String, name: String, inputs: List<Type<*>>, outputs: List<TypeReference<*>>): List<Type<*>> {
        @Suppress("UNCHECKED_CAST")
        val function = Function(name, inputs, outputs as List<TypeReference<Type<*>>>)
        val response = web3j.ethCall(
            Transaction.createEthCallTransaction(null, to, FunctionEncoder.encode(function)),
            DefaultBlockParameterName.LATEST
        ).send()
        if (response.hasError()) throw RuntimeException("$name() reverted: ${response.error.message}")
        if (response.isReverted) throw RuntimeException("$name() reverted: ${response.revertReason}")
        @Suppress("UNCHECKED_CAST")
        val decoded = FunctionReturnDecoder.decode(response.value, function.outputParameters as List<TypeReference<Type<*>>>)
        if (decoded.size != outputs.size) throw RuntimeException("$name() returned no data from $to")
        return decoded
    }

    private fun uint(to: String, name: String, vararg args: Type<*>): BigInteger =
        (call(to, name, args.toList(), listOf(object : TypeReference<Uint256>() {}))[0] as Uint256).value

    private fun address(to: String, name: String): String =
        Keys.toChecksumAddress((call(to, name, emptyList(), listOf(object : TypeReference<Address>() {}))[0] as Address).value)

    private fun bool(to: String, name: String, vararg args: Type<*>): Boolean =
        (call(to, name, args.toList(), listOf(object : TypeReference<Bool>() {}))[0] as Bool).value

    private fun string(to: String, name: String): String =
        (call(to, name, emptyList(), listOf(object : TypeReference<Utf8String>() {}))[0] as Utf8String).value

    private fun balanceOf(token: String, account: String): BigDecimal =
        toTokens(uint(token, "balanceOf", Address(account)))

    // ------------------------------------------------------------------
    // DCDN token
    // ------------------------------------------------------------------

    suspend fun loadTokenSnapshot(): Result<TokenSnapshot> = withContext(Dispatchers.IO) {
        runCatching {
            val token = requireToken()
            coroutineScope {
                val name = async { string(token, "name") }
                val symbol = async { string(token, "symbol") }
                val max = async { toTokens(uint(token, "MAX_SUPPLY")) }
                val supply = async { toTokens(uint(token, "totalSupply")) }
                val burned = async { toTokens(uint(token, "totalBurned")) }
                val remaining = async { toTokens(uint(token, "remainingSupply")) }
                val locked = async { toTokens(uint(token, "pendingLockedRewards")) }
                val owner = async { address(token, "owner") }
                val treasury = async { address(token, "treasury") }
                val devFund = async { address(token, "devFund") }
                val renounced = async { bool(token, "mintingRenounced") }

                val treasuryAddr = treasury.await()
                val devFundAddr = devFund.await()
                val safe = network.ownerSafe
                TokenSnapshot(
                    name = name.await(),
                    symbol = symbol.await(),
                    maxSupply = max.await(),
                    totalSupply = supply.await(),
                    totalBurned = burned.await(),
                    remainingSupply = remaining.await(),
                    pendingLockedRewards = locked.await(),
                    owner = owner.await(),
                    treasury = treasuryAddr,
                    devFund = devFundAddr,
                    mintingRenounced = renounced.await(),
                    treasuryBalance = balanceOf(token, treasuryAddr),
                    devFundBalance = balanceOf(token, devFundAddr),
                    ownerSafeBalance = safe?.let { balanceOf(token, it) }
                )
            }
        }
    }

    suspend fun lookupAddress(account: String, safeOwners: List<String>): Result<AddressLookup> = withContext(Dispatchers.IO) {
        runCatching {
            val token = requireToken()
            require(isValidAddress(account)) { "Not a valid 0x address" }
            val eth = web3j.ethGetBalance(account, DefaultBlockParameterName.LATEST).send()
            if (eth.hasError()) throw RuntimeException(eth.error.message)
            val code = web3j.ethGetCode(account, DefaultBlockParameterName.LATEST).send()
            if (code.hasError()) throw RuntimeException(code.error.message)
            AddressLookup(
                address = account,
                dcdnBalance = balanceOf(token, account),
                ethBalance = BigDecimal(eth.balance).divide(WEI, 6, RoundingMode.DOWN).stripTrailingZeros(),
                isMinter = bool(token, "isMinter", Address(account)),
                isExcludedFromLimits = bool(token, "isExcludedFromLimits", Address(account)),
                isSafeOwner = safeOwners.any { it.equals(account, ignoreCase = true) },
                isContract = code.code != null && code.code != "0x"
            )
        }
    }

    // ------------------------------------------------------------------
    // Safe multi-sig (read-only)
    // ------------------------------------------------------------------

    suspend fun loadSafeSnapshot(safeAddress: String? = network.ownerSafe): Result<SafeSnapshot> = withContext(Dispatchers.IO) {
        runCatching {
            val safe = safeAddress ?: throw IllegalStateException("No owner Safe configured for ${network.name}")
            @Suppress("UNCHECKED_CAST")
            val owners = (call(safe, "getOwners", emptyList(), listOf(object : TypeReference<DynamicArray<Address>>() {}))[0]
                as DynamicArray<Address>).value.map { Keys.toChecksumAddress(it.value) }
            SafeSnapshot(
                address = safe,
                owners = owners,
                threshold = uint(safe, "getThreshold").toInt(),
                nonce = uint(safe, "nonce"),
                version = string(safe, "VERSION")
            )
        }
    }

    companion object {
        private val WEI = BigDecimal.TEN.pow(18)

        fun toTokens(raw: BigInteger): BigDecimal = BigDecimal(raw).divide(WEI)

        fun isValidAddress(value: String): Boolean = Regex("^0x[0-9a-fA-F]{40}$").matches(value.trim())
    }
}
