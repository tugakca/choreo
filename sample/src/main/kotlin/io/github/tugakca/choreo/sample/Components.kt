/*
 * Copyright 2026 Choreo contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.tugakca.choreo.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.tugakca.choreo.ChoreoState
import io.github.tugakca.choreo.Choreography

val CodeStyle
    @Composable get() = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp)

/** True when the user enlarged text enough that the layout should give way (design spec: 1.5x). */
val isLargeText: Boolean
    @Composable get() = LocalDensity.current.fontScale >= 1.5f

/** A rounded scene with a dotted grid; [content] is centered in it. */
@Composable
fun Scene(height: Dp, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val dot = MaterialTheme.colorScheme.outlineVariant
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest, RoundedCornerShape(16.dp))
            .drawBehind {
                val step = 16.dp.toPx()
                var x = step / 2
                while (x < size.width) {
                    var y = step / 2
                    while (y < size.height) {
                        drawCircle(dot, radius = 1.dp.toPx(), center = Offset(x, y))
                        y += step
                    }
                    x += step
                }
            },
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** Restart, play/pause, seek slider and the time label. */
@Composable
fun Transport(choreography: Choreography, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = choreography::restart) {
            Icon(SampleIcons.Replay, contentDescription = "Restart")
        }
        PlayPauseButton(choreography)
        Slider(
            value = choreography.progress,
            onValueChange = choreography::seek,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
                .semantics { contentDescription = "Seek" },
        )
        Text(timeLabel(choreography), style = CodeStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun PlayPauseButton(choreography: Choreography) {
    val playing = choreography.state == ChoreoState.Playing
    FilledIconButton(onClick = { if (playing) choreography.pause() else choreography.play() }) {
        Icon(
            if (playing) SampleIcons.Pause else SampleIcons.Play,
            contentDescription = if (playing) "Pause" else "Play",
        )
    }
}

private fun timeLabel(choreography: Choreography): String {
    val position = choreography.positionMillis / 1000f
    val total = choreography.durationMillis / 1000f
    return "%.2f / %.1f s".format(position.coerceAtMost(total), total)
}

@Composable
fun CodeBlock(lines: List<String>, modifier: Modifier = Modifier, highlighted: (Int) -> Boolean = { false }) {
    Surface(
        modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        androidx.compose.foundation.layout.Column(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 12.dp),
        ) {
            val highlight = MaterialTheme.colorScheme.primaryContainer
            lines.forEachIndexed { index, line ->
                Text(
                    line,
                    style = CodeStyle,
                    softWrap = false,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .background(if (highlighted(index)) highlight else Color.Transparent)
                        .padding(horizontal = 12.dp, vertical = 1.dp),
                )
            }
        }
    }
}

@Composable
fun IconAction(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier) { Icon(icon, contentDescription = description) }
}

@Composable
fun MonoLine(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier,
        style = CodeStyle,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Clip,
    )
}

val SectionSpacing = Arrangement.spacedBy(12.dp)
val PreviewSize = 96.dp
