package com.dokushotracker.ui.screens.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dokushotracker.data.model.CumulativeMoji
import com.dokushotracker.data.model.MediaType
import com.dokushotracker.domain.model.MilestoneType
import com.dokushotracker.ui.components.AnimatedCounter
import com.dokushotracker.ui.components.EmptyState
import com.dokushotracker.ui.theme.mediaTypeColor
import com.dokushotracker.util.DateUtils
import com.dokushotracker.util.NumberFormatUtils
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle as DateTextStyle
import java.time.temporal.WeekFields
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    when (val uiState = state) {
        DashboardUiState.Loading -> Box(
            modifier = Modifier.fillMaxSize(),
        )

        is DashboardUiState.Success -> {
            if (uiState.totalBooks == 0) {
                EmptyState(
                    icon = Icons.Filled.BarChart,
                    title = "No reading data yet",
                    subtitle = "Log your first book to get started.",
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        TotalMojiCard(
                            totalMoji = uiState.totalMoji,
                            totalBooks = uiState.totalBooks,
                        )
                    }
                    item { MediaTypeBreakdownCard(uiState = uiState) }
                    item { GoalProgressCard(uiState = uiState) }
                    item { QuickStatsCard(uiState = uiState) }
                    item { LineChartCard(uiState = uiState) }
                    item { MonthlyCard(uiState = uiState) }
                    item { MilestoneCard(uiState = uiState) }
                    item { Spacer(modifier = Modifier.height(90.dp)) }
                }
            }
        }
    }
}

