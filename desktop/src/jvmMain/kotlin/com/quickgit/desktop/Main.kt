package com.quickgit.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.quickgit.desktop.data.AppLog
import com.quickgit.desktop.data.DesktopAppUpdateConfig
import com.quickgit.desktop.data.DesktopAppUpdateManager
import com.quickgit.desktop.data.DesktopCredentialStore
import com.quickgit.desktop.data.DesktopRepoManager
import com.quickgit.desktop.data.DesktopUpdateCheckResult
import com.quickgit.desktop.ui.DesktopScreen
import com.quickgit.desktop.ui.QuickGitDesktopApp
import com.quickgit.desktop.ui.theme.QuickGitTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.awt.Dimension
import java.net.URI

fun main() = application {
    System.getProperty("quickgitnext.version")?.let {
        DesktopAppUpdateConfig.currentVersionName = it
    }

    val windowState = rememberWindowState(width = 1280.dp, height = 840.dp)
    var menuScreen by remember { mutableStateOf<DesktopScreen?>(null) }

    Window(
        onCloseRequest = ::exitApplication,
        title = "QuickGitNEXT",
        state = windowState
    ) {
        window.minimumSize = Dimension(900, 640)

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
                Item(
                    "Browse…",
                    onClick = { menuScreen = DesktopScreen.BrowseAccount }
                )
                Item(
                    "Search…",
                    onClick = { menuScreen = DesktopScreen.UserSearch },
                    shortcut = KeyShortcut(Key.F, ctrl = true)
                )
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

        LaunchedEffect(Unit) {
            AppLog.i("Main", "QuickGitNEXT desktop started (v" + DesktopAppUpdateConfig.currentVersionName + ")")
        }
        val credentialStore = remember { DesktopCredentialStore() }
        val repoManager = remember { DesktopRepoManager(credentialStore) }
        val updateManager = remember { DesktopAppUpdateManager(credentialStore) }

        QuickGitTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                var updatePrompt by remember { mutableStateOf<DesktopUpdateCheckResult.Available?>(null) }

                LaunchedEffect(Unit) {
                    if (updateManager.isAutoPromptSuppressed()) return@LaunchedEffect
                    val result = withContext(Dispatchers.IO) { updateManager.checkForUpdate() }
                    if (result is DesktopUpdateCheckResult.Available) {
                        updatePrompt = result
                    }
                }

                Box(Modifier.fillMaxSize()) {
                    QuickGitDesktopApp(
                        repoManager = repoManager,
                        credentialStore = credentialStore,
                        updateManager = updateManager,
                        externalScreen = menuScreen,
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

@Composable
private fun UpdateAvailableDialog(
    available: DesktopUpdateCheckResult.Available,
    updateManager: DesktopAppUpdateManager,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update available") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "QuickGitNEXT " + available.latest.versionName + " is on GitHub Releases " +
                        "(you have " + available.current.versionName + ")."
                )
                Text(
                    "Open the Releases page to download the package for your platform.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    updateManager.openReleasePage()
                    onDismiss()
                }
            ) { Text("Open Releases") }
        },
        dismissButton = {
            TextButton(onClick = {
                updateManager.snoozeForDays(7)
                onDismiss()
            }) { Text("Later") }
        }
    )
}
