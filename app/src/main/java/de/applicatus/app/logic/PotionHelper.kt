package de.applicatus.app.logic

import kotlin.random.Random

/**
 * Hilfsfunktionen für die Trankverwaltung
 * 
 * Hinweise:
 * - Für Datums-Berechnungen: Verwenden Sie DerianDateCalculator
 * - Für Ablaufdaten mit Würfelunterstützung: DerianDateCalculator.calculateExpiryDate()
 * - Für Datums-Parsing und Formatierung: DerianDateCalculator.parseDerischenDate() / formatDerischenDate()
 */
object PotionHelper {
    
    // Konsistenz-Kategorien mit passenden Behältern und Gerüchen
    private data class ConsistencyInfo(
        val name: String,
        val containers: List<String>,
        val colors: List<ColorInfo>,
        val smells: List<String>
    )
    
    private data class ColorInfo(
        val nominative: String,  // z.B. "rote"
        val genitive: String     // z.B. "roter" (für "Phiole mit roter Flüssigkeit")
    )
    
    // Muster für verschiedene Konsistenzen
    private val liquidKeywords = listOf("elixier", "trank", "trunk", "öl", "tränen", "tinte", "wasser", "tropfen", "speichel")
    private val powderKeywords = listOf("pulver", "staub")
    private val pasteKeywords = listOf("salbe", "balsam", "creme")
    private val solidKeywords = listOf("kreide", "kerzen", "pastillen", "kraut", "pillen")
    private val specialContainerKeywords = listOf("töpfchen", "beutel")
    
    // Definierte Farben mit korrekten grammatikalischen Formen
    private val colorsPalette = listOf(
        ColorInfo("blaue", "blauer"),
        ColorInfo("rote", "roter"),
        ColorInfo("grüne", "grüner"),
        ColorInfo("goldene", "goldener"),
        ColorInfo("silberne", "silberner"),
        ColorInfo("violette", "violetter"),
        ColorInfo("braune", "brauner"),
        ColorInfo("schwarze", "schwarzer"),
        ColorInfo("weiße", "weißer"),
        ColorInfo("transparente", "transparenter"),
        ColorInfo("bernsteinfarbene", "bernsteinfarbener"),
        ColorInfo("türkise", "türkiser"),
        ColorInfo("rosa", "rosa"),
        ColorInfo("orangefarbene", "orangefarbener"),
        ColorInfo("grau-grüne", "grau-grüner"),
        ColorInfo("purpurne", "purpurner"),
        ColorInfo("tiefblaue", "tiefblauer"),
        ColorInfo("hellgrüne", "hellgrüner"),
        ColorInfo("dunkelrote", "dunkelroter")
    )
    
    // Gerüche nach Kategorie
    private val liquidSmells = listOf(
        "süßlichem Duft", "herbem Aroma", "scharfem Geruch", "neutralem Geruch", 
        "würzigem Duft", "blumigem Duft", "medizinischem Geruch", "fruchtigen Noten"
    )
    
    private val powderSmells = listOf(
        "staubigem Geruch", "würzigem Aroma", "scharfem Duft", "erdigen Noten",
        "kräuterigem Geruch", "leicht süßlichem Duft"
    )
    
    private val pasteSmells = listOf(
        "herbem Duft", "fettigem Geruch", "medizinischem Aroma", "würzigen Noten",
        "intensivem Kräutergeruch", "balsamischem Duft"
    )
    
    private val unpleasantSmells = listOf(
        "unangenehmen Gestank", "beißendem Geruch", "fauligem Odeur", "stechenden Dämpfen"
    )
    
    // Konsistenz-Definitionen
    private val liquidConsistency = ConsistencyInfo(
        name = "Flüssigkeit",
        containers = listOf("Phiole", "Tonfläschchen", "Glasflasche", "Kristallflakon", "Ampulle"),
        colors = colorsPalette,
        smells = liquidSmells
    )
    
    private val powderConsistency = ConsistencyInfo(
        name = "Pulver",
        containers = listOf("Lederbeutel", "Stoffbeutel", "Keramiktiegel", "Holzdose", "Pergamenttütchen"),
        colors = colorsPalette,
        smells = powderSmells
    )
    
    private val pasteConsistency = ConsistencyInfo(
        name = "Salbe",
        containers = listOf("Tiegel", "Keramikgefäß", "Holzdose", "Zinndose"),
        colors = colorsPalette,
        smells = pasteSmells
    )
    
