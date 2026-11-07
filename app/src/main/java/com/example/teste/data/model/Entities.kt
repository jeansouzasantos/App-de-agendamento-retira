package com.example.teste.data.model

import java.util.Calendar

/**
 * Estrutura auxiliar para manipular Ano e Mês no calendário sem depender
 * de APIs mais recentes do Android.
 */
data class YearMonthData(val year: Int, val month: Int) {
    fun plusMonths(count: Int): YearMonthData {
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, 1)
        cal.add(Calendar.MONTH, count)
        return YearMonthData(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }

    fun minusMonths(count: Int): YearMonthData = plusMonths(-count)

    fun lengthOfMonth(): Int {
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, 1)
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    fun firstDayOfWeekIndex(): Int {
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, 1)
        return cal.get(Calendar.DAY_OF_WEEK) - 1
    }
}

/**
 * Representa os dados relativamente permanentes de uma Carga no banco de dados.
 */
data class CargaEntity(
    val id: Long = 0,
    val awb: String,                    // Número AWB (referência logística)
    val cliente: String,                // Nome do cliente ou empresa
    val volumes: String,                // Descrição/quantidade de volumes
    val peso: String,                   // Peso (ex: "150.5 kg")
    val servico: String,                // Nível de serviço: "Expresso", "Standard", "Premium"
    val tipoCarga: String,              // Categoria: "Normal", "Perecível", "Valor", "Vulnerável", "DGR"
    val informacoesAdicionais: String? = null
)

/**
 * Representa uma tentativa ou agendamento específico de retirada da carga.
 */
data class AgendamentoEntity(
    val id: Long = 0,
    val cargaId: Long,                  // Vínculo com a CargaEntity
    val dataAgendada: String,           // Data no formato "DD/MM/YYYY"
    val horaAgendada: String,           // Horário no formato "HH:mm"
    val tentativaNumero: Int = 1,       // 1 para 1ª tentativa (AGENDADO), 2+ para REAGENDADO
    val status: String,                 // "AGENDADO", "REAGENDADO", "ENTREGUE", "CARGA NÃO LOCALIZADA", "NÃO COMPARECEU"
    val agendamentoAnteriorId: Long? = null,
    val dataCriacao: String,            // Data/hora de criação do agendamento
    val dataFinalizacao: String? = null // Data/hora da ocorrência/finalização
)

/**
 * Registra o histórico da ocorrência registrada pelo operador ao finalizar um agendamento.
 */
data class OcorrenciaEntity(
    val id: Long = 0,
    val agendamentoId: Long,            // Vínculo com o AgendamentoEntity
    val resultado: String,              // "ENTREGUE", "CARGA NÃO LOCALIZADA", "NÃO COMPARECEU"
    val observacao: String,             // Observação obrigatória registrada
    val dataHorarioRegistro: String     // Timestamp do registro
)

/**
 * Representa eventos do sistema gravados na aba Notificações.
 */
data class NotificacaoEntity(
    val id: Long = 0,
    val titulo: String,
    val mensagem: String,
    val dataHorario: String,
    val lida: Boolean = false
)

/**
 * Relacionamento completo de um Agendamento com sua Carga e Ocorrência associada.
 */
data class AgendamentoComCarga(
    val agendamento: AgendamentoEntity,
    val carga: CargaEntity,
    val ocorrencia: OcorrenciaEntity? = null
)

/**
 * Item simplificado para exibição do Histórico Completo de uma AWB.
 */
data class HistoricoTentativa(
    val tentativaNumero: Int,
    val dataAgendada: String,
    val horaAgendada: String,
    val resultadoStatus: String,
    val observacao: String?,
    val dataFinalizacao: String?
)