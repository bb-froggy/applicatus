package de.applicatus.app.ui.screen.character

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.applicatus.app.R
import de.applicatus.app.data.model.character.Character

@Composable
fun CharacterTalentsCard(
    character: Character,
    isEditMode: Boolean = false,
    onClick: () -> Unit = {}
) {
    // Prüfe, ob der Charakter überhaupt Talente hat
    val hasTalents = character.hasAlchemy || 
                     character.hasCookingPotions || 
                     character.selfControlSkill > 0 || 
                     character.sensoryAcuitySkill > 0 || 
                     character.magicalLoreSkill > 0 || 
                     character.herbalLoreSkill > 0
    
    // Im Edit-Modus zeigen wir die Card immer an, damit Talente hinzugefügt werden können
    if (!hasTalents && !isEditMode) return
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isEditMode) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.talents),
                style = MaterialTheme.typography.titleMedium
            )
            HorizontalDivider()
            
            // Alchimie
            if (character.hasAlchemy) {
                TalentItem(
                    name = stringResource(R.string.alchemy),
                    value = character.alchemySkill
                )
            }
            
            // Kochen (Tränke)
            if (character.hasCookingPotions) {
                TalentItem(
                    name = stringResource(R.string.cooking_potions),
                    value = character.cookingPotionsSkill
                )
            }
            
            // Selbstbeherrschung
            if (character.selfControlSkill > 0) {
                TalentItem(
                    name = stringResource(R.string.self_control),
                    value = character.selfControlSkill
                )
            }
            
            // Sinnenschärfe
            if (character.sensoryAcuitySkill > 0) {
                TalentItem(
                    name = stringResource(R.string.sensory_acuity),
                    value = character.sensoryAcuitySkill
                )
            }
            
            // Magiekunde
            if (character.magicalLoreSkill > 0) {
                TalentItem(
                    name = stringResource(R.string.magical_lore),
                    value = character.magicalLoreSkill
                )
            }
            
            // Pflanzenkunde
            if (character.herbalLoreSkill > 0) {
                TalentItem(
                    name = stringResource(R.string.herbal_lore),
                    value = character.herbalLoreSkill
                )
            }
        }
    }
}

@Composable
private fun TalentItem(name: String, value: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
