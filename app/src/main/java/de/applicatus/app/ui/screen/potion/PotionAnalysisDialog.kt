package de.applicatus.app.ui.screen.potion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import de.applicatus.app.data.model.potion.AnalysisMethod
import de.applicatus.app.data.model.potion.AnalysisStatus
import de.applicatus.app.data.model.potion.PotionQuality
import de.applicatus.app.data.model.potion.PotionWithRecipe
import de.applicatus.app.data.model.potion.RecipeKnowledgeLevel
import de.applicatus.app.logic.AnalysisResult
import de.applicatus.app.logic.PotionAnalyzer
import de.applicatus.app.ui.viewmodel.PotionViewModel
import de.applicatus.app.ui.viewmodel.PotionViewModelFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PotionAnalysisDialog(
    potionWithRecipe: PotionWithRecipe,
    characterId: Long,
    viewModelFactory: PotionViewModelFactory,
    onDismiss: () -> Unit,
    onAnalysisComplete: () -> Unit
) {
    val viewModel: PotionViewModel = viewModel(factory = viewModelFactory)
    val character by viewModel.character.collectAsState()
    val recipeKnowledge by viewModel.getRecipeKnowledge(potionWithRecipe.recipe.id).collectAsState(null)

    val defaultMethod = remember(character) {
        character?.let { char ->
            when {
                char.hasAlchemy -> AnalysisMethod.BY_SIGHT
                char.hasOdem -> AnalysisMethod.ODEM_SPELL
                char.hasAnalys -> AnalysisMethod.ANALYS_SPELL
                else -> AnalysisMethod.BY_SIGHT
            }
        } ?: AnalysisMethod.BY_SIGHT
    }

    var selectedMethod by remember(defaultMethod) { mutableStateOf(defaultMethod) }
    var analysisResult by remember { mutableStateOf<AnalysisResult?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Trank analysieren",
                    style = MaterialTheme.typography.headlineSmall
                )

                Divider()

                Text(
                    text = potionWithRecipe.recipe.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Qualität: ${'$'}{getQualityLabel(potionWithRecipe.potion.quality)}",
                    style = MaterialTheme.typography.bodyMedium
                )

                val currentStatusText = when (potionWithRecipe.potion.analysisStatus) {
                    AnalysisStatus.NOT_ANALYZED -> "Noch nicht analysiert"
                    AnalysisStatus.ROUGH_ANALYZED -> "Bereits grob analysiert"
                    AnalysisStatus.PRECISE_ANALYZED -> "Bereits genau analysiert"
                }
                Text(
                    text = "Status: ${'$'}currentStatusText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Divider()

                Text(
                    text = "Analysemethode",
                    style = MaterialTheme.typography.titleSmall
                )

                val availableMethods = character?.let { char ->
                    AnalysisMethod.entries.filter { method ->
                        when (method) {
                            AnalysisMethod.BY_SIGHT, AnalysisMethod.LABORATORY -> char.hasAlchemy
                            AnalysisMethod.ODEM_SPELL -> char.hasOdem
                            AnalysisMethod.ANALYS_SPELL -> char.hasAnalys
                        }
                    }
                } ?: emptyList()

                if (availableMethods.isEmpty()) {
                    Text(
                        text = "Charakter beherrscht keine Analysemethode",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    availableMethods.forEach { method ->
                        val methodText = when (method) {
                            AnalysisMethod.BY_SIGHT -> "Sichtprüfung (Alchimie)"
                            AnalysisMethod.LABORATORY -> "Laboranalyse (Alchimie)"
                            AnalysisMethod.ODEM_SPELL -> "ODEM ARCANUM (Zauberei)"
                            AnalysisMethod.ANALYS_SPELL -> "ANALYS ARKANSTRUKTUR (Zauberei)"
                        }

                        val skillValue = character?.let {
                            when (method) {
                                AnalysisMethod.BY_SIGHT, AnalysisMethod.LABORATORY -> it.alchemySkill
                                AnalysisMethod.ODEM_SPELL -> it.odemZfw
                                AnalysisMethod.ANALYS_SPELL -> it.analysZfw
                            }
                        } ?: 0

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedMethod == method,
                                onClick = { selectedMethod = method },
                                enabled = !isAnalyzing && analysisResult == null
                            )
                            Text(
                                text = "${'$'}methodText (TaW/ZfW: ${'$'}skillValue)",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }

                if (analysisResult == null && availableMethods.isNotEmpty()) {
                    Button(
                        onClick = {
                            character?.let { char ->
                                isAnalyzing = true
                                val result = PotionAnalyzer.analyzePotion(
                                    character = char,
                                    recipe = potionWithRecipe.recipe,
                                    method = selectedMethod,
                                    currentAnalysisStatus = potionWithRecipe.potion.analysisStatus
                                )
                                analysisResult = result
                                isAnalyzing = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isAnalyzing && character != null
                    ) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Analyse durchführen")
                        }
                    }
                }

                analysisResult?.let { result ->
                    Divider()

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (result.success) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.errorContainer
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (result.success) "Analyse erfolgreich!" else "Analyse fehlgeschlagen",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (result.success) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onErrorContainer
                                }
                            )

                            Text(
                                text = "Würfe: ${result.rolls.joinToString(", ")}",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Text(
                                text = "TaP*: ${result.tap}",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Text(
                                text = result.message,
                                style = MaterialTheme.typography.bodyMedium
                            )

                            if (result.recipeUnderstood) {
                                Text(
                                    text = "🎓 Rezept verstanden! (TaP* ≥ 19)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                val updatedPotion = potionWithRecipe.potion.copy(
                                    analysisStatus = result.newAnalysisStatus
                                )
                                viewModel.updatePotion(updatedPotion)

                                if (result.recipeUnderstood) {
                                    viewModel.setRecipeKnowledge(
                                        characterId = characterId,
                                        recipeId = potionWithRecipe.recipe.id,
                                        knowledgeLevel = RecipeKnowledgeLevel.UNDERSTOOD
                                    )
                                }

                                onAnalysisComplete()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Ergebnis speichern")
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (analysisResult == null) "Abbrechen" else "Schließen")
                }
            }
        }
    }
}

private fun getQualityLabel(quality: PotionQuality): String {
    return when (quality) {
        PotionQuality.A -> "A"
        PotionQuality.B -> "B"
        PotionQuality.C -> "C"
        PotionQuality.D -> "D"
        PotionQuality.E -> "E"
        PotionQuality.F -> "F"
        PotionQuality.M -> "M"
    }
}