@Composable
private fun TotalMojiCard(
    totalMoji: Long,
    totalBooks: Int,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Total 文字 Read",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            AnimatedCounter(
                targetValue = totalMoji,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp,
                    shadow = Shadow(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        offset = Offset(0f, 0f),
                        blurRadius = 24f,
                    ),
                ),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
            )
            Text(
                text = buildAnnotatedString {
                    append("across ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(NumberFormatUtils.formatInt(totalBooks))
                    }
                    append(" novels")
                },
                modifier = Modifier.padding(top = 4.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun MediaTypeBreakdownCard(uiState: DashboardUiState.Success) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Media Breakdown", style = MaterialTheme.typography.titleMedium)
            MediaType.entries.forEach { mediaType ->
                val count = uiState.mediaTypeCounts.firstOrNull { it.mediaType == mediaType }?.count ?: 0
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(mediaType.displayName)
                    Text(
                        NumberFormatUtils.formatInt(count),
                        color = mediaTypeColor(mediaType),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun GoalProgressCard(uiState: DashboardUiState.Success) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val goalProgress = uiState.goalProgress
            if (goalProgress == null) {
                Text("Set a reading goal to track your progress!")
            } else {
                Text("Reading Goal", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Target: ${NumberFormatUtils.formatLong(goalProgress.targetValue)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                LinearProgressIndicator(
                    progress = { goalProgress.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp),
                )
                Text(
                    "${(goalProgress.progress * 100f).toInt()}% • ${NumberFormatUtils.formatLong(goalProgress.currentValue)} / ${
                        NumberFormatUtils.formatLong(goalProgress.targetValue)
                    }",
                )
                goalProgress.daysRemaining?.let {
                    Text("$it days remaining", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun QuickStatsCard(uiState: DashboardUiState.Success) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Highlights", style = MaterialTheme.typography.titleMedium)
            Text("Average 文字 / Book: ${NumberFormatUtils.formatLong(uiState.averageMojiPerBook)}")
            Text(
                "Last Book: ${
                    uiState.lastEntryDate?.let(DateUtils::relativeDateText) ?: "No entries"
                }",
            )
            val monthLabel = if (uiState.readingStreakMonths == 1) "month" else "months"
            Text("Reading Streak: ${uiState.readingStreakMonths} $monthLabel")
        }
    }
}

@Composable
private fun LineChartCard(uiState: DashboardUiState.Success) {
    var range by remember { mutableStateOf(CumulativeRange.ALL) }
    val filteredData = remember(uiState.cumulativeMojiData, range) {
        filterCumulativeRange(uiState.cumulativeMojiData, range)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Cumulative 文字 Read", style = MaterialTheme.typography.titleMedium)
            RangeSelector(
                options = CumulativeRange.entries.map { it.label },
                selectedLabel = range.label,
                onSelect = { selected -> range = CumulativeRange.entries.first { it.label == selected } },
            )
            if (filteredData.size < 2) {
                Text("More entries are needed to draw this chart.")
                return@Column
            }
            AreaLineChart(
                values = filteredData.map { it.cumulativeMoji },
                lineColor = MaterialTheme.colorScheme.primary,
                fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(DateUtils.formatDate(filteredData.first().date), style = MaterialTheme.typography.labelSmall)
                Text(DateUtils.formatDate(filteredData.last().date), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun MonthlyCard(uiState: DashboardUiState.Success) {
    var range by remember { mutableStateOf(ActivityRange.MONTHS) }
    val activityPoints = remember(uiState.cumulativeMojiData, range) {
        buildActivityPoints(uiState.cumulativeMojiData, range)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Reading Activity", style = MaterialTheme.typography.titleMedium)
            RangeSelector(
                options = ActivityRange.entries.map { it.label },
                selectedLabel = range.label,
                onSelect = { selected -> range = ActivityRange.entries.first { it.label == selected } },
            )
            if (activityPoints.size < 2) {
                Text("Not enough activity data yet.")
                return@Column
            }
            AreaLineChart(
                values = activityPoints.map { it.value },
                lineColor = MaterialTheme.colorScheme.tertiary,
                fillColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp),
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(activityPoints.first().label, style = MaterialTheme.typography.labelSmall)
                Text(activityPoints.last().label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun MilestoneCard(uiState: DashboardUiState.Success) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Milestones", style = MaterialTheme.typography.titleMedium)
            uiState.achievedMilestones.takeLast(3).forEach {
                Text("${it.emoji} ${it.label}")
            }
            uiState.nextMilestone?.let { next ->
                val remaining = when (next.type) {
                    MilestoneType.MOJI -> (next.threshold - uiState.totalMoji).coerceAtLeast(0)
                    MilestoneType.BOOKS -> (next.threshold - uiState.totalBooks).coerceAtLeast(0)
                }
                val unitLabel = if (next.type == MilestoneType.MOJI) "文字" else "books"
                Text("Next: ${next.label} in ${NumberFormatUtils.formatLong(remaining)} $unitLabel")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RangeSelector(
    options: List<String>,
    selectedLabel: String,
    onSelect: (String) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { label ->
            FilterChip(
                selected = selectedLabel == label,
                onClick = { onSelect(label) },
                label = { Text(label) },
            )
        }
    }
}

@Composable
private fun AreaLineChart(
    values: List<Long>,
    lineColor: Color,
    fillColor: Color,
    modifier: Modifier = Modifier,
) {
    if (values.size < 2) return

    Canvas(modifier = modifier) {
        val maxY = values.maxOrNull()?.toFloat()?.coerceAtLeast(1f) ?: 1f
        val xStep = size.width / (values.size - 1).coerceAtLeast(1)

        val linePath = Path()
        val fillPath = Path()

        values.forEachIndexed { index, value ->
            val x = xStep * index
            val y = size.height - (value.toFloat() / maxY) * size.height
            if (index == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, size.height)
                fillPath.lineTo(x, y)
            } else {
                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
            if (index == values.lastIndex) {
                fillPath.lineTo(x, size.height)
                fillPath.close()
            }
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(fillColor, fillColor.copy(alpha = 0.02f)),
                startY = 0f,
                endY = size.height,
            ),
        )
        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(width = 4f, cap = StrokeCap.Round),
        )
    }
}

private enum class CumulativeRange(val label: String) {
    ALL("All"),
    MONTH_6("6M"),
    YEAR_1("1Y"),
    YEAR_3("3Y"),
}

private enum class ActivityRange(val label: String) {
    WEEKS("Weeks"),
    MONTHS("Months"),
    YEARS("Years"),
}

private data class ActivityPoint(
    val label: String,
    val value: Long,
)

private fun filterCumulativeRange(data: List<CumulativeMoji>, range: CumulativeRange): List<CumulativeMoji> {
    if (data.isEmpty()) return emptyList()
    val sorted = data.sortedBy { it.date }
    val lastDate = sorted.last().date
    val cutoff = when (range) {
        CumulativeRange.ALL -> null
        CumulativeRange.MONTH_6 -> lastDate.minusMonths(6)
        CumulativeRange.YEAR_1 -> lastDate.minusYears(1)
        CumulativeRange.YEAR_3 -> lastDate.minusYears(3)
    }
    return cutoff?.let { c -> sorted.filter { !it.date.isBefore(c) } } ?: sorted
}

private fun buildActivityPoints(data: List<CumulativeMoji>, range: ActivityRange): List<ActivityPoint> {
    if (data.isEmpty()) return emptyList()
    val sorted = data.sortedBy { it.date }

    val dailyDeltas = mutableListOf<Pair<LocalDate, Long>>()
    var previous = 0L
    sorted.forEachIndexed { index, point ->
        val delta = if (index == 0) point.cumulativeMoji else (point.cumulativeMoji - previous).coerceAtLeast(0)
        previous = point.cumulativeMoji
        dailyDeltas += point.date to delta
    }

    return when (range) {
        ActivityRange.WEEKS -> {
            val weekFields = WeekFields.ISO
            dailyDeltas.groupBy {
                val year = it.first.get(weekFields.weekBasedYear())
                val week = it.first.get(weekFields.weekOfWeekBasedYear())
                year to week
            }.toList()
                .sortedBy { (key, _) -> key.first * 100 + key.second }
                .takeLast(12)
                .map { (key, values) ->
                    ActivityPoint("W${key.second}", values.sumOf { it.second })
                }
        }

        ActivityRange.MONTHS -> {
            dailyDeltas.groupBy { YearMonth.from(it.first) }
                .toList()
                .sortedBy { it.first }
                .takeLast(18)
                .map { (yearMonth, values) ->
                    ActivityPoint(
                        label = "${yearMonth.month.getDisplayName(DateTextStyle.SHORT, Locale.getDefault())} ${yearMonth.year % 100}",
                        value = values.sumOf { it.second },
                    )
                }
        }

        ActivityRange.YEARS -> {
            dailyDeltas.groupBy { it.first.year }
                .toList()
                .sortedBy { it.first }
                .takeLast(10)
                .map { (year, values) ->
                    ActivityPoint(year.toString(), values.sumOf { it.second })
                }
        }
    }
}
