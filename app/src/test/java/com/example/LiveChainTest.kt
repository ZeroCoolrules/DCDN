package com.example

import com.example.config.DeploymentConfig
import com.example.repository.ExplorerRepository
import com.example.repository.WalletRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal

/**
 * Hits the real Base Sepolia deployment. Skipped by default; run with
 *   LIVE_CHAIN=1 gradle :app:testDebugUnitTest --tests com.example.LiveChainTest
 * Robolectric is used so org.json (Android framework) is available on the JVM.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LiveChainTest {

    private val net = DeploymentConfig.BASE_SEPOLIA

    @Before
    fun onlyWhenRequested() = assumeTrue(System.getenv("LIVE_CHAIN") == "1")

    @Test
    fun readsTokenAndSafe() = runBlocking {
        val repo = WalletRepository(net)
        val token = repo.loadTokenSnapshot().getOrThrow()
        assertEquals("DCDN", token.symbol)
        assertEquals(0, BigDecimal("21000000").compareTo(token.maxSupply))
        assertTrue(token.owner.equals(net.ownerSafe, ignoreCase = true))

        val safe = repo.loadSafeSnapshot().getOrThrow()
        assertTrue(safe.threshold >= 1)
        assertTrue(safe.owners.isNotEmpty())
        println("LIVE token=$token")
        println("LIVE safe=$safe")
    }

    @Test
    fun readsEventHistory() = runBlocking {
        val events = ExplorerRepository(net).loadEvents().getOrThrow()
        assertTrue(events.any { it.title == "Mint" })
        assertTrue(events.any { it.title == "Ownership transferred" })
        events.forEach { println("LIVE event=$it") }
    }
}
