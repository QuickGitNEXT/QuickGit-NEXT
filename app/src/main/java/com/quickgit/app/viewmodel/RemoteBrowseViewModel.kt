package com.quickgit.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quickgit.app.data.GitHubAccountManager
import com.quickgit.app.data.github.GitHubApi
import com.quickgit.app.data.models.PrOpResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class RemoteBrowseUiState(
    val owner: String = "",
    val repo: String = "",
    /** What is actually being read: a branch name, or a commit SHA while a commit is pinned. */
    val ref: String = "",
    /** Branch the user is on. Commit history is listed for this branch. */
    val branch: String = "",
    /** Older commit being viewed, or null when viewing the latest code on [branch]. */
    val pinnedCommit: GitHubApi.RemoteCommit? = null,
    val currentPath: String = "",
    val entries: List<GitHubApi.RemoteEntry> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val authRequired: Boolean = false,
    val branches: List<String> = emptyList(),
    val branchesLoading: Boolean = false,
    val commits: List<GitHubApi.RemoteCommit> = emptyList(),
    val commitsLoading: Boolean = false,
    val commitsLoadingMore: Boolean = false,
    val hasMoreCommits: Boolean = false
)

/**
 * Read-only directory listing for someone else's (or your own) repo, straight from the GitHub API — no clone.
 * Supports switching branches and viewing the code as of an older commit.
 */
