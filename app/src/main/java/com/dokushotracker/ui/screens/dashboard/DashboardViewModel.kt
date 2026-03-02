package com.dokushotracker.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dokushotracker.data.model.CumulativeMoji
import com.dokushotracker.data.model.MediaTypeCount
import com.dokushotracker.data.model.MediaTypeMoji
import com.dokushotracker.data.model.MonthlyCount
import com.dokushotracker.data.model.MonthlyMoji
import com.dokushotracker.domain.model.GoalProgress
import com.dokushotracker.domain.model.Milestone
import com.dokushotracker.domain.usecase.GetGoalProgressUseCase
import com.dokushotracker.domain.usecase.GetStatisticsUseCase
import com.dokushotracker.util.MilestoneChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

sealed interface DashboardUiState {
    data object Loading : DashboardUiState

    data class Success(
        val totalMoji: Long,
        val totalBooks: Int,
        val mediaTypeCounts: List<MediaTypeCount>,
        val mediaTypeMoji: List<MediaTypeMoji>,
        val goalProgress: GoalProgress?,
        val averageMojiPerBook: Long,
        val lastEntryDate: LocalDate?,
        val readingStreakMonths: Int,
        val cumulativeMojiData: List<CumulativeMoji>,
        val monthlyCountData: List<MonthlyCount>,
        val monthlyMojiData: List<MonthlyMoji>,
        val nextMilestone: Milestone?,
        val achievedMilestones: List<Milestone>,
    ) : DashboardUiState
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    getStatisticsUseCase: GetStatisticsUseCase,
    getGoalProgressUseCase: GetGoalProgressUseCase,
) : ViewModel() {
    val uiState: StateFlow<DashboardUiState> = combine(
        getStatisticsUseCase.getTotalMoji(),
        getStatisticsUseCase.getTotalBooks(),
        getStatisticsUseCase.getMediaTypeCounts(),
        getStatisticsUseCase.getMediaTypeMoji(),
        getGoalProgressUseCase(),
        getStatisticsUseCase.getAverageMojiPerBook(),
        getStatisticsUseCase.getLatestEntry(),
        getStatisticsUseCase.getReadingStreak(),
        getStatisticsUseCase.getCumulativeMoji(),
        getStatisticsUseCase.getMonthlyCount(),
        getStatisticsUseCase.getMonthlyMoji(),
    ) { totalMoji, totalBooks, mediaTypeCounts, mediaTypeMoji, goalProgress, avgMoji, latestEntry, streak, cumulative, monthlyCount, monthlyMoji ->
        val achievedMoji = MilestoneChecker.achievedMojiMilestones(totalMoji)
        val achievedBooks = MilestoneChecker.achievedBookMilestones(totalBooks)
        val achieved = (achievedMoji + achievedBooks).sortedBy { it.threshold }
        val nextMilestone = MilestoneChecker.nextMojiMilestone(totalMoji) ?: MilestoneChecker.nextBookMilestone(totalBooks)

        DashboardUiState.Success(
            totalMoji = totalMoji,
            totalBooks = totalBooks,
            mediaTypeCounts = mediaTypeCounts,
            mediaTypeMoji = mediaTypeMoji,
            goalProgress = goalProgress,
            averageMojiPerBook = avgMoji.toLong(),
            lastEntryDate = latestEntry?.dateFinished,
            readingStreakMonths = streak,
            cumulativeMojiData = cumulative,
            monthlyCountData = monthlyCount,
            monthlyMojiData = monthlyMoji,
            nextMilestone = nextMilestone,
            achievedMilestones = achieved,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = DashboardUiState.Loading,
    )
}
