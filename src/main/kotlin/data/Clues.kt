package dk.marcusrokatis.data

import kotlinx.serialization.Serializable

@Serializable
data class ClueEntry(val word: String, val clue: String)

@Serializable
data class ClueEntries(val entries: List<ClueEntry>)