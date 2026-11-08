package com.quickgit.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

/**
 * ChromeOS-style top chrome: app destinations toolbar + optional Chrome-like repo tabs.
 * No floating bottom bars — meant for Chromebook / ARC desktop shells only.
 */
@Composable
fun ChromeOsAppChrome(
    destinations: List<ChromeOsDestItem>,
    repoTabs: List<ChromeOsRepoTab>,
    activeRepoPath: String?,
    onSelectRepoTab: (String) -> Unit,
    onCloseRepoTab: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        Column(Modifier.fillMaxWidth()) {
            // Toolbar — Chrome-like denser strip
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AppBrandLogo(size = 28.dp)
                Spacer(Modifier.width(8.dp))
                Text(
                    "QuickGitNEXT",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Spacer(Modifier.width(16.dp))
                destinations.forEach { dest ->
                    ChromeOsToolbarItem(dest)
                }
                Spacer(Modifier.weight(1f))
            }

            // Repo tab strip (only when at least one repo is open)
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

@Composable
private fun ChromeOsToolbarItem(item: ChromeOsDestItem) {
    val bg = if (item.selected) {
        GitGreen.copy(alpha = 0.16f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0f)
    }
    val fg = if (item.selected) GitGreen else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(onClick = item.onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(item.icon, contentDescription = item.label, tint = fg, modifier = Modifier.size(18.dp))
        Text(
            item.label,
            style = MaterialTheme.typography.labelLarge,
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
