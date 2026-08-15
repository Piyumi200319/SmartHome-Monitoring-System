package com.example.smarthome.activities

import com.google.firebase.firestore.ListenerRegistration
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smarthome.R
import com.example.smarthome.adapters.FloorAdapter

import com.example.smarthome.service.DeviceStateStorage
import com.example.smarthome.service.NotificationService
import com.example.smarthome.service.ScheduleService
import com.example.smarthome.activities.ReportActivity
import com.google.firebase.firestore.FirebaseFirestore

import com.example.smarthome.firebase.FirebaseRepository
import android.util.Log

class MainActivity : AppCompatActivity() {
    private val dashboardDevices =
        mutableListOf<com.example.smarthome.models.Device>()

    private var allDevicesListener: ListenerRegistration? =
        null
    private lateinit var recyclerView: RecyclerView

    private val notificationPermissionRequestCode = 100

    private lateinit var txtTotalDevices: TextView
    private lateinit var txtDevicesOn: TextView
    private lateinit var txtDevicesOff: TextView
    private lateinit var txtTotalPower: TextView
    private val loadedFloorsList = mutableListOf<com.example.smarthome.models.Floor>()

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_main
        )

        val db = FirebaseFirestore.getInstance()

        db.collection("test")
            .document("connection")
            .set(mapOf("status" to "Firebase Connected"))

        // ==================================================
        // INITIALIZE SERVICES
        // ==================================================

        NotificationService.initialize(this)

        DeviceStateStorage.initialize(this)
        ScheduleService.initialize(this)

        requestNotificationPermission()

        // ==================================================
        // DASHBOARD VIEWS
        // ==================================================

        txtTotalDevices =
            findViewById(
                R.id.txtTotalDevices
            )

        txtDevicesOn =
            findViewById(
                R.id.txtDevicesOn
            )

        txtDevicesOff =
            findViewById(
                R.id.txtDevicesOff
            )

        txtTotalPower =
            findViewById(
                R.id.txtTotalPower
            )

        // ==================================================
        // UPDATE DASHBOARD
        // ==================================================

        updateDashboard()
// ==================================================
// ROOMS + DEVICES FROM FIREBASE
// ==================================================

        recyclerView =
            findViewById(R.id.rvRooms)

        recyclerView.layoutManager =
            LinearLayoutManager(this)

        val firebaseRepository =
            FirebaseRepository()

        // ==================================================
