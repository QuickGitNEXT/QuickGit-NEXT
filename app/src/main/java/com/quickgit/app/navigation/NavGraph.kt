package com.quickgit.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.quickgit.app.BuildConfig
import com.quickgit.app.QuickGitApp
import com.quickgit.app.data.PhoneNavMode
import com.quickgit.app.data.WideUiMode
import com.quickgit.app.data.models.RepoInfo
import com.quickgit.app.ui.adaptive.LocalWindowSizeClass
import com.quickgit.app.ui.adaptive.isTabletOrWider
import androidx.compose.runtime.CompositionLocalProvider
import com.quickgit.app.ui.components.ChromeOsAppChrome
import com.quickgit.app.ui.components.ChromeOsContentWithWatermark
import com.quickgit.app.ui.components.ChromeOsRepoSectionController
import com.quickgit.app.ui.components.ChromeOsRepoTab
import com.quickgit.app.ui.components.FloatingNavItem
import com.quickgit.app.ui.components.FloatingTabletNavBar
import com.quickgit.app.ui.components.LocalChromeOsRepoSection
import com.quickgit.app.ui.components.RepoDetailBottomBar
import com.quickgit.app.ui.components.RepoDetailBottomTab
import com.quickgit.app.ui.components.chromeOsDefaultDestinations
import com.quickgit.app.ui.components.isLargePhoneForFloatingNav
import com.quickgit.app.ui.screens.*
import com.quickgit.app.ui.security.isChromebookDevice
import com.quickgit.app.viewmodel.*
import androidx.compose.runtime.collectAsState
import java.io.File

/**
 * Root navigation graph.
 *
 * Navigation chrome:
 * - Chromebook: left [NavigationRail]
 * - Tablet / large phone (> 6.3″ diagonal): floating blurred bottom bar (all destinations)
 * - Smaller phones (≤ 6.3″): original in-screen layout
 *
 * Phone size: [PhoneNavMode]. Tablet/Chromebook chrome: [WideUiMode] (Settings → Layout).
 */
