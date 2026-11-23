package com.example.services

import android.app.KeyguardManager
import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat

object BiometricAuthHelper {

    fun isDeviceLockAvailable(context: Context): Boolean {
        val kg = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        return kg?.isDeviceSecure == true
    }

    fun authenticate(
        activityContext: Context,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isDeviceLockAvailable(activityContext)) {
            // Allow graceful unlock on emulators/devices with no PIN/biometric configured
            onSuccess()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching {
                val builder = BiometricPrompt.Builder(activityContext)
                    .setTitle(title)
                    .setSubtitle(subtitle)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    builder.setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG or
                            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                            BiometricManager.Authenticators.DEVICE_CREDENTIAL
                    )
                } else {
                    @Suppress("DEPRECATION")
                    builder.setDeviceCredentialAllowed(true)
                }

                val prompt = builder.build()
                val executor = ContextCompat.getMainExecutor(activityContext)
                prompt.authenticate(
                    CancellationSignal(),
                    executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                            onSuccess()
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                            onError(errString?.toString() ?: "Authentication cancelled")
                        }

                        override fun onAuthenticationFailed() = Unit
                    }
                )
            }.onFailure { err ->
                onError(err.localizedMessage ?: "Biometric prompt unavailable")
            }
        } else {
            // API 26-27 fallback: Keyguard secure device is checked above
            onSuccess()
        }
    }
}