class RemoteBrowseViewModel(
    private val accountManager: GitHubAccountManager
) : ViewModel() {

    private val _state = MutableStateFlow(RemoteBrowseUiState())
    val state: StateFlow<RemoteBrowseUiState> = _state.asStateFlow()

    /** Identifies the last successful init so returning from a file does not reset branch/folder. */
    private var initKey: String? = null
    private var commitsPage = 1
    private val commitsPageSize = 30

    fun init(owner: String, repo: String, ref: String, path: String) {
        val key = "$owner/$repo@$ref:$path"
        // The screen re-runs this whenever it re-enters composition (e.g. Back from a file).
        // Keep the user's branch / commit / folder in that case instead of resetting to the root.
        if (key == initKey) return
        initKey = key
        commitsPage = 1
        _state.value = RemoteBrowseUiState(owner = owner, repo = repo, ref = ref, branch = ref)
        openDir(path)
    }

    /**
     * Lists [path] at the current ref. When [fallbackToRoot] is set and the folder does not exist
     * at that ref (different branch / older commit), the repo root is shown instead of an error.
     */
    fun openDir(path: String, fallbackToRoot: Boolean = false) {
        val s = _state.value
        val owner = s.owner
        val repo = s.repo
        val ref = s.ref
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null, currentPath = path, authRequired = false)
            val (entries, result) = withContext(Dispatchers.IO) {
                accountManager.getRepoContents(owner, repo, path, ref)
            }
            // Ignore a response that finished after the user already switched branch/commit.
            if (_state.value.ref != ref) return@launch
            when (result) {
                is PrOpResult.Success ->
                    _state.value = _state.value.copy(loading = false, entries = entries)
                is PrOpResult.AuthRequired -> {
                    initKey = null
                    _state.value = _state.value.copy(loading = false, authRequired = true)
                }
                is PrOpResult.Error -> {
                    if (fallbackToRoot && path.isNotBlank()) {
                        openDir("")
                    } else {
                        initKey = null
                        _state.value = _state.value.copy(loading = false, error = result.message)
                    }
                }
            }
        }
    }

    fun goUp() {
        val current = _state.value.currentPath
        if (current.isBlank()) return
        openDir(current.substringBeforeLast('/', missingDelimiterValue = ""))
    }

    /** Switch to the latest code on [name]; keeps the current folder when it exists there. */
    fun selectBranch(name: String) {
        val s = _state.value
        if (name == s.branch && s.pinnedCommit == null) return
        commitsPage = 1
        _state.value = s.copy(
            branch = name,
            ref = name,
            pinnedCommit = null,
            commits = emptyList(),
            commitsLoading = false,
            commitsLoadingMore = false,
            hasMoreCommits = false
        )
        openDir(s.currentPath, fallbackToRoot = true)
    }

    /** View the code as it was at [commit]; keeps the current folder when it existed back then. */
    fun viewCommit(commit: GitHubApi.RemoteCommit) {
        val s = _state.value
        if (s.pinnedCommit?.sha == commit.sha) return
        _state.value = s.copy(ref = commit.sha, pinnedCommit = commit)
        openDir(s.currentPath, fallbackToRoot = true)
    }

    /** Leave an older commit and go back to the latest code on the selected branch. */
    fun returnToLatest() {
        val s = _state.value
        if (s.pinnedCommit == null) return
        _state.value = s.copy(ref = s.branch, pinnedCommit = null)
        openDir(s.currentPath, fallbackToRoot = true)
    }

    /** Fetch the branch list once per screen. */
    fun loadBranches() {
        val s = _state.value
        if (s.branchesLoading || s.branches.isNotEmpty()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(branchesLoading = true)
            val (names, result) = withContext(Dispatchers.IO) {
                accountManager.listRepoBranches(s.owner, s.repo)
            }
            when (result) {
                is PrOpResult.Success ->
                    _state.value = _state.value.copy(branchesLoading = false, branches = names)
                is PrOpResult.AuthRequired ->
                    _state.value = _state.value.copy(branchesLoading = false, authRequired = true)
                is PrOpResult.Error ->
                    _state.value = _state.value.copy(branchesLoading = false, error = result.message)
            }
        }
    }

    /**
     * Load the newest page of commits for the selected branch. With [refresh] the existing list is
     * kept on screen while the new page loads, so reopening the sheet does not flicker.
     */
    fun loadCommits(refresh: Boolean = false) {
        val s = _state.value
        if (s.commitsLoading || s.commitsLoadingMore) return
        if (!refresh && s.commits.isNotEmpty()) return
        val branch = s.branch
        viewModelScope.launch {
            _state.value = _state.value.copy(commitsLoading = true)
            val (batch, hasMore, result) = withContext(Dispatchers.IO) {
                accountManager.listRepoCommits(s.owner, s.repo, branch, page = 1, perPage = commitsPageSize)
            }
            if (_state.value.branch != branch) {
                // Branch changed while loading; selectBranch already reset the list.
                _state.value = _state.value.copy(commitsLoading = false)
                return@launch
            }
            when (result) {
                is PrOpResult.Success -> {
                    commitsPage = 1
                    _state.value = _state.value.copy(
                        commitsLoading = false,
                        commits = batch,
                        hasMoreCommits = hasMore
                    )
                }
                is PrOpResult.AuthRequired ->
                    _state.value = _state.value.copy(commitsLoading = false, authRequired = true)
                is PrOpResult.Error ->
                    _state.value = _state.value.copy(commitsLoading = false, error = result.message)
            }
        }
    }

    /** Append the next page of older commits. */
    fun loadMoreCommits() {
        val s = _state.value
        if (!s.hasMoreCommits || s.commitsLoading || s.commitsLoadingMore) return
        val branch = s.branch
        val next = commitsPage + 1
        viewModelScope.launch {
            _state.value = _state.value.copy(commitsLoadingMore = true)
            val (batch, hasMore, result) = withContext(Dispatchers.IO) {
                accountManager.listRepoCommits(s.owner, s.repo, branch, page = next, perPage = commitsPageSize)
            }
            if (_state.value.branch != branch) {
                _state.value = _state.value.copy(commitsLoadingMore = false)
                return@launch
            }
            when (result) {
                is PrOpResult.Success -> {
                    commitsPage = next
                    val existing = _state.value.commits
                    val seen = existing.map { it.sha }.toHashSet()
                    _state.value = _state.value.copy(
                        commitsLoadingMore = false,
                        commits = existing + batch.filter { it.sha !in seen },
                        hasMoreCommits = hasMore
                    )
                }
                is PrOpResult.AuthRequired ->
                    _state.value = _state.value.copy(commitsLoadingMore = false, authRequired = true)
                is PrOpResult.Error ->
                    _state.value = _state.value.copy(commitsLoadingMore = false, error = result.message)
            }
        }
    }

    fun consumeError() {
        _state.value = _state.value.copy(error = null)
    }

    /** Clear the one-shot auth signal so returning from Settings does not re-navigate. */
    fun consumeAuthRequired() {
        _state.value = _state.value.copy(authRequired = false)
    }
}
