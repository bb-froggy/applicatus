package de.applicatus.app.data

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit Tests für die Datenmodell-Versionierung.
 */
class DataModelVersionTest {
    
    @Test
    fun `current version is 6`() {
        assertEquals(6, DataModelVersion.CURRENT_VERSION)
    }
    
    @Test
    fun `same version is compatible`() {
        val (isCompatible, warning) = DataModelVersion.checkCompatibility(6)
        assertTrue(isCompatible)
        assertNull(warning)
    }
    
    @Test
    fun `older version is compatible with warning`() {
        val (isCompatible, warning) = DataModelVersion.checkCompatibility(4)
        assertTrue(isCompatible)
        assertNotNull(warning)
        assertTrue(warning!!.contains("älteren Version"))
    }
    
    @Test
    fun `newer version is incompatible`() {
        val (isCompatible, warning) = DataModelVersion.checkCompatibility(7)
        assertFalse(isCompatible)
        assertNotNull(warning)
        assertTrue(warning!!.contains("neueren Version"))
    }
    
    @Test
    fun `overwrite with older version shows warning`() {
        val warning = DataModelVersion.checkOverwriteWarning(
            existingVersion = 5,
            importVersion = 4
        )
        assertNotNull(warning)
        assertTrue(warning!!.contains("älteren Version"))
    }
    
    @Test
    fun `overwrite with same version shows no warning`() {
        val warning = DataModelVersion.checkOverwriteWarning(
            existingVersion = 5,
            importVersion = 5
        )
        assertNull(warning)
    }
    
    @Test
    fun `overwrite with newer version shows no warning`() {
        val warning = DataModelVersion.checkOverwriteWarning(
            existingVersion = 2,
            importVersion = 3
        )
        assertNull(warning)
    }
}
