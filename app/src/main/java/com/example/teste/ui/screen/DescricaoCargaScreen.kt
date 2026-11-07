package com.example.teste.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.teste.data.model.AgendamentoComCarga
import com.example.teste.data.model.HistoricoTentativa

/**
 * Tela de Detalhes da Carga.
 * Apresenta as especificações da Carga, o status da tentativa atual, os botões de ação (ENTREGUE, CARGA NÃO LOCALIZADA, NÃO COMPARECEU),
 * o botão "Novo agendamento" (liberado SOMENTE após NÃO COMPARECEU na tentativa mais recente) e o histórico completo por AWB.
 */
@Composable
fun DescricaoCargaScreen(
    item: AgendamentoComCarga,
    historico: List<HistoricoTentativa>,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onBack: () -> Unit,
    onRegistrarResultadoClick: (resultado: String) -> Unit,
    onNovoAgendamentoClick: () -> Unit
) {
    val ag = item.agendamento
    val carga = item.carga
    val ocorrencia = item.ocorrencia
    val primaryNavy = Color(0xFF003E8A)

    val isFinalizado = ag.status == "ENTREGUE" || ag.status == "CARGA NÃO LOCALIZADA" || ag.status == "NÃO COMPARECEU"
    val maxTentativaDaCarga = historico.maxOfOrNull { it.tentativaNumero } ?: ag.tentativaNumero
    val permiteReagendamento = ag.status == "NÃO COMPARECEU" && ag.tentativaNumero >= maxTentativaDaCarga

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(primaryNavy)
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Detalhes da Carga (AWB: ${carga.awb})",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF2F5FA))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cartão 1: Cliente, AWB e Tentativa Atual
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Cliente / Empresa", color = Color(0xFF6B7280), fontSize = 13.sp)
                    Text(
                        text = carga.cliente,
                        color = Color(0xFF0F172A),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Agendamento: ${ag.dataAgendada} às ${ag.horaAgendada}",
                            color = Color(0xFF0052CC),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF1F5F9)
                        ) {
                            Text(
                                text = "${ag.tentativaNumero}ª Tentativa",
                                color = Color(0xFF334155),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val textoStatus = if (ag.tentativaNumero > 1 && !isFinalizado) {
                        "REAGENDADO (${ag.tentativaNumero}ª)"
                    } else {
                        ag.status
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = when {
                            ag.status == "ENTREGUE" -> Color(0xFFDCFCE7)
                            ag.status == "CARGA NÃO LOCALIZADA" -> Color(0xFFFEE2E2)
                            ag.status == "NÃO COMPARECEU" -> Color(0xFFFEF3C7)
                            ag.tentativaNumero > 1 -> Color(0xFFE0F2FE)
                            else -> Color(0xFFE8F1FF)
                        }
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = when (ag.status) {
                                        "ENTREGUE" -> Icons.Default.CheckCircle
                                        "CARGA NÃO LOCALIZADA" -> Icons.Default.Cancel
                                        "NÃO COMPARECEU" -> Icons.Outlined.Inventory2
                                        else -> Icons.Default.Schedule
                                    },
                                    contentDescription = null,
                                    tint = when (ag.status) {
                                        "ENTREGUE" -> Color(0xFF166534)
                                        "CARGA NÃO LOCALIZADA" -> Color(0xFF991B1B)
                                        "NÃO COMPARECEU" -> Color(0xFF92400E)
                                        else -> Color(0xFF0369A1)
                                    },
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Status Atual: $textoStatus",
                                    color = when (ag.status) {
                                        "ENTREGUE" -> Color(0xFF166534)
                                        "CARGA NÃO LOCALIZADA" -> Color(0xFF991B1B)
                                        "NÃO COMPARECEU" -> Color(0xFF92400E)
                                        else -> Color(0xFF0369A1)
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (!ocorrencia?.observacao.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Observação: ${ocorrencia?.observacao}",
                                    color = Color(0xFF334155),
                                    fontSize = 12.sp
                                )
                            }

                            if (!ag.dataFinalizacao.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Finalizado em: ${ag.dataFinalizacao}",
                                    color = Color(0xFF6B7280),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            // Cartão 2: Especificações da Carga
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Especificações da Carga",
                        color = primaryNavy,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    HorizontalDivider(color = Color(0xFFF0F3F7))

                    DetailRow(icon = Icons.Outlined.Inventory2, label = "AWB", value = carga.awb)
                    DetailRow(icon = Icons.Outlined.Inventory2, label = "Volumes", value = carga.volumes)
                    DetailRow(icon = Icons.Outlined.Scale, label = "Peso", value = carga.peso)

                    DetailRow(
                        icon = Icons.Outlined.Speed,
                        label = "Nível de Serviço",
                        value = carga.servico,
                        badgeColor = when (carga.servico) {
                            "Expresso" -> Color(0xFFFFF7ED)
                            "Premium" -> Color(0xFFFAF5FF)
                            else -> Color(0xFFF0FDF4)
                        },
                        textColor = when (carga.servico) {
                            "Expresso" -> Color(0xFFC2410C)
                            "Premium" -> Color(0xFF6B21A8)
                            else -> Color(0xFF15803D)
                        }
                    )

                    DetailRow(
                        icon = Icons.Outlined.Category,
                        label = "Tipo de Carga",
                        value = carga.tipoCarga,
                        badgeColor = when (carga.tipoCarga) {
                            "Perecível" -> Color(0xFFFEF2F2)
                            "DGR" -> Color(0xFFFFF1F2)
                            "Valor" -> Color(0xFFFEF3C7)
                            "Vulnerável" -> Color(0xFFE0F2FE)
                            else -> Color(0xFFF1F5F9)
                        },
                        textColor = when (carga.tipoCarga) {
                            "Perecível" -> Color(0xFFB91C1C)
                            "DGR" -> Color(0xFFBE123C)
                            "Valor" -> Color(0xFFB45309)
                            "Vulnerável" -> Color(0xFF0369A1)
                            else -> Color(0xFF334155)
                        }
                    )
                }
            }

            // Cartão 3: Ações de Finalização de Ocorrência (se a tentativa estiver pendente)
            if (!isFinalizado) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Registrar Resultado da Tentativa",
                            color = primaryNavy,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )

                        HorizontalDivider(color = Color(0xFFF0F3F7))

                        Button(
                            onClick = { onRegistrarResultadoClick("ENTREGUE") },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                        ) {
                            Text("ENTREGUE", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { onRegistrarResultadoClick("CARGA NÃO LOCALIZADA") },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFDC2626))
                        ) {
                            Text("CARGA NÃO LOCALIZADA", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { onRegistrarResultadoClick("NÃO COMPARECEU") },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD97706)),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFD97706))
                        ) {
                            Text("NÃO COMPARECEU", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Botão "Novo Agendamento" - Liberado SOMENTE para NÃO COMPARECEU na tentativa mais recente
            if (permiteReagendamento) {
                Button(
                    onClick = onNovoAgendamentoClick,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003E8A))
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("NOVO AGENDAMENTO", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Cartão 4: Histórico Completo da Carga / AWB
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = null,
                            tint = primaryNavy,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Histórico Completo por AWB",
                            color = primaryNavy,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    HorizontalDivider(color = Color(0xFFF0F3F7))

                    if (historico.isEmpty()) {
                        Text(
                            text = "Nenhum histórico registrado.",
                            color = Color(0xFF6B7280),
                            fontSize = 13.sp
                        )
                    } else {
                        historico.forEach { h ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF8FAFC), shape = RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${h.tentativaNumero}ª Tentativa",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "${h.dataAgendada} ${h.horaAgendada}",
                                        fontSize = 12.sp,
                                        color = Color(0xFF6B7280)
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "Status: ${h.resultadoStatus}",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = when (h.resultadoStatus) {
                                        "ENTREGUE" -> Color(0xFF166534)
                                        "CARGA NÃO LOCALIZADA" -> Color(0xFF991B1B)
                                        "NÃO COMPARECEU" -> Color(0xFF92400E)
                                        else -> Color(0xFF0369A1)
                                    }
                                )

                                if (!h.observacao.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Obs: ${h.observacao}",
                                        fontSize = 12.sp,
                                        color = Color(0xFF334155)
                                    )
                                }

                                if (!h.dataFinalizacao.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Finalizado em: ${h.dataFinalizacao}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF6B7280)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    badgeColor: Color? = null,
    textColor: Color? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF6B7280),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = label, color = Color(0xFF6B7280), fontSize = 13.sp)
        }

        if (badgeColor != null && textColor != null) {
            Surface(shape = RoundedCornerShape(8.dp), color = badgeColor) {
                Text(
                    text = value,
                    color = textColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        } else {
            Text(
                text = value,
                color = Color(0xFF0F172A),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
