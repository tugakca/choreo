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

@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.tugakca.choreo.sample

import android.content.ClipData
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.tugakca.choreo.ChoreoCanvas
import io.github.tugakca.choreo.ChoreoDefaults
import io.github.tugakca.choreo.PaintMode
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/** Everything the Playground lets the user change; lives above the destinations so it survives tab switches. */
@Stable
class PlaygroundState {
    var shape by mutableStateOf(DemoShape.Star)
    var animation by mutableStateOf(DemoAnimation.DrawOn)
    var durationMillis by mutableIntStateOf(1200)
    var delayMillis by mutableIntStateOf(0)
    var easingIndex by mutableIntStateOf(1)
    var repeatIndex by mutableIntStateOf(0)

    val iterations: Int get() = Repeats[repeatIndex]

    companion object {
        val Repeats = listOf(1, 3, ChoreoDefaults.INFINITE)

        val Saver = listSaver<PlaygroundState, Any>(
            save = {
                listOf(
                    it.shape.name,
                    it.animation.name,
                    it.durationMillis,
                    it.delayMillis,
                    it.easingIndex,
                    it.repeatIndex,
                )
            },
            restore = {
                PlaygroundState().apply {
                    shape = DemoShape.valueOf(it[0] as String)
                    animation = DemoAnimation.valueOf(it[1] as String)
                    durationMillis = it[2] as Int
                    delayMillis = it[3] as Int
                    easingIndex = it[4] as Int
                    repeatIndex = it[5] as Int
                }
            },
        )
    }
}

@Composable
fun PlaygroundScreen(
    state: PlaygroundState,
    pausedAll: Boolean,
    colors: DemoColors,
    snackbar: SnackbarHostState,
    contentPadding: PaddingValues,
) {
    val easing = DemoEasings[state.easingIndex]
    // Low windows (landscape) and large text get a smaller scene so the parameters stay reachable.
    val lowWindow = LocalConfiguration.current.screenHeightDp < 480
    val sceneHeight = when {
        lowWindow -> 100.dp
        isLargeText -> 130.dp
        else -> 170.dp
    }
    // The scene reserves a strip on top for its labels; the canvas gets the rest.
    val canvasHeight = minOf(120f, sceneHeight.value - 36f)
    val centerY = canvasHeight / 2f
    val shapeSize = minOf(30f, canvasHeight * 0.3f)
    val outline = state.animation == DemoAnimation.DrawOn || state.animation == DemoAnimation.EraseOff
    val choreography = rememberDemo(
        state.shape,
        state.animation,
        state.durationMillis,
        state.delayMillis,
        state.easingIndex,
        state.iterations,
        colors,
        canvasHeight,
        pausedAll = pausedAll,
    ) {
        val moving = state.animation == DemoAnimation.MoveTo
        val shape = addShape(
            state.shape,
            center = Offset(if (moving) 60f else 110f, centerY),
            size = shapeSize,
            color = colors.primary,
            mode = if (outline || state.shape.strokeOnly) PaintMode.Stroke else PaintMode.Fill,
        )
        applyAnimation(
            state.animation,
            shape,
            colors,
            state.durationMillis,
            state.delayMillis,
            easing.easing,
            state.iterations,
            moveTarget = Offset(160f, centerY),
        )
    }
    val code =
        remember(
            state.shape,
            state.animation,
            state.durationMillis,
            state.delayMillis,
            state.easingIndex,
            state.iterations,
            centerY,
        ) {
            playgroundCode(state, centerY)
        }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            // In a low window there is no room for a fixed block, so the whole screen scrolls.
            .then(if (lowWindow) Modifier.verticalScroll(rememberScrollState()) else Modifier)
            .padding(top = contentPadding.calculateTopPadding())
            .padding(horizontal = 16.dp),
    ) {
        // Fixed block: the scene and transport stay put while the parameters scroll.
        Scene(height = sceneHeight) {
            ChoreoCanvas(
                choreography,
                Modifier.size(220.dp, canvasHeight.dp).align(Alignment.BottomCenter),
                contentDescription = "${state.shape.label}, ${state.animation.code}",
            )
            Text(
                "${state.animation.code} · ${state.durationMillis} ms",
                style = CodeStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
            )
            if (state.iterations == ChoreoDefaults.INFINITE) {
                Text(
                    "Looping",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                )
            }
        }
        Transport(choreography)
        Column(
            Modifier
                .then(if (lowWindow) Modifier else Modifier.weight(1f).verticalScroll(rememberScrollState()))
                .padding(bottom = contentPadding.calculateBottomPadding() + 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Animation", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(DemoAnimation.entries) { animation ->
                    val selected = animation == state.animation
                    FilterChip(
                        selected = selected,
                        onClick = { state.animation = animation },
                        label = { Text(animation.code) },
                        leadingIcon = if (selected) {
                            { Icon(Icons.Filled.Check, contentDescription = null) }
                        } else {
                            null
                        },
                    )
                }
            }
            // At least 350 ms: an infinite loop then changes at most 3 times per second (no flashing).
            SliderRow("Duration", state.durationMillis, 350..3000) { state.durationMillis = it }
            SliderRow("Delay", state.delayMillis, 0..2000) { state.delayMillis = it }
            EasingDropdown(state.easingIndex) { state.easingIndex = it }
            Text("Repeat", style = MaterialTheme.typography.labelLarge)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                PlaygroundState.Repeats.forEachIndexed { index, repeats ->
                    SegmentedButton(
                        selected = state.repeatIndex == index,
                        onClick = { state.repeatIndex = index },
                        shape = SegmentedButtonDefaults.itemShape(index, PlaygroundState.Repeats.size),
                        label = { Text(repeatLabel(repeats)) },
                    )
                }
            }
            Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Box(Modifier.weight(1f)) { CodeBlock(code, Modifier.padding(0.dp)) }
                    IconButton(onClick = {
                        scope.launch {
                            clipboard.setClipEntry(
                                ClipEntry(ClipData.newPlainText("Choreo code", code.joinToString("\n"))),
                            )
                            snackbar.showSnackbar("Code copied")
                        }
                    }) {
                        Icon(SampleIcons.Copy, contentDescription = "Copy code")
                    }
                }
            }
        }
    }
}

