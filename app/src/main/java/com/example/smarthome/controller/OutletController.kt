package com.example.smarthome.controller

import com.example.smarthome.firebase.FirebaseRepository
import com.example.smarthome.models.Device

class OutletController {

    private val firebaseRepository =
        FirebaseRepository()

    // ==========================================
    // TURN ON
    // ==========================================

    fun turnOn(device: Device) {

        device.isOn = true
        device.status = "ON"

        // Example outlet power
        device.power = 100
        device.current =
            if (device.voltage > 0) {
                device.power.toDouble() /
                        device.voltage.toDouble()
            } else {
                0.0
            }

        firebaseRepository.updateDeviceState(
            device.id,
            mapOf(
                "isOn" to true,
                "status" to "ON",
                "power" to device.power,
                "current" to device.current
            )
        )
    }

    // ==========================================
    // TURN OFF
    // ==========================================

    fun turnOff(device: Device) {

        device.isOn = false
        device.status = "OFF"
        device.power = 0
        device.current = 0.0

        firebaseRepository.updateDeviceState(
            device.id,
            mapOf(
                "isOn" to false,
                "status" to "OFF",
                "power" to 0,
                "current" to 0.0
            )
        )
    }
}