package dk.marcusrokatis.data.fallback

import dk.marcusrokatis.data.ClueEntry

val DEFAULT_ENTRIES: List<ClueEntry> = listOf(
    ClueEntry("KAT","kattedyr"), ClueEntry("HUND","hundedyr"), ClueEntry("KO","kvægdyr"),
    ClueEntry("SOL","stjerne"), ClueEntry("MÅNEN","natlys"), ClueEntry("STJERNE","lys på himlen"),
    ClueEntry("HAV","saltvand"), ClueEntry("SØ","ferskvand"), ClueEntry("REGN","nedbør"),
    ClueEntry("BY","bebyggelse"), ClueEntry("PARK","offentligt område"), ClueEntry("BIL","køretøj"),
    ClueEntry("CYKEL","køretøj med pedaler"), ClueEntry("BUS","offentligt køretøj")
)