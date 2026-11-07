package com.example.teste

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.teste.data.local.AppDatabase
import com.example.teste.data.repository.RetiraDeCargaRepository
import com.example.teste.ui.component.AppBottomNavigationBar
import com.example.teste.ui.component.CalendarioModalDialog
import com.example.teste.ui.component.ConfirmacaoOcorrenciaDialog
import com.example.teste.ui.component.NovoAgendamentoDialog
import com.example.teste.ui.component.NovoCadastroCargaDialog
import com.example.teste.ui.screen.AgendamentosScreen
import com.example.teste.ui.screen.CargasScreen
import com.example.teste.ui.screen.DescricaoCargaScreen
import com.example.teste.ui.screen.LoginScreen
import com.example.teste.ui.screen.NotificacoesScreen
import com.example.teste.ui.screen.PerfilScreen
import com.example.teste.ui.theme.TesteTheme
import com.example.teste.ui.viewmodel.RetiraDeCargaViewModel
import com.example.teste.ui.viewmodel.RetiraDeCargaViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Activity Principal do Aplicativo Retira de Carga.
 * Configura o ambiente Jetpack Compose, injeta as dependências da camada MVVM
 * e renderiza o fluxo completo do aplicativo com tela de Login e persistência SQLite local.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TesteTheme {
                RetiraDeCargaAppMVVM()
            }
        }
    }
}

/**
 * Composable Principal integrado à ViewModel (MVVM).
 */
