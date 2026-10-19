package com.example.security

import android.content.Context
import android.content.SharedPreferences
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SecurityManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "wafi_security_prefs"
        private const val KEY_SECURITY_ENABLED = "security_enabled"
        private const val KEY_PIN_CODE = "pin_code"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _isSecurityEnabled = MutableStateFlow(prefs.getBoolean(KEY_SECURITY_ENABLED, false))
    val isSecurityEnabled: StateFlow<Boolean> = _isSecurityEnabled.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false))
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    // Unlocked session state (initially unlocked if security is not enabled)
    private val _isUnlocked = MutableStateFlow(!prefs.getBoolean(KEY_SECURITY_ENABLED, false))
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    fun getSavedPin(): String {
        return prefs.getString(KEY_PIN_CODE, "") ?: ""
    }

    fun hasPin(): Boolean {
        return getSavedPin().isNotEmpty()
    }

    fun setPin(pin: String) {
        prefs.edit().putString(KEY_PIN_CODE, pin).apply()
    }

    fun setSecurityEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SECURITY_ENABLED, enabled).apply()
        _isSecurityEnabled.value = enabled
        if (!enabled) {
            _isUnlocked.value = true
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
        _isBiometricEnabled.value = enabled
    }

    fun verifyPin(enteredPin: String): Boolean {
        val saved = getSavedPin()
        val valid = saved.isNotEmpty() && saved == enteredPin
        if (valid) {
            _isUnlocked.value = true
        }
        return valid
    }

    fun unlockSession() {
        _isUnlocked.value = true
    }

    fun lockSession() {
        if (_isSecurityEnabled.value) {
            _isUnlocked.value = false
        }
    }

    fun canUseBiometrics(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun showBiometricPrompt(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("تأكيد الهوية")
            .setSubtitle("استخدم بصمة الإصبع أو الوجه لفتح وافي")
            .setNegativeButtonText("إلغاء واستخدام PIN")
            .build()

        val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                _isUnlocked.value = true
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError(errString.toString())
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onError("لم يتم التعرف على البصمة، يرجى المحاولة مجدداً")
            }
        })

        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            onError("تعذر إطلاق البصمة: ${e.message}")
        }
    }
}
