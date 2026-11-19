package com.quickgit.desktop.data

import java.awt.Desktop
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.Properties
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Checks GitHub Releases for a newer QuickGitNEXT desktop package, downloads the
 * matching installer format, and applies it:
 *  - Windows → .msi
 *  - Debian/Ubuntu (deb install) → .deb
 *  - Fedora/RHEL/openSUSE (rpm install) → .rpm
 *  - AppImage session → .AppImage (in-place replace)
 */
object DesktopAppUpdateConfig {
    const val OWNER = "QuickGitNEXT"
    const val REPO = "QuickGit-NEXT"
    /** Stamped by CI / packageVersion; override via system property quickgitnext.version. */
    var currentVersionName: String =
        System.getProperty("quickgitnext.version")
            ?: System.getenv("QUICKGIT_VERSION")
            ?: "1.0.0"
}

data class DesktopVersionInfo(val versionName: String, val versionCode: Long)

enum class DesktopPackageFormat(val label: String, val suffix: String) {
    MSI("Windows MSI", ".msi"),
    DEB("Debian package", ".deb"),
    RPM("RPM package", ".rpm"),
    APPIMAGE("AppImage", ".AppImage"),
    DMG("macOS disk image", ".dmg"),
    UNKNOWN("Unknown", "")
}

sealed class DesktopUpdateCheckResult {
    data class UpToDate(val current: DesktopVersionInfo) : DesktopUpdateCheckResult()
    data class Available(
        val current: DesktopVersionInfo,
        val latest: DesktopVersionInfo,
        val releaseName: String,
        val releaseNotes: String,
        val downloadUrl: String,
        val assetName: String,
        val format: DesktopPackageFormat
    ) : DesktopUpdateCheckResult()
    data class Error(val message: String) : DesktopUpdateCheckResult()
}

sealed class DesktopInstallResult {
    data class Success(val message: String, val needsRelaunch: Boolean = true) : DesktopInstallResult()
    data class Error(val message: String) : DesktopInstallResult()
}

