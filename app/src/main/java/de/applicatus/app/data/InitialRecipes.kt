package de.applicatus.app.data

import de.applicatus.app.data.model.potion.Laboratory
import de.applicatus.app.data.model.potion.Recipe

/**
 * Initiale Rezepte für die Datenbank
 * Basierend auf Rezepte.csv
 */
object InitialRecipes {
    fun getDefaultRecipes(): List<Recipe> = listOf(
        // Inanimatica
        Recipe(
            name = "Ewiges Öl",
            gruppe = "Inanimatica",
            lab = Laboratory.WITCHES_KITCHEN,
            preis = 50,
            zutatenPreis = 25,
            zutatenVerbreitung = 6,
            verbreitung = 2,
            brewingDifficulty = 6,
            analysisDifficulty = 2
        ),
        Recipe(
            name = "Hexensalbe",
            gruppe = "Inanimatica",
            lab = Laboratory.WITCHES_KITCHEN,
            preis = null, // unverkäuflich
            zutatenPreis = null, // unbekannt
            zutatenVerbreitung = 0,
            verbreitung = 15, // * = sehr verbreitet
            brewingDifficulty = 0, // unbekannt
            analysisDifficulty = 0
        ),
        Recipe(
            name = "Hylailer Feuer",
            gruppe = "Inanimatica",
            lab = Laboratory.LABORATORY,
            preis = 30,
            zutatenPreis = 15,
            zutatenVerbreitung = 8,
            verbreitung = 4,
            brewingDifficulty = 6,
            analysisDifficulty = 5
        ),
        Recipe(
            name = "Magisches Brandöl",
            gruppe = "Inanimatica",
            lab = Laboratory.LABORATORY,
            preis = null, // unverkäuflich
            zutatenPreis = 50,
            zutatenVerbreitung = 7,
            verbreitung = 2,
            brewingDifficulty = 12,
            analysisDifficulty = 18
        ),
        Recipe(
            name = "Sonnenlicht-Elixier",
            gruppe = "Inanimatica",
            lab = Laboratory.LABORATORY,
            preis = 100,
            zutatenPreis = 50,
            zutatenVerbreitung = 5,
            verbreitung = 1,
            brewingDifficulty = 7,
            analysisDifficulty = 4
        ),
        Recipe(
            name = "Stabilisatum",
            gruppe = "Inanimatica",
            lab = Laboratory.WITCHES_KITCHEN,
            preis = 120,
            zutatenPreis = 60,
            zutatenVerbreitung = 5,
            verbreitung = 3,
            brewingDifficulty = 9,
            analysisDifficulty = 4
        ),
        Recipe(
            name = "Waffenbalsam",
            gruppe = "Inanimatica",
            lab = Laboratory.WITCHES_KITCHEN,
            preis = 35,
            zutatenPreis = 18,
            zutatenVerbreitung = 8,
            verbreitung = 5,
            brewingDifficulty = 6,
            analysisDifficulty = 3
        ),
        
        // keine Gruppe
        Recipe(
            name = "Eulentränen",
            gruppe = "keine",
            lab = Laboratory.WITCHES_KITCHEN,
            preis = 1, // 0,1 Dukaten → 1 Zehntel
            zutatenPreis = 1, // 0,05 → gerundet auf 1
            zutatenVerbreitung = 12,
            verbreitung = 7,
            brewingDifficulty = 1,
            analysisDifficulty = 3
        ),
        Recipe(
            name = "Geheimtinte",
            gruppe = "keine",
            lab = Laboratory.WITCHES_KITCHEN,
            preis = 1, // <1
            zutatenPreis = 1, // <0,5
            zutatenVerbreitung = 13,
            verbreitung = 5,
            brewingDifficulty = 0, // var.
            analysisDifficulty = 0
        ),
        Recipe(
            name = "Kaltes Licht",
            gruppe = "keine",
            lab = Laboratory.ARCANE,
            preis = 1,
            zutatenPreis = 1, // 0,5
            zutatenVerbreitung = 12,
            verbreitung = 3,
            brewingDifficulty = 2,
            analysisDifficulty = 6
        ),
        Recipe(
            name = "Pyrophor",
            gruppe = "keine",
            lab = Laboratory.ARCANE,
            preis = 1, // <0,2
            zutatenPreis = 1, // 0,05
            zutatenVerbreitung = 10,
            verbreitung = 4,
            brewingDifficulty = 0,
            analysisDifficulty = 1
        ),
        Recipe(
            name = "Schattenkreide",
            gruppe = "keine",
            lab = Laboratory.WITCHES_KITCHEN,
            preis = 5,
            zutatenPreis = 3, // 2,5 → gerundet
            zutatenVerbreitung = 9,
            verbreitung = 3, // 3 /5
            brewingDifficulty = 2,
            analysisDifficulty = 2
        ),
        Recipe(
            name = "Schwadenbeutel",
            gruppe = "keine",
            lab = Laboratory.ARCANE,
            preis = 2,
            zutatenPreis = 1,
            zutatenVerbreitung = 8,
            verbreitung = 6,
            brewingDifficulty = 4,
            analysisDifficulty = 1
        ),
        Recipe(
            name = "Stinktöpfchen",
            gruppe = "keine",
            lab = Laboratory.ARCANE,
            preis = 1, // 0,3
            zutatenPreis = 1, // 0,15
            zutatenVerbreitung = 9,
            verbreitung = 6,
            brewingDifficulty = 0,
            analysisDifficulty = 1
        ),
        Recipe(
            name = "Tränenkraut",
            gruppe = "keine",
            lab = Laboratory.ARCANE,
            preis = 1,
            zutatenPreis = 1, // 0,5
            zutatenVerbreitung = 6,
            verbreitung = 6,
            brewingDifficulty = 2,
            analysisDifficulty = 3
        ),
        Recipe(
            name = "Wundpulver",
            gruppe = "keine",
            lab = Laboratory.ARCANE,
            preis = 1, // <0,5
            zutatenPreis = 1, // <0,25
            zutatenVerbreitung = 10,
            verbreitung = 7,
            brewingDifficulty = 0,
            analysisDifficulty = 2
        ),
        
        // Magika
        Recipe(
            name = "Bannpulver gg. Geister",
            gruppe = "Magika",
            lab = Laboratory.ARCANE,
            preis = 15,
            zutatenPreis = 8,
            zutatenVerbreitung = 9,
            verbreitung = 5,
            brewingDifficulty = 3,
            analysisDifficulty = 4
        ),
        Recipe(
            name = "Bannpulver gg. Unsichtbares",
            gruppe = "Magika",
            lab = Laboratory.LABORATORY,
            preis = 80,
            zutatenPreis = 40,
            zutatenVerbreitung = 10,
            verbreitung = 4,
            brewingDifficulty = 6,
            analysisDifficulty = 7
        ),
        Recipe(
            name = "Beschwörungskerzen",
            gruppe = "Magika",
            lab = Laboratory.ARCANE,
            preis = 5,
            zutatenPreis = 3, // 2,5
            zutatenVerbreitung = 10,
            verbreitung = 6,
            brewingDifficulty = 3,
            analysisDifficulty = 0
        ),
        Recipe(
            name = "Borbarads Hauch",
            gruppe = "Magika",
            lab = Laboratory.LABORATORY,
            preis = 25,
            zutatenPreis = 13,
            zutatenVerbreitung = 3,
            verbreitung = 3,
            brewingDifficulty = 7,
            analysisDifficulty = 18
        ),
        Recipe(
            name = "Geisterelixier",
            gruppe = "Magika",
            lab = Laboratory.ARCANE,
            preis = 30,
            zutatenPreis = 15,
            zutatenVerbreitung = 7,
            verbreitung = 3,
            brewingDifficulty = 3,
            analysisDifficulty = 4
        ),
        Recipe(
            name = "Retro-Elixier",
            gruppe = "Magika",
            lab = Laboratory.LABORATORY,
            preis = 80, // >80
            zutatenPreis = 40,
            zutatenVerbreitung = 2,
            verbreitung = 1,
            brewingDifficulty = 7,
            analysisDifficulty = 8
        ),
        Recipe(
            name = "Zauberkreide",
            gruppe = "Magika",
            lab = Laboratory.ARCANE,
            preis = 5,
            zutatenPreis = 3, // 2,5
            zutatenVerbreitung = 13,
            verbreitung = 7,
            brewingDifficulty = 2,
            analysisDifficulty = 1
        ),
        Recipe(
            name = "Zaubertrank",
            gruppe = "Magika",
            lab = Laboratory.WITCHES_KITCHEN,
            preis = 60, // >60
            zutatenPreis = 30,
            zutatenVerbreitung = 6,
            verbreitung = 5,
            brewingDifficulty = 8,
            analysisDifficulty = 4
        ),
        
        // Mutandica
        Recipe(
            name = "Elixier gegen Verfall",
            gruppe = "Mutandica",
            lab = Laboratory.LABORATORY,
            preis = 400,
            zutatenPreis = 200,
            zutatenVerbreitung = 4,
            verbreitung = 1,
            brewingDifficulty = 15,
            analysisDifficulty = 8
        ),
        Recipe(
            name = "Hauch der Jugend",
            gruppe = "Mutandica",
            lab = Laboratory.WITCHES_KITCHEN,
            preis = 60,
            zutatenPreis = 30,
            zutatenVerbreitung = 7,
            verbreitung = 3,
            brewingDifficulty = 7,
            analysisDifficulty = 4
        ),
        Recipe(
            name = "Ifirnstrunk",
            gruppe = "Mutandica",
            lab = Laboratory.WITCHES_KITCHEN,
            preis = 35,
            zutatenPreis = 18,
            zutatenVerbreitung = 7,
            verbreitung = 4,
            brewingDifficulty = 4,
            analysisDifficulty = 4
        ),
        Recipe(
            name = "Levitationselixier",
            gruppe = "Mutandica",
            lab = Laboratory.LABORATORY,
            preis = 70,
            zutatenPreis = 35,
            zutatenVerbreitung = 7,
            verbreitung = 3, // 3/5
            brewingDifficulty = 6,
            analysisDifficulty = 0
        ),
        Recipe(
            name = "Purpurwasser",
            gruppe = "Mutandica",
            lab = Laboratory.ARCANE,
            preis = null, // unverkäuflich
            zutatenPreis = 40,
            zutatenVerbreitung = 1,
            verbreitung = 1,
            brewingDifficulty = 6,
            analysisDifficulty = 6
        ),
        Recipe(
            name = "Rethonikum",
            gruppe = "Mutandica",
            lab = Laboratory.LABORATORY,
            preis = 50,
            zutatenPreis = 25,
            zutatenVerbreitung = 6,
            verbreitung = 3,
            brewingDifficulty = 5,
            analysisDifficulty = 3
        ),
        Recipe(
            name = "Unsichtbarkeitselixier",
            gruppe = "Mutandica",
            lab = Laboratory.LABORATORY,
            preis = 60,
            zutatenPreis = 30,
            zutatenVerbreitung = 6,
            verbreitung = 4,
            brewingDifficulty = 10,
            analysisDifficulty = 6
        ),
        Recipe(
            name = "Unverwundbarkeitselixier",
            gruppe = "Mutandica",
            lab = Laboratory.ARCANE,
            preis = 35,
            zutatenPreis = 18,
            zutatenVerbreitung = 8,
            verbreitung = 5,
            brewingDifficulty = 5,
            analysisDifficulty = 2
        ),
        Recipe(
            name = "Verwandlungselixier",
            gruppe = "Mutandica",
            lab = Laboratory.WITCHES_KITCHEN,
            preis = 70,
            zutatenPreis = 35,
            zutatenVerbreitung = 8,
            verbreitung = 5,
            brewingDifficulty = 8,
            analysisDifficulty = 5
        ),
        Recipe(
            name = "Zurbarans Tinktur",
            gruppe = "Mutandica",
            lab = Laboratory.LABORATORY,
            preis = 150,
            zutatenPreis = 75,
            zutatenVerbreitung = 2,
            verbreitung = 1,
            brewingDifficulty = 10,
            analysisDifficulty = 7
        ),
        Recipe(
            name = "Zwergentrunk",
            gruppe = "Mutandica",
            lab = Laboratory.LABORATORY,
            preis = 80,
            zutatenPreis = 40,
            zutatenVerbreitung = 8,
            verbreitung = 4,
            brewingDifficulty = 6,
            analysisDifficulty = 4
        ),
        
        // Narkotika
        Recipe(
            name = "Berserkerelixier",
            gruppe = "Narkotika",
            lab = Laboratory.ARCANE,
            preis = 40,
            zutatenPreis = 20,
            zutatenVerbreitung = 8,
            verbreitung = 5,
            brewingDifficulty = 6,
            analysisDifficulty = 3
        ),
        Recipe(
            name = "Bestinoid",
            gruppe = "Narkotika",
            lab = Laboratory.LABORATORY,
            preis = 100,
            zutatenPreis = 50,
            zutatenVerbreitung = 4,
            verbreitung = 3,
            brewingDifficulty = 7,
            analysisDifficulty = 9
        ),
        Recipe(
            name = "Friedenswasser",
            gruppe = "Narkotika",
            lab = Laboratory.WITCHES_KITCHEN,
            preis = 50,
            zutatenPreis = 25,
            zutatenVerbreitung = 9,
            verbreitung = 3,
            brewingDifficulty = 4,
            analysisDifficulty = 4
        ),
        Recipe(
            name = "Hauch der Weissagung",
            gruppe = "Narkotika",
            lab = Laboratory.ARCANE,
            preis = 30,
            zutatenPreis = 15,
            zutatenVerbreitung = 6,
            verbreitung = 4,
            brewingDifficulty = 3,
            analysisDifficulty = 2
        ),
        Recipe(
            name = "Liebestrunk",
            gruppe = "Narkotika",
            lab = Laboratory.WITCHES_KITCHEN,
            preis = 40,
            zutatenPreis = 20,
            zutatenVerbreitung = 8,
            verbreitung = 5,
            brewingDifficulty = 5,
            analysisDifficulty = 4
        ),
        Recipe(
            name = "Mengbiller Bannbalöl",
            gruppe = "Narkotika",
            lab = Laboratory.LABORATORY,
            preis = 140,
            zutatenPreis = 70,
            zutatenVerbreitung = 4,
            verbreitung = 2,
            brewingDifficulty = 8,
            analysisDifficulty = 6
        ),
        Recipe(
            name = "Regenbogenstaub",
            gruppe = "Narkotika",
            lab = Laboratory.LABORATORY,
            preis = 50,
            zutatenPreis = 25,
            zutatenVerbreitung = 4,
            verbreitung = 4,
            brewingDifficulty = 6,
            analysisDifficulty = 2
        ),
        Recipe(
            name = "Traumwind-Elixier",
            gruppe = "Narkotika",
            lab = Laboratory.ARCANE,
            preis = 60,
            zutatenPreis = 30,
            zutatenVerbreitung = 8,
            verbreitung = 2, // 2/4
            brewingDifficulty = 2,
            analysisDifficulty = 4
        ),
        Recipe(
            name = "Wachtrunk",
            gruppe = "Narkotika",
            lab = Laboratory.ARCANE,
            preis = 15,
            zutatenPreis = 8,
            zutatenVerbreitung = 10,
            verbreitung = 3,
            brewingDifficulty = 1,
            analysisDifficulty = 3
        ),
        
        // Spagyrik
        Recipe(
            name = "Antidot",
            gruppe = "Spagyrik",
            lab = Laboratory.WITCHES_KITCHEN,
            preis = 50,
            zutatenPreis = 25,
            zutatenVerbreitung = 9,
            verbreitung = 5,
            brewingDifficulty = 5,
            analysisDifficulty = 4
        ),
        Recipe(
            name = "Furchtlos-Tropfen",
            gruppe = "Spagyrik",
            lab = Laboratory.ARCANE,
            preis = 20,
            zutatenPreis = 10,
            zutatenVerbreitung = 12,
            verbreitung = 4,
            brewingDifficulty = 3,
            analysisDifficulty = 3
        ),
        Recipe(
            name = "Heiltrank",
            gruppe = "Spagyrik",
            lab = Laboratory.ARCANE,
            preis = 10, // >10
            zutatenPreis = 5,
            zutatenVerbreitung = 14,
            verbreitung = 7,
            brewingDifficulty = 2,
            analysisDifficulty = 1
        ),
        Recipe(
            name = "Pastillen gg. Erschöpfung",
            gruppe = "Spagyrik",
            lab = Laboratory.WITCHES_KITCHEN,
            preis = 10,
            zutatenPreis = 5,
            zutatenVerbreitung = 11,
            verbreitung = 6,
            brewingDifficulty = 4,
            analysisDifficulty = 4
        ),
        Recipe(
            name = "Prophylaktikum gg. Kukris",
            gruppe = "Spagyrik",
            lab = Laboratory.LABORATORY,
            preis = 35,
            zutatenPreis = 18,
            zutatenVerbreitung = 4,
            verbreitung = 4,
            brewingDifficulty = 7,
            analysisDifficulty = 7
        ),
        Recipe(
            name = "Pulver des klaren Geistes",
            gruppe = "Spagyrik",
            lab = Laboratory.ARCANE,
            preis = 15,
            zutatenPreis = 8,
            zutatenVerbreitung = 14,
            verbreitung = 4,
            brewingDifficulty = 3,
            analysisDifficulty = 2
        ),
        Recipe(
            name = "Restorarium",
            gruppe = "Spagyrik",
            lab = Laboratory.LABORATORY,
            preis = 120,
            zutatenPreis = 60,
            zutatenVerbreitung = 3,
            verbreitung = 3,
            brewingDifficulty = 6,
            analysisDifficulty = 3
        ),
        Recipe(
            name = "Schlaftrunk",
            gruppe = "Spagyrik",
            lab = Laboratory.ARCANE,
            preis = 20,
            zutatenPreis = 10,
            zutatenVerbreitung = 10,
            verbreitung = 5,
            brewingDifficulty = 3,
            analysisDifficulty = 1
        ),
        
        // Venenik
        Recipe(
            name = "Angstgift",
            gruppe = "Venenik",
            lab = Laboratory.ARCANE,
            preis = 25,
            zutatenPreis = 13,
            zutatenVerbreitung = 8,
            verbreitung = 5,
            brewingDifficulty = 5,
            analysisDifficulty = 3
        ),
        Recipe(
            name = "Bannstaub",
            gruppe = "Venenik",
            lab = Laboratory.LABORATORY,
            preis = 250,
            zutatenPreis = 125,
            zutatenVerbreitung = 3,
            verbreitung = 3,
            brewingDifficulty = 10,
            analysisDifficulty = 7
        ),
        Recipe(
            name = "Boabungaha",
            gruppe = "Venenik",
            lab = Laboratory.ARCANE,
            preis = 250,
            zutatenPreis = 10,
            zutatenVerbreitung = 2,
            verbreitung = 2,
            brewingDifficulty = 12,
            analysisDifficulty = 7
        ),
        Recipe(
            name = "Drachenspeichel",
            gruppe = "Venenik",
            lab = Laboratory.LABORATORY,
            preis = 60,
            zutatenPreis = 30,
            zutatenVerbreitung = 7,
            verbreitung = 5,
            brewingDifficulty = 6,
            analysisDifficulty = 3
        ),
        Recipe(
            name = "Halbgift",
            gruppe = "Venenik",
            lab = Laboratory.WITCHES_KITCHEN,
            preis = 60,
            zutatenPreis = 30,
            zutatenVerbreitung = 7,
            verbreitung = 4,
            brewingDifficulty = 6,
            analysisDifficulty = 8
        ),
        Recipe(
            name = "Krötenhauch",
            gruppe = "Venenik",
            lab = Laboratory.WITCHES_KITCHEN,
            preis = 70,
            zutatenPreis = 35,
            zutatenVerbreitung = 7,
            verbreitung = 3,
            brewingDifficulty = 4,
            analysisDifficulty = 6
        ),
        Recipe(
            name = "Purpurblitz",
            gruppe = "Venenik",
            lab = Laboratory.LABORATORY,
            preis = 300,
            zutatenPreis = 50,
            zutatenVerbreitung = 3,
            verbreitung = 3,
            brewingDifficulty = 8,
            analysisDifficulty = 4
        ),
        Recipe(
            name = "Schlafgift",
            gruppe = "Venenik",
            lab = Laboratory.WITCHES_KITCHEN,
            preis = 35,
            zutatenPreis = 18,
            zutatenVerbreitung = 13,
            verbreitung = 6,
            brewingDifficulty = 5,
            analysisDifficulty = 4
        ),
        Recipe(
            name = "Tulmadron",
            gruppe = "Venenik",
            lab = Laboratory.ARCANE,
            preis = 200,
            zutatenPreis = 20,
            zutatenVerbreitung = 4,
            verbreitung = 3, // 3/5
            brewingDifficulty = 9,
            analysisDifficulty = 10
        ),
        Recipe(
            name = "Wasserwahn",
            gruppe = "Venenik",
            lab = Laboratory.LABORATORY,
            preis = 180,
            zutatenPreis = 50,
            zutatenVerbreitung = 4,
            verbreitung = 3,
            brewingDifficulty = 9,
            analysisDifficulty = 7
        ),
        Recipe(
            name = "Zazamotoxin",
            gruppe = "Venenik",
            lab = Laboratory.LABORATORY,
            preis = 250,
            zutatenPreis = 75,
            zutatenVerbreitung = 1,
            verbreitung = 2,
            brewingDifficulty = 10,
            analysisDifficulty = 8
        ),
        Recipe(
            name = "Zwei-Komponenten-Gifte",
            gruppe = "Venenik",
            lab = Laboratory.LABORATORY,
            preis = null, // unverkäuflich (∞)
            zutatenPreis = 100, // >100
            zutatenVerbreitung = 3,
            verbreitung = 1,
            brewingDifficulty = 10,
            analysisDifficulty = 15
        ),
        
        // Virtutica
        Recipe(
            name = "Charismaelixier",
            gruppe = "Virtutica",
            lab = Laboratory.WITCHES_KITCHEN,
            preis = 45,
            zutatenPreis = 23,
            zutatenVerbreitung = 9,
            verbreitung = 5,
            brewingDifficulty = 5,
            analysisDifficulty = 4
        ),
        Recipe(
            name = "Fingerfertigkeitselixier",
            gruppe = "Virtutica",
            lab = Laboratory.ARCANE,
            preis = 30,
            zutatenPreis = 15,
            zutatenVerbreitung = 8,
            verbreitung = 4,
            brewingDifficulty = 6,
            analysisDifficulty = 5
        ),
        Recipe(
            name = "Gewandtheitselixier",
            gruppe = "Virtutica",
            lab = Laboratory.WITCHES_KITCHEN,
            preis = 50,
            zutatenPreis = 25,
            zutatenVerbreitung = 6,
            verbreitung = 5,
            brewingDifficulty = 6,
            analysisDifficulty = 7
        ),
        Recipe(
            name = "Intuitionselixier",
            gruppe = "Virtutica",
            lab = Laboratory.WITCHES_KITCHEN,
            preis = 35,
            zutatenPreis = 18,
            zutatenVerbreitung = 8,
            verbreitung = 4,
            brewingDifficulty = 2,
            analysisDifficulty = 5
        ),
        Recipe(
            name = "Klugheitselixier",
            gruppe = "Virtutica",
            lab = Laboratory.WITCHES_KITCHEN,
            preis = 45,
            zutatenPreis = 23,
            zutatenVerbreitung = 9,
            verbreitung = 4,
            brewingDifficulty = 3,
            analysisDifficulty = 5
        ),
        Recipe(
            name = "Konstitutionselixier",
            gruppe = "Virtutica",
            lab = Laboratory.WITCHES_KITCHEN,
            preis = 40,
            zutatenPreis = 20,
            zutatenVerbreitung = 7,
            verbreitung = 5,
            brewingDifficulty = 6,
            analysisDifficulty = 3
        ),
        Recipe(
            name = "Kraftelixier",
            gruppe = "Virtutica",
            lab = Laboratory.ARCANE,
            preis = 50,
            zutatenPreis = 25,
            zutatenVerbreitung = 7,
            verbreitung = 6,
            brewingDifficulty = 7,
            analysisDifficulty = 3
        ),
        Recipe(
            name = "Mutelixier",
            gruppe = "Virtutica",
            lab = Laboratory.ARCANE,
            preis = 40,
            zutatenPreis = 20,
            zutatenVerbreitung = 8,
            verbreitung = 6,
            brewingDifficulty = 1,
            analysisDifficulty = 1
        ),
        Recipe(
            name = "Respondarum",
            gruppe = "Virtutica",
            lab = Laboratory.LABORATORY,
            preis = 60,
            zutatenPreis = 30,
            zutatenVerbreitung = 7,
            verbreitung = 5,
            brewingDifficulty = 5,
            analysisDifficulty = 4
        ),
        Recipe(
            name = "Scharfsinn-Elixier",
            gruppe = "Virtutica",
            lab = Laboratory.ARCANE,
            preis = 30,
            zutatenPreis = 15,
            zutatenVerbreitung = 10,
            verbreitung = 6,
            brewingDifficulty = 4,
            analysisDifficulty = 3
        ),
        Recipe(
            name = "Willenstrunk",
            gruppe = "Virtutica",
            lab = Laboratory.LABORATORY,
            preis = 50,
            zutatenPreis = 25,
            zutatenVerbreitung = 6,
            verbreitung = 5,
            brewingDifficulty = 6,
            analysisDifficulty = 6
        ),
        Recipe(
            name = "Zielwasser",
            gruppe = "Virtutica",
            lab = Laboratory.ARCANE,
            preis = 35,
            zutatenPreis = 18,
            zutatenVerbreitung = 10,
            verbreitung = 6,
            brewingDifficulty = 6,
            analysisDifficulty = 4
        )
    )
}
