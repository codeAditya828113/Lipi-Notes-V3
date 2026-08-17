package com.example.ui.security

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricHelper {

    fun isBiometricAvailable(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        return try {
            val result = biometricManager.canAuthenticate(authenticators)
            result == BiometricManager.BIOMETRIC_SUCCESS
        } catch (e: Exception) {
            false
        }
    }

    fun isBiometricHardwarePresent(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        return try {
            val result = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)
            result != BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE && result != BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE
        } catch (e: Exception) {
            false
        }
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String = "Unlock Notebook",
        subtitle: String = "Use fingerprint or face recognition to unlock",
        negativeButtonText: String = "Use Passcode",
        onSuccess: () -> Unit,
        onError: (errorCode: Int, errString: CharSequence) -> Unit = { _, _ -> },
        onFailed: () -> Unit = {}
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()

        val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError(errorCode, errString)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onFailed()
            }
        })

        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            onError(-1, e.message ?: "Authentication failed to start")
        }
    }
}
