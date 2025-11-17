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
     * Holt die Eigenschaftswerte für ein Talent aus dem Charakter
     */
    private fun getAttributesForTalent(talent: Talent, character: Character): Triple<Int, Int, Int> {
        return Triple(
            getAttributeValue(talent.attribute1, character),
            getAttributeValue(talent.attribute2, character),
            getAttributeValue(talent.attribute3, character)
        )
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
}
