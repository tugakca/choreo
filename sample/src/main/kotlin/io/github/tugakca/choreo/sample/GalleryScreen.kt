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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import io.github.tugakca.choreo.ChoreoCanvas
import io.github.tugakca.choreo.ChoreoDefaults
import io.github.tugakca.choreo.PaintMode

private data class GalleryItem(val title: String, val code: String, val shape: DemoShape, val animation: DemoAnimation?)

@Composable
fun GalleryScreen(
    pausedAll: Boolean,
    colors: DemoColors,
    contentPadding: PaddingValues,
    onOpen: (DemoShape, DemoAnimation) -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val items = if (tab == 0) {
        DemoShape.entries.map { GalleryItem(it.label, it.code, it, null) }
    } else {
        DemoAnimation.entries.map { GalleryItem(it.code, "star.${it.code}(…)", animationShape(it), it) }
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(if (isLargeText) 1 else 2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 4.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    label = { Text("Shapes · ${DemoShape.entries.size}") },
                    leadingIcon = if (tab == 0) {
                        { Icon(Icons.Filled.Check, contentDescription = null) }
                    } else {
                        null
                    },
                )
                FilterChip(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    label = { Text("Animations · ${DemoAnimation.entries.size}") },
                    leadingIcon = if (tab == 1) {
                        { Icon(Icons.Filled.Check, contentDescription = null) }
                    } else {
                        null
                    },
                )
            }
        }
        items(items, key = { "$tab-${it.title}" }) { item ->
            GalleryCard(item, pausedAll, colors) { onOpen(item.shape, item.animation ?: DemoAnimation.DrawOn) }
        }
    }
}

private fun animationShape(animation: DemoAnimation) = when (animation) {
    DemoAnimation.DrawOn, DemoAnimation.EraseOff, DemoAnimation.RotateTo -> DemoShape.Star
    else -> DemoShape.Circle
}

@Composable
private fun GalleryCard(item: GalleryItem, pausedAll: Boolean, colors: DemoColors, onClick: () -> Unit) {
    val choreography = rememberDemo(item.shape, item.animation, colors, pausedAll = pausedAll) {
        val shape = addShape(
            item.shape,
            center = Offset(48f, 48f),
            size = 26f,
            color = colors.primary,
            mode = if (outlineOnly(item)) PaintMode.Stroke else PaintMode.Fill,
        )
        val animation = item.animation
        if (animation == null) {
            appear(item.shape, shape)
        } else {
            applyAnimation(animation, shape, colors, 900, 0, ChoreoDefaults.Easing, 1, Offset(72f, 48f))
        }
    }
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(Modifier.fillMaxWidth().height(PreviewSize), contentAlignment = Alignment.Center) {
            ChoreoCanvas(choreography, Modifier.size(PreviewSize), contentDescription = "${item.title} preview")
            IconButton(onClick = choreography::restart, modifier = Modifier.align(Alignment.BottomEnd)) {
                Icon(SampleIcons.Replay, contentDescription = "Replay ${item.title}")
            }
        }
        Column(
            Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(item.title, style = MaterialTheme.typography.labelLarge)
            MonoLine(item.code)
        }
    }
}

private fun outlineOnly(item: GalleryItem): Boolean =
    item.shape.strokeOnly || item.animation == DemoAnimation.DrawOn || item.animation == DemoAnimation.EraseOff
