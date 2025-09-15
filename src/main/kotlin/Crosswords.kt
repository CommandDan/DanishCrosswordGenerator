package dk.marcusrokatis

import dk.marcusrokatis.data.Block
import dk.marcusrokatis.data.Clue
import dk.marcusrokatis.data.ClueCell
import dk.marcusrokatis.data.ClueEntry
import dk.marcusrokatis.data.Grid
import dk.marcusrokatis.data.Letter
import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random

fun buildCrossword(
    entries: ClueEntryList,
    width: Int, height: Int,
    attempts: Int,
    minLength: Int,
    maxLength: Int,
    seedBase: Int = 0
): Grid {

    val filtered = entries.filteredByLength(minLength, maxLength)

    fun generateOnce(seed: Int): Grid? {
        val random = Random(seed)
        val sorted = filtered.shuffledSortedByLongestWord(random)
        val cells = MutableCellGrid(height, width) { _, _ -> Block}
        val usedPairs = MutableClueEntryList()
        val letters = MutableCharacterGridMap()

        fun isLetter(row: Int, column: Int) = cells[row][column] is Letter

        fun canPlace(word: String, row: Int, column: Int, direction: Char, requireCross: Boolean): Boolean {
            val (clueRow, clueColumn) = if (direction == 'H') row to (column - 1) else (row - 1) to column
            if (clueRow !in 0 until height || clueColumn !in 0 until width) return false
            if (cells[clueRow][clueColumn] !is Block) return false

            var crosses = 0
            for (index in word.indices) {
                val wordRow = if (direction == 'V') row + index else row
                val wordColumn = if (direction == 'H') column + index else column
                if (wordRow !in 0 until height || wordColumn !in 0 until width) return false
                when (val cell = cells[wordRow][wordColumn]) {
                    is Block -> when (direction) {
                        'H' if wordRow - 1 >= 0 && isLetter(wordRow - 1, wordColumn) -> return false
                        'H' if wordRow + 1 < height && isLetter(wordRow + 1, wordColumn) -> return false
                        'V' if wordColumn - 1 >= 0 && isLetter(wordRow, wordColumn - 1) -> return false
                        'V' if wordColumn + 1 < width && isLetter(wordRow, wordColumn + 1) -> return false
                    }
                    is Clue -> return false
                    is Letter -> {
                        if (cell.letter != word[index]) return false
                        crosses++
                    }
                }
            }

            val endRow = if (direction == 'V') row + word.length else row
            val endColumn = if (direction == 'H') column + word.length else column
            if (endRow in 0 until height && endColumn in 0 until width && isLetter(endRow, endColumn)) return false
            val preRow = if (direction == 'V') row - 1 else row
            val preColumn = if (direction == 'H') column - 1 else column
            if (preRow in 0 until height && preColumn in 0 until width && isLetter(preRow, preColumn)) return false

            if (requireCross && crosses == 0 && letters.isNotEmpty()) return false
            return true
        }

        fun place(clueEntry: ClueEntry, row: Int, column: Int, direction: Char) {
            val (clueRow, clueColumn) = if (direction == 'H') row to (column - 1) else (row - 1) to column
            cells[clueRow][clueColumn] = Clue(ClueCell(clueEntry.clue, direction))
            usedPairs += clueEntry
            clueEntry.word.forEachIndexed { index, char ->
                val wordRow = if (direction == 'V') row + index else row
                val wordColumn = if (direction == 'H') column + index else column
                cells[wordRow][wordColumn] = Letter(char)
                letters[wordRow to wordColumn] = char
            }
        }

        val first = sorted.first()
        val horizontalStart = max(1, (width - first.word.length) / 2 + 1)
        val verticalStart = max(1, (height - first.word.length) / 2 + 1)
        when {
            canPlace(first.word, height / 2, horizontalStart, 'H', false) ->
                place(first, height / 2, horizontalStart, 'H')
            canPlace(first.word, verticalStart, width / 2, 'V', false) ->
                place(first, verticalStart, width / 2, 'V')
            else -> return null
        }

        fun candidates(word: String): WordCandidateList {
            val list = MutableWordCandidateList()
            val posByChar = letters.entries.groupBy({ it.value }, { it.key })
            for (index in word.indices) {
                val char = word[index]
                for ((wordRow, wordColumn) in posByChar[char] ?: emptyList()) {
                    val columnHorizontal = wordColumn - index; val rowHorizontal = wordRow
                    if (columnHorizontal >= 1 && rowHorizontal >= 0 && canPlace(word, rowHorizontal, columnHorizontal, 'H', true))
                        list += Triple('H', rowHorizontal, columnHorizontal)
                    val rowVertical = wordRow - index; val columnVertical = wordColumn
                    if (rowVertical >= 1 && columnVertical >= 0 && canPlace(word, rowVertical, columnVertical, 'V', true))
                        list += Triple('V', rowVertical, columnVertical)
                }
            }
            return list.sortedWith(compareByDescending<WordCandidate> {
                val (direction, row, column) = it
                when (direction) {
                    'H' -> (0 until word.length).count { index -> cells[row][column + index] is Letter }
                    'V' -> (0 until word.length).count { index -> cells[row + index][column] is Letter }
                    else -> 0
                }
            }.thenBy {
                val (direction, row, column) = it
                val clueRow = if (direction == 'H') row else row + word.length / 2
                val clueColumn = if (direction == 'H') column + word.length / 2 else column
                abs(clueRow - height / 2) + abs(clueColumn - width / 2)
            })
        }

        sorted.drop(1).forEach { clueEntry ->
            val candidate = candidates(clueEntry.word).firstOrNull()
            candidate?.let { val (direction, rowHorizontal, columnHorizontal) = it; place(clueEntry, rowHorizontal, columnHorizontal, direction) }
        }
        return Grid(width, height, cells, usedPairs)
    }

    var best: Grid? = null
    for (attempt in 0 until attempts) {
        val grid = generateOnce(seedBase + attempt) ?: continue
        if (best == null || grid.utilization() > best.utilization()) best = grid
    }
    return best ?: error("Kunne ikke generere gitter")
}