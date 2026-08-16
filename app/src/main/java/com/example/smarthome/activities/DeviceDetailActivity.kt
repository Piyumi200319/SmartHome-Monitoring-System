package com.example.smarthome.activities

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface

import com.example.smarthome.R
import com.example.smarthome.controller.CameraController
import com.example.smarthome.controller.IronController
import com.example.smarthome.controller.LightController
import com.example.smarthome.controller.OutletController
import com.example.smarthome.controller.SwitchController
import com.example.smarthome.firebase.FirebaseDevice
import com.example.smarthome.firebase.FirebaseRepository
import com.example.smarthome.models.Device
import com.example.smarthome.service.DeviceStateStorage
import com.example.smarthome.service.EnergyService
import com.example.smarthome.service.NotificationService
import com.example.smarthome.service.ScheduleService

import com.google.firebase.firestore.ListenerRegistration
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider


class DeviceDetailActivity : AppCompatActivity() {

    // =========================================================
    // CONTROLLERS / SERVICES
    // =========================================================

    private val firebaseRepository =
        FirebaseRepository()

    private val cameraController =
        CameraController()

    private val lightController =
        LightController()

    private val switchController =
        SwitchController()

    private val outletController =
        OutletController()

    private val scheduleService =
        ScheduleService

    private var currentDevice: Device? = null

    private val mainHandler =
        Handler(Looper.getMainLooper())

    // =========================================================
// LIVE SCHEDULE COUNTDOWN FOR THIS SCREEN
// =========================================================

    private val scheduleUiHandler =
        Handler(Looper.getMainLooper())

    private var scheduleUiRunnable: Runnable? = null
    private var deviceListener: ListenerRegistration? =
        null

