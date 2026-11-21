package com.quickgit.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

data class DesktopBrowserTab(
    val id: Long,
    val title: String,
    val path: String? = null
)

data class DesktopChromeDest(
    val icon: ImageVector,
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit
)

data class DesktopSectionItem(
    val icon: ImageVector,
    val label: String
)

/**
 * Chromebook / Chrome-browser style top chrome for desktop — mirrors Android ChromeOS shell:
 * browser tabs + ☰ overflow destinations + optional repo section chips.
 */
@Composable
fun DesktopChromebookShell(
    tabs: List<DesktopBrowserTab>,
    activeTabId: Long?,
    onSelectTab: (Long) -> Unit,
    onCloseTab: (Long) -> Unit,
    onNewTab: () -> Unit,
    destinations: List<DesktopChromeDest>,
    sectionItems: List<DesktopSectionItem>,
    sectionIndex: Int,
    onSectionIndex: (Int) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    var opsMenuExpanded by remember { mutableStateOf(false) }
    Column(modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.fillMaxWidth()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 4.dp, top = 6.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Row(
                        Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        tabs.forEach { tab ->
                            DesktopBrowserTabChip(
                                title = tab.title,
                                selected = tab.id == activeTabId,
                                onSelect = { onSelectTab(tab.id) },
                                onClose = { onCloseTab(tab.id) }
                            )
                        }
                        IconButton(
                            onClick = onNewTab,
                            modifier = Modifier.padding(bottom = 2.dp).size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "New tab",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Box(Modifier.padding(bottom = 2.dp)) {
                        IconButton(
                            onClick = { opsMenuExpanded = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = opsMenuExpanded,
                            onDismissRequest = { opsMenuExpanded = false }
                        ) {
                            destinations.forEach { dest ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            dest.label,
                                            fontWeight = if (dest.selected) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (dest.selected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            dest.icon,
                                            contentDescription = null,
                                            tint = if (dest.selected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    onClick = {
                                        opsMenuExpanded = false
                                        dest.onClick()
                                    }
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                if (sectionItems.isNotEmpty()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        sectionItems.forEachIndexed { index, item ->
                            val selected = index == sectionIndex
                            val bg = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                            } else {
                                MaterialTheme.colorScheme.surface.copy(alpha = 0f)
                            }
                            val fg = if (selected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Row(
                                Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(bg)
                                    .clickable { onSectionIndex(index) }
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(item.icon, null, tint = fg, modifier = Modifier.size(18.dp))
                                Text(
                                    item.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = fg,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                }
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            // Subtle watermark like Android ChromeOS shell
            Icon(
                Icons.Default.AccountTree,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 28.dp)
                    .size(96.dp)
                    .alpha(0.06f),
                tint = MaterialTheme.colorScheme.onSurface
            )
            content()
        }
    }
}

@Composable
private fun DesktopBrowserTabChip(
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
        IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close tab", modifier = Modifier.size(16.dp), tint = fg)
        }
    }
}
