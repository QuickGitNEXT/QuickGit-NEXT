package com.quickgit.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quickgit.app.data.github.GitHubApi
import com.quickgit.app.ui.components.PullToRefreshBox
import com.quickgit.app.ui.components.FloatingCircleIconButton
import com.quickgit.app.ui.components.BrandLoadingPlaceholder
import com.quickgit.app.ui.components.AppScreenTitle
import com.quickgit.app.viewmodel.RemoteBrowseViewModel

/**
 * Read-only file browser for someone else's (or your own) repo, backed directly by the
 * GitHub Contents API — nothing gets cloned. Directories drill down in place; files hand off
 * to [RemoteFileScreen] for viewing.
 *
 * The branch chip under the top bar switches branches, and the floating Commits button lets
 * you view the code as of an older commit or jump back to the latest ("now").
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteBrowseScreen(
    owner: String,
    repo: String,
    ref: String,
    path: String,
    vm: RemoteBrowseViewModel,
    onBack: () -> Unit,
    /** Called with the file path and the ref (branch or commit SHA) it should be opened at. */
    onOpenFile: (path: String, ref: String) -> Unit,
    onNeedsAuth: () -> Unit
) {
    LaunchedEffect(owner, repo, ref, path) { vm.init(owner, repo, ref, path) }

    val state by vm.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showBranchPicker by remember { mutableStateOf(false) }
    var showCommits by remember { mutableStateOf(false) }

    LaunchedEffect(state.authRequired) {
        if (state.authRequired) {
            // Consume first so Back from Credentials does not re-open Settings in a loop.
            vm.consumeAuthRequired()
            onNeedsAuth()
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            vm.consumeError()
        }
    }

    // Before init() has run state.branch is blank; fall back to the route argument.
    val branchLabel = state.branch.ifBlank { ref }
    val pinned = state.pinnedCommit

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    AppScreenTitle(
                        pageTitle = repo,
                        subtitle = if (state.currentPath.isBlank()) {
                            "$owner / ${pinned?.sha?.take(7) ?: branchLabel}"
                        } else {
                            state.currentPath
                        }
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
                    if (state.currentPath.isNotBlank()) {
                        IconButton(onClick = vm::goUp) { Icon(Icons.Default.FolderOpen, contentDescription = "Up") }
                    }
                    // Replaces the old Refresh button (pull-to-refresh still reloads the folder).
                    FloatingCircleIconButton(
                        imageVector = Icons.Default.History,
                        contentDescription = "Commits",
                        onClick = {
                            showCommits = true
                            vm.loadCommits(refresh = true)
                        }
                    )
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            BranchBar(
                branch = branchLabel,
                pinnedCommit = pinned,
                onPickBranch = {
                    showBranchPicker = true
                    vm.loadBranches()
                },
                onReturnToLatest = vm::returnToLatest
            )
            PullToRefreshBox(
                isRefreshing = state.loading,
                onRefresh = { vm.openDir(state.currentPath) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    state.loading && state.entries.isEmpty() -> {
                        BrandLoadingPlaceholder(modifier = Modifier.align(Alignment.Center))
                    }
                    state.entries.isEmpty() -> {
                        Text(
                            "This folder is empty",
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    else -> {
                        LazyColumn(Modifier.fillMaxSize()) {
                            items(state.entries, key = { it.path }) { entry ->
                                val isDir = entry.type == "dir"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isDir) vm.openDir(entry.path) else onOpenFile(entry.path, state.ref)
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (isDir) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                                        contentDescription = null
                                    )
                                    Spacer(Modifier.size(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(entry.name, fontWeight = FontWeight.Medium)
                                        if (!isDir) {
                                            Text(
                                                "${entry.sizeBytes} bytes",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBranchPicker) {
        BranchPickerDialog(
            branches = state.branches,
            loading = state.branchesLoading,
            current = branchLabel,
            onSelect = { name ->
                showBranchPicker = false
                vm.selectBranch(name)
            },
            onDismiss = { showBranchPicker = false }
        )
    }

    if (showCommits) {
        CommitsSheet(
            branch = branchLabel,
            commits = state.commits,
            loading = state.commitsLoading,
            loadingMore = state.commitsLoadingMore,
            hasMore = state.hasMoreCommits,
            pinnedSha = pinned?.sha,
            onViewLatest = {
                showCommits = false
                vm.returnToLatest()
            },
            onViewCommit = { commit ->
                showCommits = false
                vm.viewCommit(commit)
            },
            onLoadMore = vm::loadMoreCommits,
            onDismiss = { showCommits = false }
        )
    }
}

/** Current branch chip (tap to switch) plus, while viewing an old commit, a way back to latest. */
@Composable
private fun BranchBar(
    branch: String,
    pinnedCommit: GitHubApi.RemoteCommit?,
    onPickBranch: () -> Unit,
    onReturnToLatest: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AssistChip(
            onClick = onPickBranch,
            label = { Text(branch, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            leadingIcon = { Icon(Icons.Default.AccountTree, contentDescription = null, modifier = Modifier.size(18.dp)) },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = "Switch branch") },
            modifier = Modifier.weight(1f, fill = false)
        )
        if (pinnedCommit != null) {
            Text(
                "Commit ${pinnedCommit.sha.take(7)}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            TextButton(onClick = onReturnToLatest) { Text("Latest") }
        }
    }
}

@Composable
private fun BranchPickerDialog(
    branches: List<String>,
    loading: Boolean,
    current: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var filter by remember { mutableStateOf("") }
    // Current branch first, then the API's order; filtered by the search box.
    val shown = remember(branches, filter, current) {
        val f = filter.trim()
        val matching = if (f.isEmpty()) branches else branches.filter { it.contains(f, ignoreCase = true) }
        matching.sortedByDescending { it == current }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select branch") },
        text = {
            Column {
                if (branches.size > 8) {
                    OutlinedTextField(
                        value = filter,
                        onValueChange = { filter = it },
                        singleLine = true,
                        label = { Text("Filter branches") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                }
                when {
                    loading && branches.isEmpty() -> {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            BrandLoadingPlaceholder()
                        }
                    }
                    shown.isEmpty() -> {
                        Text(
                            "No branches found",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                    else -> {
                        LazyColumn(Modifier.heightIn(max = 360.dp)) {
                            items(shown, key = { it }) { name ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelect(name) }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        name,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = if (name == current) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (name == current) {
                                        Icon(Icons.Default.Check, contentDescription = "Current branch")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

/**
 * Commit history of the selected branch. Pick a commit to browse the code as it was then, or
 * "Latest (now)" to go back to the current head of the branch.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommitsSheet(
    branch: String,
    commits: List<GitHubApi.RemoteCommit>,
    loading: Boolean,
    loadingMore: Boolean,
    hasMore: Boolean,
    pinnedSha: String?,
    onViewLatest: () -> Unit,
    onViewCommit: (GitHubApi.RemoteCommit) -> Unit,
    onLoadMore: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Text(
            "Commits on $branch",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyColumn(Modifier.fillMaxWidth()) {
            item(key = "latest") {
                ListItem(
                    headlineContent = { Text("Latest (now)", fontWeight = FontWeight.Medium) },
                    supportingContent = { Text("Current code on $branch") },
                    leadingContent = { RadioButton(selected = pinnedSha == null, onClick = null) },
                    modifier = Modifier.clickable(onClick = onViewLatest)
                )
                HorizontalDivider()
            }
            if (loading && commits.isEmpty()) {
                item(key = "loading") {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        BrandLoadingPlaceholder()
                    }
                }
            } else if (commits.isEmpty()) {
                item(key = "empty") {
                    Text(
                        "No commits found",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                items(commits, key = { it.sha }) { commit ->
                    ListItem(
                        headlineContent = {
                            Text(
                                commit.message.lineSequence().firstOrNull().orEmpty().ifBlank { "(no message)" },
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        supportingContent = {
                            Text(
                                buildString {
                                    append(commit.sha.take(7))
                                    if (commit.authorName.isNotBlank()) append(" · ").append(commit.authorName)
                                    if (commit.date.length >= 10) append(" · ").append(commit.date.take(10))
                                }
                            )
                        },
                        leadingContent = { RadioButton(selected = pinnedSha == commit.sha, onClick = null) },
                        modifier = Modifier.clickable { onViewCommit(commit) }
                    )
                    HorizontalDivider()
                }
                if (hasMore) {
                    item(key = "more") {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            if (loadingMore) {
                                BrandLoadingPlaceholder()
                            } else {
                                OutlinedButton(onClick = onLoadMore) { Text("Load more") }
                            }
                        }
                    }
                }
            }
            item(key = "bottom") { Spacer(Modifier.height(24.dp)) }
        }
    }
}
