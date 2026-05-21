package com.wesports.app.model

data class Server(
    val ip: String,
    val country: String,
    val ping: String,
    val speed: String,
    val ovpn: String,
    val port: Int = 443,
    val type: String = "SSL"
)