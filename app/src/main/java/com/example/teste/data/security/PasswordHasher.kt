package com.example.teste.data.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Geração e verificação de hash de senha com PBKDF2-HMAC-SHA256 + salt aleatório.
 * Disponível nativamente a partir da API 26 (minSdk do projeto).
 */
object PasswordHasher {
    private const val ALGORITMO = "PBKDF2WithHmacSHA256"
    const val ITERACOES_PADRAO = 120_000
    private const val TAMANHO_CHAVE_BITS = 256
    private const val TAMANHO_SALT_BYTES = 16

    private val random = SecureRandom()

    fun gerarSalt(): ByteArray = ByteArray(TAMANHO_SALT_BYTES).also { random.nextBytes(it) }

    fun hash(senha: CharArray, salt: ByteArray, iteracoes: Int = ITERACOES_PADRAO): ByteArray {
        val spec = PBEKeySpec(senha, salt, iteracoes, TAMANHO_CHAVE_BITS)
        try {
            return SecretKeyFactory.getInstance(ALGORITMO).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    /** Comparação em tempo constante para não vazar informação por timing. */
    fun verificar(senha: CharArray, salt: ByteArray, hashEsperado: ByteArray, iteracoes: Int): Boolean =
        MessageDigest.isEqual(hash(senha, salt, iteracoes), hashEsperado)
}
