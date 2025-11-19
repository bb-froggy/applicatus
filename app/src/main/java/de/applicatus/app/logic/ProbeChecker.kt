package de.applicatus.app.logic

import de.applicatus.app.data.model.character.Character
import de.applicatus.app.data.model.spell.Spell
import de.applicatus.app.data.model.spell.SystemSpell
import de.applicatus.app.data.model.talent.Talent
import kotlin.random.Random

/**
 * Ergebnis einer Drei-Eigenschafts-Probe (z.B. Talent, Zauber)
 */
data class ProbeResult(
    val success: Boolean,              // War die Probe erfolgreich?
    val qualityPoints: Int,            // Qualitätspunkte (TaP*/ZfP*) übrig
    val rolls: List<Int>,              // Die drei Würfelwürfe
    val message: String,               // Beschreibung des Ergebnisses
    val isDoubleOne: Boolean = false,  // Doppel-1 gewürfelt
    val isTripleOne: Boolean = false,  // Dreifach-1 gewürfelt
    val isDoubleTwenty: Boolean = false,  // Doppel-20 gewürfelt
    val isTripleTwenty: Boolean = false   // Dreifach-20 gewürfelt
)

/**
 * Ergebnis einer einfachen Attributsprobe (z.B. KO-Probe, IN-Probe)
 */
data class AttributeProbeResult(
    val success: Boolean,
    val roll: Int,
    val attribute: Int
)

/**
 * Zentrale Klasse für die Durchführung von DSA-Proben
 * 
 * Diese Klasse kapselt die Logik für:
 * - Drei-Eigenschafts-Proben (Talente, Zauber)
 * - Einfache Attributsproben
 * - Würfelwürfe (W20, W6)
 * 
 * Alle anderen Klassen (SpellChecker, ElixirAnalyzer, PotionAnalyzer, etc.)
 * sollten diese zentrale Implementierung verwenden.
 */
object ProbeChecker {
    
