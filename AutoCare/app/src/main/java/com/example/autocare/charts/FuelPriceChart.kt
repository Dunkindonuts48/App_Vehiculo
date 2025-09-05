package com.example.autocare.charts

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.autocare.vehicle.fuel.FuelEntry
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.*

private data class PricePoint(val ym: YearMonth, val value: Float)

private fun monthsRange(from: YearMonth, to: YearMonth): List<YearMonth> {
    val out = mutableListOf<YearMonth>()
    var cur = from
    while (!cur.isAfter(to)) { out += cur; cur = cur.plusMonths(1) }
    return out
}

private fun labelMMM(ym: YearMonth, locale: Locale = Locale.getDefault()) =
    DateTimeFormatter.ofPattern("LLL", locale).format(ym.atDay(1))
        .replaceFirstChar { it.uppercase(locale) }

private fun labelMMMYY(ym: YearMonth, locale: Locale = Locale.getDefault()) =
    DateTimeFormatter.ofPattern("LLL yy", locale).format(ym.atDay(1))
        .replaceFirstChar { it.uppercase(locale) }

private fun niceCeil(maxVal: Float): Float {
    if (maxVal <= 0f) return 1f
    val exp = floor(log10(maxVal.toDouble())).toInt()
    val base = 10.0.pow(exp).toFloat()
    val candidates = floatArrayOf(1f, 2f, 5f, 10f)
    val scaled = maxVal / base
    val chosen = candidates.firstOrNull { scaled <= it } ?: 10f
    return chosen * base
}

private fun availableYears(entries: List<FuelEntry>) =
    entries.map { it.date.year }.toSet().sorted()

private fun monthlyWeightedAvgPrice(entries: List<FuelEntry>): Map<YearMonth, Float> {
    val acc = mutableMapOf<YearMonth, Pair<Float, Float>>()
    entries.forEach { e ->
        val ym = YearMonth.of(e.date.year, e.date.month)
        val liters = e.liters
        val cost = e.liters * e.pricePerLiter
        val cur = acc[ym]
        if (cur == null) acc[ym] = liters to cost
        else acc[ym] = (cur.first + liters) to (cur.second + cost)
    }
    return acc.toSortedMap().mapValues { (_, lc) ->
        if (lc.first > 0f) lc.second / lc.first else 0f
    }
}

private fun priceSeries(
    entries: List<FuelEntry>,
    sinceStart: Boolean,
    year: Int?
): List<PricePoint> {
    val byYm = monthlyWeightedAvgPrice(entries)
    if (byYm.isEmpty()) return emptyList()

    return if (sinceStart) {
        val all = monthsRange(byYm.keys.first(), byYm.keys.last())
        all.map { ym -> PricePoint(ym, byYm[ym] ?: 0f) }
    } else {
        val y = year ?: return emptyList()
        val start = YearMonth.of(y, Month.JANUARY)
        val end = YearMonth.of(y, Month.DECEMBER)
        monthsRange(start, end).map { ym -> PricePoint(ym, byYm[ym] ?: 0f) }
    }
}

