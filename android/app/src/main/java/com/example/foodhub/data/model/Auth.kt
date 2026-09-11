package com.example.foodhub.data.model

data class RegisterRequest(
    val email: String,
    val username: String,
    val password: String
)

data class LoginRequest(
    val email: String,
    val password: String
)


data class User(
    val id:Int,
    val email: String,
    val username: String
)
data class AuthResponse(
    val token: String,
    val message: String,
    val user: User
)