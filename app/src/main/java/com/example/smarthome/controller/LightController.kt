package com.example.smarthome.controller

import com.example.smarthome.models.Device
import com.example.smarthome.service.NotificationService

class LightController {

    // =========================================================
    // TURN ON
    // =========================================================

    fun turnOn(device: Device) {

        device.isOn = true
        device.status = "ON"

        updateElectricalInfo(device)

        NotificationService.addNotification(
            "Light",
            "${device.name} turned ON"
        )
    }


    // =========================================================
    // TURN OFF
    // =========================================================

    fun turnOff(device: Device) {

        device.isOn = false
        device.status = "OFF"

        // Keep brightness value.
        // Only electrical consumption becomes zero.
        device.power = 0
        device.current = 0.0

        NotificationService.addNotification(
            "Light",
            "${device.name} turned OFF"
        )
    }


    // =========================================================
    // CHANGE BRIGHTNESS
    // =========================================================

    fun changeBrightness(
        device: Device,
        brightness: Int
    ) {

        // The UI enables brightness from the displayed status.
        // Keep isOn/status synchronized so a stale isOn value can
        // never cause a brightness change to write the light OFF.
        if (!device.isOn && device.status.equals("ON", ignoreCase = true)) {
            device.isOn = true
        }

        if (!device.isOn) {
            return
        }

        device.status = "ON"

        device.brightness =
            brightness.coerceIn(0, 100)

        updateElectricalInfo(device)

        NotificationService.addNotification(
            "Light",
            "${device.name} brightness changed to ${device.brightness}%"
        )
    }


    // =========================================================
    // UPDATE ELECTRICAL INFORMATION
    // =========================================================

    private fun updateElectricalInfo(
        device: Device
    ) {

        device.power =
            (device.brightness * 15) / 100

        device.current =
            if (device.voltage > 0) {

                device.power.toDouble() /
                        device.voltage.toDouble()

            } else {

                0.0
            }

        device.energyToday =
            device.power / 1000.0
    }
}