@Composable
fun RetiraDeCargaAppMVVM() {
    val context = LocalContext.current

    // Estado do Operador Logado no Sistema (null = Exibe Tela de Login) preservado em rotação
    var usuarioLogado by rememberSaveable { mutableStateOf<String?>(null) }

    // Se o operador não estiver autenticado, apresenta a Tela de Login
    if (usuarioLogado == null) {
        LoginScreen(
            onLoginSuccess = { usuario ->
                usuarioLogado = usuario
            }
        )
        return
    }

    // Injeção dos componentes de Dados e Arquitetura
    val database = remember { AppDatabase.getInstance(context) }
    val repository = remember { RetiraDeCargaRepository(database) }
    val factory = remember { RetiraDeCargaViewModelFactory(repository) }

    val viewModel: RetiraDeCargaViewModel = viewModel(factory = factory)

    // Coleta dos Estados reativos (StateFlow)
    val selectedTab by viewModel.selectedTab.collectAsState()
    val agendamentosExibidos by viewModel.agendamentosExibidos.collectAsState()
    val agendamentoSelecionado by viewModel.agendamentoSelecionado.collectAsState()
    val cargasState by viewModel.cargasState.collectAsState()
    val notificacoesState by viewModel.notificacoesState.collectAsState()
    val datasComCargaMap by viewModel.datasComCargaMap.collectAsState()
    val selectedDateFilter by viewModel.selectedDateFilter.collectAsState()
    val showCalendarModal by viewModel.showCalendarModal.collectAsState()
    val historicoCarga by viewModel.historicoCargaState.collectAsState()

    val unreadNotificationsCount = remember(notificacoesState) {
        notificacoesState.count { !it.lida }
    }

    // Estados para Controle dos Diálogos preservados em rotação
    var resultadoParaConfirmar by rememberSaveable { mutableStateOf<String?>(null) }
    var showNovoAgendamentoDialog by rememberSaveable { mutableStateOf(false) }
    var showNovoCadastroCargaDialog by rememberSaveable { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Tratamento do botão Voltar do sistema Android
    BackHandler(enabled = agendamentoSelecionado != null) {
        viewModel.selecionarAgendamento(null)
    }
    BackHandler(enabled = agendamentoSelecionado == null && selectedTab != 0) {
        viewModel.selecionarAba(0)
    }

    // Escuta de Eventos Globais de Mensagem do Sistema (sem bloquear o Toast pela suspensão do Snackbar)
    LaunchedEffect(Unit) {
        viewModel.userEvent.collectLatest { mensagem ->
            Toast.makeText(context, mensagem, Toast.LENGTH_SHORT).show()
            launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(mensagem)
            }
        }
    }

    // Modal de Calendário Interativo
    if (showCalendarModal) {
        CalendarioModalDialog(
            datasComCargaMap = datasComCargaMap,
            filtroAtivoAtual = selectedDateFilter,
            onDismiss = { viewModel.fecharCalendario() },
            onSelecionarEFiltrar = { data ->
                viewModel.aplicarFiltroData(data)
            }
        )
    }

    // Modal de Confirmação com Observação Obrigatória
    val selecionadoAtual = agendamentoSelecionado
    if (resultadoParaConfirmar != null && selecionadoAtual != null) {
        ConfirmacaoOcorrenciaDialog(
            resultado = resultadoParaConfirmar!!,
            onDismiss = { resultadoParaConfirmar = null },
            onConfirmar = { observacao ->
                viewModel.registrarOcorrencia(
                    agendamentoId = selecionadoAtual.agendamento.id,
                    resultado = resultadoParaConfirmar!!,
                    observacao = observacao
                )
                resultadoParaConfirmar = null
            }
        )
    }

    // Modal de Novo Agendamento (Reagendamento por AWB)
    if (showNovoAgendamentoDialog && selecionadoAtual != null) {
        val maxTentativa = maxOf(
            selecionadoAtual.agendamento.tentativaNumero,
            historicoCarga.maxOfOrNull { it.tentativaNumero } ?: 1
        )
        NovoAgendamentoDialog(
            awb = selecionadoAtual.carga.awb,
            tentativaAtual = maxTentativa,
            onDismiss = { showNovoAgendamentoDialog = false },
            onConfirmarReagendamento = { novaData, novoHorario ->
                viewModel.criarReagendamento(
                    cargaId = selecionadoAtual.carga.id,
                    agendamentoAnteriorId = selecionadoAtual.agendamento.id,
                    novaData = novaData,
                    novoHorario = novoHorario
                )
                showNovoAgendamentoDialog = false
            }
        )
    }

    // Modal de Cadastro de Nova Carga
    if (showNovoCadastroCargaDialog) {
        NovoCadastroCargaDialog(
            onDismiss = { showNovoCadastroCargaDialog = false },
            onCadastrar = { awb, cliente, volumes, peso, servico, tipoCarga, data, hora ->
                viewModel.cadastrarNovaCarga(
                    awb, cliente, volumes, peso, servico, tipoCarga, data, hora
                )
                showNovoCadastroCargaDialog = false
            }
        )
    }

    // Fluxo de Navegação: Tela de Detalhes ou Telas das Abas Principais
    if (selecionadoAtual != null) {
        DescricaoCargaScreen(
            item = selecionadoAtual,
            historico = historicoCarga,
            snackbarHostState = snackbarHostState,
            onBack = { viewModel.selecionarAgendamento(null) },
            onRegistrarResultadoClick = { resultado ->
                resultadoParaConfirmar = resultado
            },
            onNovoAgendamentoClick = {
                showNovoAgendamentoDialog = true
            }
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                AppBottomNavigationBar(
                    selectedTab = selectedTab,
                    unreadNotificationsCount = unreadNotificationsCount,
                    onTabSelected = { index -> viewModel.selecionarAba(index) }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedTab) {
                    0 -> AgendamentosScreen(
                        agendamentos = agendamentosExibidos,
                        filtroData = selectedDateFilter,
                        onCalendarClick = { viewModel.abrirCalendario() },
                        onLimparFiltro = { viewModel.limparFiltroData() },
                        onAgendamentoClick = { id -> viewModel.selecionarAgendamento(id) },
                        onNovoCadastroClick = { showNovoCadastroCargaDialog = true }
                    )
                    1 -> CargasScreen(cargas = cargasState)
                    2 -> NotificacoesScreen(
                        notificacoes = notificacoesState,
                        onMarcarTodasComoLidas = { viewModel.marcarNotificacoesComoLidas() }
                    )
                    3 -> PerfilScreen(
                        usuarioLogado = usuarioLogado ?: "Operador de Carga",
                        onLogoutClick = {
                            viewModel.resetarSessao()
                            usuarioLogado = null
                        }
                    )
                }
            }
        }
    }
}
