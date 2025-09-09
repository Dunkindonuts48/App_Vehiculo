package com.example.autocare.charts

import android.annotation.SuppressLint
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.text.font.FontWeight
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min

data class MaintenanceItem(
    val epochMillis: Long,
    val cost: Float,
    val category: String
)

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

private fun monthsRange(from: YearMonth, to: YearMonth): List<YearMonth> {
    val out = mutableListOf<YearMonth>()
    var cur = from
    while (!cur.isAfter(to)) { out += cur; cur = cur.plusMonths(1) }
    return out
}

private fun itemsByYearMonth(items: List<MaintenanceItem>): Map<YearMonth, Float> {
    val map = linkedMapOf<YearMonth, Float>()
    items.forEach { i ->
        val d = i.epochMillis.toLocalDate()
        val ym = YearMonth.of(d.year, d.month)
        map[ym] = (map[ym] ?: 0f) + i.cost
    }
    return map.toSortedMap()
}

private fun availableYears(items: List<MaintenanceItem>) =
    items.map { it.epochMillis.toLocalDate().year }.toSet().sorted()

private fun niceCeil(maxVal: Float): Float {
    if (maxVal <= 0f) return 1f
    val exp = floor(log10(maxVal.toDouble())).toInt()
    val base = 10.0.pow(exp).toFloat()
    val candidates = floatArrayOf(1f, 2f, 5f, 10f)
    val scaled = maxVal / base
    val chosen = candidates.firstOrNull { scaled <= it } ?: 10f
    return chosen * base
}

private fun labelMMM(ym: YearMonth, locale: Locale = Locale.getDefault()) =
    DateTimeFormatter.ofPattern("LLL", locale)
        .format(ym.atDay(1))
        .replaceFirstChar { it.uppercase(locale) }

private fun labelMMMYY(ym: YearMonth, locale: Locale = Locale.getDefault()) =
    DateTimeFormatter.ofPattern("LLL yy", locale)
        .format(ym.atDay(1))
        .replaceFirstChar { it.uppercase(locale) }

