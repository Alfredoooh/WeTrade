package com.wesports.app.model

data class Tweak(
    val id: String,
    val name: String,
    val message: String,
    val mode: String,
    val type: String,
    val sni: String = "",
    val payload: String = "",
    val expirationDate: Boolean = false,
    val hwid: Boolean = false,
    val passwordLock: Boolean = false,
    val mobileDataOnly: Boolean = false,
    val blockRooted: Boolean = false
)