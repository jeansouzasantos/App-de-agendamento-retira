package com.example.teste.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.teste.data.model.AgendamentoComCarga

/**
 * Tela da Aba 1: Agendamentos.
 * Exibe o cabeçalho azul, filtro ativo por data, lista de cartões de agendamento
 * e botão flutuante para cadastrar nova carga.
 */
@Composable
fun AgendamentosScreen(
    agendamentos: List<AgendamentoComCarga>,
    filtroData: String?,
    onCalendarClick: () -> Unit,
    onLimparFiltro: () -> Unit,
    onAgendamentoClick: (id: Long) -> Unit,
    onNovoCadastroClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF2F5FA))
        ) {
            // Cabeçalho Azul com Ícone de Calendário
            HeaderSection(onCalendarClick = onCalendarClick)

            // Banner do Filtro por Data (se ativo)
            if (filtroData != null) {
                Surface(
                    color = Color(0xFFE8F1FF),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                tint = Color(0xFF0052CC),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Filtrado por: $filtroData",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0052CC)
                            )
                        }

                        TextButton(onClick = onLimparFiltro) {
                            Text(
                                text = "Limpar filtro",
                                fontSize = 12.sp,
                                color = Color(0xFF0052CC),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Lista de Cartões de Agendamento
            if (agendamentos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (filtroData != null) "Nenhum agendamento encontrado para esta data." else "Nenhum agendamento cadastrado.",
                        color = Color(0xFF6B7280),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(agendamentos, key = { it.agendamento.id }) { item ->
                        AgendamentoCard(
                            item = item,
                            onClick = { onAgendamentoClick(item.agendamento.id) }
                        )
                    }
                }
            }
        }

        // Botão Flutuante (FAB) para cadastrar nova carga do zero
        FloatingActionButton(
            onClick = onNovoCadastroClick,
            containerColor = Color(0xFF003E8A),
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Cadastrar Carga")
        }
    }
}

@Composable
private fun HeaderSection(onCalendarClick: () -> Unit) {
    val primaryNavy = Color(0xFF003E8A)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(primaryNavy)
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocalShipping,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Retira de Carga",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Resumo de agendamentos",
                        color = Color(0xFFB0CEFF),
                        fontSize = 14.sp
                    )
                }
            }

            IconButton(onClick = onCalendarClick) {
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = "Abrir Calendário",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun AgendamentoCard(
    item: AgendamentoComCarga,
    onClick: () -> Unit
) {
    val ag = item.agendamento
    val carga = item.carga

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        color = Color(0xFF0052CC)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Inventory2,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Nome:",
                            color = Color(0xFF6B7280),
                            fontSize = 13.sp
                        )
                        Text(
                            text = carga.cliente,
                            color = Color(0xFF0F172A),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Horário
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFE8F1FF)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = Color(0xFF0052CC),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = ag.horaAgendada,
                            color = Color(0xFF0052CC),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF0F3F7), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "AWB:",
                            color = Color(0xFF6B7280),
                            fontSize = 13.sp,
                            modifier = Modifier.width(50.dp)
                        )
                        Text(
                            text = carga.awb,
                            color = Color(0xFF0F172A),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Data:",
                            color = Color(0xFF6B7280),
                            fontSize = 13.sp,
                            modifier = Modifier.width(50.dp)
                        )
                        Text(
                            text = ag.dataAgendada,
                            color = Color(0xFF0052CC),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Badge do Status Atual
                    val isFinalizado = ag.status == "ENTREGUE" || ag.status == "CARGA NÃO LOCALIZADA" || ag.status == "NÃO COMPARECEU"
                    val isEntregue = ag.status == "ENTREGUE"

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = when {
                            isEntregue -> Color(0xFFDCFCE7)
                            ag.status == "CARGA NÃO LOCALIZADA" -> Color(0xFFFEE2E2)
                            ag.status == "NÃO COMPARECEU" -> Color(0xFFFEF3C7)
                            ag.tentativaNumero > 1 -> Color(0xFFE0F2FE)
                            else -> Color(0xFFE8F1FF)
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        val textoStatus = if (ag.tentativaNumero > 1 && !isFinalizado) {
                            "REAGENDADO (${ag.tentativaNumero}ª)"
                        } else {
                            ag.status
                        }

                        Text(
                            text = textoStatus,
                            color = when {
                                isEntregue -> Color(0xFF166534)
                                ag.status == "CARGA NÃO LOCALIZADA" -> Color(0xFF991B1B)
                                ag.status == "NÃO COMPARECEU" -> Color(0xFF92400E)
                                else -> Color(0xFF0369A1)
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = "Detalhes",
                        tint = Color(0xFF6B7280),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
