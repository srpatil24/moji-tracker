package com.dokushotracker.domain.model

enum class SortOption(val displayName: String) {
    DATE_DESC("Newest First"),
    DATE_ASC("Oldest First"),
    MOJI_DESC("Most Moji"),
    MOJI_ASC("Least Moji"),
    TITLE_ASC("Title A-Z"),
    TITLE_DESC("Title Z-A"),
}