    /**
     * Führt eine Drei-Eigenschafts-Probe durch (z.B. Talent- oder Zauberprobe)
     * 
     * DSA-Regelwerk:
     * - 3x W20 würfeln
     * - Jeder Wurf wird gegen eine Eigenschaft geprüft
     * - Bei Überwurf: Differenz von FW abziehen
     * - Erfolg bei FP >= 0
     * - Doppel-1/Dreifach-1: Automatischer Erfolg mit max FP
     * - Doppel-20/Dreifach-20: Automatischer Patzer
     * 
     * Magisches Meisterhandwerk:
     * - Bei AE-Ausgabe: TaW wird um 2 pro AE erhöht
     * - Maximum: 2x des ursprünglichen TaW
     * 
     * @param fertigkeitswert Fertigkeitswert (FW): Talentwert oder Zauberfertigkeit
     * @param difficulty Erschwernis (positiv) oder Erleichterung (negativ)
     * @param attribute1 Erster Eigenschaftswert
     * @param attribute2 Zweiter Eigenschaftswert
     * @param attribute3 Dritter Eigenschaftswert
     * @param astralEnergyCost AE-Kosten für Magisches Meisterhandwerk (0 = keine AE ausgeben)
     * @param diceRoll Lambda für Würfelwürfe (Standard: W20, überschreibbar für Tests)
     * @param qualityPointName Name der Qualitätspunkte (z.B. "TaP*", "ZfP*")
     * @return ProbeResult mit allen Details
     */
    fun performThreeAttributeProbe(
        fertigkeitswert: Int,
        difficulty: Int,
        attribute1: Int,
        attribute2: Int,
        attribute3: Int,
        astralEnergyCost: Int = 0,
        diceRoll: () -> Int = { rollD20() },
        qualityPointName: String = "FP*"
    ): ProbeResult {
        // Berechne effektiven Fertigkeitswert mit AE-Bonus
        // +2 TaW pro AE, maximal auf 2x TaW
        val originalFW = fertigkeitswert
        val aeBonus = astralEnergyCost * 2
        val bonusFW = originalFW + aeBonus
        
        // Deckelung: Bonus darf nicht mehr als originalFW sein (also max 2x originalFW)
        // Bei negativem FW: max(-5 + bonus, -10) würde -10 geben, aber wir wollen -5
        // Bei positivem FW: min(10 + bonus, 20) gibt korrekt 20
        val effectiveFW = if (originalFW >= 0) {
            minOf(bonusFW, originalFW * 2)
        } else {
            maxOf(bonusFW, originalFW * 2)
        }
        
        // Würfle 3x W20
        val rolls = List(3) { diceRoll() }
        
        // Prüfe auf besondere Fälle
        val countOnes = rolls.count { it == 1 }
        val countTwenties = rolls.count { it == 20 }
        
        // Dreifach-1: Automatischer Erfolg mit Maximum
        if (countOnes >= 3) {
            return ProbeResult(
                success = true,
                qualityPoints = effectiveFW,
                rolls = rolls,
                message = "Dreifach-1! Legendär!",
                isTripleOne = true
            )
        }
        
        // Doppel-1: Automatischer Erfolg mit Maximum
        if (countOnes >= 2) {
            return ProbeResult(
                success = true,
                qualityPoints = effectiveFW,
                rolls = rolls,
                message = "Doppel-1! Meisterwerk!",
                isDoubleOne = true
            )
        }
        
        // Dreifach-20: Automatischer Patzer
        if (countTwenties >= 3) {
            return ProbeResult(
                success = false,
                qualityPoints = 0,
                rolls = rolls,
                message = "Dreifach-20! Katastrophaler Patzer!",
                isTripleTwenty = true
            )
        }
        
        // Doppel-20: Automatischer Patzer
        if (countTwenties >= 2) {
            return ProbeResult(
                success = false,
                qualityPoints = 0,
                rolls = rolls,
                message = "Doppel-20! Patzer!",
                isDoubleTwenty = true
            )
        }
        
        // Berechne Fertigkeitspunkte (FP* = FW - Erschwernis)
        var qualityPoints = effectiveFW - difficulty
        
        val attributes = listOf(attribute1, attribute2, attribute3)
        val success: Boolean
        
        if (effectiveFW > difficulty) {
            // Normale Probe: FW > Erschwernis
            // Prüfe jeden Würfelwurf gegen die Eigenschaft
            rolls.forEachIndexed { index, roll ->
                val attribute = attributes[index]
                if (roll > attribute) {
                    // Überwurf: Differenz von FP* abziehen
                    val difference = roll - attribute
                    qualityPoints -= difference
                }
            }
            
            // Erfolg wenn FP* >= 0
            success = qualityPoints >= 0
            
            // FP* werden auf FW gedeckelt (kann nicht höher sein als FW)
            if (success) {
                qualityPoints = minOf(qualityPoints, effectiveFW)
            } else {
                qualityPoints = 0
            }
        } else {
            // Erschwerte Probe: FW <= Erschwernis
            // Jede Eigenschaft muss um die Erschwernis unterwürfelt werden
            val effectiveDifficulty = difficulty - effectiveFW
            qualityPoints = 0 // Besser kann es nicht werden
            success = rolls.zip(attributes).all { (roll, attribute) ->
                roll + effectiveDifficulty <= attribute
            }
        }
        
        // Ein Erfolg hat immer mindestens 1 FP*
        if (success && qualityPoints == 0) {
            qualityPoints = 1
        }
        
        val message = if (success) {
            "Probe erfolgreich mit $qualityPoints $qualityPointName!"
        } else {
            "Probe fehlgeschlagen!"
        }
        
        return ProbeResult(
            success = success,
            qualityPoints = qualityPoints,
            rolls = rolls,
            message = message
        )
    }
    
