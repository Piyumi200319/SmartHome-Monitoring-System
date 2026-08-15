package com.example.smarthome.controller

import com.example.smarthome.firebase.FirebaseRepository
import android.os.CountDownTimer
import com.example.smarthome.models.Device
import com.example.smarthome.service.DeviceStateStorage
import com.example.smarthome.service.NotificationService

object IronController {

    private val firebaseRepository = FirebaseRepository()
    interface IronListener {

        fun onTick(device: Device)

        fun onFinished(device: Device)
    }

    private val timers =
        mutableMapOf<String, CountDownTimer>()

    // ==================================================
    // START NEW IRON CYCLE
    // ==================================================

    fun startIron(
        device: Device,
        listener: IronListener
    ) {

        timers[device.id]?.cancel()

        device.timer = 120
        device.isOn = true
        device.status = "ON"
        device.heating = true
        device.safetyMode = "SAFE"

        device.power = 1200

        device.current =
            if (device.voltage > 0) {
                device.power.toDouble() /
                        device.voltage.toDouble()
            } else {
                0.0
            }

        DeviceStateStorage.saveDevice(device)

        firebaseRepository.turnIronOn(
            device,
            onSuccess = {
                // Firebase successfully updated
            },
            onError = {
                // Handle Firebase error if needed
            }
        )
        NotificationService.addNotification(
            "Iron",
            "${device.name} turned ON"
        )

        createTimer(
            device,
            listener
        )
    }

    // ==================================================
    // RESUME EXISTING IRON
    // ==================================================

    fun resumeIron(
        device: Device,
        listener: IronListener
    ) {

        if (!device.isOn || device.timer <= 0) {
            return
        }

        timers[device.id]?.cancel()

        device.status = "ON"
        device.heating = true
        device.safetyMode = "SAFE"

        device.power = 1200

        device.current =
            if (device.voltage > 0) {
                device.power.toDouble() /
                        device.voltage.toDouble()
            } else {
                0.0
            }

        DeviceStateStorage.saveDevice(device)

        createTimer(
            device,
            listener
        )
    }

    // ==================================================
    // CREATE TIMER
    // ==================================================

    private fun createTimer(
        device: Device,
        listener: IronListener
    ) {

        if (device.timer <= 0) {
            return
        }

        val timer =
            object : CountDownTimer(
                device.timer * 1000L,
                1000L
            ) {

                override fun onTick(
                    millisUntilFinished: Long
                ) {

                    if (!device.isOn) {

                        cancel()

                        timers.remove(
                            device.id
                        )

                        return
                    }

                    device.timer =
                        (millisUntilFinished / 1000L)
                            .toInt()

                    // Save locally
                    DeviceStateStorage.saveDevice(device)

                    listener.onTick(device)
                }

                override fun onFinish() {

                    timers.remove(device.id)

                    if (!device.isOn) {
                        return
                    }

                    device.isOn = false
                    device.status = "OFF"
                    device.timer = 0
                    device.heating = false
                    device.power = 0
                    device.current = 0.0
                    device.safetyMode = "AUTO SHUTDOWN"

                    // Save locally
                    DeviceStateStorage.saveDevice(device)

                    // Update Firebase
                    firebaseRepository.updateDeviceState(
                        device.id,
                        mapOf(
                            "isOn" to false,
                            "status" to "OFF",
                            "timer" to 0,
                            "heating" to false,
                            "power" to 0,
                            "current" to 0.0,
                            "safetyMode" to "AUTO SHUTDOWN"
                        )
                    )

                    NotificationService.addNotification(
                        "Iron",
                        "${device.name} automatically switched OFF"
                    )

                    listener.onFinished(device)
                }
            }

        timers[device.id] = timer

        timer.start()
    }

    // ==================================================
    // STOP IRON
    // ==================================================

    fun stopIron(
        device: Device
    ) {

        timers[device.id]?.cancel()

        timers.remove(
            device.id
        )

        device.isOn = false
        device.status = "OFF"
        device.timer = 0
        device.heating = false
        device.power = 0
        device.current = 0.0
        device.safetyMode = "SAFE"

        DeviceStateStorage.saveDevice(
            device
        )

        firebaseRepository.turnIronOff(
            device,
            safetyShutdown = false
        )

        NotificationService.addNotification(
            "Iron",
            "${device.name} turned OFF"
        )
    }

    // ==================================================
    // CHECK RUNNING
    // ==================================================

    fun isRunning(
        device: Device
    ): Boolean {

        return timers.containsKey(
            device.id
        )
    }
}