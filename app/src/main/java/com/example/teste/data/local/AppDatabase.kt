package com.example.teste.data.local

import android.content.ContentValues
import android.content.Context
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.teste.data.model.AgendamentoComCarga
import com.example.teste.data.model.AgendamentoEntity
import com.example.teste.data.model.CargaEntity
import com.example.teste.data.model.HistoricoTentativa
import com.example.teste.data.model.NotificacaoEntity
import com.example.teste.data.model.OcorrenciaEntity
import com.example.teste.data.model.OperadorEntity
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onStart
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Gerenciador de Banco de Dados Local SQLite para o aplicativo Retira de Carga.
 * Garante persistência dos dados entre reinicializações do app e suporte a transações atômicas.
 */
class AppDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    // Emissor de eventos reativos para atualizar Flows quando o banco for modificado
    private val dbUpdateSignal = MutableSharedFlow<Unit>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    ).apply { tryEmit(Unit) }

    fun getChangeSignal(): Flow<Unit> = dbUpdateSignal.onStart { emit(Unit) }

    private fun notifyDataChanged() {
        dbUpdateSignal.tryEmit(Unit)
    }

    private fun nowTimestamp(): String =
        SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Tabela de Cargas
        db.execSQL(
            """
            CREATE TABLE cargas (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                awb TEXT NOT NULL,
                cliente TEXT NOT NULL,
                volumes TEXT NOT NULL,
                peso TEXT NOT NULL,
                servico TEXT NOT NULL,
                tipo_carga TEXT NOT NULL,
                informacoes_adicionais TEXT
            )
            """.trimIndent()
        )

        // Tabela de Agendamentos (Tentativas de Retirada)
        db.execSQL(
            """
            CREATE TABLE agendamentos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                carga_id INTEGER NOT NULL,
                data_agendada TEXT NOT NULL,
                hora_agendada TEXT NOT NULL,
                tentativa_numero INTEGER NOT NULL DEFAULT 1,
                status TEXT NOT NULL,
                agendamento_anterior_id INTEGER,
                data_criacao TEXT NOT NULL,
                data_finalizacao TEXT,
                FOREIGN KEY(carga_id) REFERENCES cargas(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        // Tabela de Ocorrências (Histórico de finalizações com observação obrigatória)
        db.execSQL(
            """
            CREATE TABLE ocorrencias (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                agendamento_id INTEGER NOT NULL,
                resultado TEXT NOT NULL,
                observacao TEXT NOT NULL,
                data_horario_registro TEXT NOT NULL,
                FOREIGN KEY(agendamento_id) REFERENCES agendamentos(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        // Tabela de Notificações
        db.execSQL(
            """
            CREATE TABLE notificacoes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                titulo TEXT NOT NULL,
                mensagem TEXT NOT NULL,
                data_horario TEXT NOT NULL,
                lida INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )

        // Tabela de Operadores (contas de acesso com senha em hash)
        criarTabelaOperadores(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Migrações incrementais: preservam a estrutura do banco e limpa dados de exemplo.
        if (oldVersion < 2) {
            criarTabelaOperadores(db)
        }
        if (oldVersion < 3) {
            db.execSQL("DELETE FROM ocorrencias")
            db.execSQL("DELETE FROM agendamentos")
            db.execSQL("DELETE FROM cargas")
            db.execSQL("DELETE FROM notificacoes")
        }
    }

    private fun criarTabelaOperadores(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS operadores (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                usuario TEXT NOT NULL UNIQUE COLLATE NOCASE,
                nome_completo TEXT NOT NULL,
                senha_hash TEXT NOT NULL,
                senha_salt TEXT NOT NULL,
                iteracoes INTEGER NOT NULL,
                data_criacao TEXT NOT NULL
            )
            """.trimIndent()
        )
    }

    // ============================================================================
    // MÉTODOS DE CONSULTA (READ)
    // ============================================================================

    fun getAgendamentosComCarga(): List<AgendamentoComCarga> {
        val list = mutableListOf<AgendamentoComCarga>()
        val db = readableDatabase

        val sql = """
            SELECT 
                a.id as a_id, a.carga_id as a_carga_id, a.data_agendada as a_data, a.hora_agendada as a_hora,
                a.tentativa_numero as a_tentativa, a.status as a_status, a.agendamento_anterior_id as a_anterior,
                a.data_criacao as a_criacao, a.data_finalizacao as a_finalizacao,
                c.id as c_id, c.awb as c_awb, c.cliente as c_cliente, c.volumes as c_volumes,
                c.peso as c_peso, c.servico as c_servico, c.tipo_carga as c_tipo, c.informacoes_adicionais as c_info,
                o.id as o_id, o.agendamento_id as o_agendamento_id, o.resultado as o_resultado,
                o.observacao as o_obs, o.data_horario_registro as o_registro
            FROM agendamentos a
            INNER JOIN cargas c ON a.carga_id = c.id
            LEFT JOIN ocorrencias o ON o.agendamento_id = a.id
            ORDER BY a.id DESC
        """.trimIndent()

        val cursor = db.rawQuery(sql, null)
        cursor.use { c ->
            while (c.moveToNext()) {
                val carga = CargaEntity(
                    id = c.getLong(c.getColumnIndexOrThrow("c_id")),
                    awb = c.getString(c.getColumnIndexOrThrow("c_awb")),
                    cliente = c.getString(c.getColumnIndexOrThrow("c_cliente")),
                    volumes = c.getString(c.getColumnIndexOrThrow("c_volumes")),
                    peso = c.getString(c.getColumnIndexOrThrow("c_peso")),
                    servico = c.getString(c.getColumnIndexOrThrow("c_servico")),
                    tipoCarga = c.getString(c.getColumnIndexOrThrow("c_tipo")),
                    informacoesAdicionais = c.getString(c.getColumnIndexOrThrow("c_info"))
                )

                val agendamento = AgendamentoEntity(
                    id = c.getLong(c.getColumnIndexOrThrow("a_id")),
                    cargaId = c.getLong(c.getColumnIndexOrThrow("a_carga_id")),
                    dataAgendada = c.getString(c.getColumnIndexOrThrow("a_data")),
                    horaAgendada = c.getString(c.getColumnIndexOrThrow("a_hora")),
                    tentativaNumero = c.getInt(c.getColumnIndexOrThrow("a_tentativa")),
                    status = c.getString(c.getColumnIndexOrThrow("a_status")),
                    agendamentoAnteriorId = if (c.isNull(c.getColumnIndexOrThrow("a_anterior"))) null else c.getLong(c.getColumnIndexOrThrow("a_anterior")),
                    dataCriacao = c.getString(c.getColumnIndexOrThrow("a_criacao")),
                    dataFinalizacao = c.getString(c.getColumnIndexOrThrow("a_finalizacao"))
                )

                val ocorrencia = if (!c.isNull(c.getColumnIndexOrThrow("o_id"))) {
                    OcorrenciaEntity(
                        id = c.getLong(c.getColumnIndexOrThrow("o_id")),
                        agendamentoId = c.getLong(c.getColumnIndexOrThrow("o_agendamento_id")),
                        resultado = c.getString(c.getColumnIndexOrThrow("o_resultado")),
                        observacao = c.getString(c.getColumnIndexOrThrow("o_obs")),
                        dataHorarioRegistro = c.getString(c.getColumnIndexOrThrow("o_registro"))
                    )
                } else null

                list.add(AgendamentoComCarga(agendamento, carga, ocorrencia))
            }
        }
        return list
    }

    fun getTodasCargas(): List<CargaEntity> {
        val list = mutableListOf<CargaEntity>()
        val db = readableDatabase
        val cursor = db.query("cargas", null, null, null, null, null, "id DESC")
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(
                    CargaEntity(
                        id = c.getLong(c.getColumnIndexOrThrow("id")),
                        awb = c.getString(c.getColumnIndexOrThrow("awb")),
                        cliente = c.getString(c.getColumnIndexOrThrow("cliente")),
                        volumes = c.getString(c.getColumnIndexOrThrow("volumes")),
                        peso = c.getString(c.getColumnIndexOrThrow("peso")),
                        servico = c.getString(c.getColumnIndexOrThrow("servico")),
                        tipoCarga = c.getString(c.getColumnIndexOrThrow("tipo_carga")),
                        informacoesAdicionais = c.getString(c.getColumnIndexOrThrow("informacoes_adicionais"))
                    )
                )
            }
        }
        return list
    }

    fun getHistoricoCarga(cargaId: Long): List<HistoricoTentativa> {
        val list = mutableListOf<HistoricoTentativa>()
        val db = readableDatabase

        val sql = """
            SELECT 
                a.tentativa_numero, a.data_agendada, a.hora_agendada, a.status,
                a.data_finalizacao, o.observacao
            FROM agendamentos a
            LEFT JOIN ocorrencias o ON o.agendamento_id = a.id
            WHERE a.carga_id = ?
            ORDER BY a.tentativa_numero ASC
        """.trimIndent()

        val cursor = db.rawQuery(sql, arrayOf(cargaId.toString()))
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(
                    HistoricoTentativa(
                        tentativaNumero = c.getInt(c.getColumnIndexOrThrow("tentativa_numero")),
                        dataAgendada = c.getString(c.getColumnIndexOrThrow("data_agendada")),
                        horaAgendada = c.getString(c.getColumnIndexOrThrow("hora_agendada")),
                        resultadoStatus = c.getString(c.getColumnIndexOrThrow("status")),
                        observacao = c.getString(c.getColumnIndexOrThrow("observacao")),
                        dataFinalizacao = c.getString(c.getColumnIndexOrThrow("data_finalizacao"))
                    )
                )
            }
        }
        return list
    }

    fun getNotificacoes(): List<NotificacaoEntity> {
        val list = mutableListOf<NotificacaoEntity>()
        val db = readableDatabase
        val cursor = db.query("notificacoes", null, null, null, null, null, "id DESC")
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(
                    NotificacaoEntity(
                        id = c.getLong(c.getColumnIndexOrThrow("id")),
                        titulo = c.getString(c.getColumnIndexOrThrow("titulo")),
                        mensagem = c.getString(c.getColumnIndexOrThrow("mensagem")),
                        dataHorario = c.getString(c.getColumnIndexOrThrow("data_horario")),
                        lida = c.getInt(c.getColumnIndexOrThrow("lida")) == 1
                    )
                )
            }
        }
        return list
    }

    // ============================================================================
    // MÉTODOS DE ESCRITA / TRANSAÇÕES ATÔMICAS
    // ============================================================================

    /**
     * Marca todas as notificações pendentes como lidas.
     */
    fun marcarTodasNotificacoesComoLidas() {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("lida", 1)
        }
        val rows = db.update("notificacoes", values, "lida = 0", null)
        if (rows > 0) {
            notifyDataChanged()
        }
    }

    /**
     * Registra o resultado de uma ocorrência (ENTREGUE, CARGA NÃO LOCALIZADA, NÃO COMPARECEU)
     * em uma transação atômica no banco de dados local.
     */
    fun registrarOcorrenciaAtomic(
        agendamentoId: Long,
        resultado: String,
        observacao: String
    ): Boolean {
        require(observacao.trim().isNotEmpty()) { "A observação é obrigatória." }

        val db = writableDatabase
        val now = nowTimestamp()

        db.beginTransaction()
        try {
            // Verificar se o agendamento existe e ainda está pendente
            val statusCursor = db.rawQuery(
                "SELECT status FROM agendamentos WHERE id = ?",
                arrayOf(agendamentoId.toString())
            )
            statusCursor.use { c ->
                if (!c.moveToFirst()) {
                    throw IllegalArgumentException("Agendamento não encontrado.")
                }
                val statusAtual = c.getString(0) ?: ""
                if (statusAtual == "ENTREGUE" || statusAtual == "CARGA NÃO LOCALIZADA" || statusAtual == "NÃO COMPARECEU") {
                    throw IllegalStateException("Este agendamento já foi finalizado com o status: $statusAtual.")
                }
            }

            // 1. Inserir Ocorrência
            val valOcorrencia = ContentValues().apply {
                put("agendamento_id", agendamentoId)
                put("resultado", resultado)
                put("observacao", observacao.trim())
                put("data_horario_registro", now)
            }
            db.insertOrThrow("ocorrencias", null, valOcorrencia)

            // 2. Atualizar Agendamento
            val valAgendamento = ContentValues().apply {
                put("status", resultado)
                put("data_finalizacao", now)
            }
            db.update("agendamentos", valAgendamento, "id = ?", arrayOf(agendamentoId.toString()))

            // 3. Buscar dados da carga para a notificação
            var awbCarga = ""
            val cursor = db.rawQuery(
                "SELECT c.awb FROM cargas c INNER JOIN agendamentos a ON a.carga_id = c.id WHERE a.id = ?",
                arrayOf(agendamentoId.toString())
            )
            cursor.use { c ->
                if (c.moveToFirst()) awbCarga = c.getString(0)
            }

            // 4. Inserir Notificação real de evento do sistema
            val valNotif = ContentValues().apply {
                put("titulo", "Ocorrência Registrada: $resultado")
                put("mensagem", "AWB $awbCarga finalizada como $resultado. Obs: ${observacao.trim()}")
                put("data_horario", now)
                put("lida", 0)
            }
            db.insert("notificacoes", null, valNotif)

            db.setTransactionSuccessful()
            notifyDataChanged()
            return true
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Cria um novo agendamento (reagendamento) mantendo a mesma AWB e incrementando
     * o número da tentativa de forma atômica no banco.
     */
    fun criarReagendamentoAtomic(
        cargaId: Long,
        agendamentoAnteriorId: Long,
        novaData: String,
        novoHorario: String
    ): Long {
        val db = writableDatabase
        val now = nowTimestamp()

        db.beginTransaction()
        try {
            // Verificar se o agendamento anterior permite reagendamento e se já não foi reagendado
            var statusAnterior = ""
            var tentativaAnterior = 1
            val antCursor = db.rawQuery(
                "SELECT status, tentativa_numero FROM agendamentos WHERE id = ? AND carga_id = ?",
                arrayOf(agendamentoAnteriorId.toString(), cargaId.toString())
            )
            antCursor.use { c ->
                if (!c.moveToFirst()) {
                    throw IllegalArgumentException("Agendamento original não encontrado.")
                }
                statusAnterior = c.getString(0) ?: ""
                tentativaAnterior = c.getInt(1)
            }

            if (statusAnterior != "NÃO COMPARECEU") {
                throw IllegalStateException("Somente agendamentos com status NÃO COMPARECEU podem ser reagendados.")
            }

            // 1. Obter a maior tentativa existente para esta carga e a AWB
            var maxTentativa = 1
            var awbCarga = ""
            val cursor = db.rawQuery(
                "SELECT MAX(a.tentativa_numero), c.awb FROM agendamentos a INNER JOIN cargas c ON a.carga_id = c.id WHERE a.carga_id = ? GROUP BY c.id",
                arrayOf(cargaId.toString())
            )
            cursor.use { c ->
                if (c.moveToFirst()) {
                    maxTentativa = c.getInt(0)
                    awbCarga = c.getString(1) ?: ""
                }
            }

            if (maxTentativa > tentativaAnterior) {
                throw IllegalStateException("Esta tentativa já possui um reagendamento posterior registrado.")
            }

            val proximaTentativa = maxTentativa + 1

            // 2. Inserir Novo Agendamento
            val valNovoAgendamento = ContentValues().apply {
                put("carga_id", cargaId)
                put("data_agendada", novaData)
                put("hora_agendada", novoHorario)
                put("tentativa_numero", proximaTentativa)
                put("status", "REAGENDADO")
                put("agendamento_anterior_id", agendamentoAnteriorId)
                put("data_criacao", now)
            }
            val newId = db.insertOrThrow("agendamentos", null, valNovoAgendamento)

            // 3. Inserir Notificação de sistema
            val valNotif = ContentValues().apply {
                put("titulo", "Novo Agendamento Criado")
                put("mensagem", "AWB $awbCarga reagendada para $novaData às $novoHorario (${proximaTentativa}ª tentativa).")
                put("data_horario", now)
                put("lida", 0)
            }
            db.insert("notificacoes", null, valNotif)

            db.setTransactionSuccessful()
            notifyDataChanged()
            return newId
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Cadastra uma nova Carga e seu primeiro Agendamento inicial.
     */
    fun cadastrarCargaEAgendamento(
        awb: String,
        cliente: String,
        volumes: String,
        peso: String,
        servico: String,
        tipoCarga: String,
        dataAgendada: String,
        horaAgendada: String
    ): Long {
        val db = writableDatabase
        val now = nowTimestamp()

        db.beginTransaction()
        try {
            val awbLimpo = awb.trim()
            val dupCursor = db.rawQuery(
                "SELECT id FROM cargas WHERE LOWER(awb) = LOWER(?)",
                arrayOf(awbLimpo)
            )
            dupCursor.use { c ->
                if (c.moveToFirst()) {
                    throw IllegalArgumentException("Já existe uma carga cadastrada com o AWB $awbLimpo.")
                }
            }

            val valCarga = ContentValues().apply {
                put("awb", awbLimpo)
                put("cliente", cliente.trim())
                put("volumes", volumes.trim().ifEmpty { "1 volume" })
                put("peso", peso.trim().ifEmpty { "Não informado" })
                put("servico", servico)
                put("tipo_carga", tipoCarga)
            }
            val cargaId = db.insertOrThrow("cargas", null, valCarga)

            val valAgendamento = ContentValues().apply {
                put("carga_id", cargaId)
                put("data_agendada", dataAgendada)
                put("hora_agendada", horaAgendada)
                put("tentativa_numero", 1)
                put("status", "AGENDADO")
                put("data_criacao", now)
            }
            val agendamentoId = db.insertOrThrow("agendamentos", null, valAgendamento)

            val valNotif = ContentValues().apply {
                put("titulo", "Novo Cadastro de Carga")
                put("mensagem", "Carga AWB $awbLimpo cadastrada para ${cliente.trim()} ($dataAgendada $horaAgendada).")
                put("data_horario", now)
                put("lida", 0)
            }
            db.insert("notificacoes", null, valNotif)

            db.setTransactionSuccessful()
            notifyDataChanged()
            return agendamentoId
        } finally {
            db.endTransaction()
        }
    }

    // ============================================================================
    // OPERADORES (AUTENTICAÇÃO LOCAL)
    // ============================================================================

    fun contarOperadores(): Int =
        DatabaseUtils.queryNumEntries(readableDatabase, "operadores").toInt()

    fun buscarOperadorPorUsuario(usuario: String): OperadorEntity? {
        val cursor = readableDatabase.query(
            "operadores", null, "usuario = ?", arrayOf(usuario), null, null, null, "1"
        )
        cursor.use { c ->
            if (!c.moveToFirst()) return null
            return OperadorEntity(
                id = c.getLong(c.getColumnIndexOrThrow("id")),
                usuario = c.getString(c.getColumnIndexOrThrow("usuario")),
                nomeCompleto = c.getString(c.getColumnIndexOrThrow("nome_completo")),
                senhaHash = c.getString(c.getColumnIndexOrThrow("senha_hash")),
                senhaSalt = c.getString(c.getColumnIndexOrThrow("senha_salt")),
                iteracoes = c.getInt(c.getColumnIndexOrThrow("iteracoes")),
                dataCriacao = c.getString(c.getColumnIndexOrThrow("data_criacao"))
            )
        }
    }

    /** Lança SQLiteConstraintException se o usuário já existir. */
    fun inserirOperador(
        usuario: String,
        nomeCompleto: String,
        senhaHash: String,
        senhaSalt: String,
        iteracoes: Int
    ): Long {
        val values = ContentValues().apply {
            put("usuario", usuario)
            put("nome_completo", nomeCompleto)
            put("senha_hash", senhaHash)
            put("senha_salt", senhaSalt)
            put("iteracoes", iteracoes)
            put("data_criacao", nowTimestamp())
        }
        return writableDatabase.insertOrThrow("operadores", null, values)
    }

    companion object {
        private const val DATABASE_NAME = "retira_de_carga.db"
        private const val DATABASE_VERSION = 3

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = AppDatabase(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
