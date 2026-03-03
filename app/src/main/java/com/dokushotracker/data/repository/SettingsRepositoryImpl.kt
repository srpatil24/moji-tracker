package com.dokushotracker.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dokushotracker.data.model.MediaType
import com.dokushotracker.domain.model.AccentColorOption
import com.dokushotracker.domain.model.AppSettings
import com.dokushotracker.domain.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "dokusho_settings")

class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : SettingsRepository {
    private val dataStore = context.settingsDataStore

    override fun settingsFlow(): Flow<AppSettings> {
        return dataStore.data
            .catch { throwable ->
                if (throwable is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw throwable
                }
            }
            .map { preferences ->
                AppSettings(
                    defaultMojiCount = preferences[Keys.DEFAULT_MOJI_COUNT] ?: 100_000L,
                    themeMode = preferences[Keys.THEME_MODE]?.let { safeEnumValueOf<ThemeMode>(it) } ?: ThemeMode.SYSTEM,
                    defaultMediaType = preferences[Keys.DEFAULT_MEDIA_TYPE]?.let { safeEnumValueOf<MediaType>(it) },
                    accentColor = preferences[Keys.ACCENT_COLOR]?.let { safeEnumValueOf<AccentColorOption>(it) } ?: AccentColorOption.SAGE,
                    pureBlackDarkMode = preferences[Keys.PURE_BLACK_DARK_MODE] ?: false,
                    lastCelebratedGoalCreatedAtMillis = preferences[Keys.LAST_CELEBRATED_GOAL_CREATED_AT],
                )
            }
    }

    override suspend fun setDefaultMojiCount(value: Long) {
        dataStore.edit { preferences ->
            preferences[Keys.DEFAULT_MOJI_COUNT] = value.coerceAtLeast(1L)
        }
    }

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[Keys.THEME_MODE] = themeMode.name
        }
    }

    override suspend fun setAccentColor(accentColor: AccentColorOption) {
        dataStore.edit { preferences ->
            preferences[Keys.ACCENT_COLOR] = accentColor.name
        }
    }

    override suspend fun setPureBlackDarkMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.PURE_BLACK_DARK_MODE] = enabled
        }
    }

    override suspend fun setLastCelebratedGoalCreatedAtMillis(epochMillis: Long?) {
        dataStore.edit { preferences ->
            if (epochMillis == null) {
                preferences.remove(Keys.LAST_CELEBRATED_GOAL_CREATED_AT)
            } else {
                preferences[Keys.LAST_CELEBRATED_GOAL_CREATED_AT] = epochMillis
            }
        }
    }

    override suspend fun setDefaultMediaType(mediaType: MediaType?) {
        dataStore.edit { preferences ->
            if (mediaType == null) {
                preferences.remove(Keys.DEFAULT_MEDIA_TYPE)
            } else {
                preferences[Keys.DEFAULT_MEDIA_TYPE] = mediaType.name
            }
        }
    }

    private inline fun <reified T : Enum<T>> safeEnumValueOf(raw: String): T? {
        return runCatching { enumValueOf<T>(raw) }.getOrNull()
    }

    private object Keys {
        val DEFAULT_MOJI_COUNT = longPreferencesKey("default_moji_count")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val PURE_BLACK_DARK_MODE = booleanPreferencesKey("pure_black_dark_mode")
        val LAST_CELEBRATED_GOAL_CREATED_AT = longPreferencesKey("last_celebrated_goal_created_at")
        val DEFAULT_MEDIA_TYPE = stringPreferencesKey("default_media_type")
    }
}
