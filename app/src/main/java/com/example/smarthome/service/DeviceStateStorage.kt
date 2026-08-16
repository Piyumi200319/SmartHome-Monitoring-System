package com.example.smarthome.service

import android.content.Context
import com.example.smarthome.models.Device

object DeviceStateStorage {

    private const val PREF_NAME =
        "smart_home_device_states"

    private lateinit var preferences:
            android.content.SharedPreferences

    // ==================================================
    // INITIALIZE
    // ==================================================

    fun initialize(context: Context) {

        preferences =
            context.applicationContext.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
            )
    }

    // ==================================================
    // CHECK INITIALIZED
    // ==================================================

    private fun isInitialized(): Boolean {
        return ::preferences.isInitialized
    }

    // ==================================================
    // SAVE ONE DEVICE
    // ==================================================

    fun saveDevice(device: Device) {

        if (!isInitialized()) return

        /*
         * IMPORTANT:
         *
         * If the Iron is OFF, never save its old
         * countdown value.
         *
         * OFF = timer 0
         */

        val timerToSave =
            if (device.type.equals("Iron", ignoreCase = true) && !device.isOn) {
                0
            } else {
                device.timer
            }

        preferences.edit()

            // --------------------------------------------------
            // BASIC
            // --------------------------------------------------

            .putBoolean(
                "${device.id}_isOn",
                device.isOn
            )

            .putString(
                "${device.id}_status",
                device.status
            )

            // --------------------------------------------------
            // LIGHT
            // --------------------------------------------------

            .putInt(
                "${device.id}_brightness",
                device.brightness
            )

            // --------------------------------------------------
            // IRON
            // --------------------------------------------------

            .putInt(
                "${device.id}_timer",
                timerToSave
            )

            .putInt(
                "${device.id}_temperature",
                device.temperature
            )

            .putInt(
                "${device.id}_targetTemperature",
                device.targetTemperature
            )

            .putBoolean(
                "${device.id}_heating",
                device.heating
            )

            .putString(
                "${device.id}_safetyMode",
                device.safetyMode
            )

            // --------------------------------------------------
            // CAMERA
            // --------------------------------------------------

            .putBoolean(
                "${device.id}_recording",
                device.recording
            )

            .putBoolean(
                "${device.id}_motionDetection",
                device.motionDetection
            )

            .putBoolean(
                "${device.id}_nightVision",
                device.nightVision
            )

            // --------------------------------------------------
            // MULTI SWITCH
            // --------------------------------------------------

            .putBoolean(
                "${device.id}_switch1",
                device.switch1
            )

            .putBoolean(
                "${device.id}_switch2",
                device.switch2
            )

            .putBoolean(
                "${device.id}_switch3",
                device.switch3
            )

            // --------------------------------------------------
            // ELECTRICAL INFORMATION
            // --------------------------------------------------

            .putInt(
                "${device.id}_power",
                device.power
            )

            .putInt(
                "${device.id}_voltage",
                device.voltage
            )

            .putFloat(
                "${device.id}_current",
                device.current.toFloat()
            )

            .putFloat(
                "${device.id}_energyToday",
                device.energyToday.toFloat()
            )

            .apply()
    }

    // ==================================================
    // RESTORE ONE DEVICE
    // ==================================================

    fun restoreDevice(device: Device) {

        if (!isInitialized()) return

        val hasSavedState =
            preferences.contains(
                "${device.id}_isOn"
            )

        if (!hasSavedState) {
            return
        }

        // --------------------------------------------------
        // BASIC
        // --------------------------------------------------

        device.isOn =
            preferences.getBoolean(
                "${device.id}_isOn",
                device.isOn
            )

        device.status =
            preferences.getString(
                "${device.id}_status",
                device.status
            ) ?: device.status

        // --------------------------------------------------
        // LIGHT
        // --------------------------------------------------

        device.brightness =
            preferences.getInt(
                "${device.id}_brightness",
                device.brightness
            )

        // --------------------------------------------------
        // IRON
        // --------------------------------------------------

        val savedTimer =
            preferences.getInt(
                "${device.id}_timer",
                0
            )

        /*
         * IMPORTANT:
         *
         * An Iron that is OFF must always have
         * timer = 0.
         *
         * This also fixes old saved data such as
         * timer = 50 while the Iron is OFF.
         */

        device.timer =
            if (device.type.equals("Iron", ignoreCase = true) && !device.isOn) {
                0
            } else {
                savedTimer
            }

        device.temperature =
            preferences.getInt(
                "${device.id}_temperature",
                device.temperature
            )

        device.targetTemperature =
            preferences.getInt(
                "${device.id}_targetTemperature",
                device.targetTemperature
            )

        device.heating =
            preferences.getBoolean(
                "${device.id}_heating",
                device.heating
            )

        device.safetyMode =
            preferences.getString(
                "${device.id}_safetyMode",
                device.safetyMode
            ) ?: device.safetyMode

        // --------------------------------------------------
        // CAMERA
        // --------------------------------------------------

        device.recording =
            preferences.getBoolean(
                "${device.id}_recording",
                device.recording
            )

        device.motionDetection =
            preferences.getBoolean(
                "${device.id}_motionDetection",
                device.motionDetection
            )

        device.nightVision =
            preferences.getBoolean(
                "${device.id}_nightVision",
                device.nightVision
            )

        // --------------------------------------------------
        // MULTI SWITCH
        // --------------------------------------------------

        device.switch1 =
            preferences.getBoolean(
                "${device.id}_switch1",
                device.switch1
            )

        device.switch2 =
            preferences.getBoolean(
                "${device.id}_switch2",
                device.switch2
            )

        device.switch3 =
            preferences.getBoolean(
                "${device.id}_switch3",
                device.switch3
            )

        // --------------------------------------------------
        // ELECTRICAL INFORMATION
        // --------------------------------------------------

        device.power =
            preferences.getInt(
                "${device.id}_power",
                device.power
            )

        device.voltage =
            preferences.getInt(
                "${device.id}_voltage",
                device.voltage
            )

        device.current =
            preferences.getFloat(
                "${device.id}_current",
                device.current.toFloat()
            ).toDouble()

        device.energyToday =
            preferences.getFloat(
                "${device.id}_energyToday",
                device.energyToday.toFloat()
            ).toDouble()

        // --------------------------------------------------
        // FIX IRON ELECTRICAL STATE
        // --------------------------------------------------

        if (device.type.equals("Iron", ignoreCase = true) && !device.isOn) {

            device.timer = 0
            device.power = 0
            device.current = 0.0
            device.heating = false
            device.safetyMode = "SAFE"
            device.status = "OFF"
        }
    }

    // ==================================================
    // RESTORE ALL DEVICES
    // ==================================================

    fun restoreAllDevices(
        devices: List<Device>
    ) {

        if (!isInitialized()) return

        for (device in devices) {
            restoreDevice(device)
        }
    }

    // ==================================================
    // INITIAL LIGHT STATE
    // ==================================================

    fun hasInitialLightState(deviceId: String): Boolean {

        if (!isInitialized()) return false

        return preferences.getBoolean(
            "${deviceId}_initial_light_state_set",
            false
        )
    }

    fun markInitialLightState(deviceId: String) {

        if (!isInitialized()) return

        preferences.edit()
            .putBoolean(
                "${deviceId}_initial_light_state_set",
                true
            )
            .apply()
    }

    // ==================================================
    // CLEAR SAVED DATA
    // ==================================================

    fun clearAll() {

        if (!isInitialized()) return

        preferences.edit()
            .clear()
            .apply()
    }
}