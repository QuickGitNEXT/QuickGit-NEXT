package com.quickgit.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
// wrapContentWidth via foundation.layout
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quickgit.app.data.models.FileChange
import com.quickgit.app.data.models.GitOpResult
import com.quickgit.app.ui.theme.GitAmber
import com.quickgit.app.ui.theme.GitGreen
import com.quickgit.app.ui.components.ExpressiveTaskOverlay
import com.quickgit.app.ui.components.FloatingCircleIconButton
import com.quickgit.app.ui.components.AppBrandLogo
import com.quickgit.app.viewmodel.BranchesViewModel
import com.quickgit.app.viewmodel.FilesViewModel
import com.quickgit.app.viewmodel.HistoryViewModel
import com.quickgit.app.viewmodel.IssuesViewModel
import com.quickgit.app.viewmodel.PullRequestsViewModel
import com.quickgit.app.viewmodel.ReleasesViewModel
import com.quickgit.app.viewmodel.RepoDetailViewModel
import com.quickgit.app.viewmodel.ViewModelFactory
import com.quickgit.app.viewmodel.WorkflowsViewModel

/**
 * Repo detail screen for tablet / Chromebook / desktop-window sized Android windows.
 *
 * Structured identically to the Linux/Mac desktop app: top bar (back, repo name, Pull/Push
 * with force-push options) and a [TabRow] of Changes / Files / Branches / Issues / PRs /
 * Actions / Releases / History. Force push (with lease / no lease) matches the phone app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoDetailScreenDesktop(
    repoPath: String,
    repoName: String,
    vm: RepoDetailViewModel,
    onBack: () -> Unit,
    onOpenDiff: (filePath: String, mode: String) -> Unit,
    onOpenFile: (filePath: String) -> Unit,
    onConflicts: () -> Unit,
    onNeedsAuth: (String) -> Unit
) {
    val context = LocalContext.current
    val factory = remember { ViewModelFactory(context.applicationContext as android.app.Application) }

    val branchesVm: BranchesViewModel = viewModel(factory = factory)
    val filesVm: FilesViewModel = viewModel(factory = factory)
    val issuesVm: IssuesViewModel = viewModel(factory = factory)
    val pullRequestsVm: PullRequestsViewModel = viewModel(factory = factory)
    val workflowsVm: WorkflowsViewModel = viewModel(factory = factory)
    val releasesVm: ReleasesViewModel = viewModel(factory = factory)
    val historyVm: HistoryViewModel = viewModel(factory = factory)

    LaunchedEffect(repoPath) {
        issuesVm.init(repoPath)
        pullRequestsVm.init(repoPath)
        workflowsVm.init(repoPath)
        releasesVm.init(repoPath)
    }

    val state by vm.state.collectAsState()
    val branchesState by branchesVm.state.collectAsState()
    // After a successful checkout on the Branches tab, reset History to the new HEAD and
    // refresh the detail status so the branch label updates.
    LaunchedEffect(branchesState.checkoutGeneration) {
        if (branchesState.checkoutGeneration > 0) {
            historyVm.onBranchChanged()
            vm.refresh()
        }
    }
    val snackbarHost = remember { SnackbarHostState() }
    var showTaskSuccess by remember { mutableStateOf(false) }
    var pullMenuExpanded by remember { mutableStateOf(false) }
    var showStatusOptions by remember { mutableStateOf(false) }
    var showLfsOptions by remember { mutableStateOf(false) }
    var pushMenuExpanded by remember { mutableStateOf(false) }
    var lfsMenuExpanded by remember { mutableStateOf(false) }
    var showForcePushConfirm by remember { mutableStateOf(false) }
    var forcePushUseLease by remember { mutableStateOf(true) }
    var showLfsTrack by remember { mutableStateOf(false) }
    var showLfsUntrack by remember { mutableStateOf(false) }
    var lfsTrackPattern by remember { mutableStateOf("*.psd") }
    var lfsUntrackPattern by remember { mutableStateOf("*.psd") }

    LaunchedEffect(state.lastResult, state.statusMessage) {
        state.statusMessage?.let {
            snackbarHost.showSnackbar(it)
            vm.consumeResult()
            return@LaunchedEffect
        }
        when (val r = state.lastResult) {
            is GitOpResult.Success -> { showTaskSuccess = true }
            is GitOpResult.UpToDate -> { snackbarHost.showSnackbar(r.message); vm.consumeResult() }
            is GitOpResult.Error -> { snackbarHost.showSnackbar(r.message); vm.consumeResult() }
            is GitOpResult.AuthRequired -> { vm.consumeResult(); onNeedsAuth(r.remoteUrl) }
            is GitOpResult.Conflict -> { vm.consumeResult(); onConflicts() }
            null -> {}
        }
    }

    // Changes first, then Files, Branches, Issues, PRs, Actions, Releases, History last — same order as desktop.
    val tabs = listOf("Changes", "Files", "Branches", "Issues", "PRs", "Actions", "History")
    var tabIndex by remember { mutableStateOf(0) }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            // Top: back left · tab bar truly centered · refresh right
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(
                    Modifier.align(Alignment.CenterStart),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FloatingCircleIconButton(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        onClick = onBack
                    )
                    AppBrandLogo(size = 36.dp)
                }

                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    tonalElevation = 4.dp,
                    shadowElevation = 10.dp,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .wrapContentWidth()
                ) {
                    Row(
                        Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        tabs.forEachIndexed { i, title ->
                            val selected = tabIndex == i
                            TextButton(
                                onClick = { tabIndex = i },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            ) {
                                Text(
                                    title,
                                    maxLines = 1,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (selected) {
                                        androidx.compose.ui.text.font.FontWeight.SemiBold
                                    } else {
                                        androidx.compose.ui.text.font.FontWeight.Normal
                                    }
                                )
                            }
                        }
                    }
                }

                Row(
                    Modifier.align(Alignment.CenterEnd),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state.busy) {
                        CircularProgressIndicator(
                            Modifier.padding(end = 4.dp).size(20.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    FloatingCircleIconButton(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        onClick = { vm.refresh() }
                    )
                }
            }

            // Repo name + branch on the left; bulk staging on the right (Changes tab)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        repoName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1
                    )
                    Text(
                        state.branch,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                if (tabIndex == 0) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = { vm.stageAll() },
                            enabled = !state.busy
                        ) { Text("Stage all") }
                        OutlinedButton(
                            onClick = { vm.unstageAll() },
                            enabled = !state.busy
                        ) { Text("Unstage all") }
                        OutlinedButton(
                            onClick = { vm.discardAll() },
                            enabled = !state.busy
                        ) { Text("Discard") }
                    }
                }
            }

            Box(Modifier.weight(1f).fillMaxSize()) {
                when (tabIndex) {

                    0 -> ChangesTabDesktop(vm)
                    1 -> FilesScreen(repoPath = repoPath, vm = filesVm, onBack = null, onOpenFile = onOpenFile)
                    2 -> BranchesScreen(repoPath = repoPath, vm = branchesVm, onBack = null)
                    3 -> IssuesScreen(vm = issuesVm, onBack = null, onNeedsAuth = onNeedsAuth)
                    4 -> PullRequestsScreen(vm = pullRequestsVm, onBack = null, onNeedsAuth = onNeedsAuth)
                    5 -> WorkflowsScreen(
                        vm = workflowsVm,
                        onBack = null,
                        onNeedsAuth = onNeedsAuth,
                        repoPath = repoPath,
                        showReleasesTab = true
                    )
                    6 -> HistoryScreen(repoPath = repoPath, vm = historyVm, onBack = null)
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 3.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
                modifier = Modifier.wrapContentWidth()
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        TextButton(onClick = { pullMenuExpanded = true }, enabled = !state.busy) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = "Pull")
                                Text("Pull", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        DropdownMenu(expanded = pullMenuExpanded, onDismissRequest = { pullMenuExpanded = false }) {
                            DropdownMenuItem(text = { Text("Git pull") }, onClick = { pullMenuExpanded = false; vm.pull() })
                            DropdownMenuItem(text = { Text("Pull with rebase") }, onClick = { pullMenuExpanded = false; vm.pullRebase() })
                            DropdownMenuItem(text = { Text("LFS pull only") }, onClick = { pullMenuExpanded = false; vm.fetchLfs() })
                            DropdownMenuItem(text = { Text("Git pull + LFS") }, onClick = { pullMenuExpanded = false; vm.pullWithLfs() })
                        }
                    }
                    Box {
                        TextButton(onClick = { pushMenuExpanded = true }, enabled = !state.busy) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = "Push")
                                Text("Push", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        DropdownMenu(expanded = pushMenuExpanded, onDismissRequest = { pushMenuExpanded = false }) {
                            DropdownMenuItem(text = { Text("Git push") }, onClick = { pushMenuExpanded = false; vm.push(force = false) })
                            DropdownMenuItem(text = { Text("Push with rebase") }, onClick = { pushMenuExpanded = false; vm.pushRebase() })
                            if (state.isGerritRemote) {
                                DropdownMenuItem(text = { Text("Push for review") }, onClick = { pushMenuExpanded = false; vm.pushForReview() })
                            }
                            DropdownMenuItem(text = { Text("LFS push only") }, onClick = { pushMenuExpanded = false; vm.pushLfs() })
                            DropdownMenuItem(text = { Text("Git push + LFS") }, onClick = { pushMenuExpanded = false; vm.pushWithLfs() })
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Force with lease…", color = com.quickgit.app.ui.theme.GitRed) },
                                onClick = { pushMenuExpanded = false; forcePushUseLease = true; showForcePushConfirm = true }
                            )
                            DropdownMenuItem(
                                text = { Text("Force (no lease)…", color = com.quickgit.app.ui.theme.GitRed) },
                                onClick = { pushMenuExpanded = false; forcePushUseLease = false; showForcePushConfirm = true }
                            )
                        }
                    }
                    TextButton(onClick = { showStatusOptions = true }, enabled = !state.busy) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Info, contentDescription = "Status")
                            Text("Status", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    TextButton(onClick = { showLfsOptions = true }, enabled = !state.busy) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Storage, contentDescription = "LFS")
                            Text("LFS", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            } // center Box for ops bar
        }
    }

    if (showStatusOptions) {
        AlertDialog(
            onDismissRequest = { showStatusOptions = false },
            title = { Text("Git status") },
            text = {
                Text(
                    state.status?.let { s ->
                        "Branch: ${state.branch}\nStaged: ${s.staged.size}\nUnstaged: ${s.unstaged.size}\nUntracked: ${s.untracked.size}" +
                            if (s.conflicting.isNotEmpty()) "\nConflicts: ${s.conflicting.size}" else ""
                    } ?: "No status loaded yet."
                )
            },
            confirmButton = {
                TextButton(onClick = { showStatusOptions = false; vm.refresh() }) { Text("Refresh") }
            },
            dismissButton = {
                TextButton(onClick = { showStatusOptions = false }) { Text("Close") }
            }
        )
    }
    if (showLfsOptions) {
        AlertDialog(
            onDismissRequest = { showLfsOptions = false },
            title = { Text("Git LFS") },
            text = {
                Column {
                    TextButton(onClick = { showLfsOptions = false; showLfsTrack = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Track pattern…")
                    }
                    TextButton(onClick = { showLfsOptions = false; vm.fetchLfs() }, modifier = Modifier.fillMaxWidth()) {
                        Text("LFS fetch/pull")
                    }
                    TextButton(onClick = { showLfsOptions = false; vm.pushLfs() }, modifier = Modifier.fillMaxWidth()) {
                        Text("LFS push")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLfsOptions = false }) { Text("Close") }
            }
        )
    }

    if (showLfsTrack) {
        AlertDialog(
            onDismissRequest = { showLfsTrack = false },
            title = { Text("Track with Git LFS") },
            text = {
                Column {
                    Text(
                        "Files matching this pattern will be stored in LFS when staged.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = lfsTrackPattern,
                        onValueChange = { lfsTrackPattern = it },
                        label = { Text("Pattern (e.g. *.psd)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showLfsTrack = false
                    vm.lfsTrack(lfsTrackPattern)
                }) { Text("Track") }
            },
            dismissButton = {
                TextButton(onClick = { showLfsTrack = false }) { Text("Cancel") }
            }
        )
    }

    if (showLfsUntrack) {
        AlertDialog(
            onDismissRequest = { showLfsUntrack = false },
            title = { Text("Untrack from Git LFS") },
            text = {
                Column {
                    Text(
                        "Remove this pattern from .gitattributes (stops new files from using LFS).",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = lfsUntrackPattern,
                        onValueChange = { lfsUntrackPattern = it },
                        label = { Text("Pattern (e.g. *.psd)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showLfsUntrack = false
                    vm.lfsUntrack(lfsUntrackPattern)
                }) { Text("Untrack") }
            },
            dismissButton = {
                TextButton(onClick = { showLfsUntrack = false }) { Text("Cancel") }
            }
        )
    }

    if (showForcePushConfirm) {
        AlertDialog(
            onDismissRequest = { showForcePushConfirm = false },
            title = { Text("Force push") },
            text = {
                Column {
                    Text(
                        "Choose how to overwrite the remote branch:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        onClick = { forcePushUseLease = true },
                        shape = MaterialTheme.shapes.medium,
                        color = if (forcePushUseLease) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = forcePushUseLease,
                                onClick = { forcePushUseLease = true }
                            )
                            Column(Modifier.weight(1f)) {
                                Text("Force with lease", fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Only if remote still matches your last fetch (safer)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        onClick = { forcePushUseLease = false },
                        shape = MaterialTheme.shapes.medium,
                        color = if (!forcePushUseLease) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = !forcePushUseLease,
                                onClick = { forcePushUseLease = false }
                            )
                            Column(Modifier.weight(1f)) {
                                Text("Force (no lease)", fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Always overwrite — can discard others’ commits",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showForcePushConfirm = false
                        if (forcePushUseLease) {
                            vm.push(forceWithLease = true)
                        } else {
                            vm.push(force = true)
                        }
                    },
                    colors = if (forcePushUseLease) {
                        ButtonDefaults.buttonColors()
                    } else {
                        ButtonDefaults.buttonColors(
                            containerColor = com.quickgit.app.ui.theme.GitRed
                        )
                    }
                ) {
                    Text(if (forcePushUseLease) "Force with lease" else "Force push")
                }
            },
            dismissButton = {
                TextButton(onClick = { showForcePushConfirm = false }) { Text("Cancel") }
            }
        )
    }

        ExpressiveTaskOverlay(
            visible = state.busy,
            label = state.busyLabel.ifBlank { if (state.busy) "Working…" else "" },
            success = showTaskSuccess,
            onSuccessConsumed = {
                showTaskSuccess = false
                vm.consumeResult()
            }
        )
    } // Box
}

/**
 * Changes tab for tablet / rail layout.
 *
 * Shows **Staged** and **Changes** as separate sections so staging is obvious,
 * Staging bulk actions live in the tablet header; this tab lists staged/unstaged files.
 */
