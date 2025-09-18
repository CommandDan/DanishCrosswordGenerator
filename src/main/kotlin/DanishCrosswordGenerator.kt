@file:JvmName("DanishCrosswordGenerator")

package dk.marcusrokatis

import dk.marcusrokatis.data.fallback.DEFAULT_ENTRIES
import org.openpdf.text.Chunk
import org.openpdf.text.Document
import org.openpdf.text.Font
import org.openpdf.text.PageSize
import org.openpdf.text.Paragraph
import org.openpdf.text.pdf.BaseFont
import org.openpdf.text.pdf.PdfWriter
import java.awt.Color
import java.io.FileOutputStream

fun main(rawArgs: Array<String>) {
    val args = rawArgs.toList()
    fun index(flag: String) = args.indexOf(flag).takeIf { it >= 0 }

    val sizes = index("--sizes")?.let { index -> parseSizes(args.drop(index + 1).takeWhile { !it.startsWith("--") }) }
        ?: listOf(13 to 13, 15 to 15, 17 to 17, 19 to 19)

    val attempts = index("--attempts")?.let { index -> args.getOrNull(index + 1)?.toIntOrNull() } ?: 800
    val outPuzzles = index("--out")?.let { index -> args.getOrNull(index + 1) } ?: "puzzles.pdf"
    val outSolutions = index("--solutions")?.let { index -> args.getOrNull(index + 1) } ?: "solutions.pdf"
    val outCombined = index("--combined")?.let { index -> args.getOrNull(index + 1) } ?: "combined.pdf"
    val yamlPath = index("--wordlist")?.let { index -> args.getOrNull(index + 1) } ?: "GPT-WordList.yml"

    val minLength = index("--minLen")?.let { index -> args.getOrNull(index + 1)?.toIntOrNull() } ?: 2
    val maxLength = index("--maxLen")?.let { index -> args.getOrNull(index + 1)?.toIntOrNull() } ?: 12
    val noClues = args.any { it == "--noClues" }
    val arrows = index("--arrows")?.let { index -> args.getOrNull(index + 1)?.lowercase() in listOf("1", "true", "yes") } ?: true
    val arrowStyle = index("--arrowStyle")?.let { index -> args.getOrNull(index + 1)?.lowercase() }?.takeIf { it in listOf("arrow", "tri") } ?: "arrow"

    val clueFontSize = index("--clueFont")?.let { index -> args.getOrNull(index + 1)?.toFloatOrNull() } ?: 6.0f
    val letterFontSize = index("--letterFont")?.let { index -> args.getOrNull(index + 1)?.toFloatOrNull() } ?: 8.0f
    val grayValue = index("--gray")?.let { index -> args.getOrNull(index + 1)?.toIntOrNull()?.coerceIn(0, 255) } ?: 230
    val answerGray = Color(grayValue, grayValue, grayValue)

    // Debug parametre
    val debug = args.any { it == "--debug" }
    val showAllCells = args.any { it == "--showAllCells" }
    val noMinimize = args.any { it == "--noMinimize" }
    val allowDuplicateCrosswords = args.any { it == "--allowDuplicateCrosswords" }

    val clueEntries = try {
        loadClueEntriesFromYamlKaml(yamlPath)
    } catch (exception: Exception) {
        println("Kunne ikke læse YAML (${exception.message}). Bruger indbygget ordliste.")
        DEFAULT_ENTRIES
    }

    val providedSeed: Long? = index("--seed")?.let { index -> args.getOrNull(index + 1)?.toLongOrNull() }
    val baseSeedMillis: Long = providedSeed ?: System.currentTimeMillis()
    val baseSeedInt: Int = mixToIntSeed(baseSeedMillis)

    println("Seed: $baseSeedMillis ${if (providedSeed == null) "(auto)" else "(fixed)"}")
    
    val gridsRaw = sizes.mapIndexed { gridIndex, (width, height) ->
        val seedForThisGrid = baseSeedInt + gridIndex * 100_000 // stabil offset per grid
        buildCrossword(clueEntries, width, height, attempts, minLength, maxLength, seedBase = seedForThisGrid).let { if (noMinimize) it else it.minimized }
    }
    val grids = if (allowDuplicateCrosswords) gridsRaw else gridsRaw.distinctBy { it.cells }

    if (debug) grids.forEachIndexed { gridIndex, grid ->
        println("Gitter #$gridIndex (${grid.width}x${grid.height}) bruger bredde ${grid.usedWidth} og højde ${grid.usedHeight}. Udnyttelse: ${(grid.utilization * 100).withDigits(2)}%. Hashcode: ${grid.hashCode()}") }

    // --- Opret basefonte til clues og bogstaver (Courier; monospaced; med ÆØÅ) ---
    val clueBaseFont   = BaseFont.createFont(BaseFont.COURIER, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED)
    val letterBaseFont = clueBaseFont

// Puzzles
    Document(PageSize.A4).use { document ->
        PdfWriter.getInstance(document, FileOutputStream(outPuzzles))
        document.open()
        grids.forEachIndexed { gridIndex, grid ->
            document.add(Paragraph("Automatisk krydsord – ${grid.width}×${grid.height}", Font(Font.HELVETICA, 16f, Font.BOLD)))
            document.add(Chunk.NEWLINE)
            addGridTable(
                document = document,
                grid = grid,
                showLetters = false,
                showAllCells = showAllCells,
                clueFontSize = clueFontSize,
                letterFontSize = letterFontSize,
                answerGray = answerGray,
                arrows = arrows,
                arrowStyle = arrowStyle,
                clueBaseFont = clueBaseFont,
                letterBaseFont = letterBaseFont
            )
            if (!noClues) {
                document.add(Chunk.NEWLINE)
                val used = grid.usedPairs.map { it.clue }.toSet()
                    .sortedWith(compareBy<String> { it.length }.thenBy { it })
                document.add(Paragraph("Stikord brugt på denne side", Font(Font.HELVETICA, 10f, Font.BOLD)))
                document.add(Paragraph(used.joinToString(", "), Font(Font.HELVETICA, 9f)))
            }
            if (gridIndex != grids.lastIndex) document.newPage()
        }
        document.close()
    }

// Solutions
    Document(PageSize.A4).use { document ->
        PdfWriter.getInstance(document, FileOutputStream(outSolutions))
        document.open()
        grids.forEachIndexed { gridIndex, grid ->
            document.add(Paragraph("Løsning – ${grid.width}×${grid.height}", Font(Font.HELVETICA, 16f, Font.BOLD)))
            document.add(Chunk.NEWLINE)
            addGridTable(
                document = document,
                grid = grid,
                showLetters = true,
                showAllCells = showAllCells,
                clueFontSize = clueFontSize,
                letterFontSize = letterFontSize,
                answerGray = answerGray,
                arrows = arrows,
                arrowStyle = arrowStyle,
                clueBaseFont = clueBaseFont,
                letterBaseFont = letterBaseFont
            )
            if (gridIndex != grids.lastIndex) document.newPage()
        }
        document.close()
    }

// Combined: alle krydsord først, derefter alle løsninger
    Document(PageSize.A4).use { document ->
        PdfWriter.getInstance(document, FileOutputStream(outCombined))
        document.open()
        // Puzzles section
        grids.forEachIndexed { gridIndex, grid ->
            document.add(Paragraph("Automatisk krydsord – ${grid.width}×${grid.height}", Font(Font.HELVETICA, 16f, Font.BOLD)))
            document.add(Chunk.NEWLINE)
            addGridTable(
                document = document,
                grid = grid,
                showLetters = false,
                showAllCells = showAllCells,
                clueFontSize = clueFontSize,
                letterFontSize = letterFontSize,
                answerGray = answerGray,
                arrows = arrows,
                arrowStyle = arrowStyle,
                clueBaseFont = clueBaseFont,
                letterBaseFont = letterBaseFont
            )
            if (!noClues) {
                document.add(Chunk.NEWLINE)
                val used = grid.usedPairs.map { it.clue }.toSet()
                    .sortedWith(compareBy<String> { it.length }.thenBy { it })
                document.add(Paragraph("Stikord brugt på denne side", Font(Font.HELVETICA, 10f, Font.BOLD)))
                document.add(Paragraph(used.joinToString(", "), Font(Font.HELVETICA, 9f)))
            }
            if (gridIndex != grids.lastIndex) document.newPage()
        }
        // Solutions section (ny side før)
        document.newPage()
        grids.forEachIndexed { gridIndex, grid ->
            document.add(Paragraph("Løsning – ${grid.width}×${grid.height}", Font(Font.HELVETICA, 16f, Font.BOLD)))
            document.add(Chunk.NEWLINE)
            addGridTable(
                document = document,
                grid = grid,
                showLetters = true,
                showAllCells = showAllCells,
                clueFontSize = clueFontSize,
                letterFontSize = letterFontSize,
                answerGray = answerGray,
                arrows = arrows,
                arrowStyle = arrowStyle,
                clueBaseFont = clueBaseFont,
                letterBaseFont = letterBaseFont
            )
            if (gridIndex != grids.lastIndex) document.newPage()
        }
        document.close()
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

fun mixToIntSeed(longSeed: Long): Int = (longSeed xor (longSeed ushr 32)).toInt()

// try-with-resources helper
inline fun <T: Document, R> T.use(block: (T) -> R): R {
    try { return block(this) } finally { if (this.isOpen) this.close() }
}