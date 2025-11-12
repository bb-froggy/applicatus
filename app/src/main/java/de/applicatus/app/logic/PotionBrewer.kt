package de.applicatus.app.logic

import de.applicatus.app.data.model.character.Character
import de.applicatus.app.data.model.potion.Laboratory
import de.applicatus.app.data.model.potion.Potion
import de.applicatus.app.data.model.potion.PotionQuality
import de.applicatus.app.data.model.potion.Recipe
import de.applicatus.app.data.model.potion.Substitution
import de.applicatus.app.data.model.potion.SubstitutionType
import de.applicatus.app.data.model.talent.Talent
import kotlin.math.ceil
import kotlin.math.pow

/**
 * Klasse für die Berechnung von Trank-Brauproben nach DSA 4.1
 */
object PotionBrewer {
    
    /**
     * Ergebnis einer Brauprobe
     */
    data class BrewingResult(
        val probeResult: ProbeResult,
        val qualityPoints: Int,
        val quality: PotionQuality,
        val laborModifier: Int,
        val brewingDifficultyModifier: Int,
        val voluntaryHandicapModifier: Int,
        val substitutionModifier: Int,
        val totalModifier: Int,
        val diceRoll1: Int,
        val diceRoll2: Int,
        val qualityDice: Int
    )
    
    /**
     * Berechnet den maximal möglichen freiwilligen Handicap.
     * Mindestens 2, maximal anderthalbfache Brauschwierigkeit (aufgerundet).
     */
    fun calculateMaxVoluntaryHandicap(recipe: Recipe): Int {
        return ceil(recipe.brewingDifficulty * 1.5).toInt()
    }
    
    /**
     * Berechnet die AsP-Kosten für n Qualitätspunkte: 2^(n-1)
     */
    fun calculateAspCostForQualityPoints(qualityPoints: Int): Int {
        if (qualityPoints <= 0) return 0
        return 2.0.pow(qualityPoints - 1).toInt()
    }
    
    /**
     * Berechnet die maximale Anzahl Qualitätspunkte, die mit gegebenen AsP erreicht werden können
     */
    fun calculateMaxQualityPointsFromAsp(availableAsp: Int): Int {
        if (availableAsp <= 0) return 0
        var qp = 0
        while (calculateAspCostForQualityPoints(qp + 1) <= availableAsp) {
            qp++
        }
        return qp
    }
    
    /**
     * Prüft, ob das Brauen mit dem verfügbaren Labor möglich ist
     */
    fun canBrew(recipe: Recipe, availableLaboratory: Laboratory): Boolean {
        val requiredLab = recipe.lab ?: return true  // Kein Labor erforderlich
        return requiredLab.canBrewWith(availableLaboratory)
    }
    
    /**
     * Berechnet den Gesamtmodifikator für die Brauprobe
     */
    fun calculateTotalModifier(
        recipe: Recipe,
        availableLaboratory: Laboratory,
        voluntaryHandicap: Int,
        substitutions: List<Substitution>
    ): Int {
        val laborModifier = recipe.lab?.getBrewingModifier(availableLaboratory) ?: 0
        val brewingDifficulty = recipe.brewingDifficulty
        val substitutionModifier = substitutions.sumOf { it.type.modifier }
        
        return laborModifier + brewingDifficulty + voluntaryHandicap + substitutionModifier
    }
    
