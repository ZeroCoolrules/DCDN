package com.example.repository

import com.example.config.DeploymentConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.web3j.crypto.Keys
import org.json.JSONObject
import java.math.BigDecimal
import java.math.BigInteger
import java.util.concurrent.TimeUnit

enum class EventCategory { TRANSFER, MINING, ADMIN }

/** One real on-chain event emitted by the DCDN contract. */
data class ChainEvent(
    val txHash: String,
    val blockNumber: Long,
    val logIndex: Int,
    /** ISO-8601 UTC timestamp from the explorer, e.g. 2026-09-26T15:56:54.000000Z */
    val timestamp: String?,
    val category: EventCategory,
    val title: String,
    val from: String? = null,
    val to: String? = null,
    val amount: BigDecimal? = null
)

/**
 * Event history for the DCDN contract, fetched from the Blockscout API (no API key).
 *
 * Public RPCs limit eth_getLogs to ~1,000 blocks (Base produces a block every 2s), so scanning from the
 * deploy block via RPC is impractical. Blockscout returns every log of the contract with pagination.
 * Events are decoded locally from topic0, so this works even while the source is unverified.
 */
class ExplorerRepository(
    private val network: DeploymentConfig.NetworkConfig = DeploymentConfig.currentNetwork,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {

    suspend fun loadEvents(maxPages: Int = 10): Result<List<ChainEvent>> = withContext(Dispatchers.IO) {
        runCatching {
            val token = network.dcdnToken ?: throw IllegalStateException("DCDN is not deployed on ${network.name}")
            val events = mutableListOf<ChainEvent>()
            var nextPage: JSONObject? = null
            var page = 0
            do {
                val urlBuilder = "${network.blockscoutApiUrl}/addresses/$token/logs".toHttpUrl().newBuilder()
                nextPage?.let { params -> params.keys().forEach { k -> urlBuilder.addQueryParameter(k, params.get(k).toString()) } }
                val request = Request.Builder().url(urlBuilder.build()).header("Accept", "application/json").build()
                val body = client.newCall(request).execute().use { resp ->
                    if (!resp.isSuccessful) throw RuntimeException("Explorer returned HTTP ${resp.code}")
                    resp.body?.string() ?: throw RuntimeException("Explorer returned an empty body")
                }
                val json = JSONObject(body)
                val items = json.getJSONArray("items")
                for (i in 0 until items.length()) decode(items.getJSONObject(i))?.let { events.add(it) }
                nextPage = json.optJSONObject("next_page_params")
                page++
            } while (nextPage != null && page < maxPages)
            events.sortedWith(compareByDescending<ChainEvent> { it.blockNumber }.thenByDescending { it.logIndex })
        }
    }

    private fun decode(log: JSONObject): ChainEvent? {
        val topicsJson = log.getJSONArray("topics")
        val topics = (0 until topicsJson.length()).mapNotNull { topicsJson.optString(it).takeIf { t -> t.isNotEmpty() && t != "null" } }
        if (topics.isEmpty()) return null
        val data = log.optString("data", "0x").removePrefix("0x")
        val base = ChainEvent(
            txHash = log.getString("transaction_hash"),
            blockNumber = log.getLong("block_number"),
            logIndex = log.optInt("index"),
            timestamp = log.optString("block_timestamp").takeIf { it.isNotEmpty() && it != "null" },
            category = EventCategory.ADMIN,
            title = "Unknown event"
        )
        return when (topics[0].lowercase()) {
            TOPIC_TRANSFER -> {
                val from = topicAddress(topics.getOrNull(1))
                val to = topicAddress(topics.getOrNull(2))
                val title = when {
                    from.equals(ZERO_ADDRESS) -> "Mint"
                    to.equals(ZERO_ADDRESS) -> "Burn"
                    else -> "Transfer"
                }
                base.copy(category = EventCategory.TRANSFER, title = title, from = from, to = to, amount = word(data, 0)?.let { WalletRepository.toTokens(it) })
            }
            TOPIC_BLOCK_MINED -> base.copy(
                category = EventCategory.MINING, title = "Block mined",
                to = topicAddress(topics.getOrNull(1)), amount = word(data, 0)?.let { WalletRepository.toTokens(it) }
            )
            TOPIC_REWARDS_LOCKED -> base.copy(
                category = EventCategory.MINING, title = "Rewards locked (30d)",
                to = topicAddress(topics.getOrNull(1)), amount = word(data, 0)?.let { WalletRepository.toTokens(it) }
            )
            TOPIC_REWARDS_CLAIMED -> base.copy(
                category = EventCategory.MINING, title = "Locked rewards claimed",
                to = topicAddress(topics.getOrNull(1)), amount = word(data, 0)?.let { WalletRepository.toTokens(it) }
            )
            TOPIC_HALVING -> base.copy(category = EventCategory.MINING, title = "Halving", amount = word(data, 0)?.let { WalletRepository.toTokens(it) })
            TOPIC_OWNERSHIP_TRANSFERRED -> base.copy(
                title = "Ownership transferred", from = topicAddress(topics.getOrNull(1)), to = topicAddress(topics.getOrNull(2))
            )
            TOPIC_MINTER_ADDED -> base.copy(title = "Minter added", to = word(data, 0)?.let { wordAddress(it) })
            TOPIC_MINTER_REMOVED -> base.copy(title = "Minter removed", to = word(data, 0)?.let { wordAddress(it) })
            TOPIC_DEV_FUND_UPDATED -> base.copy(title = "Dev fund changed", to = word(data, 0)?.let { wordAddress(it) })
            TOPIC_MINTING_RENOUNCED -> base.copy(title = "Minting renounced", from = topicAddress(topics.getOrNull(1)))
            else -> base.copy(title = "Event ${topics[0].take(10)}…")
        }
    }

    companion object {
        const val ZERO_ADDRESS = "0x0000000000000000000000000000000000000000"

        // keccak256 of the event signatures in contracts/DreamCoin.sol
        const val TOPIC_TRANSFER = "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef"
        const val TOPIC_OWNERSHIP_TRANSFERRED = "0x8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e0"
        const val TOPIC_MINTER_ADDED = "0x6ae172837ea30b801fbfcdd4108aa1d5bf8ff775444fd70256b44e6bf3dfc3f6"
        const val TOPIC_MINTER_REMOVED = "0xe94479a9f7e1952cc78f2d6baab678adc1b772d936c6583def489e524cb66692"
        const val TOPIC_DEV_FUND_UPDATED = "0x76238c711356e13e6b0a381b5c1103783d65515e5fc402cbd2266925fdddb3ef"
        const val TOPIC_MINTING_RENOUNCED = "0xd5807edd20c8010ed67c4c3a417b1ae7c0e00178bc973020ff7b10b71722a543"
        const val TOPIC_BLOCK_MINED = "0xfa067c77ed46ee3b681f9953a2c5a911a1c9a609235a10948d6387ae03a2497e"
        const val TOPIC_REWARDS_LOCKED = "0xb4603da56f779dc7d5a4cac192483dfd64ab453c0a4bb04c2b3964ca67d5a11d"
        const val TOPIC_REWARDS_CLAIMED = "0xfc30cddea38e2bf4d6ea7d3f9ed3b6ad7f176419f4963bd81318067a4aee73fe"
        const val TOPIC_HALVING = "0xde141711fef892cdaea55f1f6a85e241382fcefc69efdb496eff235992a18ed4"

        internal fun topicAddress(topic: String?): String? =
            topic?.removePrefix("0x")?.takeLast(40)?.let { Keys.toChecksumAddress("0x$it") }

        internal fun word(dataHex: String, index: Int): BigInteger? {
            val start = index * 64
            if (dataHex.length < start + 64) return null
            return BigInteger(dataHex.substring(start, start + 64), 16)
        }

        internal fun wordAddress(word: BigInteger): String = Keys.toChecksumAddress("0x" + word.toString(16).padStart(40, '0'))
    }
}