    /**
     * Führt eine einfache Attributsprobe durch (z.B. KO-Probe)
     * 
     * @param attributeValue Wert der Eigenschaft
     * @param difficulty Erschwernis (Standard 0)
     * @param diceRoll Lambda für Würfelwurf (Standard: W20)
     * @return AttributeProbeResult mit Erfolg, Wurf und Eigenschaftswert
     */
    fun performAttributeProbe(
        attributeValue: Int,
        difficulty: Int = 0,
        diceRoll: () -> Int = { rollD20() }
    ): AttributeProbeResult {
        val roll = diceRoll()
        val success = roll + difficulty <= attributeValue
        return AttributeProbeResult(
            success = success,
            roll = roll,
            attribute = attributeValue
        )
    }
    
    /**
     * Würfelt einen W20 (1-20)
     */
    fun rollD20(): Int = Random.nextInt(1, 21)
    
    /**
     * Würfelt einen W6 (1-6)
     */
    fun rollD6(): Int = Random.nextInt(1, 7)
    
    /**
     * Würfelt N W20 und gibt eine Liste zurück
     */
    fun rollMultipleD20(count: Int): List<Int> = List(count) { rollD20() }
    
    /**
     * Parst und würfelt eine Würfelnotation
     * 
     * Beispiele:
     * - "3W6" → Würfelt 3 sechsseitige Würfel und summiert
     * - "2W20+5" → Würfelt 2 zwanzigseitige Würfel, addiert 5
     * - "1W6-2" → Würfelt 1 sechsseitigen Würfel, subtrahiert 2
     * 
     * @param diceNotation Würfelnotation (z.B. "3W6+2", "2W20-5", "1W10")
     * @param diceRoll Lambda für einzelne Würfelwürfe (Standard: Random, überschreibbar für Tests)
     * @return Gewürfeltes Ergebnis oder null bei ungültiger Notation
     */
    fun rollDice(
        diceNotation: String,
        diceRoll: (diceSize: Int) -> Int = { diceSize -> Random.nextInt(1, diceSize + 1) }
    ): Int? {
        // Regex für Würfelnotationen: z.B. "3W6+2", "2W20-5", "1W10"
        val diceRegex = Regex("""(\d+)W(\d+)([+\-]\d+)?""", RegexOption.IGNORE_CASE)
        val match = diceRegex.matchEntire(diceNotation.trim()) ?: return null
        
        val numDice = match.groupValues[1].toIntOrNull() ?: return null
        val diceSize = match.groupValues[2].toIntOrNull() ?: return null
        val modifier = match.groupValues[3].ifEmpty { "+0" }.toIntOrNull() ?: 0
        
        if (numDice < 1 || diceSize < 1) return null
        
        // Würfle alle Würfel und summiere
        var total = 0
        repeat(numDice) {
            total += diceRoll(diceSize)
        }
        
        return total + modifier
    }
    
    data class DurationEvaluation(val amount: Int, val unit: String) {
        fun toShelfLifeString(): String = "$amount $unit"
    }
    
    private val supportedDurationUnits = setOf(
        "tag", "tage",
        "woche", "wochen",
        "mond", "monde",
        "monat", "monate",
        "jahr", "jahre"
    )
    
    /**
     * Parst eine Wirkdauerbeschreibung (z.B. "3*ZfP*+2 Tage") und liefert die tatsächliche Dauer.
     * Unterstützt Addition, Subtraktion und Multiplikation sowie Terme aus Zahlen, ZfP* und Würfelwürfen.
     */
    fun evaluateDurationSpecification(
        specification: String,
        zfpStar: Int,
        diceRoll: (diceSize: Int) -> Int = { diceSize -> Random.nextInt(1, diceSize + 1) }
    ): DurationEvaluation? {
        val trimmed = specification.trim()
        if (trimmed.isBlank()) return null
        val parts = trimmed.split(Regex("\\s+"))
        if (parts.size < 2) return null
        val unitRaw = parts.last()
        val unitNormalized = unitRaw.lowercase()
        if (unitNormalized !in supportedDurationUnits) return null
        val expression = trimmed.dropLast(unitRaw.length).trim()
        if (expression.isBlank()) return null
        val amount = DurationExpressionParser(expression, zfpStar, diceRoll).parse() ?: return null
        if (amount <= 0) return null
        return DurationEvaluation(amount, unitRaw)
    }

