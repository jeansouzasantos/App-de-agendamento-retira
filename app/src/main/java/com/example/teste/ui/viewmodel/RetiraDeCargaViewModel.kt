package com.example.teste.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.teste.data.model.AgendamentoComCarga
import com.example.teste.data.model.CargaEntity
import com.example.teste.data.model.HistoricoTentativa
import com.example.teste.data.model.NotificacaoEntity
import com.example.teste.data.repository.RetiraDeCargaRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * ViewModel da arquitetura MVVM para o aplicativo Retira de Carga.
 * Gerencia os estados visuais (StateFlow) e orquestra as operações assíncronas via Coroutines.
 */
class RetiraDeCargaViewModel(
    private val repository: RetiraDeCargaRepository
) : ViewModel() {

    // Aba atualmente selecionada (0 = Agendamentos, 1 = Cargas, 2 = Notificações, 3 = Perfil)
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // ID do Agendamento selecionado para ver a tela de detalhes (null = lista)
    private val _selectedAgendamentoId = MutableStateFlow<Long?>(null)
    val selectedAgendamentoId: StateFlow<Long?> = _selectedAgendamentoId.asStateFlow()

    // Filtro ativo por data ("DD/MM/AAAA") selecionado no calendário
    private val _selectedDateFilter = MutableStateFlow<String?>(null)
    val selectedDateFilter: StateFlow<String?> = _selectedDateFilter.asStateFlow()

    // Controle de exibição do modal do calendário
    private val _showCalendarModal = MutableStateFlow(false)
    val showCalendarModal: StateFlow<Boolean> = _showCalendarModal.asStateFlow()

    // Eventos de mensagens/feedback visual (Snackbars/Toast)
    private val _userEvent = MutableSharedFlow<String>()
    val userEvent: SharedFlow<String> = _userEvent.asSharedFlow()

    // Fluxo com todos os agendamentos cadastrados no banco local
    val todosAgendamentos: StateFlow<List<AgendamentoComCarga>> = repository.getAgendamentosFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Agendamentos filtrados (pela data do calendário, caso esteja ativa)
    val agendamentosExibidos: StateFlow<List<AgendamentoComCarga>> = combine(
        todosAgendamentos,
        _selectedDateFilter
    ) { lista, filtro ->
        if (filtro == null) lista else lista.filter { it.agendamento.dataAgendada == filtro }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Catálogo de Cargas
    val cargasState: StateFlow<List<CargaEntity>> = repository.getCargasFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Notificações ativas do sistema
    val notificacoesState: StateFlow<List<NotificacaoEntity>> = repository.getNotificacoesFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Mapa de contagem de cargas ativas por data para o calendário
    val datasComCargaMap: StateFlow<Map<String, Int>> = repository.getDatasComAgendamentoMapFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    // Agendamento selecionado atualmente
    val agendamentoSelecionado: StateFlow<AgendamentoComCarga?> = combine(
        todosAgendamentos,
        _selectedAgendamentoId
    ) { lista, id ->
        if (id == null) null else lista.find { it.agendamento.id == id }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Estado reativo do histórico da carga selecionada (sem race conditions após inserção/atualização)
    @OptIn(ExperimentalCoroutinesApi::class)
    val historicoCargaState: StateFlow<List<HistoricoTentativa>> = agendamentoSelecionado
        .mapLatest { selecionado ->
            if (selecionado == null) {
                emptyList()
            } else {
                repository.getHistoricoCarga(selecionado.carga.id)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun selecionarAba(index: Int) {
        _selectedTab.value = index
    }

    fun marcarNotificacoesComoLidas() {
        viewModelScope.launch {
            repository.marcarNotificacoesComoLidas()
        }
    }

    fun selecionarAgendamento(id: Long?) {
        _selectedAgendamentoId.value = id
    }

    fun abrirCalendario() {
        _showCalendarModal.value = true
    }

    fun fecharCalendario() {
        _showCalendarModal.value = false
    }

    fun aplicarFiltroData(data: String) {
        _selectedDateFilter.value = data
        _selectedTab.value = 0 // Muda para a aba Agendamentos
        _showCalendarModal.value = false
    }

    fun limparFiltroData() {
        _selectedDateFilter.value = null
    }

    fun resetarSessao() {
        _selectedTab.value = 0
        _selectedAgendamentoId.value = null
        _selectedDateFilter.value = null
        _showCalendarModal.value = false
    }

    /**
     * Registra o resultado da tentativa com observação obrigatória.
     */
    fun registrarOcorrencia(
        agendamentoId: Long,
        resultado: String,
        observacao: String
    ) {
        if (observacao.trim().isEmpty()) {
            viewModelScope.launch {
                _userEvent.emit("Erro: A observação é obrigatória e não pode estar em branco.")
            }
            return
        }

        viewModelScope.launch {
            try {
                val sucesso = repository.registrarResultadoOcorrencia(agendamentoId, resultado, observacao)
                if (sucesso) {
                    _userEvent.emit("Ocorrência registrada com sucesso: $resultado")
                } else {
                    _userEvent.emit("Erro ao registrar a ocorrência no banco de dados.")
                }
            } catch (e: Exception) {
                _userEvent.emit("Erro: ${e.localizedMessage ?: "Falha ao registrar ocorrência."}")
            }
        }
    }

    /**
     * Reagenda uma carga criando uma nova tentativa (tentativa #2, #3...) com a mesma AWB.
     */
    fun criarReagendamento(
        cargaId: Long,
        agendamentoAnteriorId: Long,
        novaData: String,
        novoHorario: String
    ) {
        val dataFormatada = normalizarEValidarData(novaData)
        val horaFormatada = normalizarEValidarHora(novoHorario)

        if (dataFormatada == null) {
            viewModelScope.launch {
                _userEvent.emit("Erro: Informe uma data válida no formato DD/MM/AAAA.")
            }
            return
        }
        if (horaFormatada == null) {
            viewModelScope.launch {
                _userEvent.emit("Erro: Informe um horário válido no formato HH:mm.")
            }
            return
        }

        viewModelScope.launch {
            try {
                val novoId = repository.criarReagendamento(
                    cargaId = cargaId,
                    agendamentoAnteriorId = agendamentoAnteriorId,
                    novaData = dataFormatada,
                    novoHorario = horaFormatada
                )
                if (novoId > 0) {
                    _selectedAgendamentoId.value = novoId
                    _userEvent.emit("Novo agendamento criado com sucesso!")
                } else {
                    _userEvent.emit("Erro ao criar o novo agendamento.")
                }
            } catch (e: Exception) {
                _userEvent.emit("Erro: ${e.localizedMessage ?: "Falha ao criar reagendamento."}")
            }
        }
    }

    /**
     * Cadastra uma nova Carga do zero com seu primeiro Agendamento inicial.
     */
    fun cadastrarNovaCarga(
        awb: String,
        cliente: String,
        volumes: String,
        peso: String,
        servico: String,
        tipoCarga: String,
        dataAgendada: String,
        horaAgendada: String
    ) {
        if (awb.trim().isEmpty() || cliente.trim().isEmpty()) {
            viewModelScope.launch {
                _userEvent.emit("Erro: Preencha os campos obrigatórios (AWB e Cliente).")
            }
            return
        }

        val dataFormatada = normalizarEValidarData(dataAgendada)
        val horaFormatada = normalizarEValidarHora(horaAgendada)

        if (dataFormatada == null) {
            viewModelScope.launch {
                _userEvent.emit("Erro: Informe uma data válida no formato DD/MM/AAAA.")
            }
            return
        }
        if (horaFormatada == null) {
            viewModelScope.launch {
                _userEvent.emit("Erro: Informe um horário válido no formato HH:mm.")
            }
            return
        }

        viewModelScope.launch {
            try {
                repository.cadastrarNovaCarga(
                    awb = awb.trim(),
                    cliente = cliente.trim(),
                    volumes = volumes.trim().ifEmpty { "1 volume" },
                    peso = peso.trim().ifEmpty { "Não informado" },
                    servico = servico,
                    tipoCarga = tipoCarga,
                    dataAgendada = dataFormatada,
                    horaAgendada = horaFormatada
                )
                _userEvent.emit("Carga ${awb.trim()} cadastrada com sucesso!")
                _selectedTab.value = 0
            } catch (e: Exception) {
                _userEvent.emit("Erro ao cadastrar carga: ${e.localizedMessage ?: "Falha desconhecida."}")
            }
        }
    }

    companion object {
        fun normalizarEValidarData(input: String): String? {
            val partes = input.trim().split("/")
            if (partes.size != 3) return null
            val dia = partes[0].toIntOrNull() ?: return null
            val mes = partes[1].toIntOrNull() ?: return null
            val ano = partes[2].toIntOrNull() ?: return null
            if (ano !in 2000..2100) return null

            val formatada = String.format(Locale.getDefault(), "%02d/%02d/%04d", dia, mes, ano)
            return try {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
                    isLenient = false
                }
                sdf.parse(formatada)
                formatada
            } catch (_: Exception) {
                null
            }
        }

        fun normalizarEValidarHora(input: String): String? {
            val partes = input.trim().split(":")
            if (partes.size != 2) return null
            val hora = partes[0].toIntOrNull() ?: return null
            val minuto = partes[1].toIntOrNull() ?: return null
            if (hora !in 0..23 || minuto !in 0..59) return null
            return String.format(Locale.getDefault(), "%02d:%02d", hora, minuto)
        }
    }
}

/**
 * Factory para instanciar a ViewModel fornecendo o Repositório.
 */
class RetiraDeCargaViewModelFactory(
    private val repository: RetiraDeCargaRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RetiraDeCargaViewModel::class.java)) {
            return RetiraDeCargaViewModel(repository) as T
        }
        throw IllegalArgumentException("Classe ViewModel desconhecida")
    }
}
