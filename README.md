# DanishCrosswordGenerator

Et Kotlin-baseret værktøj til at generere **danske krydsord** som PDF-filer.  
Projektet understøtter både **opgaver** (uden bogstaver) og **løsninger** (med bogstaver), samt en kombineret PDF hvor alle opgaverne står først og løsningerne bagefter.

---

## 🛠️ Bygning

Projektet bruger **Gradle** og kræver en JDK (Java 17 eller nyere anbefales).

Kør i roden af projektet:

```bash
./gradlew build
```

For at bygge en **fat jar** med alle afhængigheder:

```bash
./gradlew shadowJar
```

Artefakter findes herefter i `build/libs/`.

---

## ▶️ Kørsel

Programmet kan køres direkte via Gradle:

```bash
./gradlew run --args="--wordlist minordliste.yml --out minepuzzles.pdf"
```

Eller med JAR-filen:

```bash
java -jar build/libs/DanishCrosswordGenerator-all.jar --wordlist minordliste.yml
```

---

## ⚙️ Parametre

| Flag                         | Beskrivelse                                           | Default                      |
|------------------------------|-------------------------------------------------------|------------------------------|
| `--sizes`                    | Liste af gitterstørrelser, fx `13x13 15x15`           | `13x13 15x15 17x17 19x19`    |
| `--attempts`                 | Hvor mange forsøg på at placere ord per gitter        | `800`                        |
| `--out`                      | Filnavn for PDF med krydsord                          | `puzzles.pdf`                |
| `--solutions`                | Filnavn for PDF med løsninger                         | `solutions.pdf`              |
| `--combined`                 | Filnavn for PDF med kombineret opgaver+ løsninger     | `combined.pdf`               |
| `--wordlist`                 | Sti til YAML-ordliste                                 | `GPT-WordList.yml`           |
| `--minLength`                | Minimum ordlængde                                     | `2`                          |
| `--maxLength`                | Maksimum ordlængde                                    | `12`                         |
| `--addMeanings`              | Om synonymer/betydninger skal tilføjes (`true/false`) | `true`                       |
| `--maxUseWord`               | Maksimum brug af ord                                  | `2`                          |
| `--maxUseClue`               | Maksimum brug af ledetråd                             | `5`                          |
| `--noClues`                  | Skjul ledetråde under gitteret                        | `false`                      |
| `--arrows`                   | Vis pile (`true/false`)                               | `true`                       |
| `--arrowStyle`               | Pilestil: `arrow` eller `tri`                         | `arrow`                      |
| `--fillUnused`               | Om tomme celler skal fyldes                           | `false`                      |
| `--unusedGray`               | Gråtone for ubrugte celler (0–255)                    | `0`                          |
| `--clueFont`                 | Fontstørrelse for ledetråde                           | `6.0`                        |
| `--letterFont`               | Fontstørrelse for bogstaver                           | `8.0`                        |
| `--answerGray`               | Gråtone for svarfelter (0–255)                        | `230`                        |
| `--clueGray`                 | Gråtone for ledetrådsfelter (0–255)                   | `200`                        |
| `--debug`                    | Print ekstra debug-information til log                | `false`                      |
| `--showAllCells`             | Vis også tomme celler (for debugging)                 | `false`                      |
| `--noMinimize`               | Undgå at trimme gitteret til brugt område             | `false`                      |
| `--allowDuplicateCrosswords` | Tillad at identiske gitter beholdes                   | `false`                      |
| `--addParametersPage`        | Tilføj en side i PDF’en med de valgte parametre       | `false`                      |
| `--addLogsPage`              | Tilføj en side i PDF’en med log-output                | `false`                      |
| `--seed`                     | Fast seed for deterministisk output                   | `System.currentTimeMillis()` |

---

## 📄 YAML ordlister

Ordlister angives i YAML. Projektet understøtter både **én ledetråd pr. ord** og **flere ledetråde pr. ord**:

```yaml
entries:
  HUND: tamt dyr
  KAT:
    - kattedyr
    - husdyr
  ÆRT: lille grøntsag
```

---

## 🖼️ Output

- **Puzzles PDF**: Krydsord uden bogstaver
- **Solutions PDF**: Samme gittere med bogstaver udfyldt
- **Combined PDF**: Alle krydsord efterfulgt af alle løsninger

---

## 📜 Licens

Dette projekt bruger [OpenPDF](https://github.com/LibrePDF/OpenPDF) (LGPL/MPL) og [kaml](https://github.com/charleskorn/kaml) til YAML.  
Selve generatoren er MIT-licenseret (medmindre du ændrer det).