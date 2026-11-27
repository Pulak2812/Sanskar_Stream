package com.example.sanskarstream.domain

data class Stream(
    val id: String,
    val title: String,
    val url: String,
    val logo: String? = null,
)
