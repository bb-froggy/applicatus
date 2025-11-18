package de.applicatus.app.ui.component.character

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.applicatus.app.data.model.character.Character

@Composable
fun EditCharacterDialog(
    character: Character,
    onDismiss: () -> Unit,
    onConfirm: (Character) -> Unit
) {
    var mu by remember { mutableStateOf(character.mu.toString()) }
    var kl by remember { mutableStateOf(character.kl.toString()) }
    var inValue by remember { mutableStateOf(character.inValue.toString()) }
    var ch by remember { mutableStateOf(character.ch.toString()) }
    var ff by remember { mutableStateOf(character.ff.toString()) }
    var ge by remember { mutableStateOf(character.ge.toString()) }
    var ko by remember { mutableStateOf(character.ko.toString()) }
    var kk by remember { mutableStateOf(character.kk.toString()) }
    var hasApplicatus by remember { mutableStateOf(character.hasApplicatus) }
    var applicatusZfw by remember { mutableStateOf(character.applicatusZfw.toString()) }
    var applicatusModifier by remember { mutableStateOf(character.applicatusModifier.toString()) }
    
    var maxLe by remember { mutableStateOf(character.maxLe.toString()) }
    var hasAe by remember { mutableStateOf(character.hasAe) }
    var maxAe by remember { mutableStateOf(character.maxAe.toString()) }
    var hasKe by remember { mutableStateOf(character.hasKe) }
    var maxKe by remember { mutableStateOf(character.maxKe.toString()) }
    var leRegenBonus by remember { mutableStateOf(character.leRegenBonus.toString()) }
    var aeRegenBonus by remember { mutableStateOf(character.aeRegenBonus.toString()) }
    var hasMasteryRegeneration by remember { mutableStateOf(character.hasMasteryRegeneration) }
    
    // Alchimie & Zauber
    var hasAlchemy by remember { mutableStateOf(character.hasAlchemy) }
    var alchemySkill by remember { mutableStateOf(character.alchemySkill.toString()) }
    var alchemyIsMagicalMastery by remember { mutableStateOf(character.alchemyIsMagicalMastery) }
    var hasCookingPotions by remember { mutableStateOf(character.hasCookingPotions) }
    var cookingPotionsSkill by remember { mutableStateOf(character.cookingPotionsSkill.toString()) }
    var cookingPotionsIsMagicalMastery by remember { mutableStateOf(character.cookingPotionsIsMagicalMastery) }
    var hasOdem by remember { mutableStateOf(character.hasOdem) }
    var odemZfw by remember { mutableStateOf(character.odemZfw.toString()) }
    var hasAnalys by remember { mutableStateOf(character.hasAnalys) }
    var analysZfw by remember { mutableStateOf(character.analysZfw.toString()) }
    
    // Zusätzliche Talente für Analyse
    var selfControlSkill by remember { mutableStateOf(character.selfControlSkill.toString()) }
    var sensoryAcuitySkill by remember { mutableStateOf(character.sensoryAcuitySkill.toString()) }
    var magicalLoreSkill by remember { mutableStateOf(character.magicalLoreSkill.toString()) }
    var herbalLoreSkill by remember { mutableStateOf(character.herbalLoreSkill.toString()) }
    
    // Zauber-Sonderfertigkeiten
    var kraftkontrolle by remember { mutableStateOf(character.kraftkontrolle) }
    var hasStaffWithKraftfokus by remember { mutableStateOf(character.hasStaffWithKraftfokus) }
    
    // Spieler/Spielleiter-Modus
    var isGameMaster by remember { mutableStateOf(character.isGameMaster) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Charakter bearbeiten") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text("Eigenschaften:", style = MaterialTheme.typography.titleSmall)
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = mu,
                            onValueChange = { mu = it.filter { c -> c.isDigit() } },
                            label = { Text("MU") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = kl,
                            onValueChange = { kl = it.filter { c -> c.isDigit() } },
                            label = { Text("KL") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = inValue,
                            onValueChange = { inValue = it.filter { c -> c.isDigit() } },
                            label = { Text("IN") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = ch,
                            onValueChange = { ch = it.filter { c -> c.isDigit() } },
                            label = { Text("CH") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = ff,
                            onValueChange = { ff = it.filter { c -> c.isDigit() } },
                            label = { Text("FF") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = ge,
                            onValueChange = { ge = it.filter { c -> c.isDigit() } },
                            label = { Text("GE") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = ko,
                            onValueChange = { ko = it.filter { c -> c.isDigit() } },
                            label = { Text("KO") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = kk,
                            onValueChange = { kk = it.filter { c -> c.isDigit() } },
                            label = { Text("KK") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Energien:", style = MaterialTheme.typography.titleSmall)
                }
                item {
                    OutlinedTextField(
                        value = maxLe,
                        onValueChange = { maxLe = it.filter { c -> c.isDigit() } },
                        label = { Text("Max LE") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = leRegenBonus,
                        onValueChange = { 
                            val num = it.filter { c -> c.isDigit() || c == '-' }
                            leRegenBonus = num
                        },
                        label = { Text("LE-Regenerationsbonus (-3 bis +3)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = hasAe,
                            onCheckedChange = { hasAe = it }
                        )
                        Text("Hat Astralenergie")
                    }
                }
                if (hasAe) {
                    item {
                        OutlinedTextField(
                            value = maxAe,
                            onValueChange = { maxAe = it.filter { c -> c.isDigit() } },
                            label = { Text("Max AE") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = aeRegenBonus,
                            onValueChange = { 
                                val num = it.filter { c -> c.isDigit() || c == '-' }
                                aeRegenBonus = num
                            },
                            label = { Text("AE-Regenerationsbonus (-3 bis +3)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = hasMasteryRegeneration,
                                onCheckedChange = { hasMasteryRegeneration = it }
                            )
                            Text("Meisterliche Regeneration")
                        }
                    }
                }
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = hasKe,
                            onCheckedChange = { hasKe = it }
                        )
                        Text("Hat Karmaenergie")
                    }
                }
                if (hasKe) {
                    item {
                        OutlinedTextField(
                            value = maxKe,
                            onValueChange = { maxKe = it.filter { c -> c.isDigit() } },
                            label = { Text("Max KE") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Applicatus:", style = MaterialTheme.typography.titleSmall)
                }
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = hasApplicatus,
                            onCheckedChange = { hasApplicatus = it }
                        )
                        Text("Charakter hat Applicatus")
                    }
                }
                if (hasApplicatus) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = applicatusZfw,
                                onValueChange = { applicatusZfw = it.filter { c -> c.isDigit() } },
                                label = { Text("Applicatus ZfW") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = applicatusModifier,
                                onValueChange = { applicatusModifier = it },
                                label = { Text("Applicatus Mod") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                
                // Alchimie & Kochkunst
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Alchimie:", style = MaterialTheme.typography.titleSmall)
                }
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = hasAlchemy,
                            onCheckedChange = { hasAlchemy = it }
                        )
                        Text("Beherrscht Alchimie")
                    }
                }
                if (hasAlchemy) {
                    item {
                        OutlinedTextField(
                            value = alchemySkill,
                            onValueChange = { alchemySkill = it.filter { c -> c.isDigit() } },
                            label = { Text("Alchimie TaW (0-18)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    // Magisches Meisterhandwerk für Alchimie (nur bei AE)
                    if (hasAe) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = alchemyIsMagicalMastery,
                                    onCheckedChange = { alchemyIsMagicalMastery = it }
                                )
                                Text("Magisches Meisterhandwerk", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = hasCookingPotions,
                            onCheckedChange = { hasCookingPotions = it }
                        )
                        Text("Beherrscht Kochen (Tränke)")
                    }
                }
                if (hasCookingPotions) {
                    item {
                        OutlinedTextField(
                            value = cookingPotionsSkill,
                            onValueChange = { cookingPotionsSkill = it.filter { c -> c.isDigit() } },
                            label = { Text("Kochen (Tränke) TaW (0-18)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    // Magisches Meisterhandwerk für Kochen (nur bei AE)
                    if (hasAe) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = cookingPotionsIsMagicalMastery,
                                    onCheckedChange = { cookingPotionsIsMagicalMastery = it }
                                )
                                Text("Magisches Meisterhandwerk", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                
                // Zusätzliche Talente für Analyse (immer sichtbar)
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Analyse-Talente:", style = MaterialTheme.typography.titleSmall)
                }
                item {
                    OutlinedTextField(
                        value = selfControlSkill,
                        onValueChange = { selfControlSkill = it.filter { c -> c.isDigit() } },
                        label = { Text("Selbstbeherrschung TaW (0-18)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = sensoryAcuitySkill,
                        onValueChange = { sensoryAcuitySkill = it.filter { c -> c.isDigit() } },
                        label = { Text("Sinnenschärfe TaW (0-18)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (hasAe || hasAlchemy) {
                    item {
                        OutlinedTextField(
                            value = magicalLoreSkill,
                            onValueChange = { magicalLoreSkill = it.filter { c -> c.isDigit() } },
                            label = { Text("Magiekunde TaW (0-18)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                if (hasAlchemy) {
                    item {
                        OutlinedTextField(
                            value = herbalLoreSkill,
                            onValueChange = { herbalLoreSkill = it.filter { c -> c.isDigit() } },
                            label = { Text("Pflanzenkunde TaW (0-18)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                // Analyse-Zauber (nur für magisch Begabte)
                if (hasAe) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Analyse-Zauber:", style = MaterialTheme.typography.titleSmall)
                    }
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = hasOdem,
                                onCheckedChange = { hasOdem = it }
                            )
                            Text("Beherrscht ODEM ARCANUM")
                        }
                    }
                    if (hasOdem) {
                        item {
                            OutlinedTextField(
                                value = odemZfw,
                                onValueChange = { odemZfw = it.filter { c -> c.isDigit() } },
                                label = { Text("ODEM ARCANUM ZfW (0-18)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = hasAnalys,
                                onCheckedChange = { hasAnalys = it }
                            )
                            Text("Beherrscht ANALYS ARKANSTRUKTUR")
                        }
                    }
                    if (hasAnalys) {
                        item {
                            OutlinedTextField(
                                value = analysZfw,
                                onValueChange = { analysZfw = it.filter { c -> c.isDigit() } },
                                label = { Text("ANALYS ZfW (0-18)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                
                // Zauber-Sonderfertigkeiten
                if (hasAe) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Zauber-Sonderfertigkeiten:", style = MaterialTheme.typography.titleSmall)
                    }
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = kraftkontrolle,
                                onCheckedChange = { kraftkontrolle = it }
                            )
                            Text("Kraftkontrolle (-1 AsP pro Zauber)")
                        }
                    }
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = hasStaffWithKraftfokus,
                                onCheckedChange = { hasStaffWithKraftfokus = it }
                            )
                            Text("Hat Zauberstab mit Kraftfokus (-1 AsP pro Zauber)")
                        }
                    }
                }
                
                // Spieler/Spielleiter-Modus
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Spielmodus:", style = MaterialTheme.typography.titleSmall)
                }
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = isGameMaster,
                            onCheckedChange = { isGameMaster = it }
                        )
                        Text("Spielleiter-Modus (zeigt alle Informationen)")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val newMaxLe = maxLe.toIntOrNull() ?: character.maxLe
                    val newMaxAe = if (hasAe) maxAe.toIntOrNull() ?: character.maxAe else 0
                    val newMaxKe = if (hasKe) maxKe.toIntOrNull() ?: character.maxKe else 0
                    
                    val updatedCharacter = character.copy(
                        mu = mu.toIntOrNull() ?: 8,
                        kl = kl.toIntOrNull() ?: 8,
                        inValue = inValue.toIntOrNull() ?: 8,
                        ch = ch.toIntOrNull() ?: 8,
                        ff = ff.toIntOrNull() ?: 8,
                        ge = ge.toIntOrNull() ?: 8,
                        ko = ko.toIntOrNull() ?: 8,
                        kk = kk.toIntOrNull() ?: 8,
                        maxLe = newMaxLe,
                        currentLe = character.currentLe.coerceAtMost(newMaxLe),
                        leRegenBonus = (leRegenBonus.toIntOrNull() ?: 0).coerceIn(-3, 3),
                        hasAe = hasAe,
                        maxAe = newMaxAe,
                        currentAe = if (hasAe) character.currentAe.coerceAtMost(newMaxAe) else 0,
                        aeRegenBonus = if (hasAe) (aeRegenBonus.toIntOrNull() ?: 0).coerceIn(-3, 3) else 0,
                        hasMasteryRegeneration = if (hasAe) hasMasteryRegeneration else false,
                        hasKe = hasKe,
                        maxKe = newMaxKe,
                        currentKe = if (hasKe) character.currentKe.coerceAtMost(newMaxKe) else 0,
                        hasApplicatus = hasApplicatus,
                        applicatusZfw = applicatusZfw.toIntOrNull() ?: 0,
                        applicatusModifier = applicatusModifier.toIntOrNull() ?: 0,
                        hasAlchemy = hasAlchemy,
                        alchemySkill = if (hasAlchemy) (alchemySkill.toIntOrNull() ?: 0) else 0,
                        alchemyIsMagicalMastery = if (hasAlchemy && hasAe) alchemyIsMagicalMastery else false,
                        hasCookingPotions = hasCookingPotions,
                        cookingPotionsSkill = if (hasCookingPotions) (cookingPotionsSkill.toIntOrNull() ?: 0) else 0,
                        cookingPotionsIsMagicalMastery = if (hasCookingPotions && hasAe) cookingPotionsIsMagicalMastery else false,
                        hasOdem = if (hasAe) hasOdem else false,
                        odemZfw = if (hasAe && hasOdem) (odemZfw.toIntOrNull() ?: 0) else 0,
                        hasAnalys = if (hasAe) hasAnalys else false,
                        analysZfw = if (hasAe && hasAnalys) (analysZfw.toIntOrNull() ?: 0) else 0,
                        selfControlSkill = (selfControlSkill.toIntOrNull() ?: 0).coerceIn(0, 18),
                        sensoryAcuitySkill = (sensoryAcuitySkill.toIntOrNull() ?: 0).coerceIn(0, 18),
                        magicalLoreSkill = (magicalLoreSkill.toIntOrNull() ?: 0).coerceIn(0, 18),
                        herbalLoreSkill = (herbalLoreSkill.toIntOrNull() ?: 0).coerceIn(0, 18),
                        kraftkontrolle = if (hasAe) kraftkontrolle else false,
                        hasStaffWithKraftfokus = if (hasAe) hasStaffWithKraftfokus else false,
                        isGameMaster = isGameMaster
                    )
                    onConfirm(updatedCharacter)
                }
            ) {
                Text("Speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}
