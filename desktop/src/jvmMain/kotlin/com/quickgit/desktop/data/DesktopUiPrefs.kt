package com.quickgit.desktop.data

import java.io.File
import java.util.Properties

/**
 * Desktop UI preferences (Chromebook shell, etc.).
 * Stored under ~/.config/quickgitnext/ui.properties
 */
object DesktopUiPrefs {
    private val file = File(
        System.getProperty("user.home"),
        ".config/quickgitnext/ui.properties"
    )
    private val props = Properties().apply {
        if (file.isFile) runCatching { file.inputStream().use { load(it) } }
    }

    private fun save() {
        file.parentFile?.mkdirs()
        file.outputStream().use { props.store(it, "QuickGitNEXT UI prefs") }
    }

    /** True when the user forced Chromebook chrome, or Auto + ChromeOS-like host. */
    fun isChromebookMode(): Boolean {
        when (props.getProperty("chromebook_mode")?.trim()?.lowercase()) {
            "1", "true", "yes", "on", "always" -> return true
            "0", "false", "no", "off", "never" -> return false
        }
        // Auto (default): detect ChromeOS / Crostini / Borealis-style environments
        return detectChromeOsHost()
    }

    fun setChromebookMode(enabled: Boolean?) {
        if (enabled == null) {
            props.remove("chromebook_mode")
        } else {
            props.setProperty("chromebook_mode", if (enabled) "true" else "false")
        }
        save()
    }

    /** null = Auto, true = always, false = never */
    fun chromebookModeSetting(): Boolean? {
        return when (props.getProperty("chromebook_mode")?.trim()?.lowercase()) {
            "1", "true", "yes", "on", "always" -> true
            "0", "false", "no", "off", "never" -> false
            else -> null
        }
    }

    fun detectChromeOsHost(): Boolean {
        val forced = System.getProperty("quickgitnext.chromebook")?.trim()?.lowercase()
        if (forced in listOf("1", "true", "yes")) return true
        if (forced in listOf("0", "false", "no")) return false
        val osRelease = runCatching {
            File("/etc/os-release").takeIf { it.isFile }?.readText()?.lowercase().orEmpty()
        }.getOrDefault("")
        if ("chromeos" in osRelease || "chromebrew" in osRelease) return true
        // Crostini / ARC often expose these
        val env = (System.getenv("SOMMELIER_VERSION") ?: "") +
            (System.getenv("XDG_CURRENT_DESKTOP") ?: "") +
            (System.getenv("HOST_OS") ?: "")
        if ("chrome" in env.lowercase() && "os" in env.lowercase()) return true
        // Running under Linux on ChromeOS: /opt/google/cros-containers or similar
        if (File("/dev/.cros_milestone").exists()) return true
        if (File("/opt/google/cros-containers").isDirectory) return true
        return false
    }
}
