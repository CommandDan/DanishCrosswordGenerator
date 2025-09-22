package dk.marcusrokatis

import com.charleskorn.kaml.Yaml
import dk.marcusrokatis.data.ClueEntriesV2
import dk.marcusrokatis.data.ClueEntry
import kotlinx.serialization.Serializable
import org.openpdf.text.pdf.BaseFont
import org.openpdf.text.pdf.PdfPCell
import java.io.File
import java.io.IOException
import kotlin.random.Random

enum class TextCase { UPPER, LOWER, TITLE, NONE }


/** Trims, optionally sets case, optionally removes non letters */
fun String.normalized(textCase: TextCase = TextCase.NONE, keepOnlyLetters: Boolean = false) = this.trim().let {
    when (textCase) {
        TextCase.UPPER -> it.uppercase()
        TextCase.LOWER -> it.lowercase()
        TextCase.TITLE -> it.titlecase()
        TextCase.NONE -> it
    }
}.let { if (keepOnlyLetters) it.lettersOnly() else it }

fun String.lettersOnly() = this.replace(Regex("[^A-Za-zÆØÅæøå]"), "")

fun String.toWords() = this.trim().split(Regex("\\s+")).filter { it.isNotBlank() }

fun String.isWord() = !this.contains(Regex("[^A-Za-zÆØÅæøå]"))

fun String.titlecase() = this.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }


@Serializable
data class MapWrapper(val entries: Map<String, String>)

@Serializable
data class MapOfListsWrapper(val entries: Map<String, List<String>>)


/**
 * Canonical loader that supports:
 * 1) List format with `clue` or `clues` fields
 * 2) Map format with single string value per word
 * 3) Map format with list of clues per word
 *
 * Always returns a normalized dictionary: WORD (UPPER) -> list of non-blank clues.
 */
fun loadClueDictionaryFromYamlKaml(path: String): ClueDictionary {
    val yamlText = File(path).readText()

    // 1) Try list-format first: entries: [ { word, clue }, { word, clues: [...] } ]
    runCatching {
        val parsed = Yaml.default.decodeFromString(ClueEntriesV2.serializer(), yamlText)
        val grouped = mutableMapOf<String, MutableList<String>>()
        parsed.entries.forEach { entry ->
            val word = entry.word.normalized(TextCase.UPPER, true)
            val list = when {
                entry.clue != null -> listOf(entry.clue)
                entry.clues != null -> entry.clues
                else -> emptyList()
            }.map { it.normalized(TextCase.TITLE) }.filter { it.isNotBlank() }
            if (list.isNotEmpty()) grouped.getOrPut(word) { mutableListOf() }.addAll(list)
        }
        if (grouped.isNotEmpty()) return grouped.mapValues { (_, value) -> value.distinct() }
    }

    // 2) Map-format: entries: { WORD: "clue" }
    runCatching {
        val parsed = Yaml.default.decodeFromString(MapWrapper.serializer(), yamlText)
        val grouped = parsed.entries
            .map { (word, clue) -> word.normalized(TextCase.UPPER, true) to clue.normalized(TextCase.TITLE) }
            .filter { it.second.isNotBlank() }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, value) -> value.distinct() }
        if (grouped.isNotEmpty()) return grouped
    }

    // 3) Map-format: entries: { WORD: ["clue1", "clue2"] }
    runCatching {
        val parsed = Yaml.default.decodeFromString(MapOfListsWrapper.serializer(), yamlText)
        val grouped = parsed.entries
            .map { (word, list) -> word.normalized(TextCase.UPPER, true) to (list.map { it.normalized(TextCase.TITLE) }.filter { it.isNotBlank() }) }
            .filter { it.second.isNotEmpty() }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, lists) -> lists.flatten().distinct() }
        if (grouped.isNotEmpty()) return grouped
    }

    error("Kunne ikke parse YAML som liste- eller map-format (med en eller flere ledetråde).")
}

/**
 * Backward compatible helper: expand dictionary to a flat list of ClueEntry
 * for de steder i koden der stadig forventer ClueEntryList.
 */
fun loadClueEntriesFromYamlKaml(path: String): ClueEntryList =
    loadClueDictionaryFromYamlKaml(path).flatMap { (word, clues) -> clues.map { ClueEntry(word, it) } }


fun ClueEntryList.filteredByLength(minLength: Int, maxLength: Int): ClueEntryList =
    map { it.copy() }
    .filter { it.word.isWord() && it.word.length in minLength..maxLength }
    .ifEmpty { error("Ingen ord efter filter (minLen=$minLength, maxLen=$maxLength)") }

fun ClueEntryList.shuffledSortedByLongestWord(random: Random): ClueEntryList =
    shuffled(random).sortedByDescending { it.word.length }

fun ClueEntryList.meaningsAdded(): ClueEntryList = this + this.mapNotNull {
    if (it.clue.trim().isWord()) ClueEntry(it.clue.normalized(TextCase.UPPER, true), it.word.normalized(TextCase.TITLE)) else null
}


/** Vælg en tilfældig ledetråd for et ord. Kaster hvis ordet ikke findes i ordbogen. */
fun ClueDictionary.randomClueFor(word: String, random: Random): String {
    val clues = this[word.normalized(TextCase.UPPER)]
        ?: error("Ord ikke fundet i ordbog: $word")
    return clues.random(random)
}

/** Konverter til flad liste (hvis nogle dele af systemet stadig bruger ClueEntryList). */
fun ClueDictionary.toClueEntries(): ClueEntryList = this.flatMap { (word, clues) -> clues.map { ClueEntry(word, it) } }


var PdfPCell.padding: Float
    get() = this.paddingTop // alle fire sider har samme værdi, så top er nok
    set(value) {
        this.setPadding(value)
    }


fun Double.withDigits(digits: Int) = String.format("%.${digits}f", this)


fun BaseFont.supports(ch: Char) = try { this.charExists(ch.code) } catch (_: Throwable) { false }

fun loadEmbeddedBaseFontFromResource(resourcePath: String): BaseFont {
    val stream = Thread.currentThread().contextClassLoader.getResourceAsStream(resourcePath)
        ?: throw IOException("Font resource ikke fundet på classpath: $resourcePath")
    val ttfBytes = stream.use { it.readBytes() }

    return BaseFont.createFont(
        resourcePath,          // skal matche korrekt fontnavn
        BaseFont.IDENTITY_H,              // fuld Unicode (ÆØÅ osv.)
        BaseFont.EMBEDDED,                // indlejr fonten i PDF'en
        false,                            // cached = false (valgfrit)
        ttfBytes,                         // TTF som bytes
        null                              // pfb (kun til Type1)
    )
}