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

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.isSpecified

/** Defaults shared by every animation function. */
public object ChoreoDefaults {
    /** Duration used when none is given. */
    public const val DURATION_MILLIS: Int = 300

    /** Easing used when none is given. */
    public val Easing: Easing = FastOutSlowInEasing

    /** Pass as `iterations` to repeat forever. */
    public const val INFINITE: Int = INFINITE_ITERATIONS
}

@DslMarker
internal annotation class ChoreoDsl

/**
 * Builds the steps of a choreography. Steps run one after another inside `sequence {}` (and at the top
 * level of [rememberChoreography]) and all at once inside `parallel {}`; both nest freely.
 *
 * Every animation takes a duration (0 means "instant", negative throws [IllegalArgumentException]), an
 * easing, a start delay, and a number of iterations ([ChoreoDefaults.INFINITE] repeats forever). An
 * animation with several iterations restarts from its start value each time. For layout purposes an
 * infinitely repeating animation occupies one iteration.
 *
 * **Interruption rule:** when an animation begins on a property that another animation is still
 * changing, the new one starts from the property's current value and takes over; the value never jumps.
 */
@ChoreoDsl
public abstract class StepScope internal constructor(internal val group: GroupNode, private val root: GroupNode) {
    /** Runs the steps in [block] one after another. */
    public fun sequence(block: StepScope.() -> Unit) {
        group.children += GroupNode(parallel = false).also { GroupScope(it, root).block() }
    }

    /** Runs the steps in [block] at the same time; it lasts as long as its longest step. */
    public fun parallel(block: StepScope.() -> Unit) {
        group.children += GroupNode(parallel = true).also { GroupScope(it, root).block() }
    }

    /** Waits for [durationMillis] before the next step of a sequence. */
    public fun wait(durationMillis: Int) {
        require(durationMillis >= 0) { "wait: durationMillis must be >= 0 but was $durationMillis" }
        group.children += WaitNode(durationMillis)
    }

    /** Moves the center of this shape to [target] (in the choreography unit). */
    public fun ShapeHandle.moveTo(
        target: Offset,
        durationMillis: Int = ChoreoDefaults.DURATION_MILLIS,
        easing: Easing = ChoreoDefaults.Easing,
        delayMillis: Int = 0,
        iterations: Int = 1,
    ) {
        requireFinite("moveTo", "target.x", target.x)
        requireFinite("moveTo", "target.y", target.y)
        val both = GroupNode(parallel = true)
        both.children += spec("moveTo", Channel.X, target.x, null, durationMillis, delayMillis, iterations, easing)
        both.children += spec("moveTo", Channel.Y, target.y, null, durationMillis, delayMillis, iterations, easing)
        group.children += both
    }

    /** Scales this shape around its center to [scale] (1 is the original size). */
    public fun ShapeHandle.scaleTo(
        scale: Float,
        durationMillis: Int = ChoreoDefaults.DURATION_MILLIS,
        easing: Easing = ChoreoDefaults.Easing,
        delayMillis: Int = 0,
        iterations: Int = 1,
    ) {
        requireFinite("scaleTo", "scale", scale)
        group.children += spec("scaleTo", Channel.Scale, scale, null, durationMillis, delayMillis, iterations, easing)
    }

    /** Rotates this shape around its center to [degrees] (clockwise, 0 is the original orientation). */
    public fun ShapeHandle.rotateTo(
        degrees: Float,
        durationMillis: Int = ChoreoDefaults.DURATION_MILLIS,
        easing: Easing = ChoreoDefaults.Easing,
        delayMillis: Int = 0,
        iterations: Int = 1,
    ) {
        requireFinite("rotateTo", "degrees", degrees)
        group.children +=
            spec("rotateTo", Channel.Rotation, degrees, null, durationMillis, delayMillis, iterations, easing)
    }

    /** Changes the opacity of this shape to [alpha] (`0f..1f`). */
    public fun ShapeHandle.fadeTo(
        alpha: Float,
        durationMillis: Int = ChoreoDefaults.DURATION_MILLIS,
        easing: Easing = ChoreoDefaults.Easing,
        delayMillis: Int = 0,
        iterations: Int = 1,
    ) {
        require(alpha in 0f..1f) { "fadeTo: alpha must be within 0..1 but was $alpha" }
        group.children += spec("fadeTo", Channel.Alpha, alpha, null, durationMillis, delayMillis, iterations, easing)
    }

