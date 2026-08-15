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

        if (voltage <= 0) {
            return 0.0
        }

        return power.toDouble() / voltage.toDouble()
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

        when (device.type) {

            // ------------------------------------------
            // LIGHT
            // ------------------------------------------

            "Light" -> {

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

            "Iron" -> {

                if (device.isOn) {

                    device.power =
                        calculatePower(
                            device.temperature
                        )

                } else {

                    device.power = 0
                }
            }

            // ------------------------------------------
            // OUTLET
            // ------------------------------------------

            "Outlet" -> {

                if (device.isOn) {

                    device.power = 250

                } else {

                    device.power = 0
                }
            }

            // ------------------------------------------
            // CAMERA
            // ------------------------------------------

            "Camera" -> {

                if (device.isOn) {

                    device.power = 12

                } else {

                    device.power = 0
                }
            }

            // ------------------------------------------
            // MULTI SWITCH
            // ------------------------------------------

            "Switch" -> {

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

                device.power = switchPower
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