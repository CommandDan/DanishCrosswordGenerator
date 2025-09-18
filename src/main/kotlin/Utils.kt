package dk.marcusrokatis

import com.charleskorn.kaml.Yaml
import dk.marcusrokatis.data.ClueEntries
import dk.marcusrokatis.data.ClueEntry
import kotlinx.serialization.Serializable
import org.openpdf.text.pdf.PdfPCell
import java.io.File
import kotlin.random.Random

fun String.normalized() = this.uppercase()

fun String.lettersOnly() = this.replace(Regex("[^A-Za-zÆØÅæøå]"), "")

fun String.toWords() = this.trim().split(Regex("\\s+")).filter { it.isNotBlank() }


@Serializable
data class MapWrapper(val entries: Map<String, String>)

fun loadClueEntriesFromYamlKaml(path: String): ClueEntryList {
    val yamlText = File(path).readText()

    return try { // Try as list
        val parsed = Yaml.default.decodeFromString(ClueEntries.serializer(), yamlText)
        parsed.entries.map { it.copy(word = it.word.normalized()) }
    } catch (_: Exception) { // Try as map
        val parsed = Yaml.default.decodeFromString(MapWrapper.serializer(), yamlText)
        parsed.entries.map { (w, c) -> ClueEntry(w.normalized(), c) }
    }
}


fun ClueEntryList.filteredByLength(minLength: Int, maxLength: Int) =
    map { it.copy(word = it.word.normalized()) }
    .filter { it.word.all { char -> char.isLetter() } && it.word.length in minLength..maxLength }
    .distinctBy { it.word }
    .ifEmpty { error("Ingen ord efter filter (minLen=$minLength, maxLen=$maxLength)") }

fun ClueEntryList.shuffledSortedByLongestWord(random: Random) =
    shuffled(random).sortedByDescending { it.word.length }


var PdfPCell.padding: Float
    get() = this.paddingTop // alle fire sider har samme værdi, så top er nok
    set(value) {
        this.setPadding(value)
    }

fun Double.withDigits(digits: Int) = String.format("%.${digits}f", this)