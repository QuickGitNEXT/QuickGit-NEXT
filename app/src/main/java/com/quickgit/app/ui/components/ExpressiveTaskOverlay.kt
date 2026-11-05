package com.quickgit.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

// Brand blues from the QuickGitNEXT launcher mark
private val BrandBlue = Color(0xFF1D4ED8)
private val BrandNavy = Color(0xFF1E3A8A)
private val BrandAccent = Color(0xFF2563EB)
private val BrandDisk = Color(0xFFF0F7FF)

/**
 * QuickGitNEXT branded full-screen task indicator.
 *
 * Animates the app’s git-branch + N monogram while push/pull/commit (and similar)
 * operations run. On success, briefly shows a check then dismisses.
 */
@Composable
fun ExpressiveTaskOverlay(
    visible: Boolean,
    label: String,
    success: Boolean = false,
    onSuccessConsumed: () -> Unit = {}
) {
    LaunchedEffect(success) {
        if (success) {
            delay(900)
            onSuccessConsumed()
        }
    }

    AnimatedVisibility(
        visible = visible || success,
        enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.92f, animationSpec = tween(220)),
        exit = fadeOut(tween(160)) + scaleOut(targetScale = 0.94f, animationSpec = tween(160))
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                QuickGitNextLoader(success = success)
                Spacer(Modifier.height(20.dp))
                Text(
                    text = when {
                        success -> "Done"
                        label.isNotBlank() -> label
                        else -> "Working…"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                if (!success) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "QuickGitNEXT",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickGitNextLoader(success: Boolean) {
    val transition = rememberInfiniteTransition(label = "qgnext-loader")
    val pulse by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val travel by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "travel"
    )
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin"
    )
    val dash by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dash"
    )

    Canvas(Modifier.size(112.dp)) {
        val s = size.minDimension
        val cx = size.width / 2f
        val cy = size.height / 2f
        val scale = if (success) 1.02f else pulse

        // Soft brand disk
        drawCircle(
            color = BrandDisk.copy(alpha = 0.95f),
            radius = s * 0.42f * scale,
            center = Offset(cx, cy)
        )
        // Outer ring
        drawCircle(
            color = BrandBlue.copy(alpha = 0.22f),
            radius = s * 0.46f * scale,
            center = Offset(cx, cy),
            style = Stroke(width = s * 0.02f)
        )

        // Coordinate helpers matching the launcher mark proportions
        fun px(x: Float) = cx + (x - 54f) / 54f * s * 0.32f * scale
        fun py(y: Float) = cy + (y - 54f) / 54f * s * 0.32f * scale

        val trunkTop = Offset(px(38f), py(40f))
        val trunkMid = Offset(px(38f), py(52f))
        val trunkBot = Offset(px(38f), py(72f))
        val branchTip = Offset(px(52f), py(40f))

        val strokeW = s * 0.028f
        val nodeR = s * 0.028f

        if (!success) {
            // Animated branch strokes
            drawLine(
                color = BrandBlue,
                start = trunkBot,
                end = trunkTop,
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )
            drawLine(
                color = BrandBlue,
                start = trunkMid,
                end = branchTip,
                strokeWidth = strokeW * 0.95f,
                cap = StrokeCap.Round
            )

            // Nodes
            listOf(trunkBot, trunkMid, trunkTop, branchTip).forEach {
                drawCircle(BrandNavy, radius = nodeR, center = it)
            }

            // Commit “packet” traveling trunk → branch
            val packet = when {
                travel < 0.45f -> {
                    val t = travel / 0.45f
                    Offset(
                        trunkBot.x + (trunkTop.x - trunkBot.x) * t,
                        trunkBot.y + (trunkTop.y - trunkBot.y) * t
                    )
                }
                travel < 0.7f -> {
                    val t = (travel - 0.45f) / 0.25f
                    Offset(
                        trunkMid.x + (branchTip.x - trunkMid.x) * t,
                        trunkMid.y + (branchTip.y - trunkMid.y) * t
                    )
                }
                else -> branchTip
            }
            drawCircle(BrandAccent, radius = nodeR * 1.35f, center = packet)
            drawCircle(Color.White.copy(alpha = 0.85f), radius = nodeR * 0.45f, center = packet)

            // Stylized N (NEXT)
            val nLeftTop = Offset(px(62f), py(38f))
            val nLeftBot = Offset(px(62f), py(72f))
            val nRightTop = Offset(px(78f), py(38f))
            val nRightBot = Offset(px(78f), py(72f))
            val nPath = Path().apply {
                moveTo(nLeftBot.x, nLeftBot.y)
                lineTo(nLeftTop.x, nLeftTop.y)
                lineTo(nRightBot.x, nRightBot.y)
                lineTo(nRightTop.x, nRightTop.y)
            }
            drawPath(
                path = nPath,
                color = BrandNavy.copy(alpha = 0.55f + dash * 0.45f),
                style = Stroke(
                    width = strokeW * 1.15f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
            // Accent underline under N
            val barY = py(78f)
            drawLine(
                color = BrandAccent.copy(alpha = 0.5f + dash * 0.5f),
                start = Offset(px(60f), barY),
                end = Offset(px(80f), barY),
                strokeWidth = s * 0.018f,
                cap = StrokeCap.Round
            )

            // Orbiting status dots around the disk
            for (i in 0 until 3) {
                val a = Math.toRadians((spin + i * 120.0))
                val rad = s * 0.48f
                val x = cx + (cos(a) * rad).toFloat()
                val y = cy + (sin(a) * rad).toFloat()
                drawCircle(
                    color = BrandBlue.copy(alpha = 0.8f - i * 0.18f),
                    radius = s * 0.022f,
                    center = Offset(x, y)
                )
            }
        } else {
            // Success: solid branch + check
            drawLine(BrandBlue, trunkBot, trunkTop, strokeW, StrokeCap.Round)
            drawLine(BrandBlue, trunkMid, branchTip, strokeW * 0.95f, StrokeCap.Round)
            listOf(trunkBot, trunkMid, trunkTop, branchTip).forEach {
                drawCircle(BrandNavy, radius = nodeR, center = it)
            }
            rotate(degrees = 0f, pivot = Offset(cx, cy)) {
                val check = Path().apply {
                    moveTo(cx - s * 0.08f, cy + s * 0.02f)
                    lineTo(cx - s * 0.01f, cy + s * 0.1f)
                    lineTo(cx + s * 0.12f, cy - s * 0.08f)
                }
                drawPath(
                    path = check,
                    color = BrandAccent,
                    style = Stroke(
                        width = s * 0.045f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}
