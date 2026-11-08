package com.quickgit.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.*
// wrapContentWidth via foundation.layout
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.platform.LocalContext
import com.quickgit.app.ui.security.isChromebookDevice
import com.quickgit.app.ui.components.BrandLoadingPlaceholder
import com.quickgit.app.ui.components.BrandLoadingSpinner
import com.quickgit.app.ui.components.ChromeOsSectionItem
import com.quickgit.app.ui.components.LocalChromeOsRepoSection
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import android.content.res.Configuration
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quickgit.app.data.models.FileChange
import com.quickgit.app.data.models.GitOpResult
import com.quickgit.app.ui.theme.GitAmber
import com.quickgit.app.ui.theme.GitGreen
import com.quickgit.app.ui.components.ExpressiveTaskOverlay
import com.quickgit.app.ui.components.PullToRefreshBox
import com.quickgit.app.ui.components.FloatingCircleIconButton
import com.quickgit.app.ui.components.AppBrandLogo
import com.quickgit.app.viewmodel.BranchesViewModel
import com.quickgit.app.viewmodel.EditorViewModel
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
    val filesState by filesVm.state.collectAsState()
    val editorVm: EditorViewModel = viewModel(factory = factory)
    val issuesVm: IssuesViewModel = viewModel(factory = factory)
    val pullRequestsVm: PullRequestsViewModel = viewModel(factory = factory)
    val workflowsVm: WorkflowsViewModel = viewModel(factory = factory)
    val workflowsState by workflowsVm.state.collectAsState()
    val releasesVm: ReleasesViewModel = viewModel(factory = factory)
    val releasesState by releasesVm.state.collectAsState()
    val historyVm: HistoryViewModel = viewModel(factory = factory)

    // Chromebook / wide: Files list stays on the left as a vertical nav; editor opens on the right.
    var selectedEditorPath by remember { mutableStateOf<String?>(null) }
    // Incremented from Chromebook top chrome to open the Releases edit dialog.
    var releaseEditRequest by remember { mutableIntStateOf(0) }
    // Driven by ReleasesScreen so header Back closes detail even if VM selected lags.
    var releaseDetailOpen by remember { mutableStateOf(false) }
    // Driven by WorkflowsScreen when a run / job log / live page is open.
    var workflowDetailOpen by remember { mutableStateOf(false) }

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

    // Changes first, then Files, Branches, Issues, PRs, Actions, Releases (History is top-right).
    val tabs = listOf(
        "Changes" to Icons.Default.Edit,
        "Files" to Icons.Default.FolderOpen,
        "Branches" to Icons.Default.AccountTree,
        "Issues" to Icons.Default.BugReport,
        "PRs" to Icons.Default.CallMerge,
        "Actions" to Icons.Default.PlayCircle,
        "Releases" to Icons.Default.NewReleases,
    )
    var localTabIndex by remember { mutableStateOf(0) }
    var showHistory by remember { mutableStateOf(false) }
    val isPortrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
    val isChromebook = LocalContext.current.isChromebookDevice()
    val chromeSection = LocalChromeOsRepoSection.current
    // Chromebook: section selection lives in the top chrome under main destinations.
    // Keep activate/deactivate stable so rapid section taps don't tear down chrome state.
    if (isChromebook) {
        DisposableEffect(Unit) {
            chromeSection.activate(
                tabs.map { (label, icon) -> ChromeOsSectionItem(icon, label) },
                localTabIndex
            )
            onDispose { chromeSection.deactivate() }
        }
    }
    // Read MutableIntState during composition so clicks in ChromeOsAppChrome recompose this tree.
    val chromeTabIndex = chromeSection.selectedIndex.intValue
    val tabIndex = if (isChromebook) chromeTabIndex else localTabIndex
    fun setTabIndex(i: Int) {
        if (i == tabIndex) return
        if (isChromebook) chromeSection.selectedIndex.intValue = i
        else localTabIndex = i
    }
    // Leave the split editor when switching away from Files; refresh Changes when landing on it
    // so saves / external overwrites show up in staging without leaving the repo.
    LaunchedEffect(tabIndex) {
        if (tabIndex != 1) selectedEditorPath = null
        if (tabIndex != 6) {
            releaseDetailOpen = false
            if (releasesState.selected != null) releasesVm.closeDetail()
        }
        if (tabIndex != 5) {
            workflowDetailOpen = false
            if (workflowsState.selectedRun != null) workflowsVm.closeDetail()
            if (workflowsState.logJobId != null) workflowsVm.closeJobLog()
            if (workflowsState.livePageUrl != null) workflowsVm.closeLivePage()
        }
        if (tabIndex == 0) vm.refresh()
    }
    // Re-scan working tree whenever the activity returns to the foreground (e.g. after
    // another app overwrote files). Fixes stale Changes until leaving/re-entering the repo.
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentTab by rememberUpdatedState(tabIndex)
    val currentFilesDir by rememberUpdatedState(filesState.currentDir)
    DisposableEffect(lifecycleOwner, repoPath) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                vm.refresh()
                if (currentTab == 1) filesVm.openDir(currentFilesDir)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    /** Git Actions sheet: null = closed, "root" | "pull" | "push". */
    var gitActionsStep by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize()) {
        if (showHistory) {
            BackHandler { showHistory = false }
            HistoryScreen(
                repoPath = repoPath,
                vm = historyVm,
                onBack = { showHistory = false }
            )
            return@Box
        }

        Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            // Top bar: Chromebook = flat ChromeOS IconButtons; tablet = floating circles
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (isChromebook) 4.dp else 8.dp, vertical = 6.dp)
            ) {
                Row(
                    Modifier.align(Alignment.CenterStart),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (isChromebook) 4.dp else 8.dp)
                ) {
                    // Nested detail: Back closes release / workflow run instead of leaving the repo.
                    val openRelease =
                        if (tabIndex == 6) releasesState.selected else null
                    val openRun =
                        if (tabIndex == 5) workflowsState.selectedRun else null
                    val releaseOpen = openRelease != null || (tabIndex == 6 && releaseDetailOpen)
                    val workflowOpen = openRun != null || (tabIndex == 5 && workflowDetailOpen)
                    fun isWorkflowLive(status: String) =
                        status == "queued" || status == "in_progress" || status == "requested" ||
                            status == "waiting" || status == "pending"
                    val onHeaderBack: () -> Unit = {
                        when {
                            releaseOpen -> {
                                releasesVm.closeDetail()
                                releaseDetailOpen = false
                            }
                            workflowOpen -> {
                                when {
                                    workflowsState.livePageUrl != null -> workflowsVm.closeLivePage()
                                    workflowsState.logJobId != null -> workflowsVm.closeJobLog()
                                    else -> {
                                        workflowsVm.closeDetail()
                                        workflowDetailOpen = false
                                    }
                                }
                            }
                            else -> onBack()
                        }
                    }
                    BackHandler(enabled = releaseOpen || workflowOpen) {
                        onHeaderBack()
                    }
                    if (isChromebook) {
                        IconButton(onClick = onHeaderBack) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = when {
                                    releaseOpen -> "Back to releases"
                                    workflowOpen -> "Back to actions"
                                    else -> "Back"
                                },
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        // Detail open: show release / workflow title instead of repo/branch.
                        Column(Modifier.padding(start = 2.dp).weight(1f, fill = true)) {
                            when {
                                openRelease != null -> {
                                    Text(
                                        openRelease.name.ifBlank { openRelease.tagName },
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1
                                    )
                                    Text(
                                        openRelease.tagName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                                openRun != null -> {
                                    Text(
                                        openRun.displayTitle.ifBlank { openRun.name },
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1
                                    )
                                    Text(
                                        "Run #${openRun.runNumber}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                                else -> {
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
                            }
                        }
                    } else {
                        FloatingCircleIconButton(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = when {
                                releaseOpen -> "Back to releases"
                                workflowOpen -> "Back to actions"
                                else -> "Back"
                            },
                            onClick = onHeaderBack
                        )
                        if (!isPortrait) {
                            AppBrandLogo(size = 36.dp)
                        }
                    }
                }
                Row(
                    Modifier.align(Alignment.CenterEnd),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (state.busy) {
                        BrandLoadingSpinner(size = 20.dp)
                    }
                    // Chromebook + workflow run: watch / cancel / re-run / refresh on the header line.
                    if (isChromebook && tabIndex == 5 && workflowsState.selectedRun != null) {
                        val run = workflowsState.selectedRun!!
                        val live = run.status == "queued" || run.status == "in_progress" ||
                            run.status == "requested" || run.status == "waiting" ||
                            run.status == "pending"
                        if (workflowsState.watching) {
                            IconButton(onClick = { workflowsVm.stopWatching() }) {
                                Icon(Icons.Default.Pause, contentDescription = "Stop watching")
                            }
                        } else if (live) {
                            IconButton(onClick = { workflowsVm.startWatching(run.id) }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Watch live")
                            }
                        }
                        if (live) {
                            IconButton(
                                onClick = { workflowsVm.cancelRun(run.id) },
                                enabled = !workflowsState.busy
                            ) {
                                Icon(Icons.Default.Cancel, contentDescription = "Cancel")
                            }
                        } else {
                            IconButton(
                                onClick = { workflowsVm.rerun(run.id) },
                                enabled = !workflowsState.busy
                            ) {
                                Icon(Icons.Default.Replay, contentDescription = "Re-run")
                            }
                        }
                        IconButton(
                            onClick = { workflowsVm.openRun(run.id) },
                            enabled = !workflowsState.detailLoading
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                    // Chromebook + open release: flat Edit at the right end of the top bar line.
                    if (isChromebook && tabIndex == 6 && releasesState.selected != null) {
                        IconButton(onClick = { releaseEditRequest++ }) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit release",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    // Chromebook: Stage / Unstage / Discard / Git Actions live left of History
                    if (isChromebook && tabIndex == 0) {
                        OutlinedButton(
                            onClick = { vm.stageAll() },
                            enabled = !state.busy,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) { Text("Stage all", style = MaterialTheme.typography.labelMedium) }
                        OutlinedButton(
                            onClick = { vm.unstageAll() },
                            enabled = !state.busy,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) { Text("Unstage all", style = MaterialTheme.typography.labelMedium) }
                        OutlinedButton(
                            onClick = { vm.discardAll() },
                            enabled = !state.busy,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) { Text("Discard", style = MaterialTheme.typography.labelMedium) }
                        OutlinedButton(
                            onClick = { gitActionsStep = "root" },
                            enabled = !state.busy,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) { Text("Git Actions", style = MaterialTheme.typography.labelMedium) }
                        IconButton(onClick = { showHistory = true }) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = "History",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else if (tabIndex == 0) {
                        FloatingCircleIconButton(
                            imageVector = Icons.Default.History,
                            contentDescription = "History",
                            onClick = { showHistory = true }
                        )
                    }
                }
            }

            // Repo name + branch (tablet); Chromebook already shows them next to Back
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isChromebook) {
                    Column {
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
                }
                if (tabIndex == 1) {
                    Text(
                        if (filesState.currentDir.isBlank()) "/" else filesState.currentDir,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
                // Tablet only: action row under the title. Chromebook moves these next to History.
                if (tabIndex == 0 && !isChromebook) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
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
                        OutlinedButton(
                            onClick = { gitActionsStep = "root" },
                            enabled = !state.busy
                        ) { Text("Git Actions") }
                    }
                }
            }

            // Chromebook section menu lives in ChromeOsAppChrome under the main destinations.

            PullToRefreshBox(
                isRefreshing = if (tabIndex == 1) filesState.loading else state.busy,
                onRefresh = {
                    if (tabIndex == 1) filesVm.openDir(filesState.currentDir)
                    else vm.refresh()
                },
                modifier = Modifier.weight(1f).fillMaxSize()
            ) {
                // key() forces a clean composition when switching sections — avoids
                // leftover state / layout from the previous tab that caused glitchy transitions.
                key(tabIndex) {
                    when (tabIndex) {
                        0 -> ChangesTabDesktop(vm)
                        1 -> {
                            // Always dual-pane on desktop/Chromebook Files:
                            // left = file explorer, right = editor (or placeholder).
                            BackHandler(enabled = selectedEditorPath != null) {
                                selectedEditorPath = null
                                // Returning from editor: pick up any saves into Changes.
                                vm.refresh()
                            }
                            Row(Modifier.fillMaxSize()) {
                                Box(
                                    Modifier
                                        .weight(0.38f)
                                        .fillMaxHeight()
                                ) {
                                    FilesScreen(
                                        repoPath = repoPath,
                                        vm = filesVm,
                                        onBack = null,
                                        onOpenFile = { path -> selectedEditorPath = path }
                                    )
                                }
                                VerticalDivider(
                                    thickness = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                                Box(Modifier.weight(0.62f).fillMaxHeight()) {
                                    val path = selectedEditorPath
                                    if (path != null) {
                                        key(path) {
                                            EditorScreen(
                                                repoPath = repoPath,
                                                relativePath = path,
                                                vm = editorVm,
                                                onBack = {
                                                    selectedEditorPath = null
                                                    vm.refresh()
                                                },
                                                onSaved = { vm.refresh() }
                                            )
                                        }
                                    } else {
                                        Box(
                                            Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "Select a file to edit",
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        2 -> BranchesScreen(repoPath = repoPath, vm = branchesVm, onBack = null)
                        3 -> IssuesScreen(vm = issuesVm, onBack = null, onNeedsAuth = onNeedsAuth)
                        4 -> PullRequestsScreen(vm = pullRequestsVm, onBack = null, onNeedsAuth = onNeedsAuth)
                        5 -> WorkflowsScreen(
                            vm = workflowsVm,
                            onBack = null,
                            onNeedsAuth = onNeedsAuth,
                            repoPath = repoPath,
                            showReleasesTab = false,
                            embedded = true,
                            chromeHostsDetailHeader = isChromebook,
                            onDetailOpenChange = { open -> workflowDetailOpen = open }
                        )
                        6 -> ReleasesScreen(
                            vm = releasesVm,
                            onBack = null,
                            onNeedsAuth = onNeedsAuth,
                            embedded = true,
                            chromeHostsDetailHeader = isChromebook,
                            externalEditRequest = releaseEditRequest,
                            onDetailOpenChange = { open -> releaseDetailOpen = open }
                        )
                    }
                }
            }

            // Tablet: floating bottom section bar. Chromebook uses the top strip instead.
            if (!isChromebook) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        tonalElevation = 4.dp,
                        shadowElevation = 12.dp,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .widthIn(max = 560.dp)
                            .fillMaxWidth(0.94f)
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            tabs.forEachIndexed { i, (title, icon) ->
                                val selected = tabIndex == i
                                val selectedBg =
                                    if (selected) GitGreen.copy(alpha = 0.2f) else Color.Transparent
                                val contentColor =
                                    if (selected) GitGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(selectedBg)
                                        .clickable { setTabIndex(i) }
                                        .padding(vertical = 3.dp, horizontal = 1.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = title,
                                        tint = contentColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        title,
                                        maxLines = 1,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = contentColor,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }


        }
    }


    if (gitActionsStep == "root") {
        AlertDialog(
            onDismissRequest = { gitActionsStep = null },
            title = { Text("Git Actions") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = { gitActionsStep = "pull" },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Pull") }
                    FilledTonalButton(
                        onClick = { gitActionsStep = "push" },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Push") }
                    FilledTonalButton(
                        onClick = {
                            gitActionsStep = null
                            showStatusOptions = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Status") }
                    FilledTonalButton(
                        onClick = {
                            gitActionsStep = null
                            showLfsOptions = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("LFS") }
                }
            },
            confirmButton = {
                TextButton(onClick = { gitActionsStep = null }) { Text("Close") }
            }
        )
    }
    if (gitActionsStep == "pull") {
        AlertDialog(
            onDismissRequest = { gitActionsStep = "root" },
            title = { Text("Pull") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = { gitActionsStep = null; vm.pull() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Git pull") }
                    TextButton(
                        onClick = { gitActionsStep = null; vm.pullRebase() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Pull with rebase") }
                    TextButton(
                        onClick = { gitActionsStep = null; vm.fetchLfs() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("LFS pull only") }
                    TextButton(
                        onClick = { gitActionsStep = null; vm.pullWithLfs() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Git pull + LFS") }
                }
            },
            confirmButton = {
                TextButton(onClick = { gitActionsStep = "root" }) { Text("Back") }
            }
        )
    }
    if (gitActionsStep == "push") {
        AlertDialog(
            onDismissRequest = { gitActionsStep = "root" },
            title = { Text("Push") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = { gitActionsStep = null; vm.push(force = false) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Git push") }
                    TextButton(
                        onClick = { gitActionsStep = null; vm.pushRebase() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Push with rebase") }
                    if (state.isGerritRemote) {
                        TextButton(
                            onClick = { gitActionsStep = null; vm.pushForReview() },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Push for review") }
                    }
                    TextButton(
                        onClick = { gitActionsStep = null; vm.pushLfs() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("LFS push only") }
                    TextButton(
                        onClick = { gitActionsStep = null; vm.pushWithLfs() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Git push + LFS") }
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    TextButton(
                        onClick = {
                            gitActionsStep = null
                            forcePushUseLease = true
                            showForcePushConfirm = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Force with lease…", color = com.quickgit.app.ui.theme.GitRed)
                    }
                    TextButton(
                        onClick = {
                            gitActionsStep = null
                            forcePushUseLease = false
                            showForcePushConfirm = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Force (no lease)…", color = com.quickgit.app.ui.theme.GitRed)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { gitActionsStep = "root" }) { Text("Back") }
            }
        )
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

        // Portrait: larger brand mark as a soft watermark above the bottom edge.
        if (isPortrait) {
            AppBrandLogo(
                size = 112.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 28.dp)
                    .graphicsLayer { alpha = 0.14f }
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
    var coName by remember { mutableStateOf("") }
    var coEmail by remember { mutableStateOf("") }
    // The co-author section stays collapsed until the user taps "Add co-author".
    // Starts expanded if co-authors are already attached so they are never hidden.
    var coAuthorOpen by remember { mutableStateOf(state.coAuthors.isNotEmpty()) }
    val status = state.status
    var selected by remember { mutableStateOf(setOf<String>()) }

    val staged = status?.staged.orEmpty()
    val unstagedAll = status?.unstaged.orEmpty() + status?.untracked.orEmpty() + status?.conflicting.orEmpty()
    val stagedPaths = remember(staged) { staged.map { it.path }.toSet() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        if (state.busy && status == null) {
            BrandLoadingPlaceholder()
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
        // Hidden behind a button so the commit area stays compact by default.
        if (!coAuthorOpen && state.coAuthors.isEmpty()) {
            TextButton(onClick = { coAuthorOpen = true }) { Text("Add co-author") }
        } else {
            Text("Co-authors", style = MaterialTheme.typography.labelLarge)
            state.coAuthors.forEach { entry ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(entry, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { vm.removeCoAuthor(entry) }) { Text("Remove") }
                }
            }
            OutlinedTextField(
                value = coName,
                onValueChange = { coName = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = coEmail,
                onValueChange = { coEmail = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            if (state.savedContributors.isNotEmpty()) {
                var savedMenuOpen by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { savedMenuOpen = true }) { Text("Saved contributors") }
                    DropdownMenu(expanded = savedMenuOpen, onDismissRequest = { savedMenuOpen = false }) {
                        state.savedContributors.forEach { entry: String ->
                            DropdownMenuItem(
                                text = { Text(entry) },
                                onClick = {
                                    vm.addCoAuthorEntry(entry, save = false)
                                    savedMenuOpen = false
                                }
                            )
                        }
                    }
                }
            }
            TextButton(
                onClick = {
                    vm.addCoAuthor(coName, coEmail, save = true)
                    coName = ""
                    coEmail = ""
                },
                enabled = coEmail.isNotBlank()
            ) { Text("Add & save co-author") }
        }
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
