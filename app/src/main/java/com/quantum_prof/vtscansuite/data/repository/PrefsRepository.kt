// data/repository/PrefsRepository.kt
package com.quantum_prof.vtscansuite.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.quantum_prof.vtscansuite.data.security.ApiKeyCipher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

@Singleton
class PrefsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cipher: ApiKeyCipher
) {
    /** Alter Klartext-Schlüssel – wird nur noch gelesen, um einmalig zu migrieren. */
    private val legacyApiKey = stringPreferencesKey("api_key")

    /** Keystore-verschlüsselter API-Key (Base64). */
    private val encryptedApiKey = stringPreferencesKey("api_key_enc")

    private val showHelp = booleanPreferencesKey("show_help_icons")

    /**
     * Liefert den API-Key im Klartext (nur im Speicher). Solange [migrateLegacyKey] noch
     * nicht gelaufen ist, wird ersatzweise der alte Klartext-Wert gelesen, damit ein
     * Update niemals den Key des Nutzers verliert.
     */
    val apiKeyFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[encryptedApiKey]?.let { cipher.decrypt(it) } ?: preferences[legacyApiKey]
    }

    /** Ob auf der Ergebnisseite die Hilfe-Icons angezeigt werden (Default: an). */
    val showHelpFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[showHelp] ?: true
    }

    suspend fun saveApiKey(key: String) {
        val trimmed = key.trim()
        val encrypted = cipher.encrypt(trimmed)
        context.dataStore.edit { preferences ->
            preferences.remove(legacyApiKey)
            if (encrypted != null) {
                preferences[encryptedApiKey] = encrypted
            } else {
                // Keystore nicht verfügbar: lieber nichts persistieren als im Klartext.
                preferences.remove(encryptedApiKey)
            }
        }
    }

    suspend fun clearApiKey() {
        context.dataStore.edit { preferences ->
            preferences.remove(legacyApiKey)
            preferences.remove(encryptedApiKey)
        }
    }

    suspend fun setShowHelp(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[showHelp] = enabled
        }
    }

    /**
     * Überführt einen noch im Klartext gespeicherten Key (Versionen <= 2.2) in die
     * verschlüsselte Form und entfernt das Klartext-Feld. Wird einmalig beim App-Start
     * aufgerufen; danach ist im DataStore nur noch der Chiffretext enthalten.
     */
    suspend fun migrateLegacyKey() {
        val plain = context.dataStore.data.first()[legacyApiKey] ?: return
        val encrypted = cipher.encrypt(plain)
        context.dataStore.edit { preferences ->
            // Ein bereits verschlüsselt gespeicherter Wert hat Vorrang.
            if (encrypted != null && preferences[encryptedApiKey] == null) {
                preferences[encryptedApiKey] = encrypted
            }
            // Klartext verschwindet in jedem Fall – auch wenn kein Keystore verfügbar ist.
            preferences.remove(legacyApiKey)
        }
    }
}
