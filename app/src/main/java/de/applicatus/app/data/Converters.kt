package de.applicatus.app.data

import androidx.room.TypeConverter
import de.applicatus.app.data.model.PotionQuality
import de.applicatus.app.data.model.SlotType

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
}
