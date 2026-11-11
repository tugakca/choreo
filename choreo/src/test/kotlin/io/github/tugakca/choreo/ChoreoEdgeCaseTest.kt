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

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ChoreoEdgeCaseTest {
    @get:Rule
    val rule = createComposeRule()

    private var choreography: Choreography? = null

    private fun withAnimatorScale(scale: Float, block: () -> Unit) {
        val resolver = ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver
        Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, scale)
        try {
            block()
        } finally {
            Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        }
    }

    @Composable
    private fun Moving(policy: ReducedMotionPolicy) {
        val c = rememberChoreography(reducedMotion = policy) {
            circle(10f).moveTo(Offset(100f, 0f), durationMillis = 1000, easing = LinearEasing)
        }
        choreography = c
        ChoreoCanvas(c, Modifier.size(100.dp))
    }

    @Test
    fun alwaysPlayStillAnimatesWhenTheSystemScaleIsZero() = withAnimatorScale(0f) {
        rule.mainClock.autoAdvance = false
        rule.setContent { Moving(ReducedMotionPolicy.AlwaysPlay) }
        rule.mainClock.advanceTimeBy(500)
        assertThat(choreography!!.state).isEqualTo(ChoreoState.Playing)
        assertThat(choreography!!.progress).isWithin(0.1f).of(0.5f)
    }

    @Test
    fun followSystemSlowsDownWithADoubledScale() = withAnimatorScale(2f) {
        rule.mainClock.autoAdvance = false
        rule.setContent { Moving(ReducedMotionPolicy.FollowSystem) }
        rule.mainClock.advanceTimeBy(1000)
        assertThat(choreography!!.state).isEqualTo(ChoreoState.Playing)
        assertThat(choreography!!.progress).isWithin(0.1f).of(0.5f)
    }

    @Test
    fun clockStopsInTheBackgroundAndContinuesWithoutAJump() {
        val owner = object : LifecycleOwner {
            val registry = LifecycleRegistry.createUnsafe(this)
            override val lifecycle: Lifecycle get() = registry
        }
        owner.registry.currentState = Lifecycle.State.RESUMED
        rule.mainClock.autoAdvance = false
        rule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) { Moving(ReducedMotionPolicy.AlwaysPlay) }
        }
        rule.mainClock.advanceTimeBy(300)
        val before = choreography!!.positionMillis
        rule.runOnIdle { owner.registry.currentState = Lifecycle.State.CREATED }
        rule.mainClock.advanceTimeBy(5_000)
        assertThat(choreography!!.positionMillis).isAtMost(before + 50)
        rule.runOnIdle { owner.registry.currentState = Lifecycle.State.RESUMED }
        rule.mainClock.advanceTimeBy(100)
        assertThat(choreography!!.state).isEqualTo(ChoreoState.Playing)
        assertThat(choreography!!.positionMillis).isIn(before..before + 150)
    }

    /**
     * Lazy lists and navigation keep item state in a SaveableStateHolder. An item scrolled off while
     * playing must not come back stuck in Cancelled.
     */
    @Test
    fun itemScrolledOffAndBackIsNotStuckCancelled() {
        var show by mutableStateOf(true)
        rule.mainClock.autoAdvance = false
        rule.setContent {
            val holder = rememberSaveableStateHolder()
            if (show) holder.SaveableStateProvider("item") { Moving(ReducedMotionPolicy.AlwaysPlay) }
        }
        rule.mainClock.advanceTimeBy(300)
        rule.runOnIdle { show = false }
        rule.mainClock.advanceTimeBy(100)
        rule.runOnIdle {
            show = true
            Snapshot.sendApplyNotifications()
        }
        rule.mainClock.advanceTimeBy(100)
        assertThat(choreography!!.state).isEqualTo(ChoreoState.Playing)
        assertThat(choreography!!.positionMillis).isAtLeast(280)
    }

    @Test
    fun readmeQuickStartBuildsAndFinishes() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            val c = rememberChoreography(reducedMotion = ReducedMotionPolicy.AlwaysPlay) {
                val circle = circle(radius = 16f, center = Offset(40f, 60f), style = ShapeStyle(Color(0xFF3B5BDB)))
                val star = star(points = 5, outerRadius = 24f, center = Offset(200f, 60f))
                sequence {
                    circle.moveTo(Offset(200f, 60f), durationMillis = 800)
                    parallel {
                        star.rotateTo(180f, durationMillis = 600)
                        star.colorTo(Color(0xFFA23B72), durationMillis = 600)
                    }
                    star.eraseOff(durationMillis = 500)
                }
            }
            choreography = c
            ChoreoCanvas(c, Modifier.size(240.dp, 120.dp), contentDescription = "quick start")
        }
        assertThat(choreography!!.durationMillis).isEqualTo(1900L)
        rule.mainClock.advanceTimeBy(2_000)
        assertThat(choreography!!.state).isEqualTo(ChoreoState.Finished)
    }

    @Test
    fun everyAnimationRejectsNegativeTimingWithItsName() {
        val names = listOf("moveTo", "scaleTo", "rotateTo", "fadeTo", "colorTo", "drawOn", "eraseOff")
        names.forEach { name ->
            val error = assertThrows(IllegalArgumentException::class.java) {
                ChoreographyScope(GroupNode(parallel = false)).apply {
                    val c = circle(5f)
                    when (name) {
                        "moveTo" -> c.moveTo(Offset.Zero, durationMillis = -1)
                        "scaleTo" -> c.scaleTo(2f, durationMillis = -1)
                        "rotateTo" -> c.rotateTo(90f, durationMillis = -1)
                        "fadeTo" -> c.fadeTo(0f, durationMillis = -1)
                        "colorTo" -> c.colorTo(Color.Red, durationMillis = -1)
                        "drawOn" -> c.drawOn(durationMillis = -1)
                        else -> c.eraseOff(durationMillis = -1)
                    }
                }
            }
            assertThat(error).hasMessageThat().startsWith(name)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ChoreographyScope(GroupNode(parallel = false)).apply { circle(5f).scaleTo(2f, delayMillis = -1) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            ChoreographyScope(GroupNode(parallel = false)).apply { circle(5f).scaleTo(2f, iterations = 0) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            ChoreographyScope(GroupNode(parallel = false)).apply { wait(-1) }
        }
    }

    @Test
    fun interruptionByALaterParallelStepIsContinuous() {
        val root = GroupNode(parallel = false)
        val scope = ChoreographyScope(root).apply {
            val c = circle(5f)
            parallel {
                c.moveTo(Offset(100f, 0f), durationMillis = 1000, easing = LinearEasing)
                c.moveTo(Offset(0f, 0f), durationMillis = 400, delayMillis = 500)
            }
        }
        val timeline =
            Timeline(scope.shapes.map { ShapeInitial(it.x, it.y, it.style.alpha, it.style.color) }, root.children)
        val frame = Frame(1)
        timeline.evaluate(499.999, frame)
        val before = frame.float(0, Channel.X)
        timeline.evaluate(500.001, frame)
        val after = frame.float(0, Channel.X)
        assertThat(after).isWithin(0.5f).of(before)
        timeline.evaluate(900.0, frame)
        assertThat(frame.float(0, Channel.X)).isWithin(0.01f).of(0f)
        // The interrupted step is no longer in charge, even though it would still be running.
        timeline.evaluate(950.0, frame)
        assertThat(frame.float(0, Channel.X)).isWithin(0.01f).of(0f)
    }

    @Test
    fun cancelledChoreographyReplaysFromTheStart() {
        rule.mainClock.autoAdvance = false
        rule.setContent { Moving(ReducedMotionPolicy.AlwaysPlay) }
        rule.mainClock.advanceTimeBy(400)
        rule.runOnIdle { choreography!!.cancel() }
        val frozen = choreography!!.positionMillis
        rule.mainClock.advanceTimeBy(400)
        assertThat(choreography!!.positionMillis).isEqualTo(frozen)
        rule.runOnIdle { choreography!!.play() }
        rule.mainClock.advanceTimeBy(100)
        assertThat(choreography!!.state).isEqualTo(ChoreoState.Playing)
        assertThat(choreography!!.positionMillis).isLessThan(frozen)
    }
}