    private var currentDeviceId: String? =
        null


    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_device_detail
        )

        // -----------------------------------------------------
        // INITIALIZE SERVICES
        // -----------------------------------------------------

        NotificationService.initialize(this)

        DeviceStateStorage.initialize(this)

        ScheduleService.initialize(this)


        // -----------------------------------------------------
        // GET DEVICE ID
        // -----------------------------------------------------

        val deviceId =
            intent.getStringExtra("DEVICE_ID")

        currentDeviceId =
            deviceId

        if (deviceId.isNullOrBlank()) {

            Log.e(
                "DEVICE_DETAIL",
                "DEVICE_ID is missing"
            )

            finish()

            return
        }


        // -----------------------------------------------------
        // LOAD DEVICE FROM FIREBASE
        // -----------------------------------------------------

        firebaseRepository.getDeviceById(
            deviceId
        ) { firebaseDevice ->

            if (firebaseDevice == null) {

                Log.e(
                    "FIREBASE",
                    "Device not found: $deviceId"
                )

                runOnUiThread {
                    finish()
                }

                return@getDeviceById
            }


            // -------------------------------------------------
            // CONVERT FIREBASE DEVICE
            // -------------------------------------------------

            val device =
                convertFirebaseDevice(
                    firebaseDevice
                )


            // -------------------------------------------------
            // ATTACH DEVICE TO SCHEDULE SERVICE
            // -------------------------------------------------

            ScheduleService.attachDevice(
                device
            )


            runOnUiThread {

                currentDevice =
                    device

                setupDeviceDetails(
                    device
                )

                registerDeviceListener(
                    device.id
                )
            }
        }
    }


    // =========================================================
    // FIREBASE REAL-TIME DEVICE LISTENER
    // =========================================================

    private fun registerDeviceListener(
        deviceId: String
    ) {

        deviceListener?.remove()

        deviceListener =
            firebaseRepository.listenToDevice(
                deviceId,

                onChanged = { firebaseDevice ->

                    runOnUiThread {

                        val device =
                            currentDevice

                        if (
                            device == null ||
                            device.id != firebaseDevice.id
                        ) {
                            return@runOnUiThread
                        }

                        // Keep the SAME Device object because
                        // setupLight() listeners hold a reference
                        // to this object.
                        copyFirebaseState(
                            firebaseDevice,
                            device
                        )

                        val txtStatus =
                            findViewById<TextView>(
                                R.id.txtStatus
                            )

                        val txtPower =
                            findViewById<TextView>(
                                R.id.txtPower
                            )

                        val txtVoltage =
                            findViewById<TextView>(
                                R.id.txtVoltage
                            )

                        val txtCurrent =
                            findViewById<TextView>(
                                R.id.txtCurrent
                            )

                        val txtEnergy =
                            findViewById<TextView>(
                                R.id.txtEnergy
                            )

                        updateLiveDeviceStatus(
                            device,
                            txtStatus,
                            if (
                                device.type.equals(
                                    "camera",
                                    ignoreCase = true
                                )
                            ) {
                                findViewById<TextView>(
                                    R.id.txtCameraStatus
                                )
                            } else {
                                null
                            }
                        )

                        // =====================================================
// LIVE IRON TEMPERATURE CONTROL
// =====================================================

                        if (
                            device.type.equals(
                                "iron",
                                ignoreCase = true
                            )
                        ) {

                            val ironIsOn =
                                device.status.equals(
                                    "ON",
                                    ignoreCase = true
                                ) || device.isOn

                            device.isOn =
                                ironIsOn

                            device.status =
                                if (ironIsOn) {
                                    "ON"
                                } else {
                                    "OFF"
                                }

                            val ironTemperatureSlider =
                                findViewById<Slider>(
                                    R.id.sliderTemperature
                                )

                            val ironTemperatureText =
                                findViewById<TextView>(
                                    R.id.txtTemperature
                                )

                            val txtRemaining =
                                findViewById<TextView>(
                                    R.id.txtRemaining
                                )

                            val txtSafety =
                                findViewById<TextView>(
                                    R.id.txtSafety
                                )

                            if (ironTemperatureSlider != null && ironTemperatureText != null) {
                                ironTemperatureSlider.isEnabled = ironIsOn
                                ironTemperatureText.setTextColor(
                                    if (ironIsOn) Color.BLACK else Color.GRAY
                                )
                            }

                            if (ironIsOn) {
                                if (txtRemaining != null && txtSafety != null && ironTemperatureText != null && ironTemperatureSlider != null) {
                                    if (!com.example.smarthome.controller.IronController.isRunning(device)) {
                                        if (device.timer <= 0) {
                                            device.timer = 120
                                        }
                                        startIronTimer(device, txtStatus, txtRemaining, txtSafety, ironTemperatureText, ironTemperatureSlider)
                                    } else {
                                        txtRemaining.text = "Remaining : ${device.timer} s"
                                        txtSafety.text = "Safety : ${device.safetyMode}"
                                    }
                                }
                            } else {
                                com.example.smarthome.controller.IronController.stopIron(device)
                                device.timer = 0
                                device.heating = false
                                txtRemaining?.text = "Remaining : 0 s"
                                txtSafety?.text = "Safety : SAFE"
                            }
                        }
                        // Keep schedule display synchronized with Firebase in real time.
                        val txtScheduleStatus =
                            findViewById<TextView>(
                                R.id.txtScheduleStatus
                            )

                        if (device.scheduleDueAt > System.currentTimeMillis() && device.scheduleAction.isNotBlank()) {
                            ScheduleService.attachDevice(device)
                        } else if (device.scheduleDueAt == 0L || device.scheduleAction.isBlank()) {
                            ScheduleService.cancelSchedule(device)
                        }

                        updateScheduleStatus(
                            device,
                            txtScheduleStatus
                        )

                        if (
                            ScheduleService.hasSchedule(device.id) ||
                            (device.scheduleDueAt > System.currentTimeMillis() && device.scheduleAction.isNotBlank())
                        ) {
                            scheduleStatusUpdater(
                                device,
                                txtScheduleStatus
                            )
                        }

                        updateLiveInformation(
                            device,
                            txtPower,
                            txtVoltage,
                            txtCurrent,
                            txtEnergy
                        )

                        if (
                            device.type
                                .trim()
                                .equals(
                                    "light",
                                    ignoreCase = true
                                )
                        ) {

                            val sliderBrightness =
                                findViewById<Slider>(
                                    R.id.sliderBrightness
                                )

                            val txtBrightness =
                                findViewById<TextView>(
                                    R.id.txtBrightness
                                )

                            if (sliderBrightness != null && txtBrightness != null) {
                                sliderBrightness.value =
                                    device.brightness.toFloat()

                                updateLightControls(
                                    device,
                                    sliderBrightness,
                                    txtBrightness
                                )
                            }
                        } else if (
                            device.type
                                .trim()
                                .equals(
                                    "switch",
                                    ignoreCase = true
                                )
                        ) {

                            val switchOne =
                                findViewById<com.google.android.material.materialswitch.MaterialSwitch>(
                                    R.id.switchOne
                                )

                            val switchTwo =
                                findViewById<com.google.android.material.materialswitch.MaterialSwitch>(
                                    R.id.switchTwo
                                )

                            val switchThree =
                                findViewById<com.google.android.material.materialswitch.MaterialSwitch>(
                                    R.id.switchThree
                                )

                            if (switchOne != null && switchTwo != null && switchThree != null) {
                                switchOne.setOnCheckedChangeListener(null)
                                switchTwo.setOnCheckedChangeListener(null)
                                switchThree.setOnCheckedChangeListener(null)

                                switchOne.isChecked = device.switch1
                                switchTwo.isChecked = device.switch2
                                switchThree.isChecked = device.switch3

                                setSwitchListeners(
                                    device,
                                    switchOne,
                                    switchTwo,
                                    switchThree,
                                    txtStatus,
                                    txtPower,
                                    txtCurrent,
                                    txtEnergy
                                )
                            }
                        } else if (
                            device.type
                                .trim()
                                .equals(
                                    "camera",
                                    ignoreCase = true
                                )
                        ) {

                            val switchRecording =
                                findViewById<com.google.android.material.materialswitch.MaterialSwitch>(
                                    R.id.switchRecording
                                )

                            val switchMotion =
                                findViewById<com.google.android.material.materialswitch.MaterialSwitch>(
                                    R.id.switchMotion
                                )

                            val switchNightVision =
                                findViewById<com.google.android.material.materialswitch.MaterialSwitch>(
                                    R.id.switchNightVision
                                )

                            if (switchRecording != null && switchMotion != null && switchNightVision != null) {
                                updateCameraInformation(
                                    device,
                                    findViewById(R.id.txtCameraStatus),
                                    findViewById(R.id.txtResolution),
                                    findViewById(R.id.txtFPS),
                                    switchRecording,
                                    switchMotion,
                                    switchNightVision
                                )
                            }
                        }
                    }
                },

                onError = { exception ->

                    Log.e(
                        "FIREBASE",
                        "Device listener failed: ${exception.message}",
                        exception
                    )
                }
            )
    }


    // =========================================================
    // COPY FIREBASE STATE INTO EXISTING DEVICE
    // =========================================================

    private fun copyFirebaseState(
        firebaseDevice: FirebaseDevice,
        device: Device
    ) {

        device.status =
            firebaseDevice.status

        device.isOn =
            firebaseDevice.isOn

        device.timer =
            firebaseDevice.timer

        device.maxTime =
            firebaseDevice.maxTime

        device.temperature =
            firebaseDevice.temperature

        device.brightness =
            firebaseDevice.brightness

        device.scheduleAction =
            firebaseDevice.scheduleAction

        device.scheduleDueAt =
            firebaseDevice.scheduleDueAt

        device.scheduleRemaining =
            firebaseDevice.scheduleRemaining

        // For lights, status is the source of truth for the UI.
        // Repair any stale isOn value coming from an older write.
        if (device.type.trim().equals("light", ignoreCase = true)) {
            device.isOn = device.status.equals("ON", ignoreCase = true)
        }

        device.recording =
            firebaseDevice.recording

        device.motionDetection =
            firebaseDevice.motionDetection

        device.nightVision =
            firebaseDevice.nightVision

        device.fps =
            firebaseDevice.fps

        device.resolution =
            firebaseDevice.resolution

        device.switch1 =
            firebaseDevice.switch1

        device.switch2 =
            firebaseDevice.switch2

        device.switch3 =
            firebaseDevice.switch3

        device.targetTemperature =
            firebaseDevice.targetTemperature

        device.heating =
            firebaseDevice.heating

        device.safetyMode =
            firebaseDevice.safetyMode

        device.power =
            firebaseDevice.power

        device.voltage =
            firebaseDevice.voltage

        device.current =
            firebaseDevice.current

        device.energyToday =
            firebaseDevice.energyToday
    }


    // =========================================================
    // ON STOP
    // =========================================================

    override fun onStop() {

        super.onStop()

        currentDevice?.let { device ->

            DeviceStateStorage.saveDevice(
                device
            )
        }
    }


    // =========================================================
    // ON DESTROY
    // =========================================================

    override fun onDestroy() {

        scheduleUiRunnable?.let {
            scheduleUiHandler.removeCallbacks(it)
        }

        scheduleUiRunnable = null

        mainHandler.removeCallbacksAndMessages(
            null
        )

        scheduleUiRunnable?.let {
            scheduleUiHandler.removeCallbacks(it)
        }

        scheduleUiRunnable = null

        deviceListener?.remove()
        deviceListener = null

        super.onDestroy()
    }


    // =========================================================
    // SETUP DEVICE DETAILS
    // =========================================================

    private fun setupDeviceDetails(
        device: Device
    ) {

        // =====================================================
        // BASIC VIEWS
        // =====================================================

        val txtName =
            findViewById<TextView>(
                R.id.txtDeviceTitle
            )

        val txtType =
            findViewById<TextView>(
                R.id.txtDeviceType
            )

        val txtStatus =
            findViewById<TextView>(
                R.id.txtStatus
            )


        // =====================================================
        // CARDS
        // =====================================================

        val cardIron =
            findViewById<View>(
                R.id.cardIron
            )

        val cardLight =
            findViewById<View>(
                R.id.cardLight
            )

        val cardCamera =
            findViewById<View>(
                R.id.cardCamera
            )


        // =====================================================
        // IRON
        // =====================================================

        val txtRemaining =
            findViewById<TextView>(
                R.id.txtRemaining
            )

        val txtMaximum =
            findViewById<TextView>(
                R.id.txtMaximum
            )

        val txtSafety =
            findViewById<TextView>(
                R.id.txtSafety
            )


        // =====================================================
        // LIGHT
        // =====================================================

        val txtBrightness =
            findViewById<TextView>(
                R.id.txtBrightness
            )

        val sliderBrightness =
            findViewById<Slider>(
                R.id.sliderBrightness
            )

        val txtSchedule =
            findViewById<TextView>(
                R.id.txtSchedule
            )



        // =====================================================
        // CAMERA
        // =====================================================

        val txtCameraStatus =
            findViewById<TextView>(
                R.id.txtCameraStatus
            )

        val imgCamera =
            findViewById<ImageView>(
                R.id.imgCameraPreview
            )

        val txtResolution =
            findViewById<TextView>(
                R.id.txtResolution
            )

        val txtFPS =
            findViewById<TextView>(
                R.id.txtFPS
            )

        val switchRecording =
            findViewById<MaterialSwitch>(
                R.id.switchRecording
            )

        val switchMotion =
            findViewById<MaterialSwitch>(
                R.id.switchMotion
            )

        val switchNightVision =
            findViewById<MaterialSwitch>(
                R.id.switchNightVision
            )


        // =====================================================
        // ELECTRICAL INFORMATION
        // =====================================================

        val txtPower =
            findViewById<TextView>(
                R.id.txtPower
            )

        val txtVoltage =
            findViewById<TextView>(
                R.id.txtVoltage
            )

        val txtCurrent =
            findViewById<TextView>(
                R.id.txtCurrent
            )

        val txtEnergy =
            findViewById<TextView>(
                R.id.txtEnergy
            )


        // =====================================================
        // TEMPERATURE
        // =====================================================

        val txtTemperatureTitle =
            findViewById<TextView>(
                R.id.txtTemperatureTitle
            )

        val txtTemperature =
            findViewById<TextView>(
                R.id.txtTemperature
            )

        val sliderTemperature =
            findViewById<Slider>(
                R.id.sliderTemperature
            )


        // =====================================================
        // SWITCH
        // =====================================================

        val txtSwitchTitle =
            findViewById<TextView>(
                R.id.txtSwitchTitle
            )

        val switchOne =
            findViewById<MaterialSwitch>(
                R.id.switchOne
            )

        val switchTwo =
            findViewById<MaterialSwitch>(
                R.id.switchTwo
            )

        val switchThree =
            findViewById<MaterialSwitch>(
                R.id.switchThree
            )


        // =====================================================
        // SCHEDULE
        // =====================================================

        val txtScheduleStatus =
            findViewById<TextView>(
                R.id.txtScheduleStatus
            )

        updateScheduleStatus(
            device,
            txtScheduleStatus
        )

        scheduleStatusUpdater(
            device,
            txtScheduleStatus
        )


        val btnScheduleOn =
            findViewById<Button>(
                R.id.btnScheduleOn
            )

        val btnScheduleOff =
            findViewById<Button>(
                R.id.btnScheduleOff
            )

        val btnCancelSchedule =
            findViewById<Button>(
                R.id.btnCancelSchedule
            )


        // =====================================================
        // BASIC INFORMATION
        // =====================================================

        txtName.text =
            device.name

        txtType.text =
            device.type

        updateStatusText(
            device,
            txtStatus
        )

        // Status simulation buttons
        findViewById<View>(R.id.btnSetStatusOn)?.setOnClickListener {
            com.example.smarthome.controller.DeviceController().turnOn(device)
            if (device.type.equals("iron", ignoreCase = true)) {
                device.timer = 120
                device.heating = true
                device.safetyMode = "SAFE"
                val txtRemaining = findViewById<TextView>(R.id.txtRemaining)
                val txtSafety = findViewById<TextView>(R.id.txtSafety)
                val txtTemperature = findViewById<TextView>(R.id.txtTemperature)
                val sliderTemperature = findViewById<Slider>(R.id.sliderTemperature)
                if (txtRemaining != null && txtSafety != null && txtTemperature != null && sliderTemperature != null) {
                    startIronTimer(device, txtStatus, txtRemaining, txtSafety, txtTemperature, sliderTemperature)
                }
            }
            updateStatusText(device, txtStatus)
            updateLiveInformation(device, txtPower, txtVoltage, txtCurrent, txtEnergy)
            updateDeviceInFirebase(device)
        }

        findViewById<View>(R.id.btnSetStatusOff)?.setOnClickListener {
            com.example.smarthome.controller.DeviceController().turnOff(device)
            if (device.type.equals("iron", ignoreCase = true)) {
                com.example.smarthome.controller.IronController.stopIron(device)
                device.timer = 0
                device.heating = false
                findViewById<TextView>(R.id.txtRemaining)?.text = "Remaining : 0 s"
                findViewById<TextView>(R.id.txtSafety)?.text = "Safety : SAFE"
            }
            updateStatusText(device, txtStatus)
            updateLiveInformation(device, txtPower, txtVoltage, txtCurrent, txtEnergy)
            updateDeviceInFirebase(device)
        }

        findViewById<View>(R.id.btnSetStatusError)?.setOnClickListener {
            device.isOn = false
            device.status = "ERROR"
            device.power = 0
            device.current = 0.0
            if (device.type.equals("iron", ignoreCase = true)) {
                com.example.smarthome.controller.IronController.stopIron(device)
                device.timer = 0
                device.heating = false
                findViewById<TextView>(R.id.txtRemaining)?.text = "Remaining : 0 s"
                findViewById<TextView>(R.id.txtSafety)?.text = "Safety : SAFE"
            }
            updateStatusText(device, txtStatus)
            updateLiveInformation(device, txtPower, txtVoltage, txtCurrent, txtEnergy)
            updateDeviceInFirebase(device)
        }

        findViewById<View>(R.id.btnSetStatusDisconnected)?.setOnClickListener {
            device.isOn = false
            device.status = "DISCONNECTED"
            device.power = 0
            device.current = 0.0
            if (device.type.equals("iron", ignoreCase = true)) {
                com.example.smarthome.controller.IronController.stopIron(device)
                device.timer = 0
                device.heating = false
                findViewById<TextView>(R.id.txtRemaining)?.text = "Remaining : 0 s"
                findViewById<TextView>(R.id.txtSafety)?.text = "Safety : SAFE"
            }
            updateStatusText(device, txtStatus)
            updateLiveInformation(device, txtPower, txtVoltage, txtCurrent, txtEnergy)
            updateDeviceInFirebase(device)
        }


        // =====================================================
        // HIDE EVERYTHING FIRST
        // =====================================================

        cardIron.visibility =
            View.GONE

        cardLight.visibility =
            View.GONE

        cardCamera.visibility =
            View.GONE

        txtRemaining.visibility =
            View.GONE

        txtMaximum.visibility =
            View.GONE

        txtSafety.visibility =
            View.GONE

        txtBrightness.visibility =
            View.GONE

        txtSchedule.visibility =
            View.GONE

        sliderBrightness.visibility =
            View.GONE


        txtCameraStatus.visibility =
            View.GONE

        imgCamera.visibility =
            View.GONE

        txtResolution.visibility =
            View.GONE

        txtFPS.visibility =
            View.GONE

        switchRecording.visibility =
            View.GONE

        switchMotion.visibility =
            View.GONE

        switchNightVision.visibility =
            View.GONE

        txtTemperatureTitle.visibility =
            View.GONE

        txtTemperature.visibility =
            View.GONE

        sliderTemperature.visibility =
            View.GONE

        txtSwitchTitle.visibility =
            View.GONE

        switchOne.visibility =
            View.GONE

        switchTwo.visibility =
            View.GONE

        switchThree.visibility =
            View.GONE


        // =====================================================
        // DEVICE TYPE
        // =====================================================

        when (
            device.type
                .trim()
                .lowercase()
        ) {

            // =================================================
            // IRON
            // =================================================

            "iron" -> {

                setupIron(

                    device,

                    txtStatus,
                    txtRemaining,
                    txtMaximum,
                    txtSafety,
                    txtTemperatureTitle,
                    txtTemperature,
                    sliderTemperature,
                    txtPower,
                    txtVoltage,
                    txtCurrent,
                    txtEnergy
                )
            }


            // =================================================
            // LIGHT
            // =================================================

            "light" -> {

                setupLight(
                    device,
                    cardLight,
                    txtType,
                    txtStatus,
                    txtBrightness,
                    sliderBrightness,
                    txtSchedule,
                    txtPower,
                    txtVoltage,
                    txtCurrent,
                    txtEnergy
                )
            }


            // =================================================
            // CAMERA
            // =================================================

            "camera" -> {

                setupCamera(

                    device,

                    cardCamera,
                    txtType,
                    txtCameraStatus,
                    imgCamera,
                    txtResolution,
                    txtFPS,
                    switchRecording,
                    switchMotion,
                    switchNightVision,
                    txtPower,
                    txtVoltage,
                    txtCurrent,
                    txtEnergy
                )
            }


            // =================================================
            // OUTLET
            // =================================================

            "outlet" -> {

                txtType.text =
                    "Electrical Outlet"

                updateLiveInformation(

                    device,

                    txtPower,
                    txtVoltage,
                    txtCurrent,
                    txtEnergy
                )
            }


            // =================================================
            // SWITCH
            // =================================================

            "switch" -> {

                setupSwitch(

                    device,

                    txtType,
                    txtSwitchTitle,
                    switchOne,
                    switchTwo,
                    switchThree,
                    txtStatus,
                    txtPower,
                    txtCurrent,
                    txtEnergy
                )
            }
        }


// =====================================================
// SCHEDULE ON
// =====================================================

        // =====================================================
        // SCHEDULE ON
        // =====================================================

        btnScheduleOn.setOnClickListener {

            // ScheduleService calculates and stores the exact due time,
            // updates Firebase, and starts its background countdown.
            scheduleService.scheduleTurnOn(
                device,
                10_000L
            ) {

                runOnUiThread {

                    device.isOn = true
                    device.status = "ON"

                    EnergyService().updateDeviceElectricalInfo(device)

                    device.scheduleAction = ""
                    device.scheduleDueAt = 0L
                    device.scheduleRemaining = 0

                    updateStatusText(
                        device,
                        txtStatus
                    )

                    updateLiveInformation(
                        device,
                        txtPower,
                        txtVoltage,
                        txtCurrent,
                        txtEnergy
                    )

                    if (
                        device.type.equals(
                            "light",
                            ignoreCase = true
                        )
                    ) {

                        updateLightControls(
                            device,
                            sliderBrightness,
                            txtBrightness
                        )
                    }

                    txtScheduleStatus.text =
                        "Schedule: ON completed"

                    DeviceStateStorage.saveDevice(
                        device
                    )

                    updateDeviceInFirebase(
                        device
                    )

                    scheduleUiRunnable?.let {
                        scheduleUiHandler.removeCallbacks(it)
                    }

                    scheduleUiRunnable = null
                }
            }

            // ScheduleService has now populated scheduleAction,
            // scheduleDueAt and scheduleRemaining on this same object.
            txtScheduleStatus.text =
                "Schedule: ON in ${device.scheduleRemaining}s"

            DeviceStateStorage.saveDevice(
                device
            )

            // Start the visible countdown immediately on this page.
            scheduleStatusUpdater(
                device,
                txtScheduleStatus
            )
        }


        // =====================================================
        // SCHEDULE OFF
        // =====================================================

        btnScheduleOff.setOnClickListener {

            scheduleService.scheduleTurnOff(
                device,
                10_000L
            ) {

                runOnUiThread {

                    device.isOn = false
                    device.status = "OFF"

                    EnergyService().updateDeviceElectricalInfo(device)

                    device.scheduleAction = ""
                    device.scheduleDueAt = 0L
                    device.scheduleRemaining = 0

                    updateStatusText(
                        device,
                        txtStatus
                    )

                    updateLiveInformation(
                        device,
                        txtPower,
                        txtVoltage,
                        txtCurrent,
                        txtEnergy
                    )

                    if (
                        device.type.equals(
                            "light",
                            ignoreCase = true
                        )
                    ) {

                        updateLightControls(
                            device,
                            sliderBrightness,
                            txtBrightness
                        )
                    }

                    txtScheduleStatus.text =
                        "Schedule: OFF completed"

                    DeviceStateStorage.saveDevice(
                        device
                    )

                    updateDeviceInFirebase(
                        device
                    )

                    scheduleUiRunnable?.let {
                        scheduleUiHandler.removeCallbacks(it)
                    }

                    scheduleUiRunnable = null
                }
            }

            // ScheduleService has now populated scheduleAction,
            // scheduleDueAt and scheduleRemaining on this same object.
            txtScheduleStatus.text =
                "Schedule: OFF in ${device.scheduleRemaining}s"

            DeviceStateStorage.saveDevice(
                device
            )

            // Start the visible countdown immediately on this page.
            scheduleStatusUpdater(
                device,
                txtScheduleStatus
            )
        }


        // =====================================================
        // CANCEL SCHEDULE
        // =====================================================

        btnCancelSchedule.setOnClickListener {

            scheduleService.cancelSchedule(
                device
            )

            device.scheduleAction = ""
            device.scheduleDueAt = 0L
            device.scheduleRemaining = 0

            scheduleUiRunnable?.let {
                scheduleUiHandler.removeCallbacks(it)
            }

            scheduleUiRunnable = null

            txtScheduleStatus.text =
                "Schedule: Not Set"

            DeviceStateStorage.saveDevice(
                device
            )

            updateDeviceInFirebase(
                device
            )
        }


        // =====================================================
        // TEMPERATURE
        // =====================================================

        sliderTemperature.addOnChangeListener {

                _,
                value,
                fromUser ->

            if (
                sliderTemperature.isEnabled &&
                fromUser
            ) {

                device.temperature =
                    value.toInt()

                txtTemperature.text =
                    "${value.toInt()}°C"


                DeviceStateStorage.saveDevice(
                    device
                )
            }
        }


        sliderTemperature.addOnSliderTouchListener(

            object :
                Slider.OnSliderTouchListener {

                override fun onStartTrackingTouch(
                    slider: Slider
                ) {
                }


                override fun onStopTrackingTouch(
                    slider: Slider
                ) {

                    if (
                        slider.isEnabled
                    ) {

                        firebaseRepository
                            .updateDeviceState(

                                device.id,

                                mapOf(
                                    "temperature" to
                                            device.temperature
                                ),

                                onSuccess = {

                                    Log.d(
                                        "FIREBASE",
                                        "Temperature updated"
                                    )
                                },

                                onError = { exception ->

                                    Log.e(
                                        "FIREBASE",
                                        "Temperature update failed",
                                        exception
                                    )
                                }
                            )
                    }
                }
            }
        )
    }


    // =========================================================
    // SETUP LIGHT
    // =========================================================