    /**
     * Führt eine Brauprobe durch
     * 
     * @param character Der Charakter, der braut
     * @param recipe Das Rezept
     * @param talent Das verwendete Talent (ALCHEMY oder COOKING_POTIONS)
     * @param availableLaboratory Das verfügbare Labor
     * @param voluntaryHandicap Freiwilliger Handicap (0 oder min. 2)
     * @param substitutions Liste der Substitutionen
     * @param magicalMasteryAsp AsP für Magisches Meisterhandwerk (TaW-Erhöhung: +2 TaW pro AsP, max TaW/2 AsP)
     * @param astralCharging Anzahl der Qualitätspunkte durch astrale Aufladung (2^(n-1) AsP)
     * @return Ergebnis der Brauprobe
     */
    fun brewPotion(
        character: Character,
        recipe: Recipe,
        talent: Talent,
        availableLaboratory: Laboratory,
        voluntaryHandicap: Int = 0,
        substitutions: List<Substitution> = emptyList(),
        magicalMasteryAsp: Int = 0,
        astralCharging: Int = 0
    ): BrewingResult {
        require(talent == Talent.ALCHEMY || talent == Talent.COOKING_POTIONS) {
            "Nur Alchimie oder Kochen (Tränke) sind gültige Talente zum Brauen"
        }
        require(voluntaryHandicap == 0 || voluntaryHandicap >= 2) {
            "Freiwilliger Handicap muss 0 oder mindestens 2 sein"
        }
        require(voluntaryHandicap <= calculateMaxVoluntaryHandicap(recipe)) {
            "Freiwilliger Handicap darf maximal ${calculateMaxVoluntaryHandicap(recipe)} sein"
        }
        require(canBrew(recipe, availableLaboratory)) {
            "Brauen mit diesem Labor ist nicht möglich"
        }
        
        // Kochen (Tränke) darf kein Alchimistenlabor verwenden
        if (talent == Talent.COOKING_POTIONS && availableLaboratory == Laboratory.ALCHEMIST_LABORATORY) {
            throw IllegalArgumentException("Kochen (Tränke) kann nicht mit einem Alchimistenlabor verwendet werden")
        }
        
        // Kochen (Tränke) kann keine Rezepte brauen, die ein Alchimistenlabor erfordern
        if (talent == Talent.COOKING_POTIONS && recipe.lab == Laboratory.ALCHEMIST_LABORATORY) {
            throw IllegalArgumentException("Kochen (Tränke) kann keine Rezepte brauen, die ein Alchimistenlabor erfordern")
        }
        
        // Talentwert ermitteln
        val skillValue = when (talent) {
            Talent.ALCHEMY -> character.alchemySkill
            Talent.COOKING_POTIONS -> character.cookingPotionsSkill
            else -> 0
        }
        
        // Magisches Meisterhandwerk prüfen
        val isMagicalMastery = when (talent) {
            Talent.ALCHEMY -> character.alchemyIsMagicalMastery
            Talent.COOKING_POTIONS -> character.cookingPotionsIsMagicalMastery
            else -> false
        }
        
        // Magisches Meisterhandwerk: +2 TaW pro AsP, max bis 2x TaW (also max TaW/2 AsP)
        val maxMagicalMasteryAsp = skillValue / 2
        if (magicalMasteryAsp > 0) {
            require(isMagicalMastery) {
                "Magisches Meisterhandwerk ist für dieses Talent nicht verfügbar"
            }
            require(character.hasAe) {
                "Charakter hat keine Astralenergie"
            }
            require(magicalMasteryAsp <= maxMagicalMasteryAsp) {
                "Maximal $maxMagicalMasteryAsp AsP für Magisches Meisterhandwerk bei TaW $skillValue"
            }
            require(character.currentAe >= magicalMasteryAsp) {
                "Nicht genug Astralenergie (benötigt $magicalMasteryAsp AsP, verfügbar ${character.currentAe} AsP)"
            }
        }
        
        // AsP-Kosten für astrale Aufladung berechnen (2^(n-1))
        val astralChargingCost = calculateAspCostForQualityPoints(astralCharging)
        val totalAspCost = magicalMasteryAsp + astralChargingCost
        
        if (astralCharging > 0) {
            require(character.hasAe) {
                "Charakter hat keine Astralenergie"
            }
            require(character.currentAe >= totalAspCost) {
                "Nicht genug Astralenergie für ${astralCharging} QP (benötigt gesamt ${totalAspCost} AsP, verfügbar ${character.currentAe} AsP)"
            }
        }
        
        // Modifikatoren berechnen
        val laborModifier = recipe.lab?.getBrewingModifier(availableLaboratory) ?: 0
        val brewingDifficultyModifier = recipe.brewingDifficulty
        val substitutionModifier = substitutions.sumOf { it.type.modifier }
        val totalModifier = laborModifier + brewingDifficultyModifier + voluntaryHandicap + substitutionModifier
        
        // Probe durchführen (mit Magischem Meisterhandwerk falls verwendet)
        val probeResult = ProbeChecker.performTalentProbe(
            talent = talent,
            character = character,
            talentwert = skillValue,
            difficulty = totalModifier,
            astralEnergyCost = magicalMasteryAsp
        )
        
        // Qualitätspunkte berechnen
        val qualityPoints = if (probeResult.success) {
            val tapQualityPoints = probeResult.qualityPoints
            val voluntaryQualityPoints = voluntaryHandicap * 2
            val astralQualityPoints = astralCharging
            
            // 2W6 würfeln für zusätzliche Qualitätspunkte
            val dice1 = ProbeChecker.rollD6()
            val dice2 = ProbeChecker.rollD6()
            val diceQualityPoints = dice1 + dice2
            
            val total = tapQualityPoints + voluntaryQualityPoints + astralQualityPoints + diceQualityPoints
            
            BrewingResult(
                probeResult = probeResult,
                qualityPoints = total,
                quality = calculateQuality(total),
                laborModifier = laborModifier,
                brewingDifficultyModifier = brewingDifficultyModifier,
                voluntaryHandicapModifier = voluntaryHandicap,
                substitutionModifier = substitutionModifier,
                totalModifier = totalModifier,
                diceRoll1 = dice1,
                diceRoll2 = dice2,
                qualityDice = diceQualityPoints
            )
        } else {
            // Probe misslungen - Qualität M
            BrewingResult(
                probeResult = probeResult,
                qualityPoints = 0,
                quality = PotionQuality.M,
                laborModifier = laborModifier,
                brewingDifficultyModifier = brewingDifficultyModifier,
                voluntaryHandicapModifier = voluntaryHandicap,
                substitutionModifier = substitutionModifier,
                totalModifier = totalModifier,
                diceRoll1 = 0,
                diceRoll2 = 0,
                qualityDice = 0
            )
        }
        
        return qualityPoints
    }
    
