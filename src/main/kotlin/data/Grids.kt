package dk.marcusrokatis.data

import dk.marcusrokatis.CellGrid
import dk.marcusrokatis.MutableCellGrid
import dk.marcusrokatis.MutableClueEntryList
import kotlin.math.max
import kotlin.math.min

data class Grid(
    val width: Int,
    val height: Int,
    val cells: CellGrid,
    val usedPairs: MutableClueEntryList
) {
    val usedEdges: GridEdges get() {
        var top = height - 1
        var bottom = 0
        var left = width - 1
        var right = 0

        cells.forEachIndexed { row, columns ->
            columns.forEachIndexed { column, cell ->
                if (cell !is Block) {
                    top = min(row, top)
                    bottom = max(row, bottom)
                    left = min(column, left)
                    right = max(column, right)
                }
            }
        }
        return GridEdges(top, bottom, left, right)
    }

    val usedWidth: Int get() = usedEdges.right - usedEdges.left + 1
    val usedHeight: Int get() = usedEdges.bottom - usedEdges.top + 1

    val utilization: Double get() = cells.sumOf { row ->
        row.count { it is Letter }
    }.toDouble() / (width * height).coerceAtLeast(1)

    val minimized: Grid by lazy {
        if (usedWidth == width && usedHeight == height) return@lazy this

        val cellsCopy = MutableCellGrid(usedHeight, usedWidth) { row, column ->
            cells[usedEdges.top + row][usedEdges.left + column]
        }
        Grid(usedWidth, usedHeight, cellsCopy, usedPairs)
    }
}

data class GridEdges(
    val top: Int,
    val bottom: Int,
    val left: Int,
    val right: Int
)