// =========================================================
// SETUP LIGHT
// =========================================================

    private fun setupLight(

        device: Device,

        cardLight: View,

        txtType: TextView,

        txtStatus: TextView,

        txtBrightness: TextView,

        sliderBrightness: Slider,

        txtSchedule: TextView,

        txtPower: TextView,

        txtVoltage: TextView,

        txtCurrent: TextView,

        txtEnergy: TextView

    ) {

        // ---------------------------------------------------------
        // SHOW LIGHT CARD
        // ---------------------------------------------------------

        cardLight.visibility = View.VISIBLE

        txtType.text = "Smart Light"

        txtBrightness.visibility = View.VISIBLE

        sliderBrightness.visibility = View.VISIBLE

        txtSchedule.visibility = View.VISIBLE


        // ---------------------------------------------------------
        // SHOW CURRENT VALUES
        // ---------------------------------------------------------

        txtBrightness.text =
            "Brightness : ${device.brightness}%"

        txtSchedule.text =
            "Schedule : 6 PM - 10 PM"

        txtStatus.text =
            "Status : ${device.status}"


        // ---------------------------------------------------------
        // SET SLIDER POSITION
        // ---------------------------------------------------------

        sliderBrightness.value =
            device.brightness.toFloat()


        // ---------------------------------------------------------
        // IMPORTANT:
        // BRIGHTNESS CAN ONLY BE CHANGED WHEN
        // DEVICE STATUS IS ON
        // ---------------------------------------------------------

        val lightIsOn =
            device.status.equals(
                "ON",
                ignoreCase = true
            )


        sliderBrightness.isEnabled =
            lightIsOn


        // Make disabled slider visually clear
        sliderBrightness.alpha =
            if (lightIsOn) {
                1.0f
            } else {
                0.5f
            }


        txtBrightness.alpha =
            if (lightIsOn) {
                1.0f
            } else {
                0.5f
            }


        // ---------------------------------------------------------
        // LIVE INFORMATION
        // ---------------------------------------------------------

        updateLiveInformation(

            device,

            txtPower,

            txtVoltage,

            txtCurrent,

            txtEnergy
        )


        // ---------------------------------------------------------
        // BRIGHTNESS SLIDER
        // ---------------------------------------------------------

        sliderBrightness.addOnChangeListener {

                _,
                value,
                fromUser ->

            if (!fromUser) {
                return@addOnChangeListener
            }


            // -----------------------------------------------------
            // NEVER ALLOW BRIGHTNESS CHANGE WHEN OFF
            // -----------------------------------------------------

            if (
                !device.status.equals(
                    "ON",
                    ignoreCase = true
                )
            ) {

                sliderBrightness.value =
                    device.brightness.toFloat()

                return@addOnChangeListener
            }


            // -----------------------------------------------------
            // CHANGE BRIGHTNESS
            // -----------------------------------------------------

            val brightness =
                value.toInt()


            lightController.changeBrightness(

                device,

                brightness
            )


            // -----------------------------------------------------
            // UPDATE UI
            // -----------------------------------------------------

            txtBrightness.text =
                "Brightness : ${device.brightness}%"

            txtStatus.text =
                "Status : ${device.status}"


            updateLiveInformation(

                device,

                txtPower,

                txtVoltage,

                txtCurrent,

                txtEnergy
            )


            // -----------------------------------------------------
            // SAVE LOCAL STATE
            // -----------------------------------------------------

            DeviceStateStorage.saveDevice(
                device
            )


            // -----------------------------------------------------
            // SAVE FIREBASE STATE
            // -----------------------------------------------------

            firebaseRepository.updateDeviceState(

                device.id,

                mapOf(
                    // Brightness changes must never overwrite the
                    // light's power state. The realtime listener is
                    // responsible for ON/OFF state.
                    "brightness" to device.brightness,
                    "power" to device.power,
                    "current" to device.current,
                    "energyToday" to device.energyToday
                ),

                onSuccess = {

                    Log.d(
                        "FIREBASE",
                        "Light brightness updated: ${device.name}"
                    )
                },

                onError = { exception ->

                    Log.e(
                        "FIREBASE",
                        "Failed to update light brightness",
                        exception
                    )
                }
            )
        }
    }


    // =========================================================
    // UPDATE LIGHT CONTROLS
    // =========================================================

    private fun updateLightControls(

        device: Device,

        sliderBrightness: Slider,

        txtBrightness: TextView

    ) {

        val lightIsOn =
            device.status.equals(
                "ON",
                ignoreCase = true
            )


        // -----------------------------------------------------
        // ENABLE / DISABLE SLIDER
        // -----------------------------------------------------

        sliderBrightness.isEnabled =
            lightIsOn


        // -----------------------------------------------------
        // CHANGE VISUAL APPEARANCE
        // -----------------------------------------------------

        sliderBrightness.alpha =
            if (lightIsOn) {
                1.0f
            } else {
                0.5f
            }


        txtBrightness.alpha =
            if (lightIsOn) {
                1.0f
            } else {
                0.5f
            }


        // -----------------------------------------------------
        // UPDATE TEXT
        // -----------------------------------------------------

        txtBrightness.text =
            "Brightness : ${device.brightness}%"
    }


    // =========================================================
    // UPDATE STATUS TEXT
    // =========================================================

    private fun updateStatusText(
        device: Device,
        txtStatus: TextView
    ) {
        val layoutAlert = findViewById<View>(R.id.layoutStatusAlert)
        val alertTitle = findViewById<TextView>(R.id.txtStatusAlertTitle)
        val alertBody = findViewById<TextView>(R.id.txtStatusAlertBody)

        when (device.status.trim().uppercase()) {
            "ON" -> {
                txtStatus.text = "Status : ON"
                txtStatus.setTextColor(Color.parseColor("#15803D"))
                layoutAlert?.visibility = View.GONE
            }
            "ERROR" -> {
                txtStatus.text = "Status : ERROR"
                txtStatus.setTextColor(Color.parseColor("#DC2626"))
                layoutAlert?.visibility = View.VISIBLE
                layoutAlert?.setBackgroundColor(Color.parseColor("#FEE2E2"))
                alertTitle?.text = "HARDWARE FAULT DETECTED"
                alertTitle?.setTextColor(Color.parseColor("#DC2626"))
                alertBody?.text = "Device internal component or thermal limit fault. Controls locked."
                alertBody?.setTextColor(Color.parseColor("#991B1B"))
            }
            "DISCONNECTED" -> {
                txtStatus.text = "Status : DISCONNECTED"
                txtStatus.setTextColor(Color.parseColor("#D97706"))
                layoutAlert?.visibility = View.VISIBLE
                layoutAlert?.setBackgroundColor(Color.parseColor("#FEF3C7"))
                alertTitle?.text = "DEVICE DISCONNECTED"
                alertTitle?.setTextColor(Color.parseColor("#D97706"))
                alertBody?.text = "Device lost cloud Wi-Fi connectivity. Real-time controls unavailable."
                alertBody?.setTextColor(Color.parseColor("#92400E"))
            }
            else -> {
                txtStatus.text = "Status : OFF"
                txtStatus.setTextColor(Color.GRAY)
                layoutAlert?.visibility = View.GONE
            }
        }
    }


