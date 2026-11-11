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

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Lifecycle state of a [Choreography]. */
public enum class ChoreoState {
    /** Created with `autoPlay = false` and not started yet. */
    Idle,

    /** The clock is running. */
    Playing,

    /** Stopped part-way; [Choreography.resume] or [Choreography.play] continues from there. */
    Paused,

    /** Reached the end (or was jumped to the final frame by reduced motion). */
    Finished,

    /** Stopped by [Choreography.cancel] or by leaving the composition. */
    Cancelled,
}

/** Why a choreography stopped; delivered to the `onEnd` callback. */
public enum class ChoreoEndReason {
    /** The timeline reached its end. */
    Finished,

    /** [Choreography.cancel] was called, or the choreography left the composition while active. */
    Cancelled,
}

/** Whether a [ChoreoCanvas] coordinates are density-independent pixels or raw pixels. */
public enum class ChoreoUnit {
    /** Coordinates, sizes and stroke widths are in dp (default). */
    Dp,

    /** Coordinates, sizes and stroke widths are in raw pixels. */
    Px,
}

/**
 * Controls a choreography created by [rememberChoreography]: play, pause, seek and observe it.
 *
 * Everything shown by [ChoreoCanvas] is computed from one deterministic timeline, so the same position
 * always produces the same frame. All members are safe to call from the main thread; [state],
 * [progress] and [positionMillis] are snapshot-state backed and can be read from composition.
 */
@Stable
public class Choreography internal constructor(
    internal val timeline: Timeline,
    internal val shapes: List<ShapeSpec>,
    /** Coordinate unit used when drawing. */
    public val unit: ChoreoUnit,
    reduceMotion: Boolean,
) {
    private var timeNanos by mutableLongStateOf(0L)

    /** Current lifecycle state. */
    public var state: ChoreoState by mutableStateOf(ChoreoState.Idle)
        private set

    internal var onEnd: (ChoreoEndReason) -> Unit = {}

    /** True when the final frame must be shown instead of playing (system setting or policy). */
    internal var reduceMotion: Boolean = reduceMotion

    /** How fast the clock runs relative to wall time (the inverse of the system animation scale). */
    internal var speed: Float = 1f

    /** Length of one pass of the timeline; infinitely repeating animations count as one iteration. */
    public val durationMillis: Long get() = timeline.durationMillis

    /** True if at least one animation repeats forever, so the timeline never finishes on its own. */
    public val isInfinite: Boolean get() = timeline.isInfinite

    /** Current position in milliseconds, in `0..durationMillis` for finite timelines. */
    public val positionMillis: Long get() = timeNanos / NANOS_PER_MILLI

    /**
     * Position as a fraction in `0f..1f`. For infinite timelines it is the position within the current
     * iteration.
     */
    public val progress: Float
        get() {
            val duration = timeline.durationMillis
            if (duration == 0L) return if (state == ChoreoState.Finished) 1f else 0f
            val position = timeNanos.toDouble() / NANOS_PER_MILLI
            if (timeline.isInfinite &&
                state != ChoreoState.Finished
            ) {
                return ((position % duration) / duration).toFloat()
            }
            return (position / duration).coerceIn(0.0, 1.0).toFloat()
        }

    /** The animations of this choreography, one per DSL call, in declaration order with their placement. */
    @ExperimentalChoreoApi
    public val steps: List<ChoreoStep>
        get() = timeline.placed
            // moveTo, drawOn and friends animate several properties; report them once.
            .distinctBy { listOf(it.spec.name, it.spec.shape, it.startMillis, it.endMillis) }
            .map { ChoreoStep(it.spec.name, ShapeHandle(it.spec.shape), it.startMillis, it.endMillis) }

    /**
     * Starts playing from the current position. A finished, cancelled or fully played choreography
     * starts again from the beginning. Does nothing while already playing.
     */
    public fun play() {
        if (state == ChoreoState.Playing) return
        if (state == ChoreoState.Finished || state == ChoreoState.Cancelled || atEnd()) timeNanos = 0L
        start()
    }

    /** Pauses at the current position. Does nothing unless playing. */
    public fun pause() {
        if (state == ChoreoState.Playing) state = ChoreoState.Paused
    }

    /** Continues a paused choreography. Does nothing unless paused. */
    public fun resume() {
        if (state == ChoreoState.Paused) start()
    }

    /**
     * Stops the choreography, keeping the current frame, and reports [ChoreoEndReason.Cancelled].
     * Does nothing unless playing or paused. [play] afterwards starts from the beginning.
     */
    public fun cancel() {
        if (state != ChoreoState.Playing && state != ChoreoState.Paused) return
        state = ChoreoState.Cancelled
        onEnd(ChoreoEndReason.Cancelled)
    }

    /** Jumps to the beginning and plays. */
    public fun restart() {
        timeNanos = 0L
        start()
    }

    /**
     * Moves to [fraction] of the timeline (`0f..1f`). A playing choreography keeps playing from the new
     * position; otherwise it ends up paused there, so the frame can be inspected.
     *
     * @throws IllegalArgumentException if [fraction] is outside `0f..1f`.
     */
    public fun seek(fraction: Float) {
        require(fraction in 0f..1f) { "seek fraction must be within 0..1 but was $fraction" }
        timeNanos = (fraction.toDouble() * timeline.durationMillis * NANOS_PER_MILLI).toLong()
        when {
            state == ChoreoState.Playing -> if (atEnd()) finish()
            state == ChoreoState.Finished && atEnd() -> Unit
            else -> state = ChoreoState.Paused
        }
    }

    internal fun advance(deltaNanos: Long) {
        if (state != ChoreoState.Playing) return
        if (reduceMotion) {
            finish()
            return
        }
        timeNanos += (deltaNanos * speed).toLong()
        if (atEnd()) finish()
    }

    /** Called when the choreography leaves the composition. */
    internal fun dispose() = cancel()

    internal fun frameTimeMillis(): Double = timeNanos.toDouble() / NANOS_PER_MILLI

    internal fun snapshot(): Pair<Long, ChoreoState> = timeNanos to state

    internal fun restore(timeNanos: Long, state: ChoreoState) {
        this.timeNanos = timeNanos
        this.state = state
        if (state == ChoreoState.Playing && reduceMotion) finish()
    }

    private fun start() {
        if (reduceMotion || (timeline.durationMillis == 0L && !timeline.isInfinite)) {
            finish()
        } else {
            state = ChoreoState.Playing
        }
    }

    private fun atEnd(): Boolean = !timeline.isInfinite && timeNanos >= timeline.durationMillis * NANOS_PER_MILLI

    private fun finish() {
        timeNanos = timeline.durationMillis * NANOS_PER_MILLI
        state = ChoreoState.Finished
        onEnd(ChoreoEndReason.Finished)
    }

    private companion object {
        const val NANOS_PER_MILLI = 1_000_000L
    }
}

/** Identifies a shape created in a [ChoreographyScope]; pass it to animation functions. */
public class ShapeHandle internal constructor(internal val index: Int)

/**
 * Placement of one animation on the timeline, for tools such as timeline views.
 *
 * @property name Animation kind, for example `"moveTo"`.
 * @property shape The shape being animated.
 * @property startMillis When the animation (including its delay) begins.
 * @property endMillis When its first pass ends.
 */
@ExperimentalChoreoApi
public class ChoreoStep internal constructor(
    public val name: String,
    public val shape: ShapeHandle,
    public val startMillis: Long,
    public val endMillis: Long,
)
