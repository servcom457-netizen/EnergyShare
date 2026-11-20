package com.batteryshare.ai.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.batteryshare.ai.ui.theme.ActiveGreen
import com.batteryshare.ai.ui.theme.ElectricBlueBright
import com.batteryshare.ai.ui.theme.NightBlueElevated

@Composable
fun BatteryShareLogo(
    size: Dp = 44.dp,
    isActiveTransfer: Boolean = false,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "energy_pulse")
    val alphaAnim by transition.animateFloat(
        initialValue = if (isActiveTransfer) 0.45f else 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bolt_alpha"
    )

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        drawRoundRect(
            color = NightBlueElevated,
            topLeft = Offset.Zero,
            size = Size(w, h),
            cornerRadius = CornerRadius(w * 0.22f, w * 0.22f)
        )

        drawRoundRect(
            color = ElectricBlueBright,
            topLeft = Offset(w * 0.16f, h * 0.28f),
            size = Size(w * 0.60f, h * 0.44f),
            cornerRadius = CornerRadius(w * 0.08f, w * 0.08f),
            style = Stroke(width = w * 0.06f)
        )

        drawRoundRect(
            color = ElectricBlueBright,
            topLeft = Offset(w * 0.78f, h * 0.40f),
            size = Size(w * 0.08f, h * 0.20f),
            cornerRadius = CornerRadius(w * 0.03f, w * 0.03f)
        )

        val boltColor = if (isActiveTransfer) ActiveGreen.copy(alpha = alphaAnim) else ActiveGreen
        val path = Path().apply {
            moveTo(w * 0.49f, h * 0.34f)
            lineTo(w * 0.36f, h * 0.51f)
            lineTo(w * 0.46f, h * 0.51f)
            lineTo(w * 0.43f, h * 0.66f)
            lineTo(w * 0.56f, h * 0.49f)
            lineTo(w * 0.46f, h * 0.49f)
            close()
        }
        drawPath(path = path, color = boltColor)
    }
}

@Composable
fun SectionCardHeader(
    title: String,
    subtitle: String? = null
) {
    androidx.compose.foundation.layout.Column {
        androidx.compose.material3.Text(
            text = title,
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
            color = Color.White
        )
        if (!subtitle.isNullOrBlank()) {
            androidx.compose.material3.Text(
                text = subtitle,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                color = com.batteryshare.ai.ui.theme.MutedSlate
            )
        }
    }
}
