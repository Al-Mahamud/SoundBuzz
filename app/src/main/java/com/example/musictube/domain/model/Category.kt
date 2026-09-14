package com.example.musictube.domain.model

data class Category(
    val id: String,
    val name: String,
    val description: String = "",
    val primaryColor: Long = 0xFF6200EE,
    val secondaryColor: Long = 0xFF03DAC5
)
