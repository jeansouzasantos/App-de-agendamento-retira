package com.example.teste.data.repository

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import com.example.teste.data.local.AppDatabase
import com.example.teste.data.security.PasswordHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Base64
import java.util.Locale

sealed interface ResultadoLogin {
    data class Sucesso(val usuario: String, val nomeCompleto: String) : ResultadoLogin
    data object NenhumaContaCadastrada : ResultadoLogin
    data object CredenciaisInvalidas : ResultadoLogin
}

sealed interface ResultadoRegistro {
    data class Sucesso(val usuario: String, val nomeCompleto: String) : ResultadoRegistro
    data object UsuarioJaExiste : ResultadoRegistro
    data class Invalido(val motivo: String) : ResultadoRegistro
}

/**
 * Autenticação tradicional (usuário + senha) contra a tabela `operadores` do SQLite local.
 * O login é rejeitado se o usuário não existir, se a senha não conferir
 * ou se não houver nenhuma conta cadastrada no dispositivo.
 */
class AuthRepository(private val db: AppDatabase) {

    suspend fun existemContas(): Boolean = withContext(Dispatchers.IO) {
        db.contarOperadores() > 0
    }

    suspend fun autenticar(usuario: String, senha: String): ResultadoLogin = withContext(Dispatchers.IO) {
        if (db.contarOperadores() == 0) return@withContext ResultadoLogin.NenhumaContaCadastrada

        val operador = db.buscarOperadorPorUsuario(normalizarUsuario(usuario))
        val senhaChars = senha.toCharArray()
        try {
            if (operador == null) {
                // Executa um hash "falso" para que o tempo de resposta não revele se o usuário existe.
                PasswordHasher.hash(senhaChars, SALT_FICTICIO)
                return@withContext ResultadoLogin.CredenciaisInvalidas
            }

            val valida = PasswordHasher.verificar(
                senha = senhaChars,
                salt = decoder.decode(operador.senhaSalt),
                hashEsperado = decoder.decode(operador.senhaHash),
                iteracoes = operador.iteracoes
            )
            if (valida) {
                ResultadoLogin.Sucesso(operador.usuario, operador.nomeCompleto)
            } else {
                ResultadoLogin.CredenciaisInvalidas
            }
        } finally {
            senhaChars.fill('\u0000')
        }
    }

    suspend fun registrar(nomeCompleto: String, usuario: String, senha: String): ResultadoRegistro =
        withContext(Dispatchers.IO) {
            validarRegistro(nomeCompleto, usuario, senha)?.let {
                return@withContext ResultadoRegistro.Invalido(it)
            }

            val usuarioNormalizado = normalizarUsuario(usuario)
            val nome = nomeCompleto.trim()
            if (db.buscarOperadorPorUsuario(usuarioNormalizado) != null) {
                return@withContext ResultadoRegistro.UsuarioJaExiste
            }

            try {
                inserirComHash(usuarioNormalizado, nome, senha)
                ResultadoRegistro.Sucesso(usuarioNormalizado, nome)
            } catch (_: SQLiteConstraintException) {
                ResultadoRegistro.UsuarioJaExiste
            }
        }

    /**
     * Migra contas da versão anterior (SharedPreferences com senha em texto puro)
     * para o banco, já com hash, e apaga os dados legados.
     */
    suspend fun migrarContasLegadas(context: Context) = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_LEGADO, Context.MODE_PRIVATE)
        val todas = prefs.all
        if (todas.isEmpty()) return@withContext

        todas.keys.filter { it.startsWith("pwd_") }.forEach { chave ->
            val usuario = normalizarUsuario(chave.removePrefix("pwd_"))
            val senha = todas[chave] as? String ?: return@forEach
            val nome = (todas["name_${chave.removePrefix("pwd_")}"] as? String)?.trim().orEmpty().ifEmpty { usuario }
            if (usuario.isNotEmpty() && senha.isNotEmpty() && db.buscarOperadorPorUsuario(usuario) == null) {
                try {
                    inserirComHash(usuario, nome, senha)
                } catch (_: SQLiteConstraintException) {
                    // Já existe: ignora.
                }
            }
        }
        prefs.edit().clear().apply()
    }

    private fun inserirComHash(usuario: String, nome: String, senha: String) {
        val salt = PasswordHasher.gerarSalt()
        val senhaChars = senha.toCharArray()
        try {
            val hash = PasswordHasher.hash(senhaChars, salt)
            db.inserirOperador(
                usuario = usuario,
                nomeCompleto = nome,
                senhaHash = encoder.encodeToString(hash),
                senhaSalt = encoder.encodeToString(salt),
                iteracoes = PasswordHasher.ITERACOES_PADRAO
            )
        } finally {
            senhaChars.fill('\u0000')
        }
    }

    companion object {
        private const val PREFS_LEGADO = "operadores_prefs"
        const val SENHA_MIN = 6
        const val SENHA_MAX = 128
        private val USUARIO_REGEX = Regex("^[a-z0-9._@-]{3,50}$")
        private val SALT_FICTICIO = ByteArray(16) { 0x5A }
        private val encoder = Base64.getEncoder()
        private val decoder = Base64.getDecoder()

        fun normalizarUsuario(usuario: String): String = usuario.trim().lowercase(Locale.ROOT)

        /** Retorna a mensagem de erro, ou null se os dados forem válidos. */
        fun validarRegistro(nomeCompleto: String, usuario: String, senha: String): String? = when {
            nomeCompleto.isBlank() -> "Informe seu nome completo."
            usuario.isBlank() -> "Informe o usuário ou e-mail."
            !USUARIO_REGEX.matches(normalizarUsuario(usuario)) ->
                "Usuário deve ter de 3 a 50 caracteres (letras, números, . _ - @), sem espaços."
            senha.length < SENHA_MIN -> "A senha deve ter pelo menos $SENHA_MIN caracteres."
            senha.length > SENHA_MAX -> "A senha deve ter no máximo $SENHA_MAX caracteres."
            senha.isBlank() -> "A senha não pode conter apenas espaços."
            else -> null
        }
    }
}
