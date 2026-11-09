package com.quickgit.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quickgit.app.ui.theme.GitGreen

data class ChromeOsRepoTab(
    val path: String,
    val title: String
)

data class ChromeOsDestItem(
    val icon: ImageVector,
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit
)

data class ChromeOsSectionItem(
    val icon: ImageVector,
    val label: String
)

/**
 * Shared Chromebook repo-section selection (Changes / Files / …).
 * Provided by [RepoDetailScreenDesktop]; read by [ChromeOsAppChrome].
 */
class ChromeOsRepoSectionController {
    val selectedIndex = mutableIntStateOf(0)
    val active = mutableStateOf(false)
    val items = mutableStateOf<List<ChromeOsSectionItem>>(emptyList())

    fun activate(sections: List<ChromeOsSectionItem>, index: Int = selectedIndex.intValue) {
        items.value = sections
        selectedIndex.intValue = index.coerceIn(0, (sections.size - 1).coerceAtLeast(0))
        active.value = true
    }

    fun deactivate() {
        active.value = false
    }
}

val LocalChromeOsRepoSection = staticCompositionLocalOf { ChromeOsRepoSectionController() }

/**
 * ChromeOS-style top chrome: app destinations + optional repo section strip + repo tabs.
 * No floating bottom bars — Chromebook / ARC desktop shells only.
 *
 * @param compactForTablet When true (tablet forcing Chromebook mode, not real Chromebook
 * hardware), use denser padding and slightly smaller controls so the strip fits portrait
 * and mid-size tablet windows. Real Chromebooks leave [compactForTablet] false.
 */
@Composable
fun ChromeOsAppChrome(
    destinations: List<ChromeOsDestItem>,
    repoTabs: List<ChromeOsRepoTab>,
    activeRepoPath: String?,
    onSelectRepoTab: (String) -> Unit,
    onCloseRepoTab: (String) -> Unit,
    modifier: Modifier = Modifier,
    compactForTablet: Boolean = false
) {
    val section = LocalChromeOsRepoSection.current
    val sectionActive = section.active.value
    val sectionItems = section.items.value
    val sectionIndex = section.selectedIndex.intValue

    val barPadH = if (compactForTablet) 8.dp else 12.dp
    val barPadV = if (compactForTablet) 4.dp else 6.dp
    val sectionPadH = if (compactForTablet) 6.dp else 10.dp
    val sectionPadV = if (compactForTablet) 2.dp else 4.dp
    val chipPadH = if (compactForTablet) 8.dp else 12.dp
    val chipPadV = if (compactForTablet) 5.dp else 7.dp
    val iconSize = if (compactForTablet) 16.dp else 18.dp

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        Column(Modifier.fillMaxWidth()) {
            // Primary destinations only — no brand logo / app name in the top bar
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = barPadH, vertical = barPadV),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (compactForTablet) 2.dp else 4.dp)
            ) {
                destinations.forEach { dest ->
                    ChromeOsToolbarItem(dest, compact = compactForTablet)
                }
            }

            // Repo workspace sections under the main menu (when a repo is open)
            if (sectionActive && sectionItems.isNotEmpty()) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = sectionPadH, vertical = sectionPadV),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    sectionItems.forEachIndexed { i, item ->
                        val selected = sectionIndex == i
                        val fg = if (selected) GitGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        // Icon-only section controls (Changes / Files / Branches / Issues / PRs / Actions / Releases)
                        IconButton(
                            onClick = { section.selectedIndex.intValue = i },
                            modifier = Modifier
                                .size(if (compactForTablet) 36.dp else 40.dp)
                                .clip(RoundedCornerShape(if (compactForTablet) 6.dp else 8.dp))
                                .background(
                                    if (selected) GitGreen.copy(alpha = 0.14f)
                                    else MaterialTheme.colorScheme.surface.copy(alpha = 0f)
                                )
                        ) {
                            Icon(
                                item.icon,
                                contentDescription = item.label,
                                tint = fg,
                                modifier = Modifier.size(iconSize)
                            )
                        }
                    }
                }
            }

            // Open-repo tab strip
            if (repoTabs.isNotEmpty()) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    repoTabs.forEach { tab ->
                        val selected = tab.path == activeRepoPath
                        ChromeOsRepoTabChip(
                            title = tab.title,
                            selected = selected,
                            onSelect = { onSelectRepoTab(tab.path) },
                            onClose = { onCloseRepoTab(tab.path) }
                        )
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }
    }
}

