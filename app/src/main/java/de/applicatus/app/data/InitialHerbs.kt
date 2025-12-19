package de.applicatus.app.data

import de.applicatus.app.data.model.herb.*
import de.applicatus.app.logic.DerianDateCalculator.DerianMonth

/**
 * Vordefinierte Kräuter aus Zoo-Botanica Aventurica und Wege der Alchimie
 * 
 * Diese Liste enthält ALLE 111 Kräuter aus dem DSA 4.1 Regelwerk.
 * Quelle: Zoo-Botanica Aventurica (Ulisses Spiele), Wege der Alchimie, Kräuter und Knochen
 * 
 * Extrahiert und konvertiert aus: https://github.com/pielmach/dsa-kraeutersuche-dotnet
 * Parser Datum: 2025-01-19
 */
object InitialHerbs {
    
    private val ALL_MONTHS = listOf(
        DerianMonth.PRAIOS, DerianMonth.RONDRA, DerianMonth.EFFERD,
        DerianMonth.TRAVIA, DerianMonth.BORON, DerianMonth.HESINDE,
        DerianMonth.FIRUN, DerianMonth.TSA, DerianMonth.PHEX,
        DerianMonth.PERAINE, DerianMonth.INGERIMM, DerianMonth.RAHJA,
        DerianMonth.NAMELESS_DAYS
    )
    
