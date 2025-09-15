package dk.marcusrokatis

import dk.marcusrokatis.data.ClueEntry

fun buildCrossword(
    entries: List<ClueEntry>,
    width: Int, height: Int,
    attempts: Int,
    minLength: Int,
    maxLength: Int,
    seedBase: Int = 0
) {
    val filtered = entries.filteredByLength(minLength, maxLength)
}