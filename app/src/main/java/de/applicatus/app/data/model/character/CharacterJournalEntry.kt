package de.applicatus.app.data.model.character

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a journal entry for a character, tracking all significant events.
 * 
 * Each entry includes:
 * - Earthly timestamp (Unix timestamp in milliseconds)
 * - Derian date (from group's currentDerianDate at time of event)
 * - Event category (for UI icons and filtering)
 * - Player-visible message
 * - Optional game master-only message
 */
@Entity(
    tableName = "character_journal_entries",
    foreignKeys = [
        ForeignKey(
            entity = Character::class,
            parentColumns = ["id"],
            childColumns = ["characterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("characterId"),
        Index("timestamp")
    ]
)
data class CharacterJournalEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /**
     * ID of the character this entry belongs to.
     */
    val characterId: Long,
    
    /**
     * Earthly timestamp in milliseconds (System.currentTimeMillis()).
     * Used for precise ordering and export metadata.
     */
    val timestamp: Long = System.currentTimeMillis(),
    
    /**
     * Derian date at time of event (e.g., "15 Praios 1040 BF").
     * Captured from Group.currentDerianDate when entry is created.
     */
    val derianDate: String,
    
    /**
     * Event category for UI display (icon, color, filtering).
     * Examples: "Potion.Brew", "Spell.Cast", "Energy.Regeneration"
     */
    val category: String,
    
    /**
     * Message visible to all players.
     * Example: "Heiltrank gebraut"
     */
    val playerMessage: String,
    
    /**
     * Optional message visible only to game master.
     * Example: "Qualität: C, haltbar bis 23. Ingerimm 1031 BF"
     * Null if no GM-specific information.
     */
    val gmMessage: String? = null
)

/**
 * Predefined event categories for journal entries.
 * Format: "<Domain>.<Action>"
 * 
 * Used for:
 * - UI icons and visual indicators
 * - Filtering and grouping
 * - Export categorization
 */
object JournalCategory {
    // Character events
    const val CHARACTER_CREATED = "Character.Created"
    const val CHARACTER_RENAMED = "Character.Renamed"
    const val CHARACTER_MODIFIED = "Character.Modified"
    
    // Energy events
    const val ENERGY_REGENERATION = "Energy.Regeneration"
    const val ENERGY_LE_CHANGED = "Energy.LE.Changed"
    const val ENERGY_AE_CHANGED = "Energy.AE.Changed"
    const val ENERGY_KE_CHANGED = "Energy.KE.Changed"
    
    // Spell events
    const val SPELL_SLOT_ADDED = "Spell.Slot.Added"
    const val SPELL_SLOT_REMOVED = "Spell.Slot.Removed"
    const val SPELL_STORED = "Spell.Stored"
    const val SPELL_CAST = "Spell.Cast"
    const val SPELL_CLEARED = "Spell.Cleared"
    const val SPELL_EXPIRED = "Spell.Expired"
    
    // Potion events
    const val POTION_BREWED = "Potion.Brewed"
    const val POTION_CONSUMED = "Potion.Consumed"
    const val POTION_ANALYSIS_INTENSITY = "Potion.Analysis.Intensity"
    const val POTION_ANALYSIS_STRUCTURE = "Potion.Analysis.Structure"
    const val POTION_ANALYSIS_AUGENSCHEIN = "Potion.Analysis.Augenschein"
    const val POTION_ANALYSIS_LABOR = "Potion.Analysis.Labor"
    const val POTION_DILUTED = "Potion.Diluted"
    const val POTION_ACQUIRED = "Potion.Acquired"
    const val POTION_GIVEN = "Potion.Given"
    const val POTION_EXPIRED = "Potion.Expired"
    
    // Recipe events
    const val RECIPE_LEARNED = "Recipe.Learned"
    const val RECIPE_DISCOVERED = "Recipe.Discovered"
    
    // Inventory events
    const val INVENTORY_ITEM_ACQUIRED = "Inventory.Item.Acquired"
    const val INVENTORY_ITEM_REMOVED = "Inventory.Item.Removed"
    const val INVENTORY_ITEM_MOVED = "Inventory.Item.Moved"
    const val INVENTORY_LOCATION_CREATED = "Inventory.Location.Created"
    
    // Talent events
    const val TALENT_IMPROVED = "Talent.Improved"
    const val TALENT_USED = "Talent.Used"
    
    // Combat events
    const val COMBAT_DAMAGE_TAKEN = "Combat.Damage.Taken"
    const val COMBAT_DAMAGE_DEALT = "Combat.Damage.Dealt"
    
    // Group events
    const val GROUP_JOINED = "Group.Joined"
    const val GROUP_LEFT = "Group.Left"
    const val GROUP_DATE_ADVANCED = "Group.Date.Advanced"
    
    // Miscellaneous
    const val OTHER = "Other"
}
