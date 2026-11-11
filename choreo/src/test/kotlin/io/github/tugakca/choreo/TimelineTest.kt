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

import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TimelineTest {
    private fun anim(
        channel: Channel = Channel.X,
        to: Float = 100f,
        from: Float? = null,
        duration: Int = 1000,
        delay: Int = 0,
        iterations: Int = 1,
        color: Color = Color.Unspecified,
    ) = AnimNode(AnimSpec("test", 0, channel, to, color, from, duration, delay, iterations, LinearEasing))

    private fun sequence(vararg nodes: Node) = GroupNode(parallel = false).apply { children += nodes }

    private fun parallel(vararg nodes: Node) = GroupNode(parallel = true).apply { children += nodes }

    private fun timeline(vararg nodes: Node) = Timeline(listOf(ShapeInitial(0f, 0f, 1f, Color.Black)), nodes.toList())

    private fun Timeline.value(time: Double, channel: Channel = Channel.X): Float {
        val frame = Frame(1)
        evaluate(time, frame)
        return frame.float(0, channel)
    }

    @Test
    fun sequenceDurationIsTheSumOfItsSteps() {
        val timeline = timeline(sequence(anim(duration = 300), anim(duration = 500), anim(duration = 200)))
        assertThat(timeline.durationMillis).isEqualTo(1000)
    }

    @Test
    fun parallelDurationIsTheLongestStep() {
        val timeline = timeline(parallel(anim(duration = 300), anim(duration = 800), anim(duration = 200)))
        assertThat(timeline.durationMillis).isEqualTo(800)
    }

    @Test
    fun nestedGroupsCompose() {
        val timeline = timeline(
            anim(duration = 100),
            parallel(anim(duration = 200), sequence(anim(duration = 150), anim(duration = 150))),
            anim(duration = 400),
        )
        assertThat(timeline.durationMillis).isEqualTo(100 + 300 + 400)
    }

    @Test
    fun delayAndRepeatCountTowardsDuration() {
        val timeline = timeline(anim(duration = 200, delay = 100, iterations = 3))
        assertThat(timeline.durationMillis).isEqualTo(700)
    }

    @Test
    fun infiniteRepeatCountsAsOneIteration() {
        val timeline = timeline(anim(duration = 200, iterations = INFINITE_ITERATIONS), anim(duration = 100))
        assertThat(timeline.durationMillis).isEqualTo(300)
        assertThat(timeline.isInfinite).isTrue()
    }

    @Test
    fun valuesAreInterpolatedAndSeekingIsRandomAccess() {
        val timeline = timeline(anim(to = 100f, duration = 1000))
        assertThat(timeline.value(0.0)).isEqualTo(0f)
        assertThat(timeline.value(250.0)).isEqualTo(25f)
        assertThat(timeline.value(1000.0)).isEqualTo(100f)
        assertThat(timeline.value(5000.0)).isEqualTo(100f)
        // Going backwards gives exactly the same answers: the timeline is a pure function of time.
        assertThat(timeline.value(250.0)).isEqualTo(25f)
    }

    @Test
    fun sequenceRunsStepsInOrder() {
        val timeline = timeline(sequence(anim(to = 100f, duration = 1000), anim(to = 0f, duration = 1000)))
        assertThat(timeline.value(1000.0)).isEqualTo(100f)
        assertThat(timeline.value(1500.0)).isEqualTo(50f)
        assertThat(timeline.value(2000.0)).isEqualTo(0f)
    }

    @Test
    fun parallelRunsStepsAtTheSameTime() {
        val timeline = timeline(parallel(anim(Channel.X, 100f, duration = 1000), anim(Channel.Y, 40f, duration = 500)))
        assertThat(timeline.value(500.0, Channel.X)).isEqualTo(50f)
        assertThat(timeline.value(500.0, Channel.Y)).isEqualTo(40f)
    }

    @Test
    fun delayHoldsTheInitialValue() {
        val timeline = timeline(anim(to = 100f, duration = 1000, delay = 500))
        assertThat(timeline.value(400.0)).isEqualTo(0f)
        assertThat(timeline.value(1000.0)).isEqualTo(50f)
    }

    @Test
    fun repeatsRestartFromTheStartValue() {
        val timeline = timeline(anim(to = 100f, duration = 1000, iterations = 2))
        assertThat(timeline.value(500.0)).isEqualTo(50f)
        assertThat(timeline.value(1500.0)).isEqualTo(50f)
        assertThat(timeline.value(2000.0)).isEqualTo(100f)
    }

    @Test
    fun infiniteRepeatKeepsLooping() {
        val timeline = timeline(anim(to = 100f, duration = 1000, iterations = INFINITE_ITERATIONS))
        assertThat(timeline.value(250.0)).isEqualTo(25f)
        assertThat(timeline.value(100_250.0)).isEqualTo(25f)
    }

    @Test
    fun finalFrameOfAnInfiniteAnimationIsItsEndValue() {
        val timeline = timeline(anim(to = 100f, duration = 1000, iterations = INFINITE_ITERATIONS))
        val frame = Frame(1)
        timeline.evaluateFinal(frame)
        assertThat(frame.float(0, Channel.X)).isEqualTo(100f)
    }

    @Test
    fun zeroDurationAppliesInstantly() {
        val timeline = timeline(sequence(anim(to = 70f, duration = 0), anim(Channel.Y, 5f, duration = 1000)))
        assertThat(timeline.durationMillis).isEqualTo(1000)
        assertThat(timeline.value(0.0)).isEqualTo(70f)
    }

    @Test
    fun aNewAnimationContinuesFromTheCurrentValueInsteadOfJumping() {
        // The first animation is interrupted at 500 ms, when X is 50; the second starts from 50.
        val timeline = timeline(
            parallel(anim(to = 100f, duration = 1000), sequence(WaitNode(500), anim(to = 0f, duration = 1000))),
        )
        assertThat(timeline.value(499.0)).isWithin(0.2f).of(49.9f)
        assertThat(timeline.value(500.0)).isEqualTo(50f)
        assertThat(timeline.value(1000.0)).isEqualTo(25f)
        assertThat(timeline.value(1500.0)).isEqualTo(0f)
    }

    @Test
    fun explicitStartValueOverridesContinuation() {
        val timeline = timeline(sequence(anim(to = 100f, duration = 1000), anim(to = 0f, from = 10f, duration = 1000)))
        assertThat(timeline.value(1000.0)).isEqualTo(10f)
    }

    @Test
    fun drawOnHidesTheShapeBeforeItStarts() {
        val timeline = timeline(sequence(WaitNode(500), anim(Channel.TrimEnd, 1f, from = 0f, duration = 1000)))
        assertThat(timeline.value(0.0, Channel.TrimEnd)).isEqualTo(0f)
        assertThat(timeline.value(1000.0, Channel.TrimEnd)).isEqualTo(0.5f)
        assertThat(timeline.value(1500.0, Channel.TrimEnd)).isEqualTo(1f)
    }

    @Test
    fun colorsInterpolate() {
        val timeline = timeline(anim(Channel.Color, duration = 1000, color = Color.White))
        val frame = Frame(1)
        timeline.evaluate(0.0, frame)
        assertThat(frame.color(0)).isEqualTo(Color.Black)
        timeline.evaluate(1000.0, frame)
        assertThat(frame.color(0)).isEqualTo(Color.White)
    }

    @Test
    fun placedStepsReportTheirPosition() {
        val timeline = timeline(sequence(anim(duration = 300), anim(duration = 200, delay = 50)))
        assertThat(
            timeline.placed.map {
                it.startMillis to it.endMillis
            },
        ).containsExactly(0L to 300L, 300L to 550L).inOrder()
    }
}
