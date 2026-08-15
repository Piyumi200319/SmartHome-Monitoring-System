package com.example.smarthome.models

data class Room(
    val roomName: String = "",
    val devices: MutableList<Device> = mutableListOf(),
    val firebaseId: String = "",
    val icon: String = ""
)
