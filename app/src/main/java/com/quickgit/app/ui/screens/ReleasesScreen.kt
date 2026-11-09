package com.quickgit.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quickgit.app.data.models.Release
import com.quickgit.app.data.models.ReleaseAsset
import com.quickgit.app.ui.theme.GitAmber
import com.quickgit.app.ui.theme.GitGreen
import com.quickgit.app.viewmodel.ReleasesViewModel
import com.quickgit.app.ui.components.AppScreenTitle
import com.quickgit.app.ui.components.FloatingCircleIconButton
import com.quickgit.app.ui.components.BrandLoadingPlaceholder
import com.quickgit.app.ui.components.PullToRefreshBox
import com.quickgit.app.ui.components.isLargePhoneForFloatingNav
import com.quickgit.app.ui.security.isChromebookDevice
import com.quickgit.app.ui.components.LocalChromeOsPageHeader
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReleasesScreen(
    vm: ReleasesViewModel,
    onBack: (() -> Unit)? = null,
    onNeedsAuth: (String) -> Unit,
    /** When true (hosted under Actions tabs), omit the outer top app bar. */
    embedded: Boolean = false,
    onOpenIssues: (() -> Unit)? = null
) {
    val state by vm.state.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingRelease by remember { mutableStateOf<Release?>(null) }
    val context = LocalContext.current
    val phoneNavMode by (context.applicationContext as com.quickgit.app.QuickGitApp)
        .appPreferences.phoneNavMode.collectAsState()
    // Compact phones only: + lives top-right. Large-phone chrome is unchanged.
    val compactPhone = when (phoneNavMode) {
        com.quickgit.app.data.PhoneNavMode.COMPACT -> true
        com.quickgit.app.data.PhoneNavMode.LARGE -> false
        com.quickgit.app.data.PhoneNavMode.AUTO ->
            !context.isLargePhoneForFloatingNav()
    }

    LaunchedEffect(state.errorMessage, state.statusMessage, state.authRequiredHost) {
        state.errorMessage?.let { snackbarHost.showSnackbar(it); vm.consumeMessages() }
        state.statusMessage?.let { snackbarHost.showSnackbar(it); vm.consumeMessages() }
        state.authRequiredHost?.let { onNeedsAuth("https://$it/"); vm.consumeMessages() }
    }

    if (state.selected != null) {
        BackHandler { vm.closeDetail() }
        ReleaseDetailContent(
            release = state.selected!!,
            loading = state.detailLoading,
            onBack = { vm.closeDetail() },
            onEdit = { editingRelease = state.selected }
        )
        return
    }

    if (!state.supported) {
        Scaffold(
            topBar = {
                if (onBack != null) CenterAlignedTopAppBar(
                    title = { AppScreenTitle("Releases", showLogo = true) },
                    navigationIcon = {
                    onBack?.let { back ->
                        FloatingCircleIconButton(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            onClick = back
                        )
                    }
                }
                )
            }
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    "Releases are only available for repositories whose origin points at github.com.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            if (!embedded) {
                CenterAlignedTopAppBar(
                    title = { AppScreenTitle("Releases", showLogo = true) },
                    navigationIcon = {
                        onBack?.let { back ->
                            FloatingCircleIconButton(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                onClick = back
                            )
                        }
                    },
                    actions = {
                        if (compactPhone && state.supported) {
                            FloatingCircleIconButton(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New release",
                                onClick = { showCreateDialog = true }
                            )
                        } else if (onOpenIssues != null) {
                            FloatingCircleIconButton(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = "Issues",
                                onClick = onOpenIssues
                            )
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            // Large phones keep the bottom + FAB; compact moves + to the top-right circle.
            if (state.supported && !compactPhone) {
                FloatingActionButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "New release")
                }
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.loading,
            onRefresh = { vm.refresh() },
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            when {
                state.loading && state.releases.isEmpty() -> {
                    BrandLoadingPlaceholder(modifier = Modifier.align(Alignment.Center))
                }
                state.releases.isEmpty() -> {
                    Text(
                        "No releases published yet.",
                        Modifier.align(Alignment.Center).padding(32.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.releases, key = { it.id }) { release ->
                            ReleaseCard(release = release, onClick = { vm.openDetail(release.id) })
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateOrEditReleaseDialog(
            title = "New release",
            initialTag = "",
            initialName = "",
            initialBody = "",
            initialDraft = false,
            initialPrerelease = false,
            busy = state.busy,
            allowAssets = true,
            onDismiss = { showCreateDialog = false },
            onConfirm = { tag, name, body, draft, prerelease, target, assets ->
                vm.createRelease(tag, name, body, draft, prerelease, target, assets)
                showCreateDialog = false
            }
        )
    }

    editingRelease?.let { rel ->
        CreateOrEditReleaseDialog(
            title = "Edit release",
            initialTag = rel.tagName,
            initialName = rel.name,
            initialBody = rel.body.orEmpty(),
            initialDraft = rel.draft,
            initialPrerelease = rel.prerelease,
            busy = state.busy,
            allowAssets = false,
            onDismiss = { editingRelease = null },
            onConfirm = { tag, name, body, draft, prerelease, _, _ ->
                vm.updateRelease(rel.id, tag, name, body, draft, prerelease)
                editingRelease = null
            }
        )
    }
}

@Composable
private fun ReleaseCard(release: Release, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.NewReleases,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (release.prerelease) GitAmber else GitGreen
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    release.name.ifBlank { release.tagName },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (release.draft) {
                    AssistChip(onClick = {}, label = { Text("Draft") }, enabled = false)
                } else if (release.prerelease) {
                    AssistChip(onClick = {}, label = { Text("Pre-release") }, enabled = false)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                buildString {
                    append(release.tagName)
                    if (!release.authorLogin.isNullOrBlank()) append(" · ${release.authorLogin}")
                    val date = release.publishedAt ?: release.createdAt
                    if (date.isNotBlank()) append(" · ${formatIso(date)}")
                    if (release.assets.isNotEmpty()) append(" · ${release.assets.size} asset(s)")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReleaseDetailContent(
    release: Release,
    loading: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit = {}
) {
    val context = LocalContext.current
    val hardwareChromebook = context.isChromebookDevice()
    val wideUiMode by (context.applicationContext as com.quickgit.app.QuickGitApp)
        .appPreferences.wideUiMode.collectAsState()
    val chromeShell =
        hardwareChromebook || wideUiMode == com.quickgit.app.data.WideUiMode.CHROMEBOOK
    val pageHeader = LocalChromeOsPageHeader.current

    // ChromeOS shell: put release name/tag + Edit on the parent repo/branch line.
    if (chromeShell) {
        DisposableEffect(release.id, release.name, release.tagName) {
            pageHeader.show(
                title = release.name.ifBlank { release.tagName },
                subtitle = release.tagName,
                endActions = {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit release",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
            onDispose { pageHeader.clear() }
        }
    }

    fun openUrl(url: String?) {
        if (url.isNullOrBlank()) return
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            // No browser / activity available to handle the URI
        }
    }

    Scaffold(
        topBar = {
            // ChromeOS shell: no secondary title/back row
            if (!chromeShell) {
                CenterAlignedTopAppBar(
                    title = {
                        AppScreenTitle(
                            pageTitle = release.name.ifBlank { release.tagName },
                            subtitle = release.tagName,
                            showLogo = true
                        )
                    },
                    navigationIcon = {
                        FloatingCircleIconButton(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            onClick = onBack
                        )
                    },
                    actions = {
                        FloatingCircleIconButton(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit release",
                            onClick = onEdit
                        )
                    }
                )
            }
        }
    ) { padding ->
        if (loading) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                    BrandLoadingPlaceholder()
                }
            return@Scaffold
        }

        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (release.draft) {
                    AssistChip(onClick = {}, label = { Text("Draft") }, enabled = false)
                }
                if (release.prerelease) {
                    AssistChip(onClick = {}, label = { Text("Pre-release") }, enabled = false)
                }
                if (!release.draft && !release.prerelease) {
                    AssistChip(onClick = {}, label = { Text("Latest") }, enabled = false)
                }
            }

            Spacer(Modifier.height(12.dp))
            InfoLine("Tag", release.tagName)
            if (!release.authorLogin.isNullOrBlank()) InfoLine("Author", release.authorLogin)
            InfoLine("Published", formatIso(release.publishedAt ?: release.createdAt))
            if (release.htmlUrl.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = { openUrl(release.htmlUrl) }) {
                    Text("Open on GitHub")
                }
            }

            if (!release.body.isNullOrBlank()) {
                Spacer(Modifier.height(16.dp))
                Text("Notes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(release.body, style = MaterialTheme.typography.bodyMedium)
            }

            if (release.assets.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text(
                    "Assets (${release.assets.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                release.assets.forEach { asset ->
                    AssetRow(asset) {
                        openUrl(asset.browserDownloadUrl)
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }

            val zipUrl = release.zipballUrl
            val tarUrl = release.tarballUrl
            if (!zipUrl.isNullOrBlank() || !tarUrl.isNullOrBlank()) {
                Spacer(Modifier.height(16.dp))
                Text("Source code", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!zipUrl.isNullOrBlank()) {
                        OutlinedButton(onClick = { openUrl(zipUrl) }) {
                            Text("zip")
                        }
                    }
                    if (!tarUrl.isNullOrBlank()) {
                        OutlinedButton(onClick = { openUrl(tarUrl) }) {
                            Text("tar.gz")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AssetRow(asset: ReleaseAsset, onDownload: () -> Unit) {
    Card(onClick = onDownload, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    asset.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${formatBytes(asset.size)} · ${asset.downloadCount} downloads",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onDownload) { Text("Download") }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            label,
            Modifier.width(90.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun formatIso(iso: String): String =
    iso.replace("T", " ").removeSuffix("Z").take(19)

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
    bytes < 1024L * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
    else -> "%.1f GB".format(bytes / (1024.0 * 1024 * 1024))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateOrEditReleaseDialog(
    title: String,
    initialTag: String,
    initialName: String,
    initialBody: String,
    initialDraft: Boolean,
    initialPrerelease: Boolean,
    busy: Boolean,
    allowAssets: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (
        tag: String,
        name: String?,
        body: String?,
        draft: Boolean,
        prerelease: Boolean,
        targetCommitish: String?,
        assets: List<Pair<String, ByteArray>>
    ) -> Unit
) {
    var tag by remember { mutableStateOf(initialTag) }
    var name by remember { mutableStateOf(initialName) }
    var body by remember { mutableStateOf(initialBody) }
    var draft by remember { mutableStateOf(initialDraft) }
    var prerelease by remember { mutableStateOf(initialPrerelease) }
    var target by remember { mutableStateOf("") }
    var pendingAssets by remember { mutableStateOf<List<Pair<String, ByteArray>>>(emptyList()) }
    val context = LocalContext.current

    val pickFiles = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNullOrEmpty()) return@rememberLauncherForActivityResult
        val loaded = mutableListOf<Pair<String, ByteArray>>()
        for (uri in uris) {
            try {
                val nameGuess = uri.lastPathSegment?.substringAfterLast('/') ?: "asset.bin"
                context.contentResolver.openInputStream(uri)?.use { input ->
                    loaded += nameGuess to input.readBytes()
                }
            } catch (_: Exception) { /* skip unreadable */ }
        }
        if (loaded.isNotEmpty()) pendingAssets = pendingAssets + loaded
    }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(title) },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = tag,
                    onValueChange = { tag = it },
                    label = { Text("Tag name *") },
                    placeholder = { Text("v1.0.0") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Release title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy
                )
                if (allowAssets) {
                    OutlinedTextField(
                        value = target,
                        onValueChange = { target = it },
                        label = { Text("Target branch / SHA (optional)") },
                        placeholder = { Text("main") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !busy
                    )
                }
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("Description") },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = draft, onCheckedChange = { draft = it }, enabled = !busy)
                    Text("Draft")
                    Spacer(Modifier.width(16.dp))
                    Checkbox(checked = prerelease, onCheckedChange = { prerelease = it }, enabled = !busy)
                    Text("Pre-release")
                }
                if (allowAssets) {
                    TextButton(
                        onClick = { pickFiles.launch(arrayOf("*/*")) },
                        enabled = !busy
                    ) {
                        Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add files")
                    }
                    if (pendingAssets.isNotEmpty()) {
                        Text(
                            "${pendingAssets.size} file(s) selected:",
                            style = MaterialTheme.typography.bodySmall
                        )
                        pendingAssets.forEach { (n, bytes) ->
                            Text(
                                "• $n (${formatBytes(bytes.size.toLong())})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        tag.trim(),
                        name.trim().ifBlank { null },
                        body.ifBlank { null },
                        draft,
                        prerelease,
                        target.trim().ifBlank { null },
                        pendingAssets
                    )
                },
                enabled = !busy && tag.isNotBlank()
            ) {
                if (busy) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (allowAssets) "Create" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") }
        }
    )
}
