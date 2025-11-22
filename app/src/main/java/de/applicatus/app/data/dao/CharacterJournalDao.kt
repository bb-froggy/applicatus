package de.applicatus.app.data.dao

import androidx.room.*
import de.applicatus.app.data.model.character.CharacterJournalEntry
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for CharacterJournalEntry.
 * 
 * Provides methods to:
 * - Query entries for a character (sorted by timestamp descending)
 * - Insert new journal entries
 * - Delete entries (typically via CASCADE when character is deleted)
 * - Query by category for filtering
 */
@Dao
interface CharacterJournalDao {
    
    /**
     * Get all journal entries for a character, ordered by timestamp (newest first).
     * Returns a Flow for reactive UI updates.
     */
    @Query("SELECT * FROM character_journal_entries WHERE characterId = :characterId ORDER BY timestamp DESC")
    fun getEntriesForCharacter(characterId: Long): Flow<List<CharacterJournalEntry>>
    
    /**
     * Get all journal entries for a character, ordered by timestamp (oldest first).
     * Useful for exports and chronological views.
     */
    @Query("SELECT * FROM character_journal_entries WHERE characterId = :characterId ORDER BY timestamp ASC")
    fun getEntriesForCharacterAscending(characterId: Long): Flow<List<CharacterJournalEntry>>
    
    /**
     * Get journal entries for a character filtered by category.
     * Example: Get all "Potion.Brewed" events
     */
    @Query("SELECT * FROM character_journal_entries WHERE characterId = :characterId AND category = :category ORDER BY timestamp DESC")
    fun getEntriesByCategory(characterId: Long, category: String): Flow<List<CharacterJournalEntry>>
    
    /**
     * Get journal entries for a character filtered by category prefix.
     * Example: Get all "Potion.*" events (Potion.Brewed, Potion.Consumed, etc.)
     * Uses LIKE for pattern matching.
     */
    @Query("SELECT * FROM character_journal_entries WHERE characterId = :characterId AND category LIKE :categoryPattern ORDER BY timestamp DESC")
    fun getEntriesByCategoryPattern(characterId: Long, categoryPattern: String): Flow<List<CharacterJournalEntry>>
    
    /**
     * Get a single journal entry by ID.
     */
    @Query("SELECT * FROM character_journal_entries WHERE id = :id")
    suspend fun getEntryById(id: Long): CharacterJournalEntry?
    
    /**
     * Get all entries (synchronous, for export).
     */
    @Query("SELECT * FROM character_journal_entries WHERE characterId = :characterId ORDER BY timestamp ASC")
    suspend fun getEntriesForCharacterOnce(characterId: Long): List<CharacterJournalEntry>
    
    /**
     * Insert a new journal entry.
     * Returns the ID of the inserted entry.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: CharacterJournalEntry): Long
    
    /**
     * Insert multiple journal entries.
     * Useful for batch imports.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<CharacterJournalEntry>)
    
    /**
     * Update an existing journal entry.
     * Note: Typically journal entries are immutable once created.
     */
    @Update
    suspend fun updateEntry(entry: CharacterJournalEntry)
    
    /**
     * Delete a specific journal entry.
     */
    @Delete
    suspend fun deleteEntry(entry: CharacterJournalEntry)
    
    /**
     * Delete all journal entries for a character.
     * Note: Typically handled by CASCADE when character is deleted.
     */
    @Query("DELETE FROM character_journal_entries WHERE characterId = :characterId")
    suspend fun deleteEntriesForCharacter(characterId: Long)
    
    /**
     * Count total journal entries for a character.
     */
    @Query("SELECT COUNT(*) FROM character_journal_entries WHERE characterId = :characterId")
    suspend fun getEntryCount(characterId: Long): Int
    
    /**
     * Get the most recent journal entry for a character.
     */
    @Query("SELECT * FROM character_journal_entries WHERE characterId = :characterId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestEntry(characterId: Long): CharacterJournalEntry?
}
