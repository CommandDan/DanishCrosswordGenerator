package dk.marcusrokatis.data

import dk.marcusrokatis.CellGrid
import dk.marcusrokatis.MutableClueEntryList

data class Grid(
    val width: Int,
    val height: Int,
    val cells: CellGrid,
    val usedPairs: MutableClueEntryList
) {
    fun utilization(): Double = cells.sumOf { row ->
        row.count { it is Letter }
    }.toDouble() / (width * height).coerceAtLeast(1)
}