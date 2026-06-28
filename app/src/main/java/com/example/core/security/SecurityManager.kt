package com.example.core.security

import android.content.Context
import android.content.SharedPreferences
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executor

object SecurityManager {
    private const val PREFS_NAME = "protes_secure_prefs"
    private const val KEY_PIN_HASH = "secure_pin_hash"
    private const val KEY_LOCK_OPEN_APP = "lock_open_app"
    private const val KEY_LOCK_HIDDEN_COLLECTIONS = "lock_hidden_col"
    private const val KEY_LOCK_EXPORT_LIBRARY = "lock_export_lib"
    private const val KEY_LOCK_RESTORE_BACKUP = "lock_restore_bk"
    private const val KEY_HIDDEN_COLLECTIONS = "hidden_collections_ids"
    private const val KEY_AUTO_BACKUP_FREQUENCY = "auto_backup_freq" // none, daily, weekly, monthly
    private const val KEY_AUTO_BACKUP_RETENTION = "auto_backup_ret" // 5, 10, 20, 0 (unlimited)

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // PIN MANAGEMENT
    fun setPin(context: Context, pin: String) {
        val encrypted = EncryptionHelper.encrypt(pin)
        getPrefs(context).edit().putString(KEY_PIN_HASH, encrypted).apply()
    }

    fun hasPin(context: Context): Boolean {
        return !getPrefs(context).getString(KEY_PIN_HASH, null).isNullOrEmpty()
    }

    fun verifyPin(context: Context, inputPin: String): Boolean {
        val stored = getPrefs(context).getString(KEY_PIN_HASH, null) ?: return false
        val decrypted = EncryptionHelper.decrypt(stored) ?: return false
        return decrypted == inputPin
    }

    fun clearPin(context: Context) {
        getPrefs(context).edit().remove(KEY_PIN_HASH).apply()
    }

    // SETTINGS FOR ACTION LOCKS
    fun isLockOpenAppEnabled(context: Context): Boolean {
        return hasPin(context) && getPrefs(context).getBoolean(KEY_LOCK_OPEN_APP, false)
    }

    fun setLockOpenAppEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_LOCK_OPEN_APP, enabled).apply()
    }

    fun isLockHiddenCollectionsEnabled(context: Context): Boolean {
        return hasPin(context) && getPrefs(context).getBoolean(KEY_LOCK_HIDDEN_COLLECTIONS, false)
    }

    fun setLockHiddenCollectionsEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_LOCK_HIDDEN_COLLECTIONS, enabled).apply()
    }

    fun isLockExportLibraryEnabled(context: Context): Boolean {
        return hasPin(context) && getPrefs(context).getBoolean(KEY_LOCK_EXPORT_LIBRARY, false)
    }

    fun setLockExportLibraryEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_LOCK_EXPORT_LIBRARY, enabled).apply()
    }

    fun isLockRestoreBackupEnabled(context: Context): Boolean {
        return hasPin(context) && getPrefs(context).getBoolean(KEY_LOCK_RESTORE_BACKUP, false)
    }

    fun setLockRestoreBackupEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_LOCK_RESTORE_BACKUP, enabled).apply()
    }

    // HIDDEN COLLECTIONS MANAGEMENT
    fun getHiddenCollectionIds(context: Context): Set<String> {
        val saved = getPrefs(context).getString(KEY_HIDDEN_COLLECTIONS, null) ?: return emptySet()
        val decrypted = EncryptionHelper.decrypt(saved) ?: return emptySet()
        return decrypted.split(",").filter { it.isNotEmpty() }.toSet()
    }

    fun setHiddenCollectionIds(context: Context, ids: Set<String>) {
        val plain = ids.joinToString(",")
        val encrypted = EncryptionHelper.encrypt(plain)
        getPrefs(context).edit().putString(KEY_HIDDEN_COLLECTIONS, encrypted).apply()
    }

    fun isCollectionHidden(context: Context, collectionId: String): Boolean {
        return getHiddenCollectionIds(context).contains(collectionId)
    }

    fun setCollectionHidden(context: Context, collectionId: String, hidden: Boolean) {
        val current = getHiddenCollectionIds(context).toMutableSet()
        if (hidden) {
            current.add(collectionId)
        } else {
            current.remove(collectionId)
        }
        setHiddenCollectionIds(context, current)
    }

    // AUTO BACKUP PREFERENCES
    fun getAutoBackupFrequency(context: Context): String {
        return getPrefs(context).getString(KEY_AUTO_BACKUP_FREQUENCY, "none") ?: "none"
    }

    fun setAutoBackupFrequency(context: Context, freq: String) {
        getPrefs(context).edit().putString(KEY_AUTO_BACKUP_FREQUENCY, freq).apply()
    }

    fun getAutoBackupRetention(context: Context): Int {
        return getPrefs(context).getInt(KEY_AUTO_BACKUP_RETENTION, 10)
    }

    fun setAutoBackupRetention(context: Context, limit: Int) {
        getPrefs(context).edit().putInt(KEY_AUTO_BACKUP_RETENTION, limit).apply()
    }

    // SYSTEM BIOMETRICS TRIGGER
    fun authenticateBiometrics(
        context: Context,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            try {
                val executor: Executor = Executor { command -> Handler(Looper.getMainLooper()).post(command) }
                val biometricPrompt = BiometricPrompt.Builder(context)
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setDescription("Use fingerprint or face recognition")
                    .setNegativeButton(
                        "Cancel",
                        executor
                    ) { _, _ -> onError("Cancelled") }
                    .build()

                val cancellationSignal = CancellationSignal()
                biometricPrompt.authenticate(
                    cancellationSignal,
                    executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                            super.onAuthenticationSucceeded(result)
                            onSuccess()
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                            super.onAuthenticationError(errorCode, errString)
                            onError(errString?.toString() ?: "Authentication Error ($errorCode)")
                        }

                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                            onError("Authentication Failed")
                        }
                    }
                )
            } catch (e: Exception) {
                onError("Biometrics not available or configured: ${e.localizedMessage}")
            }
        } else {
            onError("Biometrics require Android 9.0 (API 28) or above")
        }
    }
}
