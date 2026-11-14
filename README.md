# Choreo

Draw shapes. Choreograph motion. One timeline, pure Compose.

Choreo is an open-source library for Jetpack Compose that draws shapes and animates them with a small
DSL. Everything on screen is computed from one deterministic timeline, so you can play, pause, seek and
restart it, and it behaves the same on every run.


> TODO: GIFs are not recorded yet. Planned files (see the design hand-off):
> `docs/media/hero.png` (1280x640), `docs/media/hero.gif`, `docs/media/sequence-vs-parallel.gif`,
> `docs/media/playground-seek-easing.gif`, `docs/media/reduced-motion.gif`.

## Installation

Add JitPack and the dependency:

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

// build.gradle.kts of your module
dependencies {
    implementation("com.github.tugakca.choreo:choreo:v0.1.0")
}
```

## Quick start

```kotlin
@Composable
fun Hello() {
    val choreography = rememberChoreography {
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

    ChoreoCanvas(
        choreography,
        Modifier.size(240.dp, 120.dp),
        contentDescription = "A circle moves to a star, which spins and fades out",
    )
}
```

Coordinates, sizes and stroke widths are in dp by default (`unit = ChoreoUnit.Px` for pixels), with the
origin at the top-left of the canvas. A shape's position is its center, which is also the pivot of
scaling and rotation.

## API overview

| Piece | What it does |
|---|---|
| `rememberChoreography(keys…, autoPlay = true, unit = Dp, reducedMotion, onEnd) { … }` | Builds and remembers a `Choreography`. The block runs once; it runs again only when a key changes. |
| `ChoreoCanvas(choreography, modifier, contentDescription)` | Draws it. Frames are drawn without recomposition. |
| `Choreography` | `play()`, `pause()`, `resume()`, `cancel()`, `restart()`, `seek(0f..1f)`, plus `state`, `progress`, `positionMillis`, `durationMillis`. |
| Shapes | `circle`, `rectangle`, `roundedRectangle`, `line`, `polygon`, `star`, `path(Path)`, `svgPath("M…")` with a `ShapeStyle` (fill, stroke or both, color, width, alpha, cap, join). |
| Animations | `moveTo`, `scaleTo`, `rotateTo`, `fadeTo`, `colorTo`, `drawOn`, `eraseOff`, `drawRange` (experimental). Each takes `durationMillis`, `easing`, `delayMillis`, `iterations` (`ChoreoDefaults.INFINITE` for forever). |
| Structure | `sequence { }`, `parallel { }` (nestable), `wait(ms)`. The top level of the block is a sequence. |
| `ChoreoState` / `ChoreoEndReason` | `Idle`, `Playing`, `Paused`, `Finished`, `Cancelled`; `onEnd` receives `Finished` or `Cancelled`. |

Rules worth knowing:

- A duration of `0` applies instantly; a negative duration throws `IllegalArgumentException` with a
  message that names the animation.
- **Interruption:** when an animation begins on a property that another animation is still changing, it
  starts from the current value and takes over. The value never jumps.
- `drawOn` and `eraseOff` hide the shape before they start / after they end. Partial drawing traces only
  the first sub-path of a path that has several.
- An infinitely repeating animation counts as one iteration when the timeline length is computed;
  `Choreography.isInfinite` tells you the timeline never finishes by itself.
- The animation is cancelled when it leaves the composition, is not reset by recomposition, keeps its
  position across screen rotation, and its clock stops while the app is in the background.
- Experimental APIs are marked with `@ExperimentalChoreoApi`. The public API is tracked in
  `choreo/api/choreo.api` and checked in CI.

## Reduced motion

Choreo respects the system animator duration scale (Developer options, "Animator duration scale", and the
"Remove animations" accessibility setting). The behavior is chosen per choreography, or for a whole
subtree with `LocalReducedMotionPolicy`:

| `ReducedMotionPolicy` | Behavior |
|---|---|
| `FollowSystem` (default) | Scale 0 shows the final frame instead of playing; other scales speed up or slow down time. For infinite animations the final frame is the end of one iteration. |
| `AlwaysPlay` | Ignores the system setting. |
| `AlwaysFinalFrame` | Never plays; shows the final frame. |

`ChoreoCanvas` takes a `contentDescription`. Leave it out for decorative drawings and the canvas is hidden
from accessibility services.

## Compatibility

| | Version |
|---|---|
| minSdk | 23 |
| compileSdk (needed by consumers) | 37 |
| Android Gradle Plugin | 9.5.1 |
| Kotlin | 2.4.21 |
| Compose BOM | 2026.09.00 |
| Gradle | 9.9.0 |
| JDK | 17 |

Runtime dependencies are only AndroidX Compose (animation-core, ui, foundation),
`androidx.graphics:graphics-shapes` and `androidx.lifecycle:lifecycle-runtime-compose` (already pulled in
by Compose). The AAR is compiled against compileSdk 37, so consuming projects need an Android Gradle Plugin
that supports it.

## Sample app

`:sample` has four tabs: Gallery (every shape and animation), Sequences (sequence, parallel, nested and a
logo, with a live timeline), Playground (change duration, delay, easing and repeat and copy the code) and
Motion (system animation scale and the reduced-motion policy). Run it with
`./gradlew :sample:installDebug`.

## Building

```
./gradlew spotlessCheck lint testDebugUnitTest apiCheck assembleDebug
```

After an intentional public API change run `./gradlew apiDump` and commit the updated
`choreo/api/choreo.api`.

## Privacy

Choreo asks for no permissions and collects no data.

## License

Apache License 2.0, see [LICENSE](LICENSE).
