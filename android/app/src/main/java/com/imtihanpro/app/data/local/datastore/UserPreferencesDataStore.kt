package com.imtihanpro.app.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.imtihanpro.app.data.remote.ApiConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.imtihanDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "imtihan_pro_preferences"
)

data class UserPreferencesState(
    val hasCompletedSetup: Boolean = false,
    val guestDeviceId: String = "guest-local-device",
    val displayName: String = "Guest Learner",
    val userEmail: String? = null,
    val authToken: String? = null,
    val isGoogleLinked: Boolean = false,
    val selectedClassLevel: Int = 11,
    val selectedBoardCode: String = "KPK",
    val selectedSubBoardCode: String = "PESHAWAR",
    val studyGroupCode: String = "PRE_MEDICAL",
    val isDarkMode: Boolean = true,
    val languageCode: String = "English",
    val notificationsEnabled: Boolean = true,
    val offlineOnlyMode: Boolean = false,
    val dailyTargetMinutes: Int = 45,
    val examCountdownDateIso: String? = null,
    val diagnosticCompleted: Boolean = false,
    val leaderboardOptIn: Boolean = false,
    val leaderboardAlias: String? = null,
    val reminderHourLocal: Int = 19,
    val lastSyncedAtIso: String? = null,
    val configuredApiBaseUrl: String = ApiConfig.defaultBaseUrl,
    val soundEffectsEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val readerHighContrastMode: String = "OLED",
    val allowDownloadOverMobileData: Boolean = false
) {
    fun apiLanguageCode(): String = if (languageCode.equals("Urdu", ignoreCase = true) || languageCode == "ur") "ur" else "en"
}

/**
 * DataStore-backed settings & session persistence supporting guest-first use,
 * KPK BISE sub-boards, study groups, study targets, and optional Google account linking.
 */
class UserPreferencesDataStore(private val context: Context) {

    private object Keys {
        val HAS_COMPLETED_SETUP = booleanPreferencesKey("has_completed_setup")
        val GUEST_DEVICE_ID = stringPreferencesKey("guest_device_id")
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
        val IS_GOOGLE_LINKED = booleanPreferencesKey("is_google_linked")
        val SELECTED_CLASS = intPreferencesKey("selected_class")
        val SELECTED_BOARD = stringPreferencesKey("selected_board")
        val SELECTED_SUB_BOARD = stringPreferencesKey("selected_sub_board")
        val STUDY_GROUP = stringPreferencesKey("study_group")
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val LANGUAGE_CODE = stringPreferencesKey("language_code")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val OFFLINE_ONLY_MODE = booleanPreferencesKey("offline_only_mode")
        val DAILY_TARGET_MINUTES = intPreferencesKey("daily_target_minutes")
        val EXAM_COUNTDOWN_DATE_ISO = stringPreferencesKey("exam_countdown_date_iso")
        val DIAGNOSTIC_COMPLETED = booleanPreferencesKey("diagnostic_completed")
        val LEADERBOARD_OPT_IN = booleanPreferencesKey("leaderboard_opt_in")
        val LEADERBOARD_ALIAS = stringPreferencesKey("leaderboard_alias")
        val REMINDER_HOUR_LOCAL = intPreferencesKey("reminder_hour_local")
        val LAST_SYNCED_AT_ISO = stringPreferencesKey("last_synced_at_iso")
        val API_BASE_URL = stringPreferencesKey("api_base_url")
        val SOUND_EFFECTS_ENABLED = booleanPreferencesKey("sound_effects_enabled")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val READER_HIGH_CONTRAST_MODE = stringPreferencesKey("reader_high_contrast_mode")
        val ALLOW_DOWNLOAD_OVER_MOBILE_DATA = booleanPreferencesKey("allow_download_over_mobile_data")
    }

