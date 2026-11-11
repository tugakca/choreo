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

@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalChoreoApi::class)

package io.github.tugakca.choreo.sample

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.tugakca.choreo.ExperimentalChoreoApi
import io.github.tugakca.choreo.LocalReducedMotionPolicy
import io.github.tugakca.choreo.ReducedMotionPolicy

enum class Destination(val label: String, val icon: ImageVector) {
    Gallery("Gallery", SampleIcons.Gallery),
    Sequences("Sequences", SampleIcons.Sequences),
    Playground("Playground", SampleIcons.Playground),
    Motion("Motion", SampleIcons.Motion),
}

@Composable
fun SampleApp() {
    var destination by rememberSaveable { mutableStateOf(Destination.Gallery) }
    var pausedAll by rememberSaveable { mutableStateOf(false) }
    var policy by rememberSaveable { mutableStateOf(ReducedMotionPolicy.FollowSystem) }
    val playground = rememberSaveable(saver = PlaygroundState.Saver) { PlaygroundState() }
    val snackbar = remember { SnackbarHostState() }
    val colors = DemoColors(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)

    CompositionLocalProvider(LocalReducedMotionPolicy provides policy) {
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                Destination.entries.forEach {
                    item(
                        selected = it == destination,
                        onClick = { destination = it },
                        icon = { Icon(it.icon, contentDescription = null) },
                        label = { Text(it.label, maxLines = 1) },
                        alwaysShowLabel = true,
                    )
                }
            },
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(destination.label) },
                        actions = {
                            if (destination != Destination.Motion) {
                                IconButton(onClick = { pausedAll = !pausedAll }) {
                                    Icon(
                                        if (pausedAll) SampleIcons.PlayCircle else SampleIcons.PauseCircle,
                                        contentDescription = pauseAllLabel(pausedAll),
                                    )
                                }
                            }
                        },
                    )
                },
                snackbarHost = { SnackbarHost(snackbar) },
            ) { padding ->
                AnimatedContent(
                    targetState = destination,
                    modifier = Modifier,
                    transitionSpec = {
                        // Fade through: the old screen leaves quickly, then the new one arrives.
                        fadeIn(tween(210, delayMillis = 90)) togetherWith fadeOut(tween(90))
                    },
                    label = "destination",
                ) { target ->
                    when (target) {
                        Destination.Gallery -> GalleryScreen(
                            pausedAll = pausedAll,
                            colors = colors,
                            contentPadding = padding,
                            onOpen = { shape, animation ->
                                playground.shape = shape
                                playground.animation = animation
                                destination = Destination.Playground
                            },
                        )

                        Destination.Sequences -> SequencesScreen(pausedAll, colors, padding)

                        Destination.Playground -> PlaygroundScreen(playground, pausedAll, colors, snackbar, padding)

                        Destination.Motion -> MotionScreen(policy, { policy = it }, colors, padding)
                    }
                }
            }
        }
    }
}

private fun pauseAllLabel(pausedAll: Boolean) = if (pausedAll) "Play all animations" else "Pause all animations"