    /** Changes the color of this shape to [color]. */
    public fun ShapeHandle.colorTo(
        color: Color,
        durationMillis: Int = ChoreoDefaults.DURATION_MILLIS,
        easing: Easing = ChoreoDefaults.Easing,
        delayMillis: Int = 0,
        iterations: Int = 1,
    ) {
        require(color.isSpecified) { "colorTo: color must be specified" }
        group.children += AnimNode(
            AnimSpec(
                "colorTo", ownedIndex("colorTo"), Channel.Color, 0f, color, null,
                validated(
                    "colorTo",
                    durationMillis,
                    delayMillis,
                    iterations,
                ),
                delayMillis, iterations, easing,
            ),
        )
    }

    /**
     * Draws the outline of this shape from nothing to complete. Before it starts the shape is invisible.
     * For paths with several sub-paths only the first one is traced progressively.
     */
    public fun ShapeHandle.drawOn(
        durationMillis: Int = ChoreoDefaults.DURATION_MILLIS,
        easing: Easing = ChoreoDefaults.Easing,
        delayMillis: Int = 0,
        iterations: Int = 1,
    ) {
        val both = GroupNode(parallel = true)
        both.children += spec("drawOn", Channel.TrimEnd, 1f, 0f, durationMillis, delayMillis, iterations, easing)
        both.children += spec("drawOn", Channel.TrimStart, 0f, 0f, durationMillis, delayMillis, iterations, easing)
        group.children += both
    }

    /**
     * Erases a completely drawn shape along its outline, from the start to the end. After it ends the
     * shape is invisible.
     */
    public fun ShapeHandle.eraseOff(
        durationMillis: Int = ChoreoDefaults.DURATION_MILLIS,
        easing: Easing = ChoreoDefaults.Easing,
        delayMillis: Int = 0,
        iterations: Int = 1,
    ) {
        val both = GroupNode(parallel = true)
        both.children += spec("eraseOff", Channel.TrimStart, 1f, 0f, durationMillis, delayMillis, iterations, easing)
        both.children += spec("eraseOff", Channel.TrimEnd, 1f, 1f, durationMillis, delayMillis, iterations, easing)
        group.children += both
    }

    /**
     * Animates the visible part of the outline to the range [start]..[end], given as fractions of the
     * outline length (`0f <= start <= end <= 1f`).
     */
    @ExperimentalChoreoApi
    public fun ShapeHandle.drawRange(
        start: Float,
        end: Float,
        durationMillis: Int = ChoreoDefaults.DURATION_MILLIS,
        easing: Easing = ChoreoDefaults.Easing,
        delayMillis: Int = 0,
        iterations: Int = 1,
    ) {
        require(start in 0f..1f && end in 0f..1f && start <= end) {
            "drawRange: expected 0 <= start <= end <= 1 but was start=$start, end=$end"
        }
        val both = GroupNode(parallel = true)
        both.children +=
            spec("drawRange", Channel.TrimStart, start, null, durationMillis, delayMillis, iterations, easing)
        both.children += spec("drawRange", Channel.TrimEnd, end, null, durationMillis, delayMillis, iterations, easing)
        group.children += both
    }

    private fun ShapeHandle.ownedIndex(name: String): Int {
        require(owner === root) { "$name: this ShapeHandle was created by a different choreography" }
        return index
    }

    private fun ShapeHandle.spec(
        name: String,
        channel: Channel,
        to: Float,
        from: Float?,
        durationMillis: Int,
        delayMillis: Int,
        iterations: Int,
        easing: Easing,
    ): AnimNode = AnimNode(
        AnimSpec(
            name, ownedIndex(name), channel, to, Color.Unspecified, from,
            validated(
                name,
                durationMillis,
                delayMillis,
                iterations,
            ),
            delayMillis, iterations, easing,
        ),
    )