    private class DurationExpressionParser(
        private val input: String,
        private val zfpStar: Int,
        private val diceRoll: (Int) -> Int
    ) {
        private var index = 0

        fun parse(): Int? {
            val value = parseSum() ?: return null
            skipWhitespace()
            return if (index == input.length) value else null
        }

        private fun parseSum(): Int? {
            var value = parseProduct() ?: return null
            while (true) {
                skipWhitespace()
                when (peek()) {
                    '+' -> {
                        index++
                        val rhs = parseProduct() ?: return null
                        value += rhs
                    }
                    '-' -> {
                        index++
                        val rhs = parseProduct() ?: return null
                        value -= rhs
                    }
                    else -> return value
                }
            }
        }

        private fun parseProduct(): Int? {
            var value = parseFactor() ?: return null
            while (true) {
                skipWhitespace()
                if (peek() == '*') {
                    index++
                    val rhs = parseFactor() ?: return null
                    value *= rhs
                } else {
                    return value
                }
            }
        }

        private fun parseFactor(): Int? {
            skipWhitespace()
            if (match('+')) {
                return parseFactor()
            }
            if (match('-')) {
                val value = parseFactor() ?: return null
                return -value
            }
            if (matchKeyword("ZFP*")) {
                return zfpStar
            }

            val numberStr = readDigits()
            if (!numberStr.isNullOrEmpty()) {
                if (isDiceStart()) {
                    val count = numberStr.toIntOrNull() ?: return null
                    return rollDiceTerm(count)
                }
                return numberStr.toIntOrNull()
            }

            if (isDiceStart()) {
                return rollDiceTerm(1)
            }

            return null
        }

        private fun rollDiceTerm(count: Int): Int? {
            if (count <= 0) return null
            if (!consumeDiceMarker()) return null
            val sidesStr = readDigits() ?: return null
            val sides = sidesStr.toIntOrNull()?.takeIf { it > 0 } ?: return null
            return rollDice("${count}W$sides", diceRoll)
        }

        private fun skipWhitespace() {
            while (index < input.length && input[index].isWhitespace()) {
                index++
            }
        }

        private fun readDigits(): String? {
            val start = index
            while (index < input.length && input[index].isDigit()) {
                index++
            }
            return if (index > start) input.substring(start, index) else null
        }

        private fun isDiceStart(): Boolean {
            val c = peek()
            return c.equals('w', ignoreCase = true)
        }

        private fun consumeDiceMarker(): Boolean {
            return if (isDiceStart()) {
                index++
                true
            } else {
                false
            }
        }

        private fun match(expected: Char): Boolean {
            if (peek() == expected) {
                index++
                return true
            }
            return false
        }

        private fun matchKeyword(keyword: String): Boolean {
            if (index + keyword.length > input.length) return false
            if (input.regionMatches(index, keyword, 0, keyword.length, ignoreCase = true)) {
                index += keyword.length
                return true
            }
            return false
        }

        private fun peek(): Char {
            return if (index < input.length) input[index] else '\u0000'
        }
    }
    
    /**
     * Zählt Einsen in einer Würfelliste
     */
    fun countOnes(rolls: List<Int>): Int = rolls.count { it == 1 }
    
    /**
     * Zählt Zwanziger in einer Würfelliste
     */
    fun countTwenties(rolls: List<Int>): Int = rolls.count { it == 20 }
    