// REAL-TIME DEVICE DASHBOARD LISTENER
// ==================================================

        allDevicesListener =
            firebaseRepository.listenToAllDevices(

                onChanged = { firebaseDevices ->

                    runOnUiThread {

                        synchronized(dashboardDevices) {

                            for (
                            firebaseDevice in firebaseDevices
                            ) {

                                val existingDevice =
                                    dashboardDevices.find {
                                        it.id ==
                                                firebaseDevice.id
                                    }

                                if (
                                    existingDevice != null
                                ) {

                                    // ----------------------------------
                                    // UPDATE EXISTING DEVICE
                                    // ----------------------------------

                                    existingDevice.status =
                                        firebaseDevice.status

                                    existingDevice.isOn =
                                        firebaseDevice.status
                                            .equals(
                                                "ON",
                                                ignoreCase = true
                                            )

                                    existingDevice.power =
                                        firebaseDevice.power

                                    existingDevice.current =
                                        firebaseDevice.current

                                    existingDevice.energyToday =
                                        firebaseDevice.energyToday

                                    existingDevice.brightness =
                                        firebaseDevice.brightness

                                    existingDevice.temperature =
                                        firebaseDevice.temperature

                                    existingDevice.timer =
                                        firebaseDevice.timer

                                    existingDevice.switch1 =
                                        firebaseDevice.switch1

                                    existingDevice.switch2 =
                                        firebaseDevice.switch2

                                    existingDevice.switch3 =
                                        firebaseDevice.switch3

                                } else {

                                    // ----------------------------------
                                    // INSTANTLY ADD NEW DEVICE TO DASHBOARD
                                    // ----------------------------------
                                    val newDevice = com.example.smarthome.models.Device(
                                        id = firebaseDevice.id,
                                        name = firebaseDevice.name,
                                        type = firebaseDevice.type,
                                        status = firebaseDevice.status,
                                        isOn = firebaseDevice.status.equals("ON", ignoreCase = true),
                                        power = firebaseDevice.power,
                                        voltage = firebaseDevice.voltage,
                                        current = firebaseDevice.current,
                                        energyToday = firebaseDevice.energyToday,
                                        brightness = firebaseDevice.brightness,
                                        temperature = firebaseDevice.temperature,
                                        timer = firebaseDevice.timer,
                                        maxTime = firebaseDevice.maxTime,
                                        safetyMode = firebaseDevice.safetyMode,
                                        resolution = firebaseDevice.resolution,
                                        fps = firebaseDevice.fps,
                                        recording = firebaseDevice.recording,
                                        motionDetection = firebaseDevice.motionDetection,
                                        nightVision = firebaseDevice.nightVision,
                                        switch1 = firebaseDevice.switch1,
                                        switch2 = firebaseDevice.switch2,
                                        switch3 = firebaseDevice.switch3
                                    )
                                    dashboardDevices.add(newDevice)
                                }
                            }
                        }


                        // ------------------------------------------
                        // UPDATE MAIN DASHBOARD
                        // ------------------------------------------

                        updateDashboard()


                        // ------------------------------------------
                        // UPDATE DEVICE CARDS
                        // ------------------------------------------

                        if (
                            ::recyclerView.isInitialized
                        ) {

                            recyclerView.adapter
                                ?.notifyDataSetChanged()
                        }
                    }
                },

                onError = { exception ->

                    Log.e(
                        "MAIN_DEVICE_LISTENER",
                        "Failed to listen to devices",
                        exception
                    )
                }
            )

        firebaseRepository.getFloors { firebaseFloors ->

            val floors =
                mutableListOf<com.example.smarthome.models.Floor>()

            if (firebaseFloors.isEmpty()) {

                runOnUiThread {

                    recyclerView.adapter =
                        FloorAdapter(floors)
                }

                return@getFloors
            }

            var completedFloors = 0

            for (firebaseFloor in firebaseFloors) {

                firebaseRepository.getRooms(
                    firebaseFloor.id
                ) { firebaseRooms ->

                    val rooms =
                        mutableListOf<com.example.smarthome.models.Room>()

                    if (firebaseRooms.isEmpty()) {

                        val floor =
                            com.example.smarthome.models.Floor(
                                floorName =
                                    effectiveFloorName(firebaseFloor),
                                rooms =
                                    rooms,
                                firebaseId = firebaseFloor.id,
                                icon = effectiveFloorIcon(firebaseFloor)
                            )

                        synchronized(floors) {

                            floors.add(floor)
                            completedFloors++

                            if (
                                completedFloors ==
                                firebaseFloors.size
                            ) {

                                runOnUiThread {

                                    recyclerView.adapter =
                                        FloorAdapter(floors)

                                    updateDashboard()
                                }
                            }
                        }

                        return@getRooms
                    }

                    var completedRooms = 0

                    for (firebaseRoom in firebaseRooms) {

                        firebaseRepository.getDevices(
                            firebaseRoom.id
                        ) { firebaseDevices ->

                            val devices =
                                mutableListOf<com.example.smarthome.models.Device>()

                            for (firebaseDevice in firebaseDevices) {

                                /*
                                 * IMPORTANT:
                                 * Firebase document ID is the ID
                                 * that DeviceDetailActivity must receive.
                                 *
                                 * Your Device model must therefore
                                 * contain this Firebase ID.
                                 */

                                val device =
                                    com.example.smarthome.models.Device(
                                        id = firebaseDevice.id,
                                        name = firebaseDevice.name,
                                        type = firebaseDevice.type,
                                        status = firebaseDevice.status,
                                        isOn = firebaseDevice.isOn,
                                        power = firebaseDevice.power,
                                        voltage = firebaseDevice.voltage,
                                        current = firebaseDevice.current,
                                        energyToday = firebaseDevice.energyToday,
                                        brightness = firebaseDevice.brightness,
                                        temperature = firebaseDevice.temperature,
                                        timer = firebaseDevice.timer,
                                        maxTime = firebaseDevice.maxTime,
                                        safetyMode = firebaseDevice.safetyMode,
                                        resolution = firebaseDevice.resolution,
                                        fps = firebaseDevice.fps,
                                        recording = firebaseDevice.recording,
                                        motionDetection =
                                            firebaseDevice.motionDetection,
                                        nightVision =
                                            firebaseDevice.nightVision,
                                        switch1 = firebaseDevice.switch1,
                                        switch2 = firebaseDevice.switch2,
                                        switch3 = firebaseDevice.switch3
                                    )
                                devices.add(device)

                                synchronized(dashboardDevices) {

                                    dashboardDevices.removeAll {
                                        it.id == device.id
                                    }

                                    dashboardDevices.add(device)
                                }
                            }

                            val room =
                                com.example.smarthome.models.Room(
                                    roomName =
                                        effectiveRoomName(firebaseRoom),
                                    devices =
                                        devices,
                                    firebaseId =
                                        firebaseRoom.id,
                                    icon = effectiveRoomIcon(firebaseRoom)
                                )

                            synchronized(rooms) {

                                rooms.add(room)
                                completedRooms++

                                if (
                                    completedRooms ==
                                    firebaseRooms.size
                                ) {

                                    val floor =
                                        com.example.smarthome.models.Floor(
                                            floorName =
                                                effectiveFloorName(firebaseFloor),
                                            rooms =
                                                rooms,
                                            firebaseId =
                                                firebaseFloor.id,
                                            icon = effectiveFloorIcon(firebaseFloor)
                                        )

                                    synchronized(floors) {

                                        floors.add(floor)
                                        completedFloors++

                                        if (
                                            completedFloors ==
                                            firebaseFloors.size
                                        ) {

                                            runOnUiThread {

                                                Log.d(
                                                    "FIREBASE",
                                                    "Floors loaded: ${floors.size}"
                                                )

                                                for (
                                                loadedFloor in floors
                                                ) {

                                                    Log.d(
                                                        "FIREBASE",
                                                        "${loadedFloor.floorName}: " +
                                                                "${loadedFloor.rooms.size} rooms"
                                                    )

                                                    for (
                                                    loadedRoom in
                                                    loadedFloor.rooms
                                                    ) {

                                                        Log.d(
                                                            "FIREBASE",
                                                            "${loadedRoom.roomName}: " +
                                                                    "${loadedRoom.devices.size} devices"
                                                        )
                                                    }
                                                }

                                                recyclerView.adapter =
                                                    FloorAdapter(floors)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }


        // ==================================================
        // NOTIFICATIONS BUTTON
        // ==================================================

        val btnNotifications =
            findViewById<Button>(
                R.id.btnNotifications
            )
        val btnReports =
            findViewById<com.google.android.material.button.MaterialButton>(
                R.id.btnReports
            )

        btnNotifications.setOnClickListener {

            val intent =
                Intent(
                    this,
                    NotificationActivity::class.java
                )

            startActivity(intent)
        }
        btnReports.setOnClickListener {

            val intent =
                Intent(
                    this,
                    ReportActivity::class.java
                )

            startActivity(intent)
        }

        val btnAddFloor = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnAddFloor)

        btnAddFloor.setOnClickListener {
            showAddFloorDialog(firebaseRepository)
        }
    }


    // ==================================================
    // UPDATE DASHBOARD
    // ==================================================

    private fun updateDashboard() {

        var totalDevices = 0
        var devicesOn = 0
        var devicesOff = 0
        var totalPower = 0

        for (device in dashboardDevices) {

            totalDevices++

            if (device.isOn || device.status.equals("ON", ignoreCase = true)) {
                devicesOn++
                totalPower += device.power
            } else {
                devicesOff++
            }
        }

        txtTotalDevices.text =
            totalDevices.toString()

        txtDevicesOn.text =
            devicesOn.toString()

        txtDevicesOff.text =
            devicesOff.toString()

        txtTotalPower.text =
            "$totalPower W"
    }

    // ==================================================
    // REFRESH WHEN RETURNING TO HOME
    // ==================================================

    override fun onResume() {

        super.onResume()

        if (::txtTotalDevices.isInitialized) {

            updateDashboard()
        }

        if (::recyclerView.isInitialized) {

            recyclerView.adapter?.notifyDataSetChanged()
        }
    }

    // ==================================================
    // NOTIFICATION PERMISSION
    // ==================================================

    private fun requestNotificationPermission() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {

            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.POST_NOTIFICATIONS
                    ),
                    notificationPermissionRequestCode
                )
            }
        }
    }

    // ==================================================
    // SAVE DEVICE STATES
    // ==================================================



    private fun effectiveFloorName(
        floor: com.example.smarthome.firebase.FirebaseFloor
    ): String {
        return floor.floorName.trim().ifBlank {
            floor.name.trim()
        }.ifBlank {
            when (floor.order) {
                1 -> "Ground Floor"
                2 -> "First Floor"
                3 -> "Second Floor"
                else -> "Floor"
            }
        }
    }

    private fun effectiveRoomName(
        room: com.example.smarthome.firebase.FirebaseRoom
    ): String {
        return room.roomName.trim().ifBlank {
            room.name.trim()
        }.ifBlank {
            "Room"
        }
    }

    private fun effectiveFloorIcon(
        floor: com.example.smarthome.firebase.FirebaseFloor
    ): String {
        return floor.floorIcon.trim()
            .ifBlank { floor.icon.trim() }
            .ifBlank { floor.emoji.trim() }
            .ifBlank { getFloorIcon(effectiveFloorName(floor)) }
    }

    private fun effectiveRoomIcon(
        room: com.example.smarthome.firebase.FirebaseRoom
    ): String {
        return room.roomIcon.trim()
            .ifBlank { room.icon.trim() }
            .ifBlank { room.emoji.trim() }
            .ifBlank { getRoomIcon(effectiveRoomName(room)) }
    }

    private fun getFloorIcon(floorName: String): String {
        val name = floorName.trim().lowercase()
        return when {
            "ground" in name || "main" in name -> "ground"
            "first" in name || "1st" in name -> "first"
            "second" in name || "2nd" in name -> "second"
            "third" in name || "3rd" in name -> "third"
            "basement" in name -> "basement"
            else -> "floor"
        }
    }

    private fun getRoomIcon(roomName: String): String {
        val name = roomName.trim().lowercase()
        return when {
            "kitchen" in name -> "kitchen"
            "living" in name || "lounge" in name -> "living"
            "garage" in name -> "garage"
            "bedroom" in name || "bed room" in name -> "bedroom"
            "study" in name || "office" in name -> "office"
            "bath" in name || "toilet" in name -> "bathroom"
            "dining" in name -> "dining"
            "garden" in name || "yard" in name -> "garden"
            "balcony" in name -> "balcony"
            "hall" in name -> "hall"
            else -> "room"
        }


    }

    // ==================================================
    // ADD FLOOR DIALOG
    // ==================================================

    private fun showAddFloorDialog(firebaseRepository: FirebaseRepository) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Add New House Floor")

        val scrollView = android.widget.ScrollView(this)
        val container = android.widget.LinearLayout(this)
        container.orientation = android.widget.LinearLayout.VERTICAL
        container.setPadding(40, 24, 40, 24)

        // Subtitle
        val subTxt = android.widget.TextView(this)
        subTxt.text = "Configure floor name and initial devices for this floor:"
        subTxt.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 13f)
        subTxt.setTextColor(android.graphics.Color.parseColor("#64748B"))
        subTxt.setPadding(0, 0, 0, 16)
        container.addView(subTxt)

        // Floor Name Label
        val lblName = android.widget.TextView(this)
        lblName.text = "Floor Name"
        lblName.setTypeface(null, android.graphics.Typeface.BOLD)
        lblName.setTextColor(android.graphics.Color.parseColor("#1E293B"))
        container.addView(lblName)

        val inputName = android.widget.EditText(this)
        inputName.hint = "e.g. Second Floor / Roof Terrace"
        inputName.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14f)
        container.addView(inputName)

        // Primary Room Label
        val lblRoom = android.widget.TextView(this)
        lblRoom.text = "\nPrimary Room Name"
        lblRoom.setTypeface(null, android.graphics.Typeface.BOLD)
        lblRoom.setTextColor(android.graphics.Color.parseColor("#1E293B"))
        container.addView(lblRoom)

        val inputRoom = android.widget.EditText(this)
        inputRoom.hint = "e.g. Master Suite / Main Hall"
        inputRoom.setText("Main Room")
        inputRoom.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14f)
        container.addView(inputRoom)

        // Floor Devices Section Label
        val lblDevices = android.widget.TextView(this)
        lblDevices.text = "\nInclude Initial Floor Devices"
        lblDevices.setTypeface(null, android.graphics.Typeface.BOLD)
        lblDevices.setTextColor(android.graphics.Color.parseColor("#1E293B"))
        container.addView(lblDevices)

        val chkLight = android.widget.CheckBox(this).apply { text = "Ceiling Light"; isChecked = true }
        val chkOutlet = android.widget.CheckBox(this).apply { text = "Wall Power Outlet"; isChecked = true }
        val chkSwitch = android.widget.CheckBox(this).apply { text = "Multi-Switch Unit"; isChecked = false }
        val chkIron = android.widget.CheckBox(this).apply { text = "Safety Iron Appliance"; isChecked = false }
        val chkCamera = android.widget.CheckBox(this).apply { text = "Security Camera"; isChecked = false }

        container.addView(chkLight)
        container.addView(chkOutlet)
        container.addView(chkSwitch)
        container.addView(chkIron)
        container.addView(chkCamera)

        scrollView.addView(container)
        builder.setView(scrollView)

        builder.setPositiveButton("Create Floor") { dialog, _ ->
            val floorName = inputName.text.toString().trim()
            val roomName = inputRoom.text.toString().trim().ifBlank { "Main Room" }

            if (floorName.isNotBlank()) {
                val icon = getFloorIcon(floorName)
                firebaseRepository.addFloor(
                    floorName = floorName,
                    order = dashboardDevices.size + 1,
                    icon = icon,
                    onSuccess = { newFloorId ->
                        // Add primary room to floor
                        firebaseRepository.addRoom(
                            roomName = roomName,
                            floorId = newFloorId,
                            icon = getRoomIcon(roomName),
                            onSuccess = { newRoomId ->
                                // Add selected devices
                                if (chkLight.isChecked) {
                                    firebaseRepository.addDevice("${floorName} Light", "Light", newRoomId)
                                }
                                if (chkOutlet.isChecked) {
                                    firebaseRepository.addDevice("${floorName} Outlet", "Outlet", newRoomId)
                                }
                                if (chkSwitch.isChecked) {
                                    firebaseRepository.addDevice("${floorName} Multi-Switch", "Switch", newRoomId)
                                }
                                if (chkIron.isChecked) {
                                    firebaseRepository.addDevice("${floorName} Iron", "Iron", newRoomId)
                                }
                                if (chkCamera.isChecked) {
                                    firebaseRepository.addDevice("${floorName} Camera", "Camera", newRoomId)
                                }

                                android.widget.Toast.makeText(
                                    this,
                                    "Floor '$floorName' created with devices!",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                                recreate()
                            }
                        )
                    },
                    onError = { ex ->
                        android.widget.Toast.makeText(
                            this,
                            "Failed to create floor: ${ex.message}",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                )
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    // ==================================================
    // STOP REAL-TIME DEVICE LISTENER
    // ==================================================

    override fun onDestroy() {

        allDevicesListener?.remove()

        allDevicesListener =
            null

        super.onDestroy()
    }
}