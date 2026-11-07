package com.example.teste.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.teste.data.model.YearMonthData
import java.util.Locale

/**
 * Modal do Calendário Interativo.
 * Calcula a quantidade de cargas agendadas no banco para cada data, destaca visualmente os dias com cargas
 * e permite filtrar a lista principal ao clicar em "Selecionar e filtrar".
 */
@Composable
fun CalendarioModalDialog(
    datasComCargaMap: Map<String, Int>,
    filtroAtivoAtual: String?,
    onDismiss: () -> Unit,
    onSelecionarEFiltrar: (data: String) -> Unit
) {
    val initialDateStr = remember(filtroAtivoAtual, datasComCargaMap) {
        filtroAtivoAtual ?: datasComCargaMap.keys.minByOrNull { dateStr ->
            val p = dateStr.split("/")
            val d = p.getOrNull(0)?.toIntOrNull() ?: 0
            val m = p.getOrNull(1)?.toIntOrNull() ?: 0
            val y = p.getOrNull(2)?.toIntOrNull() ?: 0
            y * 10000 + m * 100 + d
        } ?: "30/03/2026"
    }

    val initialYearMonth = remember(initialDateStr) {
        val parts = initialDateStr.split("/")
        val mes = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(1, 12) ?: 3
        val ano = parts.getOrNull(2)?.toIntOrNull() ?: 2026
        YearMonthData(ano, mes)
    }

    var currentYearMonth by remember { mutableStateOf(initialYearMonth) }
    var selectedDateStr by remember { mutableStateOf(initialDateStr) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth().padding(4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Título e Botão Fechar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Calendário de Cargas",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF003E8A)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = Color(0xFF6B7280)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Controles do Mês (< Março 2026 >)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { currentYearMonth = currentYearMonth.minusMonths(1) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Mês anterior",
                            tint = Color(0xFF003E8A)
                        )
                    }

                    val nomeMes = when (currentYearMonth.month) {
                        1 -> "Janeiro"
                        2 -> "Fevereiro"
                        3 -> "Março"
                        4 -> "Abril"
                        5 -> "Maio"
                        6 -> "Junho"
                        7 -> "Julho"
                        8 -> "Agosto"
                        9 -> "Setembro"
                        10 -> "Outubro"
                        11 -> "Novembro"
                        else -> "Dezembro"
                    }

                    Text(
                        text = "$nomeMes ${currentYearMonth.year}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    IconButton(onClick = { currentYearMonth = currentYearMonth.plusMonths(1) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Próximo mês",
                            tint = Color(0xFF003E8A)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Cabeçalho Dias da Semana
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    val diasSemana = listOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb")
                    diasSemana.forEach { dia ->
                        Text(
                            text = dia,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6B7280),
                            modifier = Modifier.width(36.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Grid do Mês
                val firstDayOfWeek = currentYearMonth.firstDayOfWeekIndex()
                val daysInMonth = currentYearMonth.lengthOfMonth()
                val totalCells = firstDayOfWeek + daysInMonth
                val rows = (totalCells + 6) / 7

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (row in 0 until rows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            for (col in 0 until 7) {
                                val dayIndex = row * 7 + col
                                val dayNum = dayIndex - firstDayOfWeek + 1

                                if (dayNum in 1..daysInMonth) {
                                    val formattedDateStr = String.format(
                                        Locale.getDefault(),
                                        "%02d/%02d/%04d",
                                        dayNum,
                                        currentYearMonth.month,
                                        currentYearMonth.year
                                    )
                                    val qtdCargas = datasComCargaMap[formattedDateStr] ?: 0
                                    val isSelected = formattedDateStr == selectedDateStr

                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isSelected -> Color(0xFF003E8A)
                                                    qtdCargas > 0 -> Color(0xFFE8F1FF)
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .clickable {
                                                selectedDateStr = formattedDateStr
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = dayNum.toString(),
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected || qtdCargas > 0) FontWeight.Bold else FontWeight.Normal,
                                                color = when {
                                                    isSelected -> Color.White
                                                    qtdCargas > 0 -> Color(0xFF0052CC)
                                                    else -> Color(0xFF0F172A)
                                                }
                                            )

                                            if (qtdCargas > 0 && !isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF0052CC))
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.size(38.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Painel da Data Selecionada e Botão Selecionar e Filtrar
                val cargasNoDia = datasComCargaMap[selectedDateStr] ?: 0
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (cargasNoDia > 0) Color(0xFFE8F1FF) else Color(0xFFF1F5F9),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Data: $selectedDateStr",
                                fontSize = 12.sp,
                                color = Color(0xFF6B7280)
                            )
                            Text(
                                text = if (cargasNoDia > 0) "$cargasNoDia carga(s) agendada(s)" else "Nenhuma carga agendada nesta data",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (cargasNoDia > 0) Color(0xFF0052CC) else Color(0xFF6B7280)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = { onSelecionarEFiltrar(selectedDateStr) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003E8A))
                        ) {
                            Text("Selecionar e filtrar", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
