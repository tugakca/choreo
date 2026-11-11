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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@OptIn(ExperimentalChoreoApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ChoreoCanvasTest {
    @get:Rule
    val rule = createComposeRule()

    private var choreography: Choreography? = null

    private fun setContent(
        autoPlay: Boolean = true,
        policy: ReducedMotionPolicy = ReducedMotionPolicy.AlwaysPlay,
        onEnd: (ChoreoEndReason) -> Unit = {},
        extra: @androidx.compose.runtime.Composable () -> Unit = {},
    ) = rule.setContent {
        val c = rememberChoreography(autoPlay = autoPlay, reducedMotion = policy, onEnd = onEnd) {
            circle(10f).moveTo(Offset(100f, 0f), durationMillis = 1000)
        }
        choreography = c
        ChoreoCanvas(c, Modifier.size(100.dp), contentDescription = "Moving circle")
        extra()
    }

    @Test
    fun autoPlayStartsAndFinishes() {
        val ends = mutableListOf<ChoreoEndReason>()
        rule.mainClock.autoAdvance = false
        setContent(onEnd = ends::add)
        rule.mainClock.advanceTimeBy(500)
        assertThat(choreography!!.state).isEqualTo(ChoreoState.Playing)
        assertThat(choreography!!.progress).isGreaterThan(0.3f)
        rule.mainClock.advanceTimeBy(1000)
        assertThat(choreography!!.state).isEqualTo(ChoreoState.Finished)
        assertThat(ends).containsExactly(ChoreoEndReason.Finished)
    }

    @Test
    fun autoPlayCanBeDisabled() {
        rule.mainClock.autoAdvance = false
        setContent(autoPlay = false)
        rule.mainClock.advanceTimeBy(500)
        assertThat(choreography!!.state).isEqualTo(ChoreoState.Idle)
        assertThat(choreography!!.progress).isEqualTo(0f)
    }

    @Test
    fun pauseStopsProgress() {
        rule.mainClock.autoAdvance = false
        setContent()
        rule.mainClock.advanceTimeBy(300)
        rule.runOnIdle { choreography!!.pause() }
        val paused = choreography!!.progress
        rule.mainClock.advanceTimeBy(500)
        assertThat(choreography!!.progress).isEqualTo(paused)
        rule.runOnIdle {
            choreography!!.resume()
            androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
        }
        rule.mainClock.advanceTimeBy(300)
        assertThat(choreography!!.progress).isGreaterThan(paused)
    }

    @Test
    fun seekShowsThatPosition() {
        rule.mainClock.autoAdvance = false
        setContent(autoPlay = false)
        rule.runOnIdle { choreography!!.seek(0.6f) }
        rule.mainClock.advanceTimeBy(100)
        assertThat(choreography!!.progress).isWithin(0.001f).of(0.6f)
        assertThat(choreography!!.state).isEqualTo(ChoreoState.Paused)
    }

    @Test
    fun leavingTheCompositionCancels() {
        val ends = mutableListOf<ChoreoEndReason>()
        var show by mutableStateOf(true)
        rule.mainClock.autoAdvance = false
        rule.setContent {
            if (show) {
                val c = rememberChoreography(reducedMotion = ReducedMotionPolicy.AlwaysPlay, onEnd = ends::add) {
                    circle(10f).moveTo(Offset(100f, 0f), durationMillis = 1000)
                }
                choreography = c
                ChoreoCanvas(c, Modifier.size(100.dp))
            }
        }
        rule.mainClock.advanceTimeBy(300)
        rule.runOnIdle { show = false }
        rule.mainClock.advanceTimeBy(100)
        assertThat(choreography!!.state).isEqualTo(ChoreoState.Cancelled)
        assertThat(ends).isEmpty()
    }

    @Test
    fun recompositionDoesNotResetTheAnimation() {
        var counter by mutableIntStateOf(0)
        rule.mainClock.autoAdvance = false
        setContent(extra = { Box(Modifier.size((10 + counter).dp)) })
        rule.mainClock.advanceTimeBy(500)
        val before = choreography!!.progress
        rule.runOnIdle { counter++ }
        rule.mainClock.advanceTimeBy(16)
        assertThat(choreography!!.progress).isAtLeast(before)
    }

    @Test
    fun finalFrameIsShownWhenThePolicySaysSo() {
        rule.mainClock.autoAdvance = false
        setContent(policy = ReducedMotionPolicy.AlwaysFinalFrame)
        rule.mainClock.advanceTimeBy(16)
        assertThat(choreography!!.state).isEqualTo(ChoreoState.Finished)
        assertThat(choreography!!.progress).isEqualTo(1f)
    }

    @Test
    fun finalFrameIsShownWhenTheSystemAnimationScaleIsZero() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        try {
            rule.mainClock.autoAdvance = false
            setContent(policy = ReducedMotionPolicy.FollowSystem)
            rule.mainClock.advanceTimeBy(16)
            assertThat(choreography!!.state).isEqualTo(ChoreoState.Finished)
        } finally {
            Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        }
    }

    @Test
    fun canvasExposesItsContentDescription() {
        setContent()
        rule.onNodeWithContentDescription("Moving circle").assertContentDescriptionEquals("Moving circle")
    }

    @Test
    fun canvasWithoutDescriptionIsDecorative() {
        rule.setContent {
            val c = rememberChoreography(autoPlay = false) { circle(10f) }
            ChoreoCanvas(c, Modifier.size(100.dp))
        }
        rule.onAllNodesWithContentDescription("Moving circle").assertCountEquals(0)
    }
}