    /**
     * Führt eine Talentprobe durch
     * 
     * @param talent Das Talent, für das die Probe durchgeführt wird
     * @param character Der Charakter, der die Probe durchführt
     * @param talentwert Der Talentwert (0-18+)
     * @param difficulty Erschwernis (positiv) oder Erleichterung (negativ)
     * @param astralEnergyCost AE-Kosten für Magisches Meisterhandwerk (0 = keine AE ausgeben)
     * @param diceRoll Lambda für Würfelwürfe (Standard: W20)
     * @return ProbeResult mit allen Details
     */
    fun performTalentProbe(
        talent: Talent,
        character: Character,
        talentwert: Int,
        difficulty: Int = 0,
        astralEnergyCost: Int = 0,
        diceRoll: () -> Int = { rollD20() }
    ): ProbeResult {
        val (attr1, attr2, attr3) = getAttributesForTalent(talent, character)
        return performThreeAttributeProbe(
            fertigkeitswert = talentwert,
            difficulty = difficulty,
            attribute1 = attr1,
            attribute2 = attr2,
            attribute3 = attr3,
            astralEnergyCost = astralEnergyCost,
            diceRoll = diceRoll,
            qualityPointName = "TaP*"
        )
    }
    
    /**
     * Führt eine Zauberprobe durch
     * 
     * @param spell Der Zauber, für den die Probe durchgeführt wird
     * @param character Der Charakter, der die Probe durchführt
     * @param zauberfertigkeit Die Zauberfertigkeit (ZfW)
     * @param difficulty Erschwernis (positiv) oder Erleichterung (negativ)
     * @param diceRoll Lambda für Würfelwürfe (Standard: W20)
     * @return ProbeResult mit allen Details
     */
    fun performSpellProbe(
        spell: Spell,
        character: Character,
        zauberfertigkeit: Int,
        difficulty: Int = 0,
        diceRoll: () -> Int = { rollD20() }
    ): ProbeResult {
        val (attr1, attr2, attr3) = getAttributesForSpell(spell, character)
        return performThreeAttributeProbe(
            fertigkeitswert = zauberfertigkeit,
            difficulty = difficulty,
            attribute1 = attr1,
            attribute2 = attr2,
            attribute3 = attr3,
            diceRoll = diceRoll,
            qualityPointName = "ZfP*"
        )
    }
    
    /**
     * Führt eine System-Zauberprobe durch (z.B. ODEM, ANALYS)
     * 
     * @param systemSpell Der System-Zauber
     * @param character Der Charakter, der die Probe durchführt
     * @param zauberfertigkeit Die Zauberfertigkeit (ZfW)
     * @param difficulty Erschwernis (positiv) oder Erleichterung (negativ)
     * @param diceRoll Lambda für Würfelwürfe (Standard: W20)
     * @return ProbeResult mit allen Details
     */
    fun performSystemSpellProbe(
        systemSpell: SystemSpell,
        character: Character,
        zauberfertigkeit: Int,
        difficulty: Int = 0,
        diceRoll: () -> Int = { rollD20() }
    ): ProbeResult {
        val (attr1, attr2, attr3) = getAttributesForSystemSpell(systemSpell, character)
        return performThreeAttributeProbe(
            fertigkeitswert = zauberfertigkeit,
            difficulty = difficulty,
            attribute1 = attr1,
            attribute2 = attr2,
            attribute3 = attr3,
            diceRoll = diceRoll,
            qualityPointName = "ZfP*"
        )
    }
    
    /**
     * Holt die Eigenschaftswerte für einen Talent aus dem Charakter
     */
    private fun getAttributesForTalent(talent: Talent, character: Character): Triple<Int, Int, Int> {
        return Triple(
            getAttributeValue(talent.attribute1, character),
            getAttributeValue(talent.attribute2, character),
            getAttributeValue(talent.attribute3, character)
        )
    }
    
