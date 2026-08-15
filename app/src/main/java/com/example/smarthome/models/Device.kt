package com.example.smarthome.models

import java.io.Serializable

data class Device(

    val id: String,

    var name: String,

    var type: String,

    var status: String,

    var isOn: Boolean,

    // -------------------------------
    // Iron
    // -------------------------------

    var timer: Int = 120,

    var maxTime: Int = 120,

    var temperature: Int = 120,

    // -------------------------------
    // Light
    // -------------------------------

    var brightness: Int = 80,

    // -------------------------------
    // Schedule
    // -------------------------------

    var scheduleAction: String = "",

    var scheduleDueAt: Long = 0L,

    var scheduleRemaining: Int = 0,

    // -------------------------------
    // Camera
    // -------------------------------

    var recording: Boolean = false,

    var motionDetection: Boolean = true,

    var nightVision: Boolean = false,

    var fps: Int = 30,

    var resolution: String = "1080P",

    // -------------------------------
    // Multi Switch
    // -------------------------------

    var switch1: Boolean = true,

    var switch2: Boolean = false,

    var switch3: Boolean = true,

    // -------------------------------
    // Iron temperature
    // -------------------------------

    var targetTemperature: Int = 120,

    var heating: Boolean = false,

    var safetyMode: String = "SAFE",

    // -------------------------------
    // Electrical information
    // -------------------------------

    var power: Int = 0,

    var voltage: Int = 230,

    var current: Double = 0.0,

    var energyToday: Double = 0.0

) : Serializable