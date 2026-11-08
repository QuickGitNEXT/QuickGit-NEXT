package com.quickgit.app.ui.components

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quickgit.app.ui.theme.GitGreen

enum class RepoDetailBottomTab {
    Files, Branches, PullRequests, Actions, Releases
}

/**
 * Compact floating bar matching [FloatingTabletNavBar] large-phone sizing.
 * Used on repo detail and repo sub-pages (Files / Branches / PRs / Actions / Releases / Issues).
 */
@Composable
fun RepoDetailBottomBar(
    selected: RepoDetailBottomTab?,
    onFiles: () -> Unit,
    onBranches: () -> Unit,
    onPullRequests: () -> Unit,
    onActions: () -> Unit,
    onReleases: () -> Unit,
    prLabel: String = "PRs",
    actionsLabel: String = "Actions",
    modifier: Modifier = Modifier
) {
    val items = listOf(
        Triple(Icons.Default.FolderOpen, "Files", onFiles to RepoDetailBottomTab.Files),
        Triple(Icons.Default.AccountTree, "Branches", onBranches to RepoDetailBottomTab.Branches),
        Triple(Icons.Default.CallMerge, prLabel, onPullRequests to RepoDetailBottomTab.PullRequests),
        Triple(Icons.Default.PlayCircle, actionsLabel, onActions to RepoDetailBottomTab.Actions),
        Triple(Icons.Default.NewReleases, "Releases", onReleases to RepoDetailBottomTab.Releases),
    )
    val navPad = WindowInsets.navigationBars.asPaddingValues()
    val shape = RoundedCornerShape(22.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(navPad)
            .padding(horizontal = 28.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                // Same max width as FloatingTabletNavBar so the bars match end-to-end
                .widthIn(max = 560.dp)
                .fillMaxWidth(0.94f)
                .clip(shape)
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Box(
                    Modifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f))
                        .blur(20.dp)
                )
            }
            Surface(
                shape = shape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(
                    alpha = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 0.78f else 0.96f
                ),
                tonalElevation = 4.dp,
                shadowElevation = 12.dp,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEach { (icon, label, actionSel) ->
                        val (onClick, tab) = actionSel
                        val isSelected = selected == tab
                        val selectedBg = if (isSelected) GitGreen.copy(alpha = 0.2f) else Color.Transparent
                        val contentColor =
                            if (isSelected) GitGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        val interaction = remember { MutableInteractionSource() }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(selectedBg)
                                .clickable(
                                    interactionSource = interaction,
                                    indication = null,
                                    onClick = onClick
                                )
                                .padding(vertical = 3.dp, horizontal = 1.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                icon as ImageVector,
                                contentDescription = label,
                                tint = contentColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                color = contentColor,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
