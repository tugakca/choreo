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

@file:OptIn(ExperimentalChoreoApi::class)

package io.github.tugakca.choreo.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.tugakca.choreo.ChoreoCanvas
import io.github.tugakca.choreo.ChoreoStep
import io.github.tugakca.choreo.Choreography
import io.github.tugakca.choreo.ChoreographyScope
import io.github.tugakca.choreo.ExperimentalChoreoApi
import io.github.tugakca.choreo.PaintMode
import io.github.tugakca.choreo.ShapeStyle

/** One line of displayed code and the timeline row (if any) it belongs to. */
private class CodeLine(val text: String, val step: Int? = null)

private class SequenceDemo(
    val title: String,
    val description: String,
    val code: List<CodeLine>,
    val build: ChoreographyScope.(DemoColors) -> Unit,
)

private val SequenceDemos = listOf(
    SequenceDemo(
        "Sequence",
        "One step after another",
        listOf(
            CodeLine("rememberChoreography {"),
            CodeLine("    sequence {"),
            CodeLine("        circle.moveTo(Offset(220f, 85f), 800)", 0),
            CodeLine("        square.scaleTo(1.4f, 600)", 1),
            CodeLine("        star.rotateTo(180f, 700)", 2),
            CodeLine("    }"),
            CodeLine("}"),
        ),
    ) { colors ->
        basicShapes(colors) { circle, square, star ->
            sequence {
                circle.moveTo(Offset(220f, 85f), 800)
                square.scaleTo(1.4f, 600)
                star.rotateTo(180f, 700)
            }
        }
    },
    SequenceDemo(
        "Parallel",
        "All steps at once",
        listOf(
            CodeLine("rememberChoreography {"),
            CodeLine("    parallel {"),
            CodeLine("        circle.moveTo(Offset(220f, 85f), 800)", 0),
            CodeLine("        square.scaleTo(1.4f, 600)", 1),
            CodeLine("        star.rotateTo(180f, 700)", 2),
            CodeLine("    }"),
            CodeLine("}"),
        ),
    ) { colors ->
        basicShapes(colors) { circle, square, star ->
            parallel {
                circle.moveTo(Offset(220f, 85f), 800)
                square.scaleTo(1.4f, 600)
                star.rotateTo(180f, 700)
            }
        }
    },
    SequenceDemo(
        "Nested",
        "A parallel group inside a sequence",
        listOf(
            CodeLine("rememberChoreography {"),
            CodeLine("    sequence {"),
            CodeLine("        circle.moveTo(Offset(220f, 85f), 800)", 0),
            CodeLine("        parallel {"),
            CodeLine("            square.scaleTo(1.4f, 600)", 1),
            CodeLine("            star.rotateTo(180f, 700)", 2),
            CodeLine("        }"),
            CodeLine("    }"),
            CodeLine("}"),
        ),
    ) { colors ->
        basicShapes(colors) { circle, square, star ->
            sequence {
                circle.moveTo(Offset(220f, 85f), 800)
                parallel {
                    square.scaleTo(1.4f, 600)
                    star.rotateTo(180f, 700)
                }
            }
        }
    },
    SequenceDemo(
        "Logo",
        "Draws itself once, no loop",
        listOf(
            CodeLine("rememberChoreography {"),
            CodeLine("    sequence {"),
            CodeLine("        arc.drawOn(1000)", 0),
            CodeLine("        dot.fadeTo(1f, 300)", 1),
            CodeLine("        parallel { … }", 2),
            CodeLine("    }"),
            CodeLine("}"),
        ),
    ) { colors ->
        val arc = svgPath(
            "M182.1 49.6 A55 55 0 1 0 182.1 120.4",
            style = ShapeStyle(colors.primary, PaintMode.Stroke, strokeWidth = 14f),
        )
        val dot = circle(8f, Offset(190f, 56f), ShapeStyle(colors.tertiary, alpha = 0f))
        val star = star(5, 12f, 6f, Offset(120f, 150f), ShapeStyle(colors.tertiary, PaintMode.Stroke, 2f))
        val square = rectangle(Size(14f, 14f), Offset(150f, 150f), ShapeStyle(colors.primary, PaintMode.Stroke, 2f))
        val ring = circle(7f, Offset(178f, 150f), ShapeStyle(colors.primary, PaintMode.Stroke, 2f))
        sequence {
            arc.drawOn(1000)
            dot.fadeTo(1f, 300)
            parallel {
                for (shape in listOf(star, square, ring)) shape.drawOn(500)
            }
        }
    },
)

