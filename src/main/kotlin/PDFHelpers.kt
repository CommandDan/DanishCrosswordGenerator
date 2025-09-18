package dk.marcusrokatis

import dk.marcusrokatis.data.*
import dk.marcusrokatis.data.Cell
import org.openpdf.text.*
import org.openpdf.text.pdf.BaseFont
import org.openpdf.text.pdf.PdfPCell
import org.openpdf.text.pdf.PdfPTable
import kotlin.math.max
import kotlin.math.min

fun estimateLinesWithFont(
    text: String,
    baseFont: BaseFont,
    fontSize: Float,
    columnWidth: Float,
    padding: Float
): Int {
    // Reél tegnbredde i punkter (Courier: samme for alle tegn)
    val charWidth = baseFont.getWidthPoint("M", fontSize).coerceAtLeast(1f)

    val usable = (columnWidth - 2 * padding).coerceAtLeast(1f)
    val maxCharsPerLine = maxOf(1, kotlin.math.floor(usable / charWidth).toInt())

    // Simpel ord-wrapping estimering
    val words = text.toWords()
    var lineLength = 0
    var lines = 1
    for (word in words) {
        val add = (if (lineLength == 0) 0 else 1) + word.length // +1 for mellemrum, hvis ikke linjen er tom
        if (lineLength + add <= maxCharsPerLine) {
            lineLength += add
        } else {
            lines++
            lineLength = word.length
            // hårdt break hvis ét ord overstiger maxCharsPerLine
            if (word.length > maxCharsPerLine) {
                val extra = (word.length - 1) / maxCharsPerLine
                lines += extra
                lineLength = word.length % maxCharsPerLine
                if (lineLength == 0) lineLength = maxCharsPerLine
            }
        }
    }
    return lines
}

fun autoClueParagraph(
    text: String,
    baseFont: BaseFont,
    baseFontSize: Float,
    columnWidth: Float,
    cellHeight: Float,
    padding: Float
): Paragraph {
    var fontSize = baseFontSize
    val minFontSize = 4.5f
    val leadingFactor = 1.1f

    while (fontSize >= minFontSize) {
        val lines = estimateLinesWithFont(text, baseFont, fontSize, columnWidth, padding)
        val neededHeight = lines * (fontSize * leadingFactor) + 2 * padding
        if (neededHeight <= cellHeight + 0.01f) {
            val paragraph = Paragraph(text, Font(baseFont, fontSize, Font.NORMAL))
            paragraph.leading = fontSize * leadingFactor
            return paragraph
        }
        fontSize -= 0.3f
    }
    // fallback: mindste skrift
    val paragraph = Paragraph(text, Font(baseFont, minFontSize, Font.NORMAL))
    paragraph.leading = minFontSize * leadingFactor
    return paragraph
}

fun chooseLetterFontSize(base: Float, cellHeight: Float, padding: Float): Float {
    // Approximate cap height ~ 0.7 * font size; aim to fit with leading
    val maxByHeight = (cellHeight - 2 * padding) / 0.9f
    return max(4.5f, min(base, maxByHeight))
}

fun addGridTable(
    document: Document,
    grid: Grid,
    showLetters: Boolean,
    styleParameters: StyleParameters,
    fontParameters: FontParameters,
    debugParameters: DebugParameters
) {
    val availableWidth = document.pageSize.width - document.leftMargin() - document.rightMargin()
    val table = PdfPTable(grid.width).apply {
        totalWidth = availableWidth
        isLockedWidth = true
        widthPercentage = 100f
    }

    val fancyRight = '→'
    val fancyDown  = '↓'
    val triRight   = '▸'
    val triDown    = '▾'

    val canFancy = fontParameters.clueBaseFont.supports(fancyRight) && fontParameters.clueBaseFont.supports(fancyDown)
    val canTri   = fontParameters.clueBaseFont.supports(triRight)   && fontParameters.clueBaseFont.supports(triDown)

    val padding = 1f
    val (horizontalArrow, verticalArrow) = when (styleParameters.arrowStyle) {
            "tri" if canTri -> "▸ " to "▾ "
            "arrow" if canFancy -> "→ " to "↓ "
            else -> "> " to "v "
        }

    val columnWidth = availableWidth / grid.width
    val cellSize = columnWidth // square cells: height == width

    fun cellFor(cell: Cell): PdfPCell {
        val pdfCell = when (cell) {
            is Clue -> {
                val arrow = if (styleParameters.arrows) if (cell.clueInfo.dir == 'H') horizontalArrow else verticalArrow else ""
                val paragraph = autoClueParagraph(
                    text = arrow + cell.clueInfo.clueText,
                    baseFont = fontParameters.clueBaseFont,
                    baseFontSize = fontParameters.clueFontSize,
                    columnWidth = columnWidth,
                    cellHeight = cellSize,
                    padding = padding
                )
                PdfPCell(paragraph).apply {
                    horizontalAlignment = Element.ALIGN_CENTER
                    verticalAlignment = Element.ALIGN_MIDDLE
                }
            }
            is Letter -> {
                val fontSize = chooseLetterFontSize(fontParameters.letterFontSize, cellSize, padding)
                val text = if (showLetters) cell.letter.toString() else ""
                val paragraph = Paragraph(text, Font(fontParameters.letterBaseFont, fontSize, Font.NORMAL))
                PdfPCell(paragraph).apply {
                    backgroundColor = styleParameters.answerGray
                    horizontalAlignment = Element.ALIGN_CENTER
                    verticalAlignment = Element.ALIGN_MIDDLE
                }
            }
            is Block -> if (debugParameters.showAllCells) PdfPCell(Paragraph("")) else PdfPCell().apply { border = Rectangle.NO_BORDER }
        }
        pdfCell.fixedHeight = cellSize
        pdfCell.padding = padding
        if (cell !is Block || debugParameters.showAllCells) pdfCell.border = Rectangle.BOX else pdfCell.border = Rectangle.NO_BORDER
        return pdfCell
    }

    for (row in 0 until grid.height) {
        for (column in 0 until grid.width) {
            table.addCell(cellFor(grid.cells[row][column]))
        }
    }
    document.add(table)
}

fun Document.addGridPages(
    pageParameters: PageParameters,
    crosswordParameters: CrosswordParameters,
    styleParameters: StyleParameters,
    fontParameters: FontParameters,
    debugParameters: DebugParameters
) {
    crosswordParameters.grids.forEachIndexed { gridIndex, grid ->
        this.add(Paragraph("${pageParameters.titlePrefix} – ${grid.width}×${grid.height} (Seed: ${crosswordParameters.seed})", Font(Font.HELVETICA, 16f, Font.BOLD)))
        this.add(Chunk.NEWLINE)
        addGridTable(
            document = this,
            grid = grid,
            showLetters = pageParameters.showLetters,
            styleParameters = styleParameters,
            fontParameters = fontParameters,
            debugParameters = debugParameters
        )
        if (pageParameters.includeCluesBelowGrid) {
            this.add(Chunk.NEWLINE)
            val used = grid.usedPairs.map { it.clue }.toSet().sortedWith(compareBy<String> { it.length }.thenBy { it })
            this.add(Paragraph("Stikord brugt på denne side", Font(Font.HELVETICA, 10f, Font.BOLD)))
            this.add(Paragraph(used.joinToString(", "), Font(Font.HELVETICA, 9f)))
        }
        if (gridIndex != crosswordParameters.grids.lastIndex) this.newPage()
    }
}