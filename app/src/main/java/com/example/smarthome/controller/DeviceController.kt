package com.example.smarthome.controller

import com.example.smarthome.firebase.FirebaseRepository
import com.example.smarthome.models.Device
import com.example.smarthome.service.DeviceStateNotifier
import com.example.smarthome.service.DeviceStateStorage
import com.example.smarthome.service.NotificationService

class DeviceController {

    private val firebaseRepository = FirebaseRepository()

    fun turnOn(device: Device) {

        device.isOn = true
        device.status = "ON"

        when (device.type.trim().lowercase()) {

            "light" -> {

                device.power =
                    (device.brightness * 15) / 100
            }

            "outlet" -> {

                device.power = 250
            }

            "camera" -> {

                device.power = 12
            }

            "switch" -> {

                device.switch1 = true
                device.switch2 = true
                device.switch3 = true

                device.power = 3
            }

            "iron" -> {

                device.power = 1200
            }
        }


        device.current =
            if (device.voltage > 0) {

                device.power.toDouble() /
                        device.voltage.toDouble()

            } else {

                0.0
            }


        DeviceStateStorage.saveDevice(device)

        DeviceStateNotifier.notifyDeviceChanged(device)

        firebaseRepository.updateDeviceState(
            device.id,
            mapOf(
                "isOn" to device.isOn,
                "status" to device.status,
                "power" to device.power,
                "current" to device.current,
                "switch1" to device.switch1,
                "switch2" to device.switch2,
                "switch3" to device.switch3
            )
        )

        NotificationService.addNotification(
            device.type,
            "${device.name} turned ON"
        )
    }

    fun turnOff(device: Device) {

        device.isOn = false
        device.status = "OFF"


        if (
            device.type.trim()
                .lowercase() == "switch"
        ) {

            device.switch1 = false
            device.switch2 = false
            device.switch3 = false
        }


        // IMPORTANT:
        // Do not change brightness here.
        // The next time the light is ON, it should
        // return to the previous brightness.

        device.power = 0
        device.current = 0.0


        DeviceStateStorage.saveDevice(device)

        DeviceStateNotifier.notifyDeviceChanged(device)

        firebaseRepository.updateDeviceState(
            device.id,
            mapOf(
                "isOn" to device.isOn,
                "status" to device.status,
                "power" to device.power,
                "current" to device.current,
                "switch1" to device.switch1,
                "switch2" to device.switch2,
                "switch3" to device.switch3
            )
        )

        NotificationService.addNotification(
            device.type,
            "${device.name} turned OFF"
        )
    }

    fun changeTemperature(
        device: Device,
        temperature: Int
    ) {

        if (device.isOn) {

            device.temperature = temperature

            DeviceStateStorage.saveDevice(device)

            DeviceStateNotifier.notifyDeviceChanged(device)
        }
    }
}