class DesktopAppUpdateManager(
    private val credentialStore: DesktopCredentialStore
) {
    private val prefsFile = File(
        System.getProperty("user.home"),
        ".config/quickgitnext/update_prefs.properties"
    )
    private val prefs = Properties().apply {
        if (prefsFile.exists()) prefsFile.inputStream().use { load(it) }
    }

    private fun savePrefs() {
        prefsFile.parentFile?.mkdirs()
        prefsFile.outputStream().use { prefs.store(it, "QuickGitNEXT update prefs") }
    }

    fun isAutoPromptSuppressed(): Boolean {
        if (prefs.getProperty("never") == "true") return true
        val until = prefs.getProperty("snooze_until_ms")?.toLongOrNull() ?: 0L
        return until > System.currentTimeMillis()
    }

    fun snoozeForDays(days: Int) {
        prefs.setProperty(
            "snooze_until_ms",
            (System.currentTimeMillis() + days * 24L * 60 * 60 * 1000).toString()
        )
        savePrefs()
    }

    fun neverPrompt() {
        prefs.setProperty("never", "true")
        savePrefs()
    }

    fun clearSuppression() {
        prefs.remove("never")
        prefs.remove("snooze_until_ms")
        savePrefs()
    }

    fun currentVersion(): DesktopVersionInfo {
        val name = DesktopAppUpdateConfig.currentVersionName
        return DesktopVersionInfo(name, parseVersionCode(name))
    }

    /** How this binary was installed — drives which release asset we fetch. */
    fun detectPackageFormat(): DesktopPackageFormat {
        // Explicit override for testing
        when (System.getProperty("quickgitnext.package")?.trim()?.lowercase()) {
            "msi", "windows" -> return DesktopPackageFormat.MSI
            "deb", "debian", "ubuntu" -> return DesktopPackageFormat.DEB
            "rpm", "fedora", "rhel" -> return DesktopPackageFormat.RPM
            "appimage" -> return DesktopPackageFormat.APPIMAGE
            "dmg", "mac" -> return DesktopPackageFormat.DMG
        }
        val os = System.getProperty("os.name").orEmpty().lowercase()
        if (os.contains("win")) return DesktopPackageFormat.MSI
        if (os.contains("mac") || os.contains("darwin")) return DesktopPackageFormat.DMG

        // AppImage sets APPIMAGE to the image path
        System.getenv("APPIMAGE")?.takeIf { it.isNotBlank() }?.let {
            return DesktopPackageFormat.APPIMAGE
        }
        // Also detect if the running binary path ends with .AppImage
        runCatching {
            val exe = File("/proc/self/exe").canonicalPath
            if (exe.endsWith(".AppImage", ignoreCase = true)) return DesktopPackageFormat.APPIMAGE
        }

        // Packaged deb/rpm typically under /opt/quickgitnext or /usr
        val installedDeb = processSucceeded("dpkg-query", "-W", "-f=\${Status}", "quickgitnext") &&
            processStdout("dpkg-query", "-W", "-f=\${Status}", "quickgitnext").contains("install ok")
        if (installedDeb) return DesktopPackageFormat.DEB

        if (processSucceeded("rpm", "-q", "quickgitnext") ||
            processSucceeded("rpm", "-q", "QuickGitNEXT")
        ) {
            return DesktopPackageFormat.RPM
        }

        // Distro heuristics when package query is unavailable
        val osRelease = File("/etc/os-release").readTextSafe().lowercase()
        val isDebianFamily = File("/etc/debian_version").exists() ||
            "ubuntu" in osRelease || "debian" in osRelease ||
            "linuxmint" in osRelease || "pop" in osRelease || "elementary" in osRelease
        val isRpmFamily = "fedora" in osRelease || "rhel" in osRelease ||
            "centos" in osRelease || "rocky" in osRelease || "alma" in osRelease ||
            "opensuse" in osRelease || "sles" in osRelease || "mageia" in osRelease
        return when {
            isDebianFamily -> DesktopPackageFormat.DEB
            isRpmFamily -> DesktopPackageFormat.RPM
            else -> DesktopPackageFormat.APPIMAGE
        }
    }

    /** Ordered suffixes to match for this install type (strict preferred first). */
    private fun preferredAssetSuffixes(format: DesktopPackageFormat = detectPackageFormat()): List<String> {
        return when (format) {
            DesktopPackageFormat.MSI -> listOf(".msi")
            DesktopPackageFormat.DEB -> listOf(".deb")
            DesktopPackageFormat.RPM -> listOf(".rpm")
            DesktopPackageFormat.APPIMAGE -> listOf(".AppImage", ".appimage")
            DesktopPackageFormat.DMG -> listOf(".dmg")
            DesktopPackageFormat.UNKNOWN -> listOf(".AppImage", ".deb", ".rpm", ".msi")
        }
    }

    private fun File.readTextSafe(): String =
        try {
            if (isFile) readText() else ""
        } catch (_: Exception) {
            ""
        }

    fun checkForUpdate(): DesktopUpdateCheckResult {
        val current = currentVersion()
        val format = detectPackageFormat()
        return try {
            val api =
                "https://api.github.com/repos/${DesktopAppUpdateConfig.OWNER}/${DesktopAppUpdateConfig.REPO}/releases/latest"
            val body = httpGet(api) ?: return DesktopUpdateCheckResult.Error("Empty response from GitHub")
            if (body.contains("\"message\"") && body.contains("Not Found")) {
                return DesktopUpdateCheckResult.UpToDate(current)
            }
            val tag = jsonString(body, "tag_name")?.removePrefix("v")?.removePrefix("V")
                ?: return DesktopUpdateCheckResult.Error("No tag_name in release")
            val latestCode = parseVersionCode(tag)
            if (latestCode <= current.versionCode) {
                return DesktopUpdateCheckResult.UpToDate(current)
            }
            val releaseName = jsonString(body, "name") ?: tag
            val notes = jsonString(body, "body") ?: ""
            val assetsBlob = extractArray(body, "assets") ?: ""
            val suffixes = preferredAssetSuffixes(format)
            var chosenUrl: String? = null
            var chosenName: String? = null
            for (suffix in suffixes) {
                val match = findAsset(assetsBlob, suffix)
                if (match != null) {
                    chosenName = match.first
                    chosenUrl = match.second
                    break
                }
            }
            if (chosenUrl.isNullOrBlank()) {
                return DesktopUpdateCheckResult.Error(
                    "No ${format.label} (${suffixes.joinToString("/")}) asset on the latest release"
                )
            }
            DesktopUpdateCheckResult.Available(
                current = current,
                latest = DesktopVersionInfo(tag, latestCode),
                releaseName = releaseName,
                releaseNotes = notes,
                downloadUrl = chosenUrl,
                assetName = chosenName.orEmpty(),
                format = format
            )
        } catch (e: Exception) {
            DesktopUpdateCheckResult.Error(e.message ?: "Update check failed")
        }
    }

    fun downloadUpdate(
        url: String,
        assetName: String,
        onProgress: (Int) -> Unit = {}
    ): Result<File> {
        return try {
            val dir = File(System.getProperty("user.home"), ".cache/quickgitnext/updates").apply { mkdirs() }
            val out = File(dir, assetName)
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 20_000
                readTimeout = 180_000
                instanceFollowRedirects = true
                setRequestProperty("Accept", "application/octet-stream")
                setRequestProperty("User-Agent", "QuickGitNEXT-Desktop/${DesktopAppUpdateConfig.currentVersionName}")
                authHeader()?.let { setRequestProperty("Authorization", it) }
            }
            val code = conn.responseCode
            if (code !in 200..299) {
                return Result.failure(IllegalStateException("Download failed (HTTP $code)"))
            }
            val total = conn.contentLengthLong.takeIf { it > 0 } ?: -1L
            conn.inputStream.use { input ->
                FileOutputStream(out).use { output ->
                    val buf = ByteArray(64 * 1024)
                    var read = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n <= 0) break
                        output.write(buf, 0, n)
                        read += n
                        if (total > 0) onProgress(((read * 100) / total).toInt().coerceIn(0, 100))
                    }
                }
            }
            onProgress(100)
            Result.success(out)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Install a downloaded package using the matching package manager.
     * May prompt for polkit/UAC elevation.
     */
    fun installUpdate(file: File, format: DesktopPackageFormat = detectPackageFormat()): DesktopInstallResult {
        if (!file.isFile) return DesktopInstallResult.Error("Package not found: ${file.absolutePath}")
        return try {
            when (format) {
                DesktopPackageFormat.DEB -> installDeb(file)
                DesktopPackageFormat.RPM -> installRpm(file)
                DesktopPackageFormat.MSI -> installMsi(file)
                DesktopPackageFormat.APPIMAGE -> installAppImage(file)
                DesktopPackageFormat.DMG -> {
                    openFile(file)
                    DesktopInstallResult.Success(
                        "Opened ${file.name}. Drag the app to Applications to finish updating.",
                        needsRelaunch = false
                    )
                }
                DesktopPackageFormat.UNKNOWN -> {
                    openFile(file)
                    DesktopInstallResult.Success("Opened ${file.name}", needsRelaunch = false)
                }
            }
        } catch (e: Exception) {
            DesktopInstallResult.Error(e.message ?: "Install failed")
        }
    }

    /** Download the matching asset and install it. */
    fun downloadAndInstall(
        available: DesktopUpdateCheckResult.Available,
        onProgress: (Int) -> Unit = {}
    ): DesktopInstallResult {
        if (available.downloadUrl.isBlank()) {
            return DesktopInstallResult.Error("No download URL for ${available.format.label}")
        }
        val file = downloadUpdate(available.downloadUrl, available.assetName, onProgress).getOrElse {
            return DesktopInstallResult.Error(it.message ?: "Download failed")
        }
        return installUpdate(file, available.format)
    }

    private fun installDeb(file: File): DesktopInstallResult {
        // Prefer apt so dependencies resolve; fall back to dpkg
        val absolute = file.absolutePath
        val script = """
            set -e
            export DEBIAN_FRONTEND=noninteractive
            if command -v apt-get >/dev/null 2>&1; then
              apt-get install -y "$absolute" || { dpkg -i "$absolute" || true; apt-get install -y -f; }
            else
              dpkg -i "$absolute" || true
              apt-get install -y -f || true
            fi
        """.trimIndent()
        val r = runElevatedShell(script)
        return if (r.first == 0) {
            DesktopInstallResult.Success("Installed ${file.name}. Restart QuickGitNEXT to use the new version.")
        } else {
            DesktopInstallResult.Error("deb install failed (exit ${r.first}): ${r.second.take(400)}")
        }
    }

    private fun installRpm(file: File): DesktopInstallResult {
        val absolute = file.absolutePath
        val script = """
            set -e
            if command -v dnf >/dev/null 2>&1; then
              dnf install -y "$absolute"
            elif command -v zypper >/dev/null 2>&1; then
              zypper --non-interactive install --force "$absolute"
            elif command -v yum >/dev/null 2>&1; then
              yum install -y "$absolute"
            else
              rpm -Uvh --force "$absolute"
            fi
        """.trimIndent()
        val r = runElevatedShell(script)
        return if (r.first == 0) {
            DesktopInstallResult.Success("Installed ${file.name}. Restart QuickGitNEXT to use the new version.")
        } else {
            DesktopInstallResult.Error("rpm install failed (exit ${r.first}): ${r.second.take(400)}")
        }
    }

    private fun installMsi(file: File): DesktopInstallResult {
        // Passive UI shows progress and requests elevation when needed
        val pb = ProcessBuilder(
            "msiexec",
            "/i", file.absolutePath,
            "/passive",
            "/norestart"
        ).redirectErrorStream(true)
        val p = pb.start()
        val out = p.inputStream.bufferedReader().readText()
        val code = p.waitFor()
        // 0 success, 3010 success reboot required
        return if (code == 0 || code == 3010) {
            DesktopInstallResult.Success(
                "Installed ${file.name}. Restart QuickGitNEXT to use the new version.",
                needsRelaunch = true
            )
        } else {
            DesktopInstallResult.Error("MSI install failed (exit $code): ${out.take(400)}")
        }
    }

    private fun installAppImage(file: File): DesktopInstallResult {
        val current = System.getenv("APPIMAGE")?.takeIf { it.isNotBlank() }
            ?: runCatching { File("/proc/self/exe").canonicalPath }.getOrNull()
                ?.takeIf { it.endsWith(".AppImage", ignoreCase = true) }
        if (current.isNullOrBlank()) {
            // Save next to Downloads and open folder
            val destDir = File(System.getProperty("user.home"), "Downloads")
            val dest = File(destDir, file.name)
            file.copyTo(dest, overwrite = true)
            dest.setExecutable(true, false)
            openFile(destDir)
            return DesktopInstallResult.Success(
                "Downloaded AppImage to ${dest.absolutePath}. Replace your current AppImage with this file and relaunch.",
                needsRelaunch = false
            )
        }
        val target = File(current)
        val backup = File(target.parentFile, target.name + ".bak")
        val staged = File(target.parentFile, target.name + ".new")
        return try {
            file.copyTo(staged, overwrite = true)
            staged.setExecutable(true, false)
            if (target.exists()) {
                target.copyTo(backup, overwrite = true)
            }
            if (!staged.renameTo(target)) {
                staged.copyTo(target, overwrite = true)
                staged.delete()
            }
            target.setExecutable(true, false)
            DesktopInstallResult.Success(
                "AppImage updated at ${target.absolutePath}. Restart QuickGitNEXT to use the new version."
            )
        } catch (e: Exception) {
            // Attempt restore
            runCatching { if (backup.exists()) backup.copyTo(target, overwrite = true) }
            DesktopInstallResult.Error("AppImage replace failed: ${e.message}")
        }
    }

    /** Run shell script with pkexec when not root. */
    private fun runElevatedShell(script: String): Pair<Int, String> {
        val tmp = File.createTempFile("quickgitnext-update-", ".sh")
        try {
            tmp.writeText("#!/bin/sh\n$script\n")
            tmp.setExecutable(true)
            val isRoot = runCatching {
                processStdout("id", "-u").trim() == "0"
            }.getOrDefault(false)
            val cmd = if (isRoot) {
                listOf("/bin/sh", tmp.absolutePath)
            } else if (commandExists("pkexec")) {
                listOf("pkexec", "/bin/sh", tmp.absolutePath)
            } else if (commandExists("sudo")) {
                listOf("sudo", "-n", "/bin/sh", tmp.absolutePath)
            } else {
                listOf("/bin/sh", tmp.absolutePath)
            }
            val pb = ProcessBuilder(cmd).redirectErrorStream(true)
            val p = pb.start()
            val out = p.inputStream.bufferedReader().readText()
            val finished = p.waitFor(30, TimeUnit.MINUTES)
            if (!finished) {
                p.destroyForcibly()
                return 124 to "Timed out"
            }
            return p.exitValue() to out
        } finally {
            tmp.delete()
        }
    }

    private fun commandExists(name: String): Boolean {
        val path = System.getenv("PATH") ?: return false
        return path.split(File.pathSeparator).any { dir ->
            File(dir, name).canExecute()
        }
    }

    private fun processSucceeded(vararg cmd: String): Boolean = runCatching {
        val p = ProcessBuilder(*cmd).redirectErrorStream(true).start()
        p.waitFor(8, TimeUnit.SECONDS) && p.exitValue() == 0
    }.getOrDefault(false)

    private fun processStdout(vararg cmd: String): String = runCatching {
        val p = ProcessBuilder(*cmd).redirectErrorStream(true).start()
        val out = p.inputStream.bufferedReader().readText()
        p.waitFor(8, TimeUnit.SECONDS)
        out
    }.getOrDefault("")

    fun openFile(file: File) {
        try {
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(file)
        } catch (_: Exception) {
        }
    }

    fun openReleasePage() {
        val uri = URI(
            "https://github.com/${DesktopAppUpdateConfig.OWNER}/${DesktopAppUpdateConfig.REPO}/releases/latest"
        )
        try {
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(uri)
        } catch (_: Exception) {
        }
    }

    private fun authHeader(): String? {
        val token = credentialStore.getGithubToken()?.takeIf { it.isNotBlank() }
            ?: credentialStore.getHttpsToken("github.com")?.takeIf { it.isNotBlank() }
        return token?.let { "Bearer $it" }
    }

    private fun httpGet(url: String): String? {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 20_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "QuickGitNEXT-Desktop/${DesktopAppUpdateConfig.currentVersionName}")
            authHeader()?.let { setRequestProperty("Authorization", it) }
        }
        val code = conn.responseCode
        if (code == 404) return """{"message":"Not Found"}"""
        if (code !in 200..299) throw IllegalStateException("GitHub API HTTP $code")
        return conn.inputStream.bufferedReader().readText()
    }

    companion object {
        private val VERSION_PATTERN = Pattern.compile("""(\d+)\.(\d+)(?:\.(\d+))?""")

        fun parseVersionCode(name: String): Long {
            var s = name.trim()
            s = s.removePrefix("QuickGitNEXT").trim()
            s = s.removePrefix("v").removePrefix("V")
            if (s.length >= 2 && (s[0] == 'A' || s[0] == 'D' || s[0] == 'a' || s[0] == 'd') && s[1].isDigit()) {
                s = s.substring(1)
            }
            if (s.isNotEmpty() && (s.last() == 'A' || s.last() == 'D' || s.last() == 'a' || s.last() == 'd')) {
                s = s.dropLast(1)
            }
            val m = VERSION_PATTERN.matcher(s)
            if (!m.find()) return 0L
            val major = m.group(1).toLongOrNull() ?: 0L
            val minor = m.group(2).toLongOrNull() ?: 0L
            val patch = m.group(3)?.toLongOrNull() ?: 0L
            return major * 1_000_000L + minor * 1_000L + patch
        }

        private fun jsonString(json: String, key: String): String? {
            val re = Pattern.compile("\"${Pattern.quote(key)}\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
            val m = re.matcher(json)
            if (!m.find()) return null
            return m.group(1)
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
        }

        private fun extractArray(json: String, key: String): String? {
            val idx = json.indexOf("\"$key\"")
            if (idx < 0) return null
            val bracket = json.indexOf('[', idx)
            if (bracket < 0) return null
            var depth = 0
            for (i in bracket until json.length) {
                when (json[i]) {
                    '[' -> depth++
                    ']' -> {
                        depth--
                        if (depth == 0) return json.substring(bracket, i + 1)
                    }
                }
            }
            return null
        }

        /** Returns (name, browser_download_url) for first asset ending with [suffix]. */
        private fun findAsset(assetsJson: String, suffix: String): Pair<String, String>? {
            val nameRe = Pattern.compile("\"name\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
            val urlRe = Pattern.compile("\"browser_download_url\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
            var searchFrom = 0
            while (searchFrom < assetsJson.length) {
                val nm = nameRe.matcher(assetsJson)
                if (!nm.find(searchFrom)) break
                val name = nm.group(1)
                val afterName = nm.end()
                val um = urlRe.matcher(assetsJson)
                if (!um.find(afterName)) {
                    searchFrom = afterName
                    continue
                }
                val url = um.group(1)
                val nextName = nameRe.matcher(assetsJson)
                val nextNamePos = if (nextName.find(afterName)) nextName.start() else assetsJson.length
                if (um.start() < nextNamePos && name.endsWith(suffix, ignoreCase = true)) {
                    return name to url
                }
                searchFrom = afterName
            }
            return null
        }
    }
}
