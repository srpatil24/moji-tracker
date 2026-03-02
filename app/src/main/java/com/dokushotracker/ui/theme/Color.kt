package com.dokushotracker.ui.theme

import androidx.compose.ui.graphics.Color
import com.dokushotracker.data.model.MediaType

val PrimaryLight = Color(0xFF4A6741)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFCCE8C5)
val OnPrimaryContainerLight = Color(0xFF0A2005)
val SecondaryLight = Color(0xFF54634D)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFD7E8CD)
val OnSecondaryContainerLight = Color(0xFF122810)
val TertiaryLight = Color(0xFF386568)
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFBBEBEE)
val OnTertiaryContainerLight = Color(0xFF002022)
val BackgroundLight = Color(0xFFF8FAF5)
val OnBackgroundLight = Color(0xFF1A1C19)
val SurfaceLight = Color(0xFFF8FAF5)
val OnSurfaceLight = Color(0xFF1A1C19)

val PrimaryDark = Color(0xFFB2D0A9)
val OnPrimaryDark = Color(0xFF183114)
val PrimaryContainerDark = Color(0xFF314A2A)
val OnPrimaryContainerDark = Color(0xFFCCE8C5)
val SecondaryDark = Color(0xFFBCCCB2)
val OnSecondaryDark = Color(0xFF273422)
val SecondaryContainerDark = Color(0xFF3D4B37)
val OnSecondaryContainerDark = Color(0xFFD7E8CD)
val TertiaryDark = Color(0xFF9FCFD2)
val OnTertiaryDark = Color(0xFF003739)
val TertiaryContainerDark = Color(0xFF1E4D50)
val OnTertiaryContainerDark = Color(0xFFBBEBEE)
val BackgroundDark = Color(0xFF1A1C19)
val OnBackgroundDark = Color(0xFFE1E3DD)
val SurfaceDark = Color(0xFF1A1C19)
val OnSurfaceDark = Color(0xFFE1E3DD)

val VnAccentLight = Color(0xFF7B5EA7)
val WnAccentLight = Color(0xFF2E7D6F)
val LnAccentLight = Color(0xFF3F6BA5)
val NovelAccentLight = Color(0xFFA67C2E)

fun mediaTypeColor(mediaType: MediaType): Color = when (mediaType) {
    MediaType.VN -> VnAccentLight
    MediaType.WN -> WnAccentLight
    MediaType.LN -> LnAccentLight
    MediaType.NOVEL -> NovelAccentLight
}