// =========================================================
// SETUP CAMERA
// =========================================================

    private fun setupCamera(

        device: Device,

        cardCamera: View,

        txtType: TextView,

        txtCameraStatus: TextView,

        imgCamera: ImageView,

        txtResolution: TextView,

        txtFPS: TextView,

        switchRecording: MaterialSwitch,

        switchMotion: MaterialSwitch,

        switchNightVision: MaterialSwitch,

        txtPower: TextView,

        txtVoltage: TextView,

        txtCurrent: TextView,

        txtEnergy: TextView

    ) {

        cardCamera.visibility = View.VISIBLE

        txtType.text = "Security Camera"

        txtCameraStatus.visibility = View.VISIBLE

        imgCamera.visibility = View.VISIBLE

        txtResolution.visibility = View.VISIBLE

        txtFPS.visibility = View.VISIBLE

        switchRecording.visibility = View.VISIBLE

        switchMotion.visibility = View.VISIBLE

        switchNightVision.visibility = View.VISIBLE


        // =====================================================
        // CAMERA PHOTO
        // =====================================================

        imgCamera.setImageResource(
            R.drawable.camera_snapshot
        )

        imgCamera.scaleType =
            ImageView.ScaleType.CENTER_CROP


        // =====================================================
        // CAMERA STATUS
        // =====================================================

// =====================================================
// CAMERA STATUS
// =====================================================

        val currentStatus = device.status.trim().uppercase()

        when (currentStatus) {

            "ON" -> {
                txtCameraStatus.text = "LIVE"
                txtCameraStatus.setTextColor(Color.GREEN)
            }

            "OFF" -> {
                txtCameraStatus.text = "OFFLINE"
                txtCameraStatus.setTextColor(Color.RED)
            }

            "ERROR" -> {
                txtCameraStatus.text = "ERROR"
                txtCameraStatus.setTextColor(Color.RED)
            }

            "DISCONNECTED" -> {
                txtCameraStatus.text = "DISCONNECTED"
                txtCameraStatus.setTextColor(Color.GRAY)
            }

            else -> {
                // If status is missing but isOn says the camera is running
                if (device.isOn) {
                    txtCameraStatus.text = "LIVE"
                    txtCameraStatus.setTextColor(Color.GREEN)
                } else {
                    txtCameraStatus.text = "OFFLINE"
                    txtCameraStatus.setTextColor(Color.RED)
                }
            }
        }


        // =====================================================
        // CAMERA INFORMATION
        // =====================================================

        updateCameraInformation(

            device,

            txtCameraStatus,

            txtResolution,

            txtFPS,

            switchRecording,

            switchMotion,

            switchNightVision
        )


        // =====================================================
        // ELECTRICAL INFORMATION
        // =====================================================

        updateLiveInformation(

            device,

            txtPower,

            txtVoltage,

            txtCurrent,

            txtEnergy
        )
    }


    // =========================================================
    // SETUP IRON
    // =========================================================

    private fun setupIron(
        device: Device,
        txtStatus: TextView,
        txtRemaining: TextView,
        txtMaximum: TextView,
        txtSafety: TextView,
        txtTemperatureTitle: TextView,
        txtTemperature: TextView,
        sliderTemperature: Slider,
        txtPower: TextView,
        txtVoltage: TextView,
        txtCurrent: TextView,
        txtEnergy: TextView
    ) {

        val cardIron =
            findViewById<View>(R.id.cardIron)

        cardIron.visibility =
            View.VISIBLE

        txtRemaining.visibility =
            View.VISIBLE

        txtMaximum.visibility =
            View.VISIBLE

        txtSafety.visibility =
            View.VISIBLE

        txtTemperatureTitle.visibility =
            View.VISIBLE

        txtTemperature.visibility =
            View.VISIBLE

        sliderTemperature.visibility =
            View.VISIBLE


        // -----------------------------------------------------
        // SYNCHRONIZE IRON STATE
        // -----------------------------------------------------

        val ironIsOn =
            device.status.equals(
                "ON",
                ignoreCase = true
            ) || device.isOn

        device.isOn =
            ironIsOn

        device.status =
            if (ironIsOn) "ON" else "OFF"


        // -----------------------------------------------------
        // DISPLAY VALUES
        // -----------------------------------------------------

        txtMaximum.text =
            "Maximum : ${device.maxTime} Minutes"

        txtSafety.text =
            "Safety : ${device.safetyMode}"

        txtTemperature.text =
            "${device.temperature}°C"

        sliderTemperature.value =
            device.temperature.toFloat()


        updateLiveInformation(
            device,
            txtPower,
            txtVoltage,
            txtCurrent,
            txtEnergy
        )


        // -----------------------------------------------------
        // TEMPERATURE CONTROL
        // IMPORTANT:
        // ENABLE BASED ON IRON ON/OFF, NOT TIMER
        // -----------------------------------------------------

        sliderTemperature.isEnabled =
            ironIsOn

        txtTemperature.setTextColor(
            if (ironIsOn) {
                Color.BLACK
            } else {
                Color.GRAY
            }
        )


        // -----------------------------------------------------
        // STATUS
        // -----------------------------------------------------

        updateStatusText(
            device,
            txtStatus
        )


        // -----------------------------------------------------
        // TIMER
        // -----------------------------------------------------

        if (
            ironIsOn &&
            device.timer > 0
        ) {

            txtRemaining.text =
                "Remaining : ${device.timer} s"

            startIronTimer(
                device,
                txtStatus,
                txtRemaining,
                txtSafety,
                txtTemperature,
                sliderTemperature
            )

        } else {

            txtRemaining.text =
                if (ironIsOn) {
                    "Remaining : No Timer"
                } else {
                    "Remaining : 0 s"
                }
        }
    }


    // =========================================================
    // SETUP SWITCH
    // =========================================================

    private fun setupSwitch(

        device: Device,

        txtType: TextView,

        txtSwitchTitle: TextView,

        switchOne: MaterialSwitch,

        switchTwo: MaterialSwitch,

        switchThree: MaterialSwitch,

        txtStatus: TextView,

        txtPower: TextView,

        txtCurrent: TextView,

        txtEnergy: TextView

    ) {

        txtType.text =
            "Multi Switch"


        txtSwitchTitle.visibility =
            View.VISIBLE


        switchOne.visibility =
            View.VISIBLE

        switchTwo.visibility =
            View.VISIBLE

        switchThree.visibility =
            View.VISIBLE


        switchOne.setOnCheckedChangeListener(
            null
        )

        switchTwo.setOnCheckedChangeListener(
            null
        )

        switchThree.setOnCheckedChangeListener(
            null
        )


        switchOne.isChecked =
            device.switch1

        switchTwo.isChecked =
            device.switch2

        switchThree.isChecked =
            device.switch3


        setSwitchListeners(

            device,

            switchOne,
            switchTwo,
            switchThree,

            txtStatus,
            txtPower,
            txtCurrent,
            txtEnergy
        )


        updateSwitchInformation(

            device,

            txtStatus,
            txtPower,
            txtCurrent,
            txtEnergy
        )
    }


    // =========================================================
    // LIVE ELECTRICAL INFORMATION
    // =========================================================

    private fun updateLiveInformation(

        device: Device,

        txtPower: TextView,

        txtVoltage: TextView,

        txtCurrent: TextView,

        txtEnergy: TextView

    ) {

        txtPower.text =
            "Power : ${device.power} W"


        txtVoltage.text =
            "Voltage : ${device.voltage} V"


        txtCurrent.text =
            "Current : %.3f A"
                .format(device.current)


        txtEnergy.text =
            "Today's Energy : %.2f kWh"
                .format(device.energyToday)
    }


    // =========================================================
    // CAMERA INFORMATION
    // =========================================================

    private fun updateCameraInformation(

        device: Device,

        txtCameraStatus: TextView,

        txtResolution: TextView,

        txtFPS: TextView,

        switchRecording: MaterialSwitch,

        switchMotion: MaterialSwitch,

        switchNightVision: MaterialSwitch

    ) {

        val currentStatus = device.status.trim().uppercase()

        txtCameraStatus.text = when (currentStatus) {
            "ON" -> "LIVE"
            "OFF" -> "OFFLINE"
            "ERROR" -> "ERROR"
            "DISCONNECTED" -> "DISCONNECTED"
            else -> if (device.isOn) "LIVE" else "OFFLINE"
        }


        txtResolution.text =
            "Resolution : ${device.resolution}"


        txtFPS.text =
            "FPS : ${device.fps}"


        switchRecording.setOnCheckedChangeListener(
            null
        )

        switchMotion.setOnCheckedChangeListener(
            null
        )

        switchNightVision.setOnCheckedChangeListener(
            null
        )


        switchRecording.isChecked =
            device.recording

        switchMotion.isChecked =
            device.motionDetection

        switchNightVision.isChecked =
            device.nightVision


        switchRecording.setOnCheckedChangeListener {

                _,
                _ ->

            cameraController.toggleRecording(
                device
            )

            DeviceStateStorage.saveDevice(
                device
            )

            updateDeviceInFirebase(
                device
            )
        }


        switchMotion.setOnCheckedChangeListener {

                _,
                _ ->

            cameraController.toggleMotion(
                device
            )

            DeviceStateStorage.saveDevice(
                device
            )

            updateDeviceInFirebase(
                device
            )
        }


        switchNightVision.setOnCheckedChangeListener {

                _,
                _ ->

            cameraController.toggleNightVision(
                device
            )

            DeviceStateStorage.saveDevice(
                device
            )

            updateDeviceInFirebase(
                device
            )
        }
    }


    // =========================================================
    // IRON TIMER
    // =========================================================

    private fun startIronTimer(
        device: Device,
        txtStatus: TextView,
        txtRemaining: TextView,
        txtSafety: TextView,
        txtTemperature: TextView,
        sliderTemperature: Slider
    ) {

        val ironIsOn =
            device.status.equals(
                "ON",
                ignoreCase = true
            ) || device.isOn

        device.isOn =
            ironIsOn

        device.status =
            if (ironIsOn) "ON" else "OFF"


        sliderTemperature.isEnabled =
            ironIsOn

        txtTemperature.setTextColor(
            if (ironIsOn) {
                Color.BLACK
            } else {
                Color.GRAY
            }
        )


        IronController.resumeIron(
            device,

            object :
                IronController.IronListener {

                override fun onTick(
                    device: Device
                ) {

                    runOnUiThread {

                        val currentlyOn =
                            device.status.equals(
                                "ON",
                                ignoreCase = true
                            ) || device.isOn

                        device.isOn =
                            currentlyOn

                        device.status =
                            if (currentlyOn) "ON"
                            else "OFF"


                        txtRemaining.text =
                            "Remaining : ${device.timer} s"

                        updateStatusText(
                            device,
                            txtStatus
                        )

                        txtSafety.text =
                            "Safety : ${device.safetyMode}"


                        // IMPORTANT
                        sliderTemperature.isEnabled =
                            currentlyOn

                        txtTemperature.setTextColor(
                            if (currentlyOn) {
                                Color.BLACK
                            } else {
                                Color.GRAY
                            }
                        )


                        updateDeviceInFirebase(
                            device
                        )
                    }
                }


                override fun onFinished(
                    device: Device
                ) {

                    runOnUiThread {

                        device.isOn =
                            false

                        device.status =
                            "OFF"

                        device.timer =
                            0

                        device.heating =
                            false

                        device.power =
                            0

                        device.current =
                            0.0

                        device.safetyMode =
                            "AUTO SHUTDOWN"


                        updateStatusText(
                            device,
                            txtStatus
                        )

                        txtSafety.text =
                            "Safety : AUTO SHUTDOWN"

                        txtRemaining.text =
                            "Remaining : 0 s"


                        sliderTemperature.isEnabled =
                            false

                        txtTemperature.setTextColor(
                            Color.GRAY
                        )


                        DeviceStateStorage.saveDevice(
                            device
                        )

                        updateDeviceInFirebase(
                            device
                        )
                    }
                }
            }
        )
    }


    // =========================================================
    // SWITCH INFORMATION
    // =========================================================

    private fun updateSwitchInformation(
        device: Device,
        txtStatus: TextView,
        txtPower: TextView,
        txtCurrent: TextView,
        txtEnergy: TextView
    ) {

        device.isOn =
            device.switch1 ||
                    device.switch2 ||
                    device.switch3


        device.status =
            if (device.isOn) {
                "ON"
            } else {
                "OFF"
            }

        device.power =
            (if (device.switch1) 1 else 0) +
                    (if (device.switch2) 1 else 0) +
                    (if (device.switch3) 1 else 0)

        device.current =
            if (device.voltage > 0) {
                device.power.toDouble() / device.voltage.toDouble()
            } else {
                0.0
            }


        updateStatusText(
            device,
            txtStatus
        )


        txtPower.text =
            "Power : ${device.power} W"


        txtCurrent.text =
            "Current : %.3f A"
                .format(device.current)


        txtEnergy.text =
            "Today's Energy : %.2f kWh"
                .format(device.energyToday)


        DeviceStateStorage.saveDevice(
            device
        )


        updateDeviceInFirebase(
            device
        )
    }


    // =========================================================
    // SWITCH LISTENERS
    // =========================================================

    private fun setSwitchListeners(

        device: Device,

        switchOne: MaterialSwitch,

        switchTwo: MaterialSwitch,

        switchThree: MaterialSwitch,

        txtStatus: TextView,

        txtPower: TextView,

        txtCurrent: TextView,

        txtEnergy: TextView

    ) {

        switchOne.setOnCheckedChangeListener {

                _,
                _ ->

            switchController.toggleSwitch1(
                device
            )


            updateSwitchInformation(

                device,

                txtStatus,
                txtPower,
                txtCurrent,
                txtEnergy
            )
        }


        switchTwo.setOnCheckedChangeListener {

                _,
                _ ->

            switchController.toggleSwitch2(
                device
            )


            updateSwitchInformation(

                device,

                txtStatus,
                txtPower,
                txtCurrent,
                txtEnergy
            )
        }


        switchThree.setOnCheckedChangeListener {

                _,
                _ ->

            switchController.toggleSwitch3(
                device
            )


            updateSwitchInformation(

                device,

                txtStatus,
                txtPower,
                txtCurrent,
                txtEnergy
            )
        }
    }


    // =========================================================
    // CONVERT FIREBASE DEVICE
    // =========================================================

    private fun convertFirebaseDevice(

        firebaseDevice: FirebaseDevice

    ): Device {

        return Device(

            id =
                firebaseDevice.id,

            name =
                firebaseDevice.name,

            type =
                firebaseDevice.type,

            status =
                firebaseDevice.status,

            isOn =
                when {
                    firebaseDevice.type
                        .trim()
                        .equals("light", ignoreCase = true) -> {

                        firebaseDevice.status
                            .equals("ON", ignoreCase = true)
                    }

                    firebaseDevice.type
                        .trim()
                        .equals("iron", ignoreCase = true) -> {

                        firebaseDevice.status
                            .equals("ON", ignoreCase = true) ||
                                firebaseDevice.isOn
                    }

                    else -> {

                        firebaseDevice.isOn
                    }
                },

            timer =
                firebaseDevice.timer,

            maxTime =
                firebaseDevice.maxTime,

            temperature =
                firebaseDevice.temperature,

            brightness =
                firebaseDevice.brightness,

            scheduleAction =
                firebaseDevice.scheduleAction,

            scheduleDueAt =
                firebaseDevice.scheduleDueAt,

            scheduleRemaining =
                firebaseDevice.scheduleRemaining,

            recording =
                firebaseDevice.recording,

            motionDetection =
                firebaseDevice.motionDetection,

            nightVision =
                firebaseDevice.nightVision,

            fps =
                firebaseDevice.fps,

            resolution =
                firebaseDevice.resolution,

            switch1 =
                firebaseDevice.switch1,

            switch2 =
                firebaseDevice.switch2,

            switch3 =
                firebaseDevice.switch3,

            targetTemperature =
                firebaseDevice.targetTemperature,

            heating =
                firebaseDevice.heating,

            safetyMode =
                firebaseDevice.safetyMode,

            power =
                firebaseDevice.power,

            voltage =
                firebaseDevice.voltage,

            current =
                firebaseDevice.current,

            energyToday =
                firebaseDevice.energyToday
        )
    }


    // =========================================================
    // UPDATE DEVICE IN FIREBASE
    // =========================================================

    private fun updateDeviceInFirebase(
        device: Device
    ) {

        firebaseRepository.updateDeviceState(

            device.id,

            mapOf(

                "isOn" to
                        device.isOn,

                "status" to
                        device.status,

                "power" to
                        device.power,

                "current" to
                        device.current,

                "energyToday" to
                        device.energyToday,

                "timer" to
                        device.timer,

                "heating" to
                        device.heating,

                "safetyMode" to
                        device.safetyMode,

                "temperature" to
                        device.temperature,

                "brightness" to
                        device.brightness,

                "scheduleAction" to device.scheduleAction,
                "scheduleDueAt" to device.scheduleDueAt,
                "scheduleRemaining" to device.scheduleRemaining,

                "switch1" to
                        device.switch1,

                "switch2" to
                        device.switch2,

                "switch3" to
                        device.switch3,

                "recording" to
                        device.recording,

                "motionDetection" to
                        device.motionDetection,

                "nightVision" to
                        device.nightVision
            ),

            onSuccess = {

                Log.d(
                    "FIREBASE",
                    "Device updated: ${device.name}"
                )
            },

            onError = { exception ->

                Log.e(
                    "FIREBASE",
                    "Failed to update device: ${device.name}",
                    exception
                )
            }
        )
    }


    // =========================================================
    // SCHEDULE STATUS
    // =========================================================

    private fun updateScheduleStatus(
        device: Device,
        txtScheduleStatus: TextView
    ) {
        val hasLocal = ScheduleService.hasSchedule(device.id)
        val hasRemote = device.scheduleDueAt > System.currentTimeMillis() || (device.scheduleRemaining > 0 && device.scheduleAction.isNotBlank())

        if (hasLocal || hasRemote) {
            val localMs = ScheduleService.remainingMillis(device.id)
            val remainingMillis = if (localMs > 0L) {
                localMs
            } else if (device.scheduleDueAt > 0L) {
                val rawMs = device.scheduleDueAt - System.currentTimeMillis()
                if (device.scheduleRemaining > 0 && Math.abs(rawMs - (device.scheduleRemaining * 1000L)) > 1500L) {
                    device.scheduleRemaining * 1000L
                } else {
                    rawMs.coerceAtLeast(0L)
                }
            } else if (device.scheduleRemaining > 0) {
                device.scheduleRemaining * 1000L
            } else {
                0L
            }

            val seconds = ((remainingMillis + 999L) / 1000L).coerceAtLeast(0L)
            val action = device.scheduleAction.trim().uppercase()

            txtScheduleStatus.text = when (action) {
                "ON" -> "Schedule: ON in ${seconds}s"
                "OFF" -> "Schedule: OFF in ${seconds}s"
                else -> if (action.isNotEmpty()) "Schedule: $action in ${seconds}s" else "Schedule: Pending (${seconds}s remaining)"
            }
        } else {
            txtScheduleStatus.text = "Schedule: Not Set"
        }
    }


