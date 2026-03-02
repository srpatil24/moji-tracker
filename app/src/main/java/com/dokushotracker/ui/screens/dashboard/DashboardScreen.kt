package com.dokushotracker.ui.screens.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dokushotracker.data.model.MediaType
import com.dokushotracker.ui.components.AnimatedCounter
import com.dokushotracker.ui.components.EmptyState
import com.dokushotracker.ui.theme.mediaTypeColor
import com.dokushotracker.util.DateUtils
import com.dokushotracker.util.NumberFormatUtils

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
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        TotalMojiCard(
                            totalMoji = uiState.totalMoji,
                            totalBooks = uiState.totalBooks,
                        )
                    }
                    item {
                        MediaTypeBreakdownCard(uiState = uiState)
                    }
                    item {
                        GoalProgressCard(uiState = uiState)
                    }
                    item {
                        QuickStatsCard(uiState = uiState)
                    }
                    item {
                        LineChartCard(uiState = uiState)
                    }
                    item {
                        MonthlyCard(uiState = uiState)
                    }
                    item {
                        MilestoneCard(uiState = uiState)
                    }
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
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Total 文字 Read",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            AnimatedCounter(
                targetValue = totalMoji,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = "across ${NumberFormatUtils.formatInt(totalBooks)} books",
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
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Cumulative 文字 Read", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            if (uiState.cumulativeMojiData.size < 2) {
                Text("More entries are needed to draw this chart.")
                return@Column
            }
            val lineColor = MaterialTheme.colorScheme.primary
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
            ) {
                val points = uiState.cumulativeMojiData
                val maxY = points.maxOf { it.cumulativeMoji }.toFloat().coerceAtLeast(1f)
                val xStep = size.width / (points.size - 1).coerceAtLeast(1)
                val path = Path()
                points.forEachIndexed { index, point ->
                    val x = xStep * index
                    val y = size.height - (point.cumulativeMoji.toFloat() / maxY) * size.height
                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 5f, cap = StrokeCap.Round),
                )
            }
        }
    }
}

@Composable
private fun MonthlyCard(uiState: DashboardUiState.Success) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Monthly Reading Activity", style = MaterialTheme.typography.titleMedium)
            val maxCount = uiState.monthlyCountData.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
            uiState.monthlyCountData.takeLast(6).forEach { month ->
                val fraction = month.count.toFloat() / maxCount.toFloat()
                Text(text = month.yearMonth)
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                )
            }
            if (uiState.monthlyCountData.isEmpty()) {
                Text("No monthly data yet.")
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
                    com.dokushotracker.domain.model.MilestoneType.MOJI -> {
                        (next.threshold - uiState.totalMoji).coerceAtLeast(0)
                    }
                    com.dokushotracker.domain.model.MilestoneType.BOOKS -> {
                        (next.threshold - uiState.totalBooks).coerceAtLeast(0)
                    }
                }
                val unitLabel = if (next.type == com.dokushotracker.domain.model.MilestoneType.MOJI) "文字" else "books"
                Text("Next: ${next.label} in ${NumberFormatUtils.formatLong(remaining)} $unitLabel")
            }
        }
    }
}