    /**
     * Berechnet die Qualitätsstufe basierend auf den Qualitätspunkten
     */
    private fun calculateQuality(qualityPoints: Int): PotionQuality {
        return when {
            qualityPoints <= 6 -> PotionQuality.A
            qualityPoints <= 12 -> PotionQuality.B
            qualityPoints <= 18 -> PotionQuality.C
            qualityPoints <= 24 -> PotionQuality.D
            qualityPoints <= 30 -> PotionQuality.E
            else -> PotionQuality.F
        }
    }
    
    /**
     * Formatiert das Brau-Ergebnis als String
     */
    fun formatBrewingResult(result: BrewingResult, isGameMaster: Boolean): String {
        val sb = StringBuilder()
        
        sb.append("Brauprobe: ${if (result.probeResult.success) "Erfolg" else "Misserfolg"}\n")
        sb.append("Würfe: ${result.probeResult.rolls.joinToString("/")}\n")
        
        if (result.probeResult.success) {
            sb.append("TaP*: ${result.probeResult.qualityPoints}\n")
            sb.append("Freiwilliger Handicap: ${result.voluntaryHandicapModifier} → +${result.voluntaryHandicapModifier * 2} QP\n")
            sb.append("2W6: ${result.diceRoll1}+${result.diceRoll2} = ${result.qualityDice}\n")
            sb.append("Gesamt: ${result.qualityPoints} Qualitätspunkte\n")
            
            if (isGameMaster) {
                sb.append("Qualität: ${result.quality.name}\n")
            } else {
                sb.append("(Qualität unbekannt - nur Spielleiter sichtbar)\n")
            }
        } else {
            sb.append("Probe misslungen\n")
            if (isGameMaster) {
                sb.append("Qualität: M (Misslungen)\n")
            }
        }
        
        // Modifikatoren-Details
        sb.append("\nModifikatoren:\n")
        if (result.laborModifier != 0) {
            sb.append("  Labor: ${if (result.laborModifier > 0) "+" else ""}${result.laborModifier}\n")
        }
        if (result.brewingDifficultyModifier != 0) {
            sb.append("  Brauschwierigkeit: +${result.brewingDifficultyModifier}\n")
        }
        if (result.voluntaryHandicapModifier != 0) {
            sb.append("  Freiwillig: +${result.voluntaryHandicapModifier}\n")
        }
        if (result.substitutionModifier != 0) {
            sb.append("  Substitutionen: ${if (result.substitutionModifier > 0) "+" else ""}${result.substitutionModifier}\n")
        }
        sb.append("  Gesamt: ${if (result.totalModifier > 0) "+" else ""}${result.totalModifier}")
        
        return sb.toString()
    }
    
    data class DilutionResult(
        val probeResult: ProbeResult,
        val success: Boolean,
        val newQuality: PotionQuality,
        val numberOfPotions: Int,
        val totalModifier: Int
    )
    
