package com.quickgit.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quickgit.app.R

/**
 * Centered QuickGitNEXT mark (branch + N) for top bars and loaders.
 */
@Composable
fun AppBrandLogo(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp
) {
    Image(
        painter = painterResource(R.drawable.ic_launcher_foreground),
        contentDescription = "QuickGitNEXT",
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit
    )
}

/**
 * Standard screen header: centered logo with the page name under it.
 */
@Composable
fun AppScreenTitle(
    pageTitle: String,
    modifier: Modifier = Modifier,
    logoSize: Dp = 36.dp,
    subtitle: String? = null,
    /** When false (embedded tablet sub-tabs), only the page title is shown. */
    showLogo: Boolean = true
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showLogo) {
            AppBrandLogo(size = logoSize)
        }
        Text(
            text = pageTitle,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
