package dk.marcusrokatis

import dk.marcusrokatis.data.Block
import dk.marcusrokatis.data.Clue
import dk.marcusrokatis.data.Grid
import dk.marcusrokatis.data.Letter
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

    fun generateOnce(seed: Int): Grid {
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
    }
}