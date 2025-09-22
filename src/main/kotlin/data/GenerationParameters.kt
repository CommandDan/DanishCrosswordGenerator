package dk.marcusrokatis.data

import dk.marcusrokatis.IntPair

data class GenerationParameters(
    val crosswordParameters: GenerationCrosswordParameters,
    val fileParameters: GenerationFileParameters,
    val styleParameters: GenerationStyleParameters,
    val fontParameters: GenerationFontParameters,
    val colorParameters: GenerationColorParameters,
    val debugParameters: GenerationDebugParameters
)

data class GenerationCrosswordParameters(
    val sizes: List<IntPair>,
    val attempts: Int,
    val minLength: Int,
    val maxLength: Int,
    val addMeanings: Boolean
)

data class GenerationFileParameters(
    val puzzlesFile: String,
    val solutionsFile: String,
    val combinedFile: String,
    val wordListFile: String
)

data class GenerationStyleParameters(
    val noClues: Boolean,
    val arrows: Boolean,
    val arrowStyle: String
)

data class GenerationFontParameters(
    val clueFontSize: Float,
    val letterFontSize: Float
)

data class GenerationColorParameters(
    val answerGrayValue: Int,
    val clueGrayValue: Int
)

data class GenerationDebugParameters(
    val debug: Boolean,
    val showAllCells: Boolean,
    val noMinimize: Boolean,
    val allowDuplicateCrosswords: Boolean
)