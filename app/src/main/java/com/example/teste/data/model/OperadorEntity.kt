package com.example.teste.data.model

/**
 * Conta de operador armazenada no banco local.
 * A senha nunca é persistida em texto puro: apenas o hash PBKDF2 e o salt (Base64).
 */
data class OperadorEntity(
    val id: Long = 0,
    val usuario: String,
    val nomeCompleto: String,
    val senhaHash: String,
    val senhaSalt: String,
    val iteracoes: Int,
    val dataCriacao: String
)
