package de.applicatus.app.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.applicatus.app.data.export.CharacterExportDto
import de.applicatus.app.data.export.CharacterExportManager
import de.applicatus.app.data.model.character.Character
import de.applicatus.app.data.model.spell.Spell
import de.applicatus.app.data.model.spell.SpellSlot
import de.applicatus.app.data.model.spell.SpellSlotWithSpell
import de.applicatus.app.data.model.spell.SlotType
import de.applicatus.app.data.repository.ApplicatusRepository
import de.applicatus.app.logic.SpellChecker
import de.applicatus.app.logic.SpellCheckResult
import de.applicatus.app.logic.DerianDateCalculator
import de.applicatus.app.logic.ProbeChecker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CharacterDetailViewModel(
    private val repository: ApplicatusRepository,
    private val characterId: Long
) : ViewModel() {
    
    private val exportManager = CharacterExportManager(repository)
    
    // Export/Import State
    var exportState by mutableStateOf<ExportState>(ExportState.Idle)
        private set
    
    sealed class ExportState {
        object Idle : ExportState()
        object Exporting : ExportState()
        data class Success(val message: String) : ExportState()
        data class Error(val message: String) : ExportState()
    }
    
    // Zauber-Auslösen-State
    var spellCastMessage by mutableStateOf<String?>(null)
        private set
    
    // Animation State für Zauber-Einspeicherung
    var showSpellAnimation by mutableStateOf(false)
        private set
    
    var animatingSlotId by mutableStateOf<Long?>(null)
        private set
    
    fun clearSpellCastMessage() {
        spellCastMessage = null
    }
    
    fun hideSpellAnimation() {
        showSpellAnimation = false
        animatingSlotId = null
    }
    
    // Bearbeitungsmodus-State
    var isEditMode by mutableStateOf(false)
        private set
    
    val character: StateFlow<Character?> = repository.getCharacterByIdFlow(characterId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    
    @OptIn(ExperimentalCoroutinesApi::class)
    val groupDate: StateFlow<String> = character
        .mapLatest { char ->
            char?.groupId?.let { groupId ->
                repository.getGroupByIdOnce(groupId)?.currentDerianDate
            } ?: "1 Praios 1040 BF"
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "1 Praios 1040 BF"
        )
    
    val spellSlots: StateFlow<List<SpellSlotWithSpell>> = 
        repository.getSlotsWithSpellsByCharacter(characterId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    
    val allSpells: StateFlow<List<Spell>> = repository.allSpells
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    fun toggleEditMode() {
        isEditMode = !isEditMode
    }
    
    fun updateCharacter(updatedCharacter: Character) {
        viewModelScope.launch {
            repository.updateCharacter(updatedCharacter)
        }
    }
    
    fun addSlot(slotType: SlotType, volumePoints: Int = 0) {
        viewModelScope.launch {
            // Prüfe Volumenpunkte-Limit bei Zauberspeicher
            if (slotType == SlotType.SPELL_STORAGE) {
                val currentVolume = spellSlots.value
                    .filter { it.slot.slotType == SlotType.SPELL_STORAGE }
                    .sumOf { it.slot.volumePoints }
                
                if (currentVolume + volumePoints > 100) {
                    // Fehler: Limit überschritten
                    return@launch
                }
            }
            
            val nextSlotNumber = (spellSlots.value.maxOfOrNull { it.slot.slotNumber } ?: -1) + 1
            
            val newSlot = SpellSlot(
                characterId = characterId,
                slotNumber = nextSlotNumber,
                slotType = slotType,
                volumePoints = volumePoints,
                spellId = null
            )
            
            repository.insertSlot(newSlot)
        }
    }
    
    fun removeSlot(slot: SpellSlot) {
        viewModelScope.launch {
            repository.deleteSlot(slot)
        }
    }
    
    fun canAddApplicatusSlot(): Boolean {
        return character.value?.hasApplicatus == true
    }
    
    fun getRemainingVolumePoints(): Int {
        val usedVolume = spellSlots.value
            .filter { it.slot.slotType == SlotType.SPELL_STORAGE }
            .sumOf { it.slot.volumePoints }
        return 100 - usedVolume
    }
    
    fun updateSlotSpell(slot: SpellSlot, spellId: Long?) {
        viewModelScope.launch {
            repository.updateSlot(
                slot.copy(
                    spellId = spellId,
                    isFilled = false,
                    zfpStar = null,
                    lastRollResult = null,
                    applicatusRollResult = null
                )
            )
        }
    }
    
    fun updateSlotZfw(slot: SpellSlot, zfw: Int) {
        viewModelScope.launch {
            repository.updateSlot(slot.copy(zfw = zfw))
        }
    }
    
    fun updateSlotModifier(slot: SpellSlot, modifier: Int) {
        viewModelScope.launch {
            repository.updateSlot(slot.copy(modifier = modifier))
        }
    }
    
    fun updateSlotVariant(slot: SpellSlot, variant: String) {
        viewModelScope.launch {
            repository.updateSlot(slot.copy(variant = variant))
        }
    }
    
    fun updateSlotDurationFormula(slot: SpellSlot, formula: String) {
        viewModelScope.launch {
            repository.updateSlot(slot.copy(longDurationFormula = formula.trim()))
        }
    }
    
    fun updateAllModifiers(delta: Int) {
        viewModelScope.launch {
            spellSlots.value.forEach { slotWithSpell ->
                val slot = slotWithSpell.slot
                repository.updateSlot(
                    slot.copy(modifier = slot.modifier + delta)
                )
            }
        }
    }
    
    fun castSpell(slot: SpellSlot, spell: Spell) {
        viewModelScope.launch {
            val char = character.value ?: return@launch
            
            // Starte die Animation
            showSpellAnimation = true
            animatingSlotId = slot.id
            
            // Hole die Eigenschaftswerte basierend auf dem Zauber
            val attr1 = getAttributeValue(char, spell.attribute1)
            val attr2 = getAttributeValue(char, spell.attribute2)
            val attr3 = getAttributeValue(char, spell.attribute3)
            
            // Prüfe, ob es ein Applicatus-Slot ist
            if (slot.slotType == SlotType.APPLICATUS && char.hasApplicatus) {
                // Führe Applicatus-Probe durch (mit Wirkungsdauer-Modifikator)
                val result = SpellChecker.performApplicatusCheck(
                    spellZfw = slot.zfw,
                    spellModifier = slot.modifier,
                    spellAttribute1 = attr1,
                    spellAttribute2 = attr2,
                    spellAttribute3 = attr3,
                    applicatusZfw = char.applicatusZfw,
                    applicatusModifier = char.applicatusModifier,
                    applicatusDurationModifier = char.applicatusDuration.difficultyModifier,
                    characterKl = char.kl,
                    characterFf = char.ff
                )
                
                // Prüfe auf Patzer (Doppel-20 oder Dreifach-20)
                // Patzer kann sowohl bei Applicatus als auch beim eigentlichen Zauber auftreten
                val applicatusPatzer = result.applicatusResult?.let { 
                    it.isDoubleTwenty || it.isTripleTwenty
                } ?: false
                
                val spellPatzer = result.spellResult.isDoubleTwenty || result.spellResult.isTripleTwenty
                
                // Wenn Applicatus patzt, wird der Slot belegt aber der Zauber wird nicht gewürfelt
                // Wenn der Zauber patzt (und Applicatus erfolgreich war), wird der Slot auch belegt
                val isPatzer = applicatusPatzer || (result.applicatusResult?.success == true && spellPatzer)
                val summaryText = buildSpellCastSummary(
                    spell = spell,
                    slot = slot,
                    spellResult = result.spellResult,
                    applicatusResult = result.applicatusResult,
                    isPatzer = isPatzer,
                    overallSuccess = result.overallSuccess
                )
                
                // Berechne Ablaufdatum bei Erfolg
                val expiryDate = if (result.overallSuccess) {
                    val groupDate = resolveGroupDate(char)
                    DerianDateCalculator.calculateSpellExpiry(
                        currentDate = groupDate,
                        slotType = SlotType.APPLICATUS,
                        applicatusDuration = char.applicatusDuration
                    )
                } else {
                    null
                }
                
                // Aktualisiere den Slot
                repository.updateSlot(
                    slot.copy(
                        // Slot wird nur bei Erfolg ODER Patzer belegt
                        // Bei normalem Fehlschlag bleibt Slot leer
                        isFilled = result.overallSuccess || isPatzer,
                        zfpStar = if (result.overallSuccess) result.spellResult.zfpStar else null,
                        lastRollResult = formatRollResult(result.spellResult),
                        applicatusRollResult = result.applicatusResult?.let { formatRollResult(it) },
                        isBotched = isPatzer, // true wenn Patzer, false sonst
                        expiryDate = expiryDate
                    )
                )
                spellCastMessage = summaryText
            } else {
                // Normale Zauberprobe (Zauberspeicher oder langwirkend)
                val result = SpellChecker.performSpellCheck(
                    zfw = slot.zfw,
                    modifier = slot.modifier,
                    attribute1 = attr1,
                    attribute2 = attr2,
                    attribute3 = attr3
                )
                
                // Prüfe auf Patzer (Doppel-20 oder Dreifach-20)
                val isPatzer = result.isDoubleTwenty || result.isTripleTwenty
                val summaryText = buildSpellCastSummary(
                    spell = spell,
                    slot = slot,
                    spellResult = result,
                    applicatusResult = null,
                    isPatzer = isPatzer,
                    overallSuccess = result.success
                )
                
                // Berechne Ablaufdatum bei Erfolg
                val expiryDate = if (result.success) {
                    val groupDate = resolveGroupDate(char)
                    when (slot.slotType) {
                        SlotType.SPELL_STORAGE -> DerianDateCalculator.calculateSpellExpiry(
                            currentDate = groupDate,
                            slotType = SlotType.SPELL_STORAGE
                        )
                        SlotType.LONG_DURATION -> calculateLongDurationExpiry(slot, result.zfpStar, groupDate)
                        else -> null
                    }
                } else {
                    null
                }
                
                // Aktualisiere den Slot
                repository.updateSlot(
                    slot.copy(
                        // Slot wird nur bei Erfolg ODER Patzer belegt
                        // Bei normalem Fehlschlag bleibt Slot leer
                        isFilled = result.success || isPatzer,
                        zfpStar = if (result.success) result.zfpStar else null,
                        lastRollResult = formatRollResult(result),
                        applicatusRollResult = null,
                        isBotched = isPatzer, // true wenn Patzer, false sonst
                        expiryDate = expiryDate
                    )
                )
                spellCastMessage = summaryText
            }
        }
    }
    
    fun clearSlot(slot: SpellSlot, spell: Spell?) {
        viewModelScope.launch {
            spellCastMessage = buildSlotReleaseSummary(slot, spell)
            
            repository.updateSlot(
                slot.copy(
                    isFilled = false,
                    zfpStar = null,
                    lastRollResult = null,
                    applicatusRollResult = null,
                    isBotched = false
                )
            )
        }
    }
    
    private fun getAttributeValue(character: Character, attributeName: String): Int {
        return when (attributeName.uppercase()) {
            "MU" -> character.mu
            "KL" -> character.kl
            "IN" -> character.inValue
            "CH" -> character.ch
            "FF" -> character.ff
            "GE" -> character.ge
            "KO" -> character.ko
            "KK" -> character.kk
            else -> 8 // Default
        }
    }
    
    private fun formatRollResult(result: SpellCheckResult): String {
        val rollsStr = result.rolls.joinToString(", ")
        val rollSuffix = if (rollsStr.isBlank()) "" else " [$rollsStr]"
        return when {
            result.isTripleOne -> "Dreifach-1!$rollSuffix ZfP*: ${result.zfpStar}"
            result.isDoubleOne -> "Doppel-1!$rollSuffix ZfP*: ${result.zfpStar}"
            result.isTripleTwenty -> "Dreifach-20!$rollSuffix Katastrophe!"
            result.isDoubleTwenty -> "Doppel-20!$rollSuffix Patzer!"
            result.success -> "Erfolg!$rollSuffix ZfP*: ${result.zfpStar}"
            else -> "Fehlgeschlagen!$rollSuffix"
        }
    }

    private fun buildSpellCastSummary(
        spell: Spell,
        slot: SpellSlot,
        spellResult: SpellCheckResult,
        applicatusResult: SpellCheckResult?,
        isPatzer: Boolean,
        overallSuccess: Boolean
    ): String {
        val slotLabel = when (slot.slotType) {
            SlotType.APPLICATUS -> "Applicatus"
            SlotType.SPELL_STORAGE -> "Zauberspeicher (${slot.volumePoints} VP)"
            SlotType.LONG_DURATION -> "Langwirkender Zauber"
        }
        val modifierText = if (slot.modifier >= 0) "+${slot.modifier}" else slot.modifier.toString()
        val statusLine = when {
            isPatzer -> "⚠️ Patzer beim Wirken von ${spell.name}!"
            overallSuccess -> "✓ ${spell.name} erfolgreich eingespeichert."
            else -> "✗ ${spell.name} fehlgeschlagen."
        }
        return buildString {
            appendLine(statusLine)
            appendLine("Slot: $slotLabel")
            appendLine(
                "Probe: ${spell.attribute1}/${spell.attribute2}/${spell.attribute3} | ZfW ${slot.zfw} | Mod $modifierText"
            )
            applicatusResult?.let {
                appendLine("Applicatusprobe: ${formatRollResult(it)}")
            }
            appendLine("Zauberprobe: ${formatRollResult(spellResult)}")
        }.trim()
    }

    private fun buildSlotReleaseSummary(slot: SpellSlot, spell: Spell?): String {
        val spellName = spell?.name ?: "Zauber"
        val statusLine = when {
            slot.isBotched -> "⚠️ Patzer! $spellName verpufft wirkungslos."
            slot.isFilled -> "✓ $spellName erfolgreich ausgelöst."
            else -> "$spellName wurde geleert."
        }
        return buildString {
            appendLine(statusLine)
            slot.applicatusRollResult?.let {
                appendLine("Applicatusprobe: $it")
            }
            slot.lastRollResult?.let {
                appendLine("Zauberprobe: $it")
            }
            if (!slot.isBotched) {
                slot.zfpStar?.let {
                    appendLine("Gespeicherte ZfP*: $it")
                }
            }
        }.trim()
    }
    
    // Export/Import Funktionen
    fun exportCharacterToFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            exportState = ExportState.Exporting
            val result = exportManager.saveCharacterToFile(context, characterId, uri)
            exportState = if (result.isSuccess) {
                ExportState.Success("Charakter erfolgreich exportiert")
            } else {
                ExportState.Error("Export fehlgeschlagen: ${result.exceptionOrNull()?.message}")
            }
        }
    }
    
    private suspend fun resolveGroupDate(character: Character): String {
        val group = character.groupId?.let { repository.getGroupByIdOnce(it) }
        return group?.currentDerianDate ?: "1 Praios 1040 BF"
    }
    
    private fun calculateLongDurationExpiry(slot: SpellSlot, zfpStar: Int, groupDate: String): String? {
        val formula = slot.longDurationFormula.takeIf { it.isNotBlank() } ?: return null
        val evaluation = ProbeChecker.evaluateDurationSpecification(formula, zfpStar) ?: return null
        return DerianDateCalculator.calculateExpiryDate(groupDate, evaluation.toShelfLifeString())
    }
    
    /**
     * Importiert einen Charakter aus einer Datei und überschreibt den aktuellen Charakter.
     * GUID-Validierung stellt sicher, dass nur der richtige Charakter überschrieben wird.
     */
    fun importCharacterFromFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            exportState = ExportState.Exporting
            val result = exportManager.loadCharacterFromFile(context, uri, characterId)
            exportState = if (result.isSuccess) {
                val (_, warning) = result.getOrNull()!!
                val message = if (warning != null) {
                    "Import erfolgreich mit Warnung:\n$warning"
                } else {
                    "Charakter erfolgreich überschrieben"
                }
                ExportState.Success(message)
            } else {
                ExportState.Error("Import fehlgeschlagen: ${result.exceptionOrNull()?.message}")
            }
        }
    }
    
    suspend fun exportCharacterForNearby(): Result<CharacterExportDto> {
        return try {
            val jsonResult = exportManager.exportCharacter(characterId)
            if (jsonResult.isFailure) {
                return Result.failure(jsonResult.exceptionOrNull() ?: Exception("Export fehlgeschlagen"))
            }
            
            // Parse JSON zurück zu DTO für Nearby
            val json = kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
            }
            val dto = json.decodeFromString<CharacterExportDto>(jsonResult.getOrNull()!!)
            Result.success(dto)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Importiert einen Charakter von Nearby und überschreibt den aktuellen Charakter.
     * GUID-Validierung stellt sicher, dass nur der richtige Charakter überschrieben wird.
     */
    suspend fun importCharacterFromNearby(dto: CharacterExportDto): Result<Pair<Long, String?>> {
        return try {
            val json = kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
            }
            val jsonString = json.encodeToString(kotlinx.serialization.serializer(), dto)
            exportManager.importCharacter(jsonString, characterId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun resetExportState() {
        exportState = ExportState.Idle
    }
}

class CharacterDetailViewModelFactory(
    private val repository: ApplicatusRepository,
    private val characterId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CharacterDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CharacterDetailViewModel(repository, characterId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
