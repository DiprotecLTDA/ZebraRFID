package com.diprotec.inventariozebratc27.ui.inventory.pending

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
                    .widthIn(max = 560.dp)
                    .padding(horizontal = 20.dp, vertical = 18.dp)
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

                Spacer(modifier = Modifier.height(16.dp))

                if (items.isEmpty()) {
                    EmptyInventoryListMessage(
                        filter = filter
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
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
            .height(56.dp)
            .padding(horizontal = 16.dp),
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
            .height(56.dp)
            .border(1.dp, BorderGray, RoundedCornerShape(50.dp))
            .clip(RoundedCornerShape(50.dp))
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
            .clip(RoundedCornerShape(50.dp))
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
            .padding(top = 28.dp),
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
            .border(1.dp, BorderGray, RoundedCornerShape(16.dp))
            .background(White, RoundedCornerShape(16.dp))
            .clickable {
                onClick()
            }
            .padding(16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
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