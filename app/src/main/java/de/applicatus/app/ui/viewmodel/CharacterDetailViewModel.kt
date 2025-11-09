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
                // Führe Applicatus-Probe durch
                val result = SpellChecker.performApplicatusCheck(
                    spellZfw = slot.zfw,
                    spellModifier = slot.modifier,
                    spellAttribute1 = attr1,
                    spellAttribute2 = attr2,
                    spellAttribute3 = attr3,
                    applicatusZfw = char.applicatusZfw,
                    applicatusModifier = char.applicatusModifier,
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
                
                // Aktualisiere den Slot
                repository.updateSlot(
                    slot.copy(
                        // Slot wird nur bei Erfolg ODER Patzer belegt
                        // Bei normalem Fehlschlag bleibt Slot leer
                        isFilled = result.overallSuccess || isPatzer,
                        zfpStar = if (result.overallSuccess) result.spellResult.zfpStar else null,
                        lastRollResult = formatRollResult(result.spellResult),
                        applicatusRollResult = result.applicatusResult?.let { formatRollResult(it) },
                        isBotched = isPatzer // true wenn Patzer, false sonst
                    )
                )
            } else {
                // Normale Zauberprobe (Zauberspeicher)
                val result = SpellChecker.performSpellCheck(
                    zfw = slot.zfw,
                    modifier = slot.modifier,
                    attribute1 = attr1,
                    attribute2 = attr2,
                    attribute3 = attr3
                )
                
                // Prüfe auf Patzer (Doppel-20 oder Dreifach-20)
                val isPatzer = result.isDoubleTwenty || result.isTripleTwenty
                
                // Aktualisiere den Slot
                repository.updateSlot(
                    slot.copy(
                        // Slot wird nur bei Erfolg ODER Patzer belegt
                        // Bei normalem Fehlschlag bleibt Slot leer
                        isFilled = result.success || isPatzer,
                        zfpStar = if (result.success) result.zfpStar else null,
                        lastRollResult = formatRollResult(result),
                        applicatusRollResult = null,
                        isBotched = isPatzer // true wenn Patzer, false sonst
                    )
                )
            }
        }
    }
    
    fun clearSlot(slot: SpellSlot) {
        viewModelScope.launch {
            // Prüfe, ob der Slot einen verpatzten Zauber enthält
            if (slot.isBotched) {
                spellCastMessage = "⚠️ PATZER! Der Zauber ist fehlgeschlagen und verpufft wirkungslos!"
            } else if (slot.isFilled) {
                spellCastMessage = "✓ Zauber erfolgreich ausgelöst!"
            }
            
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
    
    private fun formatRollResult(result: de.applicatus.app.logic.SpellCheckResult): String {
        val rollsStr = result.rolls.joinToString(", ")
        return when {
            result.isTripleOne -> "Dreifach-1! [$rollsStr] ZfP*: ${result.zfpStar}"
            result.isDoubleOne -> "Doppel-1! [$rollsStr] ZfP*: ${result.zfpStar}"
            result.isTripleTwenty -> "Dreifach-20! [$rollsStr] Katastrophe!"
            result.isDoubleTwenty -> "Doppel-20! [$rollsStr] Patzer!"
            result.success -> "Erfolg! [$rollsStr] ZfP*: ${result.zfpStar}"
            else -> "Fehlgeschlagen! [$rollsStr]"
        }
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