/** Soft brand watermark for Chromebook pages (bottom-centered). */
@Composable
fun BoxScope.ChromeOsWatermark(compactForTablet: Boolean = false) {
    AppBrandLogo(
        size = if (compactForTablet) 72.dp else 96.dp,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = if (compactForTablet) 16.dp else 28.dp)
            .alpha(if (compactForTablet) 0.05f else 0.07f)
    )
}

/** Content host with optional Chromebook watermark behind [content]. */
@Composable
fun ChromeOsContentWithWatermark(
    showWatermark: Boolean,
    modifier: Modifier = Modifier,
    compactForTablet: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier.fillMaxSize()) {
        if (showWatermark) {
            ChromeOsWatermark(compactForTablet = compactForTablet)
        }
        content()
    }
}

@Composable
private fun ChromeOsToolbarItem(item: ChromeOsDestItem, compact: Boolean = false) {
    val bg = if (item.selected) {
        GitGreen.copy(alpha = 0.16f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0f)
    }
    val fg = if (item.selected) GitGreen else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        Modifier
            .clip(RoundedCornerShape(if (compact) 6.dp else 8.dp))
            .background(bg)
            .clickable(onClick = item.onClick)
            .padding(
                horizontal = if (compact) 8.dp else 10.dp,
                vertical = if (compact) 5.dp else 6.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp)
    ) {
        Icon(
            item.icon,
            contentDescription = item.label,
            tint = fg,
            modifier = Modifier.size(if (compact) 16.dp else 18.dp)
        )
        Text(
            item.label,
            style = if (compact) MaterialTheme.typography.labelMedium
            else MaterialTheme.typography.labelLarge,
            color = fg,
            fontWeight = if (item.selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun ChromeOsRepoTabChip(
    title: String,
    selected: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit
) {
    val shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)
    val bg = if (selected) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f)
    }
    val fg = if (selected) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        Modifier
            .widthIn(min = 120.dp, max = 220.dp)
            .clip(shape)
            .background(bg)
            .clickable(onClick = onSelect)
            .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = fg,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = onClose,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Close tab",
                modifier = Modifier.size(16.dp),
                tint = fg
            )
        }
    }
}

/** Default Chromebook primary destinations. */
fun chromeOsDefaultDestinations(
    selectedRoute: String?,
    onRepos: () -> Unit,
    onClone: () -> Unit,
    onBrowse: () -> Unit,
    onSearch: () -> Unit,
    onProfiles: () -> Unit,
    onSettings: () -> Unit
): List<ChromeOsDestItem> {
    fun sel(prefix: String) =
        selectedRoute == prefix || selectedRoute?.startsWith(prefix) == true
    return listOf(
        ChromeOsDestItem(
            Icons.Default.FolderOpen,
            "Repos",
            sel("repo_list") || selectedRoute == null,
            onRepos
        ),
        ChromeOsDestItem(Icons.Default.CloudDownload, "Clone", sel("clone"), onClone),
        ChromeOsDestItem(Icons.Default.TravelExplore, "Browse", sel("browse_github"), onBrowse),
        ChromeOsDestItem(Icons.Default.Search, "Search", sel("user_search"), onSearch),
        ChromeOsDestItem(Icons.Default.Person, "Profiles", sel("profile"), onProfiles),
        ChromeOsDestItem(Icons.Default.Settings, "Settings", sel("settings"), onSettings)
    )
}