    /**
     * Führt eine Astrale Meditation durch
     * 
     * Die Astrale Meditation erlaubt es einem Charakter, LE in AE umzuwandeln.
     * 
     * Regelwerk:
     * - Probe auf IN/CH/KO
     * - Erleichterung um RkW/2 (aufgerundet)
     * - Zusätzlich -2 Erschwernis bei SF Konzentrationsstärke
     * - Bei Erfolg: 1:1 LE → AE Umwandlung
     * - Zusätzliche Kosten: 1 AsP + 1W3-1 LE
     * 
     * @param character Charakter, der meditiert
     * @param leToConvert Anzahl LE, die umgewandelt werden sollen
     * @param diceRoll Lambda für W20-Würfe (überschreibbar für Tests)
     * @return Paar aus ProbeResult und zusätzlichen LE-Kosten (1W3-1)
     */
    fun performAstralMeditation(
        character: Character,
        leToConvert: Int,
        diceRoll: () -> Int = { rollD20() }
    ): Pair<ProbeResult, Int> {
        // Berechne Erleichterung
        // RkW/2 (aufgerundet) + optional 2 für SF Konzentrationsstärke
        val ritualBonus = (character.ritualKnowledgeValue + 1) / 2
        val sfBonus = if (character.hasKonzentrationsstärke) 2 else 0
        val totalFacilitation = ritualBonus + sfBonus
        
        // Probe auf IN/CH/KO mit Erleichterung (negative Erschwernis = Erleichterung)
        val probeResult = performThreeAttributeProbe(
            fertigkeitswert = 0, // Keine Fertigkeitspunkte, nur Eigenschaftsprobe
            difficulty = -totalFacilitation, // Negativ = Erleichterung
            attribute1 = character.inValue,
            attribute2 = character.ch,
            attribute3 = character.ko,
            diceRoll = diceRoll,
            qualityPointName = "FP*"
        )
        
        // Würfle zusätzliche LE-Kosten: 1W3-1 = W3 - 1
        val additionalLeCost = (Random.nextInt(1, 4) - 1).coerceAtLeast(0) // W3-1, min 0
        
        return Pair(probeResult, additionalLeCost)
    }
    
    /**
     * Holt die Eigenschaftswerte für einen Zauber aus dem Charakter
     */
    private fun getAttributesForSpell(spell: Spell, character: Character): Triple<Int, Int, Int> {
        return Triple(
            getAttributeValue(spell.attribute1, character),
            getAttributeValue(spell.attribute2, character),
            getAttributeValue(spell.attribute3, character)
        )
    }
    
    /**
     * Holt die Eigenschaftswerte für einen System-Zauber aus dem Charakter
     */
    private fun getAttributesForSystemSpell(systemSpell: SystemSpell, character: Character): Triple<Int, Int, Int> {
        return Triple(
            getAttributeValue(systemSpell.attribute1, character),
            getAttributeValue(systemSpell.attribute2, character),
            getAttributeValue(systemSpell.attribute3, character)
        )
    }
    
    /**
     * Holt einen einzelnen Eigenschaftswert aus dem Charakter basierend auf dem Namen
     */
    private fun getAttributeValue(attributeName: String, character: Character): Int {
        return when (attributeName.uppercase()) {
            "MU" -> character.mu
            "KL" -> character.kl
            "IN" -> character.inValue
            "CH" -> character.ch
            "FF" -> character.ff
            "GE" -> character.ge
            "KO" -> character.ko
            "KK" -> character.kk
            else -> throw IllegalArgumentException("Unbekannte Eigenschaft: $attributeName")
        }
    }
    
