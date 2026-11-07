package com.quickgit.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quickgit.app.data.models.ChangeType
import com.quickgit.app.data.models.FileChange
import com.quickgit.app.data.models.GitOpResult
import com.quickgit.app.ui.adaptive.AdaptiveContent
import com.quickgit.app.ui.adaptive.LocalWindowSizeClass
import com.quickgit.app.ui.components.isLargePhoneForFloatingNav
import com.quickgit.app.ui.components.BrandLoadingPlaceholder
import com.quickgit.app.ui.components.AppBrandLogo
import com.quickgit.app.ui.components.FloatingCircleIconButton
import com.quickgit.app.ui.components.AppScreenTitle
import com.quickgit.app.ui.components.ExpressiveTaskOverlay
import com.quickgit.app.ui.adaptive.isCompactHeight
import com.quickgit.app.ui.adaptive.isTabletOrWider
import com.quickgit.app.ui.components.PullToRefreshBox
import com.quickgit.app.ui.theme.GitAmber
import com.quickgit.app.ui.theme.GitGreen
import com.quickgit.app.ui.theme.GitRed
import com.quickgit.app.viewmodel.RepoDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoDetailScreen(
    repoPath: String,
    repoName: String,
    vm: RepoDetailViewModel,
    onBack: () -> Unit,
    onOpenDiff: (filePath: String, mode: String) -> Unit,
    onOpenFile: (filePath: String) -> Unit = {},
    onOpenHistory: () -> Unit,
    onOpenBranches: () -> Unit,
    onOpenFiles: () -> Unit,
    onOpenPullRequests: () -> Unit = {},
    onOpenIssues: () -> Unit = {},
    onOpenWorkflows: () -> Unit = {},
    onOpenReleases: () -> Unit = {},
    onConflicts: () -> Unit,
    onNeedsAuth: (String) -> Unit
) {
    // Tablet / Chromebook / desktop-window sized: use the same single-screen,
    // tabbed layout as the Linux/Mac desktop app instead of navigating to
    // separate screens.
    if (LocalWindowSizeClass.current.isTabletOrWider) {
        RepoDetailScreenDesktop(
            repoPath = repoPath,
            repoName = repoName,
            vm = vm,
            onBack = onBack,
            onOpenDiff = onOpenDiff,
            onOpenFile = onOpenFile,
            onConflicts = onConflicts,
            onNeedsAuth = onNeedsAuth
        )
        return
    }

    val state by vm.state.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    var showTaskSuccess by remember { mutableStateOf(false) }

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

    Box(Modifier.fillMaxSize()) {
        Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { padding ->
        val status = state.status
        AdaptiveContent(Modifier.padding(padding)) {
        Column(Modifier.fillMaxSize()) {

            // Back (left) + centered repo name / branch + History (right, icon only)
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 2.dp)
            ) {
                FloatingCircleIconButton(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
                Column(
                    Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AppScreenTitle(
                        pageTitle = repoName,
                        logoSize = 40.dp,
                        subtitle = state.branch
                    )
                }
                FloatingCircleIconButton(
                    imageVector = Icons.Default.History,
                    contentDescription = "History",
                    onClick = onOpenHistory,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }

            val largePhone = LocalContext.current.isLargePhoneForFloatingNav()
            // GitLab: MRs / Issues board / Build. GitHub: PRs / Issues / Actions.
            val prLabel = if (state.isGitLabRemote) "MRs" else "PRs"
            val issuesLabel = if (state.isGitLabRemote) "Board" else "Issues"
            val ciLabel = if (state.isGitLabRemote) "Build" else "Actions"
            // Small phones: full sub-page row under the title (incl. Releases).
            // Large phones: sub-pages sit here (Releases lives under Actions); git ops move above commit.
            if (!largePhone) {
                androidx.compose.foundation.lazy.LazyRow(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    item { SubPageButton(Icons.Default.FolderOpen, "Files", onOpenFiles) }
                    item { Spacer(Modifier.width(4.dp)) }
                    item { SubPageButton(Icons.Default.AccountTree, "Branches", onOpenBranches) }
                    item { Spacer(Modifier.width(4.dp)) }
                    item { SubPageButton(Icons.Default.CallMerge, prLabel, onOpenPullRequests) }
                    item { Spacer(Modifier.width(4.dp)) }
                    item { SubPageButton(Icons.Default.BugReport, issuesLabel, onOpenIssues) }
                    item { Spacer(Modifier.width(4.dp)) }
                    item { SubPageButton(Icons.Default.PlayCircle, ciLabel, onOpenWorkflows) }
                    item { Spacer(Modifier.width(4.dp)) }
                    item { SubPageButton(Icons.Default.NewReleases, "Releases", onOpenReleases) }
                }
            }

            var showLfsTrack by remember { mutableStateOf(false) }
            var lfsTrackPattern by remember { mutableStateOf("*.psd") }
            var showForcePushConfirm by remember { mutableStateOf(false) }
            // true = force-with-lease (safer default), false = unconditional force
            var forcePushUseLease by remember { mutableStateOf(true) }
            var showPullOptions by remember { mutableStateOf(false) }
            var showPushOptions by remember { mutableStateOf(false) }
            var showStatusOptions by remember { mutableStateOf(false) }
            var showLfsOptions by remember { mutableStateOf(false) }

            // Small phones: labeled floating git-ops bar under the title.
            // Large phones: same bar sits above the commit control instead.
            if (!largePhone) {
                RepoSubNavFloatingBar(
                    items = listOf(
                        SubNavItem(Icons.Default.ArrowDownward, "Pull") {
                            if (!state.busy) showPullOptions = true
                        },
                        SubNavItem(Icons.Default.ArrowUpward, "Push") {
                            if (!state.busy) showPushOptions = true
                        },
                        SubNavItem(Icons.Default.Info, "Status") {
                            if (!state.busy) showStatusOptions = true
                        },
                        SubNavItem(Icons.Default.Storage, "LFS") {
                            if (!state.busy) showLfsOptions = true
                        },
                    )
                )
            }

            if (showPullOptions) {
                androidx.compose.ui.window.Dialog(onDismissRequest = { showPullOptions = false }) {
                    Box(
                        Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.extraLarge,
                            tonalElevation = 6.dp,
                            modifier = Modifier
                                .widthIn(max = 400.dp)
                                .fillMaxWidth()
                        ) {
                            Column(
                                Modifier.padding(24.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Pull", style = MaterialTheme.typography.headlineSmall)
                                Spacer(Modifier.height(12.dp))
                                TextButton(
                                    onClick = { showPullOptions = false; vm.pull() },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Git pull") }
                                TextButton(
                                    onClick = { showPullOptions = false; vm.pullRebase() },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Pull with rebase") }
                                TextButton(
                                    onClick = { showPullOptions = false; vm.fetchLfs() },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("LFS pull only") }
                                TextButton(
                                    onClick = { showPullOptions = false; vm.pullWithLfs() },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Git pull + LFS") }
                                Spacer(Modifier.height(8.dp))
                                TextButton(onClick = { showPullOptions = false }) {
                                    Text("Cancel")
                                }
                            }
                        }
                    }
                }
            }

            if (showPushOptions) {
                androidx.compose.ui.window.Dialog(onDismissRequest = { showPushOptions = false }) {
                    Box(
                        Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.extraLarge,
                            tonalElevation = 6.dp,
                            modifier = Modifier
                                .widthIn(max = 400.dp)
                                .fillMaxWidth()
                        ) {
                            Column(
                                Modifier.padding(24.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Push", style = MaterialTheme.typography.headlineSmall)
                                Spacer(Modifier.height(12.dp))
                                TextButton(
                                    onClick = { showPushOptions = false; vm.push(force = false) },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Git push") }
                                TextButton(
                                    onClick = { showPushOptions = false; vm.pushRebase() },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Push with rebase") }
                                if (state.isGerritRemote) {
                                    TextButton(
                                        onClick = { showPushOptions = false; vm.pushForReview() },
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("Push for review") }
                                }
                                TextButton(
                                    onClick = { showPushOptions = false; vm.pushLfs() },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("LFS push only") }
                                TextButton(
                                    onClick = { showPushOptions = false; vm.pushWithLfs() },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Git push + LFS") }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Force push",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = GitRed
                                )
                                Spacer(Modifier.height(4.dp))
                                TextButton(
                                    onClick = {
                                        showPushOptions = false
                                        forcePushUseLease = true
                                        showForcePushConfirm = true
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.textButtonColors(contentColor = GitRed)
                                ) { Text("Force with lease…") }
                                TextButton(
                                    onClick = {
                                        showPushOptions = false
                                        forcePushUseLease = false
                                        showForcePushConfirm = true
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.textButtonColors(contentColor = GitRed)
                                ) { Text("Force (no lease)…") }
                                Spacer(Modifier.height(8.dp))
                                TextButton(onClick = { showPushOptions = false }) {
                                    Text("Cancel")
                                }
                            }
                        }
                    }
                }
            }

            if (showLfsOptions) {
                androidx.compose.ui.window.Dialog(onDismissRequest = { showLfsOptions = false }) {
                    Box(
                        Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.extraLarge,
                            tonalElevation = 6.dp,
                            modifier = Modifier
                                .widthIn(max = 400.dp)
                                .fillMaxWidth()
                        ) {
                            Column(
                                Modifier.padding(24.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Git LFS", style = MaterialTheme.typography.headlineSmall)
                                Spacer(Modifier.height(12.dp))
                                TextButton(
                                    onClick = { showLfsOptions = false; vm.lfsInstall() },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("LFS install") }
                                TextButton(
                                    onClick = { showLfsOptions = false; showLfsTrack = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("LFS track…") }
                                Spacer(Modifier.height(8.dp))
                                TextButton(onClick = { showLfsOptions = false }) {
                                    Text("Cancel")
                                }
                            }
                        }
                    }
                }
            }

            // Full status page (colorized) instead of a short snackbar
            state.statusDetail?.let { detail ->
                AlertDialog(
                    onDismissRequest = { vm.dismissStatusDetail() },
                    title = { Text("Git status") },
                    text = {
                        SelectionContainer {
                            Column(Modifier.verticalScroll(rememberScrollState()).heightIn(max = 480.dp)) {
                                detail.lines().forEach { line ->
                                    val color = when {
                                        line.startsWith("  new file:") || line.trimStart().startsWith("new file") -> GitGreen
                                        line.contains("modified:") -> GitAmber
                                        line.contains("deleted:") || line.contains("both modified") -> GitRed
                                        line.startsWith("  ") && line.trim().isNotEmpty() &&
                                            !line.contains(":") -> Color.Gray // untracked
                                        line.startsWith("Changes to be committed") -> GitGreen
                                        line.startsWith("Changes not staged") -> GitAmber
                                        line.startsWith("Untracked") -> Color.Gray
                                        line.startsWith("Unmerged") -> GitRed
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                    Text(
                                        line.ifEmpty { " " },
                                        color = color,
                                        fontFamily = FontFamily.Monospace,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { vm.dismissStatusDetail() }) { Text("Close") }
                    }
                )
            }

            if (showStatusOptions) {
                androidx.compose.ui.window.Dialog(onDismissRequest = { showStatusOptions = false }) {
                    Box(
                        Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.extraLarge,
                            tonalElevation = 6.dp,
                            modifier = Modifier
                                .widthIn(max = 400.dp)
                                .fillMaxWidth()
                        ) {
                            Column(
                                Modifier.padding(24.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Status", style = MaterialTheme.typography.headlineSmall)
                                Spacer(Modifier.height(12.dp))
                                TextButton(
                                    onClick = { showStatusOptions = false; vm.gitStatus() },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Git status") }
                                TextButton(
                                    onClick = { showStatusOptions = false; vm.lfsStatus() },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("LFS status") }
                                TextButton(
                                    onClick = { showStatusOptions = false; vm.fullStatus() },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Git + LFS status") }
                                Spacer(Modifier.height(8.dp))
                                TextButton(onClick = { showStatusOptions = false }) {
                                    Text("Cancel")
                                }
                            }
                        }
                    }
                }
            }


            if (showLfsTrack) {
                AlertDialog(
                    onDismissRequest = { showLfsTrack = false },
                    title = { Text("Track with Git LFS") },
                    text = {
                        Column {
                            Text(
                                "Files matching this pattern will be stored in LFS on GitHub/GitLab when staged.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = lfsTrackPattern,
                                onValueChange = { lfsTrackPattern = it },
                                label = { Text("Pattern") },
                                placeholder = { Text("*.psd") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showLfsTrack = false
                                vm.lfsTrack(lfsTrackPattern)
                            }
                        ) { Text("Track") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showLfsTrack = false }) { Text("Cancel") }
                    }
                )
            }

            if (showForcePushConfirm) {
                // Custom dialog so options are never clipped (M3 AlertDialog text slot often truncates)
                androidx.compose.ui.window.Dialog(onDismissRequest = { showForcePushConfirm = false }) {
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        tonalElevation = 6.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(24.dp)) {
                            Text(
                                "Force push",
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Choose how to overwrite the remote branch:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(16.dp))

                            ForcePushModeOption(
                                title = "Force with lease",
                                subtitle = "Only if remote still matches your last fetch (safer)",
                                selected = forcePushUseLease,
                                onSelect = { forcePushUseLease = true }
                            )
                            Spacer(Modifier.height(8.dp))
                            ForcePushModeOption(
                                title = "Force (no lease)",
                                subtitle = "Always overwrite — can discard others’ commits",
                                selected = !forcePushUseLease,
                                onSelect = { forcePushUseLease = false }
                            )

                            Spacer(Modifier.height(20.dp))
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { showForcePushConfirm = false }) {
                                    Text("Cancel")
                                }
                                Spacer(Modifier.width(8.dp))
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
                                        ButtonDefaults.buttonColors(containerColor = GitRed)
                                    }
                                ) {
                                    Text(if (forcePushUseLease) "Force with lease" else "Force push")
                                }
                            }
                        }
                    }
                }
            }


            val pageNavItems = listOf(
                SubNavItem(Icons.Default.FolderOpen, "Files", onOpenFiles),
                SubNavItem(Icons.Default.AccountTree, "Branches", onOpenBranches),
                SubNavItem(Icons.Default.CallMerge, prLabel, onOpenPullRequests),
                SubNavItem(Icons.Default.BugReport, issuesLabel, onOpenIssues),
                SubNavItem(Icons.Default.PlayCircle, ciLabel, onOpenWorkflows),
            )
            val gitOpsItems = listOf(
                SubNavItem(Icons.Default.ArrowDownward, "Pull") {
                    if (!state.busy) showPullOptions = true
                },
                SubNavItem(Icons.Default.ArrowUpward, "Push") {
                    if (!state.busy) showPushOptions = true
                },
                SubNavItem(Icons.Default.Info, "Status") {
                    if (!state.busy) showStatusOptions = true
                },
                SubNavItem(Icons.Default.Storage, "LFS") {
                    if (!state.busy) showLfsOptions = true
                },
            )

            @Composable
            fun ConflictBanner() {
                if (status != null && status.conflicting.isNotEmpty()) {
                    Surface(color = GitAmber.copy(alpha = 0.15f), modifier = Modifier.fillMaxWidth().padding(12.dp, 4.dp)) {
                        Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, null, tint = GitAmber)
                            Spacer(Modifier.width(8.dp))
                            Text("${status.conflicting.size} conflicting file(s)", Modifier.weight(1f))
                            TextButton(onClick = onConflicts) { Text("Resolve") }
                        }
                    }
                }
            }

            @Composable
            fun ChangesLazy(bottomPad: androidx.compose.ui.unit.Dp) {
                if ((state.busy || state.refreshing) && status == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    BrandLoadingPlaceholder()
                }
                } else if (status != null) {
                    LazyColumn(Modifier.fillMaxSize()) {
                        if (status.staged.isNotEmpty()) {
                            item {
                                Row(
                                    Modifier.fillMaxWidth().padding(16.dp, 8.dp, 16.dp, 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Staged changes (${status.staged.size})",
                                        style = MaterialTheme.typography.labelLarge,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = { vm.unstageAll() }, enabled = !state.busy) {
                                        Text("Unstage all")
                                    }
                                }
                            }
                            items(status.staged, key = { "s_" + it.path }) { fc ->
                                ChangeRow(
                                    fc,
                                    onToggle = { vm.toggleStage(fc.path, true) },
                                    onClick = { onOpenDiff(fc.path, "staged") }
                                )
                            }
                        }
                        val unstagedAll = status.unstaged + status.untracked
                        if (unstagedAll.isNotEmpty()) {
                            item {
                                Row(
                                    Modifier.fillMaxWidth().padding(16.dp, 8.dp, 16.dp, 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Changes (${unstagedAll.size})",
                                        style = MaterialTheme.typography.labelLarge,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = { vm.discardAll() }, enabled = !state.busy) {
                                        Text("Revert all")
                                    }
                                    TextButton(onClick = { vm.stageAll() }) { Text("Stage all") }
                                }
                            }
                            items(unstagedAll, key = { "u_" + it.path }) { fc ->
                                ChangeRow(
                                    fc,
                                    onToggle = { vm.toggleStage(fc.path, false) },
                                    onClick = { onOpenDiff(fc.path, "working") },
                                    onDiscard = { vm.discard(fc.path) }
                                )
                            }
                        }
                        if (status.isClean) {
                            item {
                                Box(
                                    Modifier.fillParentMaxWidth().padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "Nothing to commit — working tree clean",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        item { Spacer(Modifier.height(bottomPad)) }
                    }
                }
            }

            if (largePhone) {
                // Flat workspace (no floating card): page nav → changes → git ops → commit
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .imePadding()
                ) {
                        RepoSubNavFloatingBar(items = pageNavItems, flat = true)
                        HorizontalDivider(
                            Modifier.padding(horizontal = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                        )
                        ConflictBanner()
                        PullToRefreshBox(
                            isRefreshing = state.refreshing,
                            onRefresh = vm::refresh,
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        ) {
                            ChangesLazy(bottomPad = 8.dp)
                        }
                        HorizontalDivider(
                            Modifier.padding(horizontal = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                        )
                        RepoSubNavFloatingBar(items = gitOpsItems, flat = true)
                        HorizontalDivider(
                            Modifier.padding(horizontal = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                        )
                        if (status != null) {
                            CommitBar(
                                message = state.commitMessage,
                                onMessageChange = vm::setCommitMessage,
                                suggestedMessage = state.suggestedCommitMessage,
                                onUseSuggested = vm::applySuggestedCommitMessage,
                                signOff = state.signOff,
                                onSignOffChange = vm::setSignOff,
                                amend = state.amend,
                                onAmendChange = vm::setAmend,
                                authorName = state.authorName,
                                authorEmail = state.authorEmail,
                                coAuthors = state.coAuthors,
                                onAddCoAuthor = vm::addCoAuthor,
                                onRemoveCoAuthor = vm::removeCoAuthor,
                                enabled = (status.staged.isNotEmpty() || state.amend) && !state.busy,
                                onCommit = vm::commit,
                                compactOptions = true,
                                bottomInset = 0.dp,
                                embedded = true
                            )
                        }
                    }
            } else {
                ConflictBanner()
                PullToRefreshBox(
                    isRefreshing = state.refreshing,
                    onRefresh = vm::refresh,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    if ((state.busy || state.refreshing) && status == null) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    BrandLoadingPlaceholder()
                }
                    } else if (status != null) {
                        Column(Modifier.fillMaxSize()) {
                            Box(Modifier.weight(1f).fillMaxWidth()) {
                                ChangesLazy(
                                    bottomPad = if (LocalWindowSizeClass.current.isCompactHeight) 72.dp else 140.dp
                                )
                            }
                            CommitBar(
                                message = state.commitMessage,
                                onMessageChange = vm::setCommitMessage,
                                suggestedMessage = state.suggestedCommitMessage,
                                onUseSuggested = vm::applySuggestedCommitMessage,
                                signOff = state.signOff,
                                onSignOffChange = vm::setSignOff,
                                amend = state.amend,
                                onAmendChange = vm::setAmend,
                                authorName = state.authorName,
                                authorEmail = state.authorEmail,
                                coAuthors = state.coAuthors,
                                onAddCoAuthor = vm::addCoAuthor,
                                onRemoveCoAuthor = vm::removeCoAuthor,
                                enabled = (status.staged.isNotEmpty() || state.amend) && !state.busy,
                                onCommit = vm::commit,
                                compactOptions = false,
                                bottomInset = 0.dp
                            )
                        }
                    }
                }
            }
        }
        } // AdaptiveContent

        ExpressiveTaskOverlay(
            visible = state.busy,
            label = state.busyLabel.ifBlank { if (state.busy) "Working…" else "" },
            success = showTaskSuccess,
            onSuccessConsumed = {
                showTaskSuccess = false
                vm.consumeResult()
            }
        )
    } // overlay Box

    }
}

@Composable
private fun ForcePushModeOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        onClick = onSelect,
        shape = MaterialTheme.shapes.medium,
        color = if (selected) {
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
            RadioButton(selected = selected, onClick = onSelect)
            Spacer(Modifier.width(4.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SubPageButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .widthIn(min = 56.dp)
            .clickableSimple(onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )
    }
}

@Composable
private fun ChangeRow(
    fc: FileChange,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onDiscard: (() -> Unit)? = null
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            fc.type.badge(),
            color = fc.type.color(),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(20.dp)
        )
        Text(
            fc.path,
            modifier = Modifier.weight(1f).clickableSimple(onClick),
            maxLines = 1,
            style = MaterialTheme.typography.bodyMedium
        )
        if (onDiscard != null) {
            IconButton(onClick = onDiscard) { Icon(Icons.Default.Undo, "Discard", modifier = Modifier.size(18.dp)) }
        }
        IconButton(onClick = onToggle) {
            Icon(if (fc.staged) Icons.Default.RemoveCircleOutline else Icons.Default.AddCircleOutline, "Toggle stage")
        }
    }
}

internal fun ChangeType.badge() = when (this) {
    ChangeType.ADDED -> "A"
    ChangeType.MODIFIED -> "M"
    ChangeType.DELETED -> "D"
    ChangeType.RENAMED -> "R"
    ChangeType.CONFLICTING -> "!"
    ChangeType.UNTRACKED -> "U"
}

internal fun ChangeType.color() = when (this) {
    ChangeType.ADDED -> GitGreen
    ChangeType.MODIFIED -> GitAmber
    ChangeType.DELETED -> GitRed
    ChangeType.RENAMED -> Color(0xFF8250DF)
    ChangeType.CONFLICTING -> GitRed
    ChangeType.UNTRACKED -> Color.Gray
}

@Composable
private fun CommitBar(
    message: String,
    onMessageChange: (String) -> Unit,
    suggestedMessage: String = "",
    onUseSuggested: () -> Unit = {},
    signOff: Boolean,
    onSignOffChange: (Boolean) -> Unit,
    amend: Boolean = false,
    onAmendChange: (Boolean) -> Unit = {},
    authorName: String,
    authorEmail: String,
    coAuthors: List<String> = emptyList(),
    onAddCoAuthor: (name: String, email: String) -> Unit = { _, _ -> },
    onRemoveCoAuthor: (String) -> Unit = {},
    enabled: Boolean,
    onCommit: () -> Unit,
    /** Large phones: fold amend / sign-off into the commit control. */
    compactOptions: Boolean = false,
    bottomInset: androidx.compose.ui.unit.Dp = 0.dp,
    embedded: Boolean = false
) {
    var optionsOpen by remember { mutableStateOf(false) }
    var coName by remember { mutableStateOf("") }
    var coEmail by remember { mutableStateOf("") }
    val body: @Composable () -> Unit = {
        Column(
            Modifier.padding(
                start = 16.dp,
                end = 16.dp,
                top = if (compactOptions) 8.dp else 16.dp,
                bottom = if (compactOptions) 10.dp else 16.dp
            )
        ) {
            OutlinedTextField(
                value = message,
                onValueChange = onMessageChange,
                label = { Text("Commit message") },
                modifier = Modifier.fillMaxWidth(),
                minLines = if (compactOptions) 1 else 2,
                maxLines = if (compactOptions) 3 else 4
            )
            if (suggestedMessage.isNotBlank() && message.isBlank()) {
                TextButton(
                    onClick = onUseSuggested,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text("Use suggested: $suggestedMessage", maxLines = 1)
                }
            } else if (suggestedMessage.isNotBlank()) {
                TextButton(
                    onClick = onUseSuggested,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text("Replace with suggested message")
                }
            }

            // Co-authors → Co-authored-by trailers on the commit message
            Spacer(Modifier.height(8.dp))
            Text(
                "Co-authors",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (coAuthors.isNotEmpty()) {
                coAuthors.forEach { entry ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(entry, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { onRemoveCoAuthor(entry) }) { Text("Remove") }
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = coName,
                    onValueChange = { coName = it },
                    label = { Text("Name") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = coEmail,
                    onValueChange = { coEmail = it },
                    label = { Text("Email") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
            TextButton(
                onClick = {
                    onAddCoAuthor(coName, coEmail)
                    coName = ""
                    coEmail = ""
                },
                enabled = coEmail.isNotBlank()
            ) { Text("Add co-author") }
            if (!compactOptions) {
                Spacer(Modifier.height(4.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = signOff, onCheckedChange = onSignOffChange)
                    Column(Modifier.weight(1f).clickableSimple { onSignOffChange(!signOff) }) {
                        Text("Sign-off", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Signed-off-by: $authorName <$authorEmail>",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = amend, onCheckedChange = onAmendChange)
                    Column(Modifier.weight(1f).clickableSimple { onAmendChange(!amend) }) {
                        Text("Amend previous commit", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Rewrites the last commit (git commit --amend)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
            Spacer(Modifier.height(if (compactOptions) 6.dp else 8.dp))
            val label = when {
                amend && signOff -> "Amend (signed-off)"
                amend -> "Amend previous commit"
                signOff -> "Commit (signed-off)"
                else -> "Commit to current branch"
            }
            if (compactOptions) {
                // Split-style control: primary commit + options menu (amend / sign-off).
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onCommit,
                        enabled = enabled && message.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium.copy(
                            topEnd = androidx.compose.foundation.shape.CornerSize(0.dp),
                            bottomEnd = androidx.compose.foundation.shape.CornerSize(0.dp)
                        )
                    ) {
                        Text(label, maxLines = 1)
                    }
                    Box {
                        FilledIconButton(
                            onClick = { optionsOpen = true },
                            shape = MaterialTheme.shapes.medium.copy(
                                topStart = androidx.compose.foundation.shape.CornerSize(0.dp),
                                bottomStart = androidx.compose.foundation.shape.CornerSize(0.dp)
                            )
                        ) {
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Commit options"
                            )
                        }
                        DropdownMenu(
                            expanded = optionsOpen,
                            onDismissRequest = { optionsOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = amend, onCheckedChange = null)
                                        Column {
                                            Text("Amend previous commit")
                                            Text(
                                                "git commit --amend",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                },
                                onClick = { onAmendChange(!amend) }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = signOff, onCheckedChange = null)
                                        Column {
                                            Text("Sign-off")
                                            Text(
                                                "$authorName <$authorEmail>",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                },
                                onClick = { onSignOffChange(!signOff) }
                            )
                        }
                    }
                }
            } else {
                Button(
                    onClick = onCommit,
                    enabled = enabled && message.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(label)
                }
            }
        }
    }
    if (embedded) {
        body()
    } else {
        Surface(
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(bottom = bottomInset)
        ) {
            body()
        }
    }
}


private data class SubNavItem(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String,
    val onClick: () -> Unit
)

/** Labeled floating bar of repo sub-pages for large phones (above the commit bar). */
@Composable
private fun RepoSubNavFloatingBar(items: List<SubNavItem>, flat: Boolean = false) {
    val row = @Composable {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = if (flat) 8.dp else 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = item.onClick)
                        .padding(vertical = 4.dp, horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        item.icon,
                        contentDescription = item.label,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        item.label,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
    if (flat) {
        row()
    } else {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 3.dp,
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            row()
        }
    }
}

private fun Modifier.clickableSimple(onClick: () -> Unit): Modifier =
    this.clickable(onClick = onClick)
