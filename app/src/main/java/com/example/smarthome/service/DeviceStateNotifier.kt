package com.example.smarthome.service

import com.example.smarthome.models.Device

object DeviceStateNotifier {

    private val listeners =
        mutableSetOf<(Device) -> Unit>()

    fun registerListener(
        listener: (Device) -> Unit
    ) {
        listeners.add(listener)
    }

    fun unregisterListener(
        listener: (Device) -> Unit
    ) {
        listeners.remove(listener)
    }

    fun notifyDeviceChanged(
        device: Device
    ) {
        listeners.toList().forEach { listener ->

            listener(device)
        }
    }
}