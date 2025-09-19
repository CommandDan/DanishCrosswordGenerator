package dk.marcusrokatis.data

sealed interface Cell
data class Letter(val letter: Char) : Cell
data class Clue(val clueInfo: ClueCell): Cell
object Block: Cell

data class ClueCell(val clueText: String, val direction: Direction)

enum class Direction {
    HORIZONTAL,
    VERTICAL
}