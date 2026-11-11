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

import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import io.github.tugakca.choreo.ChoreoDefaults
import io.github.tugakca.choreo.Choreography
import io.github.tugakca.choreo.ChoreographyScope
import io.github.tugakca.choreo.PaintMode
import io.github.tugakca.choreo.ShapeHandle
import io.github.tugakca.choreo.ShapeStyle
import io.github.tugakca.choreo.StepScope
import io.github.tugakca.choreo.rememberChoreography

/** The two accent colors every demo is drawn with. */
data class DemoColors(val primary: Color, val tertiary: Color)

enum class DemoShape(val label: String, val code: String, val strokeOnly: Boolean = false) {
    Circle("Circle", "circle(radius = 24f)"),
    Rectangle("Rectangle", "rectangle(Size(48f, 36f))"),
    RoundedRect("Rounded rect", "roundedRectangle(radius = 12f)"),
    Line("Line", "line(start, end)", strokeOnly = true),
    Polygon("Polygon", "polygon(sides = 6)"),
    Star("Star", "star(points = 5)"),
    Path("Path", "path(composePath)", strokeOnly = true),
    Svg("SVG path-data", "svgPath(\"M0 40 L20 0 L…\")", strokeOnly = true),
}

enum class DemoAnimation(val code: String) {
    DrawOn("drawOn"),
    EraseOff("eraseOff"),
    MoveTo("moveTo"),
    ScaleTo("scaleTo"),
    RotateTo("rotateTo"),
    FadeTo("fadeTo"),
    ColorTo("colorTo"),
}

class DemoEasing(val label: String, val code: String, val easing: Easing)

val DemoEasings = listOf(
    DemoEasing("Linear", "LinearEasing", LinearEasing),
    DemoEasing("FastOutSlowIn", "FastOutSlowInEasing", FastOutSlowInEasing),
    DemoEasing("EaseIn", "EaseIn", EaseIn),
    DemoEasing("EaseOut", "EaseOut", EaseOut),
    DemoEasing("EaseInOut", "EaseInOut", EaseInOut),
)

/** Adds [shape] centered at [center]; [size] is roughly its radius. */
fun ChoreographyScope.addShape(
    shape: DemoShape,
    center: Offset,
    size: Float,
    color: Color,
    mode: PaintMode = if (shape.strokeOnly) PaintMode.Stroke else PaintMode.Fill,
): ShapeHandle {
    val style = ShapeStyle(color = color, mode = mode, strokeWidth = 3f)
    return when (shape) {
        DemoShape.Circle -> circle(size, center, style)
        DemoShape.Rectangle -> rectangle(Size(size * 1.6f, size * 1.3f), center, style)
        DemoShape.RoundedRect -> roundedRectangle(Size(size * 1.6f, size * 1.3f), size * 0.4f, center, style)
        DemoShape.Line -> line(center + Offset(-size, size), center + Offset(size, -size), style)
        DemoShape.Polygon -> polygon(6, size, center, style)
        DemoShape.Star -> star(5, size * 1.1f, size * 0.5f, center, style)
        DemoShape.Path -> path(wavePath(size), center, style)
        DemoShape.Svg -> svgPath("M0 40 L20 0 L40 30 L60 0 L80 40", center, style)
    }
}

private fun wavePath(size: Float): Path = Path().apply {
    fillType = PathFillType.NonZero
    moveTo(0f, 0f)
    cubicTo(size * 0.6f, -size, size * 0.6f, -size, size, 0f)
    cubicTo(size * 1.4f, size, size * 1.4f, size, size * 2f, 0f)
}

/** Plays a one-shot appearance suited to [shape] inside [this] scope. */
fun StepScope.appear(shape: DemoShape, handle: ShapeHandle, durationMillis: Int = 900) {
    with(handle) {
        if (shape.strokeOnly) {
            drawOn(durationMillis)
        } else {
            sequence {
                scaleTo(0f, durationMillis = 0)
                scaleTo(1f, durationMillis = durationMillis)
            }
        }
    }
}

/** Applies [animation] with the given timing to [handle]. */
fun StepScope.applyAnimation(
    animation: DemoAnimation,
    handle: ShapeHandle,
    colors: DemoColors,
    durationMillis: Int,
    delayMillis: Int,
    easing: Easing,
    iterations: Int,
    moveTarget: Offset,
) {
    with(handle) {
        when (animation) {
            DemoAnimation.DrawOn -> drawOn(durationMillis, easing, delayMillis, iterations)
            DemoAnimation.EraseOff -> eraseOff(durationMillis, easing, delayMillis, iterations)
            DemoAnimation.MoveTo -> moveTo(moveTarget, durationMillis, easing, delayMillis, iterations)
            DemoAnimation.ScaleTo -> scaleTo(1.6f, durationMillis, easing, delayMillis, iterations)
            DemoAnimation.RotateTo -> rotateTo(180f, durationMillis, easing, delayMillis, iterations)
            DemoAnimation.FadeTo -> fadeTo(0.2f, durationMillis, easing, delayMillis, iterations)
            DemoAnimation.ColorTo -> colorTo(colors.tertiary, durationMillis, easing, delayMillis, iterations)
        }
    }
}

/** Remembers a choreography that follows the global "pause all" switch. */
@Composable
fun rememberDemo(vararg keys: Any?, pausedAll: Boolean, block: ChoreographyScope.() -> Unit): Choreography {
    val choreography = rememberChoreography(*keys, autoPlay = !pausedAll, block = block)
    LaunchedEffect(choreography, pausedAll) {
        if (pausedAll) choreography.pause() else choreography.resume()
    }
    return choreography
}

fun repeatLabel(iterations: Int): String = if (iterations == ChoreoDefaults.INFINITE) "Infinite" else "${iterations}x"
