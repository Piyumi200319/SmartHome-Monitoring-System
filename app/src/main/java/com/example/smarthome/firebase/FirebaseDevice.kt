package com.example.smarthome.firebase

import com.google.firebase.Timestamp

data class FirebaseDevice(

    var id: String = "",

    var name: String = "",

    var type: String = "",

    var status: String = "",

    var isOn: Boolean = false,

    var roomId: String = "",

    // --------------------------------
    // Backend Safety
    // --------------------------------

    var turnedOnAt: Timestamp? = null,

    var lastSafetyEvent: String = "",

    var lastSafetyEventAt: Timestamp? = null,

    // --------------------------------
    // Iron
    // --------------------------------

    var timer: Int = 120,

    var maxTime: Int = 120,

    var temperature: Int = 120,

    var targetTemperature: Int = 120,

    var heating: Boolean = false,

    var safetyMode: String = "SAFE",

    // --------------------------------
    // Light
    // --------------------------------

    var brightness: Int = 80,

    // --------------------------------
    // Schedule
    // --------------------------------

    var scheduleAction: String = "",

    var scheduleDueAt: Long = 0L,

    var scheduleRemaining: Int = 0,

    // --------------------------------
    // Camera
    // --------------------------------

    var recording: Boolean = false,

    var motionDetection: Boolean = true,

    var nightVision: Boolean = false,

    var fps: Int = 30,

    var resolution: String = "1080P",

    // --------------------------------
    // Multi Switch
    // --------------------------------

    var switch1: Boolean = false,

    var switch2: Boolean = false,

    var switch3: Boolean = false,

    // --------------------------------
    // Electrical
    // --------------------------------

    var power: Int = 0,

    var voltage: Int = 230,

    var current: Double = 0.0,

    var energyToday: Double = 0.0
)