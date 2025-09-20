package dk.marcusrokatis.data

import dk.marcusrokatis.CellGrid
import dk.marcusrokatis.MutableCellGrid
import dk.marcusrokatis.MutableClueEntryList
import dk.marcusrokatis.log
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
        if (usedWidth == width && usedHeight == height) {
            // Already tight; rotate only if portrait
            if (width >= height) this else Grid(height, width, rotate90Clockwise(cells), usedPairs)
        } else {
            // Crop to used bounding box
            val croppedCells = MutableCellGrid(usedHeight, usedWidth) { row, column ->
                cells[usedEdges.top + row][usedEdges.left + column]
            }
            val cropped = Grid(usedWidth, usedHeight, croppedCells, usedPairs)
            if (cropped.width >= cropped.height) cropped
            else Grid(cropped.height, cropped.width, rotate90Clockwise(croppedCells), usedPairs)
        }
    }

    /** Rotate a cell grid 90° clockwise. Also swaps clue directions H<->V. */
    private fun rotate90Clockwise(sourceGrid: CellGrid): CellGrid {
        log("Roterer gitter så bredde er større end højde")
        val height = sourceGrid.size
        val width = sourceGrid.firstOrNull()?.size ?: 0
        return MutableCellGrid(width, height) { row, column ->
            when (val old = sourceGrid[height - 1 - column][row]) {
                is Clue -> Clue(old.clueInfo.copy(direction = if (old.clueInfo.direction == Direction.HORIZONTAL) Direction.VERTICAL else Direction.HORIZONTAL))
                is Letter -> old
                is Block -> old
            }
        }
    }
}

data class GridEdges(
    val top: Int,
    val bottom: Int,
    val left: Int,
    val right: Int
)