    /**
     * Berechnet die tatsaechlichen AsP-Kosten eines Zaubers unter Beruecksichtigung aller Modifikatoren
     * 
     * @param baseCost Basis-AsP-Kosten (fester Wert, 0 wenn nur Formel)
     * @param costFormula AsP-Kosten-Formel (z.B. 16-ZfP/2)
     * @param zfpStar ZfP des Zaubers (fuer Formelauswertung)
     * @param success War die Probe erfolgreich?
     * @param useHexenRepresentation Wurde in hexischer Repraesentation gesprochen?
     * @param hasKraftkontrolle Hat der Charakter Kraftkontrolle?
     * @param hasKraftfokus Hat der Charakter einen Kraftfokus? (nicht bei Zauberspeicher)
     * @param applicKraftfokus Kraftfokus bei Applicatus anwendbar? (false fuer Zauberspeicher)
     * @return Tatsaechliche AsP-Kosten
     */
    fun calculateAspCost(
        costFormula: String,
        zfpStar: Int,
        success: Boolean,
        useHexenRepresentation: Boolean,
        hasKraftkontrolle: Boolean,
        hasKraftfokus: Boolean,
        applicKraftfokus: Boolean = true
    ): Int {
        // 1. Berechne Basis-Kosten aus Formel oder fester Zahl
        var cost = if (costFormula.isNotBlank()) {
            evaluateAspCostFormula(costFormula, zfpStar) ?: 0
        } else {
            0
        }
        
        // 2. Bei Misserfolg: Halbierung (hexisch: Drittelung)
        if (!success) {
            cost = if (useHexenRepresentation) {
                // Drittelung, kaufmännisch gerundet
                (cost + 1) / 3
            } else {
                // Halbierung, aufgerundet
                (cost + 1) / 2
            }
        }
        
        // 3. Kraftkontrolle: -1 AsP (immer anwendbar)
        if (hasKraftkontrolle) {
            cost = maxOf(1, cost - 1)
        }
        
        // 4. Kraftfokus: -1 AsP (nur wenn applicKraftfokus = true)
        if (hasKraftfokus && applicKraftfokus) {
            cost = maxOf(1, cost - 1)
        }
        
        // 5. Minimum 1 AsP für jeden Zauber
        return maxOf(1, cost)
    }
    
    /**
     * Wertet eine AsP-Kosten-Formel aus (z.B. 16-ZfP/2 fuer Armatrutz)
     * 
     * Unterstuetzt:
     * - Zahlen: 8
     * - ZfP: Variable fuer Qualitaetspunkte
     * - Addition: ZfP+2
     * - Subtraktion: 16-ZfP/2
     * - Multiplikation: ZfP*2
     * - Division: ZfP/2
     * 
     * @param formula Die Formel als String
     * @param zfpStar Der Wert von ZfP
     * @return Berechnete AsP-Kosten oder null bei Fehler
     */
    fun evaluateAspCostFormula(formula: String, zfpStar: Int): Int? {
        if (formula.isBlank()) return null
        
        try {
            // Ersetze ZfP durch tatsaechlichen Wert
            // ZfP* wird als "ZfP*" erkannt (nicht als "ZfP" + "*")
            val substituted = formula.replace("ZfP", zfpStar.toString(), ignoreCase = true)
            
            // Parse und evaluiere die Formel
            return AspCostExpressionParser(substituted).parseExpression()
        } catch (e: Exception) {
            return null
        }
    }
    
    /**
     * Parser fuer AsP-Kosten-Formeln (aehnlich wie DurationExpressionParser)
     * Unterstuetzt +, -, *, / mit korrekter Operator-Praezedenz
     */
    private class AspCostExpressionParser(private val expression: String) {
        private var index = 0
        
        fun parseExpression(): Int {
            return parseAddSubtract()
        }
        
        private fun parseAddSubtract(): Int {
            var result = parseMultDivide()
            
            while (index < expression.length) {
                skipWhitespace()
                if (index >= expression.length) break
                
                when (expression[index]) {
                    '+' -> {
                        index++
                        result += parseMultDivide()
                    }
                    '-' -> {
                        index++
                        result -= parseMultDivide()
                    }
                    else -> break
                }
            }
            
            return result
        }
        
        private fun parseMultDivide(): Int {
            var result = parsePrimary()
            
            while (index < expression.length) {
                skipWhitespace()
                if (index >= expression.length) break
                
                when (expression[index]) {
                    '*' -> {
                        index++
                        result *= parsePrimary()
                    }
                    '/' -> {
                        index++
                        val divisor = parsePrimary()
                        if (divisor != 0) {
                            // Division durch 2 wird aufgerundet, durch 3 kaufmaennisch gerundet
                            if (divisor == 2) {
                                result = (result + 1) / 2
                            } else if (divisor == 3) {
                                result = (result + 1) / 3
                            } else {
                                result /= divisor
                            }
                        }
                    }
                    else -> break
                }
            }
            
            return result
        }
        