private fun ChoreographyScope.basicShapes(
    colors: DemoColors,
    steps: ChoreographyScope.(
        circle: io.github.tugakca.choreo.ShapeHandle,
        square: io.github.tugakca.choreo.ShapeHandle,
        star: io.github.tugakca.choreo.ShapeHandle,
    ) -> Unit,
) {
    val circle = circle(16f, Offset(60f, 85f), ShapeStyle(colors.primary))
    val square = roundedRectangle(Size(36f, 36f), 6f, Offset(140f, 85f), ShapeStyle(colors.tertiary, alpha = 0.9f))
    val star = star(5, 22f, 10f, Offset(220f, 40f), ShapeStyle(Color.Gray))
    steps(circle, square, star)
}

@Composable
fun SequencesScreen(pausedAll: Boolean, colors: DemoColors, contentPadding: PaddingValues) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val demo = SequenceDemos[tab]
    val choreography = rememberDemo(tab, colors, pausedAll = pausedAll) { demo.build(this, colors) }

    Column(Modifier.fillMaxSize().padding(top = contentPadding.calculateTopPadding())) {
        PrimaryTabRow(selectedTabIndex = tab) {
            SequenceDemos.forEachIndexed { index, item ->
                Tab(selected = tab == index, onClick = { tab = index }, text = { Text(item.title) })
            }
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = contentPadding.calculateBottomPadding() + 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                demo.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
            Scene(210.dp) {
                ChoreoCanvas(choreography, Modifier.size(280.dp, 170.dp), contentDescription = "${demo.title} demo")
            }
            Transport(choreography)
            TimelineCard(choreography)
            val position = choreography.positionMillis
            val steps = choreography.steps
            CodeBlock(demo.code.map { it.text }, highlighted = { index ->
                val step = demo.code[index].step?.let(steps::getOrNull)
                step != null && position >= step.startMillis && position < step.endMillis
            })
        }
    }
}

@Composable
private fun TimelineCard(choreography: Choreography) {
    val steps = choreography.steps
    val total = choreography.durationMillis.coerceAtLeast(1)
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Timeline", style = MaterialTheme.typography.labelLarge)
            Box(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    steps.forEach { TimelineRow(it, total) }
                }
                // The playhead follows the choreography without recomposing the rows.
                PlayHead(choreography, steps.size)
            }
        }
    }
}

@Composable
private fun TimelineRow(step: ChoreoStep, total: Long) {
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth().height(28.dp)) {
        val labelWidth = 72.dp
        val trackWidth = maxWidth - labelWidth
        Text(
            step.name,
            style = CodeStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterStart),
        )
        Box(
            Modifier
                .offset(x = labelWidth + trackWidth * (step.startMillis.toFloat() / total))
                .width(trackWidth * ((step.endMillis - step.startMillis).toFloat() / total))
                .height(16.dp)
                .align(Alignment.CenterStart)
                .background(MaterialTheme.colorScheme.tertiary, RoundedCornerShape(4.dp)),
        )
    }
}

@Composable
private fun PlayHead(choreography: Choreography, rows: Int) {
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth().height((rows * 32).dp)) {
        val labelWidth: Dp = 72.dp
        val x = labelWidth + (maxWidth - labelWidth) * choreography.progress
        Box(Modifier.offset(x = x).width(2.dp).fillMaxSize().background(MaterialTheme.colorScheme.primary))
    }
}
