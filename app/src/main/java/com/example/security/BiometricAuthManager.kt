package com.example.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Manages biometric authentication using AndroidX BiometricPrompt API.
 * Ensures strict identity verification before hardware wallet signing or treasury disbursements.
 */
class BiometricAuthManager(private val activity: FragmentActivity) {

    sealed class AuthResult {
        object Success : AuthResult()
        data class Error(val errorCode: Int, val errorMessage: String) : AuthResult()
        object Failed : AuthResult()
        data class Unavailable(val reason: String) : AuthResult()
    }

    enum class BiometricStatus {
        READY,
        NO_HARDWARE,
        HARDWARE_UNAVAILABLE,
        NONE_ENROLLED,
        SECURITY_UPDATE_REQUIRED,
        UNSUPPORTED
    }

    /**
     * Checks if biometric authentication can be performed on this device.
     */
    fun checkBiometricAvailability(): BiometricStatus {
        val biometricManager = BiometricManager.from(activity)
        return when (biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.READY
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.HARDWARE_UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NONE_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricStatus.SECURITY_UPDATE_REQUIRED
            else -> BiometricStatus.UNSUPPORTED
        }
    }

    /**
     * Prompts the user with BiometricPrompt to authorize a treasury action.
     */
    fun authenticate(
        title: String = "Authorize Treasury Transaction",
        subtitle: String = "Verify identity to sign with DCDN Safe hardware key",
        description: String = "Biometric proof is cryptographically required for signing.",
        onResult: (AuthResult) -> Unit
    ) {
        val availability = checkBiometricAvailability()
        if (availability != BiometricStatus.READY) {
            val message = when (availability) {
                BiometricStatus.NO_HARDWARE -> "Device lacks biometric hardware sensors"
                BiometricStatus.HARDWARE_UNAVAILABLE -> "Biometric hardware is currently busy or unavailable"
                BiometricStatus.NONE_ENROLLED -> "No fingerprint or face enrolled on device"
                BiometricStatus.SECURITY_UPDATE_REQUIRED -> "Biometric security update required by Android OS"
                BiometricStatus.UNSUPPORTED -> "Biometric authentication not supported on this device"
                else -> "Biometrics unavailable"
            }
            onResult(AuthResult.Unavailable(message))
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onResult(AuthResult.Success)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onResult(AuthResult.Error(errorCode, errString.toString()))
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onResult(AuthResult.Failed)
            }
        }

        val biometricPrompt = BiometricPrompt(activity, executor, callback)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription(description)
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
