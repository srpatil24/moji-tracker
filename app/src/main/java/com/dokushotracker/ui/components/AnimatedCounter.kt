package com.dokushotracker.ui.components

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import com.dokushotracker.util.NumberFormatUtils
import kotlin.math.min

@Composable
fun AnimatedCounter(
    targetValue: Long,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
) {
    val safeTarget = min(targetValue, Int.MAX_VALUE.toLong()).toInt()
    val animated by animateIntAsState(
        targetValue = safeTarget,
        animationSpec = tween(durationMillis = 1_000),
        label = "animated_counter",
    )
    Text(
        modifier = modifier,
        text = NumberFormatUtils.formatInt(animated),
        style = style,
        color = color,
        textAlign = textAlign,
    )
}
