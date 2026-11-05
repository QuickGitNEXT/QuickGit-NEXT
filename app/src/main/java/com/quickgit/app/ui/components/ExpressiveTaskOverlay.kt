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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/**
 * Material 3 Expressive–style full-screen task indicator.
 *
 * Morphing rounded shapes replace blocking "Done" dialogs while push/pull/commit
 * and similar git operations run. On success, briefly pulses then dismisses.
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
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ExpressiveShapesLoader(
                    accent = if (success) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.tertiary,
                    secondary = MaterialTheme.colorScheme.secondary,
                    success = success
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    text = when {
                        success -> "Done"
                        label.isNotBlank() -> label
                        else -> "Working…"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun ExpressiveShapesLoader(
    accent: Color,
    secondary: Color,
    success: Boolean
) {
    val transition = rememberInfiniteTransition(label = "m3e-shapes")
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin"
    )
    val morph by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "morph"
    )
    val pulse by transition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Canvas(Modifier.size(96.dp)) {
        val w = size.minDimension
        val cx = size.width / 2f
        val cy = size.height / 2f
        val base = w * 0.28f

        // Soft ambient disc
        drawCircle(
            color = accent.copy(alpha = 0.12f),
            radius = w * 0.48f * if (success) 1.05f else pulse,
            center = Offset(cx, cy)
        )

        rotate(degrees = if (success) 0f else spin * 0.35f, pivot = Offset(cx, cy)) {
            // Outer morphing rounded square
            val corner = base * (0.35f + morph * 0.55f)
            val outer = w * 0.34f * if (success) 1f else pulse
            drawRoundRect(
                color = accent.copy(alpha = 0.85f),
                topLeft = Offset(cx - outer, cy - outer),
                size = Size(outer * 2, outer * 2),
                cornerRadius = CornerRadius(corner, corner)
            )
        }

        rotate(degrees = if (success) 0f else -spin * 0.55f, pivot = Offset(cx, cy)) {
            // Inner diamond / squircle that counter-rotates
            val inset = w * 0.16f * (0.85f + morph * 0.25f)
            val path = Path().apply {
                val r = inset * (0.4f + (1f - morph) * 0.35f)
                addRoundRect(
                    RoundRect(
                        left = cx - inset,
                        top = cy - inset,
                        right = cx + inset,
                        bottom = cy + inset,
                        cornerRadius = CornerRadius(r, r)
                    )
                )
            }
            scale(if (success) 1.05f else 0.95f + morph * 0.1f, Offset(cx, cy)) {
                drawPath(path, color = secondary.copy(alpha = 0.9f))
            }
        }

        // Orbiting dots (expressive motion)
        if (!success) {
            for (i in 0 until 3) {
                val a = Math.toRadians((spin + i * 120.0))
                val rad = w * 0.42f
                val x = cx + (cos(a) * rad).toFloat()
                val y = cy + (sin(a) * rad).toFloat()
                drawCircle(
                    color = accent.copy(alpha = 0.75f - i * 0.15f),
                    radius = w * 0.045f,
                    center = Offset(x, y)
                )
            }
        } else {
            // Success check mark
            val path = Path().apply {
                moveTo(cx - w * 0.14f, cy)
                lineTo(cx - w * 0.02f, cy + w * 0.12f)
                lineTo(cx + w * 0.16f, cy - w * 0.12f)
            }
            drawPath(
                path = path,
                color = Color.White,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = w * 0.06f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                )
            )
        }
    }
}
