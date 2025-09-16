package dk.marcusrokatis

import dk.marcusrokatis.data.Cell
import dk.marcusrokatis.data.ClueEntry

typealias IntPair = Pair<Int, Int>


typealias IntPairList = List<IntPair>
typealias MutableIntPairList = MutableList<IntPair>


typealias List2D<T> = List<List<T>>
typealias MutableList2D<T> = MutableList<MutableList<T>>

fun <T> List2D(rows: Int, columns: Int, init: (Int, Int) -> T): List2D<T> =
    List(rows) { row ->
        List(columns) { col ->
            init(row, col)
        }
    }

fun <T> MutableList2D(rows: Int, columns: Int, init: (Int, Int) -> T): MutableList2D<T> =
    MutableList(rows) { row ->
        MutableList(columns) { col ->
            init(row, col)
        }
    }


typealias CellGrid = List2D<Cell>
typealias MutableCellGrid = MutableList2D<Cell>

fun CellGrid(rows: Int, columns: Int, init: (Int, Int) -> Cell): CellGrid =
    List2D(rows, columns) { row, column -> init(row, column) }

fun MutableCellGrid(rows: Int, columns: Int, init: (Int, Int) -> Cell): MutableCellGrid =
    MutableList2D(rows, columns) { row, column -> init(row, column) }


typealias ClueEntryList = List<ClueEntry>
typealias MutableClueEntryList = MutableList<ClueEntry>

fun ClueEntryList(size: Int, init: (Int) -> ClueEntry): ClueEntryList =
    List(size) { index -> init(index) }

fun ClueEntryList(): ClueEntryList = listOf()

fun MutableClueEntryList(size: Int, init: (Int) -> ClueEntry): MutableClueEntryList =
    MutableList(size) { index -> init(index) }

fun MutableClueEntryList(): MutableClueEntryList = mutableListOf()


typealias CharacterGridMap = Map<IntPair, Char>
typealias MutableCharacterGridMap = MutableMap<IntPair, Char>

fun CharacterGridMap(): CharacterGridMap = mapOf()

fun MutableCharacterGridMap(): MutableCharacterGridMap = mutableMapOf()


typealias WordCandidate = Triple<Char, Int, Int>


typealias WordCandidateList = List<WordCandidate>
typealias MutableWordCandidateList = MutableList<WordCandidate>

fun WordCandidateList(): WordCandidateList = listOf()

fun MutableWordCandidateList(): MutableWordCandidateList = mutableListOf()