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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/** Animatable properties of a shape. The order matches the layout of [Frame.floats]. */
internal enum class Channel { X, Y, Scale, Rotation, Alpha, TrimStart, TrimEnd, Color }

internal const val FLOAT_CHANNELS = 7
internal const val INFINITE_ITERATIONS = Int.MAX_VALUE

/** Values a shape has before any animation touches it. */
internal class ShapeInitial(val x: Float, val y: Float, val alpha: Float, val color: Color) {
    fun floatValue(channel: Channel): Float = when (channel) {
        Channel.X -> x
        Channel.Y -> y
        Channel.Scale -> 1f
        Channel.Rotation -> 0f
        Channel.Alpha -> alpha
        Channel.TrimStart -> 0f
        Channel.TrimEnd -> 1f
        Channel.Color -> error("Color is not a float channel")
    }
}

/** One animation as written in the DSL, before it is placed on the timeline. */
internal class AnimSpec(
    val name: String,
    val shape: Int,
    val channel: Channel,
    val to: Float,
    val toColor: Color,
    /** Explicit start value; null means "continue from the current value". */
    val from: Float?,
    val durationMillis: Int,
    val delayMillis: Int,
    val iterations: Int,
    val easing: Easing,
)

internal sealed interface Node

internal class AnimNode(val spec: AnimSpec) : Node

internal class WaitNode(val millis: Int) : Node

internal class GroupNode(val parallel: Boolean) : Node {
    val children: MutableList<Node> = mutableListOf()
}

/** An animation with an absolute position on the timeline. */
internal class PlacedAnim(val spec: AnimSpec, val startMillis: Long, val order: Int) {
    /** The moment the value starts changing (after the delay). */
    val activeStart: Long get() = startMillis + spec.delayMillis

    /** End of the first iteration; the end for infinite animations. */
    val endMillis: Long
        get() = activeStart +
            spec.durationMillis.toLong() * if (spec.iterations == INFINITE_ITERATIONS) 1 else spec.iterations
}

private class Segment<T>(
    val activeStart: Long,
    val durationMillis: Long,
    val iterations: Int,
    val easing: Easing,
    val from: T,
    val to: T,
    private val interpolate: (T, T, Float) -> T,
) {
    fun valueAt(timeMillis: Double): T {
        if (timeMillis == Double.POSITIVE_INFINITY || durationMillis == 0L) return to
        val local = timeMillis - activeStart
        if (iterations != INFINITE_ITERATIONS && local >= durationMillis.toDouble() * iterations) return to
        val fraction = ((local % durationMillis) / durationMillis).toFloat()
        return interpolate(from, to, easing.transform(fraction))
    }
}

/**
 * All segments that animate one property, ordered by start time. The segment that started last wins,
 * and a new segment without an explicit start value begins at whatever value the property has at that
 * moment, so interrupting an animation never makes the value jump.
 */
private class Track<T>(private val initial: T, private val interpolate: (T, T, Float) -> T) {
    private val segments = mutableListOf<Segment<T>>()
    private var backfill: T = initial

    fun add(placed: PlacedAnim, to: T, explicitFrom: T?) {
        val start = placed.activeStart
        val from = explicitFrom ?: valueAt(start.toDouble())
        if (segments.isEmpty()) backfill = from
        segments += Segment(
            start,
            placed.spec.durationMillis.toLong(),
            placed.spec.iterations,
            placed.spec.easing,
            from,
            to,
            interpolate,
        )
    }

    fun valueAt(timeMillis: Double): T {
        val segment = segments.lastOrNull { it.activeStart <= timeMillis } ?: return backfill
        return segment.valueAt(timeMillis)
    }
}

/** Reusable output of [Timeline.evaluate]; avoids allocating on every frame. */
internal class Frame(val shapeCount: Int) {
    val floats = FloatArray(shapeCount * FLOAT_CHANNELS)
    val colors = LongArray(shapeCount)

    fun float(shape: Int, channel: Channel): Float = floats[shape * FLOAT_CHANNELS + channel.ordinal]

    fun color(shape: Int): Color = Color(colors[shape].toULong())
}

/**
 * The single, deterministic description of a choreography. Everything that is drawn is a pure function
 * of the time passed to [evaluate].
 */
internal class Timeline(shapes: List<ShapeInitial>, root: List<Node>) {
    private val floatTracks: List<List<Track<Float>>> = shapes.map { shape ->
        (0 until FLOAT_CHANNELS).map { index ->
            Track(shape.floatValue(Channel.entries[index]), ::lerpFloat)
        }
    }
    private val colorTracks: List<Track<Color>> = shapes.map { Track(it.color, ::lerp) }

    val placed: List<PlacedAnim>

    /** Length of one pass; infinite animations count as a single iteration. */
    val durationMillis: Long
    val isInfinite: Boolean

    init {
        val collected = mutableListOf<PlacedAnim>()
        durationMillis = layout(GroupNode(parallel = false).apply { children += root }, 0L, collected)
        placed = collected
        isInfinite = collected.any { it.spec.iterations == INFINITE_ITERATIONS }
        collected.sortedWith(compareBy({ it.activeStart }, { it.order })).forEach { anim ->
            val spec = anim.spec
            if (spec.channel == Channel.Color) {
                colorTracks[spec.shape].add(anim, spec.toColor, null)
            } else {
                floatTracks[spec.shape][spec.channel.ordinal].add(anim, spec.to, spec.from)
            }
        }
    }

    /** Writes the state at [timeMillis] into [frame]. */
    fun evaluate(timeMillis: Double, frame: Frame) {
        for (shape in floatTracks.indices) {
            for (channel in 0 until FLOAT_CHANNELS) {
                frame.floats[shape * FLOAT_CHANNELS + channel] = floatTracks[shape][channel].valueAt(timeMillis)
            }
            frame.colors[shape] = colorTracks[shape].valueAt(timeMillis).value.toLong()
        }
    }

    /** The state after everything has settled; for infinite animations this is the end of an iteration. */
    fun evaluateFinal(frame: Frame) = evaluate(Double.POSITIVE_INFINITY, frame)

    private fun layout(node: Node, start: Long, out: MutableList<PlacedAnim>): Long = when (node) {
        is AnimNode -> {
            out += PlacedAnim(node.spec, start, out.size)
            val passes = if (node.spec.iterations == INFINITE_ITERATIONS) 1 else node.spec.iterations
            node.spec.delayMillis + node.spec.durationMillis.toLong() * passes
        }

        is WaitNode -> node.millis.toLong()

        is GroupNode -> if (node.parallel) {
            node.children.maxOfOrNull { layout(it, start, out) } ?: 0L
        } else {
            var cursor = start
            node.children.forEach { cursor += layout(it, cursor, out) }
            cursor - start
        }
    }
}

private fun lerpFloat(start: Float, stop: Float, fraction: Float): Float = start + (stop - start) * fraction