    val ALL_HERBS = listOf(
        Herb(
            name = "Alraune",
            identificationDifficulty = 9,
            baseQuantity = "eine Pflanze",
            distributions = listOf(
                HerbDistribution(Landscape.RIVER_LAKE, Occurrence.RARE),
                HerbDistribution(Landscape.FOREST_EDGE, Occurrence.RARE)
            ),
            harvestMonths = ALL_MONTHS,
            pageReference = 227
        ),
        Herb(
            name = "Alveranie",
            identificationDifficulty = -5,
            baseQuantity = "12 einzelne Blätter, in der Farbe des Monats",
            distributions = listOf(
                HerbDistribution(Landscape.ICE, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.DESERT, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.MOUNTAINS, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.HIGHLANDS, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.STEPPE, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.GRASSLANDS, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.RIVER_LAKE, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.RIVER_FLOODPLAINS, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.SWAMP, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.RAINFOREST, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.FOREST, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.FOREST_EDGE, Occurrence.VERY_RARE)
            ),
            harvestMonths = listOf(
                DerianMonth.PRAIOS,
                DerianMonth.RONDRA,
                DerianMonth.EFFERD,
                DerianMonth.TRAVIA,
                DerianMonth.BORON,
                DerianMonth.HESINDE,
                DerianMonth.FIRUN,
                DerianMonth.TSA,
                DerianMonth.PHEX,
                DerianMonth.PERAINE,
                DerianMonth.INGERIMM,
                DerianMonth.RAHJA
            ),
            pageReference = 228
        ),
        Herb(
            name = "Arganstrauch",
            identificationDifficulty = 4,
            baseQuantity = "eine Wurzel",
            distributions = listOf(
                HerbDistribution(Landscape.SWAMP, Occurrence.RARE),
                HerbDistribution(Landscape.RAINFOREST, Occurrence.OCCASIONAL),
                HerbDistribution(Landscape.FOREST, Occurrence.VERY_RARE)
            ),
            harvestMonths = ALL_MONTHS,
            pageReference = 228
        ),
        Herb(
            name = "Atan-Kiefer",
            identificationDifficulty = 6,
            baseQuantity = "W20 Stein Rinde, bei komplettem Abschälen Verdreifachung des Wertes",
            distributions = listOf(
                HerbDistribution(Landscape.MOUNTAINS, Occurrence.RARE)
            ),
            harvestMonths = ALL_MONTHS,
            pageReference = 228
        ),
        Herb(
            name = "Atmon",
            identificationDifficulty = 5,
            baseQuantity = "W6 Büschel",
            distributions = listOf(
                HerbDistribution(Landscape.HIGHLANDS, Occurrence.RARE),
                HerbDistribution(Landscape.STEPPE, Occurrence.OCCASIONAL),
                HerbDistribution(Landscape.RIVER_FLOODPLAINS, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.SWAMP, Occurrence.VERY_RARE)
            ),
            harvestMonths = listOf(
                DerianMonth.PERAINE
            ),
            pageReference = 229
        ),
        Herb(
            name = "Axorda-Baum",
            identificationDifficulty = 4,
            baseQuantity = "ein Baum",
            distributions = listOf(
                HerbDistribution(Landscape.MOUNTAINS, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.RAINFOREST, Occurrence.VERY_RARE)
            ),
            harvestMonths = ALL_MONTHS,
            pageReference = 229
        ),
        Herb(
            name = "Basilamine",
            identificationDifficulty = 15,
            baseQuantity = "W20+10 Schoten",
            distributions = listOf(
                HerbDistribution(Landscape.FOREST, Occurrence.OCCASIONAL),
                HerbDistribution(Landscape.FOREST_EDGE, Occurrence.RARE)
            ),
            harvestMonths = ALL_MONTHS,
            pageReference = 230,
            dangers = "Wer in einem Feld von Basilaminen steht, wird von der säurehaltigen Schoten beschossen."
        ),
        Herb(
            name = "Belmart",
            identificationDifficulty = 6,
            baseQuantity = "2W20 Blätter",
            distributions = listOf(
                HerbDistribution(Landscape.FOREST, Occurrence.OCCASIONAL),
                HerbDistribution(Landscape.FOREST_EDGE, Occurrence.RARE)
            ),
            harvestMonths = listOf(
                DerianMonth.PRAIOS,
                DerianMonth.RONDRA,
                DerianMonth.EFFERD,
                DerianMonth.TRAVIA,
                DerianMonth.BORON,
                DerianMonth.PERAINE,
                DerianMonth.INGERIMM,
                DerianMonth.RAHJA,
                DerianMonth.NAMELESS_DAYS
            ),
            pageReference = 230
        ),
        Herb(
            name = "Bleichmohn (Weißer Mohn)",
            identificationDifficulty = 5,
            baseQuantity = "W6 geschlossene Samenkapseln",
            distributions = listOf(
                HerbDistribution(Landscape.MOUNTAINS, Occurrence.RARE)
            ),
            harvestMonths = listOf(
                DerianMonth.RONDRA
            ),
            pageReference = 252
        ),
        Herb(
            name = "Blutblatt",
            identificationDifficulty = 4,
            baseQuantity = "W20+2 Zweige pro 10 AsP der Quelle",
            distributions = listOf(
                HerbDistribution(Landscape.ICE, Occurrence.RARE),
                HerbDistribution(Landscape.DESERT, Occurrence.RARE),
                HerbDistribution(Landscape.MOUNTAINS, Occurrence.RARE),
                HerbDistribution(Landscape.HIGHLANDS, Occurrence.RARE),
                HerbDistribution(Landscape.STEPPE, Occurrence.RARE),
                HerbDistribution(Landscape.GRASSLANDS, Occurrence.RARE),
                HerbDistribution(Landscape.RIVER_LAKE, Occurrence.RARE),
                HerbDistribution(Landscape.RIVER_FLOODPLAINS, Occurrence.RARE),
                HerbDistribution(Landscape.SWAMP, Occurrence.RARE),
                HerbDistribution(Landscape.RAINFOREST, Occurrence.RARE),
                HerbDistribution(Landscape.FOREST, Occurrence.RARE),
                HerbDistribution(Landscape.FOREST_EDGE, Occurrence.RARE)
            ),
            harvestMonths = ALL_MONTHS,
            pageReference = 230
        ),
        Herb(
            name = "Boronie",
            identificationDifficulty = -2,
            baseQuantity = "5 Blüten, die kurz vor dem Verblühen sind",
            distributions = listOf(
                HerbDistribution(Landscape.GRASSLANDS, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.RAINFOREST, Occurrence.RARE)
            ),
            harvestMonths = ALL_MONTHS,
            pageReference = 231
        ),
        Herb(
            name = "Boronsschlinge",
            identificationDifficulty = 15,
            baseQuantity = "",
            distributions = listOf(
                HerbDistribution(Landscape.RAINFOREST, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.FOREST, Occurrence.VERY_RARE)
            ),
            harvestMonths = ALL_MONTHS,
            pageReference = 231,
            dangers = "Wer sich der Boronsschlinge auf einen halben Schritt nähert muss eine KO+4 Probe ablegen oder er schläft binnen einer halben Minute ein und wird anschließend von den Ranken umschlungen."
        ),
        Herb(
            name = "Braunschlinge",
            identificationDifficulty = 6,
            baseQuantity = "vier Farnblätter und zwei je 3W6 Schritt lange Rangen (je nach Alter der Pflanze)",
            distributions = listOf(
                HerbDistribution(Landscape.STEPPE, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.SWAMP, Occurrence.OCCASIONAL),
                HerbDistribution(Landscape.FOREST, Occurrence.OCCASIONAL),
                HerbDistribution(Landscape.FOREST_EDGE, Occurrence.RARE)
            ),
            harvestMonths = ALL_MONTHS,
            pageReference = 192
        ),
        Herb(
            name = "Bunter Mohn",
            identificationDifficulty = -5,
            baseQuantity = "eine geschlossene Samenkapsel",
            distributions = listOf(
                HerbDistribution(Landscape.GRASSLANDS, Occurrence.COMMON),
                HerbDistribution(Landscape.RIVER_LAKE, Occurrence.OCCASIONAL),
                HerbDistribution(Landscape.FOREST_EDGE, Occurrence.RARE)
            ),
            harvestMonths = listOf(
                DerianMonth.TRAVIA
            ),
            pageReference = 252
        ),
        Herb(
            name = "Carlog",
            identificationDifficulty = 5,
            baseQuantity = "W6 Blüten mit je einem Stempel",
            distributions = listOf(
                HerbDistribution(Landscape.RIVER_LAKE, Occurrence.RARE),
                HerbDistribution(Landscape.RIVER_FLOODPLAINS, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.SWAMP, Occurrence.OCCASIONAL)
            ),
            harvestMonths = listOf(
                DerianMonth.EFFERD,
                DerianMonth.PERAINE
            ),
            pageReference = 232
        ),
        Herb(
            name = "Cheria-Kaktus",
            identificationDifficulty = 4,
            baseQuantity = "W3 Stein Kaktusfleisch und pro Stein 3W6+8 Stacheln",
            distributions = listOf(
                HerbDistribution(Landscape.DESERT, Occurrence.RARE)
            ),
            harvestMonths = ALL_MONTHS,
            pageReference = 232,
            dangers = "Werden bei der Kaktusernte keine dicken Lederhandschuhe getragen, muss eine FF Probe abgelegt werden. Ansonsten verletzt man sich an den Stacheln und wird vergiftet."
        ),
        Herb(
            name = "Chonchinis",
            identificationDifficulty = 6,
            baseQuantity = "W20 Blätter",
            distributions = listOf(
                HerbDistribution(Landscape.HIGHLANDS, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.STEPPE, Occurrence.OCCASIONAL),
                HerbDistribution(Landscape.FOREST_EDGE, Occurrence.RARE)
            ),
            harvestMonths = listOf(
                DerianMonth.PRAIOS,
                DerianMonth.RONDRA,
                DerianMonth.EFFERD,
                DerianMonth.TRAVIA,
                DerianMonth.BORON,
                DerianMonth.TSA,
                DerianMonth.PHEX,
                DerianMonth.PERAINE,
                DerianMonth.INGERIMM,
                DerianMonth.RAHJA,
                DerianMonth.NAMELESS_DAYS
            ),
            pageReference = 233
        ),
        Herb(
            name = "Dergolasch",
            identificationDifficulty = 8,
            baseQuantity = "1W6 Pilzhüte",
            distributions = listOf(
                HerbDistribution(Landscape.CAVE_WET, Occurrence.RARE)
            ),
            harvestMonths = listOf(
                DerianMonth.RONDRA,
                DerianMonth.EFFERD,
                DerianMonth.TRAVIA,
                DerianMonth.BORON,
                DerianMonth.HESINDE,
                DerianMonth.FIRUN,
                DerianMonth.TSA,
                DerianMonth.PHEX,
                DerianMonth.PERAINE,
                DerianMonth.INGERIMM
            ),
            pageReference = 193
        ),
        Herb(
            name = "Disdychonda",
            identificationDifficulty = 5,
            baseQuantity = "4 Blätter",
            distributions = listOf(
                HerbDistribution(Landscape.RAINFOREST, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.FOREST, Occurrence.VERY_RARE)
            ),
            harvestMonths = ALL_MONTHS,
            pageReference = 234,
            dangers = "Die Disdychonda greift mit ihren Blättern an. Außerdem befindet sich in der Umgebung möglicherweise noch ein Feld von Raubnesseln."
        ),
        Herb(
            name = "Donf",
            identificationDifficulty = 6,
            baseQuantity = "ein Stängel",
            distributions = listOf(
                HerbDistribution(Landscape.RIVER_LAKE, Occurrence.RARE),
                HerbDistribution(Landscape.RIVER_FLOODPLAINS, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.SWAMP, Occurrence.OCCASIONAL)
            ),
            harvestMonths = ALL_MONTHS,
            pageReference = 234
        ),
        Herb(
            name = "Dornrose",
            identificationDifficulty = 3,
            baseQuantity = "Strauch mit W6 Blüten",
            distributions = listOf(
                HerbDistribution(Landscape.GRASSLANDS, Occurrence.COMMON),
                HerbDistribution(Landscape.FOREST, Occurrence.OCCASIONAL)
            ),
            harvestMonths = ALL_MONTHS,
            pageReference = 235
        ),
        Herb(
            name = "Efeuer",
            identificationDifficulty = 4,
            baseQuantity = "",
            distributions = listOf(
                HerbDistribution(Landscape.FOREST, Occurrence.RARE),
                HerbDistribution(Landscape.CAVE_WET, Occurrence.OCCASIONAL),
                HerbDistribution(Landscape.CAVE_DRY, Occurrence.OCCASIONAL)
            ),
            harvestMonths = ALL_MONTHS,
            pageReference = 235,
            dangers = "Efeuer gilt als gefährliches Dornicht (ZBA S.205) und eine Berührung verursacht Schaden."
        ),
        Herb(
            name = "Egelschreck",
            identificationDifficulty = 6,
            baseQuantity = "2W20 Blätter",
            distributions = listOf(
                HerbDistribution(Landscape.GRASSLANDS, Occurrence.OCCASIONAL),
                HerbDistribution(Landscape.RIVER_FLOODPLAINS, Occurrence.OCCASIONAL),
                HerbDistribution(Landscape.SWAMP, Occurrence.COMMON),
                HerbDistribution(Landscape.FOREST_EDGE, Occurrence.RARE)
            ),
            harvestMonths = listOf(
                DerianMonth.RONDRA,
                DerianMonth.EFFERD
            ),
            pageReference = 235
        ),
        Herb(
            name = "Eitriger Krötenschemel",
            identificationDifficulty = 2,
            baseQuantity = "2W6 Pilzhäute",
            distributions = listOf(
                HerbDistribution(Landscape.RIVER_FLOODPLAINS, Occurrence.RARE),
                HerbDistribution(Landscape.SWAMP, Occurrence.OCCASIONAL),
                HerbDistribution(Landscape.FOREST, Occurrence.VERY_RARE)
            ),
            harvestMonths = listOf(
                DerianMonth.EFFERD,
                DerianMonth.TRAVIA,
                DerianMonth.BORON
            ),
            pageReference = 236
        ),
        Herb(
            name = "Felsenmilch",
            identificationDifficulty = 4,
            baseQuantity = "1 Schank",
            distributions = listOf(
                HerbDistribution(Landscape.CAVE_WET, Occurrence.RARE),
                HerbDistribution(Landscape.CAVE_DRY, Occurrence.RARE)
            ),
            harvestMonths = ALL_MONTHS,
            pageReference = 193
        ),
        Herb(
            name = "Feuermoos und Efferdmoos",
            identificationDifficulty = 15,
            baseQuantity = "",
            distributions = listOf(
                HerbDistribution(Landscape.CAVE_WET, Occurrence.OCCASIONAL)
            ),
            harvestMonths = ALL_MONTHS,
            pageReference = 236,
            dangers = "Die Berührung mit Feuer- bzw. Efferdmoos erzeugt schwere Verätzungen. Die Wirkungen von Feuer- und Efferdmoos heben sich jedoch gegenseitig auf."
        ),
        Herb(
            name = "Feuerschlick",
            identificationDifficulty = 6,
            baseQuantity = "W6 Stein der Algen",
            distributions = listOf(
                HerbDistribution(Landscape.SEA, Occurrence.VERY_RARE)
            ),
            harvestMonths = ALL_MONTHS,
            pageReference = 237
        ),
        Herb(
            name = "Finage",
            identificationDifficulty = 5,
            baseQuantity = "Baum mit W20 Trieben und Bast",
            distributions = listOf(
                HerbDistribution(Landscape.GRASSLANDS, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.RAINFOREST, Occurrence.RARE),
                HerbDistribution(Landscape.FOREST, Occurrence.RARE)
            ),
            harvestMonths = listOf(
                DerianMonth.BORON,
                DerianMonth.HESINDE,
                DerianMonth.FIRUN,
                DerianMonth.PERAINE
            ),
            pageReference = 238
        ),
        Herb(
            name = "Färberlotus (Gelber, Blauer, Roter und Rosa Lotus)",
            identificationDifficulty = 9,
            baseQuantity = "2W6+1 Blüten",
            distributions = listOf(
                HerbDistribution(Landscape.RIVER_LAKE, Occurrence.OCCASIONAL)
            ),
            harvestMonths = listOf(
                DerianMonth.PRAIOS,
                DerianMonth.INGERIMM,
                DerianMonth.RAHJA,
                DerianMonth.NAMELESS_DAYS
            ),
            pageReference = 246
        ),
        Herb(
            name = "Grauer Lotus",
            identificationDifficulty = 8,
            baseQuantity = "W6+1 Blüten",
            distributions = listOf(
                HerbDistribution(Landscape.RIVER_LAKE, Occurrence.RARE),
                HerbDistribution(Landscape.SWAMP, Occurrence.VERY_RARE)
            ),
            harvestMonths = listOf(
                DerianMonth.PRAIOS,
                DerianMonth.RONDRA,
                DerianMonth.EFFERD,
                DerianMonth.TRAVIA,
                DerianMonth.BORON,
                DerianMonth.PHEX,
                DerianMonth.PERAINE,
                DerianMonth.INGERIMM,
                DerianMonth.RAHJA,
                DerianMonth.NAMELESS_DAYS
            ),
            pageReference = 246,
            dangers = "Im Umkreis von 5 Schritt ist eine KO Probe nötig um den giftigen Blütenstaub nicht einzuatmen. Fehlende Punkte entsprechen der Zahl an eingeatmeten Dosen."
        ),
        Herb(
            name = "Grauer Mohn",
            identificationDifficulty = 1,
            baseQuantity = "eine geschlossene Samenkapsel und eine Blüte",
            distributions = listOf(
                HerbDistribution(Landscape.MOUNTAINS, Occurrence.VERY_RARE)
            ),
            harvestMonths = ALL_MONTHS,
            pageReference = 253
        ),
        Herb(
            name = "Grüner Schleimpilz",
            identificationDifficulty = 6,
            baseQuantity = "1W20 Unzen",
            distributions = listOf(
                HerbDistribution(Landscape.CAVE_WET, Occurrence.RARE),
                HerbDistribution(Landscape.CAVE_DRY, Occurrence.RARE)
            ),
            harvestMonths = ALL_MONTHS,
            pageReference = 194
        ),
        Herb(
            name = "Grüne Schleimschlange",
            identificationDifficulty = 4,
            baseQuantity = "",
            distributions = listOf(
                HerbDistribution(Landscape.RIVER_FLOODPLAINS, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.SWAMP, Occurrence.VERY_RARE)
            ),
            harvestMonths = ALL_MONTHS,
            pageReference = 238,
            dangers = "Bei Anblick eines überwucherten Kadavers ist eine MU Probe fällig (vgl. MGS 51/54 Dämoneneigenschaft Schreckgestalt I) und ein Patzer bringt Phobie gegen die Pflanze als permanenten Nachteil."
        ),
        Herb(
            name = "Gulmond",
            identificationDifficulty = 6,
            baseQuantity = "2W6 Blätter",
            distributions = listOf(
                HerbDistribution(Landscape.HIGHLANDS, Occurrence.OCCASIONAL),
                HerbDistribution(Landscape.STEPPE, Occurrence.COMMON),
                HerbDistribution(Landscape.FOREST, Occurrence.OCCASIONAL)
            ),
            harvestMonths = listOf(
                DerianMonth.PRAIOS,
                DerianMonth.RONDRA,
                DerianMonth.EFFERD,
                DerianMonth.TRAVIA,
                DerianMonth.BORON,
                DerianMonth.PHEX,
                DerianMonth.PERAINE,
                DerianMonth.INGERIMM,
                DerianMonth.RAHJA,
                DerianMonth.NAMELESS_DAYS
            ),
            pageReference = 238
        ),
        Herb(
            name = "Hiradwurz",
            identificationDifficulty = 8,
            baseQuantity = "eine Wurzel",
            distributions = listOf(
                HerbDistribution(Landscape.STEPPE, Occurrence.RARE)
            ),
            harvestMonths = ALL_MONTHS,
            pageReference = 239
        ),
        Herb(
            name = "Hollbeere",
            identificationDifficulty = 4,
            baseQuantity = "2W6 Sträucher mit jeweils 2W6+5 Beeren und 2W6+3 Blätter der untersten Zweige",
            distributions = listOf(
                HerbDistribution(Landscape.FOREST, Occurrence.OCCASIONAL),
                HerbDistribution(Landscape.FOREST_EDGE, Occurrence.COMMON)
            ),
            harvestMonths = listOf(
                DerianMonth.RONDRA,
                DerianMonth.EFFERD
            ),
            pageReference = 240
        ),
        Herb(
            name = "Horusche",
            identificationDifficulty = 7,
            baseQuantity = "W6 erntereife Schoten mit je W3 Kernen",
            distributions = listOf(
                HerbDistribution(Landscape.FOREST, Occurrence.RARE)
            ),
            harvestMonths = ALL_MONTHS,
            pageReference = 240
        ),
        Herb(
            name = "Höllenkraut",
            identificationDifficulty = 8,
            baseQuantity = "W10 Stein der Ranken",
            distributions = listOf(
                HerbDistribution(Landscape.RAINFOREST, Occurrence.COMMON),
                HerbDistribution(Landscape.FOREST, Occurrence.RARE),
                HerbDistribution(Landscape.FOREST_EDGE, Occurrence.VERY_RARE)
            ),
            harvestMonths = ALL_MONTHS,
            pageReference = 240
        ),
        Herb(
            name = "Ilmenblatt",
            identificationDifficulty = 2,
            baseQuantity = "W20 Blätter und Blüten sowie 1 Unze Harz",
            distributions = listOf(
                HerbDistribution(Landscape.MOUNTAINS, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.GRASSLANDS, Occurrence.OCCASIONAL),
                HerbDistribution(Landscape.FOREST, Occurrence.OCCASIONAL)
            ),
            harvestMonths = listOf(
                DerianMonth.TRAVIA,
                DerianMonth.INGERIMM
            ),
            pageReference = 241
        ),
        Herb(
            name = "Iribaarslilie",
            identificationDifficulty = 12,
            baseQuantity = "",
            distributions = listOf(
                HerbDistribution(Landscape.SWAMP, Occurrence.VERY_RARE)
            ),
            harvestMonths = ALL_MONTHS,
            pageReference = 241,
            dangers = "Die Iribaarslilie verzaubert jeden, der sich ihr nähert und zieht ihn anschließend in die Tiefe."
        ),
        Herb(
            name = "Jagdgras",
            identificationDifficulty = 15,
            baseQuantity = "",
            distributions = listOf(
                HerbDistribution(Landscape.MOUNTAINS, Occurrence.OCCASIONAL),
                HerbDistribution(Landscape.HIGHLANDS, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.STEPPE, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.FOREST, Occurrence.VERY_RARE)
            ),
            harvestMonths = ALL_MONTHS,
            pageReference = 242,
            dangers = "Jagdgras wandert nachts und lässt sich auf Opfern nieder um seine Wurzeln in sie zu schlagen. Wird manchmal mit Wirselkraut verwechselt, was schlimme Folgen hat."
        ),
        Herb(
            name = "Joruga",
            identificationDifficulty = 7,
            baseQuantity = "eine Wurzel",
            distributions = listOf(
                HerbDistribution(Landscape.MOUNTAINS, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.GRASSLANDS, Occurrence.RARE),
                HerbDistribution(Landscape.FOREST, Occurrence.OCCASIONAL)
            ),
            harvestMonths = listOf(
                DerianMonth.PRAIOS,
                DerianMonth.RONDRA,
                DerianMonth.EFFERD,
                DerianMonth.TRAVIA,
                DerianMonth.BORON,
                DerianMonth.RAHJA,
                DerianMonth.NAMELESS_DAYS
            ),
            pageReference = 243
        ),
        Herb(
            name = "Kairan",
            identificationDifficulty = 6,
            baseQuantity = "ein Halm",
            distributions = listOf(
                HerbDistribution(Landscape.RIVER_LAKE, Occurrence.RARE)
            ),
            harvestMonths = ALL_MONTHS,
            pageReference = 243
        ),
        Herb(
            name = "Kajubo",
            identificationDifficulty = 4,
            baseQuantity = "2W6 Knospen (Nur die Hälfte um den Strauch zu schonen)",
            distributions = listOf(
                HerbDistribution(Landscape.FOREST_EDGE, Occurrence.RARE)
            ),
            harvestMonths = listOf(
                DerianMonth.PRAIOS,
                DerianMonth.RONDRA,
                DerianMonth.EFFERD,
                DerianMonth.TRAVIA,
                DerianMonth.PHEX,
                DerianMonth.PERAINE,
                DerianMonth.INGERIMM,
                DerianMonth.RAHJA,
                DerianMonth.NAMELESS_DAYS
            ),
            pageReference = 244
        ),
        Herb(
            name = "Khôm- oder Mhanadiknolle",
            identificationDifficulty = 12,
            baseQuantity = "eine Wurzel mit W6 Maß klarem Wasser",
            distributions = listOf(
                HerbDistribution(Landscape.DESERT, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.STEPPE, Occurrence.RARE)
            ),
            harvestMonths = ALL_MONTHS,
            pageReference = 244
        ),
        Herb(
            name = "Klippenzahn",
            identificationDifficulty = 8,
            baseQuantity = "2W6 Stängel",
            distributions = listOf(
                HerbDistribution(Landscape.MOUNTAINS, Occurrence.OCCASIONAL),
                HerbDistribution(Landscape.HIGHLANDS, Occurrence.OCCASIONAL)
            ),
            harvestMonths = listOf(
                DerianMonth.PRAIOS,
                DerianMonth.RONDRA,
                DerianMonth.PERAINE,
                DerianMonth.INGERIMM,
                DerianMonth.RAHJA,
                DerianMonth.NAMELESS_DAYS
            ),
            pageReference = 245
        ),
        Herb(
            name = "Kukuka",
            identificationDifficulty = 10,
            baseQuantity = "1W3 x 20 Blätter",
            distributions = listOf(
                HerbDistribution(Landscape.RAINFOREST, Occurrence.RARE)
            ),
            harvestMonths = ALL_MONTHS,
            pageReference = 245
        ),
        Herb(
            name = "Libellengras",
            identificationDifficulty = 5,
            baseQuantity = "eine Frucht",
            distributions = listOf(
                HerbDistribution(Landscape.RIVER_LAKE, Occurrence.OCCASIONAL),
                HerbDistribution(Landscape.RIVER_FLOODPLAINS, Occurrence.OCCASIONAL),
                HerbDistribution(Landscape.SWAMP, Occurrence.RARE)
            ),
            harvestMonths = listOf(
                DerianMonth.RONDRA,
                DerianMonth.EFFERD,
                DerianMonth.INGERIMM,
                DerianMonth.RAHJA
            ),
            pageReference = 194
        ),
        Herb(
            name = "Lichtnebler",
            identificationDifficulty = 10,
            baseQuantity = "1 Skrupel Sporen",
            distributions = listOf(
                HerbDistribution(Landscape.CAVE_WET, Occurrence.OCCASIONAL),
                HerbDistribution(Landscape.CAVE_DRY, Occurrence.OCCASIONAL)
            ),
            harvestMonths = listOf(
                DerianMonth.PRAIOS,
                DerianMonth.EFFERD,
                DerianMonth.TRAVIA,
                DerianMonth.BORON,
                DerianMonth.HESINDE,
                DerianMonth.PERAINE,
                DerianMonth.INGERIMM,
                DerianMonth.RAHJA,
                DerianMonth.NAMELESS_DAYS
            ),
            pageReference = 194
        ),
        Herb(
            name = "Lulanie",
            identificationDifficulty = 5,
            baseQuantity = "eine Blüte",
            distributions = listOf(
                HerbDistribution(Landscape.FOREST, Occurrence.RARE),
                HerbDistribution(Landscape.FOREST_EDGE, Occurrence.VERY_RARE)
            ),
            harvestMonths = listOf(
                DerianMonth.PRAIOS,
                DerianMonth.RONDRA,
                DerianMonth.RAHJA,
                DerianMonth.NAMELESS_DAYS
            ),
            pageReference = 248
        ),
        Herb(
            name = "Madablüte",
            identificationDifficulty = 15,
            baseQuantity = "eine Blüte",
            distributions = listOf(
                HerbDistribution(Landscape.MOUNTAINS, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.STEPPE, Occurrence.VERY_RARE)
            ),
            harvestMonths = ALL_MONTHS,
            pageReference = 248
        ),
        Herb(
            name = "Menchal-Kaktus",
            identificationDifficulty = 4,
            baseQuantity = "ein Kaktus mit W3 Maß Menchalsaft; bei 1 auf W20 außerdem mit W6 Blüten",
            distributions = listOf(
                HerbDistribution(Landscape.DESERT, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.HIGHLANDS, Occurrence.VERY_RARE)
            ),
            harvestMonths = ALL_MONTHS,
            pageReference = 249
        ),
        Herb(
            name = "Merach-Strauch",
            identificationDifficulty = 2,
            baseQuantity = "2W20 reife Früchte",
            distributions = listOf(
                HerbDistribution(Landscape.RAINFOREST, Occurrence.RARE),
                HerbDistribution(Landscape.FOREST, Occurrence.VERY_RARE)
            ),
            harvestMonths = listOf(
                DerianMonth.EFFERD,
                DerianMonth.TRAVIA
            ),
            pageReference = 250
        ),
        Herb(
            name = "Messergras",
            identificationDifficulty = 6,
            baseQuantity = "",
            distributions = listOf(
                HerbDistribution(Landscape.DESERT, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.HIGHLANDS, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.STEPPE, Occurrence.RARE)
            ),
            harvestMonths = ALL_MONTHS,
            pageReference = 250,
            dangers = "Messergras verletzt bei Berührungen und eine Reise durch ein derart bewachsende Gebiet kann tödlich enden."
        ),
        Herb(
            name = "Mibelrohr",
            identificationDifficulty = 10,
            baseQuantity = "2W6 Kolben",
            distributions = listOf(
                HerbDistribution(Landscape.RIVER_LAKE, Occurrence.OCCASIONAL),
                HerbDistribution(Landscape.RIVER_FLOODPLAINS, Occurrence.RARE),
                HerbDistribution(Landscape.SWAMP, Occurrence.VERY_RARE)
            ),
            harvestMonths = listOf(
                DerianMonth.PRAIOS,
                DerianMonth.RONDRA,
                DerianMonth.EFFERD,
                DerianMonth.INGERIMM,
                DerianMonth.RAHJA,
                DerianMonth.NAMELESS_DAYS
            ),
            pageReference = 251
        ),
        Herb(
            name = "Mirbelstein",
            identificationDifficulty = 8,
            baseQuantity = "1 Wurzelknolle",
            distributions = listOf(
                HerbDistribution(Landscape.HIGHLANDS, Occurrence.RARE),
                HerbDistribution(Landscape.GRASSLANDS, Occurrence.OCCASIONAL),
                HerbDistribution(Landscape.FOREST, Occurrence.COMMON)
            ),
            harvestMonths = listOf(
                DerianMonth.PRAIOS,
                DerianMonth.RONDRA,
                DerianMonth.EFFERD,
                DerianMonth.TRAVIA,
                DerianMonth.BORON,
                DerianMonth.PERAINE,
                DerianMonth.INGERIMM,
                DerianMonth.RAHJA,
                DerianMonth.NAMELESS_DAYS
            ),
            pageReference = 251
        ),
        Herb(
            name = "Mirhamer Seidenliane",
            identificationDifficulty = 4,
            baseQuantity = "eine Ranke mit W2+1 Knoten",
            distributions = listOf(
                HerbDistribution(Landscape.MOUNTAINS, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.RIVER_LAKE, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.RAINFOREST, Occurrence.RARE)
            ),
            harvestMonths = listOf(
                DerianMonth.PRAIOS,
                DerianMonth.RONDRA,
                DerianMonth.EFFERD,
                DerianMonth.TRAVIA,
                DerianMonth.BORON,
                DerianMonth.TSA,
                DerianMonth.PHEX,
                DerianMonth.PERAINE,
                DerianMonth.INGERIMM,
                DerianMonth.RAHJA,
                DerianMonth.NAMELESS_DAYS
            ),
            pageReference = 251
        ),
        Herb(
            name = "Morgendornstrauch",
            identificationDifficulty = 13,
            baseQuantity = "",
            distributions = listOf(
                HerbDistribution(Landscape.SWAMP, Occurrence.RARE)
            ),
            harvestMonths = ALL_MONTHS,
            pageReference = 254,
            dangers = "Die Berührung einer Blüte des Morgendornstrauchs verwandelt denjenigen binnen einer Woche in eine Sumpfranze."
        ),
        Herb(
            name = "Naftanstaude",
            identificationDifficulty = 1,
            baseQuantity = "eine Staude",
            distributions = listOf(
                HerbDistribution(Landscape.STEPPE, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.GRASSLANDS, Occurrence.RARE),
                HerbDistribution(Landscape.FOREST_EDGE, Occurrence.VERY_RARE)
            ),
            harvestMonths = listOf(
                DerianMonth.PRAIOS,
                DerianMonth.RONDRA,
                DerianMonth.INGERIMM,
                DerianMonth.RAHJA,
                DerianMonth.NAMELESS_DAYS
            ),
            pageReference = 255,
            dangers = "Der Saft der Naftanstaude ist stark ätzend und kann nur mit einer FF+2 Probe gefahrlos geerntet werden."
        ),
        Herb(
            name = "Neckerkraut",
            identificationDifficulty = 4,
            baseQuantity = "W20+5 Blätter",
            distributions = listOf(
                HerbDistribution(Landscape.GRASSLANDS, Occurrence.RARE),
                HerbDistribution(Landscape.SWAMP, Occurrence.VERY_RARE)
            ),
            harvestMonths = ALL_MONTHS,
            pageReference = 255
        ),
        Herb(
            name = "Nothilf",
            identificationDifficulty = 6,
            baseQuantity = "W20+2 Blüten und 2W20+10 Blätter",
            distributions = listOf(
                HerbDistribution(Landscape.MOUNTAINS, Occurrence.RARE),
                HerbDistribution(Landscape.FOREST, Occurrence.RARE)
            ),
            harvestMonths = listOf(
                DerianMonth.PRAIOS,
                DerianMonth.PERAINE
            ),
            pageReference = 256
        ),
        Herb(
            name = "Olginwurz",
            identificationDifficulty = 10,
            baseQuantity = "W3 Moosballen",
            distributions = listOf(
                HerbDistribution(Landscape.MOUNTAINS, Occurrence.RARE),
                HerbDistribution(Landscape.HIGHLANDS, Occurrence.RARE),
                HerbDistribution(Landscape.FOREST, Occurrence.RARE)
            ),
            harvestMonths = ALL_MONTHS,
            pageReference = 256
        ),
        Herb(
            name = "Orazal",
            identificationDifficulty = 4,
            baseQuantity = "W6 verholzte Stängel",
            distributions = listOf(
                HerbDistribution(Landscape.RAINFOREST, Occurrence.RARE),
                HerbDistribution(Landscape.FOREST, Occurrence.VERY_RARE)
            ),
            harvestMonths = ALL_MONTHS,
            pageReference = 257,
            dangers = "In großer Hitze kann sich Orazal so sehr aufheizen, dass die Pflanze bei Berührung auf der Haut festklebt und beim Ablösen die Haut verletzt."
        ),
        Herb(
            name = "Orklandbovist",
            identificationDifficulty = 4,
            baseQuantity = "",
            distributions = listOf(
                HerbDistribution(Landscape.HIGHLANDS, Occurrence.RARE),
                HerbDistribution(Landscape.STEPPE, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.FOREST, Occurrence.VERY_RARE)
            ),
            harvestMonths = ALL_MONTHS,
            pageReference = 258,
            dangers = "In den Monaten Ingerimm, Rahja, Praios und Rondra ist der Orklandbovist gefährlich. Platzt er auf, so kann man sich in 5 Schritt Umkreis nur durch eine Athletik-Probe +15 in Deckung bringen. Andernfalls muss eine KO Probe klären ob man die Pilzsporen eingeatmet hat."
        ),
        Herb(
            name = "Pestsporenpilz",
            identificationDifficulty = 6,
            baseQuantity = "eine Pilzhaut",
            distributions = listOf(
                HerbDistribution(Landscape.GRASSLANDS, Occurrence.RARE),
                HerbDistribution(Landscape.SWAMP, Occurrence.RARE),
                HerbDistribution(Landscape.FOREST, Occurrence.VERY_RARE)
            ),
            harvestMonths = listOf(
                DerianMonth.EFFERD,
                DerianMonth.TRAVIA,
                DerianMonth.BORON,
                DerianMonth.PERAINE,
                DerianMonth.INGERIMM,
                DerianMonth.RAHJA
            ),
            pageReference = 258,
            dangers = "Wird beim Ernten die Haut des Pestsporenpilzes nicht vorsichtig abgelöst (FF+2 Probe) oder stolpert man versehentlich über einen Pilz (GE+2 Probe) so setzt der Pilz eine giftige Wolke frei."
        ),
        Herb(
            name = "Phosphorpilz",
            identificationDifficulty = 10,
            baseQuantity = "W6 Stein Geflechtstücke",
            distributions = listOf(
                HerbDistribution(Landscape.CAVE_WET, Occurrence.OCCASIONAL),
                HerbDistribution(Landscape.CAVE_DRY, Occurrence.RARE)
            ),
            harvestMonths = ALL_MONTHS,
            pageReference = 259
        ),
        Herb(
            name = "Purpurmohn",
            identificationDifficulty = 3,
            baseQuantity = "eine geschlossene Samenkapsel",
            distributions = listOf(
                HerbDistribution(Landscape.GRASSLANDS, Occurrence.RARE),
                HerbDistribution(Landscape.RIVER_LAKE, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.FOREST, Occurrence.VERY_RARE)
            ),
            harvestMonths = listOf(
                DerianMonth.RAHJA
            ),
            pageReference = 253
        ),
        Herb(
            name = "Purpurner Lotus",
            identificationDifficulty = 7,
            baseQuantity = "W6+1 Blüten",
            distributions = listOf(
                HerbDistribution(Landscape.RIVER_LAKE, Occurrence.RARE)
            ),
            harvestMonths = listOf(
                DerianMonth.PRAIOS,
                DerianMonth.INGERIMM,
                DerianMonth.RAHJA,
                DerianMonth.NAMELESS_DAYS
            ),
            pageReference = 246,
            dangers = "Im Umkreis von 5 Schritt ist eine KO Probe nötig um den giftigen Blütenstaub nicht einzuatmen. Fehlende Punkte entsprechen der Zahl an eingeatmeten Dosen."
        ),
        Herb(
            name = "Quasselwurz",
            identificationDifficulty = 12,
            baseQuantity = "eine Wurzel",
            distributions = listOf(
                HerbDistribution(Landscape.FOREST, Occurrence.RARE)
            ),
            harvestMonths = listOf(
                DerianMonth.PRAIOS,
                DerianMonth.RONDRA,
                DerianMonth.EFFERD,
                DerianMonth.TRAVIA,
                DerianMonth.PHEX,
                DerianMonth.PERAINE,
                DerianMonth.INGERIMM,
                DerianMonth.RAHJA,
                DerianMonth.NAMELESS_DAYS
            ),
            pageReference = 259
        ),
        Herb(
            name = "Quinja",
            identificationDifficulty = 6,
            baseQuantity = "W3+2 Beeren",
            distributions = listOf(
                HerbDistribution(Landscape.RAINFOREST, Occurrence.COMMON),
                HerbDistribution(Landscape.FOREST, Occurrence.OCCASIONAL),
                HerbDistribution(Landscape.FOREST_EDGE, Occurrence.RARE)
            ),
            harvestMonths = listOf(
                DerianMonth.PRAIOS,
                DerianMonth.RONDRA,
                DerianMonth.EFFERD,
                DerianMonth.TRAVIA,
                DerianMonth.INGERIMM,
                DerianMonth.RAHJA,
                DerianMonth.NAMELESS_DAYS
            ),
            pageReference = 260,
            dangers = "Verwechslung mit Scheinquinja möglich (zusätzliche Pflanzenkunde-Probe +8) welcher leicht giftig ist."
        ),
        Herb(
            name = "Rahjalieb",
            identificationDifficulty = 5,
            baseQuantity = "2W6 Blätter",
            distributions = listOf(
                HerbDistribution(Landscape.GRASSLANDS, Occurrence.OCCASIONAL),
                HerbDistribution(Landscape.SWAMP, Occurrence.OCCASIONAL),
                HerbDistribution(Landscape.RAINFOREST, Occurrence.COMMON),
                HerbDistribution(Landscape.FOREST, Occurrence.OCCASIONAL)
            ),
            harvestMonths = listOf(
                DerianMonth.PRAIOS,
                DerianMonth.RONDRA,
                DerianMonth.EFFERD,
                DerianMonth.TRAVIA,
                DerianMonth.BORON,
                DerianMonth.HESINDE,
                DerianMonth.FIRUN,
                DerianMonth.PHEX,
                DerianMonth.PERAINE,
                DerianMonth.INGERIMM,
                DerianMonth.RAHJA,
                DerianMonth.NAMELESS_DAYS
            ),
            pageReference = 260
        ),
        Herb(
            name = "Rattenpilz",
            identificationDifficulty = 7,
            baseQuantity = "ein Pilz",
            distributions = listOf(
                HerbDistribution(Landscape.MOUNTAINS, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.HIGHLANDS, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.STEPPE, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.GRASSLANDS, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.RIVER_LAKE, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.RIVER_FLOODPLAINS, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.SWAMP, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.RAINFOREST, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.FOREST, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.FOREST_EDGE, Occurrence.VERY_RARE)
            ),
            harvestMonths = listOf(
                DerianMonth.PRAIOS,
                DerianMonth.RONDRA,
                DerianMonth.EFFERD,
                DerianMonth.TRAVIA,
                DerianMonth.BORON,
                DerianMonth.PHEX,
                DerianMonth.PERAINE,
                DerianMonth.INGERIMM,
                DerianMonth.RAHJA,
                DerianMonth.NAMELESS_DAYS
            ),
            pageReference = 260,
            dangers = "Der Rattenpilz verströmt eine magische Anziehungskraft auf jeden Wanderer."
        ),
        Herb(
            name = "Rauschgurke",
            identificationDifficulty = 3,
            baseQuantity = "3W6 reife Rauschgurken",
            distributions = listOf(
                HerbDistribution(Landscape.FOREST, Occurrence.OCCASIONAL),
                HerbDistribution(Landscape.FOREST_EDGE, Occurrence.RARE)
            ),
            harvestMonths = listOf(
                DerianMonth.PRAIOS,
                DerianMonth.RONDRA,
                DerianMonth.EFFERD,
                DerianMonth.TRAVIA,
                DerianMonth.BORON,
                DerianMonth.TSA,
                DerianMonth.PHEX,
                DerianMonth.PERAINE,
                DerianMonth.INGERIMM,
                DerianMonth.RAHJA,
                DerianMonth.NAMELESS_DAYS
            ),
            pageReference = 261
        ),
        Herb(
            name = "Rote Pfeilblüte",
            identificationDifficulty = 7,
            baseQuantity = "W6 Blüten",
            distributions = listOf(
                HerbDistribution(Landscape.SWAMP, Occurrence.OCCASIONAL),
                HerbDistribution(Landscape.RAINFOREST, Occurrence.OCCASIONAL),
                HerbDistribution(Landscape.FOREST, Occurrence.VERY_RARE)
            ),
            harvestMonths = listOf(
                DerianMonth.PERAINE,
                DerianMonth.INGERIMM,
                DerianMonth.RAHJA
            ),
            pageReference = 261
        ),
        Herb(
            name = "Roter Drachenschlund",
            identificationDifficulty = 10,
            baseQuantity = "W6 Blätter",
            distributions = listOf(
                HerbDistribution(Landscape.RIVER_LAKE, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.FOREST_EDGE, Occurrence.VERY_RARE)
            ),
            harvestMonths = ALL_MONTHS,
            pageReference = 262
        ),
        Herb(
            name = "Sansaro",
            identificationDifficulty = 12,
            baseQuantity = "eine Pflanze",
            distributions = listOf(
                HerbDistribution(Landscape.SEA, Occurrence.RARE)
            ),
            harvestMonths = ALL_MONTHS,
            pageReference = 262
        ),
        Herb(
            name = "Satuariensbusch",
            identificationDifficulty = -2,
            baseQuantity = "4W20 Blätter, W20 Blüten, W20 Früchte, W3 Flux Saft",
            distributions = listOf(
                HerbDistribution(Landscape.FOREST, Occurrence.OCCASIONAL),
                HerbDistribution(Landscape.FOREST_EDGE, Occurrence.OCCASIONAL)
            ),
            harvestMonths = listOf(
                DerianMonth.PRAIOS,
                DerianMonth.EFFERD,
                DerianMonth.TRAVIA,
                DerianMonth.PHEX,
                DerianMonth.PERAINE,
                DerianMonth.INGERIMM,
                DerianMonth.RAHJA,
                DerianMonth.NAMELESS_DAYS
            ),
            pageReference = 263
        ),
        Herb(
            name = "Schlangenzünglein",
            identificationDifficulty = 3,
            baseQuantity = "Saft einer Pflanze",
            distributions = listOf(
                HerbDistribution(Landscape.RIVER_LAKE, Occurrence.RARE),
                HerbDistribution(Landscape.SWAMP, Occurrence.RARE),
                HerbDistribution(Landscape.RAINFOREST, Occurrence.VERY_RARE)
            ),
            harvestMonths = ALL_MONTHS,
            pageReference = 263
        ),
        Herb(
            name = "Schleichender Tod",
            identificationDifficulty = 6,
            baseQuantity = "W6 Blüten",
            distributions = listOf(
                HerbDistribution(Landscape.RIVER_LAKE, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.FOREST_EDGE, Occurrence.VERY_RARE)
            ),
            harvestMonths = listOf(
                DerianMonth.INGERIMM,
                DerianMonth.RAHJA
            ),
            pageReference = 264
        ),
        Herb(
            name = "Schleimiger Sumpfknöterich",
            identificationDifficulty = 3,
            baseQuantity = "2W6 Pilze",
            distributions = listOf(
                HerbDistribution(Landscape.FOREST, Occurrence.RARE),
                HerbDistribution(Landscape.FOREST_EDGE, Occurrence.VERY_RARE)
            ),
            harvestMonths = listOf(
                DerianMonth.PRAIOS,
                DerianMonth.RONDRA,
                DerianMonth.EFFERD,
                DerianMonth.TRAVIA
            ),
            pageReference = 264,
            dangers = "Die Berührung mit bloßer Haut verursacht 3 SP pro Pilz."
        ),
        Herb(
            name = "Schlinggras",
            identificationDifficulty = 12,
            baseQuantity = "",
            distributions = listOf(
                HerbDistribution(Landscape.SWAMP, Occurrence.RARE)
            ),
            harvestMonths = ALL_MONTHS,
            pageReference = 265,
            dangers = "Eine IN Probe klärt ob man rechtzeitig auf das Schlinggras aufmerksam wird um es zu umgehen. Andernfalls versucht die Pflanze einen zu packen und ins Moor zu ziehen."
        ),
        Herb(
            name = "Schwarmschwamm",
            identificationDifficulty = 3,
            baseQuantity = "ein Schwamm und W2 Samenkörper",
            distributions = listOf(
                HerbDistribution(Landscape.RIVER_LAKE, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.RIVER_FLOODPLAINS, Occurrence.VERY_RARE)
            ),
            harvestMonths = ALL_MONTHS,
            pageReference = 265
        ),
        Herb(
            name = "Schwarzer Lotus",
            identificationDifficulty = 6,
            baseQuantity = "W6 Blüten",
            distributions = listOf(
                HerbDistribution(Landscape.RIVER_LAKE, Occurrence.VERY_RARE)
            ),
            harvestMonths = listOf(
                DerianMonth.PRAIOS,
                DerianMonth.INGERIMM,
                DerianMonth.RAHJA,
                DerianMonth.NAMELESS_DAYS
            ),
            pageReference = 246,
            dangers = "Im Umkreis von 5 Schritt ist eine KO Probe nötig um den giftigen Blütenstaub nicht einzuatmen. Fehlende Punkte entsprechen der Zahl an eingeatmeten Dosen."
        ),
        Herb(
            name = "Schwarzer Mohn",
            identificationDifficulty = 5,
            baseQuantity = "2 Blätter und eine geschlossene Samenkapsel",
            distributions = listOf(
                HerbDistribution(Landscape.STEPPE, Occurrence.VERY_COMMON),
                HerbDistribution(Landscape.GRASSLANDS, Occurrence.VERY_COMMON),
                HerbDistribution(Landscape.RIVER_LAKE, Occurrence.VERY_COMMON),
                HerbDistribution(Landscape.RIVER_FLOODPLAINS, Occurrence.VERY_COMMON),
                HerbDistribution(Landscape.FOREST, Occurrence.VERY_COMMON),
                HerbDistribution(Landscape.FOREST_EDGE, Occurrence.VERY_COMMON)
            ),
            harvestMonths = listOf(
                DerianMonth.EFFERD,
                DerianMonth.TRAVIA,
                DerianMonth.BORON
            ),
            pageReference = 253
        ),
        Herb(
            name = "Schwarzer Wein",
            identificationDifficulty = 2,
            baseQuantity = "",
            distributions = listOf(
                HerbDistribution(Landscape.HIGHLANDS, Occurrence.OCCASIONAL),
                HerbDistribution(Landscape.STEPPE, Occurrence.OCCASIONAL),
                HerbDistribution(Landscape.FOREST, Occurrence.COMMON)
            ),
            harvestMonths = ALL_MONTHS,
            pageReference = 266,
            dangers = "Schwarzer Wein bildet nur dann Früchte aus, wenn zuvor Menschen ausgesaugt wurden. Außerdem sind die Ranken gefährlich und giftig. Nur bei mehr als 7 TaP* kann man einige Beeren finden ohne zuvor Menschen opfern zu müssen."
        ),
        Herb(
            name = "Seelenhauch",
            identificationDifficulty = 3,
            baseQuantity = "eine Blüte",
            distributions = listOf(
                HerbDistribution(Landscape.HIGHLANDS, Occurrence.RARE),
                HerbDistribution(Landscape.STEPPE, Occurrence.RARE),
                HerbDistribution(Landscape.GRASSLANDS, Occurrence.RARE),
                HerbDistribution(Landscape.RIVER_LAKE, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.RIVER_FLOODPLAINS, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.SWAMP, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.RAINFOREST, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.FOREST, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.FOREST_EDGE, Occurrence.VERY_RARE)
            ),
            harvestMonths = listOf(
                DerianMonth.PRAIOS,
                DerianMonth.INGERIMM,
                DerianMonth.RAHJA,
                DerianMonth.NAMELESS_DAYS
            ),
            pageReference = 195
        ),
        Herb(
            name = "Shurinstrauch",
            identificationDifficulty = 2,
            baseQuantity = "W20 Knollen",
            distributions = listOf(
                HerbDistribution(Landscape.STEPPE, Occurrence.RARE),
                HerbDistribution(Landscape.GRASSLANDS, Occurrence.RARE),
                HerbDistribution(Landscape.FOREST, Occurrence.VERY_RARE)
            ),
            harvestMonths = listOf(
                DerianMonth.PRAIOS,
                DerianMonth.RONDRA,
                DerianMonth.INGERIMM,
                DerianMonth.RAHJA,
                DerianMonth.NAMELESS_DAYS
            ),
            pageReference = 267
        ),
        Herb(
            name = "Steinrinde",
            identificationDifficulty = 12,
            baseQuantity = "1W6 Stein",
            distributions = listOf(
                HerbDistribution(Landscape.CAVE_WET, Occurrence.RARE),
                HerbDistribution(Landscape.CAVE_DRY, Occurrence.RARE)
            ),
            harvestMonths = ALL_MONTHS,
            pageReference = 195
        ),
        Herb(
            name = "Talaschin",
            identificationDifficulty = 5,
            baseQuantity = "W6 Flechten",
            distributions = listOf(
                HerbDistribution(Landscape.MOUNTAINS, Occurrence.OCCASIONAL),
                HerbDistribution(Landscape.ICE, Occurrence.RARE),
                HerbDistribution(Landscape.DESERT, Occurrence.VERY_RARE)
            ),
            harvestMonths = ALL_MONTHS,
            pageReference = 268
        ),
        Herb(
            name = "Tarnblatt",
            identificationDifficulty = 8,
            baseQuantity = "",
            distributions = listOf(
                HerbDistribution(Landscape.RAINFOREST, Occurrence.RARE),
                HerbDistribution(Landscape.FOREST, Occurrence.RARE)
            ),
            harvestMonths = ALL_MONTHS,
            pageReference = 268,
            dangers = "Tarnblatt ist leicht giftig und gibt sich je nach Jahreszeit als eine andere Pflanze aus."
        ),
        Herb(
            name = "Tarnele",
            identificationDifficulty = 4,
            baseQuantity = "eine Pflanze",
            distributions = listOf(
                HerbDistribution(Landscape.HIGHLANDS, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.GRASSLANDS, Occurrence.COMMON),
                HerbDistribution(Landscape.RIVER_FLOODPLAINS, Occurrence.COMMON),
                HerbDistribution(Landscape.SWAMP, Occurrence.OCCASIONAL),
                HerbDistribution(Landscape.FOREST, Occurrence.OCCASIONAL)
            ),
            harvestMonths = listOf(
                DerianMonth.PRAIOS,
                DerianMonth.RONDRA,
                DerianMonth.EFFERD,
                DerianMonth.TRAVIA,
                DerianMonth.INGERIMM,
                DerianMonth.RAHJA,
                DerianMonth.NAMELESS_DAYS
            ),
            pageReference = 268
        ),
        Herb(
            name = "Thonnys",
            identificationDifficulty = 12,
            baseQuantity = "W6+4 Blätter",
            distributions = listOf(
                HerbDistribution(Landscape.STEPPE, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.FOREST, Occurrence.VERY_RARE)
            ),
            harvestMonths = listOf(
                DerianMonth.PRAIOS,
                DerianMonth.RONDRA,
                DerianMonth.EFFERD,
                DerianMonth.TRAVIA,
                DerianMonth.PERAINE,
                DerianMonth.INGERIMM,
                DerianMonth.RAHJA,
                DerianMonth.NAMELESS_DAYS
            ),
            pageReference = 269
        ),
        Herb(
            name = "Tigermohn",
            identificationDifficulty = 10,
            baseQuantity = "eine geschlossene Samenkapsel",
            distributions = listOf(
                HerbDistribution(Landscape.GRASSLANDS, Occurrence.RARE),
                HerbDistribution(Landscape.RIVER_FLOODPLAINS, Occurrence.RARE),
                HerbDistribution(Landscape.FOREST_EDGE, Occurrence.RARE)
            ),
            harvestMonths = listOf(
                DerianMonth.TRAVIA
            ),
            pageReference = 254
        ),
        Herb(
            name = "Traschbart",
            identificationDifficulty = 6,
            baseQuantity = "W6 Flechten",
            distributions = listOf(
                HerbDistribution(Landscape.SWAMP, Occurrence.OCCASIONAL),
                HerbDistribution(Landscape.FOREST, Occurrence.COMMON)
            ),
            harvestMonths = ALL_MONTHS,
            pageReference = 269
        ),
        Herb(
            name = "Trichterwurzel",
            identificationDifficulty = 11,
            baseQuantity = "",
            distributions = listOf(
                HerbDistribution(Landscape.FOREST, Occurrence.OCCASIONAL),
                HerbDistribution(Landscape.FOREST_EDGE, Occurrence.VERY_RARE)
            ),
            harvestMonths = ALL_MONTHS,
            pageReference = 270,
            dangers = "Nur eine Sinnesschärfe-Probe +8 erlaubt es die Grube der Trichterwurzel rechtzeitig zu erkennen. Andernfalls fällt man in diese hinein und wird zusätzlich von Wurzeln attackiert."
        ),
        Herb(
            name = "Tuur-Amash-Kelch",
            identificationDifficulty = 1,
            baseQuantity = "W6+3 Kelche",
            distributions = listOf(
                HerbDistribution(Landscape.FOREST, Occurrence.RARE),
                HerbDistribution(Landscape.FOREST_EDGE, Occurrence.VERY_RARE)
            ),
            harvestMonths = ALL_MONTHS,
            pageReference = 270,
            dangers = "Nur eine Sinnesschärfe-Probe +7 erlaubt es den Tuur-Amash-Kelch rechtzeitig zu entdecken. Andernfalls greift eine Pflanze an und kurz darauf weitere. Nur bei mehr als 13 TaP* findet sich auch eine Beere."
        ),
        Herb(
            name = "Ulmenwürger",
            identificationDifficulty = 2,
            baseQuantity = "W20 Blüten",
            distributions = listOf(
                HerbDistribution(Landscape.FOREST, Occurrence.OCCASIONAL),
                HerbDistribution(Landscape.FOREST_EDGE, Occurrence.RARE)
            ),
            harvestMonths = listOf(
                DerianMonth.EFFERD,
                DerianMonth.TRAVIA
            ),
            pageReference = 271
        ),
        Herb(
            name = "Vierblättrige Einbeere",
            identificationDifficulty = 5,
            baseQuantity = "W6 Beeren",
            distributions = listOf(
                HerbDistribution(Landscape.ICE, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.MOUNTAINS, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.HIGHLANDS, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.STEPPE, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.GRASSLANDS, Occurrence.OCCASIONAL),
                HerbDistribution(Landscape.RIVER_LAKE, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.COAST, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.RIVER_FLOODPLAINS, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.SWAMP, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.FOREST, Occurrence.COMMON),
                HerbDistribution(Landscape.FOREST_EDGE, Occurrence.COMMON)
            ),
            harvestMonths = listOf(
                DerianMonth.PRAIOS,
                DerianMonth.RONDRA,
                DerianMonth.EFFERD,
                DerianMonth.TRAVIA,
                DerianMonth.BORON,
                DerianMonth.HESINDE,
                DerianMonth.PHEX,
                DerianMonth.PERAINE,
                DerianMonth.INGERIMM,
                DerianMonth.RAHJA,
                DerianMonth.NAMELESS_DAYS
            ),
            pageReference = 271
        ),
        Herb(
            name = "Vragieswurzel",
            identificationDifficulty = 6,
            baseQuantity = "eine Wurzel",
            distributions = listOf(
                HerbDistribution(Landscape.MOUNTAINS, Occurrence.RARE),
                HerbDistribution(Landscape.HIGHLANDS, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.RAINFOREST, Occurrence.OCCASIONAL),
                HerbDistribution(Landscape.FOREST, Occurrence.RARE)
            ),
            harvestMonths = listOf(
                DerianMonth.EFFERD,
                DerianMonth.TRAVIA
            ),
            pageReference = 272
        ),
        Herb(
            name = "Waldwebe",
            identificationDifficulty = 9,
            baseQuantity = "",
            distributions = listOf(
                HerbDistribution(Landscape.FOREST, Occurrence.OCCASIONAL)
            ),
            harvestMonths = ALL_MONTHS,
            pageReference = 272,
            dangers = "Sofern keine besonderne Umstände eine leichte Erkennung erlauben, ist eine Sinnensschärfe-Probe +12 nötig um das Netz der Waldwebe zu erkennen. Andernfalls verfängt man sich im Netz."
        ),
        Herb(
            name = "Wandermoos",
            identificationDifficulty = 14,
            baseQuantity = "ein Moosballen",
            distributions = listOf(
                HerbDistribution(Landscape.CAVE_WET, Occurrence.RARE),
                HerbDistribution(Landscape.CAVE_DRY, Occurrence.VERY_RARE)
            ),
            harvestMonths = ALL_MONTHS,
            pageReference = 196
        ),
        Herb(
            name = "Wasserrausch",
            identificationDifficulty = 1,
            baseQuantity = "2W20 Blüten",
            distributions = listOf(
                HerbDistribution(Landscape.RIVER_LAKE, Occurrence.RARE)
            ),
            harvestMonths = listOf(
                DerianMonth.PRAIOS,
                DerianMonth.RONDRA,
                DerianMonth.EFFERD,
                DerianMonth.TRAVIA,
                DerianMonth.RAHJA,
                DerianMonth.NAMELESS_DAYS
            ),
            pageReference = 273,
            dangers = "Im Umkreis von 5 Metern um die Blüten des Wasserrausches ist eine KO+5 Probe nötig. Andernfalls fällt man in berauschende Träume, was für eine Schwimmer den Tod bedeuten kann. Nur bei mehr als 12 TaP* findet sich auch eine Frucht."
        ),
        Herb(
            name = "Winselgras",
            identificationDifficulty = 12,
            baseQuantity = "",
            distributions = listOf(
                HerbDistribution(Landscape.STEPPE, Occurrence.OCCASIONAL),
                HerbDistribution(Landscape.GRASSLANDS, Occurrence.RARE)
            ),
            harvestMonths = ALL_MONTHS,
            pageReference = 273,
            dangers = "Das Heulen des Winselgrases kann einen um den Schlaf bringen und vermindert die nächtliche Regeneration um 2 Punkte."
        ),
        Herb(
            name = "Weißer Lotus",
            identificationDifficulty = 10,
            baseQuantity = "W6+1 Blüten",
            distributions = listOf(
                HerbDistribution(Landscape.RIVER_LAKE, Occurrence.RARE),
                HerbDistribution(Landscape.SWAMP, Occurrence.VERY_RARE)
            ),
            harvestMonths = listOf(
                DerianMonth.PRAIOS,
                DerianMonth.INGERIMM,
                DerianMonth.RAHJA,
                DerianMonth.NAMELESS_DAYS
            ),
            pageReference = 247,
            dangers = "Im Umkreis von 5 Schritt ist eine KO Probe nötig um den giftigen Blütenstaub nicht einzuatmen. Fehlende Punkte entsprechen der Zahl an eingeatmeten Dosen."
        ),
        Herb(
            name = "Weißgelber Lotus",
            identificationDifficulty = 10,
            baseQuantity = "W3 Blüten",
            distributions = listOf(
                HerbDistribution(Landscape.RIVER_LAKE, Occurrence.OCCASIONAL)
            ),
            harvestMonths = listOf(
                DerianMonth.PRAIOS,
                DerianMonth.INGERIMM,
                DerianMonth.RAHJA,
                DerianMonth.NAMELESS_DAYS
            ),
            pageReference = 247,
            dangers = "Im Umkreis von 5 Schritt ist eine KO Probe nötig um den giftigen Blütenstaub nicht einzuatmen. Fehlende Punkte entsprechen der Zahl an eingeatmeten Dosen."
        ),
        Herb(
            name = "Wirselkraut",
            identificationDifficulty = 5,
            baseQuantity = "W6+4 Blätter",
            distributions = listOf(
                HerbDistribution(Landscape.STEPPE, Occurrence.COMMON),
                HerbDistribution(Landscape.GRASSLANDS, Occurrence.OCCASIONAL)
            ),
            harvestMonths = listOf(
                DerianMonth.PRAIOS,
                DerianMonth.RONDRA,
                DerianMonth.EFFERD,
                DerianMonth.TRAVIA,
                DerianMonth.BORON,
                DerianMonth.PHEX,
                DerianMonth.PERAINE,
                DerianMonth.INGERIMM,
                DerianMonth.RAHJA,
                DerianMonth.NAMELESS_DAYS
            ),
            pageReference = 273
        ),
        Herb(
            name = "Würgedattel",
            identificationDifficulty = 5,
            baseQuantity = "",
            distributions = listOf(
                HerbDistribution(Landscape.RAINFOREST, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.FOREST_EDGE, Occurrence.VERY_RARE)
            ),
            harvestMonths = ALL_MONTHS,
            pageReference = 274,
            dangers = "Wer eine Frucht der Würgedattel berührt, wird von den Würgeschlingen der Pflanze attackiert."
        ),
        Herb(
            name = "Yaganstrauch",
            identificationDifficulty = 6,
            baseQuantity = "W6 Nüsse",
            distributions = listOf(
                HerbDistribution(Landscape.HIGHLANDS, Occurrence.RARE),
                HerbDistribution(Landscape.FOREST, Occurrence.OCCASIONAL)
            ),
            harvestMonths = listOf(
                DerianMonth.BORON
            ),
            pageReference = 274
        ),
        Herb(
            name = "Zithabar",
            identificationDifficulty = 5,
            baseQuantity = "3W20 Blätter",
            distributions = listOf(
                HerbDistribution(Landscape.RIVER_LAKE, Occurrence.COMMON),
                HerbDistribution(Landscape.SWAMP, Occurrence.OCCASIONAL),
                HerbDistribution(Landscape.FOREST, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.FOREST_EDGE, Occurrence.RARE)
            ),
            harvestMonths = listOf(
                DerianMonth.PRAIOS,
                DerianMonth.PERAINE,
                DerianMonth.INGERIMM,
                DerianMonth.RAHJA,
                DerianMonth.NAMELESS_DAYS
            ),
            pageReference = 275
        ),
        Herb(
            name = "Zunderschwamm",
            identificationDifficulty = 4,
            baseQuantity = "W6 Pilze",
            distributions = listOf(
                HerbDistribution(Landscape.RAINFOREST, Occurrence.OCCASIONAL),
                HerbDistribution(Landscape.FOREST, Occurrence.COMMON)
            ),
            harvestMonths = ALL_MONTHS,
            pageReference = 275
        ),
        Herb(
            name = "Zwölfblatt",
            identificationDifficulty = 5,
            baseQuantity = "12 Stängel",
            distributions = listOf(
                HerbDistribution(Landscape.HIGHLANDS, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.STEPPE, Occurrence.OCCASIONAL),
                HerbDistribution(Landscape.GRASSLANDS, Occurrence.RARE),
                HerbDistribution(Landscape.SWAMP, Occurrence.VERY_RARE),
                HerbDistribution(Landscape.FOREST, Occurrence.OCCASIONAL)
            ),
            harvestMonths = listOf(
                DerianMonth.PRAIOS,
                DerianMonth.HESINDE,
                DerianMonth.FIRUN,
                DerianMonth.TSA,
                DerianMonth.PHEX,
                DerianMonth.PERAINE,
                DerianMonth.INGERIMM,
                DerianMonth.RAHJA,
                DerianMonth.NAMELESS_DAYS
            ),
            pageReference = 276
        )
    )

    /**
     * Findet ein Kraut nach Namen
     */
    fun findHerbByName(name: String): Herb? {
        return ALL_HERBS.find { it.name == name }
    }
    
    /**
     * Filtert Kräuter nach Verfügbarkeit in einer Landschaft
     */
    fun getHerbsForLandscape(landscape: Landscape): List<Herb> {
        return ALL_HERBS.filter { it.isAvailableInLandscape(landscape) }
    }
    
    /**
     * Filtert Kräuter nach Verfügbarkeit in einem Monat
     */
    fun getHerbsForMonth(month: DerianMonth): List<Herb> {
        return ALL_HERBS.filter { it.isAvailableInMonth(month) }
    }
}
