package com.diprotec.inventariozebratc27.ui.inventory.pending

import com.diprotec.inventariozebratc27.ui.theme.Dimens

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.diprotec.inventariozebratc27.data.local.entity.InventoryEntity
import com.diprotec.inventariozebratc27.data.local.inventory.InventoryStatus
import com.diprotec.inventariozebratc27.ui.theme.Background
import com.diprotec.inventariozebratc27.ui.theme.BorderGray
import com.diprotec.inventariozebratc27.ui.theme.TextPrimary
import com.diprotec.inventariozebratc27.ui.theme.White

@Composable
fun PendingInventoriesScreen(
    onBack: () -> Unit,
    onOpenPendingInventory: (Long, Int) -> Unit,
    onOpenFinishedInventory: (Long) -> Unit,
    viewModel: PendingInventoriesViewModel = hiltViewModel()
) {
    val items by viewModel.inventories.collectAsState()
    val filter by viewModel.filter.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        PendingInventoriesHeader(
            title = "LISTADO DE INVENTARIOS"
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
                PendingFinishedSelector(
                    filter = filter,
                    onPendingClick = {
                        viewModel.setFilter(PendingInventoryFilter.PENDING)
                    },
                    onFinishedClick = {
                        viewModel.setFilter(PendingInventoryFilter.FINISHED)
                    }
                )

                Spacer(modifier = Modifier.height(Dimens.space16))

                if (items.isEmpty()) {
                    EmptyInventoryListMessage(
                        filter = filter
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(Dimens.space10)
                    ) {
                        items(
                            items = items,
                            key = { it.id }
                        ) { item ->
                            InventoryCard(
                                item = item,
                                onClick = {
                                    if (item.status == InventoryStatus.FINISHED.name) {
                                        onOpenFinishedInventory(item.id)
                                    } else {
                                        onOpenPendingInventory(
                                            item.id,
                                            item.tipoLectura
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingInventoriesHeader(
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
private fun PendingFinishedSelector(
    filter: PendingInventoryFilter,
    onPendingClick: () -> Unit,
    onFinishedClick: () -> Unit
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
            text = "Pendientes",
            selected = filter == PendingInventoryFilter.PENDING,
            onClick = onPendingClick,
            modifier = Modifier.weight(1f)
        )

        SelectorOption(
            text = "Finalizados",
            selected = filter == PendingInventoryFilter.FINISHED,
            onClick = onFinishedClick,
            modifier = Modifier.weight(1f)
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
private fun EmptyInventoryListMessage(
    filter: PendingInventoryFilter
) {
    val message = when (filter) {
        PendingInventoryFilter.PENDING -> "No hay inventarios pendientes."
        PendingInventoryFilter.FINISHED -> "No hay inventarios finalizados."
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Dimens.space28),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = TextPrimary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun InventoryCard(
    item: InventoryEntity,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(Dimens.borderWidth, BorderGray, MaterialTheme.shapes.medium)
            .background(White, MaterialTheme.shapes.medium)
            .clickable {
                onClick()
            }
            .padding(Dimens.space16)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Dimens.space6)
        ) {
            Text(
                text = item.name,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Inicio: ${item.desde.orEmpty()} ${item.hora.orEmpty()}",
                color = TextPrimary
            )

            Text(
                text = "Termino: ${item.hasta.orEmpty()} ${item.hora.orEmpty()}",
                color = TextPrimary
            )

            Text(
                text = "Estado: ${formatInventoryStatus(item.status)}",
                color = TextPrimary
            )
        }
    }
}

private fun formatInventoryStatus(status: String): String {
    return when (status) {
        InventoryStatus.PENDING.name -> "Pendiente"
        InventoryStatus.FINISHED.name -> "Finalizado"
        InventoryStatus.EXPIRED.name -> "Expirado"
        InventoryStatus.IN_PROGRESS.name -> "En progreso"
        else -> status
    }
}