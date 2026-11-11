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

package io.github.tugakca.choreo

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics

/**
 * Draws the shapes of [choreography]. Frames are drawn without recomposition: only the draw phase
 * observes the clock.
 *
 * The canvas has no intrinsic size; give it one through [modifier]. The origin of the choreography
 * coordinates is its top-left corner.
 *
 * @param contentDescription Describes the drawing for accessibility services. When `null` the canvas is
 * treated as decorative and is invisible to them.
 */
@Composable
public fun ChoreoCanvas(
    choreography: Choreography,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val renderer = remember(choreography) { ChoreoRenderer(choreography) }
    val density = LocalDensity.current.density
    val semantics = if (contentDescription != null) {
        Modifier.semantics {
            this.contentDescription = contentDescription
            role = Role.Image
        }
    } else {
        Modifier
    }
    Canvas(modifier.then(semantics)) {
        renderer.draw(this, if (choreography.unit == ChoreoUnit.Dp) density else 1f)
    }
}

private class ChoreoRenderer(private val choreography: Choreography) {
    private val timeline = choreography.timeline
    private val frame = Frame(choreography.shapes.size)
    private val measures = arrayOfNulls<PathMeasure>(choreography.shapes.size)
    private val segment = Path()

    fun draw(scope: DrawScope, unitScale: Float) {
        // Both reads below happen in the draw phase, so frames never trigger recomposition.
        if (choreography.state == ChoreoState.Finished) {
            timeline.evaluateFinal(frame)
        } else {
            timeline.evaluate(choreography.frameTimeMillis(), frame)
        }
        choreography.shapes.forEachIndexed { index, shape ->
            val scale = frame.float(index, Channel.Scale) * unitScale
            val alpha = frame.float(index, Channel.Alpha)
            if (scale == 0f || alpha <= 0f) return@forEachIndexed
            val path =
                visiblePath(
                    index,
                    shape.path,
                    frame.float(index, Channel.TrimStart),
                    frame.float(index, Channel.TrimEnd),
                )
                    ?: return@forEachIndexed
            scope.withTransform({
                translate(frame.float(index, Channel.X) * unitScale, frame.float(index, Channel.Y) * unitScale)
                rotate(frame.float(index, Channel.Rotation), Offset.Zero)
                scale(scale, scale, Offset.Zero)
            }) {
                paint(path, shape.style, frame.color(index), alpha)
            }
        }
    }

    private fun DrawScope.paint(path: Path, style: ShapeStyle, color: Color, alpha: Float) {
        if (style.mode != PaintMode.Stroke) drawPath(path, color, alpha, Fill)
        if (style.mode != PaintMode.Fill) {
            val strokeColor = if (style.strokeColor == Color.Unspecified) color else style.strokeColor
            drawPath(path, strokeColor, alpha, Stroke(style.strokeWidth, cap = style.cap, join = style.join))
        }
    }

    /** The part of [path] between the trim fractions, or null when nothing is visible. */
    private fun visiblePath(index: Int, path: Path, start: Float, end: Float): Path? {
        if (start <= 0f && end >= 1f) return path
        if (end <= start) return null
        val measure = measures[index] ?: PathMeasure().also {
            it.setPath(path, false)
            measures[index] = it
        }
        val length = measure.length
        segment.rewind()
        measure.getSegment(start * length, end * length, segment, true)
        return segment
    }
}
