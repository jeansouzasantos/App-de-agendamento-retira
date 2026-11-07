package com.example.teste.data.repository

import com.example.teste.data.local.AppDatabase
import com.example.teste.data.model.AgendamentoComCarga
import com.example.teste.data.model.CargaEntity
import com.example.teste.data.model.HistoricoTentativa
import com.example.teste.data.model.NotificacaoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Repositório central da aplicação Retira de Carga.
 * Intermedeia o acesso aos dados do banco de dados local com as ViewModels.
 */
class RetiraDeCargaRepository(private val db: AppDatabase) {

    /**
     * Retorna o fluxo reativo de todos os Agendamentos com suas respectivas Cargas.
     */
    fun getAgendamentosFlow(): Flow<List<AgendamentoComCarga>> {
        return db.getChangeSignal().map {
            withContext(Dispatchers.IO) {
                db.getAgendamentosComCarga()
            }
        }
    }

    /**
     * Retorna o fluxo reativo do catálogo de todas as Cargas cadastradas.
     */
    fun getCargasFlow(): Flow<List<CargaEntity>> {
        return db.getChangeSignal().map {
            withContext(Dispatchers.IO) {
                db.getTodasCargas()
            }
        }
    }

    /**
     * Retorna o fluxo reativo das Notificações do sistema.
     */
    fun getNotificacoesFlow(): Flow<List<NotificacaoEntity>> {
        return db.getChangeSignal().map {
            withContext(Dispatchers.IO) {
                db.getNotificacoes()
            }
        }
    }

    /**
     * Calcula o mapa de contagem de agendamentos pendentes por data (para o calendário).
     * Conta somente agendamentos com status pendente ("AGENDADO" ou "REAGENDADO").
     */
    fun getDatasComAgendamentoMapFlow(): Flow<Map<String, Int>> {
        return getAgendamentosFlow().map { lista ->
            lista
                .filter { it.agendamento.status == "AGENDADO" || it.agendamento.status == "REAGENDADO" }
                .groupBy { it.agendamento.dataAgendada }
                .mapValues { it.value.size }
        }
    }

    /**
     * Retorna o histórico completo de tentativas e ocorrências para uma Carga específica.
     */
    suspend fun getHistoricoCarga(cargaId: Long): List<HistoricoTentativa> =
        withContext(Dispatchers.IO) {
            db.getHistoricoCarga(cargaId)
        }

    /**
     * Marca todas as notificações não lidas como lidas.
     */
    suspend fun marcarNotificacoesComoLidas() = withContext(Dispatchers.IO) {
        db.marcarTodasNotificacoesComoLidas()
    }

    /**
     * Registra o resultado de uma ocorrência (ENTREGUE, CARGA NÃO LOCALIZADA, NÃO COMPARECEU)
     * e sua observação obrigatória.
     */
    suspend fun registrarResultadoOcorrencia(
        agendamentoId: Long,
        resultado: String,
        observacao: String
    ): Boolean = withContext(Dispatchers.IO) {
        db.registrarOcorrenciaAtomic(agendamentoId, resultado, observacao)
    }

    /**
     * Cria um novo agendamento (reagendamento) reutilizando os dados da Carga e AWB original.
     */
    suspend fun criarReagendamento(
        cargaId: Long,
        agendamentoAnteriorId: Long,
        novaData: String,
        novoHorario: String
    ): Long = withContext(Dispatchers.IO) {
        db.criarReagendamentoAtomic(cargaId, agendamentoAnteriorId, novaData, novoHorario)
    }

    /**
     * Cadastra uma nova carga e seu primeiro agendamento inicial.
     */
    suspend fun cadastrarNovaCarga(
        awb: String,
        cliente: String,
        volumes: String,
        peso: String,
        servico: String,
        tipoCarga: String,
        dataAgendada: String,
        horaAgendada: String
    ): Long = withContext(Dispatchers.IO) {
        db.cadastrarCargaEAgendamento(
            awb, cliente, volumes, peso, servico, tipoCarga, dataAgendada, horaAgendada
        )
    }
}
