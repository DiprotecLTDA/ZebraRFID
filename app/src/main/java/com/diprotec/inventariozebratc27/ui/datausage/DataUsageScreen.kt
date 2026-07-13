package com.diprotec.inventariozebratc27.ui.datausage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.diprotec.inventariozebratc27.data.local.dao.NetworkUsageGroupRow
import com.diprotec.inventariozebratc27.ui.common.AppFloatingMessage
import com.diprotec.inventariozebratc27.ui.theme.Background
import com.diprotec.inventariozebratc27.ui.theme.BorderGray
import com.diprotec.inventariozebratc27.ui.theme.ButtonRed
import com.diprotec.inventariozebratc27.ui.theme.InventoryMenuButton
import com.diprotec.inventariozebratc27.ui.theme.TextPrimary
import com.diprotec.inventariozebratc27.ui.theme.White

@Composable
fun DataUsageScreen(
    onBack: () -> Unit,
    viewModel: DataUsageViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(state.message) {
        state.message?.let { message ->
            AppFloatingMessage.info(message)
            viewModel.clearMessage()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 520.dp)
                .padding(20.dp)
        ) {
            Text(
                text = "Consumo de datos",
                color = TextPrimary,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.padding(6.dp))

            if (state.loading) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()

                    Spacer(modifier = Modifier.padding(6.dp))

                    Text(
                        text = "Cargando consumo...",
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SummaryCard(
                            title = "Hoy",
                            value = state.today,
                            modifier = Modifier.weight(1f)
                        )

                        SummaryCard(
                            title = "Últimos 7 días",
                            value = state.last7Days,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SummaryCard(
                            title = "Llamadas hoy",
                            value = state.todayCalls.toString(),
                            modifier = Modifier.weight(1f)
                        )

                        SummaryCard(
                            title = "Promedio llamada",
                            value = state.averagePerCall,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    UsageGroupCard(
                        title = "Consumo por origen",
                        rows = state.bySource,
                        formatBytes = viewModel::formatBytes
                    )

                    UsageGroupCard(
                        title = "Consumo por operación",
                        rows = state.byOperation,
                        formatBytes = viewModel::formatBytes
                    )

                    UsageGroupCard(
                        title = "Consumo por endpoint",
                        rows = state.byEndpoint,
                        formatBytes = viewModel::formatBytes
                    )

                    Spacer(modifier = Modifier.padding(4.dp))
                }
            }

            Spacer(modifier = Modifier.padding(8.dp))

            Button(
                onClick = {
                    viewModel.refresh()
                },
                enabled = !state.loading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ButtonRed
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null
                )

                Spacer(modifier = Modifier.padding(3.dp))

                Text("Actualizar")
            }

            Spacer(modifier = Modifier.padding(4.dp))

            Button(
                onClick = {
                    viewModel.clearLogs()
                },
                enabled = !state.loading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ButtonRed
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null
                )

                Spacer(modifier = Modifier.padding(4.dp))

                Text("Limpiar registros")
            }

            Spacer(modifier = Modifier.padding(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Top
            ) {
                InventoryMenuButton(
                    text = "Volver",
                    icon = Icons.Default.ArrowBack,
                    onClick = onBack
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                color = White,
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.dp,
                color = BorderGray,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Text(
            text = title,
            color = TextPrimary,
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.padding(4.dp))

        Text(
            text = value,
            color = TextPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun UsageGroupCard(
    title: String,
    rows: List<NetworkUsageGroupRow>,
    formatBytes: (Long) -> String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = White,
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.dp,
                color = BorderGray,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Text(
            text = title,
            color = TextPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.padding(6.dp))

        if (rows.isEmpty()) {
            Text(
                text = "Sin registros",
                color = TextPrimary,
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            rows.forEachIndexed { index, row ->
                UsageRow(
                    row = row,
                    formatBytes = formatBytes
                )

                if (index < rows.lastIndex) {
                    Divider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = BorderGray
                    )
                }
            }
        }
    }
}

@Composable
private fun UsageRow(
    row: NetworkUsageGroupRow,
    formatBytes: (Long) -> String
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = row.name,
            color = TextPrimary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.padding(2.dp))

        Text(
            text = "${formatBytes(row.totalBytes)} - ${row.callCount} llamadas",
            color = TextPrimary,
            style = MaterialTheme.typography.bodySmall
        )
    }
}