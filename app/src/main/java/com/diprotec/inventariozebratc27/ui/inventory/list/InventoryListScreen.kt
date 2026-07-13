package com.diprotec.inventariozebratc27.ui.inventory.list

import com.diprotec.inventariozebratc27.ui.theme.Dimens
import com.diprotec.inventariozebratc27.ui.components.AppActionButton
import com.diprotec.inventariozebratc27.ui.components.AppButtonStyle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.diprotec.inventariozebratc27.data.local.entity.InventoryItemEntity
import com.diprotec.inventariozebratc27.ui.theme.Background
import com.diprotec.inventariozebratc27.ui.theme.BorderGray
import com.diprotec.inventariozebratc27.ui.theme.ButtonRedDark
import com.diprotec.inventariozebratc27.ui.theme.TextPrimary
import com.diprotec.inventariozebratc27.ui.theme.White
import java.util.Locale

@Composable
fun InventoryListScreen(
    inventoryId: Long,
    viewModel: InventoryListViewModel = hiltViewModel()
) {
    val stateFlow = remember(inventoryId) {
        viewModel.uiState(inventoryId)
    }

    val uiState by stateFlow.collectAsState()

    var itemToDelete by remember {
        mutableStateOf<InventoryItemEntity?>(null)
    }

    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = {
                itemToDelete = null
            },
            title = {
                Text("Eliminar captura")
            },
            text = {
                Text(
                    text = "¿Desea eliminar la captura del producto ${item.barcode}?"
                )
            },
            confirmButton = {
                AppActionButton(
                    text = "Eliminar",
                    onClick = {
                        viewModel.deleteItem(
                            inventoryId = inventoryId,
                            itemId = item.id
                        )
                        itemToDelete = null
                    },
                    modifier = Modifier.testTag("btn_confirm_delete_inventory_item")
                )
            },
            dismissButton = {
                AppActionButton(
                    text = "Cancelar",
                    onClick = {
                        itemToDelete = null
                    },
                    modifier = Modifier.testTag("btn_cancel_delete_inventory_item"),
                    style = AppButtonStyle.OUTLINE
                )
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("inventory_list_screen")
    ) {
        InventoryHeader(
            title = "LISTADO DE CAPTURAS"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Background),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = Dimens.listContentWidth)
                    .padding(horizontal = Dimens.space20, vertical = Dimens.space18)
            ) {
                Text(
                    text = "Inventario: ${uiState.inventoryName}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.testTag("inventory_name")
                )

                if (!uiState.canDeleteItems && uiState.errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(Dimens.space10))

                    Text(
                        text = "Las capturas solo se pueden visualizar. La eliminación no está permitida para este inventario o perfil.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        modifier = Modifier.testTag("inventory_readonly_message")
                    )
                }

                if (!uiState.errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(Dimens.space10))

                    Text(
                        text = uiState.errorMessage.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ButtonRedDark,
                        modifier = Modifier.testTag("inventory_error_message")
                    )
                }

                Spacer(modifier = Modifier.height(Dimens.space18))

                InventoryGroupedSelector(
                    isGrouped = uiState.isGrouped,
                    onUngroupedClick = {
                        viewModel.setGrouped(false)
                    },
                    onGroupedClick = {
                        viewModel.setGrouped(true)
                    }
                )

                Spacer(modifier = Modifier.height(Dimens.space16))

                if (uiState.isGrouped) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .testTag("inventory_grouped_list"),
                        verticalArrangement = Arrangement.spacedBy(Dimens.space10)
                    ) {
                        items(uiState.groupedItems) { item ->
                            InventoryGroupedCard(
                                barcode = item.barcode,
                                description = item.description.orEmpty(),
                                quantity = item.totalQuantity,
                                unitMeasure = item.unitMeasure.orEmpty(),
                                ubicacionNombre = item.ubicacionNombre.orEmpty(),
                                totalRows = item.totalRows,
                                modifier = Modifier.testTag("inventory_grouped_item_${item.barcode}")
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .testTag("inventory_ungrouped_list"),
                        verticalArrangement = Arrangement.spacedBy(Dimens.space10)
                    ) {
                        items(
                            items = uiState.ungroupedItems,
                            key = { it.id }
                        ) { item ->
                            InventoryItemCard(
                                barcode = item.barcode,
                                description = item.description,
                                quantity = item.quantity,
                                unitMeasure = item.unitMeasure,
                                ubicacionNombre = item.ubicacionNombre,
                                canDelete = uiState.canDeleteItems,
                                onClick = {
                                    itemToDelete = item
                                },
                                modifier = Modifier.testTag("inventory_item_${item.id}")
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InventoryHeader(
    title: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .height(Dimens.buttonHeight)
            .padding(horizontal = Dimens.space16),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun InventoryGroupedSelector(
    isGrouped: Boolean,
    onUngroupedClick: () -> Unit,
    onGroupedClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimens.buttonHeight)
            .border(Dimens.borderWidth, BorderGray, RoundedCornerShape(Dimens.radiusPill))
            .clip(RoundedCornerShape(Dimens.radiusPill))
            .background(White),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SelectorOption(
            text = "Desagrupar",
            selected = !isGrouped,
            onClick = onUngroupedClick,
            modifier = Modifier
                .weight(1f)
                .testTag("selector_ungrouped")
        )

        SelectorOption(
            text = "Agrupar",
            selected = isGrouped,
            onClick = onGroupedClick,
            modifier = Modifier
                .weight(1f)
                .testTag("selector_grouped")
        )
    }
}

@Composable
private fun SelectorOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(Dimens.radiusPill))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    White
                }
            )
            .clickable {
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) {
                White
            } else {
                TextPrimary
            },
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun InventoryItemCard(
    barcode: String,
    description: String,
    quantity: Double,
    unitMeasure: String,
    ubicacionNombre: String,
    canDelete: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(Dimens.borderWidth, BorderGray, MaterialTheme.shapes.medium)
            .background(White, MaterialTheme.shapes.medium)
            .clickable(enabled = canDelete) {
                onClick()
            }
            .padding(Dimens.space16)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Dimens.space6)
        ) {
            Text(
                text = "Código: $barcode",
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = "Descripción: $description",
                color = TextPrimary
            )

            Text(
                text = "Cantidad: ${formatQuantity(quantity)}",
                color = TextPrimary
            )

            Text(
                text = "Unidad: $unitMeasure",
                color = TextPrimary
            )

            Text(
                text = "Ubicación: $ubicacionNombre",
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun InventoryGroupedCard(
    barcode: String,
    description: String,
    quantity: Double,
    unitMeasure: String,
    ubicacionNombre: String,
    totalRows: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(Dimens.borderWidth, BorderGray, MaterialTheme.shapes.medium)
            .background(White, MaterialTheme.shapes.medium)
            .padding(Dimens.space16)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Dimens.space6)
        ) {
            Text(
                text = "Código: $barcode",
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = "Descripción: $description",
                color = TextPrimary
            )

            Text(
                text = "Cantidad total: ${formatQuantity(quantity)}",
                color = TextPrimary
            )

            Text(
                text = "Unidad: $unitMeasure",
                color = TextPrimary
            )

            Text(
                text = "Ubicación: $ubicacionNombre",
                color = TextPrimary
            )

            Text(
                text = "Registros agrupados: $totalRows",
                color = TextPrimary
            )
        }
    }
}

private fun formatQuantity(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format(Locale.US, "%.3f", value)
            .trimEnd('0')
            .trimEnd('.')
    }
}
