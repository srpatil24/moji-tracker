package com.dokushotracker.data.local.converter

import androidx.room.TypeConverter
import com.dokushotracker.data.model.GoalType
import com.dokushotracker.data.model.MediaType
import java.time.Instant
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun localDateToEpochDay(value: LocalDate?): Long? = value?.toEpochDay()

    @TypeConverter
    fun epochDayToLocalDate(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun instantToEpochMilli(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun epochMilliToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun mediaTypeToString(value: MediaType?): String? = value?.name

    @TypeConverter
    fun stringToMediaType(value: String?): MediaType? = value?.let(MediaType::valueOf)

    @TypeConverter
    fun goalTypeToString(value: GoalType?): String? = value?.name

    @TypeConverter
    fun stringToGoalType(value: String?): GoalType? = value?.let(GoalType::valueOf)
}