@Composable
private fun ChangesTabDesktop(vm: RepoDetailViewModel) {
    val state by vm.state.collectAsState()
    val status = state.status
    var selected by remember { mutableStateOf(setOf<String>()) }

    val staged = status?.staged.orEmpty()
    val unstagedAll = status?.unstaged.orEmpty() + status?.untracked.orEmpty() + status?.conflicting.orEmpty()
    val stagedPaths = remember(staged) { staged.map { it.path }.toSet() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        if (state.busy && status == null) {
            CircularProgressIndicator()
        } else if (staged.isEmpty() && unstagedAll.isEmpty()) {
            Text("Working tree clean", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (staged.isNotEmpty()) {
                    item(key = "hdr_staged") {
                        Text(
                            "Staged changes (${staged.size})",
                            style = MaterialTheme.typography.labelLarge,
                            color = GitGreen,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(staged, key = { "s_${it.path}" }) { fc ->
                        DesktopChangeRow(
                            fc = fc,
                            isStaged = true,
                            isSelected = fc.path in selected,
                            onSelect = {
                                selected = if (fc.path in selected) selected - fc.path else selected + fc.path
                            }
                        )
                    }
                }
                if (unstagedAll.isNotEmpty()) {
                    item(key = "hdr_unstaged") {
                        Text(
                            "Changes (${unstagedAll.size})",
                            style = MaterialTheme.typography.labelLarge,
                            color = GitAmber,
                            modifier = Modifier.padding(top = if (staged.isNotEmpty()) 12.dp else 0.dp, bottom = 8.dp)
                        )
                    }
                    items(unstagedAll, key = { "u_${it.path}" }) { fc ->
                        DesktopChangeRow(
                            fc = fc,
                            isStaged = false,
                            isSelected = fc.path in selected,
                            onSelect = {
                                selected = if (fc.path in selected) selected - fc.path else selected + fc.path
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = state.commitMessage,
            onValueChange = vm::setCommitMessage,
            label = { Text("Commit message") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4
        )
        Button(
            onClick = vm::commit,
            enabled = state.commitMessage.isNotBlank() && staged.isNotEmpty() && !state.busy,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(if (staged.isEmpty()) "Commit (nothing staged)" else "Commit (${staged.size})")
        }
    }
}

@Composable
private fun DesktopChangeRow(
    fc: FileChange,
    isStaged: Boolean,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    isStaged -> GitGreen.copy(alpha = 0.10f)
                    else -> Color.Transparent
                }
            )
            .clickable(onClick = onSelect)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = isSelected, onCheckedChange = { onSelect() })
        Text(
            if (isStaged) "S" else fc.type.badge(),
            color = if (isStaged) GitGreen else fc.type.color(),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(24.dp)
        )
        Column(Modifier.weight(1f)) {
            Text(fc.path, maxLines = 1)
            if (isStaged) {
                Text(
                    "Staged · ${fc.type.name.lowercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = GitGreen
                )
            }
        }
    }
}
