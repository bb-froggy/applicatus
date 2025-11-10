package de.applicatus.app.data

import androidx.room.TypeConverter
import de.applicatus.app.data.model.inventory.Weight
import de.applicatus.app.data.model.potion.*
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
    fun fromPotionQualityNullable(value: PotionQuality?): String? {
        return value?.name
    }
    
    @TypeConverter
    fun toPotionQualityNullable(value: String?): PotionQuality? {
        return value?.let { PotionQuality.valueOf(it) }
    }
    
    @TypeConverter
    fun fromKnownQualityLevel(value: KnownQualityLevel): String {
        return value.name
    }
    
    @TypeConverter
    fun toKnownQualityLevel(value: String): KnownQualityLevel {
        return KnownQualityLevel.valueOf(value)
    }
    
    @TypeConverter
    fun fromIntensityQuality(value: IntensityQuality): String {
        return value.name
    }
    
    @TypeConverter
    fun toIntensityQuality(value: String): IntensityQuality {
        return IntensityQuality.valueOf(value)
    }
    
    @TypeConverter
    fun fromRefinedQuality(value: RefinedQuality): String {
        return value.name
    }
    
    @TypeConverter
    fun toRefinedQuality(value: String): RefinedQuality {
        return RefinedQuality.valueOf(value)
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
    
    // Weight Converters (stored as total ounces)
    @TypeConverter
    fun fromWeight(value: Weight): Int {
        return value.toOunces()
    }
    
    @TypeConverter
    fun toWeight(value: Int): Weight {
        return Weight.fromOunces(value)
    }
}
