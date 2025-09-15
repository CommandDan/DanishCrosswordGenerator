package dk.marcusrokatis.data

data class Synonym(val word: String, val relatedWord: String)

data class Synonyms(val synonyms: List<Synonym>)