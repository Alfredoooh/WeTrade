package com.wesports.app.model

data class Tweak(
    val id: String,
    val name: String,
    val message: String,
    val mode: String,
    val type: String,
    val expirationDate: Boolean,
    val hwid: Boolean,
    val passwordLock: Boolean,
    val mobileDataOnly: Boolean,
    val blockRooted: Boolean
)