    fun dilutePotion(
        character: Character,
        potion: Potion,
        recipe: Recipe,
        talent: Talent,
        dilutionSteps: Int,
        facilitationFromAnalysis: Int = 0,
        magicalMasteryAsp: Int = 0
    ): DilutionResult {
        require(talent == Talent.ALCHEMY || talent == Talent.COOKING_POTIONS) {
            "Nur Alchimie oder Kochen (Tränke) sind gültige Talente zum Verdünnen"
        }
        require(dilutionSteps in 1..10) {
            "Verdünnung muss zwischen 1 und 10 Stufen liegen"
        }
        
        // Bei M bleibt es immer M, egal was passiert
        if (potion.actualQuality == PotionQuality.M) {
            // Trotzdem eine Probe durchführen für die Show
            val probeResult = ProbeChecker.performTalentProbe(
                talent = talent,
                character = character,
                talentwert = when (talent) {
                    Talent.ALCHEMY -> character.alchemySkill
                    Talent.COOKING_POTIONS -> character.cookingPotionsSkill
                    else -> 0
                },
                difficulty = recipe.brewingDifficulty - facilitationFromAnalysis,
                astralEnergyCost = magicalMasteryAsp
            )
            
            return DilutionResult(
                probeResult = probeResult,
                success = false,
                newQuality = PotionQuality.M,
                numberOfPotions = dilutionSteps + 1,
                totalModifier = recipe.brewingDifficulty - facilitationFromAnalysis
            )
        }
        
        // Berechne die neue Qualität (kann über A hinaus gehen → X)
        val currentQualityIndex = potion.actualQuality.ordinal
        val newQualityIndex = currentQualityIndex - dilutionSteps
        
        // Talentwert ermitteln
        val skillValue = when (talent) {
            Talent.ALCHEMY -> character.alchemySkill
            Talent.COOKING_POTIONS -> character.cookingPotionsSkill
            else -> 0
        }
        
        // Magisches Meisterhandwerk prüfen
        val isMagicalMastery = when (talent) {
            Talent.ALCHEMY -> character.alchemyIsMagicalMastery
            Talent.COOKING_POTIONS -> character.cookingPotionsIsMagicalMastery
            else -> false
        }
        
        // Magisches Meisterhandwerk: +2 TaW pro AsP, max bis 2x TaW (also max TaW AsP)
        val maxMagicalMasteryAsp = skillValue
        if (magicalMasteryAsp > 0) {
            require(isMagicalMastery) {
                "Magisches Meisterhandwerk ist für dieses Talent nicht verfügbar"
            }
            require(character.hasAe) {
                "Charakter hat keine Astralenergie"
            }
            require(magicalMasteryAsp <= maxMagicalMasteryAsp) {
                "Maximal $maxMagicalMasteryAsp AsP für Magisches Meisterhandwerk bei TaW $skillValue"
            }
            require(character.currentAe >= magicalMasteryAsp) {
                "Nicht genug Astralenergie (benötigt $magicalMasteryAsp AsP, verfügbar ${character.currentAe} AsP)"
            }
        }
        
        // Gesamtmodifikator: Brauschwierigkeit - Erleichterung aus Analyse
        val totalModifier = recipe.brewingDifficulty - facilitationFromAnalysis
        
        // Probe durchführen (mit Magischem Meisterhandwerk falls verwendet)
        val probeResult = ProbeChecker.performTalentProbe(
            talent = talent,
            character = character,
            talentwert = skillValue,
            difficulty = totalModifier,
            astralEnergyCost = magicalMasteryAsp
        )
        
        // Ergebnis berechnen
        val success = probeResult.success
        val newQuality = if (success) {
            // Wenn Index < 0, wird es X (wirkungslos)
            if (newQualityIndex < 0) {
                PotionQuality.X
            } else {
                PotionQuality.values()[newQualityIndex]
            }
        } else {
            PotionQuality.M
        }
        val numberOfPotions = dilutionSteps + 1
        
        return DilutionResult(
            probeResult = probeResult,
            success = success,
            newQuality = newQuality,
            numberOfPotions = numberOfPotions,
            totalModifier = totalModifier
        )
    }
    
    fun formatDilutionResult(result: DilutionResult, isGameMaster: Boolean): String {
        val sb = StringBuilder()
        
        if (isGameMaster) {
            // Spielleiter sieht alle Details
            sb.append("Verdünnungsprobe: ${if (result.success) "Erfolg" else "Misserfolg"}\n")
            sb.append("Würfe: ${result.probeResult.rolls.joinToString("/")}\n")
            
            if (result.success) {
                sb.append("TaP*: ${result.probeResult.qualityPoints}\n")
                sb.append("Anzahl Tränke: ${result.numberOfPotions}\n")
                sb.append("Neue Qualität: ${result.newQuality.name}\n")
            } else {
                sb.append("Probe misslungen\n")
                sb.append("Anzahl Tränke: ${result.numberOfPotions}\n")
                sb.append("Qualität aller Tränke: M (Misslungen)\n")
            }
            
            sb.append("\nModifikator: ${if (result.totalModifier > 0) "+" else ""}${result.totalModifier}")
        } else {
            // Spieler sieht nur minimale Informationen
            sb.append("Verdünnung abgeschlossen.\n\n")
            sb.append("Anzahl Tränke: ${result.numberOfPotions}")
        }
        
        return sb.toString()
    }
}
