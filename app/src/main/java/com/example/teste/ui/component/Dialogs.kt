package com.example.teste.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.teste.ui.viewmodel.RetiraDeCargaViewModel

/**
 * Janela de Confirmação com Observação Obrigatória.
 * Atende às especificações: validação de observação vazia/com espaços, operação atômica e botões Confirmar/Cancelar.
 */
@Composable
fun ConfirmacaoOcorrenciaDialog(
    resultado: String, // "ENTREGUE", "CARGA NÃO LOCALIZADA", "NÃO COMPARECEU"
    onDismiss: () -> Unit,
    onConfirmar: (observacao: String) -> Unit
) {
    var observacao by rememberSaveable { mutableStateOf("") }
    var erroValidacao by remember { mutableStateOf<String?>(null) }

    val corResultado = when (resultado) {
        "ENTREGUE" -> Color(0xFF16A34A)
        "CARGA NÃO LOCALIZADA" -> Color(0xFFDC2626)
        else -> Color(0xFFD97706) // NÃO COMPARECEU
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth().padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "CONFIRMAÇÃO - $resultado",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = corResultado
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Por favor, informe uma observação obrigatória sobre esta ocorrência para concluir o registro.",
                    fontSize = 13.sp,
                    color = Color(0xFF6B7280)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Campo de Observação Obrigatória
                OutlinedTextField(
                    value = observacao,
                    onValueChange = {
                        observacao = it
                        if (it.trim().isNotEmpty()) erroValidacao = null
                    },
                    label = { Text("Observação Obrigatória *") },
                    isError = erroValidacao != null,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp)
                )

                if (erroValidacao != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = erroValidacao!!,
                        color = Color(0xFFDC2626),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Start)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Botões de Ação: Confirmar e Cancelar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancelar")
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = {
                            if (observacao.trim().isEmpty()) {
                                erroValidacao = "A observação é obrigatória e não pode conter apenas espaços."
                            } else {
                                onConfirmar(observacao.trim())
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = corResultado)
                    ) {
                        Text("Confirmar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Formulário de Reagendamento de Carga (Novo Agendamento para a mesma AWB).
 */
@Composable
fun NovoAgendamentoDialog(
    awb: String,
    tentativaAtual: Int,
    onDismiss: () -> Unit,
    onConfirmarReagendamento: (novaData: String, novoHorario: String) -> Unit
) {
    var novaData by rememberSaveable { mutableStateOf("02/04/2026") }
    var novoHorario by rememberSaveable { mutableStateOf("10:00") }
    var erroMsg by remember { mutableStateOf<String?>(null) }

    val proximaTentativa = tentativaAtual + 1

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth().padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text(
                    text = "NOVO AGENDAMENTO (${proximaTentativa}ª Tentativa)",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF003E8A)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "AWB: $awb - Reutilizando cadastro original",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0052CC)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Campo Nova Data
                OutlinedTextField(
                    value = novaData,
                    onValueChange = {
                        novaData = it
                        erroMsg = null
                    },
                    label = { Text("Nova Data (DD/MM/AAAA) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Campo Novo Horário
                OutlinedTextField(
                    value = novoHorario,
                    onValueChange = {
                        novoHorario = it
                        erroMsg = null
                    },
                    label = { Text("Novo Horário (HH:mm) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (erroMsg != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = erroMsg!!, color = Color(0xFFDC2626), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancelar")
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = {
                            val dataValida = RetiraDeCargaViewModel.normalizarEValidarData(novaData)
                            val horaValida = RetiraDeCargaViewModel.normalizarEValidarHora(novoHorario)
                            when {
                                dataValida == null -> erroMsg = "Informe uma data válida no formato DD/MM/AAAA."
                                horaValida == null -> erroMsg = "Informe um horário válido no formato HH:mm."
                                else -> onConfirmarReagendamento(dataValida, horaValida)
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003E8A))
                    ) {
                        Text("Confirmar Reagendamento", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Diálogo de Cadastro de Nova Carga e Primeiro Agendamento.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovoCadastroCargaDialog(
    onDismiss: () -> Unit,
    onCadastrar: (
        awb: String,
        cliente: String,
        volumes: String,
        peso: String,
        servico: String,
        tipoCarga: String,
        data: String,
        hora: String
    ) -> Unit
) {
    var awb by rememberSaveable { mutableStateOf("") }
    var cliente by rememberSaveable { mutableStateOf("") }
    var volumes by rememberSaveable { mutableStateOf("10 volumes") }
    var peso by rememberSaveable { mutableStateOf("100.0 kg") }
    var data by rememberSaveable { mutableStateOf("02/04/2026") }
    var hora by rememberSaveable { mutableStateOf("09:00") }

    val servicos = listOf("Expresso", "Standard", "Premium")
    var servicoExpanded by remember { mutableStateOf(false) }
    var servicoSelecionado by rememberSaveable { mutableStateOf(servicos[0]) }

    val tiposCarga = listOf("Normal", "Perecível", "Valor", "Vulnerável", "DGR")
    var tipoExpanded by remember { mutableStateOf(false) }
    var tipoSelecionado by rememberSaveable { mutableStateOf(tiposCarga[0]) }

    var erroMsg by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth().padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text(
                    text = "CADASTRAR NOVA CARGA",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF003E8A)
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = awb,
                    onValueChange = {
                        awb = it
                        erroMsg = null
                    },
                    label = { Text("Número AWB *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = cliente,
                    onValueChange = {
                        cliente = it
                        erroMsg = null
                    },
                    label = { Text("Cliente / Empresa *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = volumes,
                        onValueChange = { volumes = it },
                        label = { Text("Volumes") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = peso,
                        onValueChange = { peso = it },
                        label = { Text("Peso") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Select de Serviço
                ExposedDropdownMenuBox(
                    expanded = servicoExpanded,
                    onExpandedChange = { servicoExpanded = !servicoExpanded }
                ) {
                    OutlinedTextField(
                        value = servicoSelecionado,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Nível de Serviço") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = servicoExpanded) },
                        modifier = Modifier.menuAnchor(androidx.compose.material3.ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = servicoExpanded,
                        onDismissRequest = { servicoExpanded = false }
                    ) {
                        servicos.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item) },
                                onClick = {
                                    servicoSelecionado = item
                                    servicoExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Select de Tipo de Carga
                ExposedDropdownMenuBox(
                    expanded = tipoExpanded,
                    onExpandedChange = { tipoExpanded = !tipoExpanded }
                ) {
                    OutlinedTextField(
                        value = tipoSelecionado,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo de Carga") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tipoExpanded) },
                        modifier = Modifier.menuAnchor(androidx.compose.material3.ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = tipoExpanded,
                        onDismissRequest = { tipoExpanded = false }
                    ) {
                        tiposCarga.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item) },
                                onClick = {
                                    tipoSelecionado = item
                                    tipoExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = data,
                        onValueChange = {
                            data = it
                            erroMsg = null
                        },
                        label = { Text("Data (DD/MM/AAAA) *") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = hora,
                        onValueChange = {
                            hora = it
                            erroMsg = null
                        },
                        label = { Text("Hora (HH:mm) *") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                if (erroMsg != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = erroMsg!!, color = Color(0xFFDC2626), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            val dataValida = RetiraDeCargaViewModel.normalizarEValidarData(data)
                            val horaValida = RetiraDeCargaViewModel.normalizarEValidarHora(hora)
                            when {
                                awb.trim().isEmpty() -> erroMsg = "Informe o número AWB."
                                cliente.trim().isEmpty() -> erroMsg = "Informe o cliente ou empresa."
                                dataValida == null -> erroMsg = "Informe uma data válida no formato DD/MM/AAAA."
                                horaValida == null -> erroMsg = "Informe um horário válido no formato HH:mm."
                                else -> {
                                    onCadastrar(
                                        awb.trim(),
                                        cliente.trim(),
                                        volumes.trim().ifEmpty { "1 volume" },
                                        peso.trim().ifEmpty { "Não informado" },
                                        servicoSelecionado,
                                        tipoSelecionado,
                                        dataValida,
                                        horaValida
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003E8A))
                    ) {
                        Text("Salvar Carga")
                    }
                }
            }
        }
    }
}
