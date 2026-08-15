package com.example.smarthome.models

data class Floor(

    val floorName: String,

    val rooms: MutableList<Room>,

    val firebaseId: String = "",

    val icon: String = ""
)
