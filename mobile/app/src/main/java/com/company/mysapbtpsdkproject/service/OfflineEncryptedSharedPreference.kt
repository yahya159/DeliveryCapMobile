package com.company.mysapbtpsdkproject.service

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * The preference file should not be backed up with Auto Backup.
 */
class OfflineEncryptedSharedPreference {

    private var preferences: SharedPreferences

    private constructor(context: Context) {
        val mainKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        preferences = EncryptedSharedPreferences.create(
            context,
            DB_USER_PREF_FILE,
            mainKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getOfflineDBUser() : String? {
        return preferences.getString(DB_USER_KEY, null)
    }

    fun setOfflineDBUser(user: String) {
        with(preferences.edit()) {
            putString(DB_USER_KEY, user)
            commit()
        }
    }

    fun clear() {
        with(preferences.edit()) {
            clear()
            commit()
        }
    }

    companion object {
        private const val DB_USER_PREF_FILE = "DB_USER_PREF"
        private const val DB_USER_KEY = "DB_USER_KEY"

        @Volatile
        private var instance: OfflineEncryptedSharedPreference? = null

        fun getInstance(context: Context) = instance ?: synchronized(this) {
            instance ?: OfflineEncryptedSharedPreference(context).also {
                instance = it
            }
        }
    }
}

