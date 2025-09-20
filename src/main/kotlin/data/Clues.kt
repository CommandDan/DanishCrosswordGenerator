package dk.marcusrokatis.data

import kotlinx.serialization.Serializable

@Serializable
data class ClueEntry(val word: String, val clue: String)

@Serializable
data class ClueEntries(val entries: List<ClueEntry>)

@Serializable
data class ClueEntryV2(
    val word: String,
    val clue: String? = null,
    val clues: List<String>? = null
)

@Serializable
data class ClueEntriesV2(val entries: List<ClueEntryV2>)