// =========================================================
// LIVE SCHEDULE COUNTDOWN
// =========================================================

    private fun scheduleStatusUpdater(
        device: Device,
        txtScheduleStatus: TextView
    ) {

        // Stop any previous countdown
        scheduleUiRunnable?.let {
            scheduleUiHandler.removeCallbacks(it)
        }

        val runnable =
            object : Runnable {

                override fun run() {

                    val hasLocal = ScheduleService.hasSchedule(device.id)
                    val hasRemote = device.scheduleDueAt > System.currentTimeMillis() || (device.scheduleRemaining > 0 && device.scheduleAction.isNotBlank())

                    if (!hasLocal && !hasRemote) {
                        txtScheduleStatus.text = "Schedule: Not Set"
                        scheduleUiRunnable = null
                        return
                    }

                    val localMs = ScheduleService.remainingMillis(device.id)
                    val remainingMillis = if (localMs > 0L) {
                        localMs
                    } else if (device.scheduleDueAt > 0L) {
                        val rawMs = device.scheduleDueAt - System.currentTimeMillis()
                        if (device.scheduleRemaining > 0 && Math.abs(rawMs - (device.scheduleRemaining * 1000L)) > 1500L) {
                            device.scheduleRemaining * 1000L
                        } else {
                            rawMs.coerceAtLeast(0L)
                        }
                    } else if (device.scheduleRemaining > 0) {
                        device.scheduleRemaining * 1000L
                    } else {
                        0L
                    }

                    val seconds = ((remainingMillis + 999L) / 1000L).coerceAtLeast(0L)
                    val action = device.scheduleAction.trim().uppercase()

                    if (remainingMillis > 0L) {

                        device.scheduleRemaining =
                            seconds.toInt()

                        txtScheduleStatus.text =
                            when (action) {

                                "ON" ->
                                    "Schedule: ON in ${seconds}s"

                                "OFF" ->
                                    "Schedule: OFF in ${seconds}s"

                                else ->
                                    "Schedule: Pending (${seconds}s remaining)"
                            }

                        scheduleUiHandler.postDelayed(
                            this,
                            250L
                        )

                        return
                    }


                    // =================================================
                    // COUNTDOWN REACHED ZERO
                    // =================================================

                    // IMPORTANT:
                    // Update the CURRENT PAGE immediately.
                    // Do not wait for Firebase or a page refresh.

                    val txtStatus =
                        findViewById<TextView>(
                            R.id.txtStatus
                        )

                    when (action) {

                        "ON" -> {
                            com.example.smarthome.controller.DeviceController().turnOn(device)
                            if (device.type.equals("iron", ignoreCase = true)) {
                                device.timer = 120
                                device.heating = true
                                device.safetyMode = "SAFE"
                                val txtRemaining = findViewById<TextView>(R.id.txtRemaining)
                                val txtSafety = findViewById<TextView>(R.id.txtSafety)
                                val txtTemperature = findViewById<TextView>(R.id.txtTemperature)
                                val sliderTemperature = findViewById<Slider>(R.id.sliderTemperature)
                                if (txtStatus != null && txtRemaining != null && txtSafety != null && txtTemperature != null && sliderTemperature != null) {
                                    startIronTimer(device, txtStatus, txtRemaining, txtSafety, txtTemperature, sliderTemperature)
                                }
                            }
                        }

                        "OFF" -> {
                            com.example.smarthome.controller.DeviceController().turnOff(device)
                            if (device.type.equals("iron", ignoreCase = true)) {
                                com.example.smarthome.controller.IronController.stopIron(device)
                                device.timer = 0
                                device.heating = false
                                findViewById<TextView>(R.id.txtRemaining)?.text = "Remaining : 0 s"
                                findViewById<TextView>(R.id.txtSafety)?.text = "Safety : SAFE"
                            }
                        }
                    }

                    if (txtStatus != null) {
                        updateStatusText(
                            device,
                            txtStatus
                        )
                    }


                    // ---------------------------------------------
                    // UPDATE POWER / CURRENT / ENERGY
                    // ---------------------------------------------

                    val txtPower =
                        findViewById<TextView>(
                            R.id.txtPower
                        )

                    val txtVoltage =
                        findViewById<TextView>(
                            R.id.txtVoltage
                        )

                    val txtCurrent =
                        findViewById<TextView>(
                            R.id.txtCurrent
                        )

                    val txtEnergy =
                        findViewById<TextView>(
                            R.id.txtEnergy
                        )

                    updateLiveInformation(
                        device,
                        txtPower,
                        txtVoltage,
                        txtCurrent,
                        txtEnergy
                    )


                    // ---------------------------------------------
                    // UPDATE LIGHT CONTROLS
                    // ---------------------------------------------

                    if (
                        device.type.equals(
                            "light",
                            ignoreCase = true
                        )
                    ) {

                        val sliderBrightness =
                            findViewById<Slider>(
                                R.id.sliderBrightness
                            )

                        val txtBrightness =
                            findViewById<TextView>(
                                R.id.txtBrightness
                            )

                        updateLightControls(
                            device,
                            sliderBrightness,
                            txtBrightness
                        )
                    }


                    // ---------------------------------------------
                    // UPDATE SCHEDULE TEXT
                    // ---------------------------------------------

                    txtScheduleStatus.text =
                        when (action) {

                            "ON" ->
                                "Schedule: ON completed"

                            "OFF" ->
                                "Schedule: OFF completed"

                            else ->
                                "Schedule: Completed"
                        }


                    // ---------------------------------------------
                    // CLEAR LOCAL SCHEDULE DATA
                    // ---------------------------------------------

                    device.scheduleAction =
                        ""

                    device.scheduleDueAt =
                        0L

                    device.scheduleRemaining =
                        0


                    // ---------------------------------------------
                    // SAVE CURRENT STATE LOCALLY
                    // ---------------------------------------------

                    DeviceStateStorage.saveDevice(
                        device
                    )


                    // ---------------------------------------------
                    // UPDATE FIREBASE IMMEDIATELY
                    // ---------------------------------------------

                    updateDeviceInFirebase(
                        device
                    )


                    // ---------------------------------------------
                    // STOP THIS UI COUNTDOWN
                    // ---------------------------------------------

                    scheduleUiRunnable?.let {
                        scheduleUiHandler.removeCallbacks(
                            it
                        )
                    }

                    scheduleUiRunnable =
                        null
                }
            }


        scheduleUiRunnable =
            runnable


        // ---------------------------------------------
        // START IMMEDIATELY
        // ---------------------------------------------

        scheduleUiHandler.post(
            runnable
        )
    }

    // =========================================================
    // LIVE DEVICE STATUS UPDATE
    // =========================================================

    private fun updateLiveDeviceStatus(
        device: Device,
        txtStatus: TextView,
        txtCameraStatus: TextView? = null
    ) {

        val currentStatus = device.status.trim().uppercase()
        val isDeviceOn = currentStatus == "ON" || (currentStatus != "ERROR" && currentStatus != "DISCONNECTED" && device.isOn)

        device.isOn = isDeviceOn

        if (currentStatus != "ERROR" && currentStatus != "DISCONNECTED") {
            device.status = if (isDeviceOn) "ON" else "OFF"
        }

        // -----------------------------------------------------
        // MAIN STATUS
        // -----------------------------------------------------

        updateStatusText(
            device,
            txtStatus
        )


        // -----------------------------------------------------
        // CAMERA STATUS
        // -----------------------------------------------------

        if (
            device.type.equals(
                "camera",
                ignoreCase = true
            )
        ) {

            txtCameraStatus?.text =
                when (device.status.uppercase()) {
                    "ON" -> "LIVE"
                    "ERROR" -> "HARDWARE ERROR"
                    "DISCONNECTED" -> "DISCONNECTED"
                    else -> "OFFLINE"
                }
        }
    }
}
