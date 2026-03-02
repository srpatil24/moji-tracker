package com.dokushotracker.domain.model

enum class SortOption(val displayName: String) {
    DATE_DESC("Newest First"),
    DATE_ASC("Oldest First"),
    MOJI_DESC("Most 文字"),
    MOJI_ASC("Least 文字"),
    TITLE_ASC("Title A-Z"),
    TITLE_DESC("Title Z-A"),
}
