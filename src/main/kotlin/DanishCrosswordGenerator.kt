@file:JvmName("DanishCrosswordGenerator")

package dk.marcusrokatis

import org.openpdf.text.Document

fun main(rawArgs: Array<String>) {
    val args = rawArgs.toList()
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