        private fun parsePrimary(): Int {
            skipWhitespace()
            
            if (index >= expression.length) {
                throw IllegalArgumentException("Unerwartetes Ende der Formel")
            }
            
            // Negative Zahlen
            if (expression[index] == '-') {
                index++
                return -parsePrimary()
            }
            
            // Klammern
            if (expression[index] == '(') {
                index++
                val result = parseExpression()
                skipWhitespace()
                if (index < expression.length && expression[index] == ')') {
                    index++
                }
                return result
            }
            
            // Zahlen
            val start = index
            while (index < expression.length && expression[index].isDigit()) {
                index++
            }
            
            if (start == index) {
                throw IllegalArgumentException("Ungültiges Zeichen in Formel: ${expression[index]}")
            }
            
            return expression.substring(start, index).toInt()
        }
        
        private fun skipWhitespace() {
            while (index < expression.length && expression[index].isWhitespace()) {
                index++
            }
        }
    }
    
    /**
     * Berechnet die Applicatus-AsP-Kosten mit optionaler Kostenersparnis
     * 
     * @param duration Applicatus-Dauer
     * @param savingPercent Kostenersparnis in Prozent (0-50)
     * @param hasKraftkontrolle Hat der Charakter Kraftkontrolle?
     * @param hasKraftfokus Hat der Charakter einen Kraftfokus?
     * @param diceRoll Lambda fuer Wuerfelwuerfe (Standard: W6)
     * @return Tuple (tatsaechliche Kosten, Basis-Kosten, Wuerfelergebnis-Text)
     */
    fun calculateApplicatusAspCost(
        duration: de.applicatus.app.data.model.spell.ApplicatusDuration,
        savingPercent: Int,
        hasKraftkontrolle: Boolean,
        hasKraftfokus: Boolean,
        diceRoll: (diceSize: Int) -> Int = { diceSize -> Random.nextInt(1, diceSize + 1) }
    ): Triple<Int, Int, String> {
        // Basis-Kosten würfeln
        val (baseCost, rollText) = when (duration) {
            de.applicatus.app.data.model.spell.ApplicatusDuration.DAY -> {
                val roll1 = diceRoll(6)
                val roll2 = diceRoll(6)
                Pair(roll1 + roll2, "2W6: $roll1 + $roll2")
            }
            de.applicatus.app.data.model.spell.ApplicatusDuration.MOON -> {
                val roll1 = diceRoll(6)
                val roll2 = diceRoll(6)
                val roll3 = diceRoll(6)
                Pair(roll1 + roll2 + roll3, "3W6: $roll1 + $roll2 + $roll3")
            }
            de.applicatus.app.data.model.spell.ApplicatusDuration.QUARTER -> {
                val roll1 = diceRoll(6)
                val roll2 = diceRoll(6)
                val roll3 = diceRoll(6)
                Pair(roll1 + roll2 + roll3 + 2, "3W6+2: $roll1 + $roll2 + $roll3 + 2")
            }
            de.applicatus.app.data.model.spell.ApplicatusDuration.WINTER_SOLSTICE -> {
                val roll1 = diceRoll(6)
                val roll2 = diceRoll(6)
                val roll3 = diceRoll(6)
                val roll4 = diceRoll(6)
                Pair(roll1 + roll2 + roll3 + roll4, "4W6: $roll1 + $roll2 + $roll3 + $roll4")
            }
        }
        
        // Kostenersparnis anwenden (kaufmaennisch gerundet)
        val validSavingPercent = savingPercent.coerceIn(0, 50)
        val costAfterSaving = if (validSavingPercent > 0) {
            val savingAmount = (baseCost * validSavingPercent + 50) / 100
            maxOf(0, baseCost - savingAmount)
        } else {
            baseCost
        }
        
        // Kraftkontrolle und Kraftfokus anwenden
        var finalCost = costAfterSaving
        if (hasKraftkontrolle) {
            finalCost = maxOf(0, finalCost - 1)
        }
        if (hasKraftfokus) {
            finalCost = maxOf(0, finalCost - 1)
        }
        
        return Triple(finalCost, baseCost, rollText)
    }
}

