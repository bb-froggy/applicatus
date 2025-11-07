package de.applicatus.app.data

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit Tests für die Datenmodell-Versionierung.
 */
class DataModelVersionTest {
    
    @Test
    fun `current version is 3`() {
        assertEquals(3, DataModelVersion.CURRENT_VERSION)
    }
    
    @Test
    fun `same version is compatible`() {
        val (isCompatible, warning) = DataModelVersion.checkCompatibility(3)
        assertTrue(isCompatible)
        assertNull(warning)
    }
    
    @Test
    fun `older version is compatible with warning`() {
        val (isCompatible, warning) = DataModelVersion.checkCompatibility(2)
        assertTrue(isCompatible)
        assertNotNull(warning)
        assertTrue(warning!!.contains("älteren Version"))
    }
    
    @Test
    fun `newer version is incompatible`() {
        val (isCompatible, warning) = DataModelVersion.checkCompatibility(4)
        assertFalse(isCompatible)
        assertNotNull(warning)
        assertTrue(warning!!.contains("neueren Version"))
    }
    
    @Test
    fun `overwrite with older version shows warning`() {
        val warning = DataModelVersion.checkOverwriteWarning(
            existingVersion = 3,
            importVersion = 2
        )
        assertNotNull(warning)
        assertTrue(warning!!.contains("älteren Version"))
    }
    
    @Test
    fun `overwrite with same version shows no warning`() {
        val warning = DataModelVersion.checkOverwriteWarning(
            existingVersion = 3,
            importVersion = 3
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
