package com.quickgit.app.data

import com.quickgit.app.data.github.GitHubApi
import com.quickgit.app.data.github.toPrOpResult
import com.quickgit.app.data.models.PrOpResult
import com.quickgit.app.data.models.Release

/**
 * GitHub Releases for github.com remotes, using the same HTTPS token as other GitHub API flows.
 */
class ReleaseManager(
    private val repoManager: RepoManager,
    private val credentialStore: CredentialStore
) {
    private val host = "github.com"

    private val api: GitHubApi get() = GitHubApi(credentialStore.getHttpsToken(host))

    fun ownerRepoFor(path: String): GitHubApi.OwnerRepo? {
        val remoteUrl = repoManager.openGit(path).use {
            it.repository.config.getString("remote", "origin", "url")
        }
        return GitHubApi(null).parseOwnerRepo(remoteUrl)
    }

    fun listReleases(owner: String, repo: String): Pair<List<Release>, PrOpResult> {
        val result = api.listReleases(owner, repo)
        return (result.getOrNull() ?: emptyList()) to result.toPrOpResult(host)
    }

    fun getRelease(owner: String, repo: String, releaseId: Long): Pair<Release?, PrOpResult> {
        val result = api.getRelease(owner, repo, releaseId)
        return result.getOrNull() to result.toPrOpResult(host)
    }

    fun getLatestRelease(owner: String, repo: String): Pair<Release?, PrOpResult> {
        val result = api.getLatestRelease(owner, repo)
        return result.getOrNull() to result.toPrOpResult(host)
    }

    fun createRelease(
        owner: String,
        repo: String,
        tagName: String,
        name: String?,
        body: String?,
        draft: Boolean,
        prerelease: Boolean,
        targetCommitish: String?
    ): Pair<Release?, PrOpResult> {
        val result = api.createRelease(owner, repo, tagName, name, body, draft, prerelease, targetCommitish)
        return result.getOrNull() to result.toPrOpResult(host)
    }

    fun updateRelease(
        owner: String,
        repo: String,
        releaseId: Long,
        tagName: String?,
        name: String?,
        body: String?,
        draft: Boolean?,
        prerelease: Boolean?
    ): Pair<Release?, PrOpResult> {
        val result = api.updateRelease(owner, repo, releaseId, tagName, name, body, draft, prerelease)
        return result.getOrNull() to result.toPrOpResult(host)
    }

    fun uploadReleaseAsset(
        owner: String,
        repo: String,
        releaseId: Long,
        fileName: String,
        bytes: ByteArray,
        contentType: String = "application/octet-stream"
    ): Pair<com.quickgit.app.data.models.ReleaseAsset?, PrOpResult> {
        val result = api.uploadReleaseAsset(owner, repo, releaseId, fileName, bytes, contentType)
        return result.getOrNull() to result.toPrOpResult(host)
    }
}