@Composable
private fun SliderRow(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text("$value ms", style = CodeStyle, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(((it / 50).roundToInt() * 50).coerceIn(range)) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            modifier = Modifier.semantics { contentDescription = "$label in milliseconds" },
        )
    }
}

@Composable
private fun EasingDropdown(selected: Int, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded, { expanded = it }) {
        OutlinedTextField(
            value = DemoEasings[selected].label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Easing") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded, { expanded = false }) {
            DemoEasings.forEachIndexed { index, easing ->
                DropdownMenuItem(text = { Text(easing.label) }, onClick = {
                    onSelect(index)
                    expanded = false
                })
            }
        }
    }
}

private fun playgroundCode(state: PlaygroundState, centerY: Float): List<String> {
    val receiver = state.shape.name.replaceFirstChar { it.lowercase() }
    val args = buildList {
        if (state.animation == DemoAnimation.MoveTo) add("target = Offset(160f, ${centerY.toInt()}f)")
        if (state.animation == DemoAnimation.ScaleTo) add("scale = 1.6f")
        if (state.animation == DemoAnimation.RotateTo) add("degrees = 180f")
        if (state.animation == DemoAnimation.FadeTo) add("alpha = 0.2f")
        if (state.animation == DemoAnimation.ColorTo) add("color = tertiary")
        add("durationMillis = ${state.durationMillis}")
        if (state.delayMillis != 0) add("delayMillis = ${state.delayMillis}")
        add("easing = ${DemoEasings[state.easingIndex].code}")
        if (state.iterations != 1) {
            add(
                "iterations = ${if (state.iterations == ChoreoDefaults.INFINITE) "ChoreoDefaults.INFINITE" else state.iterations}",
            )
        }
    }
    return buildList {
        add("$receiver.${state.animation.code}(")
        args.forEach { add("    $it,") }
        add(")")
    }
}
