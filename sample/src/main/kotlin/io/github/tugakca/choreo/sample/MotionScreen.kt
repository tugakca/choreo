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

@file:OptIn(ExperimentalChoreoApi::class)

package io.github.tugakca.choreo.sample

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.tugakca.choreo.ChoreoCanvas
import io.github.tugakca.choreo.ChoreoState
import io.github.tugakca.choreo.ExperimentalChoreoApi
import io.github.tugakca.choreo.PaintMode
import io.github.tugakca.choreo.ReducedMotionPolicy
import io.github.tugakca.choreo.rememberChoreography
import io.github.tugakca.choreo.rememberSystemAnimationScale

private val PolicyOptions = listOf(
    Triple(ReducedMotionPolicy.FollowSystem, "Follow system", "Default"),
    Triple(ReducedMotionPolicy.AlwaysPlay, "Always play", null),
    Triple(ReducedMotionPolicy.AlwaysFinalFrame, "Always show final frame", null),
)

@Composable
fun MotionScreen(
    policy: ReducedMotionPolicy,
    onPolicyChange: (ReducedMotionPolicy) -> Unit,
    colors: DemoColors,
    contentPadding: PaddingValues,
) {
    val scale by rememberSystemAnimationScale()
    val animationsOff = scale == 0f
    val context = LocalContext.current

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = contentPadding.calculateTopPadding())
            .padding(horizontal = 16.dp)
            .padding(bottom = contentPadding.calculateBottomPadding() + 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    if (animationsOff) SampleIcons.Motion else SampleIcons.Play,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Column {
                    Text(
                        if (animationsOff) "System: animations off" else "System: animations on",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        if (animationsOff) {
                            "Animator duration scale is 0x. Choreo shows the final frame."
                        } else {
                            "Animator duration scale is ${"%.1f".format(
                                scale,
                            ).trimEnd('0').trimEnd('.')}x. Animations are enabled."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
        Text("Reduced-motion policy", style = MaterialTheme.typography.labelLarge)
        Column(Modifier.selectableGroup()) {
            PolicyOptions.forEach { (option, label, hint) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .selectable(selected = policy == option, onClick = {
                            onPolicyChange(option)
                        }, role = Role.RadioButton),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RadioButton(selected = policy == option, onClick = null)
                    Column {
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                        if (hint !=
                            null
                        ) {
                            Text(
                                hint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MotionPreview("Normal", "Plays, pausable", ReducedMotionPolicy.AlwaysPlay, colors, Modifier.weight(1f))
            MotionPreview(
                "Reduced",
                "Final frame, no motion",
                ReducedMotionPolicy.AlwaysFinalFrame,
                colors,
                Modifier.weight(1f),
            )
        }
        FilledTonalButton(onClick = {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS,
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(intent)
            } catch (_: ActivityNotFoundException) {
                context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }) { Text("Open system settings") }
    }
}

@Composable
private fun MotionPreview(
    title: String,
    caption: String,
    policy: ReducedMotionPolicy,
    colors: DemoColors,
    modifier: Modifier = Modifier,
) {
    val choreography = rememberChoreography(colors, reducedMotion = policy) {
        val star =
            star(
                5,
                34f,
                16f,
                Offset(60f, 60f),
                io.github.tugakca.choreo.ShapeStyle(colors.primary, PaintMode.Stroke, 3f),
            )
        star.drawOn(1500)
    }
    Surface(modifier, shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(8.dp)) {
            Scene(120.dp) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    ChoreoCanvas(choreography, Modifier.size(120.dp), contentDescription = "$title preview")
                    if (policy == ReducedMotionPolicy.AlwaysPlay) {
                        val playing = choreography.state == ChoreoState.Playing
                        androidx.compose.material3.IconButton(
                            onClick = { if (playing) choreography.pause() else choreography.play() },
                        ) {
                            Icon(
                                if (playing) SampleIcons.Pause else SampleIcons.Play,
                                contentDescription = if (playing) "Pause" else "Play",
                            )
                        }
                    }
                }
            }
            Text(title, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
            Text(
                caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
