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

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.PathParser
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star

/** How a shape is painted. */
public enum class PaintMode {
    /** Fill the inside of the shape. */
    Fill,

    /** Draw only the outline. */
    Stroke,

    /** Fill the inside and draw the outline. */
    FillAndStroke,
}

/**
 * Visual style of a shape.
 *
 * @property color Fill color (and stroke color unless [strokeColor] is set). This is the value
 * animated by `colorTo`.
 * @property mode Whether to fill, stroke or both.
 * @property strokeWidth Outline thickness in the choreography unit; must be `>= 0`.
 * @property strokeColor Outline color; [Color.Unspecified] means "same as [color]".
 * @property alpha Initial opacity in `0f..1f`, animated by `fadeTo`.
 * @property cap Shape of line ends.
 * @property join Shape of corners.
 */
public class ShapeStyle(
    public val color: Color = Color.Black,
    public val mode: PaintMode = PaintMode.Fill,
    public val strokeWidth: Float = 2f,
    public val strokeColor: Color = Color.Unspecified,
    public val alpha: Float = 1f,
    public val cap: StrokeCap = StrokeCap.Round,
    public val join: StrokeJoin = StrokeJoin.Round,
) {
    init {
        require(strokeWidth.isFinite() && strokeWidth >= 0f) { "strokeWidth must be >= 0 but was $strokeWidth" }
        require(alpha in 0f..1f) { "alpha must be within 0..1 but was $alpha" }
    }
}

/** A shape's geometry centred on the origin, plus where it starts and how it looks. */
internal class ShapeSpec(val path: Path, val style: ShapeStyle, val x: Float, val y: Float)

private val UpRotation = Matrix().apply { rotateZ(-90f) }

internal fun circlePath(radius: Float): Path {
    require(radius.isFinite() && radius > 0f) { "circle radius must be > 0 but was $radius" }
    return Path().apply { addOval(Rect(Offset.Zero, radius)) }
}

internal fun rectanglePath(size: Size): Path {
    requireValidSize("rectangle", size)
    return Path().apply { addRect(Rect(Offset.Zero, size).centered()) }
}

internal fun roundedRectanglePath(size: Size, cornerRadius: Float): Path {
    requireValidSize("roundedRectangle", size)
    require(cornerRadius.isFinite() && cornerRadius >= 0f) { "cornerRadius must be >= 0 but was $cornerRadius" }
    return Path().apply { addRoundRect(RoundRect(Rect(Offset.Zero, size).centered(), CornerRadius(cornerRadius))) }
}

internal fun linePath(start: Offset, end: Offset): Path {
    require(start.isFinite() && end.isFinite()) { "line endpoints must be finite but were $start and $end" }
    val middle = Offset((start.x + end.x) / 2f, (start.y + end.y) / 2f)
    return Path().apply {
        moveTo(start.x - middle.x, start.y - middle.y)
        lineTo(end.x - middle.x, end.y - middle.y)
    }
}

internal fun polygonPath(sides: Int, radius: Float): Path {
    require(sides >= 3) { "polygon needs at least 3 sides but had $sides" }
    require(radius.isFinite() && radius > 0f) { "polygon radius must be > 0 but was $radius" }
    return RoundedPolygon(numVertices = sides, radius = radius).toComposePath()
}

internal fun starPath(points: Int, outerRadius: Float, innerRadius: Float): Path {
    require(points >= 3) { "star needs at least 3 points but had $points" }
    require(outerRadius.isFinite() && outerRadius > 0f) { "star outerRadius must be > 0 but was $outerRadius" }
    require(innerRadius.isFinite() && innerRadius > 0f && innerRadius < outerRadius) {
        "star innerRadius must be > 0 and < outerRadius ($outerRadius) but was $innerRadius"
    }
    return RoundedPolygon.star(numVerticesPerRadius = points, radius = outerRadius, innerRadius = innerRadius)
        .toComposePath()
}

internal fun parseSvgPath(pathData: String): Path {
    require(pathData.isNotBlank()) { "svgPath data must not be blank" }
    return try {
        PathParser().parsePathString(pathData).toPath()
    } catch (e: RuntimeException) {
        throw IllegalArgumentException("svgPath data is not valid SVG path data: \"$pathData\"", e)
    }
}

/** Moves [source] so its bounds are centred on the origin and returns that path with its original centre. */
internal fun recentered(source: Path): Pair<Path, Offset> {
    require(!source.isEmpty) { "path must contain at least one segment" }
    val copy = Path().apply { addPath(source) }
    val center = copy.getBounds().center
    copy.translate(-center)
    return copy to center
}

private fun RoundedPolygon.toComposePath(): Path {
    val path = Path()
    val cubics = cubics
    path.moveTo(cubics.first().anchor0X, cubics.first().anchor0Y)
    cubics.forEach { path.cubicTo(it.control0X, it.control0Y, it.control1X, it.control1Y, it.anchor1X, it.anchor1Y) }
    path.close()
    path.transform(UpRotation)
    return path
}

private fun Rect.centered(): Rect = translate(-center)

private fun Offset.isFinite(): Boolean = x.isFinite() && y.isFinite()

private fun requireValidSize(name: String, size: Size) {
    require(size.width.isFinite() && size.height.isFinite() && size.width > 0f && size.height > 0f) {
        "$name size must be positive but was $size"
    }
}
