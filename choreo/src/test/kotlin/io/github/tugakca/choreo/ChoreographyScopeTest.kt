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
import androidx.compose.ui.geometry.Size
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalChoreoApi::class)
@RunWith(RobolectricTestRunner::class)
class ChoreographyScopeTest {
    private fun build(block: ChoreographyScope.() -> Unit): Timeline {
        val root = GroupNode(parallel = false)
        val scope = ChoreographyScope(root).apply(block)
        return Timeline(scope.shapes.map { ShapeInitial(it.x, it.y, it.style.alpha, it.style.color) }, root.children)
    }

    @Test
    fun topLevelStepsRunInSequenceAndMoveToCostsOnlyItsDuration() {
        val timeline = build {
            val circle = circle(10f)
            circle.moveTo(Offset(100f, 50f), durationMillis = 400)
            circle.scaleTo(2f, durationMillis = 600)
        }
        assertThat(timeline.durationMillis).isEqualTo(1000)
    }

    @Test
    fun parallelAndSequenceNest() {
        val timeline = build {
            val a = circle(10f)
            val b = rectangle(Size(10f, 10f))
            sequence {
                a.fadeTo(0.5f, durationMillis = 100)
                parallel {
                    a.moveTo(Offset(10f, 10f), durationMillis = 300)
                    sequence {
                        b.rotateTo(90f, durationMillis = 100)
                        b.colorTo(androidx.compose.ui.graphics.Color.Red, durationMillis = 100)
                    }
                }
            }
        }
        assertThat(timeline.durationMillis).isEqualTo(400)
    }

    @Test
    fun negativeDurationThrowsWithAClearMessage() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            build { circle(10f).moveTo(Offset.Zero, durationMillis = -1) }
        }
        assertThat(error).hasMessageThat().contains("durationMillis must be >= 0")
        assertThat(error).hasMessageThat().contains("moveTo")
    }

    @Test
    fun zeroDurationIsValidAndInstant() {
        val timeline = build { circle(10f).scaleTo(3f, durationMillis = 0) }
        val frame = Frame(1)
        timeline.evaluate(0.0, frame)
        assertThat(timeline.durationMillis).isEqualTo(0)
        assertThat(frame.float(0, Channel.Scale)).isEqualTo(3f)
    }

    @Test
    fun invalidParametersThrow() {
        assertThrows(IllegalArgumentException::class.java) { build { circle(10f).fadeTo(2f) } }
        assertThrows(IllegalArgumentException::class.java) { build { circle(10f).scaleTo(1f, delayMillis = -5) } }
        assertThrows(IllegalArgumentException::class.java) { build { circle(10f).scaleTo(1f, iterations = 0) } }
        assertThrows(IllegalArgumentException::class.java) { build { circle(0f) } }
        assertThrows(IllegalArgumentException::class.java) { build { polygon(2, 10f) } }
        assertThrows(IllegalArgumentException::class.java) { build { star(5, 10f, 20f) } }
        assertThrows(IllegalArgumentException::class.java) { build { svgPath("not a path") } }
        assertThrows(IllegalArgumentException::class.java) { build { circle(10f).drawRange(0.8f, 0.2f) } }
        assertThrows(IllegalArgumentException::class.java) { build { wait(-1) } }
    }

    @Test
    fun allShapeKindsCanBeCreated() {
        val timeline = build {
            circle(10f)
            rectangle(Size(10f, 20f))
            roundedRectangle(Size(10f, 20f), 4f)
            line(Offset.Zero, Offset(10f, 10f))
            polygon(6, 10f)
            star(5, 10f)
            path(androidx.compose.ui.graphics.Path().apply { lineTo(10f, 10f) })
            svgPath("M0 0 L10 0 L10 10 Z")
        }
        assertThat(timeline.durationMillis).isEqualTo(0)
    }

    @Test
    fun drawOnDrawsFromNothingToComplete() {
        val timeline = build { circle(10f).drawOn(durationMillis = 1000) }
        val frame = Frame(1)
        timeline.evaluate(0.0, frame)
        assertThat(frame.float(0, Channel.TrimEnd)).isEqualTo(0f)
        timeline.evaluate(1000.0, frame)
        assertThat(frame.float(0, Channel.TrimEnd)).isEqualTo(1f)
        assertThat(frame.float(0, Channel.TrimStart)).isEqualTo(0f)
    }

    @Test
    fun eraseOffEndsWithNothingVisible() {
        val timeline = build { circle(10f).eraseOff(durationMillis = 1000) }
        val frame = Frame(1)
        timeline.evaluate(1000.0, frame)
        assertThat(frame.float(0, Channel.TrimStart)).isEqualTo(1f)
        assertThat(frame.float(0, Channel.TrimEnd)).isEqualTo(1f)
    }
}
