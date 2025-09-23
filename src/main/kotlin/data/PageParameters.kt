package dk.marcusrokatis.data

import org.openpdf.text.pdf.BaseFont
import java.awt.Color

data class PageParameters(
    val titlePrefix: String,
    val showLetters: Boolean,
    val includeCluesBelowGrid: Boolean
)

data class CrosswordParameters(
    val grids: List<Grid>,
    val seed: Long
)

data class StyleParameters(
    val answerGray: Color,
    val clueGray: Color,
    val arrows: Boolean,
    val arrowStyle: String,
    val fillUnused: Boolean,
    val unusedGray: Color
)

data class FontParameters(
    val clueFontSize: Float,
    val letterFontSize: Float,
    val clueBaseFont: BaseFont,
    val letterBaseFont: BaseFont
)

data class DebugParameters(
    val showAllCells: Boolean
)