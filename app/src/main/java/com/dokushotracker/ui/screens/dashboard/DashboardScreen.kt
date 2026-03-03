package com.dokushotracker.ui.screens.dashboard

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardColors
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dokushotracker.data.model.CumulativeMoji
import com.dokushotracker.data.model.MediaType
import com.dokushotracker.domain.model.AppSettings
import com.dokushotracker.domain.model.MilestoneType
import com.dokushotracker.domain.model.ThemeMode
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlinx.coroutines.delay

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    appSettings: AppSettings,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pendingCelebration by viewModel.pendingCelebration.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val pureBlackMode = isPureBlackActive(appSettings = appSettings)
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
                var celebrationVisible by rememberSaveable { mutableStateOf(false) }

                LaunchedEffect(pendingCelebration?.goalCreatedAtMillis) {
                    pendingCelebration?.let { pending ->
                        celebrationVisible = true
                        viewModel.markCelebrationShown(pending.goalCreatedAtMillis)
                    }
                }

                LaunchedEffect(celebrationVisible) {
                    if (celebrationVisible) {
                        performGoalCelebrationFeedback(context)
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp)
                            .alpha(if (celebrationVisible) 0.08f else 1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item {
                            TotalMojiCard(
                                totalMoji = uiState.totalMoji,
                                totalBooks = uiState.totalBooks,
                            )
                        }
                        item { GoalProgressCard(uiState = uiState, pureBlackMode = pureBlackMode) }
                        item { QuickStatsCard(uiState = uiState, pureBlackMode = pureBlackMode) }
                        item { MediaTypeBreakdownCard(uiState = uiState, pureBlackMode = pureBlackMode) }
                        item { LineChartCard(uiState = uiState, pureBlackMode = pureBlackMode) }
                        item { MonthlyCard(uiState = uiState, pureBlackMode = pureBlackMode) }
                        item { MilestoneCard(uiState = uiState, pureBlackMode = pureBlackMode) }
                        item { Spacer(modifier = Modifier.height(90.dp)) }
                    }

                    if (celebrationVisible) {
                        GoalCelebrationOverlay(
                            uiState = uiState,
                            onDismiss = { celebrationVisible = false },
                        )
                    }
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
@OptIn(ExperimentalLayoutApi::class)
private fun MediaTypeBreakdownCard(
    uiState: DashboardUiState.Success,
    pureBlackMode: Boolean,
) {
    val countsByType = MediaType.entries.associateWith { mediaType ->
        uiState.mediaTypeCounts.firstOrNull { it.mediaType == mediaType }?.count ?: 0
    }
    Card(
        modifier = dashboardCardModifier(pureBlackMode),
        colors = dashboardCardColors(pureBlackMode),
        border = dashboardCardBorder(pureBlackMode),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Media Breakdown", style = MaterialTheme.typography.titleMedium)
            MediaType.entries.forEach { mediaType ->
                val count = countsByType[mediaType] ?: 0
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(mediaType.displayName)
                    Text(
                        NumberFormatUtils.formatInt(count),
                        color = mediaTypeColor(mediaType),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            SpiderMediaChart(
                countsByType = countsByType,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
            )
        }
    }
}

@Composable
private fun GoalProgressCard(
    uiState: DashboardUiState.Success,
    pureBlackMode: Boolean,
) {
    Card(
        modifier = dashboardCardModifier(pureBlackMode),
        colors = dashboardCardColors(pureBlackMode),
        border = dashboardCardBorder(pureBlackMode),
    ) {
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
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
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
private fun QuickStatsCard(
    uiState: DashboardUiState.Success,
    pureBlackMode: Boolean,
) {
    Card(
        modifier = dashboardCardModifier(pureBlackMode),
        colors = dashboardCardColors(pureBlackMode),
        border = dashboardCardBorder(pureBlackMode),
    ) {
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
private fun LineChartCard(
    uiState: DashboardUiState.Success,
    pureBlackMode: Boolean,
) {
    var range by remember { mutableStateOf(CumulativeRange.ALL) }
    val filteredData = remember(uiState.cumulativeMojiData, range) {
        filterCumulativeRange(uiState.cumulativeMojiData, range)
    }

    Card(
        modifier = dashboardCardModifier(pureBlackMode),
        colors = dashboardCardColors(pureBlackMode),
        border = dashboardCardBorder(pureBlackMode),
    ) {
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
private fun MonthlyCard(
    uiState: DashboardUiState.Success,
    pureBlackMode: Boolean,
) {
    var range by remember { mutableStateOf(ActivityRange.MONTHS) }
    val activityPoints = remember(uiState.cumulativeMojiData, range) {
        buildActivityPoints(uiState.cumulativeMojiData, range)
    }

    Card(
        modifier = dashboardCardModifier(pureBlackMode),
        colors = dashboardCardColors(pureBlackMode),
        border = dashboardCardBorder(pureBlackMode),
    ) {
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
private fun MilestoneCard(
    uiState: DashboardUiState.Success,
    pureBlackMode: Boolean,
) {
    Card(
        modifier = dashboardCardModifier(pureBlackMode),
        colors = dashboardCardColors(pureBlackMode),
        border = dashboardCardBorder(pureBlackMode),
    ) {
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

@Composable
private fun GoalCelebrationOverlay(
    uiState: DashboardUiState.Success,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.92f))
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Filled.Celebration,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Goal Complete!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = "You reached ${NumberFormatUtils.formatLong(uiState.goalProgress?.targetValue ?: 0L)}.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "Amazing consistency. Keep going!",
                    style = MaterialTheme.typography.bodyMedium,
                )
                FilledTonalButton(onClick = onDismiss) {
                    Text("Continue")
                }
            }
        }
    }
}

@Composable
private fun SpiderMediaChart(
    countsByType: Map<MediaType, Int>,
    modifier: Modifier = Modifier,
) {
    val mediaTypes = MediaType.entries
    val values = mediaTypes.map { (countsByType[it] ?: 0).toFloat() }
    val maxValue = values.maxOrNull()?.coerceAtLeast(1f) ?: 1f
    val outlineColor = MaterialTheme.colorScheme.outline
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Canvas(modifier = modifier) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = min(size.width, size.height) * 0.34f
            val axisCount = mediaTypes.size
            val webLevels = 4

            repeat(webLevels) { level ->
                val levelPath = Path()
                val levelRadius = radius * ((level + 1).toFloat() / webLevels)
                repeat(axisCount) { index ->
                    val angle = (-PI / 2.0) + (2.0 * PI * index / axisCount.toDouble())
                    val point = Offset(
                        x = center.x + (cos(angle) * levelRadius).toFloat(),
                        y = center.y + (sin(angle) * levelRadius).toFloat(),
                    )
                    if (index == 0) levelPath.moveTo(point.x, point.y) else levelPath.lineTo(point.x, point.y)
                }
                levelPath.close()
                drawPath(
                    path = levelPath,
                    color = outlineColor.copy(alpha = 0.25f),
                    style = Stroke(width = 1f),
                )
            }

            repeat(axisCount) { index ->
                val angle = (-PI / 2.0) + (2.0 * PI * index / axisCount.toDouble())
                val point = Offset(
                    x = center.x + (cos(angle) * radius).toFloat(),
                    y = center.y + (sin(angle) * radius).toFloat(),
                )
                drawLine(
                    color = outlineColor.copy(alpha = 0.35f),
                    start = center,
                    end = point,
                    strokeWidth = 1f,
                )
            }

            val dataPath = Path()
            values.forEachIndexed { index, value ->
                val ratio = value / maxValue
                val pointRadius = radius * ratio
                val angle = (-PI / 2.0) + (2.0 * PI * index / axisCount.toDouble())
                val point = Offset(
                    x = center.x + (cos(angle) * pointRadius).toFloat(),
                    y = center.y + (sin(angle) * pointRadius).toFloat(),
                )
                if (index == 0) dataPath.moveTo(point.x, point.y) else dataPath.lineTo(point.x, point.y)
            }
            dataPath.close()
            drawPath(
                path = dataPath,
                color = primaryColor.copy(alpha = 0.22f),
            )
            drawPath(
                path = dataPath,
                color = primaryColor,
                style = Stroke(width = 3f),
            )
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

@Composable
private fun isPureBlackActive(appSettings: AppSettings): Boolean {
    val systemDark = isSystemInDarkTheme()
    val darkModeActive = when (appSettings.themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemDark
    }
    return darkModeActive && appSettings.pureBlackDarkMode
}

private fun dashboardCardModifier(pureBlackMode: Boolean): Modifier {
    return Modifier.fillMaxWidth()
}

@Composable
private fun dashboardCardColors(pureBlackMode: Boolean): CardColors {
    return if (pureBlackMode) {
        CardDefaults.cardColors(containerColor = Color.Black)
    } else {
        CardDefaults.cardColors()
    }
}

@Composable
private fun dashboardCardBorder(pureBlackMode: Boolean): BorderStroke? {
    return if (pureBlackMode) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    } else {
        null
    }
}

private suspend fun performGoalCelebrationFeedback(context: Context) {
    vibrateCelebration(context)
    val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 95)
    val tones = listOf(
        ToneGenerator.TONE_CDMA_PIP,
        ToneGenerator.TONE_PROP_BEEP,
        ToneGenerator.TONE_PROP_BEEP2,
    )
    tones.forEach { tone ->
        toneGenerator.startTone(tone, 150)
        delay(140L)
    }
    toneGenerator.release()
}

private fun vibrateCelebration(context: Context) {
    val heartbeat = VibrationEffect.createWaveform(
        longArrayOf(0L, 30L, 80L, 35L, 120L, 42L, 160L, 48L),
        -1,
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(VibratorManager::class.java)
        manager?.defaultVibrator?.vibrate(heartbeat)
    } else {
        @Suppress("DEPRECATION")
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        @Suppress("DEPRECATION")
        vibrator?.vibrate(heartbeat)
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
