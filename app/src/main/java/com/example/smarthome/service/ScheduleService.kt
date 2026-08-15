package com.example.smarthome.service


import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.smarthome.firebase.FirebaseRepository
import com.example.smarthome.models.Device

/**
 * Application-wide scheduler.
 *
 * Responsibilities:
 * 1. Keep scheduled tasks alive when an Activity is destroyed.
 * 2. Persist pending schedules in SharedPreferences.
 * 3. Restore pending schedules when a Device is loaded again.
 * 4. Change the actual Device object when the schedule executes.
 * 5. Save the new state locally.
 * 6. Update Firebase with the new state.
 * 7. Notify other parts of the application.
 */
object ScheduleService {

    private val firebaseRepository =
        FirebaseRepository()
    private const val TAG = "ScheduleService"

    private const val PREF_NAME =
        "smart_home_schedules"

    private const val KEY_PREFIX =
        "schedule_"

    private lateinit var preferences:
            android.content.SharedPreferences

    private val handler =
        Handler(Looper.getMainLooper())


    /**
     * Currently scheduled Runnable for each device.
     */
    private val scheduledTasks =
        mutableMapOf<String, Runnable>()

    /**
     * Scheduled execution time for each device.
     */
    private val dueTimes =
        mutableMapOf<String, Long>()

    /**
     * Scheduled action for each device.
     *
     * Values:
     * ON
     * OFF
     */
    private val actions =
        mutableMapOf<String, String>()

    /** Separate 1-second countdown updater for persisted DB state. */
    private val countdownTasks =
        mutableMapOf<String, Runnable>()


    // ==========================================================
    // INITIALIZE
    // ==========================================================

    fun initialize(context: Context) {

        if (::preferences.isInitialized) {
            return
        }

        preferences =
            context.applicationContext.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
            )

