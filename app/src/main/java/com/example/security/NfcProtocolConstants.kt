package com.example.security

import java.io.ByteArrayOutputStream

/**
 * ISO 7816-4 APDU protocol constants and builder for hardware wallet NFC communication
 * Compatible with Ledger (Ethereum applet), Satochip, Securosys, and Keycard NFC cards.
 */
object NfcProtocolConstants {

    // APDU Header constants
    const val CLA_ISO7816: Byte = 0x00.toByte()
    const val CLA_LEDGER: Byte = 0xE0.toByte()
    const val CLA_SATOCHIP: Byte = 0xB0.toByte()

    // Instruction bytes
    const val INS_SELECT: Byte = 0xA4.toByte()
    const val INS_GET_VERSION: Byte = 0x01.toByte()
    const val INS_GET_PUBLIC_KEY: Byte = 0x02.toByte()
    const val INS_SIGN_HASH: Byte = 0x08.toByte()
    const val INS_GET_APP_NAME: Byte = 0x00.toByte()

    // Applet Application Identifiers (AIDs)
    // Ledger Ethereum Applet AID: 0x45, 0x74, 0x68, 0x65, 0x72, 0x65, 0x75, 0x6D ("Ethereum")
    val AID_LEDGER_ETHEREUM = byteArrayOf(0x45.toByte(), 0x74.toByte(), 0x68.toByte(), 0x65.toByte(), 0x72.toByte(), 0x65.toByte(), 0x75.toByte(), 0x6D.toByte())

    // Satochip hardware wallet AID
    val AID_SATOCHIP = byteArrayOf(0x53.toByte(), 0x61.toByte(), 0x74.toByte(), 0x6F.toByte(), 0x43.toByte(), 0x68.toByte(), 0x69.toByte(), 0x70.toByte())

    // Keycard Ethereum AID
    val AID_KEYCARD = byteArrayOf(0xA0.toByte(), 0x00.toByte(), 0x00.toByte(), 0x08.toByte(), 0x04.toByte(), 0x00.toByte(), 0x01.toByte(), 0x01.toByte())

    // ISO 7816 Status Words (SW1, SW2)
    const val SW_SUCCESS = 0x9000
    const val SW_CONDITIONS_NOT_SATISFIED = 0x6985 // User cancelled or pin required
    const val SW_SECURITY_STATUS_NOT_SATISFIED = 0x6982
    const val SW_APPLET_NOT_FOUND = 0x6A82
    const val SW_WRONG_LENGTH = 0x6700
    const val SW_INS_NOT_SUPPORTED = 0x6D00
    const val SW_CLA_NOT_SUPPORTED = 0x6E00

    /**
     * Constructs ISO 7816-4 SELECT command APDU for target hardware applet AID.
     */
    fun buildSelectAppletApdu(aid: ByteArray = AID_LEDGER_ETHEREUM): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(byteArrayOf(CLA_ISO7816, INS_SELECT, 0x04.toByte(), 0x00.toByte()))
        out.write(aid.size)
        out.write(aid)
        out.write(0x00) // Le
        return out.toByteArray()
    }

    /**
     * Constructs APDU to query hardware wallet applet version / status.
     */
    fun buildGetAppVersionApdu(): ByteArray {
        return byteArrayOf(CLA_LEDGER, INS_GET_VERSION, 0x00.toByte(), 0x00.toByte(), 0x00.toByte())
    }

    /**
     * Constructs APDU to derive secp256k1 public key and Ethereum address
     * BIP-44 Derivation Path: m/44'/60'/0'/0/0 (5 elements, 4 bytes each = 20 bytes payload)
     */
    fun buildGetPublicKeyApdu(
        bip44Path: IntArray = intArrayOf(
            0x80000000.toInt() or 44, // 44'
            0x80000000.toInt() or 60, // 60'
            0x80000000.toInt() or 0,  // 0'
            0,                         // 0
            0                          // 0
        )
    ): ByteArray {
        val out = ByteArrayOutputStream()
        val pathBytes = ByteArray(1 + bip44Path.size * 4)
        pathBytes[0] = bip44Path.size.toByte()

        var offset = 1
        for (index in bip44Path) {
            pathBytes[offset++] = ((index shr 24) and 0xFF).toByte()
            pathBytes[offset++] = ((index shr 16) and 0xFF).toByte()
            pathBytes[offset++] = ((index shr 8) and 0xFF).toByte()
            pathBytes[offset++] = (index and 0xFF).toByte()
        }

        // CLA=0xE0, INS=0x02 (GET_PUBLIC_KEY), P1=0x00 (return address), P2=0x00 (chain code), Lc=21
        out.write(byteArrayOf(CLA_LEDGER, INS_GET_PUBLIC_KEY, 0x00.toByte(), 0x00.toByte()))
        out.write(pathBytes.size)
        out.write(pathBytes)
        out.write(0x00) // Le
        return out.toByteArray()
    }

    /**
     * Constructs APDU to sign a 32-byte transaction hash / EIP-712 Safe hash on-device.
     */
    fun buildSignHashApdu(
        hash32: ByteArray,
        bip44Path: IntArray = intArrayOf(
            0x80000000.toInt() or 44,
            0x80000000.toInt() or 60,
            0x80000000.toInt() or 0,
            0,
            0
        )
    ): ByteArray {
        val out = ByteArrayOutputStream()
        val pathSize = 1 + bip44Path.size * 4
        val totalLength = pathSize + hash32.size
        val payload = ByteArray(totalLength)

        payload[0] = bip44Path.size.toByte()
        var offset = 1
        for (index in bip44Path) {
            payload[offset++] = ((index shr 24) and 0xFF).toByte()
            payload[offset++] = ((index shr 16) and 0xFF).toByte()
            payload[offset++] = ((index shr 8) and 0xFF).toByte()
            payload[offset++] = (index and 0xFF).toByte()
        }

        System.arraycopy(hash32, 0, payload, offset, hash32.size)

        // CLA=0xE0, INS=0x08 (SIGN_HASH), P1=0x00, P2=0x00
        out.write(byteArrayOf(CLA_LEDGER, INS_SIGN_HASH, 0x00.toByte(), 0x00.toByte()))
        out.write(payload.size)
        out.write(payload)
        out.write(0x00) // Le
        return out.toByteArray()
    }

    /**
     * Extracts status word (SW1 SW2) from the trailing two bytes of an APDU response.
     */
    fun extractStatusWord(response: ByteArray): Int {
        if (response.size < 2) return -1
        val sw1 = response[response.size - 2].toInt() and 0xFF
        val sw2 = response[response.size - 1].toInt() and 0xFF
        return (sw1 shl 8) or sw2
    }

    fun toHexString(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02X".format(it) }
    }
}
