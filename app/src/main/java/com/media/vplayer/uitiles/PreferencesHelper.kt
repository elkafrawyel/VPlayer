package com.media.vplayer.uitiles

import android.content.Context
import android.preference.PreferenceManager

class PreferencesHelper(private val context: Context) {

    companion object {
        private const val IS_LOGGED_IN = "isLoggedIn"
        private const val PIP_MODE = "pip_mode"
        private const val THEME = "theme"
        private const val AUDIO_ALLOWED = "audio_allowed"
        private const val USER = "userModel"
        private const val LANGUAGE = "language"
    }

    private val preference = PreferenceManager.getDefaultSharedPreferences(context)

    var isLoggedIn = preference.getBoolean(IS_LOGGED_IN, false)
        set(value) = preference.edit().putBoolean(IS_LOGGED_IN, value).apply()

    var user = preference.getString(USER, null)
        set(value) = preference.edit().putString(USER, value).apply()

    var themeNumber = preference.getInt(THEME, 1)
        set(value) = preference.edit().putInt(THEME, value).apply()

    var pipMode = preference.getInt(PIP_MODE, 1)
        set(value) = preference.edit().putInt(PIP_MODE, value).apply()

    var audioAllowed = preference.getInt(AUDIO_ALLOWED, 0)
        set(value) = preference.edit().putInt(AUDIO_ALLOWED, value).apply()

    var language = preference.getString(LANGUAGE, null)
        //    var language = preference.getString(LANGUAGE, Constants.Language.ARABIC.value)
        set(value) = preference.edit().putString(LANGUAGE, value).apply()

    fun clear() {
        val lang = language
        preference.edit().clear().apply()
        language = lang
    }
}