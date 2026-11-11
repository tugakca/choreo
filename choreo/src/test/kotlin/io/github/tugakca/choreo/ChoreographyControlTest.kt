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

import androidx.compose.ui.geometry.Offset
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ChoreographyControlTest {
    private val ends = mutableListOf<ChoreoEndReason>()

    private fun create(durationMillis: Int = 1000, reduceMotion: Boolean = false, iterations: Int = 1): Choreography {
        val root = GroupNode(parallel = false)
        val scope = ChoreographyScope(root).apply {
            circle(10f).moveTo(Offset(100f, 0f), durationMillis = durationMillis, iterations = iterations)
        }
        val timeline =
            Timeline(scope.shapes.map { ShapeInitial(it.x, it.y, it.style.alpha, it.style.color) }, root.children)
        return Choreography(timeline, scope.shapes, ChoreoUnit.Dp, reduceMotion).also { it.onEnd = ends::add }
    }

    private fun Choreography.tick(millis: Long) = advance(millis * 1_000_000L)

    @Test
    fun startsIdleAndPlayAdvancesUntilFinished() {
        val choreography = create()
        assertThat(choreography.state).isEqualTo(ChoreoState.Idle)
        choreography.play()
        choreography.tick(400)
        assertThat(choreography.state).isEqualTo(ChoreoState.Playing)
        assertThat(choreography.progress).isWithin(0.001f).of(0.4f)
        choreography.tick(700)
        assertThat(choreography.state).isEqualTo(ChoreoState.Finished)
        assertThat(choreography.progress).isEqualTo(1f)
        assertThat(ends).containsExactly(ChoreoEndReason.Finished)
    }

    @Test
    fun pauseStopsTheClockAndResumeContinues() {
        val choreography = create().also { it.play() }
        choreography.tick(300)
        choreography.pause()
        choreography.tick(300)
        assertThat(choreography.state).isEqualTo(ChoreoState.Paused)
        assertThat(choreography.positionMillis).isEqualTo(300)
        choreography.resume()
        choreography.tick(200)
        assertThat(choreography.positionMillis).isEqualTo(500)
    }

    @Test
    fun cancelKeepsTheFrameAndReportsCancelled() {
        val choreography = create().also { it.play() }
        choreography.tick(500)
        choreography.cancel()
        assertThat(choreography.state).isEqualTo(ChoreoState.Cancelled)
        assertThat(choreography.positionMillis).isEqualTo(500)
        assertThat(ends).containsExactly(ChoreoEndReason.Cancelled)
        choreography.play()
        assertThat(choreography.positionMillis).isEqualTo(0)
        assertThat(choreography.state).isEqualTo(ChoreoState.Playing)
    }

    @Test
    fun cancelWhileIdleDoesNothing() {
        val choreography = create()
        choreography.cancel()
        assertThat(choreography.state).isEqualTo(ChoreoState.Idle)
        assertThat(ends).isEmpty()
    }

    @Test
    fun restartJumpsToTheBeginning() {
        val choreography = create().also { it.play() }
        choreography.tick(1200)
        choreography.restart()
        assertThat(choreography.state).isEqualTo(ChoreoState.Playing)
        assertThat(choreography.positionMillis).isEqualTo(0)
    }

    @Test
    fun seekMovesAndPausesWhenNotPlaying() {
        val choreography = create()
        choreography.seek(0.25f)
        assertThat(choreography.positionMillis).isEqualTo(250)
        assertThat(choreography.state).isEqualTo(ChoreoState.Paused)
        choreography.play()
        choreography.tick(250)
        assertThat(choreography.positionMillis).isEqualTo(500)
    }

    @Test
    fun seekWhilePlayingKeepsPlayingAndSeekingToTheEndFinishes() {
        val choreography = create().also { it.play() }
        choreography.seek(0.5f)
        assertThat(choreography.state).isEqualTo(ChoreoState.Playing)
        choreography.seek(1f)
        assertThat(choreography.state).isEqualTo(ChoreoState.Finished)
    }

    @Test
    fun seekRejectsValuesOutsideZeroToOne() {
        val choreography = create()
        val error = assertThrows(IllegalArgumentException::class.java) { choreography.seek(1.5f) }
        assertThat(error).hasMessageThat().contains("0..1")
        assertThrows(IllegalArgumentException::class.java) { choreography.seek(Float.NaN) }
    }

    @Test
    fun zeroDurationFinishesImmediately() {
        val choreography = create(durationMillis = 0)
        choreography.play()
        assertThat(choreography.state).isEqualTo(ChoreoState.Finished)
        assertThat(ends).containsExactly(ChoreoEndReason.Finished)
    }

    @Test
    fun reducedMotionJumpsToTheFinalFrame() {
        val choreography = create(reduceMotion = true)
        choreography.play()
        assertThat(choreography.state).isEqualTo(ChoreoState.Finished)
        assertThat(choreography.progress).isEqualTo(1f)
    }

    @Test
    fun reducedMotionTurnedOnWhilePlayingFinishesOnTheNextTick() {
        val choreography = create().also { it.play() }
        choreography.tick(100)
        choreography.reduceMotion = true
        choreography.tick(16)
        assertThat(choreography.state).isEqualTo(ChoreoState.Finished)
    }

    @Test
    fun speedScalesTheClock() {
        val choreography = create().also { it.play() }
        choreography.speed = 0.5f
        choreography.tick(400)
        assertThat(choreography.positionMillis).isEqualTo(200)
    }

    @Test
    fun infiniteChoreographyNeverFinishesAndReportsProgressWithinTheIteration() {
        val choreography = create(iterations = ChoreoDefaults.INFINITE).also { it.play() }
        choreography.tick(2250)
        assertThat(choreography.state).isEqualTo(ChoreoState.Playing)
        assertThat(choreography.progress).isWithin(0.001f).of(0.25f)
        assertThat(ends).isEmpty()
    }

    @Test
    fun savedStateRestoresPositionAndState() {
        val choreography = create().also { it.play() }
        choreography.tick(400)
        val (time, state) = choreography.snapshot()
        val restored = create().also { it.restore(time, state) }
        assertThat(restored.state).isEqualTo(ChoreoState.Playing)
        assertThat(restored.positionMillis).isEqualTo(400)
    }
}
