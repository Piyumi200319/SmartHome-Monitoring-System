package com.example.smarthome.controller

import com.example.smarthome.firebase.FirebaseRepository
import com.example.smarthome.models.Device
import com.example.smarthome.service.NotificationService

class CameraController {

    private val firebaseRepository =
        FirebaseRepository()

    // ==========================================
    // RECORDING
    // ==========================================

    fun toggleRecording(device: Device) {

        device.recording =
            !device.recording

        firebaseRepository.updateDeviceState(
            device.id,
            mapOf(
                "recording" to device.recording
            )
        )

        NotificationService.addNotification(
            "Camera",
            if (device.recording)
                "${device.name} recording started"
            else
                "${device.name} recording stopped"
        )
    }

    // ==========================================
    // MOTION DETECTION
    // ==========================================

    fun toggleMotion(device: Device) {

        device.motionDetection =
            !device.motionDetection

        firebaseRepository.updateDeviceState(
            device.id,
            mapOf(
                "motionDetection" to
                        device.motionDetection
            )
        )

        NotificationService.addNotification(
            "Camera",
            if (device.motionDetection)
                "${device.name} motion detection enabled"
            else
                "${device.name} motion detection disabled"
        )
    }

    // ==========================================
    // NIGHT VISION
    // ==========================================

    fun toggleNightVision(device: Device) {

        device.nightVision =
            !device.nightVision

        firebaseRepository.updateDeviceState(
            device.id,
            mapOf(
                "nightVision" to
                        device.nightVision
            )
        )

        NotificationService.addNotification(
            "Camera",
            if (device.nightVision)
                "${device.name} night vision enabled"
            else
                "${device.name} night vision disabled"
        )
    }
}