    val preferencesFlow: Flow<UserPreferencesState> = context.imtihanDataStore.data.map { prefs ->
        val rawLang = prefs[Keys.LANGUAGE_CODE]
        val normalizedLang = when (rawLang) {
            null, "en", "English" -> "English"
            "ur", "Urdu" -> "Urdu"
            else -> rawLang
        }
        val cls = prefs[Keys.SELECTED_CLASS] ?: 11
        UserPreferencesState(
            hasCompletedSetup = prefs[Keys.HAS_COMPLETED_SETUP] ?: false,
            guestDeviceId = prefs[Keys.GUEST_DEVICE_ID] ?: "guest-local-device",
            displayName = prefs[Keys.DISPLAY_NAME] ?: "Guest Learner",
            userEmail = prefs[Keys.USER_EMAIL],
            authToken = prefs[Keys.AUTH_TOKEN],
            isGoogleLinked = prefs[Keys.IS_GOOGLE_LINKED] ?: false,
            selectedClassLevel = cls,
            selectedBoardCode = prefs[Keys.SELECTED_BOARD] ?: "KPK",
            selectedSubBoardCode = prefs[Keys.SELECTED_SUB_BOARD] ?: "PESHAWAR",
            studyGroupCode = prefs[Keys.STUDY_GROUP] ?: if (cls >= 11) "PRE_MEDICAL" else "SCIENCE_BIOLOGY",
            isDarkMode = prefs[Keys.IS_DARK_MODE] ?: true,
            languageCode = normalizedLang,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            offlineOnlyMode = prefs[Keys.OFFLINE_ONLY_MODE] ?: false,
            dailyTargetMinutes = prefs[Keys.DAILY_TARGET_MINUTES] ?: 45,
            examCountdownDateIso = prefs[Keys.EXAM_COUNTDOWN_DATE_ISO],
            diagnosticCompleted = prefs[Keys.DIAGNOSTIC_COMPLETED] ?: false,
            leaderboardOptIn = prefs[Keys.LEADERBOARD_OPT_IN] ?: false,
            leaderboardAlias = prefs[Keys.LEADERBOARD_ALIAS],
            reminderHourLocal = prefs[Keys.REMINDER_HOUR_LOCAL] ?: 19,
            lastSyncedAtIso = prefs[Keys.LAST_SYNCED_AT_ISO],
            configuredApiBaseUrl = prefs[Keys.API_BASE_URL] ?: ApiConfig.defaultBaseUrl,
            soundEffectsEnabled = prefs[Keys.SOUND_EFFECTS_ENABLED] ?: true,
            hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true,
            readerHighContrastMode = prefs[Keys.READER_HIGH_CONTRAST_MODE] ?: "OLED",
            allowDownloadOverMobileData = prefs[Keys.ALLOW_DOWNLOAD_OVER_MOBILE_DATA] ?: false
        )
    }

    suspend fun completeFirstTimeSetup(
        classLevel: Int,
        boardCode: String,
        displayName: String,
        isGoogleLinked: Boolean = false,
        subBoardCode: String = "PESHAWAR",
        studyGroupCode: String = if (classLevel >= 11) "PRE_MEDICAL" else "SCIENCE_BIOLOGY",
        languageCode: String = "English"
    ) {
        context.imtihanDataStore.edit { prefs ->
            prefs[Keys.HAS_COMPLETED_SETUP] = true
            prefs[Keys.SELECTED_CLASS] = classLevel
            prefs[Keys.SELECTED_BOARD] = boardCode
            prefs[Keys.SELECTED_SUB_BOARD] = subBoardCode
            prefs[Keys.STUDY_GROUP] = studyGroupCode
            prefs[Keys.LANGUAGE_CODE] = languageCode
            prefs[Keys.IS_GOOGLE_LINKED] = isGoogleLinked
            if (displayName.isNotBlank()) {
                prefs[Keys.DISPLAY_NAME] = displayName.trim()
            }
            if (!prefs.contains(Keys.GUEST_DEVICE_ID)) {
                prefs[Keys.GUEST_DEVICE_ID] = "guest-${UUID.randomUUID().toString().take(8)}"
            }
        }
    }

    suspend fun saveAuthSession(
        token: String?,
        displayName: String?,
        email: String?,
        isGoogleLinked: Boolean
    ) {
        context.imtihanDataStore.edit { prefs ->
            if (!token.isNullOrBlank()) {
                prefs[Keys.AUTH_TOKEN] = token
            }
            if (!displayName.isNullOrBlank()) {
                prefs[Keys.DISPLAY_NAME] = displayName.trim()
            }
            if (!email.isNullOrBlank()) {
                prefs[Keys.USER_EMAIL] = email.trim()
            }
            prefs[Keys.IS_GOOGLE_LINKED] = isGoogleLinked
        }
    }