sealed interface FuelPriceScopeOption {
    data class Year(val year: Int): FuelPriceScopeOption
    data object SinceStart: FuelPriceScopeOption
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuelPriceOverTimeCard(
    entries: List<FuelEntry>,
    modifier: Modifier = Modifier,
    title: String = "Precio medio combustible / mes (€/L)"
) {
    val years = remember(entries) { availableYears(entries) }
    var expanded by remember { mutableStateOf(false) }
    var selected by remember(years) {
        mutableStateOf<FuelPriceScopeOption>(
            years.lastOrNull()?.let { FuelPriceScopeOption.Year(it) } ?: FuelPriceScopeOption.SinceStart
        )
    }

    val series = remember(entries, selected) {
        when (selected) {
            is FuelPriceScopeOption.Year -> priceSeries(entries, false, (selected as FuelPriceScopeOption.Year).year)
            FuelPriceScopeOption.SinceStart -> priceSeries(entries, true, null)
        }
    }
    val showYearLabels = selected is FuelPriceScopeOption.SinceStart

    Card(modifier, shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.background(MaterialTheme.colorScheme.surfaceVariant).padding(14.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    val label = when (val s = selected) {
                        is FuelPriceScopeOption.Year -> s.year.toString()
                        FuelPriceScopeOption.SinceStart -> "Desde el inicio"
                    }
                    OutlinedTextField(
                        value = label,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true)
                            .widthIn(min = 150.dp),
                        label = { Text("Periodo") }
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(text = { Text("Desde el inicio") }, onClick = {
                            selected = FuelPriceScopeOption.SinceStart; expanded = false
                        })
                        years.forEach { y ->
                            DropdownMenuItem(text = { Text(y.toString()) }, onClick = {
                                selected = FuelPriceScopeOption.Year(y); expanded = false
                            })
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            FuelPriceLineChart(
                points = series,
                height = 240.dp,
                contentPadding = PaddingValues(start = 44.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
                lineColor = MaterialTheme.colorScheme.primary,
                axisColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                showYearOnXAxis = showYearLabels
            )
        }
    }
}

@Composable
private fun FuelPriceLineChart(
    points: List<PricePoint>,
    modifier: Modifier = Modifier.fillMaxWidth(),
    height: Dp = 220.dp,
    contentPadding: PaddingValues = PaddingValues(32.dp),
    lineColor: Color = MaterialTheme.colorScheme.primary,
    axisColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    showYearOnXAxis: Boolean = false
) {
    val density = LocalDensity.current
    var touchX by remember { mutableStateOf<Float?>(null) }

    val monthLabels = remember(points, showYearOnXAxis) {
        points.map {
            if (showYearOnXAxis && it.ym.monthValue == 1) labelMMMYY(it.ym) else labelMMM(it.ym)
        }
    }
    val maxY = remember(points) { niceCeil(points.maxOfOrNull { it.value } ?: 0f) }
    val crosshairPointColor = MaterialTheme.colorScheme.tertiary
    val tooltipBgColor = MaterialTheme.colorScheme.surface
    val tooltipBorderColor = MaterialTheme.colorScheme.outline

    Box(
        modifier
            .height(height)
            .pointerInput(points) {
                detectTapGestures(
                    onPress = { offset ->
                        touchX = offset.x
                        tryAwaitRelease()
                        touchX = null
                    }
                )
            }
            .pointerInput(points) {
                detectDragGestures(
                    onDragStart = { offset -> touchX = offset.x },
                    onDrag = { change, _ -> touchX = change.position.x },
                    onDragEnd = { touchX = null }
                )
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val leftPad = with(density) { contentPadding.calculateLeftPadding(layoutDirection).toPx() }
            val rightPad = with(density) { contentPadding.calculateRightPadding(layoutDirection).toPx() }
            val topPad = with(density) { contentPadding.calculateTopPadding().toPx() }
            val bottomPad = with(density) { contentPadding.calculateBottomPadding().toPx() }
            val w = size.width - leftPad - rightPad
            val h = size.height - topPad - bottomPad
            val origin = Offset(leftPad, size.height - bottomPad)
            val xEnd = Offset(size.width - rightPad, size.height - bottomPad)
            val yTop = Offset(leftPad, topPad)
            drawLine(axisColor, origin, xEnd, strokeWidth = 2f)
            drawLine(axisColor, origin, yTop, strokeWidth = 2f)
            val yTicks = 5
            val step = maxY / yTicks
            val textPaint = Paint().apply {
                color = android.graphics.Color.argb(
                    (labelColor.alpha * 255).toInt(),
                    (labelColor.red * 255).toInt(),
                    (labelColor.green * 255).toInt(),
                    (labelColor.blue * 255).toInt()
                )
                textSize = with(density) { 11.sp.toPx() }
                isAntiAlias = true
            }
            repeat(yTicks + 1) { i ->
                val value = step * i
                val y = origin.y - (value / maxY) * h
                drawLine(axisColor.copy(alpha = 0.15f), Offset(leftPad, y), Offset(size.width - rightPad, y), 1f)
                drawContext.canvas.nativeCanvas.drawText(
                    "€/${"L"} ${"%.2f".format(value)}",
                    leftPad - with(density) { 8.dp.toPx() },
                    y + with(density) { 4.dp.toPx() },
                    textPaint
                )
            }

            if (points.isEmpty()) return@Canvas

            val n = points.size
            val dx = if (n <= 1) w else w / (n - 1)
            fun xy(idx: Int, v: Float): Offset {
                val x = leftPad + idx * dx
                val y = origin.y - (v / maxY) * h
                return Offset(x, y)
            }

            // line
            val path = Path().apply {
                moveTo(xy(0, points[0].value).x, xy(0, points[0].value).y)
                for (i in 1 until n) lineTo(xy(i, points[i].value).x, xy(i, points[i].value).y)
            }
            drawPath(path, lineColor, style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round))

            for (i in 0 until n) drawCircle(color = lineColor, radius = 5f, center = xy(i, points[i].value))

            val maxLabels = 8
            val stepLabel = max(1, n / maxLabels)
            monthLabels.forEachIndexed { i, label ->
                if (i % stepLabel == 0 || i == n - 1) {
                    val x = leftPad + i * dx
                    val y = origin.y + with(density) { 14.dp.toPx() }
                    drawContext.canvas.nativeCanvas.drawText(label, x, y, textPaint)
                }
            }

            touchX?.let { tx ->
                val idx = ((tx - leftPad) / dx).roundToInt().coerceIn(0, n - 1)
                val p = points[idx]
                val pos = xy(idx, p.value)

                drawLine(
                    color = labelColor.copy(alpha = 0.35f),
                    start = Offset(pos.x, topPad),
                    end = Offset(pos.x, origin.y),
                    strokeWidth = 2f
                )
                drawCircle(color = crosshairPointColor, radius = 7f, center = pos)

                val tooltip = "${labelMMMYY(p.ym)}  •  €/${"L"} ${"%.2f".format(p.value)}"
                val textWidth = textPaint.measureText(tooltip)
                val pad = with(density) { 6.dp.toPx() }
                val boxW = textWidth + pad * 2
                val boxH = with(density) { 22.dp.toPx() }
                val boxX = (pos.x - boxW / 2).coerceIn(leftPad, size.width - rightPad - boxW)
                val boxY = (pos.y - with(density){ 32.dp.toPx() }).coerceAtLeast(topPad + 4f)

                drawRect(color = tooltipBgColor, topLeft = Offset(boxX, boxY),
                    size = Size(boxW, boxH)
                )
                drawRect(color = tooltipBorderColor, topLeft = Offset(boxX, boxY),
                    size = Size(boxW, boxH), style = Stroke(width = 1.5f))
                drawContext.canvas.nativeCanvas.drawText(
                    tooltip, boxX + pad, boxY + boxH - with(density){ 6.dp.toPx() }, textPaint
                )
            }
        }
    }
}