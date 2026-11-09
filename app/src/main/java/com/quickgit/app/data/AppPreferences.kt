package com.quickgit.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Phone navigation density:
 * - [AUTO] — use device diagonal / dp heuristics (~6.3″ threshold)
 * - [LARGE] — force large-phone floating bottom nav (≥6.3″ style)
 * - [COMPACT] — force compact in-screen navigation (<6.3″ style)
 */
enum class PhoneNavMode {
    AUTO,
    LARGE,
    COMPACT;

    companion object {
        fun fromStorage(value: String?): PhoneNavMode =
            entries.find { it.name.equals(value, ignoreCase = true) } ?: AUTO
    }
}

/**
 * Wide-device chrome (tablets & Chromebooks):
 * - [AUTO] — Chromebook hardware → ChromeOS shell; other wide windows → tablet floating nav
 * - [TABLET] — force tablet touch UI (floating bars)
 * - [CHROMEBOOK] — force ChromeOS top-chrome shell
 */
enum class WideUiMode {
    AUTO,
    TABLET,
    CHROMEBOOK;

    companion object {
        fun fromStorage(value: String?): WideUiMode =
            entries.find { it.name.equals(value, ignoreCase = true) } ?: AUTO
    }
}

/**
 * Legacy desktop-rail preference. Kept for migration only; UI no longer exposes it.
 */
@Deprecated("Use PhoneNavMode / WideUiMode")
enum class DesktopLayoutMode {
    AUTO,
    ALWAYS,
    NEVER;

    companion object {
        fun fromStorage(value: String?): DesktopLayoutMode =
            entries.find { it.name.equals(value, ignoreCase = true) } ?: AUTO
    }
}

class AppPreferences(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _phoneNavMode = MutableStateFlow(
        PhoneNavMode.fromStorage(prefs.getString(KEY_PHONE_NAV, null))
            .let { stored ->
                // One-time migration from old desktop_layout_mode on first run
                if (prefs.contains(KEY_PHONE_NAV)) stored
                else when (DesktopLayoutMode.fromStorage(prefs.getString(KEY_DESKTOP_LAYOUT, null))) {
                    DesktopLayoutMode.NEVER -> PhoneNavMode.COMPACT
                    DesktopLayoutMode.ALWAYS -> PhoneNavMode.LARGE
                    DesktopLayoutMode.AUTO -> PhoneNavMode.AUTO
                }
            }
    )
    val phoneNavMode: StateFlow<PhoneNavMode> = _phoneNavMode.asStateFlow()

    private val _wideUiMode = MutableStateFlow(
        WideUiMode.fromStorage(prefs.getString(KEY_WIDE_UI, null))
            .let { stored ->
                if (prefs.contains(KEY_WIDE_UI)) stored
                else when (DesktopLayoutMode.fromStorage(prefs.getString(KEY_DESKTOP_LAYOUT, null))) {
                    DesktopLayoutMode.ALWAYS -> WideUiMode.CHROMEBOOK
                    DesktopLayoutMode.NEVER -> WideUiMode.TABLET
                    DesktopLayoutMode.AUTO -> WideUiMode.AUTO
                }
            }
    )
    val wideUiMode: StateFlow<WideUiMode> = _wideUiMode.asStateFlow()

    /** @deprecated Prefer [phoneNavMode] / [wideUiMode]. */
    @Deprecated("Use phoneNavMode / wideUiMode")
    private val _desktopLayoutMode = MutableStateFlow(DesktopLayoutMode.AUTO)
    @Deprecated("Use phoneNavMode / wideUiMode")
    val desktopLayoutMode: StateFlow<DesktopLayoutMode> = _desktopLayoutMode.asStateFlow()

    fun setPhoneNavMode(mode: PhoneNavMode) {
        prefs.edit().putString(KEY_PHONE_NAV, mode.name).apply()
        _phoneNavMode.value = mode
    }

    fun setWideUiMode(mode: WideUiMode) {
        prefs.edit().putString(KEY_WIDE_UI, mode.name).apply()
        _wideUiMode.value = mode
    }

    @Deprecated("Use setPhoneNavMode / setWideUiMode")
    fun setDesktopLayoutMode(mode: DesktopLayoutMode) {
        // Map legacy calls into the new prefs
        when (mode) {
            DesktopLayoutMode.AUTO -> {
                setPhoneNavMode(PhoneNavMode.AUTO)
                setWideUiMode(WideUiMode.AUTO)
            }
            DesktopLayoutMode.ALWAYS -> {
                setPhoneNavMode(PhoneNavMode.LARGE)
                setWideUiMode(WideUiMode.CHROMEBOOK)
            }
            DesktopLayoutMode.NEVER -> {
                setPhoneNavMode(PhoneNavMode.COMPACT)
                setWideUiMode(WideUiMode.TABLET)
            }
        }
        _desktopLayoutMode.value = mode
    }

    companion object {
        private const val PREFS_NAME = "quickgitnext_prefs"
        private const val KEY_DESKTOP_LAYOUT = "desktop_layout_mode"
        private const val KEY_PHONE_NAV = "phone_nav_mode"
        private const val KEY_WIDE_UI = "wide_ui_mode"
    }
}
