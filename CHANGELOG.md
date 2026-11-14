# Changelog

All notable changes to Choreo are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project follows
[Semantic Versioning](https://semver.org/); while the major version is 0, minor releases may contain
breaking changes.

## [0.1.0] - Unreleased

First release.

### Added
- `rememberChoreography { }` and `ChoreoCanvas`.
- Shapes: circle, rectangle, rounded rectangle, line, polygon, star, Compose `Path`, SVG path data.
- Styles: fill, stroke or both, color, stroke width, alpha, cap and join.
- Animations: `moveTo`, `scaleTo`, `rotateTo`, `fadeTo`, `colorTo`, `drawOn`, `eraseOff` and
  (experimental) `drawRange`, each with duration, easing, delay and repeat (n times or forever).
- `sequence {}` and `parallel {}`, nestable; `wait()`.
- One deterministic timeline: `play`, `pause`, `resume`, `cancel`, `restart`, `seek(0..1)`, `state`,
  `progress` and `onEnd(reason)`.
- Interruption rule: an animation that takes over a property continues from its current value.
- Lifecycle: cancelled on leaving the composition, not reset by recomposition, survives rotation, clock
  stops while the app is in the background.
- Reduced motion: follows the system animator duration scale (0 shows the final frame), with a policy
  to always play or always show the final frame.
- Accessibility: `contentDescription` on `ChoreoCanvas`, or decorative when omitted.
- `explicitApi()`, KDoc on the public API, `@ExperimentalChoreoApi` opt-in, binary API dump checked in CI.
- Sample app with a gallery, sequence demos, a playground and a reduced-motion demo.
