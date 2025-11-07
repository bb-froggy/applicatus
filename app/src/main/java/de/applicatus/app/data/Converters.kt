package de.applicatus.app.data

import androidx.room.TypeConverter
import de.applicatus.app.data.model.potion.AnalysisStatus
import de.applicatus.app.data.model.potion.Laboratory
import de.applicatus.app.data.model.potion.PotionQuality
import de.applicatus.app.data.model.potion.RecipeKnowledgeLevel
import de.applicatus.app.data.model.spell.SlotType

class Converters {
    @TypeConverter
    fun fromSlotType(value: SlotType): String {
        return value.name
    }
    
    @TypeConverter
    fun toSlotType(value: String): SlotType {
        return SlotType.valueOf(value)
    }
    
    @TypeConverter
    fun fromPotionQuality(value: PotionQuality): String {
        return value.name
    }
    
    @TypeConverter
    fun toPotionQuality(value: String): PotionQuality {
        return PotionQuality.valueOf(value)
    }
    
    @TypeConverter
    fun fromAnalysisStatus(value: AnalysisStatus): String {
        return value.name
    }
    
    @TypeConverter
    fun toAnalysisStatus(value: String): AnalysisStatus {
        return AnalysisStatus.valueOf(value)
    }
    
    @TypeConverter
    fun fromRecipeKnowledgeLevel(value: RecipeKnowledgeLevel): String {
        return value.name
    }
    
    @TypeConverter
    fun toRecipeKnowledgeLevel(value: String): RecipeKnowledgeLevel {
        return RecipeKnowledgeLevel.valueOf(value)
    }
    
    @TypeConverter
    fun fromLaboratory(value: Laboratory?): String? {
        return value?.name
    }
    
    @TypeConverter
    fun toLaboratory(value: String?): Laboratory? {
        return value?.let { Laboratory.valueOf(it) }
    }
}
