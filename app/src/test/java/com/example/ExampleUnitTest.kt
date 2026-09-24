package com.example

import com.example.config.DeploymentConfig
import com.example.repository.WalletRepository
import com.example.security.NfcProtocolConstants
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
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
        val sw = NfcProtocolConstants.extractStatusWord(successResponse)
        assertEquals(0x9000, sw)

        val errorResponse = byteArrayOf(0x69.toByte(), 0x85.toByte())
        val errSw = NfcProtocolConstants.extractStatusWord(errorResponse)
        assertEquals(0x6985, errSw)
    }

    @Test
    fun testNfcGetPublicKeyApdu() {
        val pubKeyApdu = NfcProtocolConstants.buildGetPublicKeyApdu()
        assertEquals(0xE0.toByte(), pubKeyApdu[0])
        assertEquals(0x02.toByte(), pubKeyApdu[1]) // INS_GET_PUBLIC_KEY
    }

    @Test
    fun testDeploymentConfigAddresses() {
        assertTrue(DeploymentConfig.TREASURY_COLD_SAFE_ADDRESS.startsWith("0x"))
        assertEquals(42, DeploymentConfig.TREASURY_COLD_SAFE_ADDRESS.length)
        assertTrue(DeploymentConfig.TIMELOCK_CONTROLLER_ADDRESS.startsWith("0x"))
        assertEquals(42, DeploymentConfig.TIMELOCK_CONTROLLER_ADDRESS.length)
        assertTrue(DeploymentConfig.OPERATIONS_SAFE_ADDRESS.startsWith("0x"))
        assertEquals(42, DeploymentConfig.OPERATIONS_SAFE_ADDRESS.length)
        assertTrue(DeploymentConfig.DCDN_TOKEN_ADDRESS.startsWith("0x"))
        assertEquals(42, DeploymentConfig.DCDN_TOKEN_ADDRESS.length)
    }

    @Test
    fun testSafeCalldataGeneration() = runBlocking {
        val repo = WalletRepository()
        val result = repo.executeSafeTransaction(
            to = DeploymentConfig.OPERATIONS_SAFE_ADDRESS,
            valueWei = BigInteger.ZERO,
            signatures = ByteArray(65)
        )
        assertTrue(result.isSuccess)
        val payload = result.getOrNull()
        assertNotNull(payload)
        assertTrue(payload!!.contains("execTransaction") || payload.startsWith("0x") || payload.contains("Payload prepared"))
    }

    @Test
    fun testHistoricalFlowDataIntegrity() {
        val data = com.example.ui.components.defaultHistoricalFlowData
        assertFalse(data.isEmpty())
        assertEquals(12, data.size)

        // Verify Genesis lock is 500M
        val genesis = data.first()
        assertEquals("Jan '26", genesis.month)
        assertEquals(500.0, genesis.reserves, 0.01)

        // Verify current September reserves are tracked properly
        val sep = data.find { it.month == "Sep '26" }
        assertNotNull(sep)
        assertEquals(423.5, sep!!.reserves, 0.01)
        assertTrue(sep.outflow > 0.0)

        // Verify net delta is computed as inflow - outflow
        data.forEach { pt ->
            assertEquals(pt.inflow - pt.outflow, pt.netDelta, 0.01)
        }
    }
}
