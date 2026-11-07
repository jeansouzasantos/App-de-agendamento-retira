package com.example.teste.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.teste.data.local.AppDatabase
import com.example.teste.data.repository.AuthRepository
import com.example.teste.data.repository.ResultadoLogin
import com.example.teste.data.repository.ResultadoRegistro
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Tela de Login com verificação tradicional de usuário e senha contra o banco SQLite local.
 * O acesso é negado se não houver nenhuma conta cadastrada, se o usuário não existir
 * ou se a senha não conferir.
 */
@Composable
fun LoginScreen(
    onLoginSuccess: (nomeOperador: String) -> Unit
) {
    val context = LocalContext.current
    val authRepository = remember { AuthRepository(AppDatabase.getInstance(context)) }
    val scope = rememberCoroutineScope()

    var usuario by rememberSaveable { mutableStateOf("") }
    // A senha não é salva no Bundle de estado (rememberSaveable) por segurança.
    var senha by remember { mutableStateOf("") }
    var senhaVisivel by remember { mutableStateOf(false) }
    var erroValidacao by remember { mutableStateOf<String?>(null) }
    var verificando by remember { mutableStateOf(false) }
    var existemContas by remember { mutableStateOf<Boolean?>(null) }

    var showRegistroDialog by rememberSaveable { mutableStateOf(false) }
    var registroCarregando by remember { mutableStateOf(false) }
    var registroErro by remember { mutableStateOf<String?>(null) }

    val primaryNavy = Color(0xFF003E8A)

    // Migra contas da versão antiga (SharedPreferences) e verifica se há contas no banco
    LaunchedEffect(Unit) {
        try {
            authRepository.migrarContasLegadas(context.applicationContext)
            existemContas = authRepository.existemContas()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            existemContas = null
        }
    }

    fun tentarLogin() {
        if (verificando) return
        when {
            usuario.isBlank() -> erroValidacao = "Por favor, informe o usuário do operador."
            senha.isEmpty() -> erroValidacao = "Por favor, informe a senha de acesso."
            else -> {
                verificando = true
                erroValidacao = null
                scope.launch {
                    val resultado = try {
                        authRepository.autenticar(usuario, senha)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        null
                    }
                    verificando = false
                    when (resultado) {
                        is ResultadoLogin.Sucesso -> {
                            senha = ""
                            onLoginSuccess(resultado.nomeCompleto)
                        }
                        ResultadoLogin.NenhumaContaCadastrada -> {
                            existemContas = false
                            erroValidacao = "Nenhuma conta cadastrada neste dispositivo. Registre-se para acessar."
                        }
                        ResultadoLogin.CredenciaisInvalidas -> {
                            senha = ""
                            erroValidacao = "Usuário ou senha inválidos."
                        }
                        null -> erroValidacao = "Erro ao verificar as credenciais no banco local."
                    }
                }
            }
        }
    }

    if (showRegistroDialog) {
        RegistroOperadorDialog(
            carregando = registroCarregando,
            erroExterno = registroErro,
            onLimparErroExterno = { registroErro = null },
            onDismiss = {
                if (!registroCarregando) {
                    showRegistroDialog = false
                    registroErro = null
                }
            },
            onSubmit = { nome, novoUsuario, novaSenha ->
                registroCarregando = true
                registroErro = null
                scope.launch {
                    val resultado = try {
                        authRepository.registrar(nome, novoUsuario, novaSenha)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        null
                    }
                    registroCarregando = false
                    when (resultado) {
                        is ResultadoRegistro.Sucesso -> {
                            usuario = resultado.usuario
                            senha = ""
                            erroValidacao = null
                            existemContas = true
                            showRegistroDialog = false
                            Toast.makeText(
                                context,
                                "Operador ${resultado.nomeCompleto} cadastrado! Informe a senha para entrar.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        ResultadoRegistro.UsuarioJaExiste -> registroErro = "Este usuário já está cadastrado."
                        is ResultadoRegistro.Invalido -> registroErro = resultado.motivo
                        null -> registroErro = "Erro ao salvar o operador no banco local."
                    }
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(primaryNavy)
            .systemBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logotipo de Transporte / Logística
            Surface(
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                color = Color.White
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.LocalShipping,
                        contentDescription = null,
                        tint = primaryNavy,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Retira de Carga",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Terminal Operacional Logístico",
                color = Color(0xFFB0CEFF),
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Cartão de Login
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Acesso ao Sistema",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryNavy
                    )

                    if (existemContas == false) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFEF3C7),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Nenhuma conta cadastrada neste dispositivo. Toque em \"Registrar-se\" para criar o primeiro operador.",
                                color = Color(0xFF92400E),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Campo Usuário / E-mail
                    OutlinedTextField(
                        value = usuario,
                        onValueChange = {
                            usuario = it
                            erroValidacao = null
                        },
                        label = { Text("Usuário / Operador") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color(0xFF6B7280)
                            )
                        },
                        singleLine = true,
                        enabled = !verificando,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Campo Senha
                    OutlinedTextField(
                        value = senha,
                        onValueChange = {
                            senha = it
                            erroValidacao = null
                        },
                        label = { Text("Senha") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFF6B7280)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { senhaVisivel = !senhaVisivel }) {
                                Icon(
                                    imageVector = if (senhaVisivel) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (senhaVisivel) "Ocultar senha" else "Exibir senha",
                                    tint = Color(0xFF6B7280)
                                )
                            }
                        },
                        visualTransformation = if (senhaVisivel) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onDone = { tentarLogin() }
                        ),
                        singleLine = true,
                        enabled = !verificando,
                        isError = erroValidacao != null,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (erroValidacao != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = erroValidacao!!,
                            color = Color(0xFFDC2626),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.Start)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Botão Entrar
                    Button(
                        onClick = { tentarLogin() },
                        enabled = !verificando,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryNavy)
                    ) {
                        if (verificando) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(22.dp)
                            )
                        } else {
                            Text(
                                text = "Entrar no Sistema",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Botão Registrar-se
                    OutlinedButton(
                        onClick = {
                            registroErro = null
                            showRegistroDialog = true
                        },
                        enabled = !verificando,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Registrar-se (Criar Conta)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryNavy
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Nota sobre Operação Local
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x33FFFFFF)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Autenticação da Estação Operacional Local (Modo Offline).",
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

/**
 * Diálogo de Registro de Novo Operador no Sistema.
 * A validação final e a gravação (com hash da senha) são feitas pelo [AuthRepository].
 */
@Composable
fun RegistroOperadorDialog(
    carregando: Boolean,
    erroExterno: String?,
    onLimparErroExterno: () -> Unit,
    onDismiss: () -> Unit,
    onSubmit: (nome: String, usuario: String, senha: String) -> Unit
) {
    var nomeCompleto by rememberSaveable { mutableStateOf("") }
    var usuario by rememberSaveable { mutableStateOf("") }
    // Senhas não são salvas no Bundle de estado por segurança.
    var senha by remember { mutableStateOf("") }
    var confirmarSenha by remember { mutableStateOf("") }
    var erroLocal by remember { mutableStateOf<String?>(null) }

    val erroMsg = erroLocal ?: erroExterno
    val primaryNavy = Color(0xFF003E8A)

    fun limparErros() {
        erroLocal = null
        onLimparErroExterno()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = !carregando,
            dismissOnClickOutside = !carregando
        )
    ) {
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
                    text = "REGISTRAR NOVO OPERADOR",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryNavy
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Nome Completo
                OutlinedTextField(
                    value = nomeCompleto,
                    onValueChange = {
                        nomeCompleto = it
                        limparErros()
                    },
                    label = { Text("Nome Completo *") },
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = Color(0xFF6B7280)) },
                    singleLine = true,
                    enabled = !carregando,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Usuário / E-mail
                OutlinedTextField(
                    value = usuario,
                    onValueChange = {
                        usuario = it
                        limparErros()
                    },
                    label = { Text("Usuário / E-mail *") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF6B7280)) },
                    singleLine = true,
                    enabled = !carregando,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Senha
                OutlinedTextField(
                    value = senha,
                    onValueChange = {
                        senha = it
                        limparErros()
                    },
                    label = { Text("Senha * (mín. ${AuthRepository.SENHA_MIN} caracteres)") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF6B7280)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    enabled = !carregando,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Confirmar Senha
                OutlinedTextField(
                    value = confirmarSenha,
                    onValueChange = {
                        confirmarSenha = it
                        limparErros()
                    },
                    label = { Text("Confirmar Senha *") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF6B7280)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    enabled = !carregando,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                if (erroMsg != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = erroMsg, color = Color(0xFFDC2626), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss, enabled = !carregando) {
                        Text("Cancelar")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val erro = AuthRepository.validarRegistro(nomeCompleto, usuario, senha)
                                ?: if (senha != confirmarSenha) "As senhas não coincidem." else null
                            if (erro != null) {
                                erroLocal = erro
                            } else {
                                onSubmit(nomeCompleto, usuario, senha)
                            }
                        },
                        enabled = !carregando,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryNavy)
                    ) {
                        if (carregando) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Text("Cadastrar Operador", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