    suspend fun clearGoogleSession() {
        context.imtihanDataStore.edit { prefs ->
            prefs.remove(Keys.AUTH_TOKEN)
            prefs.remove(Keys.USER_EMAIL)
            prefs[Keys.IS_GOOGLE_LINKED] = false
        }
    }

    suspend fun setApiBaseUrl(url: String) {
        context.imtihanDataStore.edit { prefs ->
            prefs[Keys.API_BASE_URL] = ApiConfig.normalizeBaseUrl(url)
        }
    }

    suspend fun setLastSyncedAtIso(iso: String) {
        context.imtihanDataStore.edit { prefs ->
            prefs[Keys.LAST_SYNCED_AT_ISO] = iso
        }
    }

    suspend fun setOfflineOnlyMode(enabled: Boolean) {
        context.imtihanDataStore.edit { prefs ->
            prefs[Keys.OFFLINE_ONLY_MODE] = enabled
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.imtihanDataStore.edit { prefs ->
            prefs[Keys.IS_DARK_MODE] = enabled
        }
    }

    suspend fun setClassAndBoard(
        classLevel: Int,
        boardCode: String,
        subBoardCode: String? = null,
        studyGroupCode: String? = null
    ) {
        context.imtihanDataStore.edit { prefs ->
            prefs[Keys.SELECTED_CLASS] = classLevel
            prefs[Keys.SELECTED_BOARD] = boardCode
            if (!subBoardCode.isNullOrBlank()) {
                prefs[Keys.SELECTED_SUB_BOARD] = subBoardCode
            }
            if (!studyGroupCode.isNullOrBlank()) {
                prefs[Keys.STUDY_GROUP] = studyGroupCode
            }
        }
    }

    suspend fun setLanguage(languageCode: String) {
        context.imtihanDataStore.edit { prefs ->
            prefs[Keys.LANGUAGE_CODE] = languageCode
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.imtihanDataStore.edit { prefs ->
            prefs[Keys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setStudyGoalPreferences(
        dailyTargetMinutes: Int,
        examCountdownDateIso: String?,
        reminderHourLocal: Int
    ) {
        context.imtihanDataStore.edit { prefs ->
            prefs[Keys.DAILY_TARGET_MINUTES] = dailyTargetMinutes.coerceIn(15, 360)
            if (examCountdownDateIso.isNullOrBlank()) {
                prefs.remove(Keys.EXAM_COUNTDOWN_DATE_ISO)
            } else {
                prefs[Keys.EXAM_COUNTDOWN_DATE_ISO] = examCountdownDateIso.trim()
            }
            prefs[Keys.REMINDER_HOUR_LOCAL] = reminderHourLocal.coerceIn(5, 23)
        }
    }

    suspend fun setDiagnosticCompleted(completed: Boolean) {
        context.imtihanDataStore.edit { prefs ->
            prefs[Keys.DIAGNOSTIC_COMPLETED] = completed
        }
    }

    suspend fun setLeaderboardOptIn(optIn: Boolean, aliasHandle: String?) {
        context.imtihanDataStore.edit { prefs ->
            prefs[Keys.LEADERBOARD_OPT_IN] = optIn
            if (!aliasHandle.isNullOrBlank()) {
                prefs[Keys.LEADERBOARD_ALIAS] = aliasHandle.trim()
            }
        }
    }

    suspend fun setSoundEffectsEnabled(enabled: Boolean) {
        context.imtihanDataStore.edit { prefs ->
            prefs[Keys.SOUND_EFFECTS_ENABLED] = enabled
        }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.imtihanDataStore.edit { prefs ->
            prefs[Keys.HAPTICS_ENABLED] = enabled
        }
    }

    suspend fun setReaderHighContrastMode(mode: String) {
        context.imtihanDataStore.edit { prefs ->
            prefs[Keys.READER_HIGH_CONTRAST_MODE] = if (mode == "PAPER") "PAPER" else "OLED"
        }
    }

    suspend fun setAllowDownloadOverMobileData(enabled: Boolean) {
        context.imtihanDataStore.edit { prefs ->
            prefs[Keys.ALLOW_DOWNLOAD_OVER_MOBILE_DATA] = enabled
        }
    }
}
