package dk.marcusrokatis

sealed interface Cell
data class Letter(val letter: Char) : Cell
data class Clue(val clueInfo: ClueCell): Cell
object Empty: Cell

data class ClueCell(val clueText: String, val dir: Char)