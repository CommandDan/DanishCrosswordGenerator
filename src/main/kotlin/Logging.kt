package dk.marcusrokatis

val logs = mutableListOf<String>()

fun log(message: String) {
    println(message)
    logs += message
}