sealed interface MaintScopeOption {
    data class Year(val year: Int): MaintScopeOption
    data object SinceStart: MaintScopeOption
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceCostOverTimeCard(
    items: List<MaintenanceItem>?,
    modifier: Modifier = Modifier,
    title: String = "Gasto de mantenimiento"
) {
    val dataAll = items ?: emptyList()

    val years = remember(dataAll) { availableYears(dataAll) }
    var expandedPeriod by remember { mutableStateOf(false) }
    var selectedPeriod by remember(years) {
        mutableStateOf<MaintScopeOption>(
            years.lastOrNull()?.let { MaintScopeOption.Year(it) } ?: MaintScopeOption.SinceStart
        )
    }

    val categories = remember(dataAll) {
        buildList {
            add("Todas")
            addAll(dataAll.map { it.category.ifBlank { "Sin categoría" } }.toSet().sorted())
        }
    }
    var expandedCat by remember { mutableStateOf(false) }
    var selectedCat by remember(categories) { mutableStateOf(categories.first()) }

    val filtered = remember(dataAll, selectedPeriod, selectedCat) {
        val base = when (val s = selectedPeriod) {
            is MaintScopeOption.Year -> dataAll.filter { it.epochMillis.toLocalDate().year == s.year }
            MaintScopeOption.SinceStart -> dataAll
        }
        if (selectedCat == "Todas") base
        else base.filter { (it.category.ifBlank { "Sin categoría" }) == selectedCat }
    }

    val byYm = remember(filtered) {
        val map = itemsByYearMonth(filtered)
        val (start, end) = if (map.isEmpty()) YearMonth.now() to YearMonth.now()
        else map.keys.first() to map.keys.last()
        val full = monthsRange(start, end)
        full.map { ym -> ym to (map[ym] ?: 0f) }
    }
    val showYearLabels = selectedPeriod is MaintScopeOption.SinceStart

    Card(modifier, shape = RoundedCornerShape(12.dp)) {
        Column(
            Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(14.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(10.dp))

            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val compact = maxWidth < 420.dp
                if (compact) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        PeriodFilterDropdown(
                            expanded = expandedPeriod,
                            onExpandedChange = { expandedPeriod = it },
                            selected = when (val s = selectedPeriod) {
                                is MaintScopeOption.Year -> s.year.toString()
                                MaintScopeOption.SinceStart -> "Desde el inicio"
                            },
                            years = years,
                            onPickSinceStart = { selectedPeriod = MaintScopeOption.SinceStart },
                            onPickYear = { y -> selectedPeriod = MaintScopeOption.Year(y) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        CategoryFilterDropdown(
                            expanded = expandedCat,
                            onExpandedChange = { expandedCat = it },
                            value = selectedCat,
                            options = categories,
                            onPick = { selectedCat = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        PeriodFilterDropdown(
                            expanded = expandedPeriod,
                            onExpandedChange = { expandedPeriod = it },
                            selected = when (val s = selectedPeriod) {
                                is MaintScopeOption.Year -> s.year.toString()
                                MaintScopeOption.SinceStart -> "Desde el inicio"
                            },
                            years = years,
                            onPickSinceStart = { selectedPeriod = MaintScopeOption.SinceStart },
                            onPickYear = { y -> selectedPeriod = MaintScopeOption.Year(y) },
                            modifier = Modifier.weight(1f)
                        )
                        CategoryFilterDropdown(
                            expanded = expandedCat,
                            onExpandedChange = { expandedCat = it },
                            value = selectedCat,
                            options = categories,
                            onPick = { selectedCat = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            MaintenanceBarChart(
                series = byYm,
                height = 240.dp,
                contentPadding = PaddingValues(start = 44.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
                barColor = MaterialTheme.colorScheme.primary,
                axisColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                showYearOnXAxis = showYearLabels
            )
        }
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceByCategoryCard(
    items: List<MaintenanceItem>?,
    modifier: Modifier = Modifier,
    title: String = "Gasto por categoría"
) {
    val dataAll = items ?: emptyList()
    val categories = remember(dataAll) {
        buildList {
            add("Todas")
            addAll(dataAll.map { it.category.ifBlank { "Sin categoría" } }.toSet().sorted())
        }
    }
    var expandedCat by remember { mutableStateOf(false) }
    var selectedCat by remember(categories) { mutableStateOf(categories.first()) }

    val data = remember(dataAll, selectedCat) {
        if (selectedCat == "Todas") dataAll
        else dataAll.filter { (it.category.ifBlank { "Sin categoría" }) == selectedCat }
    }

    val totals = remember(data) {
        data.groupBy { it.category.ifBlank { "Sin categoría" } }
            .mapValues { (_, l) -> l.sumOf { it.cost.toDouble() }.toFloat() }
            .toList()
            .sortedByDescending { it.second }
    }

    Card(modifier, shape = RoundedCornerShape(12.dp)) {
        Column(
            Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(14.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(10.dp))

            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val compact = maxWidth < 420.dp
                CategoryFilterDropdown(
                    expanded = expandedCat,
                    onExpandedChange = { expandedCat = it },
                    value = selectedCat,
                    options = categories,
                    onPick = { selectedCat = it },
                    modifier = if (compact) Modifier.fillMaxWidth() else Modifier.widthIn(min = 220.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DonutChart(
                    data = totals,
                    modifier = Modifier
                        .weight(1f)
                        .height(220.dp)
                )
                Spacer(Modifier.width(12.dp))
                CategoryLegend(
                    data = totals,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun DonutChart(
    data: List<Pair<String, Float>>,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 28.dp
) {
    val density = LocalDensity.current
    val currency = remember { NumberFormat.getCurrencyInstance() }
    val total = remember(data) { data.sumOf { it.second.toDouble() }.toFloat() }

    val colorScheme = MaterialTheme.colorScheme
    val palette = remember(colorScheme) {
        listOf(
            colorScheme.primary,
            colorScheme.secondary,
            colorScheme.tertiary,
            colorScheme.primaryContainer,
            colorScheme.secondaryContainer,
            colorScheme.tertiaryContainer,
        )
    }
    val onSurfaceVariant = colorScheme.onSurfaceVariant

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val strokeWidthPx = with(density) { strokeWidth.toPx() }

    Box(
        modifier
            .pointerInput(data, canvasSize) {
                detectTapGestures { tap ->
                    if (total <= 0f || data.isEmpty() || canvasSize == IntSize.Zero) {
                        selectedIndex = null
                        return@detectTapGestures
                    }
                    val minSide = min(canvasSize.width, canvasSize.height).toFloat()
                    val baseRadius = minSide / 2f * 0.8f
                    val center = Offset(canvasSize.width / 2f, canvasSize.height / 2f)
                    val dx = tap.x - center.x
                    val dy = tap.y - center.y
                    val dist = hypot(dx, dy)

                    val inner = baseRadius - strokeWidthPx / 2f
                    val outer = baseRadius + strokeWidthPx / 2f
                    if (dist < inner || dist > outer) {
                        selectedIndex = null
                        return@detectTapGestures
                    }

                    val tapAngle = ((Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())) + 360.0) % 360.0).toFloat()
                    val start0 = -90f
                    val rel = ((tapAngle - start0 + 360f) % 360f)

                    var acc = 0f
                    var found: Int? = null
                    data.forEachIndexed { idx, (_, value) ->
                        val sweep = if (total > 0f) (value / total) * 360f else 0f
                        if (rel >= acc && rel < acc + sweep) {
                            found = idx
                            return@forEachIndexed
                        }
                        acc += sweep
                    }
                    selectedIndex = if (selectedIndex == found) null else found
                }
            }
            .onSizeChanged { canvasSize = it }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            if (total <= 0f || data.isEmpty()) return@Canvas

            val sizeMin = size.minDimension
            val baseRadius = sizeMin / 2f * 0.8f
            val center = Offset(size.width / 2f, size.height / 2f)

            var start = -90f
            data.forEachIndexed { idx, (_, value) ->
                val sweep = (value / total) * 360f
                if (sweep <= 0f) return@forEachIndexed

                val isSel = selectedIndex == idx
                val explode = if (isSel) strokeWidthPx * 0.6f else 0f
                val ringWidth = if (isSel) strokeWidthPx * 1.25f else strokeWidthPx
                val rect = arcRect(center = center, radius = baseRadius + explode)

                drawArc(
                    color = palette[idx % palette.size],
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(rect.left, rect.top),
                    size = Size(rect.width, rect.height),
                    style = Stroke(width = ringWidth)
                )
                start += sweep
            }

            val textPaint = Paint().apply {
                val c = onSurfaceVariant
                color = android.graphics.Color.argb(
                    (c.alpha * 255).toInt(),
                    (c.red * 255).toInt(),
                    (c.green * 255).toInt(),
                    (c.blue * 255).toInt()
                )
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }

            if (selectedIndex != null) {
                val (label, value) = data[selectedIndex!!]
                val pct = if (total > 0f) (value / total * 100f) else 0f
                textPaint.textSize = with(density) { 13.sp.toPx() }
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    center.x,
                    center.y - with(density) { 8.dp.toPx() },
                    textPaint
                )
                textPaint.textSize = with(density) { 14.sp.toPx() }
                drawContext.canvas.nativeCanvas.drawText(
                    "${currency.format(value.toDouble())} · ${pct.toInt()}%",
                    center.x,
                    center.y + with(density) { 12.dp.toPx() },
                    textPaint
                )
            } else {
                textPaint.textSize = with(density) { 13.sp.toPx() }
                drawContext.canvas.nativeCanvas.drawText(
                    currency.format(total.toDouble()),
                    center.x,
                    center.y + with(density) { 5.dp.toPx() },
                    textPaint
                )
            }
        }
    }
}
@Composable
private fun CategoryLegend(
    data: List<Pair<String, Float>>,
    modifier: Modifier = Modifier
) {
    val currency = remember { NumberFormat.getCurrencyInstance() }
    val total = remember(data) { data.sumOf { it.second.toDouble() }.toFloat() }

    val colorScheme = MaterialTheme.colorScheme
    val palette = remember(colorScheme) {
        listOf(
            colorScheme.primary,
            colorScheme.secondary,
            colorScheme.tertiary,
            colorScheme.primaryContainer,
            colorScheme.secondaryContainer,
            colorScheme.tertiaryContainer,
        )
    }

    Column(modifier) {
        data.forEachIndexed { idx, (label, value) ->
            val color = palette[idx % palette.size]
            val pct = if (total > 0f) (value / total * 100f) else 0f
            Row(
                Modifier.padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(14.dp)
                        .background(color, RoundedCornerShape(3.dp))
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(label, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "${currency.format(value.toDouble())} · ${pct.toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (data.isEmpty()) {
            Text("Sin datos", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun MaintenanceBarChart(
    series: List<Pair<YearMonth, Float>>,
    modifier: Modifier = Modifier,
    height: Dp = 240.dp,
    contentPadding: PaddingValues = PaddingValues(start = 44.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
    barColor: Color = MaterialTheme.colorScheme.primary,
    axisColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    showYearOnXAxis: Boolean = false
) {
    val density = LocalDensity.current
    val padStart = with(density) { contentPadding.calculateLeftPadding(LayoutDirection.Ltr).toPx() }
    val padEnd   = with(density) { contentPadding.calculateRightPadding(LayoutDirection.Ltr).toPx() }
    val padTop   = with(density) { contentPadding.calculateTopPadding().toPx() }
    val padBot   = with(density) { contentPadding.calculateBottomPadding().toPx() }

    val maxVal = remember(series) { niceCeil(series.maxOfOrNull { it.second } ?: 0f) }
    val stepsY = 4
    val currency = remember { NumberFormat.getCurrencyInstance() }

    val labelsPaint = remember(labelColor, density) {
        Paint().apply {
            color = android.graphics.Color.argb(
                (labelColor.alpha * 255).toInt(),
                (labelColor.red * 255).toInt(),
                (labelColor.green * 255).toInt(),
                (labelColor.blue * 255).toInt()
            )
            textSize = with(density) { 11.sp.toPx() }
            isAntiAlias = true
        }
    }

    Canvas(
        modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val chartW = size.width - padStart - padEnd
        val chartH = size.height - padTop - padBot
        val origin = Offset(padStart, size.height - padBot)

        drawLine(
            color = axisColor,
            start = origin,
            end = Offset(padStart + chartW, origin.y),
            strokeWidth = 1f
        )
        drawLine(
            color = axisColor,
            start = origin,
            end = Offset(origin.x, origin.y - chartH),
            strokeWidth = 1f
        )

        repeat(stepsY + 1) { i ->
            val yVal = i * (maxVal / stepsY)
            val y = origin.y - (yVal / maxVal) * chartH
            drawLine(
                color = axisColor.copy(alpha = 0.25f),
                start = Offset(padStart, y),
                end = Offset(padStart + chartW, y),
                strokeWidth = 1f
            )
            drawContext.canvas.nativeCanvas.drawText(
                currency.format(yVal.toDouble()),
                padStart - with(density) { 6.dp.toPx() },
                y + with(density) { 4.dp.toPx() },
                labelsPaint.apply { textAlign = Paint.Align.RIGHT }
            )
        }

        if (series.isEmpty()) return@Canvas

        val n = series.size.coerceAtLeast(1)
        val gap = chartW * 0.12f / n
        val barW = (chartW - gap * (n + 1)) / n

        series.forEachIndexed { index, (ym, value) ->
            val x = padStart + gap + index * (barW + gap)
            val h = (value / maxVal).coerceIn(0f, 1f) * chartH
            drawRect(
                color = barColor,
                topLeft = Offset(x, origin.y - h),
                size = Size(barW, h)
            )

            val lbl = if (showYearOnXAxis && (ym.monthValue == 1 || index == 0)) {
                labelMMMYY(ym)
            } else {
                labelMMM(ym)
            }
            drawContext.canvas.nativeCanvas.drawText(
                lbl,
                x + barW / 2f,
                origin.y + with(density) { 14.dp.toPx() },
                labelsPaint.apply { textAlign = Paint.Align.CENTER }
            )
        }
    }
}

private fun arcRect(center: Offset, radius: Float): Rect {
    val left = center.x - radius
    val top = center.y - radius
    return Rect(left, top, left + 2 * radius, top + 2 * radius)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodFilterDropdown(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    selected: String,
    years: List<Int>,
    onPickSinceStart: () -> Unit,
    onPickYear: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = onExpandedChange) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            modifier = modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
            label = { Text("Periodo") }
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            DropdownMenuItem(text = { Text("Desde el inicio") }, onClick = {
                onPickSinceStart(); onExpandedChange(false)
            })
            years.forEach { y ->
                DropdownMenuItem(text = { Text(y.toString()) }, onClick = {
                    onPickYear(y); onExpandedChange(false)
                })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryFilterDropdown(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    value: String,
    options: List<String>,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = onExpandedChange) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            modifier = modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
            label = { Text("Categoría") }
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            options.forEach { cat ->
                DropdownMenuItem(text = { Text(cat) }, onClick = {
                    onPick(cat); onExpandedChange(false)
                })
            }
        }
    }
}