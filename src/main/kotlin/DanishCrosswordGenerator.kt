@file:JvmName("DanishCrosswordGenerator")

package dk.marcusrokatis

import dk.marcusrokatis.data.fallback.DEFAULT_ENTRIES
import org.openpdf.text.Document
import java.awt.Color

fun main(rawArgs: Array<String>) {
    val args = rawArgs.toList()
    fun index(flag: String) = args.indexOf(flag).takeIf { it >= 0 }

    val sizes = index("--sizes")?.let { index -> parseSizes(args.drop(index + 1).takeWhile { !it.startsWith("--") }) }
        ?: listOf(13 to 13, 15 to 15, 17 to 17, 19 to 19)

    val attempts = index("--attempts")?.let { index -> args.getOrNull(index + 1)?.toIntOrNull() } ?: 800
    val outPuzzles = index("--out")?.let { index -> args.getOrNull(index + 1) } ?: "puzzles.pdf"
    val outSolutions = index("--solutions")?.let { index -> args.getOrNull(index + 1) } ?: "solutions.pdf"
    val outCombined = index("--combined")?.let { index -> args.getOrNull(index + 1) } ?: "combined.pdf"
    val yamlPath = index("--wordlist")?.let { index -> args.getOrNull(index + 1) }

    val minLength = index("--minLen")?.let { index -> args.getOrNull(index + 1)?.toIntOrNull() } ?: 2
    val maxLength = index("--maxLen")?.let { index -> args.getOrNull(index + 1)?.toIntOrNull() } ?: 12
    val noClues = args.any { it == "--noClues" }
    val arrows = index("--arrows")?.let { index -> args.getOrNull(index + 1)?.lowercase() in listOf("1", "true", "yes") } ?: true
    val arrowStyle = index("--arrowStyle")?.let { index -> args.getOrNull(index + 1)?.lowercase() }?.takeIf { it in listOf("arrow", "tri") } ?: "arrow"

    val cellHeight = index("--cellHeight")?.let { index -> args.getOrNull(index + 1)?.toFloatOrNull() } ?: 18f
    val clueFontSize = index("--clueFont")?.let { index -> args.getOrNull(index + 1)?.toFloatOrNull() } ?: 6.0f
    val letterFontSize = index("--letterFont")?.let { index -> args.getOrNull(index + 1)?.toFloatOrNull() } ?: 8.0f
    val grayValue = index("--gray")?.let { index -> args.getOrNull(index + 1)?.toIntOrNull()?.coerceIn(0, 255) } ?: 230
    val answerGray = Color(grayValue, grayValue, grayValue)

    val clueEntries = try {
        yamlPath?.let { loadClueEntriesFromYamlKaml(it) } ?: DEFAULT_ENTRIES
    } catch (exception: Exception) {
        println("Kunne ikke læse YAML (${exception.message}). Bruger indbygget ordliste.")
        DEFAULT_ENTRIES
    }

    val grids = sizes.mapIndexed { gridIndex, (width, height) ->
        buildCrossword(clueEntries, width, height, attempts, minLength, maxLength, seedBase = gridIndex * 1000)
    }
}

fun parseSizes(args: List<String>): IntPairList =
    args.mapNotNull { string ->
        val parse = string.lowercase()
        if ('x' in parse) {
            val (sizeHorizontal, sizeVertical) = parse.split('x', limit = 2)
            sizeHorizontal.toIntOrNull()?.let { width -> sizeVertical.toIntOrNull()?.let { height -> width to height } }
        } else null
    }

// try-with-resources helper
inline fun <T: Document, R> T.use(block: (T) -> R): R {
    try { return block(this) } finally { if (this.isOpen) this.close() }
}