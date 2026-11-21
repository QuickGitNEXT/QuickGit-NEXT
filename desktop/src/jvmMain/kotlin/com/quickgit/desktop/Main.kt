package com.quickgit.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.quickgit.desktop.data.AppLog
import com.quickgit.desktop.data.DesktopAppUpdateConfig
import com.quickgit.desktop.data.DesktopAppUpdateManager
import com.quickgit.desktop.data.DesktopCredentialStore
import com.quickgit.desktop.data.DesktopRepoManager
import com.quickgit.desktop.data.DesktopUpdateCheckResult
import com.quickgit.desktop.data.DesktopInstallResult
import com.quickgit.desktop.ui.DesktopScreen
import com.quickgit.desktop.ui.QuickGitDesktopApp
import com.quickgit.desktop.ui.theme.QuickGitTheme
import com.quickgit.desktop.ui.theme.detectSystemAccentColor
import com.quickgit.desktop.data.DesktopUiPrefs
import com.quickgit.desktop.ui.theme.detectSystemDarkTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.awt.Dimension
import java.net.URI

/**
 * Real application bootstrap. Invoked from the top-level [QuickGitNEXT] entry
 * so the process / WM_CLASS name is QuickGitNEXT on Linux.
 */
fun bootstrap(args: Array<String> = emptyArray()) {
    System.setProperty("awt.application.name", "QuickGitNEXT")
    System.setProperty("compose.application.name", "QuickGitNEXT")
    System.setProperty("apple.awt.application.name", "QuickGitNEXT")
    applyLinuxAppClassName("QuickGitNEXT")
    application {
        System.getProperty("quickgitnext.version")?.let {
            DesktopAppUpdateConfig.currentVersionName = it
        }

        // Default ~full laptop window; freely resizable down to half/quarter screen
        val windowState = rememberWindowState(width = 1280.dp, height = 840.dp)
        var menuScreen by remember { mutableStateOf<DesktopScreen?>(null) }
        val appIcon = painterResource("icon.png")
        val isLinux = remember {
            System.getProperty("os.name").orEmpty().lowercase().contains("linux")
        }
        val chromebookMode = remember { DesktopUiPrefs.isChromebookMode() }

        Window(
            onCloseRequest = ::exitApplication,
            title = "QuickGitNEXT",
            state = windowState,
            icon = appIcon,
            // Chrome-style: no system title bar in Chromebook mode
            undecorated = chromebookMode,
            transparent = false
        ) {
            // Allow half-screen and smaller laptops; content layouts adapt below this
            window.minimumSize = Dimension(480, 360)
            window.isResizable = true
            LaunchedEffect(Unit) {
                runCatching {
                    val stream = object {}.javaClass.getResourceAsStream("/icon.png")
                        ?: Thread.currentThread().contextClassLoader.getResourceAsStream("icon.png")
                    if (stream != null) {
                        stream.use { input ->
                            val base = javax.imageio.ImageIO.read(input)
                            if (base != null) {
                                val sizes = listOf(16, 24, 32, 48, 64, 128, 256)
                                window.iconImages = sizes.map { s ->
                                    val scaled = java.awt.image.BufferedImage(
                                        s, s, java.awt.image.BufferedImage.TYPE_INT_ARGB
                                    )
                                    val g = scaled.createGraphics()
                                    g.setRenderingHint(
                                        java.awt.RenderingHints.KEY_INTERPOLATION,
                                        java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR
                                    )
                                    g.drawImage(base, 0, 0, s, s, null)
                                    g.dispose()
                                    scaled
                                }
                            }
                        }
                    }
                }
            }

            // Native MenuBar: skip on Linux (themed bar) and whenever Chromebook shell is active
            if (!isLinux && !chromebookMode) {
                MenuBar {
                    Menu("File", mnemonic = 'F') {
                        Item("Quit", onClick = ::exitApplication, shortcut = KeyShortcut(Key.Q, ctrl = true))
                    }
                    Menu("Repositories", mnemonic = 'R') {
                        Item(
                            "All repositories",
                            onClick = { menuScreen = DesktopScreen.RepoList },
                            shortcut = KeyShortcut(Key.One, ctrl = true)
                        )
                        Separator()
                        Item(
                            "Clone…",
                            onClick = { menuScreen = DesktopScreen.Clone },
                            shortcut = KeyShortcut(Key.N, ctrl = true)
                        )
                        Item("Browse…", onClick = { menuScreen = DesktopScreen.BrowseAccount })
                        Item(
                            "Search…",
                            onClick = { menuScreen = DesktopScreen.UserSearch },
                            shortcut = KeyShortcut(Key.F, ctrl = true)
                        )
                    }
                    Menu("Identity", mnemonic = 'I') {
                        Item("Profiles", onClick = { menuScreen = DesktopScreen.Profile })
                        Item("Credentials…", onClick = { menuScreen = DesktopScreen.Credentials })
                    }
                    Menu("Options", mnemonic = 'O') {
                        Item(
                            "Settings…",
                            onClick = { menuScreen = DesktopScreen.Settings },
                            shortcut = KeyShortcut(Key.Comma, ctrl = true)
                        )
                        Item("Logs", onClick = { menuScreen = DesktopScreen.Logs })
                    }
                    Menu("Help", mnemonic = 'H') {
                        Item(
                            "GitHub repository",
                            onClick = {
                                runCatching {
                                    Desktop.getDesktop().browse(URI("https://github.com/QuickGitNEXT/QuickGit-NEXT"))
                                }
                            }
                        )
                        Item(
                            "Report an issue",
                            onClick = {
                                runCatching {
                                    Desktop.getDesktop().browse(URI("https://github.com/QuickGitNEXT/QuickGit-NEXT/issues"))
                                }
                            }
                        )
                    }
                }
            }

            LaunchedEffect(Unit) {
                AppLog.i("Main", "QuickGitNEXT desktop started (v" + DesktopAppUpdateConfig.currentVersionName + ")")
            }
            val credentialStore = remember { DesktopCredentialStore() }
            val repoManager = remember { DesktopRepoManager(credentialStore) }
            val updateManager = remember { DesktopAppUpdateManager(credentialStore) }

            var darkTheme by remember { mutableStateOf(detectSystemDarkTheme()) }
            var accentKey by remember { mutableStateOf(detectSystemAccentColor()?.value ?: 0u) }
            SideEffect {
                System.setProperty("quickgitnext.runtime.dark", darkTheme.toString())
            }
            LaunchedEffect(Unit) {
                while (true) {
                    kotlinx.coroutines.delay(800)
                    val next = detectSystemDarkTheme()
                    if (next != darkTheme) darkTheme = next
                    val nextAccent = detectSystemAccentColor()?.value ?: 0u
                    if (nextAccent != accentKey) accentKey = nextAccent
                }
            }
            key(darkTheme, accentKey) {
                QuickGitTheme(
                    chromeBrowserTheme = chromebookMode,
                    darkTheme = darkTheme
                ) {
                    val bg = MaterialTheme.colorScheme.background
                    LaunchedEffect(bg) {
                        runCatching {
                            val rgb = bg.toArgb()
                            window.background = java.awt.Color(rgb, true)
                            window.contentPane?.background = java.awt.Color(rgb, true)
                        }
                    }
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = bg
                    ) {
                        var updatePrompt by remember { mutableStateOf<DesktopUpdateCheckResult.Available?>(null) }

                        LaunchedEffect(Unit) {
                            if (updateManager.isAutoPromptSuppressed()) return@LaunchedEffect
                            val result = withContext(Dispatchers.IO) { updateManager.checkForUpdate() }
                            if (result is DesktopUpdateCheckResult.Available) {
                                updatePrompt = result
                            }
                        }

                        Column(Modifier.fillMaxSize()) {
                            if (isLinux && !chromebookMode) {
                                LinuxThemedMenuBar(
                                    onNavigate = { menuScreen = it },
                                    onQuit = ::exitApplication
                                )
                            }
                            Box(Modifier.weight(1f).fillMaxWidth()) {
                                QuickGitDesktopApp(
                                    repoManager = repoManager,
                                    credentialStore = credentialStore,
                                    updateManager = updateManager,
                                    externalScreen = menuScreen,
                                    onWindowMinimize = if (chromebookMode) ({ windowState.isMinimized = true }) else null,
                                    onWindowMaximize = if (chromebookMode) ({
                                        windowState.placement =
                                            if (windowState.placement == WindowPlacement.Maximized)
                                                WindowPlacement.Floating
                                            else WindowPlacement.Maximized
                                    }) else null,
                                    onWindowClose = if (chromebookMode) ({ exitApplication() }) else null,
                                    onWindowDrag = if (chromebookMode) ({ dx, dy ->
                                        val p = windowState.position
                                        // WindowPosition may be Absolute
                                        runCatching {
                                            val x = (window.x + dx).toInt()
                                            val y = (window.y + dy).toInt()
                                            window.setLocation(x, y)
                                        }
                                    }) else null,
                                    onScreenChanged = { menuScreen = it }
                                )
                                updatePrompt?.let { available ->
                                    UpdateAvailableDialog(
                                        available = available,
                                        updateManager = updateManager,
                                        onDismiss = { updatePrompt = null }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * In-app menu strip for Linux. The platform [MenuBar] does not follow Material
 * dark/light colors; this bar uses surfaceContainer + onSurface instead.
 */
@Composable
private fun LinuxThemedMenuBar(
    onNavigate: (DesktopScreen) -> Unit,
    onQuit: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(36.dp)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            MenuDropdown("File") {
                DropdownMenuItem(
                    text = { Text("Quit") },
                    onClick = { it(); onQuit() },
                    trailingIcon = { Text("Ctrl+Q", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                )
            }
            MenuDropdown("Repositories") {
                DropdownMenuItem(
                    text = { Text("All repositories") },
                    onClick = { it(); onNavigate(DesktopScreen.RepoList) }
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Clone…") },
                    onClick = { it(); onNavigate(DesktopScreen.Clone) }
                )
                DropdownMenuItem(
                    text = { Text("Browse…") },
                    onClick = { it(); onNavigate(DesktopScreen.BrowseAccount) }
                )
                DropdownMenuItem(
                    text = { Text("Search…") },
                    onClick = { it(); onNavigate(DesktopScreen.UserSearch) }
                )
            }
            MenuDropdown("Identity") {
                DropdownMenuItem(
                    text = { Text("Profiles") },
                    onClick = { it(); onNavigate(DesktopScreen.Profile) }
                )
                DropdownMenuItem(
                    text = { Text("Credentials…") },
                    onClick = { it(); onNavigate(DesktopScreen.Credentials) }
                )
            }
            MenuDropdown("Options") {
                DropdownMenuItem(
                    text = { Text("Settings…") },
                    onClick = { it(); onNavigate(DesktopScreen.Settings) }
                )
                DropdownMenuItem(
                    text = { Text("Logs") },
                    onClick = { it(); onNavigate(DesktopScreen.Logs) }
                )
            }
            MenuDropdown("Help") {
                DropdownMenuItem(
                    text = { Text("GitHub repository") },
                    onClick = {
                        it()
                        runCatching {
                            Desktop.getDesktop().browse(URI("https://github.com/QuickGitNEXT/QuickGit-NEXT"))
                        }
                    }
                )
                DropdownMenuItem(
                    text = { Text("Report an issue") },
                    onClick = {
                        it()
                        runCatching {
                            Desktop.getDesktop().browse(URI("https://github.com/QuickGitNEXT/QuickGit-NEXT/issues"))
                        }
                    }
                )
            }
        }
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

@Composable
private fun MenuDropdown(
    label: String,
    content: @Composable (dismiss: () -> Unit) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .clickable { expanded = true }
                .padding(horizontal = 10.dp, vertical = 8.dp)
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(6.dp))
        ) {
            content { expanded = false }
        }
    }
}

/** Set AWT X11/Wayland app class so dock/taskbar do not show MainKt. */
private fun applyLinuxAppClassName(name: String) {
    runCatching {
        val toolkit = java.awt.Toolkit.getDefaultToolkit()
        val field = toolkit.javaClass.getDeclaredField("awtAppClassName")
        field.trySetAccessible()
        field.set(toolkit, name)
    }
}

@Composable
private fun UpdateAvailableDialog(
    available: DesktopUpdateCheckResult.Available,
    updateManager: DesktopAppUpdateManager,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(-1) }
    var status by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Update available") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "QuickGitNEXT " + available.latest.versionName + " is available " +
                        "(you have " + available.current.versionName + ")."
                )
                Text(
                    "Package: " + available.format.label + " — " + available.assetName.ifBlank { available.format.suffix },
                    style = MaterialTheme.typography.bodySmall
                )
                if (available.releaseNotes.isNotBlank()) {
                    Text(
                        available.releaseNotes.take(400),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                status?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
                if (progress in 0..100) {
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (available.downloadUrl.isBlank()) {
                        updateManager.openReleasePage()
                        onDismiss()
                        return@Button
                    }
                    scope.launch {
                        busy = true
                        status = "Downloading…"
                        progress = 0
                        val result = withContext(Dispatchers.IO) {
                            updateManager.downloadAndInstall(available) { p -> progress = p }
                        }
                        busy = false
                        progress = -1
                        when (result) {
                            is DesktopInstallResult.Success -> {
                                status = result.message
                            }
                            is DesktopInstallResult.Error -> {
                                status = result.message
                            }
                        }
                    }
                },
                enabled = !busy
            ) {
                Text(
                    when {
                        busy && progress in 0..99 -> "Downloading $progress%"
                        busy -> "Installing…"
                        available.downloadUrl.isBlank() -> "Open Releases"
                        else -> "Download & install"
                    }
                )
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = {
                        updateManager.snoozeForDays(7)
                        onDismiss()
                    },
                    enabled = !busy
                ) { Text("Later") }
                TextButton(
                    onClick = onDismiss,
                    enabled = !busy
                ) { Text(if (status != null && !busy) "Close" else "Cancel") }
            }
        }
    )
}