@Composable
fun QuickGitNavGraph() {
    val context = LocalContext.current
    val factory = ViewModelFactory(context.applicationContext as android.app.Application)
    // Shared across Settings + Credentials so the first open pays init cost once and
    // subsequent navigations are instant (no multi-second re-verify on every open).
    val activity = context as ComponentActivity
    val settingsVm: SettingsViewModel = viewModel(viewModelStoreOwner = activity, factory = factory)
    val wsc = LocalWindowSizeClass.current
    val app = context.applicationContext as QuickGitApp
    val phoneNavMode by app.appPreferences.phoneNavMode.collectAsState()
    val wideUiMode by app.appPreferences.wideUiMode.collectAsState()
    val isChromebook = remember(context) { context.isChromebookDevice() }
    // Phones: Auto / force large (≥6.3″ floating) / force compact
    val isLargePhone = when (phoneNavMode) {
        PhoneNavMode.LARGE -> !wsc.isTabletOrWider
        PhoneNavMode.COMPACT -> false
        PhoneNavMode.AUTO -> !wsc.isTabletOrWider && context.isLargePhoneForFloatingNav()
    }
    val preferWideChrome = wsc.isTabletOrWider
    // Wide UI: tablet floating vs Chromebook top chrome
    val useChromeOsShell = when (wideUiMode) {
        WideUiMode.CHROMEBOOK -> preferWideChrome || isChromebook
        WideUiMode.TABLET -> false
        WideUiMode.AUTO -> isChromebook && preferWideChrome
    }
    val useRail = false
    // Floating navbar: large phones; tablets when not using ChromeOS shell
    val useFloatingBar =
        !useRail && !useChromeOsShell && (isLargePhone || (preferWideChrome && !useChromeOsShell))
    val floatingBarCompact = isLargePhone
    val isDesktopLayout = preferWideChrome || useChromeOsShell
    val chromeRepoTabs = remember { mutableStateListOf<ChromeOsRepoTab>() }
    val chromeRepoSection = remember { ChromeOsRepoSectionController() }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    // Tablets: hide app nav on repo detail. Large phones: hide on detail + Files/Branches/PRs/Actions
    // and show the compact repo sub-nav bar instead.
    val onRepoDetailRoute = currentRoute?.startsWith("repo_detail") == true
    val largePhoneRepoSubRoute = isLargePhone && currentRoute != null && (
        currentRoute.startsWith("repo_detail/") ||
        currentRoute.startsWith("files/") ||
        currentRoute.startsWith("branches/") ||
        currentRoute.startsWith("issues/") ||
        currentRoute.startsWith("pull_requests/") ||
        currentRoute.startsWith("workflows/") ||
        currentRoute.startsWith("releases/")
    )
    val hideFloatingBarOnRepoDetail =
        useFloatingBar && (largePhoneRepoSubRoute || (onRepoDetailRoute && !isLargePhone))

    fun repoPathFromCurrentRoute(): String? {
        // Use nav arguments — destination.route is the template (e.g. files/{repoPath}),
        // not the concrete path, which caused RepositoryNotFoundException: /{repoPath}.
        val encoded = navBackStackEntry?.arguments?.getString("repoPath") ?: return null
        if (encoded.isBlank() || '{' in encoded || '}' in encoded) return null
        return Dest.decode(encoded)
    }

    fun openRepoInChromeTab(path: String, title: String? = null) {
        val name = title ?: File(path).name.ifBlank { path.substringAfterLast('/') }
        if (chromeRepoTabs.none { it.path == path }) {
            chromeRepoTabs.add(ChromeOsRepoTab(path = path, title = name))
        }
        navController.navigate(Dest.repoDetail(path)) {
            launchSingleTop = true
        }
    }

    fun closeChromeRepoTab(path: String) {
        val idx = chromeRepoTabs.indexOfFirst { it.path == path }
        if (idx < 0) return
        chromeRepoTabs.removeAt(idx)
        val active = repoPathFromCurrentRoute()
        if (active == path) {
            val next = chromeRepoTabs.getOrNull(idx) ?: chromeRepoTabs.getOrNull(idx - 1)
            if (next != null) {
                navController.navigate(Dest.repoDetail(next.path)) {
                    launchSingleTop = true
                    popUpTo(Dest.REPO_LIST) { inclusive = false }
                }
            } else {
                navController.popBackStack(Dest.REPO_LIST, inclusive = false)
            }
        }
    }

    fun selectedRepoTab(): RepoDetailBottomTab? = when {
        currentRoute?.startsWith("files/") == true -> RepoDetailBottomTab.Files
        currentRoute?.startsWith("branches/") == true -> RepoDetailBottomTab.Branches
        currentRoute?.startsWith("pull_requests/") == true -> RepoDetailBottomTab.PullRequests
        currentRoute?.startsWith("workflows/") == true -> RepoDetailBottomTab.Actions
        currentRoute?.startsWith("releases/") == true -> RepoDetailBottomTab.Releases
        else -> null // repo_detail / issues
    }

    fun isSelected(prefix: String): Boolean {
        return currentRoute == prefix || currentRoute?.startsWith(prefix) == true
    }

    @Composable
    fun NavContent() {
        NavHost(
            navController = navController,
            startDestination = Dest.REPO_LIST,
            modifier = Modifier.fillMaxSize(),
            // Snappy tab switches for the floating bottom bar (phones ≥ 6.3" / tablets).
            enterTransition = { fadeIn(animationSpec = tween(80)) },
            exitTransition = { fadeOut(animationSpec = tween(60)) },
            popEnterTransition = { fadeIn(animationSpec = tween(80)) },
            popExitTransition = { fadeOut(animationSpec = tween(60)) }
        ) {

        composable(Dest.REPO_LIST) {
            val vm: RepoListViewModel = viewModel(factory = factory)
            RepoListScreen(
                vm = vm,
                onOpenRepo = { repo: RepoInfo ->
                    if (useChromeOsShell) openRepoInChromeTab(repo.localPath, repo.name)
                    else navController.navigate(Dest.repoDetail(repo.localPath))
                },
                onClone = { navController.navigate(Dest.CLONE) },
                onBrowseGitHub = { navController.navigate(Dest.BROWSE_GITHUB) },
                onOpenProfile = { navController.navigate(Dest.profile()) },
                onSearchPeople = { navController.navigate(Dest.USER_SEARCH) },
                onSettings = { navController.navigate(Dest.SETTINGS) { launchSingleTop = true } },
                onCredentials = { navController.navigate(Dest.CREDENTIALS) { launchSingleTop = true } },
                onLogs = { if (BuildConfig.SHOW_LOGS_UI) navController.navigate(Dest.LOGS) },
                isDesktopLayout = isDesktopLayout,
                useFloatingNav = useFloatingBar,
                isChromebookLayout = isChromebook
            )
        }





        composable(Dest.USER_SEARCH) {
            val vm: UserSearchViewModel = viewModel(factory = factory)
            UserSearchScreen(
                vm = vm,
                onBack = { navController.popBackStack() },
                onUserClick = { login -> navController.navigate(Dest.profile(login)) },
                onNeedsAuth = { navController.navigate(Dest.CREDENTIALS) { launchSingleTop = true } }
            )
        }

        composable(Dest.PROFILE_SELF) {
            val vm: ProfileViewModel = viewModel(factory = factory)
            ProfileScreen(
                vm = vm,
                login = null,
                onBack = { navController.popBackStack() },
                onOpenUser = { login -> navController.navigate(Dest.profile(login)) },
                onCloneRepo = { navController.navigate(Dest.CLONE) },
                onOpenRepo = { repo ->
                    navController.navigate(Dest.remoteBrowse(repo.ownerLogin, repo.name, repo.defaultBranch))
                },
                onNeedsAuth = { navController.navigate(Dest.CREDENTIALS) { launchSingleTop = true } },
                onOpenCredentials = { navController.navigate(Dest.CREDENTIALS) { launchSingleTop = true } },
                // Search lives on the main floating nav bar, not under Profile.
                onSearchPeople = null
            )
        }

        composable(
            Dest.PROFILE_USER,
            arguments = listOf(navArgument("login") { type = NavType.StringType })
        ) { backStackEntry ->
            val login = Dest.decode(backStackEntry.arguments!!.getString("login")!!)
            val vm: ProfileViewModel = viewModel(factory = factory)
            ProfileScreen(
                vm = vm,
                login = login,
                onBack = { navController.popBackStack() },
                onOpenUser = { other -> navController.navigate(Dest.profile(other)) },
                onCloneRepo = { navController.navigate(Dest.CLONE) },
                onOpenRepo = { repo ->
                    navController.navigate(Dest.remoteBrowse(repo.ownerLogin, repo.name, repo.defaultBranch))
                },
                onNeedsAuth = { navController.navigate(Dest.CREDENTIALS) { launchSingleTop = true } }
            )
        }

        composable(
            Dest.REMOTE_BROWSE,
            arguments = listOf(
                navArgument("owner") { type = NavType.StringType },
                navArgument("repo") { type = NavType.StringType },
                navArgument("ref") { type = NavType.StringType },
                navArgument("path") {
                    type = NavType.StringType
                    defaultValue = ""
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val owner = Dest.decode(backStackEntry.arguments!!.getString("owner")!!)
            val repo = Dest.decode(backStackEntry.arguments!!.getString("repo")!!)
            val ref = Dest.decode(backStackEntry.arguments!!.getString("ref")!!)
            val path = Dest.decode(backStackEntry.arguments?.getString("path").orEmpty())
            val vm: RemoteBrowseViewModel = viewModel(factory = factory)
            RemoteBrowseScreen(
                owner = owner,
                repo = repo,
                ref = ref,
                path = path,
                vm = vm,
                onBack = { navController.popBackStack() },
                // Open the file at whatever branch / commit the browser is currently on.
                onOpenFile = { filePath, currentRef ->
                    navController.navigate(Dest.remoteFile(owner, repo, currentRef, filePath))
                },
                onNeedsAuth = { navController.navigate(Dest.CREDENTIALS) { launchSingleTop = true } }
            )
        }

        composable(
            Dest.REMOTE_FILE,
            arguments = listOf(
                navArgument("owner") { type = NavType.StringType },
                navArgument("repo") { type = NavType.StringType },
                navArgument("ref") { type = NavType.StringType },
                navArgument("path") {
                    type = NavType.StringType
                    defaultValue = ""
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val owner = Dest.decode(backStackEntry.arguments!!.getString("owner")!!)
            val repo = Dest.decode(backStackEntry.arguments!!.getString("repo")!!)
            val ref = Dest.decode(backStackEntry.arguments!!.getString("ref")!!)
            val path = Dest.decode(backStackEntry.arguments?.getString("path").orEmpty())
            val vm: RemoteFileViewModel = viewModel(factory = factory)
            RemoteFileScreen(
                owner = owner,
                repo = repo,
                ref = ref,
                path = path,
                vm = vm,
                onBack = { navController.popBackStack() },
                onNeedsAuth = { navController.navigate(Dest.CREDENTIALS) { launchSingleTop = true } }
            )
        }

        composable(Dest.LOGS) {
            val vm: LogsViewModel = viewModel(factory = factory)
            LogsScreen(vm = vm, onBack = { navController.popBackStack() })
        }

        composable(Dest.CLONE) {
            val vm: CloneViewModel = viewModel(factory = factory)
            CloneScreen(
                vm = vm,
                onBack = { navController.popBackStack() },
                onCloned = { navController.popBackStack() },
                onNeedsAuth = { url -> navController.navigate(Dest.SETTINGS) { launchSingleTop = true } },
                onBrowseGitHub = { navController.navigate(Dest.BROWSE_GITHUB) }
            )
        }

        composable(Dest.BROWSE_GITHUB) {
            val vm: BrowseGitHubViewModel = viewModel(factory = factory)
            BrowseGitHubScreen(
                vm = vm,
                onBack = { navController.popBackStack() },
                onCloned = {
                    navController.popBackStack(Dest.REPO_LIST, inclusive = false)
                },
                // Search results can be opened read-only (branch / commit picker included).
                onOpenCode = { repo ->
                    navController.navigate(Dest.remoteBrowse(repo.ownerLogin, repo.name, repo.defaultBranch))
                },
                onNeedsAuth = { navController.navigate(Dest.CREDENTIALS) { launchSingleTop = true } }
            )
        }

        composable(Dest.SETTINGS) {
            // Reuse activity-scoped SettingsViewModel (same as Credentials) so opening
            // Settings does not re-run encrypted-store init and freeze the UI ~2s.
            SettingsScreen(
                vm = settingsVm,
                onBack = { navController.popBackStack() },
                onOpenLogs = if (BuildConfig.SHOW_LOGS_UI) {
                    { navController.navigate(Dest.LOGS) { launchSingleTop = true } }
                } else null
            )
        }

        composable(Dest.CREDENTIALS) {
            val vm = settingsVm
            CredentialsScreen(vm = vm, onBack = { navController.popBackStack() })
        }

        composable(
            Dest.REPO_DETAIL,
            arguments = listOf(navArgument("repoPath") { type = NavType.StringType })
        ) { backStackEntry ->
            val repoPath = Dest.decode(backStackEntry.arguments!!.getString("repoPath")!!)
            val vm: RepoDetailViewModel = viewModel(factory = factory)
            androidx.compose.runtime.LaunchedEffect(repoPath) { vm.init(repoPath) }
            RepoDetailScreen(
                repoPath = repoPath,
                repoName = repoPath.substringAfterLast('/'),
                vm = vm,
                onBack = { navController.popBackStack() },
                onOpenDiff = { filePath, mode -> navController.navigate(Dest.diff(repoPath, filePath, mode)) },
                onOpenFile = { filePath -> navController.navigate(Dest.editor(repoPath, filePath)) },
                onOpenHistory = { navController.navigate(Dest.history(repoPath)) },
                onOpenBranches = { navController.navigate(Dest.branches(repoPath)) },
                onOpenFiles = { navController.navigate(Dest.files(repoPath)) },
                onOpenPullRequests = { navController.navigate(Dest.pullRequests(repoPath)) },
                onOpenIssues = { navController.navigate(Dest.issues(repoPath)) },
                onOpenWorkflows = { navController.navigate(Dest.workflows(repoPath)) },
                onOpenReleases = { navController.navigate(Dest.releases(repoPath)) },
                onConflicts = { navController.navigate(Dest.merge(repoPath)) },
                onNeedsAuth = { _ -> navController.navigate(Dest.SETTINGS) { launchSingleTop = true } }
            )
        }





        composable(
            Dest.HISTORY,
            arguments = listOf(navArgument("repoPath") { type = NavType.StringType })
        ) { backStackEntry ->
            val repoPath = Dest.decode(backStackEntry.arguments!!.getString("repoPath")!!)
            val vm: HistoryViewModel = viewModel(factory = factory)
            HistoryScreen(
                repoPath = repoPath,
                vm = vm,
                onBack = { navController.popBackStack() },
                onOpenCommitDiff = { filePath, commitId ->
                    navController.navigate(Dest.diff(repoPath, filePath, "commit:$commitId"))
                },
                onBrowseTreeAtCommit = { commitId ->
                    navController.navigate(Dest.commitTree(repoPath, commitId))
                },
                onBrowseTreeBefore = { parentId ->
                    navController.navigate(Dest.commitTree(repoPath, parentId))
                }
            )
        }

        composable(
            Dest.BRANCHES,
            arguments = listOf(navArgument("repoPath") { type = NavType.StringType })
        ) { backStackEntry ->
            val repoPath = Dest.decode(backStackEntry.arguments!!.getString("repoPath")!!)
            val vm: BranchesViewModel = viewModel(factory = factory)
            BranchesScreen(
                repoPath = repoPath,
                vm = vm,
                onBack = { navController.popBackStack() },
                onOpenIssues = { navController.navigate(Dest.issues(repoPath)) { launchSingleTop = true } }
            )
        }

        composable(
            Dest.FILES,
            arguments = listOf(navArgument("repoPath") { type = NavType.StringType })
        ) { backStackEntry ->
            val repoPath = Dest.decode(backStackEntry.arguments!!.getString("repoPath")!!)
            val vm: FilesViewModel = viewModel(factory = factory)
            FilesScreen(
                repoPath = repoPath,
                vm = vm,
                onBack = { navController.popBackStack() },
                onOpenFile = { filePath -> navController.navigate(Dest.editor(repoPath, filePath)) },
                onOpenHistory = { navController.navigate(Dest.history(repoPath)) }
            )
        }

        composable(
            Dest.EDITOR,
            arguments = listOf(
                navArgument("repoPath") { type = NavType.StringType },
                navArgument("filePath") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val repoPath = Dest.decode(backStackEntry.arguments!!.getString("repoPath")!!)
            val filePath = Dest.decode(backStackEntry.arguments!!.getString("filePath")!!)
            val vm: EditorViewModel = viewModel(factory = factory)
            EditorScreen(
                repoPath = repoPath,
                relativePath = filePath,
                vm = vm,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            Dest.DIFF,
            arguments = listOf(
                navArgument("repoPath") { type = NavType.StringType },
                navArgument("filePath") { type = NavType.StringType },
                navArgument("mode") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val repoPath = Dest.decode(backStackEntry.arguments!!.getString("repoPath")!!)
            val filePath = Dest.decode(backStackEntry.arguments!!.getString("filePath")!!)
            val mode = Dest.decode(backStackEntry.arguments!!.getString("mode")!!)
            val vm: DiffViewModel = viewModel(factory = factory)
            DiffScreen(repoPath = repoPath, filePath = filePath, mode = mode, vm = vm, onBack = { navController.popBackStack() })
        }

        composable(
            Dest.COMMIT_TREE,
            arguments = listOf(
                navArgument("repoPath") { type = NavType.StringType },
                navArgument("commitId") { type = NavType.StringType },
                navArgument("path") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val repoPath = Dest.decode(backStackEntry.arguments!!.getString("repoPath")!!)
            val commitId = Dest.decode(backStackEntry.arguments!!.getString("commitId")!!)
            val path = Dest.decode(backStackEntry.arguments?.getString("path") ?: "")
            val vm: CommitTreeViewModel = viewModel(factory = factory)
            CommitTreeScreen(
                repoPath = repoPath,
                commitId = commitId,
                initialPath = path,
                vm = vm,
                onBack = { navController.popBackStack() },
                onOpenFile = { filePath ->
                    navController.navigate(Dest.commitFile(repoPath, commitId, filePath))
                },
                onNavigateDir = { dirPath ->
                    navController.navigate(Dest.commitTree(repoPath, commitId, dirPath)) {
                        // Replace so back stack doesn't grow unboundedly for deep trees
                        popUpTo(Dest.COMMIT_TREE) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            Dest.COMMIT_FILE,
            arguments = listOf(
                navArgument("repoPath") { type = NavType.StringType },
                navArgument("commitId") { type = NavType.StringType },
                navArgument("path") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val repoPath = Dest.decode(backStackEntry.arguments!!.getString("repoPath")!!)
            val commitId = Dest.decode(backStackEntry.arguments!!.getString("commitId")!!)
            val filePath = Dest.decode(backStackEntry.arguments?.getString("path") ?: "")
            val vm: CommitFileViewModel = viewModel(factory = factory)
            CommitFileScreen(
                repoPath = repoPath,
                commitId = commitId,
                filePath = filePath,
                vm = vm,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            Dest.MERGE,
            arguments = listOf(navArgument("repoPath") { type = NavType.StringType })
        ) { backStackEntry ->
            val repoPath = Dest.decode(backStackEntry.arguments!!.getString("repoPath")!!)
            val vm: MergeViewModel = viewModel(factory = factory)
            MergeScreen(
                repoPath = repoPath,
                vm = vm,
                onBack = { navController.popBackStack() },
                onFinished = { navController.popBackStack() }
            )
        }

        composable(
            Dest.ISSUES,
            arguments = listOf(navArgument("repoPath") { type = NavType.StringType })
        ) { backStackEntry ->
            val repoPath = Dest.decode(backStackEntry.arguments!!.getString("repoPath")!!)
            val vm: IssuesViewModel = viewModel(factory = factory)
            androidx.compose.runtime.LaunchedEffect(repoPath) { vm.init(repoPath) }
            IssuesScreen(
                vm = vm,
                onBack = { navController.popBackStack() },
                onNeedsAuth = { navController.navigate(Dest.CREDENTIALS) { launchSingleTop = true } }
            )
        }

        composable(
            Dest.WORKFLOWS,
            arguments = listOf(navArgument("repoPath") { type = NavType.StringType })
        ) { backStackEntry ->
            val repoPath = Dest.decode(backStackEntry.arguments!!.getString("repoPath")!!)
            val vm: WorkflowsViewModel = viewModel(factory = factory)
            androidx.compose.runtime.LaunchedEffect(repoPath) { vm.init(repoPath) }
            WorkflowsScreen(
                vm = vm,
                onBack = { navController.popBackStack() },
                onNeedsAuth = { navController.navigate(Dest.CREDENTIALS) { launchSingleTop = true } },
                repoPath = repoPath,
                showReleasesTab = false,
                onOpenIssues = { navController.navigate(Dest.issues(repoPath)) { launchSingleTop = true } }
            )
        }

        composable(
            Dest.RELEASES,
            arguments = listOf(navArgument("repoPath") { type = NavType.StringType })
        ) { backStackEntry ->
            val repoPath = Dest.decode(backStackEntry.arguments!!.getString("repoPath")!!)
            val vm: ReleasesViewModel = viewModel(factory = factory)
            androidx.compose.runtime.LaunchedEffect(repoPath) { vm.init(repoPath) }
            ReleasesScreen(
                vm = vm,
                onBack = { navController.popBackStack() },
                onNeedsAuth = { navController.navigate(Dest.CREDENTIALS) { launchSingleTop = true } },
                onOpenIssues = { navController.navigate(Dest.issues(repoPath)) { launchSingleTop = true } }
            )
        }

        composable(
            Dest.PULL_REQUESTS,
            arguments = listOf(navArgument("repoPath") { type = NavType.StringType })
        ) { backStackEntry ->
            val repoPath = Dest.decode(backStackEntry.arguments!!.getString("repoPath")!!)
            val vm: PullRequestsViewModel = viewModel(factory = factory)
            androidx.compose.runtime.LaunchedEffect(repoPath) { vm.init(repoPath) }
            PullRequestsScreen(
                vm = vm,
                onBack = { navController.popBackStack() },
                onNeedsAuth = { navController.navigate(Dest.CREDENTIALS) { launchSingleTop = true } },
                onOpenIssues = { navController.navigate(Dest.issues(repoPath)) { launchSingleTop = true } }
            )
        }


        }
    }

    /** Bottom-bar style navigate: reuse back stack entries and avoid deep stacks. */
    fun tabNavigate(route: String) {
        navController.navigate(route) {
            launchSingleTop = true
            restoreState = true
            popUpTo(Dest.REPO_LIST) {
                saveState = true
            }
        }
    }

        fun navItems(
        includeBrowse: Boolean = true,
        includeUsers: Boolean = true,
        includeLogs: Boolean = true,
        includeCreds: Boolean = true
    ): List<FloatingNavItem> {
        val items = mutableListOf(
            FloatingNavItem(
                icon = Icons.Default.Folder,
                label = "Repos",
                selected = isSelected(Dest.REPO_LIST) ||
                    isSelected("repo_detail") || isSelected("history") ||
                    isSelected("branches") || isSelected("diff") ||
                    isSelected("merge") || isSelected("files") ||
                    isSelected("editor") || isSelected("pull_requests") ||
                    isSelected("issues") || isSelected("workflows") ||
                    isSelected("releases")
            ) {
                tabNavigate(Dest.REPO_LIST)
            },
            FloatingNavItem(
                icon = Icons.Default.CloudDownload,
                label = "Clone",
                // Clone stays selected when browsing was opened from Clone on large phones.
                selected = isSelected(Dest.CLONE) ||
                    (!includeBrowse && (isSelected(Dest.BROWSE_GITHUB) ||
                        isSelected("remote_browse") || isSelected("remote_file")))
            ) {
                tabNavigate(Dest.CLONE)
            },
        )
        if (includeBrowse) {
            items += FloatingNavItem(
                icon = Icons.Default.TravelExplore,
                label = "Browse",
                selected = isSelected(Dest.BROWSE_GITHUB) ||
                    isSelected("remote_browse") || isSelected("remote_file")
            ) {
                tabNavigate(Dest.BROWSE_GITHUB)
            }
        }
        items += FloatingNavItem(
            icon = Icons.Default.Person,
            label = "Profiles",
            selected = isSelected(Dest.PROFILE_SELF) || isSelected("profile/")
        ) {
            tabNavigate(Dest.profile())
        }
        if (includeUsers) {
            items += FloatingNavItem(
                icon = Icons.Default.Search,
                label = "Search",
                selected = isSelected(Dest.USER_SEARCH)
            ) {
                tabNavigate(Dest.USER_SEARCH)
            }
        }
        if (includeLogs && BuildConfig.SHOW_LOGS_UI) {
            items += FloatingNavItem(
                icon = Icons.Default.Article,
                label = "Logs",
                selected = isSelected(Dest.LOGS)
            ) {
                tabNavigate(Dest.LOGS)
            }
        }
        if (includeCreds) {
            items += FloatingNavItem(
                icon = Icons.Default.Key,
                label = "Creds",
                selected = isSelected(Dest.CREDENTIALS)
            ) {
                tabNavigate(Dest.CREDENTIALS)
            }
        }
        items += FloatingNavItem(
            icon = Icons.Default.Settings,
            label = "Settings",
            selected = isSelected(Dest.SETTINGS)
        ) {
            tabNavigate(Dest.SETTINGS)
        }
        return items
    }

    when {
        // Chromebook shell: real Chromebook hardware, or tablet with WideUiMode.CHROMEBOOK.
        // Tablets get denser chrome (compactForTablet); hardware Chromebooks stay as designed.
        useChromeOsShell -> {
            val compactChromeForTablet = !isChromebook
            CompositionLocalProvider(LocalChromeOsRepoSection provides chromeRepoSection) {
                // Tablets in Chromebook mode still need top inset under the multi-window pill
                // when the activity is edge-to-edge; real Chromebooks keep full-bleed chrome.
                val chromeRootMod = Modifier.fillMaxSize().then(
                    if (compactChromeForTablet) {
                        Modifier.windowInsetsPadding(
                            WindowInsets.safeDrawing.only(
                                WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                            )
                        )
                    } else {
                        Modifier
                    }
                )
                Column(chromeRootMod) {
                    ChromeOsAppChrome(
                        destinations = chromeOsDefaultDestinations(
                            selectedRoute = currentRoute,
                            onRepos = {
                                chromeRepoSection.deactivate()
                                navController.navigate(Dest.REPO_LIST) {
                                    launchSingleTop = true
                                    popUpTo(Dest.REPO_LIST) { inclusive = false }
                                }
                            },
                            onClone = {
                                chromeRepoSection.deactivate()
                                navController.navigate(Dest.CLONE) { launchSingleTop = true }
                            },
                            onBrowse = {
                                chromeRepoSection.deactivate()
                                navController.navigate(Dest.BROWSE_GITHUB) { launchSingleTop = true }
                            },
                            onSearch = {
                                chromeRepoSection.deactivate()
                                navController.navigate(Dest.USER_SEARCH) { launchSingleTop = true }
                            },
                            onProfiles = {
                                chromeRepoSection.deactivate()
                                navController.navigate(Dest.profile()) { launchSingleTop = true }
                            },
                            onSettings = {
                                chromeRepoSection.deactivate()
                                navController.navigate(Dest.SETTINGS) { launchSingleTop = true }
                            }
                        ),
                        repoTabs = chromeRepoTabs.toList(),
                        activeRepoPath = repoPathFromCurrentRoute(),
                        onSelectRepoTab = { path ->
                            navController.navigate(Dest.repoDetail(path)) {
                                launchSingleTop = true
                                popUpTo(Dest.REPO_LIST) { inclusive = false }
                            }
                        },
                        onCloseRepoTab = { path -> closeChromeRepoTab(path) },
                        compactForTablet = compactChromeForTablet
                    )
                    ChromeOsContentWithWatermark(
                        showWatermark = true,
                        compactForTablet = compactChromeForTablet,
                        modifier = Modifier.weight(1f).fillMaxSize()
                    ) {
                        NavContent()
                    }
                }
            }
        }
        // Optional left NavigationRail (disabled by default).
        useRail -> {
            Row(Modifier.fillMaxSize()) {
                NavigationRail(modifier = Modifier.fillMaxHeight()) {
                    val items = navItems(includeBrowse = true, includeUsers = true)
                    val bottomLabels = setOf("Logs", "Creds", "Settings")
                    items.filter { it.label !in bottomLabels }.forEach { item ->
                        RailItem(item.icon, item.label, item.selected, item.onClick)
                    }
                    Spacer(Modifier.weight(1f))
                    items.filter { it.label in bottomLabels }.forEach { item ->
                        RailItem(item.icon, item.label, item.selected, item.onClick)
                    }
                }
                VerticalDivider()
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    NavContent()
                }
            }
        }
        // Android tablets / large phones: M3 floating bottom bar.
        useFloatingBar -> {
            // Tablets: clear status / caption bar (multi-window pill). Large phones: keep edge-to-edge.
            val floatingRootMod = Modifier.fillMaxSize().then(
                if (!isLargePhone) {
                    Modifier.windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                        )
                    )
                } else {
                    Modifier
                }
            )
            Box(floatingRootMod) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(
                            bottom = when {
                                largePhoneRepoSubRoute -> 80.dp
                                hideFloatingBarOnRepoDetail -> 0.dp
                                else -> 80.dp // compact FloatingTabletNavBar matches RepoDetailBottomBar
                            }
                        )
                ) {
                    NavContent()
                }
                if (!hideFloatingBarOnRepoDetail) {
                    FloatingTabletNavBar(
                        // Large phones: drop Browse/Logs from the bar (Logs lives in Settings).
                        // Search is always on the main floating bar (Repos, Clone, Profiles, Settings, Search).
                        // Always compact so size matches RepoDetailBottomBar.
                        items = navItems(
                            includeBrowse = !isLargePhone,
                            includeUsers = true,
                            includeLogs = !isLargePhone,
                            // Large phones: credentials live under Profile only.
                            includeCreds = !isLargePhone
                        ),
                        compact = true,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                } else if (largePhoneRepoSubRoute) {
                    val path = repoPathFromCurrentRoute()
                    if (path != null) {
                        RepoDetailBottomBar(
                            selected = selectedRepoTab(),
                            onFiles = {
                                navController.navigate(Dest.files(path)) {
                                    launchSingleTop = true
                                    popUpTo(Dest.repoDetail(path)) { inclusive = false }
                                }
                            },
                            onBranches = {
                                navController.navigate(Dest.branches(path)) {
                                    launchSingleTop = true
                                    popUpTo(Dest.repoDetail(path)) { inclusive = false }
                                }
                            },
                            onPullRequests = {
                                navController.navigate(Dest.pullRequests(path)) {
                                    launchSingleTop = true
                                    popUpTo(Dest.repoDetail(path)) { inclusive = false }
                                }
                            },
                            onActions = {
                                navController.navigate(Dest.workflows(path)) {
                                    launchSingleTop = true
                                    popUpTo(Dest.repoDetail(path)) { inclusive = false }
                                }
                            },
                            onReleases = {
                                navController.navigate(Dest.releases(path)) {
                                    launchSingleTop = true
                                    popUpTo(Dest.repoDetail(path)) { inclusive = false }
                                }
                            },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        }
        else -> NavContent()
    }
}

@Composable
private fun RailItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    NavigationRailItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label) }
    )
}
