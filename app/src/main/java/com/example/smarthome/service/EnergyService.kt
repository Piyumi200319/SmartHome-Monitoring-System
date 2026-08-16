package com.example.smarthome.service

import com.example.smarthome.models.Device

class EnergyService {

    // ==================================================
    // CALCULATE POWER
    // ==================================================

    fun calculatePower(
        temperature: Int
    ): Int {

        return when {

            temperature <= 80 -> 600

            temperature <= 120 -> 900

            temperature <= 160 -> 1200

            temperature <= 200 -> 1500

            else -> 1800
        }
    }

    // ==================================================
    // CALCULATE CURRENT
    // ==================================================

    fun calculateCurrent(
        power: Int,
        voltage: Int
    ): Double {

        val safeVoltage = if (voltage > 0) voltage else 230

        return power.toDouble() / safeVoltage.toDouble()
    }

    // ==================================================
    // CALCULATE ENERGY
    // ==================================================

    fun calculateEnergy(
        power: Int,
        minutes: Int
    ): Double {

        return (power / 1000.0) *
                (minutes / 60.0)
    }

    // ==================================================
    // UPDATE DEVICE ELECTRICAL INFORMATION
    // ==================================================

    fun updateDeviceElectricalInfo(
        device: Device
    ) {

        if (device.voltage <= 0) {
            device.voltage = 230
        }

        val normalizedType = device.type.trim().lowercase()

        when (normalizedType) {

            // ------------------------------------------
            // LIGHT
            // ------------------------------------------

            "light" -> {

                if (device.isOn) {

                    // Maximum light power = 15 W
                    device.power =
                        (15 * device.brightness) / 100

                } else {

                    device.power = 0
                }
            }

            // ------------------------------------------
            // IRON
            // ------------------------------------------

            "iron" -> {

                if (device.isOn) {

                    val temp = if (device.temperature > 0) device.temperature else 120
                    device.power =
                        calculatePower(
                            temp
                        )

                } else {

                    device.power = 0
                }
            }

            // ------------------------------------------
            // OUTLET
            // ------------------------------------------

            "outlet" -> {

                if (device.isOn) {

                    device.power = 250

                } else {

                    device.power = 0
                }
            }

            // ------------------------------------------
            // CAMERA
            // ------------------------------------------

            "camera" -> {

                if (device.isOn) {

                    device.power = 12

                } else {

                    device.power = 0
                }
            }

            // ------------------------------------------
            // MULTI SWITCH
            // ------------------------------------------

            "switch" -> {

                if (device.isOn) {

                    var switchPower = 0

                    if (device.switch1) {
                        switchPower += 1
                    }

                    if (device.switch2) {
                        switchPower += 1
                    }

                    if (device.switch3) {
                        switchPower += 1
                    }

                    device.power = if (switchPower > 0) switchPower else 3

                } else {

                    device.power = 0
                }
            }

            else -> {

                if (!device.isOn) {
                    device.power = 0
                }
            }
        }

        // ------------------------------------------
        // CURRENT
        // ------------------------------------------

        device.current =
            calculateCurrent(
                device.power,
                device.voltage
            )
    }
}