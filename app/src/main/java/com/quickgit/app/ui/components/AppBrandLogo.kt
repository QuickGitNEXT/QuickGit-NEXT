package com.quickgit.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quickgit.app.R

/**
 * Centered QuickGitNEXT mark (branch + N) for top bars and loaders.
 * Use instead of a "QuickGitNEXT" text title on Android screens.
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