        Log.d(
            TAG,
            "ScheduleService initialized"
        )
    }


    private fun ready(): Boolean {

        return ::preferences.isInitialized
    }


    // ==========================================================
    // SCHEDULE ON
    // ==========================================================

    fun scheduleTurnOn(
        device: Device,
        delayMillis: Long,
        onComplete: () -> Unit = {}
    ) {

        schedule(
            device = device,
            action = "ON",
            delayMillis = delayMillis,
            onComplete = onComplete
        )
    }


    // ==========================================================
    // SCHEDULE OFF
    // ==========================================================

    fun scheduleTurnOff(
        device: Device,
        delayMillis: Long,
        onComplete: () -> Unit = {}
    ) {

        schedule(
            device = device,
            action = "OFF",
            delayMillis = delayMillis,
            onComplete = onComplete
        )
    }


    // ==========================================================
    // CREATE SCHEDULE
    // ==========================================================

    private fun schedule(
        device: Device,
        action: String,
        delayMillis: Long,
        onComplete: () -> Unit
    ) {

        /*
         * Remove any previous schedule for this device.
         */
        cancelScheduleInternal(device, clearRemote = false)

        val safeDelay =
            delayMillis.coerceAtLeast(0L)

        val dueAt =
            System.currentTimeMillis() + safeDelay


        /*
         * Keep schedule in memory.
         */
        dueTimes[device.id] =
            dueAt

        actions[device.id] =
            action

        device.scheduleAction = action
        device.scheduleDueAt = dueAt
        device.scheduleRemaining =
            ((safeDelay + 999L) / 1000L).toInt()


        /*
         * IMPORTANT:
         *
         * Persist the schedule so that it survives
         * Activity destruction/navigation.
         */
        if (ready()) {

            preferences.edit()
                .putLong(
                    "${KEY_PREFIX}${device.id}_due",
                    dueAt
                )
                .putString(
                    "${KEY_PREFIX}${device.id}_action",
                    action
                )
                .apply()
        }

        // Persist the schedule in Firestore as well. This makes the
        // remaining time visible in the database and lets a reopened
        // detail screen reconstruct the same countdown.
        updateScheduleInFirebase(device)


        Log.d(
            TAG,
            "Scheduled $action for ${device.name} " +
                    "after ${safeDelay} ms"
        )


        /*
         * Start the actual timer.
         */
        post(
            device = device,
            action = action,
            delayMillis = safeDelay,
            onComplete = onComplete
        )
    }


    // ==========================================================
    // ATTACH DEVICE
    // ==========================================================

    /**
     * Call this whenever a Device is loaded from Firebase.
     *
     * If the device already has a pending schedule, reconnect
     * the schedule to the newly-created Device object.
     */
    fun attachDevice(
        device: Device,
        onComplete: () -> Unit = {}
    ) {

        if (!ready()) {
            Log.w(
                TAG,
                "attachDevice called before initialize()"
            )
            return
        }


        val preferenceDue =
            preferences.getLong(
                "${KEY_PREFIX}${device.id}_due",
                0L
            )

        val preferenceAction =
            preferences.getString(
                "${KEY_PREFIX}${device.id}_action",
                null
            )

        // Prefer the Firestore schedule fields when present.
        // Fall back to SharedPreferences for schedules created by an
        // older build.
        val savedDue =
            if (device.scheduleDueAt > 0L) device.scheduleDueAt
            else preferenceDue

        val savedAction =
            if (device.scheduleAction.isNotBlank()) device.scheduleAction
            else preferenceAction

        if (savedDue <= 0L || savedAction.isNullOrBlank()) {
            return
        }

        device.scheduleDueAt = savedDue
        device.scheduleAction = savedAction

        val rawRemaining = savedDue - System.currentTimeMillis()
        val remaining = if (device.scheduleRemaining > 0 && Math.abs(rawRemaining - (device.scheduleRemaining * 1000L)) > 1500L) {
            device.scheduleRemaining * 1000L
        } else {
            rawRemaining
        }
        val actualDue = System.currentTimeMillis() + remaining.coerceAtLeast(0L)

        /*
         * Schedule already reached its execution time.
         */
        if (remaining <= 0L) {

            Log.d(
                TAG,
                "Restored schedule is already due: " +
                        "${device.name} -> $savedAction"
            )


            /*
             * Remove persisted schedule first so that
             * attachDevice cannot execute it repeatedly.
             */
            clearPersistedSchedule(
                device.id
            )

            device.scheduleAction = ""
            device.scheduleDueAt = 0L
            device.scheduleRemaining = 0

            scheduledTasks[device.id]
                ?.let(handler::removeCallbacks)

            scheduledTasks.remove(
                device.id
            )

            dueTimes.remove(
                device.id
            )

            actions.remove(
                device.id
            )


            /*
             * Apply action.
             */
            applyAction(
                device,
                savedAction
            )


            /*
             * Save locally.
             */
            DeviceStateStorage.saveDevice(
                device
            )


            /*
             * VERY IMPORTANT:
             *
             * Firebase must also receive the scheduled state.
             */
            updateDeviceInFirebase(
                device
            )

            clearScheduleInFirebase(device)


            /*
             * Notify UI.
             */
            DeviceStateNotifier.notifyDeviceChanged(
                device
            )


            onComplete()

            return
        }


        /*
         * Cancel an old in-memory Runnable.
         */
        scheduledTasks[device.id]
            ?.let(handler::removeCallbacks)


        /*
         * Attach the new Device object to the
         * existing schedule.
         */
        dueTimes[device.id] =
            actualDue

        actions[device.id] =
            savedAction

        device.scheduleRemaining =
            ((remaining + 999L) / 1000L).toInt()

        startCountdown(device)


        Log.d(
            TAG,
            "Restored schedule for ${device.name}: " +
                    "$savedAction in $remaining ms"
        )


        post(
            device = device,
            action = savedAction,
            delayMillis = remaining,
            onComplete = onComplete
        )
    }


    // ==========================================================
    // POST SCHEDULED TASK
    // ==========================================================
    private fun post(
        device: Device,
        action: String,
        delayMillis: Long,
        onComplete: () -> Unit
    ) {

        val task = Runnable {

            // ==========================================
            // APPLY SCHEDULED ACTION
            // ==========================================

            applyAction(
                device,
                action
            )

            // ==========================================
            // SAVE LOCAL STATE
            // ==========================================

            DeviceStateStorage.saveDevice(
                device
            )

            // ==========================================
            // UPDATE FIREBASE
            // ==========================================

            firebaseRepository.updateDeviceState(

                device.id,

                mapOf(
                    "isOn" to device.isOn,
                    "status" to device.status,
                    "power" to device.power,
                    "current" to device.current,
                    "energyToday" to device.energyToday,
                    "brightness" to device.brightness,
                    "timer" to device.timer,
                    "heating" to device.heating,
                    "safetyMode" to device.safetyMode,
                    "temperature" to device.temperature,
                    "switch1" to device.switch1,
                    "switch2" to device.switch2,
                    "switch3" to device.switch3,
                    "scheduleAction" to device.scheduleAction,
                    "scheduleDueAt" to device.scheduleDueAt,
                    "scheduleRemaining" to device.scheduleRemaining
                ),

                onSuccess = {

                    DeviceStateNotifier.notifyDeviceChanged(
                        device
                    )

                    NotificationService.addNotification(
                        device.type,
                        "${device.name} scheduled $action"
                    )

                    onComplete()
                },

                onError = { exception ->

                    // Still notify the local application
                    // so the UI reflects the actual local
                    // scheduled state.

                    DeviceStateNotifier.notifyDeviceChanged(
                        device
                    )

                    onComplete()
                }
            )

            // ==========================================
            // CLEAR SCHEDULE
            // ==========================================

            scheduledTasks.remove(
                device.id
            )

            dueTimes.remove(
                device.id
            )

            actions.remove(
                device.id
            )

            stopCountdown(device.id)

            device.scheduleAction = ""
            device.scheduleDueAt = 0L
            device.scheduleRemaining = 0

            clearPersistedSchedule(
                device.id
            )

            clearScheduleInFirebase(device)
        }

        scheduledTasks[device.id] =
            task

        startCountdown(device)

        handler.postDelayed(
            task,
            delayMillis.coerceAtLeast(0L)
        )
    }



    // ==========================================================
    // APPLY ACTION
    // ==========================================================

    private fun applyAction(
        device: Device,
        action: String
    ) {

        when (action.uppercase()) {

            // ==================================================
            // TURN ON
            // ==================================================

            "ON" -> {

                device.isOn = true

                device.status = "ON"


                /*
                 * ------------------------------
                 * IRON
                 * ------------------------------
                 */
                if (
                    device.type
                        .trim()
                        .equals(
                            "Iron",
                            ignoreCase = true
                        )
                ) {

                    device.timer = 120

                    device.heating =
                        true

                    device.safetyMode =
                        "SAFE"

                    device.power =
                        1200

                    device.current =
                        if (device.voltage > 0) {

                            device.power.toDouble() /
                                    device.voltage.toDouble()

                        } else {

                            0.0
                        }
                }


                /*
                 * ------------------------------
                 * SWITCH
                 * ------------------------------
                 */
                if (
                    device.type
                        .trim()
                        .equals(
                            "Switch",
                            ignoreCase = true
                        )
                ) {

                    device.switch1 =
                        true

                    device.switch2 =
                        true

                    device.switch3 =
                        true
                }


                /*
                 * LIGHT
                 *
                 * Do NOT change brightness here.
                 *
                 * A scheduled ON should preserve the
                 * user's existing brightness.
                 */
                if (
                    device.type
                        .trim()
                        .equals(
                            "Light",
                            ignoreCase = true
                        )
                ) {

                    /*
                     * Keep brightness unchanged.
                     */
                }
            }


            // ==================================================
            // TURN OFF
            // ==================================================

            "OFF" -> {

                device.isOn =
                    false

                device.status =
                    "OFF"


                /*
                 * ------------------------------
                 * IRON
                 * ------------------------------
                 */
                if (
                    device.type
                        .trim()
                        .equals(
                            "Iron",
                            ignoreCase = true
                        )
                ) {

                    device.timer =
                        0

                    device.heating =
                        false

                    device.safetyMode =
                        "SAFE"

                    device.power =
                        0

                    device.current =
                        0.0
                }


                /*
                 * ------------------------------
                 * SWITCH
                 * ------------------------------
                 */
                if (
                    device.type
                        .trim()
                        .equals(
                            "Switch",
                            ignoreCase = true
                        )
                ) {

                    device.switch1 =
                        false

                    device.switch2 =
                        false

                    device.switch3 =
                        false

                    device.power =
                        0

                    device.current =
                        0.0
                }


                /*
                 * ------------------------------
                 * LIGHT
                 * ------------------------------
                 *
                 * Brightness is intentionally preserved.
                 *
                 * Example:
                 *
                 * Brightness = 70%
                 *
                 * OFF schedule:
                 * isOn = false
                 * status = OFF
                 * brightness = 70%
                 *
                 * Later ON:
                 * isOn = true
                 * status = ON
                 * brightness = 70%
                 */
                if (
                    device.type
                        .trim()
                        .equals(
                            "Light",
                            ignoreCase = true
                        )
                ) {

                    device.power =
                        0

                    device.current =
                        0.0
                }
            }
        }


        /*
         * Recalculate electrical information.
         */
        EnergyService()
            .updateDeviceElectricalInfo(
                device
            )
    }


    // ==========================================================
    // FIREBASE UPDATE
    // ==========================================================

    private fun updateDeviceInFirebase(
        device: Device
    ) {

        Log.d(
            TAG,
            "Updating Firebase for ${device.name}: " +
                    "isOn=${device.isOn}, " +
                    "status=${device.status}"
        )


        firebaseRepository.updateDeviceState(

            device.id,

            mapOf(

                /*
                 * Core state
                 */
                "isOn" to device.isOn,

                "status" to device.status,


                /*
                 * Electrical information
                 */
                "power" to device.power,

                "voltage" to device.voltage,

                "current" to device.current,

                "energyToday" to device.energyToday,


                /*
                 * Iron
                 */
                "timer" to device.timer,

                "heating" to device.heating,

                "safetyMode" to device.safetyMode,

                "temperature" to device.temperature,


                /*
                 * Light
                 */
                "brightness" to device.brightness,


                /*
                 * Switch
                 */
                "switch1" to device.switch1,

                "switch2" to device.switch2,

                "switch3" to device.switch3
            ),

            onSuccess = {

                Log.d(
                    TAG,
                    "Firebase schedule update SUCCESS: " +
                            "${device.name} -> ${device.status}"
                )
            },

            onError = { exception ->

                Log.e(
                    TAG,
                    "Firebase schedule update FAILED: " +
                            "${device.name}",
                    exception
                )
            }
        )
    }


    // ==========================================================
    // CANCEL SCHEDULE
    // ==========================================================

    fun cancelSchedule(
        device: Device
    ) {
        cancelScheduleInternal(device, clearRemote = true)
    }

    private fun cancelScheduleInternal(
        device: Device,
        clearRemote: Boolean
    ) {

        scheduledTasks[device.id]
            ?.let(handler::removeCallbacks)

        scheduledTasks.remove(device.id)
        dueTimes.remove(device.id)
        actions.remove(device.id)
        stopCountdown(device.id)

        device.scheduleAction = ""
        device.scheduleDueAt = 0L
        device.scheduleRemaining = 0

        clearPersistedSchedule(device.id)

        if (clearRemote) {
            clearScheduleInFirebase(device)
        }

        Log.d(
            TAG,
            "Schedule cancelled for ${device.name}"
        )
    }


    // ==========================================================
    // HAS SCHEDULE
    // ==========================================================

    fun hasSchedule(
        deviceId: String
    ): Boolean {

        if (
            dueTimes.containsKey(deviceId)
        ) {
            return true
        }


        if (!ready()) {
            return false
        }


        return preferences.contains(
            "${KEY_PREFIX}${deviceId}_due"
        )
    }


    // ==========================================================
    // REMAINING TIME
    // ==========================================================

    fun remainingMillis(
        deviceId: String
    ): Long {

        val due =
            dueTimes[deviceId]
                ?: if (ready()) {

                    preferences.getLong(
                        "${KEY_PREFIX}${deviceId}_due",
                        0L
                    )

                } else {

                    0L
                }


        return (
                due -
                        System.currentTimeMillis()
                )
            .coerceAtLeast(0L)
    }


    // ==========================================================
    // LIVE PERSISTED COUNTDOWN
    // ==========================================================

    private fun startCountdown(device: Device) {

        stopCountdown(device.id)

        val countdown = object : Runnable {
            override fun run() {
                val due = dueTimes[device.id]
                    ?: device.scheduleDueAt

                val action = actions[device.id]
                    ?: device.scheduleAction

                if (due <= 0L || action.isBlank() || device.scheduleDueAt == 0L || device.scheduleAction.isBlank()) {
                    stopCountdown(device.id)
                    return
                }

                val remaining =
                    (due - System.currentTimeMillis()).coerceAtLeast(0L)

                if (remaining <= 0L) {
                    stopCountdown(device.id)
                    return
                }

                device.scheduleRemaining =
                    ((remaining + 999L) / 1000L).toInt()

                device.scheduleDueAt = due
                device.scheduleAction = action

                DeviceStateNotifier.notifyDeviceChanged(device)

                if (remaining > 0L) {
                    countdownTasks[device.id] = this
                    handler.postDelayed(this, 1000L)
                }
            }
        }

        countdownTasks[device.id] = countdown
        handler.post(countdown)
    }

    private fun stopCountdown(deviceId: String) {
        countdownTasks[deviceId]?.let(handler::removeCallbacks)
        countdownTasks.remove(deviceId)
    }

    private fun updateScheduleInFirebase(device: Device) {
        firebaseRepository.updateDeviceState(
            device.id,
            mapOf(
                "scheduleAction" to device.scheduleAction,
                "scheduleDueAt" to device.scheduleDueAt,
                "scheduleRemaining" to device.scheduleRemaining
            )
        )
    }

    private fun clearScheduleInFirebase(device: Device) {
        firebaseRepository.updateDeviceState(
            device.id,
            mapOf(
                "scheduleAction" to "",
                "scheduleDueAt" to 0L,
                "scheduleRemaining" to 0
            )
        )
    }


    // ==========================================================
    // CLEAR PERSISTED SCHEDULE
    // ==========================================================

    private fun clearPersistedSchedule(
        deviceId: String
    ) {

        if (!ready()) {
            return
        }


        preferences.edit()
            .remove(
                "${KEY_PREFIX}${deviceId}_due"
            )
            .remove(
                "${KEY_PREFIX}${deviceId}_action"
            )
            .apply()
    }
}