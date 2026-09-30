package com.example

import com.example.config.DeploymentConfig
import com.example.repository.ExplorerRepository
import com.example.repository.TokenSnapshot
import com.example.repository.WalletRepository
import com.example.security.NfcProtocolConstants
import com.example.ui.screens.buildAllocationSlices
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger

class ExampleUnitTest {

    @Test
    fun testNfcSelectAppletApdu() {
        val selectApdu = NfcProtocolConstants.buildSelectAppletApdu()
        assertNotNull(selectApdu)
        assertTrue(selectApdu.size > 5)
        assertEquals(0x00.toByte(), selectApdu[0]) // CLA
        assertEquals(0xA4.toByte(), selectApdu[1]) // INS_SELECT
        assertEquals(0x04.toByte(), selectApdu[2]) // P1
    }

    @Test
    fun testNfcStatusWordExtraction() {
        val successResponse = byteArrayOf(0x01, 0x02, 0x90.toByte(), 0x00.toByte())
        assertEquals(0x9000, NfcProtocolConstants.extractStatusWord(successResponse))
        val errorResponse = byteArrayOf(0x69.toByte(), 0x85.toByte())
        assertEquals(0x6985, NfcProtocolConstants.extractStatusWord(errorResponse))
    }

    @Test
    fun baseSepoliaConfigHasRealDeployment() {
        val net = DeploymentConfig.BASE_SEPOLIA
        assertEquals(84532L, net.chainId)
        assertEquals("0x0602074976325a455BA3Fc53fD43294C5a3ABd66", net.dcdnToken)
        assertEquals("0xd8cb95124fbc5611Ad834ac3efDff85048FA0724", net.ownerSafe)
        listOfNotNull(net.dcdnToken, net.ownerSafe, net.treasury, net.devFund).forEach {
            assertTrue("bad address $it", WalletRepository.isValidAddress(it))
        }
        assertEquals(DeploymentConfig.BASE_SEPOLIA, DeploymentConfig.currentNetwork)
    }

    @Test
    fun mainnetIsNotDeployedYet() {
        assertFalse(DeploymentConfig.BASE_MAINNET.isDeployed)
        assertNull(DeploymentConfig.safeHomeUrl(DeploymentConfig.BASE_MAINNET))
    }

    @Test
    fun safeUrlsUseNetworkPrefix() {
        assertEquals(
            "https://app.safe.global/home?safe=basesep:0xd8cb95124fbc5611Ad834ac3efDff85048FA0724",
            DeploymentConfig.safeHomeUrl(DeploymentConfig.BASE_SEPOLIA)
        )
    }

    @Test
    fun tokenAmountsUse18Decimals() {
        val raw = BigInteger("2100000000000000000000000")
        assertEquals(0, BigDecimal("2100000").compareTo(WalletRepository.toTokens(raw)))
    }

    @Test
    fun decodesTopicsAndDataWords() {
        assertEquals(
            "0x0Ec2924F933bbf4591157E7A500feCEf4e26653C",
            ExplorerRepository.topicAddress("0x0000000000000000000000000ec2924f933bbf4591157e7a500fecef4e26653c")
        )
        val data = "00000000000000000000000000000000000000000001bcb13a657b2638800000"
        assertEquals(BigInteger("2100000000000000000000000"), ExplorerRepository.word(data, 0))
        assertNull(ExplorerRepository.word(data, 1))
        assertEquals(
            "0x0Ec2924F933bbf4591157E7A500feCEf4e26653C",
            ExplorerRepository.wordAddress(BigInteger("0ec2924f933bbf4591157e7a500fecef4e26653c", 16))
        )
    }

    @Test
    fun allocationSlicesAddUpToMaxSupply() {
        val s = sampleSnapshot()
        val total = buildAllocationSlices(s).fold(BigDecimal.ZERO) { acc, sl -> acc + sl.amount }
        assertEquals(0, s.maxSupply.compareTo(total))
    }

    companion object {
        /** Mirrors the live Base Sepolia state on 2026-09-30 (2.1M pre-mine in treasury, nothing mined). */
        fun sampleSnapshot() = TokenSnapshot(
            name = "Dreamcadian", symbol = "DCDN",
            maxSupply = BigDecimal("21000000"), totalSupply = BigDecimal("2100000"),
            totalBurned = BigDecimal.ZERO, remainingSupply = BigDecimal("18900000"),
            pendingLockedRewards = BigDecimal.ZERO,
            owner = "0xd8cb95124fbc5611Ad834ac3efDff85048FA0724",
            treasury = "0x375CEf117533803cB287ad80Ddca179117e63BEE",
            devFund = "0x391F652bc3c948dBf7Bde95591eB9Dac13B18ea1",
            mintingRenounced = false,
            treasuryBalance = BigDecimal("2100000"), devFundBalance = BigDecimal.ZERO,
            ownerSafeBalance = BigDecimal.ZERO
        )
    }
}
