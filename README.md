# Danish Crossword Generator

Et lille CLI-værktøj, der genererer svensk-inspirerede krydsord (clue i feltet før ordet) som PDF.
Understøtter danske bogstaver (ÆØÅ), autoskalering af tekst i ledetrådsfelter, valgfri pile (→/↓ eller ▸/▾), samt både opgave- og løsnings-PDF – og en kombineret PDF.

Indhold
•	Krav
•	Build & kørsel
•	Kommando-linjeparametre
•	Eksempler
•	YAML-ordlister
•	Fonte (PDF/OpenPDF)
•	Reproducerbarhed (seed & builds)
•	Fejlfinding

⸻

Krav
•	JDK 17+ (testet med JDK 23)
•	Gradle wrapper følger med (brug ./gradlew)
•	Platform: macOS, Linux, Windows

⸻

Build & kørsel

Hurtig kørsel under udvikling

./gradlew run --args="--wordlist wordlist.yml --sizes 13x13 15x15"

Installerbar distribution (scripts + libs)

./gradlew installDist
# Kør:
build/install/DanishCrosswordGenerator/bin/DanishCrosswordGenerator --sizes 13x13 15x15

Eller lav zip til deling:

./gradlew distZip
# -> build/distributions/DanishCrosswordGenerator-<version>.zip

Én kørbar “fat JAR” (Shadow)

./gradlew shadowJar
java -jar build/libs/DanishCrosswordGenerator-<version>-all.jar --sizes 13x13 15x15

Tip (IntelliJ IDEA):
•	Application run config: udfyld feltet Program arguments.
•	Gradle run config: skriv run --args="--sizes 13x13 --seed 123" i feltet Arguments.

⸻

Kommando-linjeparametre

Flag	Type	Default	Beskrivelse
--sizes W×H [W×H ...]	liste	13x13 15x15 17x17 19x19	Liste af gitterstørrelser (bredde×højde).
--attempts N	int	800	Antal genereringsforsøg pr. gitter (vælger bedste udnyttelse).
--wordlist PATH	fil	GPT-WordList.yml	YAML-ordliste med danske ord og ledetråde.
--minLen N	int	2	Mindste ordlængde der må bruges.
--maxLen N	int	12	Største ordlængde der må bruges.
--out FILE	fil	puzzles.pdf	Opgave-PDF (uden bogstaver).
--solutions FILE	fil	solutions.pdf	Løsnings-PDF (med bogstaver).
--combined FILE	fil	combined.pdf	Kombineret PDF: først alle opgaver, derefter alle løsninger.
--noClues	flag	(fra)	Skjul “Stikord brugt på denne side” under gitteret.
`–arrows true	false`	bool	true
`–arrowStyle arrow	tri`	enum	arrow
--clueFont F	float	6.0	Basestørrelse for ledetrådsskrift (autoskalering sørger for, at det passer i cellen).
--letterFont F	float	8.0	Basestørrelse for bogstavskrift i løsninger (autoskalering nedjusterer om nødvendigt).
--gray V	0..255	230	Gråtone for bogstavceller i løsning (jo lavere, jo mørkere).
--seed S	long	(clock)	Fast seed for fuld reproducerbarhed. Udelades = bruger System.currentTimeMillis().
--debug	flag	(fra)	Logger størrelser, udnyttelse m.m.
--showAllCells	flag	(fra)	Debug: vis også de ellers skjulte “Block”-felter.
--noMinimize	flag	(fra)	Spring efterfølgende “minimering”/oprunding af grid over (debug/eksperimentelt).
--allowDuplicateCrosswords	flag	(fra)	Tillad duplikerede grids. Uden flag filtreres dubletter væk på celle-layout.

Bemærk: Vi viser kun rammer omkring relevante felter (clue/letter). Ubrugte felter (“Block”) er usynlige, så print spares for “døde” områder.

⸻

Eksempler

Standardkørsel (4 størrelser, pile, autoskalering):

./gradlew run

Med ordliste og fast seed (reproducerbar):

./gradlew run --args="--wordlist wordlist.yml --sizes 13x13 15x15 --seed 1713412345678"

Uden pile og uden stikordsopsummering:

./gradlew run --args="--arrows false --noClues"

Skyggeskrift lysere og længere ord:

./gradlew run --args="--minLen 3 --maxLen 14 --gray 240"

Kør “fat JAR”:

java -jar build/libs/DanishCrosswordGenerator-<version>-all.jar --wordlist wordlist.yml --sizes 15x15


⸻

YAML-ordlister

Programmet læser YAML via kaml og understøtter to formater:

1) Map-format (anbefalet)

entries:
HUND: tamt dyr
KAT: kattedyr
ÆRT: lille grøntsag
Ø: lille ø
Å: lille vandløb

2) Liste-format

entries:
- word: HUND
  clue: tamt dyr
- word: KAT
  clue: kattedyr

  •	Ord normaliseres (Æ→AE, Ø→OE, Å→AA) internt til gitteret; ledetrådene vises som skrevet.
  •	Ugyldige ord (tomme/ikke-bogstaver) filtreres fra.

⸻

Fonte (PDF/OpenPDF)
•	Som standard bruges Courier (monospace) fra PDF’s “standard 14 fonts”:

BaseFont.createFont(BaseFont.COURIER, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED)

– WINANSI dækker ÆØÅ; NOT_EMBEDDED er påkrævet for standard 14.

	•	Vil du have en moderne monospace med fuld Unicode og embedding, så brug TTF/OTF (fx JetBrains Mono):

BaseFont.createFont("fonts/JetBrainsMono-Regular.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED)

Læg fontfilen i fx src/main/resources/fonts/ og læs den fra classpath/fil.

	•	Autoskalering:
Ledetrådsceller måler reel tegnbredde via BaseFont.getWidthPoint(...) → pænere linjebrud.
Bogstavceller i løsninger skalerer også automatisk så bogstavet altid passer i cellen.

⸻

Reproducerbarhed (seed & builds)
•	Seed: med --seed <long> får du samme layout for samme input.
Uden seed bruges System.currentTimeMillis() → ny variation hver kørsel.
•	Build-artefakter: projektet er sat op til reproducible JARs (stabil filorden, ingen timestamps), og Shadow genererer -all.jar.

PDF-bytes kan stadig afvige pga. metadata/tidsstempel fra PDF-motoren, men layoutet bliver ens for samme seed og input.

⸻

Fejlfinding

Process finished with non-zero exit value 1
•	Oftest fordi man forsøger at oprette Courier med IDENTITY_H + EMBEDDED.
Løsning: brug WINANSI + NOT_EMBEDDED (se Fonte).

“Kan ikke læse YAML”
•	Tjek stien fra --wordlist.
•	Sørg for at YAML har entries: øverst og er i ét af de to understøttede formater.

Grid løber ud over siden
•	Sæt færre rækker/kolonner i --sizes, brug A4 i landskab, eller reducer overskrifter/margener.
•	(Kan udbygges med “smart cellSize” der tager højde for sidehøjde, sig til hvis du vil have det indbygget.)

Duplikerede krydsord
•	Uden --allowDuplicateCrosswords filtrerer programmet dubletter væk (baseret på cellelayout).
•	Med flagget tillades dubletter (hurtigere).
