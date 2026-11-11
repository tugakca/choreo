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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.first

/**
 * Creates and remembers a [Choreography].
 *
 * The [block] runs once: recomposition never resets a running animation. It runs again (and the
 * animation restarts) only when one of [keys] changes. Position and play state survive configuration
 * changes such as screen rotation. The clock stops while the app is in the background, and the
 * choreography stops (state [ChoreoState.Cancelled], without calling [onEnd]) when it leaves the composition,
 * and its pre-exit state is what gets saved, so it can come back playing.
 *
 * @param keys Values the choreography depends on; a change rebuilds it.
 * @param autoPlay Start playing as soon as it is created (default `true`). Only read on creation.
 * @param unit Unit of every coordinate, size and stroke width in [block] (default [ChoreoUnit.Dp]).
 * @param reducedMotion How to react to the system animation scale; see [ReducedMotionPolicy].
 * @param onEnd Called with the reason when it finishes or is cancelled.
 * @param block Declares the shapes and animations; see [ChoreographyScope].
 */
@OptIn(ExperimentalChoreoApi::class)
@Composable
public fun rememberChoreography(
    vararg keys: Any?,
    autoPlay: Boolean = true,
    unit: ChoreoUnit = ChoreoUnit.Dp,
    reducedMotion: ReducedMotionPolicy = LocalReducedMotionPolicy.current,
    onEnd: (ChoreoEndReason) -> Unit = {},
    block: ChoreographyScope.() -> Unit,
): Choreography {
    val systemScale by rememberSystemAnimationScale()
    val reduce = when (reducedMotion) {
        ReducedMotionPolicy.FollowSystem -> systemScale == 0f
        ReducedMotionPolicy.AlwaysPlay -> false
        ReducedMotionPolicy.AlwaysFinalFrame -> true
    }
    val speed = if (reducedMotion == ReducedMotionPolicy.FollowSystem && systemScale > 0f) 1f / systemScale else 1f

    fun create(play: Boolean): Choreography {
        val root = GroupNode(parallel = false)
        val scope = ChoreographyScope(root).apply(block)
        val timeline =
            Timeline(scope.shapes.map { ShapeInitial(it.x, it.y, it.style.alpha, it.style.color) }, root.children)
        return Choreography(timeline, scope.shapes.toList(), unit, reduce).also {
            it.speed = speed
            it.onEnd = onEnd
            if (play) it.play()
        }
    }

    val saver = Saver<Choreography, List<Any>>(
        save = { choreography ->
            val (timeNanos, state) = choreography.snapshot()
            listOf(timeNanos, state.ordinal)
        },
        restore = { saved ->
            create(play = false).also { it.restore(saved[0] as Long, ChoreoState.entries[saved[1] as Int]) }
        },
    )
    val choreography = rememberSaveable(*keys, unit, saver = saver) { create(play = autoPlay) }

    SideEffect {
        choreography.reduceMotion = reduce
        choreography.speed = speed
        choreography.onEnd = onEnd
    }
    DisposableEffect(choreography) { onDispose { choreography.dispose() } }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(choreography, lifecycle) {
        // Only frames delivered while the app is started advance the clock, so it stops in the background.
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            var last = -1L
            while (true) {
                if (choreography.state != ChoreoState.Playing) {
                    last = -1L
                    snapshotFlow { choreography.state }.first { it == ChoreoState.Playing }
                }
                val now = withFrameNanos { it }
                choreography.advance(if (last < 0L) 0L else now - last)
                last = now
            }
        }
    }
    return choreography
}
