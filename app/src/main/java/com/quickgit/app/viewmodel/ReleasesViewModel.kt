package com.quickgit.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quickgit.app.data.ReleaseManager
import com.quickgit.app.data.models.PrOpResult
import com.quickgit.app.data.models.Release
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ReleasesUiState(
    val supported: Boolean = true,
    val releases: List<Release> = emptyList(),
    val loading: Boolean = false,
    val busy: Boolean = false,
    val errorMessage: String? = null,
    val authRequiredHost: String? = null,
    val statusMessage: String? = null,
    val selected: Release? = null,
    val detailLoading: Boolean = false
)

class ReleasesViewModel(private val releaseManager: ReleaseManager) : ViewModel() {

    private val _state = MutableStateFlow(ReleasesUiState())
    val state: StateFlow<ReleasesUiState> = _state.asStateFlow()

    private lateinit var repoPath: String
    private var owner: String? = null
    private var repo: String? = null

    fun init(repoPath: String) {
        this.repoPath = repoPath
        viewModelScope.launch {
            val ownerRepo = withContext(Dispatchers.IO) { releaseManager.ownerRepoFor(repoPath) }
            if (ownerRepo == null) {
                _state.value = _state.value.copy(supported = false)
                return@launch
            }
            owner = ownerRepo.owner
            repo = ownerRepo.repo
            refresh()
        }
    }

    fun refresh() {
        val o = owner ?: return
        val r = repo ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            val (releases, result) = withContext(Dispatchers.IO) {
                releaseManager.listReleases(o, r)
            }
            _state.value = applyResult(
                _state.value.copy(loading = false, releases = releases),
                result
            )
        }
    }

    fun openDetail(releaseId: Long) {
        val o = owner ?: return
        val r = repo ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(detailLoading = true, selected = null)
            val (release, result) = withContext(Dispatchers.IO) {
                releaseManager.getRelease(o, r, releaseId)
            }
            _state.value = applyResult(
                _state.value.copy(detailLoading = false, selected = release),
                result
            )
        }
    }

    fun closeDetail() {
        _state.value = _state.value.copy(selected = null)
    }

    fun createRelease(
        tagName: String,
        name: String?,
        body: String?,
        draft: Boolean,
        prerelease: Boolean,
        targetCommitish: String?,
        assets: List<Pair<String, ByteArray>> = emptyList()
    ) {
        val o = owner ?: return
        val r = repo ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true)
            val (created, result) = withContext(Dispatchers.IO) {
                releaseManager.createRelease(o, r, tagName, name, body, draft, prerelease, targetCommitish)
            }
            if (created != null && assets.isNotEmpty()) {
                for ((fileName, bytes) in assets) {
                    withContext(Dispatchers.IO) {
                        releaseManager.uploadReleaseAsset(o, r, created.id, fileName, bytes)
                    }
                }
            }
            _state.value = applyResult(
                _state.value.copy(
                    busy = false,
                    statusMessage = if (created != null) "Release “${created.tagName}” created" else null
                ),
                result
            )
            if (created != null) refresh()
        }
    }

    fun updateRelease(
        releaseId: Long,
        tagName: String?,
        name: String?,
        body: String?,
        draft: Boolean?,
        prerelease: Boolean?
    ) {
        val o = owner ?: return
        val r = repo ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true)
            val (updated, result) = withContext(Dispatchers.IO) {
                releaseManager.updateRelease(o, r, releaseId, tagName, name, body, draft, prerelease)
            }
            _state.value = applyResult(
                _state.value.copy(
                    busy = false,
                    selected = updated ?: _state.value.selected,
                    statusMessage = if (updated != null) "Release updated" else null
                ),
                result
            )
            if (updated != null) refresh()
        }
    }

    fun consumeMessages() {
        _state.value = _state.value.copy(
            errorMessage = null,
            statusMessage = null,
            authRequiredHost = null
        )
    }

    private fun applyResult(state: ReleasesUiState, result: PrOpResult): ReleasesUiState =
        when (result) {
            is PrOpResult.Success -> state.copy(
                errorMessage = null,
                authRequiredHost = null
            )
            is PrOpResult.Error -> state.copy(errorMessage = result.message)
            is PrOpResult.AuthRequired -> state.copy(authRequiredHost = result.host)
        }
}
