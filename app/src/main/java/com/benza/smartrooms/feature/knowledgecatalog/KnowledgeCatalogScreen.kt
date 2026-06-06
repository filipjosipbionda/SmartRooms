package com.benza.smartrooms.feature.knowledgecatalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.benza.smartrooms.R
import com.benza.smartrooms.data.knowledge.KnowledgeCatalog
import com.benza.smartrooms.data.knowledge.KnowledgeCatalogItem
import com.benza.smartrooms.data.room.model.QuizKind
import com.benza.smartrooms.ui.theme.SmartRoomsTheme

@Composable
internal fun KnowledgeCatalogRouteScreen(
    cefrLevel: String,
    quizKindValue: String,
    selectedCatalogItemIds: String,
    onBackClick: () -> Unit,
    onCatalogItemsSelected: (String) -> Unit,
) {
    val quizKind = quizKindValue.toQuizKind()
    KnowledgeCatalogScreen(
        cefrLevel = cefrLevel,
        quizKind = quizKind,
        selectedCatalogItemIds = selectedCatalogItemIds,
        onBackClick = onBackClick,
        onCatalogItemsSelected = onCatalogItemsSelected,
    )
}

@Composable
private fun KnowledgeCatalogScreen(
    cefrLevel: String,
    quizKind: QuizKind,
    selectedCatalogItemIds: String,
    onBackClick: () -> Unit,
    onCatalogItemsSelected: (String) -> Unit,
) {
    val catalogItems = KnowledgeCatalog.itemsFor(cefrLevel, quizKind)
    val catalogItemIds = remember(catalogItems) { catalogItems.map(KnowledgeCatalogItem::id).toSet() }
    val groupedItems = catalogItems.groupBy(KnowledgeCatalogItem::category)
    var selectedIds by remember(selectedCatalogItemIds, catalogItemIds) {
        mutableStateOf(selectedCatalogItemIds.toCatalogItemIdSet().intersect(catalogItemIds))
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (catalogItems.isNotEmpty()) {
                KnowledgeCatalogBottomBar(
                    selectedCount = selectedIds.size,
                    onUseSelectedClick = {
                        onCatalogItemsSelected(selectedIds.toCatalogItemRouteValue())
                    },
                )
            }
        },
    ) { paddingValues ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                KnowledgeCatalogTopBar(
                    cefrLevel = cefrLevel,
                    quizKind = quizKind,
                    onBackClick = onBackClick,
                )
            }
            if (catalogItems.isEmpty()) {
                item {
                    EmptyCatalogCard()
                }
            } else {
                groupedItems.forEach { (category, items) ->
                    item {
                        Text(
                            text = category,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    items(
                        items = items,
                        key = KnowledgeCatalogItem::id,
                    ) { item ->
                        KnowledgeCatalogItemCard(
                            item = item,
                            isSelected = item.id in selectedIds,
                            onClick = {
                                selectedIds =
                                    if (item.id in selectedIds) {
                                        selectedIds - item.id
                                    } else {
                                        selectedIds + item.id
                                    }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KnowledgeCatalogTopBar(
    cefrLevel: String,
    quizKind: QuizKind,
    onBackClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.action_back),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = stringResource(R.string.knowledge_catalog_title),
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text =
                    stringResource(
                        R.string.knowledge_catalog_subtitle,
                        cefrLevel.ifBlank { stringResource(R.string.room_detail_level_not_set) },
                        stringResource(quizKind.labelRes()),
                    ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun KnowledgeCatalogBottomBar(
    selectedCount: Int,
    onUseSelectedClick: () -> Unit,
) {
    Surface(
        tonalElevation = 3.dp,
        shadowElevation = 3.dp,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text =
                    pluralStringResource(
                        R.plurals.room_quizzes_catalog_unit_count,
                        selectedCount,
                        selectedCount,
                    ),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Button(
                onClick = onUseSelectedClick,
                enabled = selectedCount > 0,
            ) {
                Text(
                    text = stringResource(R.string.room_quizzes_catalog_use_selected),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun KnowledgeCatalogItemCard(
    item: KnowledgeCatalogItem,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = item.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.quizKind == QuizKind.VOCABULARY && item.suggestedVocabularyWords.isNotEmpty()) {
                    Text(
                        text =
                            pluralStringResource(
                                R.plurals.room_quizzes_catalog_word_count,
                                item.suggestedVocabularyWords.size,
                                item.suggestedVocabularyWords.size,
                            ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() },
            )
        }
    }
}

@Composable
private fun EmptyCatalogCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Text(
            text = stringResource(R.string.knowledge_catalog_empty),
            modifier = Modifier.padding(18.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun String.toQuizKind(): QuizKind =
    when (lowercase()) {
        "vocabulary" -> QuizKind.VOCABULARY
        else -> QuizKind.GRAMMAR
    }

private fun QuizKind.labelRes(): Int =
    when (this) {
        QuizKind.GRAMMAR -> R.string.room_quiz_kind_grammar
        QuizKind.VOCABULARY -> R.string.room_quiz_kind_vocabulary
    }

private fun String.toCatalogItemIdSet(): Set<String> =
    split(CATALOG_ITEM_ID_SEPARATOR)
        .map(String::trim)
        .filter(String::isNotBlank)
        .toSet()

private fun Set<String>.toCatalogItemRouteValue(): String = joinToString(separator = CATALOG_ITEM_ID_SEPARATOR)

private const val CATALOG_ITEM_ID_SEPARATOR = ","

@Preview(showBackground = true)
@Composable
private fun KnowledgeCatalogScreenPreview() {
    SmartRoomsTheme {
        KnowledgeCatalogScreen(
            cefrLevel = "B1",
            quizKind = QuizKind.GRAMMAR,
            selectedCatalogItemIds = "",
            onBackClick = {},
            onCatalogItemsSelected = {},
        )
    }
}