    private fun validated(name: String, durationMillis: Int, delayMillis: Int, iterations: Int): Int {
        require(durationMillis >= 0) {
            "$name: durationMillis must be >= 0 (use 0 for an instant change) but was $durationMillis"
        }
        require(delayMillis >= 0) { "$name: delayMillis must be >= 0 but was $delayMillis" }
        require(iterations >= 1) { "$name: iterations must be >= 1 (or ChoreoDefaults.INFINITE) but was $iterations" }
        return durationMillis
    }

    private fun requireFinite(name: String, parameter: String, value: Float) {
        require(value.isFinite()) { "$name: $parameter must be finite but was $value" }
    }
}

internal class GroupScope(group: GroupNode, root: GroupNode) : StepScope(group, root)

/**
 * Receiver of the [rememberChoreography] block. Declare shapes first, then describe the animations;
 * the body itself is a `sequence {}`. Shapes are drawn in declaration order (later ones on top).
 *
 * Positions are given in the choreography unit (dp by default) with the origin at the top-left of the
 * [ChoreoCanvas]. A shape's position is its center, which is also the pivot of scaling and rotation.
 */
public class ChoreographyScope internal constructor(private val root: GroupNode) : StepScope(root, root) {
    internal val shapes = mutableListOf<ShapeSpec>()

    /** Adds a circle of [radius] centered at [center]. */
    public fun circle(radius: Float, center: Offset = Offset.Zero, style: ShapeStyle = ShapeStyle()): ShapeHandle =
        add(circlePath(radius), center, style)

    /** Adds a rectangle of [size] centered at [center]. */
    public fun rectangle(size: Size, center: Offset = Offset.Zero, style: ShapeStyle = ShapeStyle()): ShapeHandle =
        add(rectanglePath(size), center, style)

    /** Adds a rectangle of [size] with rounded corners, centered at [center]. */
    public fun roundedRectangle(
        size: Size,
        cornerRadius: Float,
        center: Offset = Offset.Zero,
        style: ShapeStyle = ShapeStyle(),
    ): ShapeHandle = add(roundedRectanglePath(size, cornerRadius), center, style)

    /** Adds a line from [start] to [end]; its position is the midpoint. Lines are stroked by default. */
    public fun line(start: Offset, end: Offset, style: ShapeStyle = ShapeStyle(mode = PaintMode.Stroke)): ShapeHandle {
        val path = linePath(start, end)
        return add(path, Offset((start.x + end.x) / 2f, (start.y + end.y) / 2f), style)
    }

    /** Adds a regular polygon with [sides] corners on a circle of [radius], centered at [center]. */
    public fun polygon(
        sides: Int,
        radius: Float,
        center: Offset = Offset.Zero,
        style: ShapeStyle = ShapeStyle(),
    ): ShapeHandle = add(polygonPath(sides, radius), center, style)

    /** Adds a star with [points] tips between [innerRadius] and [outerRadius], centered at [center]. */
    public fun star(
        points: Int,
        outerRadius: Float,
        innerRadius: Float = outerRadius * DEFAULT_STAR_INNER_RATIO,
        center: Offset = Offset.Zero,
        style: ShapeStyle = ShapeStyle(),
    ): ShapeHandle = add(starPath(points, outerRadius, innerRadius), center, style)

    /**
     * Adds a Compose [path]. The path is copied; its coordinates are in the choreography unit. The
     * shape starts where the path is, unless [center] moves the path's bounds center there.
     */
    public fun path(path: Path, center: Offset? = null, style: ShapeStyle = ShapeStyle()): ShapeHandle {
        val (centered, original) = recentered(path)
        return add(centered, center ?: original, style)
    }

    /**
     * Adds a shape from SVG path data (the `d` attribute, for example `"M0 0 L10 10"`). Coordinates are
     * in the choreography unit. Throws [IllegalArgumentException] for malformed data.
     */
    public fun svgPath(pathData: String, center: Offset? = null, style: ShapeStyle = ShapeStyle()): ShapeHandle =
        path(parseSvgPath(pathData), center, style)

    private fun add(path: Path, center: Offset, style: ShapeStyle): ShapeHandle {
        require(center.x.isFinite() && center.y.isFinite()) { "shape center must be finite but was $center" }
        shapes += ShapeSpec(path, style, center.x, center.y)
        return ShapeHandle(shapes.size - 1, root)
    }

    private companion object {
        const val DEFAULT_STAR_INNER_RATIO = 0.5f
    }
}
