package com.example.smarthome.controller

import com.example.smarthome.firebase.FirebaseRepository
import com.example.smarthome.models.Device

class SwitchController {

    private val firebaseRepository =
        FirebaseRepository()

    // ==========================================
    // TURN ON
    // ==========================================

    fun turnOn(device: Device) {

        device.isOn = true
        device.status = "ON"

        device.switch1 = true
        device.switch2 = true
        device.switch3 = true

        calculatePower(device)

        saveToFirebase(device)
    }

    // ==========================================
    // TURN OFF
    // ==========================================

    fun turnOff(device: Device) {

        device.isOn = false
        device.status = "OFF"

        device.switch1 = false
        device.switch2 = false
        device.switch3 = false

        calculatePower(device)

        saveToFirebase(device)
    }

    // ==========================================
    // SWITCH 1
    // ==========================================

    fun toggleSwitch1(device: Device) {

        device.switch1 =
            !device.switch1

        updateDeviceState(device)
    }

    // ==========================================
    // SWITCH 2
    // ==========================================

    fun toggleSwitch2(device: Device) {

        device.switch2 =
            !device.switch2

        updateDeviceState(device)
    }

    // ==========================================
    // SWITCH 3
    // ==========================================

    fun toggleSwitch3(device: Device) {

        device.switch3 =
            !device.switch3

        updateDeviceState(device)
    }

    // ==========================================
    // UPDATE DEVICE
    // ==========================================

    private fun updateDeviceState(
        device: Device
    ) {

        device.isOn =
            device.switch1 ||
                    device.switch2 ||
                    device.switch3

        device.status =
            if (device.isOn)
                "ON"
            else
                "OFF"

        calculatePower(device)

        saveToFirebase(device)
    }

    // ==========================================
    // CALCULATE POWER
    // ==========================================

    private fun calculatePower(
        device: Device
    ) {

        var power = 0

        if (device.switch1) {
            power += 1
        }

        if (device.switch2) {
            power += 1
        }

        if (device.switch3) {
            power += 1
        }

        device.power = power

        device.isOn =
            device.switch1 ||
                    device.switch2 ||
                    device.switch3

        device.status =
            if (device.isOn)
                "ON"
            else
                "OFF"

        device.current =
            if (device.voltage > 0) {

                device.power.toDouble() /
                        device.voltage.toDouble()

            } else {

                0.0
            }
    }

    // ==========================================
    // FIREBASE
    // ==========================================

    private fun saveToFirebase(
        device: Device
    ) {

        firebaseRepository.updateDeviceState(
            device.id,
            mapOf(
                "isOn" to device.isOn,
                "status" to device.status,
                "switch1" to device.switch1,
                "switch2" to device.switch2,
                "switch3" to device.switch3,
                "power" to device.power,
                "current" to device.current
            )
        )
    }
}