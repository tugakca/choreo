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

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** How a choreography reacts to the user's "reduce animations" setting. */
public enum class ReducedMotionPolicy {
    /**
     * Follow the system animator duration scale (default): at 0 the final frame is shown instead of
     * playing, otherwise time runs at `1 / scale`.
     */
    FollowSystem,

    /** Always play at normal speed, ignoring the system setting. */
    AlwaysPlay,

    /** Never play; jump straight to the final frame. */
    AlwaysFinalFrame,
}

/**
 * Policy used by [rememberChoreography] when none is passed explicitly. Provide a value to change it
 * for a whole part of the UI, for example from an app-level motion setting.
 */
@ExperimentalChoreoApi
public val LocalReducedMotionPolicy: ProvidableCompositionLocal<ReducedMotionPolicy> =
    compositionLocalOf { ReducedMotionPolicy.FollowSystem }

/**
 * The system animator duration scale (Developer options, "Animator duration scale"), updated live.
 * `0f` means the user turned animations off.
 */
@ExperimentalChoreoApi
@Composable
public fun rememberSystemAnimationScale(): State<Float> {
    val context = LocalContext.current
    val scale = remember { mutableFloatStateOf(readAnimationScale(context)) }
    DisposableEffect(context) {
        val resolver = context.contentResolver
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                scale.floatValue = readAnimationScale(context)
            }
        }
        resolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            observer,
        )
        scale.floatValue = readAnimationScale(context)
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return scale
}

private fun readAnimationScale(context: Context): Float =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