    private val tinkturConsistency = ConsistencyInfo(
        name = "Tinktur",
        containers = listOf("Phiole", "Tropfflasche", "Glasfläschchen"),
        colors = colorsPalette,
        smells = liquidSmells
    )
    
    /**
     * Generiert ein zufälliges Trank-Aussehen basierend auf dem Rezeptnamen
     */
    fun generateRandomAppearance(recipeName: String = ""): String {
        val nameLower = recipeName.lowercase()
        
        // Bestimme Konsistenz basierend auf Rezeptnamen
        val consistency = when {
            // Spezielle Behälter im Namen
            nameLower.contains("töpfchen") -> return generateSpecialAppearance("Töpfchen", pasteSmells + unpleasantSmells)
            nameLower.contains("beutel") -> return generateSpecialAppearance("Beutel", powderSmells)
            nameLower.contains("kerzen") -> return "Beschwörungskerzen aus dunklem Wachs mit eingraviertem Pentagramm"
            nameLower.contains("kreide") -> return generateChalkAppearance(nameLower)
            nameLower.contains("kraut") -> return "Getrocknete Kräuterbündel in Leinentuch eingeschlagen"
            
            // Flüssigkeiten
            liquidKeywords.any { nameLower.contains(it) } -> liquidConsistency
            
            // Pulver
            powderKeywords.any { nameLower.contains(it) } -> powderConsistency
            
            // Salben/Pasten
            pasteKeywords.any { nameLower.contains(it) } -> pasteConsistency
            
            // Spezialfall: Pastillen/Pillen
            solidKeywords.any { nameLower.contains(it) } -> {
                val color = colorsPalette.random()
                return "Kleine ${color.nominative} Pastillen in einem Stoffbeutel"
            }
            
            // Spezialfall: "Hauch" - meist gasförmig/räucherwerk
            nameLower.contains("hauch") -> return generateHauchAppearance()
            
            // Spezialfall: "Feuer" - brennbare Substanz
            nameLower.contains("feuer") -> return "Dickflüssige, ölige Substanz in einer feuerfesten Keramikflasche"
            
            // Spezialfall: Licht-Effekte
            nameLower.contains("licht") -> return "Leuchtende Substanz in einer dunklen Glasphiole"
            
            // Standard: Flüssigkeit oder Tinktur
            else -> if (Random.nextBoolean()) liquidConsistency else tinkturConsistency
        }
        
        // Generiere Beschreibung
        return when (Random.nextInt(4)) {
            0 -> generateContainerWithColor(consistency)
            1 -> generateContainerWithSmell(consistency)
            2 -> generateColorFirst(consistency)
            else -> generateDetailedDescription(consistency)
        }
    }
    
    private fun generateContainerWithColor(info: ConsistencyInfo): String {
        val container = info.containers.random()
        val color = info.colors.random()
        return "$container mit ${color.genitive} ${info.name}"
    }
    
    private fun generateContainerWithSmell(info: ConsistencyInfo): String {
        val container = info.containers.random()
        val smell = info.smells.random()
        return "$container mit $smell"
    }
    
    private fun generateColorFirst(info: ConsistencyInfo): String {
        val container = info.containers.random()
        val color = info.colors.random()
        return "${color.nominative.replaceFirstChar { it.uppercaseChar() }} ${info.name} in $container"
    }
    
    private fun generateDetailedDescription(info: ConsistencyInfo): String {
        val container = info.containers.random()
        val color = info.colors.random()
        val smell = info.smells.random()
        return "$container mit ${color.genitive} ${info.name} und $smell"
    }
    
    private fun generateSpecialAppearance(containerType: String, smells: List<String>): String {
        val smell = smells.random()
        return "$containerType mit $smell"
    }
    
    private fun generateChalkAppearance(nameLower: String): String {
        val color = if (nameLower.contains("schatten")) {
            "Schwarze"
        } else if (nameLower.contains("zauber")) {
            listOf("Silberne", "Goldene", "Weiße").random()
        } else {
            colorsPalette.random().nominative.replaceFirstChar { it.uppercaseChar() }
        }
        return "$color Kreidestücke in Pergamentpapier eingewickelt"
    }
    
    private fun generateHauchAppearance(): String {
        val options = listOf(
            "Räucherstäbchen mit süßlichem Duft in einer Holzschachtel",
            "Feine Kristalle, die beim Verbrennen Rauch entwickeln",
            "Getrocknete Kräutermischung für Räucherwerk",
            "Duftendes Harz in einem kleinen Lederbeutel"
        )
        return options.random()
    }
}
