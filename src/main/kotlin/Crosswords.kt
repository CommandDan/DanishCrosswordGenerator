package dk.marcusrokatis

import dk.marcusrokatis.data.Block
import dk.marcusrokatis.data.Clue
import dk.marcusrokatis.data.ClueCell
import dk.marcusrokatis.data.ClueEntry
import dk.marcusrokatis.data.Direction
import dk.marcusrokatis.data.Grid
import dk.marcusrokatis.data.Letter
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

fun buildCrossword(
    entries: ClueEntryList,
    width: Int, height: Int,
    attempts: Int,
    minLength: Int,
    maxLength: Int,
    seedBase: Int = 0,
    maxUseWord: Int = 5,
    maxUseClue: Int = 10
): Grid {
    log("Genererer krydsord med størrelse $width gange $height. Forsøger $attempts gange:\nOrdlængder mellem $minLength og $maxLength. Seed $seedBase.")

    val gridMaxLength = min(maxLength, max(width, height) - 1) // -1 for clue-plads
    val filtered = entries.filteredByLength(minLength, gridMaxLength)

    log("Antal ord efter filter: ${filtered.size}. Filtrede ${entries.size - filtered.size} ord fra.")
    if (gridMaxLength < maxLength) {
        log("Max ordlængde sat til $gridMaxLength pga. plads til ledetråd.")
    }

    fun generateOnce(seed: Int): Grid? {
        val random = Random(seed)
        val sorted = filtered.shuffledSortedByLongestWord(random)
        val cells = MutableCellGrid(height, width) { _, _ -> Block}
        val usedPairs = MutableClueEntryList()
        val letters = MutableCharacterGridMap()
        val wordUses = mutableMapOf<String, Int>()
        val clueUses = mutableMapOf<String, Int>()

        fun isLetter(row: Int, column: Int) = cells[row][column] is Letter

        fun canPlace(word: String, row: Int, column: Int, direction: Direction, requireCross: Boolean): Boolean {
            // krav: 1 celle til clue før ordet
            if (direction == Direction.HORIZONTAL) {
                if (column < 1 || column + word.length > width) return false
            } else { // VERTICAL
                if (row < 1 || row + word.length > height) return false
            }
            val clueRow = if (direction == Direction.HORIZONTAL) row else row - 1
            val clueColumn = if (direction == Direction.HORIZONTAL) column - 1 else column
            if (clueRow !in 0 until height || clueColumn !in 0 until width) return false
            if (cells[clueRow][clueColumn] !is Block) return false

            var crosses = 0
            for (index in word.indices) {
                val wordRow = if (direction == Direction.VERTICAL) row + index else row
                val wordColumn = if (direction == Direction.HORIZONTAL) column + index else column
                if (wordRow !in 0 until height || wordColumn !in 0 until width) return false
                when (val cell = cells[wordRow][wordColumn]) {
                    is Block -> when (direction) {
                        Direction.HORIZONTAL if wordRow - 1 >= 0 && isLetter(wordRow - 1, wordColumn) -> return false
                        Direction.HORIZONTAL if wordRow + 1 < height && isLetter(wordRow + 1, wordColumn) -> return false
                        Direction.VERTICAL if wordColumn - 1 >= 0 && isLetter(wordRow, wordColumn - 1) -> return false
                        Direction.VERTICAL if wordColumn + 1 < width && isLetter(wordRow, wordColumn + 1) -> return false
                        else -> { /* ok: ingen nabo-konflikt for tom celle */ }
                    }
                    is Clue -> return false
                    is Letter -> {
                        if (cell.letter != word[index]) return false
                        crosses++
                    }
                }
            }

            val endRow = if (direction == Direction.VERTICAL) row + word.length else row
            val endColumn = if (direction == Direction.HORIZONTAL) column + word.length else column
            if (endRow in 0 until height && endColumn in 0 until width && isLetter(endRow, endColumn)) return false
            val preRow = if (direction == Direction.VERTICAL) row - 1 else row
            val preColumn = if (direction == Direction.HORIZONTAL) column - 1 else column
            if (preRow in 0 until height && preColumn in 0 until width && isLetter(preRow, preColumn)) return false

            if (requireCross && crosses == 0 && letters.isNotEmpty()) return false
            return true
        }

        fun place(clueEntry: ClueEntry, row: Int, column: Int, direction: Direction) {
            if (wordUses.getOrDefault(clueEntry.word, 0) >= maxUseWord) return // Ord er brugt nok
            if (clueUses.getOrDefault(clueEntry.clue, 0) >= maxUseClue) return // Ledetråd er brugt nok

            val (clueRow, clueColumn) = if (direction == Direction.HORIZONTAL) row to (column - 1) else (row - 1) to column
            cells[clueRow][clueColumn] = Clue(ClueCell(clueEntry.clue, direction))
            usedPairs += clueEntry
            clueEntry.word.forEachIndexed { index, char ->
                val wordRow = if (direction == Direction.VERTICAL) row + index else row
                val wordColumn = if (direction == Direction.HORIZONTAL) column + index else column
                cells[wordRow][wordColumn] = Letter(char)
                letters[wordRow to wordColumn] = char
            }
            wordUses.increment(clueEntry.word)
            clueUses.increment(clueEntry.clue)
        }

        val first = sorted.firstOrNull { it.word.length <= width || it.word.length <= height } ?: return null
        val wordLength = first.word.length

        val canHorizontal = wordLength <= width - 1
        val canVertical = wordLength <= height - 1

        fun centeredStartHorizontal(): Int {
            val maxStart = width - wordLength           // sidste lovlige start (inklusive)
            val minStart = 1                      // pga. clue til venstre
            return minStart + (max(0, maxStart - minStart) / 2)
        }
        fun centeredStartVertical(): Int {
            val maxStart = height - wordLength          // sidste lovlige start (inklusive)
            val minStart = 1                      // pga. clue over
            return minStart + (max(0, maxStart - minStart) / 2)
        }

        when {
            canHorizontal && canPlace(first.word, height / 2, centeredStartHorizontal(), Direction.HORIZONTAL, false) ->
                place(first, height / 2, centeredStartHorizontal(), Direction.HORIZONTAL)
            canVertical && canPlace(first.word, centeredStartVertical(), width / 2, Direction.VERTICAL, false) ->
                place(first, centeredStartVertical(), width / 2, Direction.VERTICAL)
            else -> return null
        }

        fun candidates(word: String): WordCandidateList {
            val list = MutableWordCandidateList()
            val posByChar = letters.entries.groupBy({ it.value }, { it.key })
            for (index in word.indices) {
                val char = word[index]
                for ((wordRow, wordColumn) in posByChar[char] ?: emptyList()) {
                    val columnHorizontal = wordColumn - index; val rowHorizontal = wordRow
                    if (columnHorizontal >= 1 && rowHorizontal >= 0 && canPlace(word, rowHorizontal, columnHorizontal, Direction.HORIZONTAL, true))
                        list += Triple(Direction.HORIZONTAL, rowHorizontal, columnHorizontal)
                    val rowVertical = wordRow - index; val columnVertical = wordColumn
                    if (rowVertical >= 1 && columnVertical >= 0 && canPlace(word, rowVertical, columnVertical, Direction.VERTICAL, true))
                        list += Triple(Direction.VERTICAL, rowVertical, columnVertical)
                }
            }
            return list.sortedWith(compareByDescending<WordCandidate> {
                val (direction, row, column) = it
                when (direction) {
                    Direction.HORIZONTAL -> (0 until word.length).count { index -> cells[row][column + index] is Letter }
                    Direction.VERTICAL -> (0 until word.length).count { index -> cells[row + index][column] is Letter }
                }
            }.thenBy {
                val (direction, row, column) = it
                val clueRow = if (direction == Direction.HORIZONTAL) row else row + word.length / 2
                val clueColumn = if (direction == Direction.HORIZONTAL) column + word.length / 2 else column
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
        if (best == null || grid.utilization > best.utilization) best = grid
    }
    return best ?: error("Kunne ikke generere gitter ${width}x${height} med ordlængder mellem $minLength og $maxLength")
}