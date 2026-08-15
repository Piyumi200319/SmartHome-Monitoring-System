package com.example.smarthome.activities

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smarthome.R
import com.example.smarthome.adapters.RoomAdapter
import com.example.smarthome.firebase.FirebaseRepository
import com.example.smarthome.models.Device
import com.example.smarthome.models.Room
import com.example.smarthome.service.DeviceStateStorage
import com.example.smarthome.service.ScheduleService

class FloorActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView

    private val firebaseRepository =
        FirebaseRepository()

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_floor
        )

        DeviceStateStorage.initialize(this)
        ScheduleService.initialize(this)

        // ==========================================
        // GET FLOOR INFORMATION
        // ==========================================

        val floorId =
            intent.getStringExtra(
                "FLOOR_ID"
            )

        val floorName =
            intent.getStringExtra(
                "FLOOR_NAME"
            ) ?: "Floor"

        // ==========================================
        // TITLE
        // ==========================================

        findViewById<TextView>(
            R.id.txtFloorTitle
        ).text = floorName

        val imgFloorPlanGrid = findViewById<android.widget.ImageView>(R.id.imgFloorPlanGrid)
        val txtFloorPlanCaption = findViewById<TextView>(R.id.txtFloorPlanCaption)

        val planRes = when {
            "ground" in floorName.lowercase() -> R.drawable.ic_floor_plan_ground
            "first" in floorName.lowercase() -> R.drawable.ic_floor_plan_first
            else -> R.drawable.ic_floor_plan_default
        }
        imgFloorPlanGrid?.setImageResource(planRes)
        txtFloorPlanCaption?.text = "$floorName Grid Plan • Zone Coordinates & Mapping"

        // ==========================================
        // RECYCLER
        // ==========================================

        recycler =
            findViewById(
                R.id.rvFloors
            )

        recycler.layoutManager =
            GridLayoutManager(
                this,
                2
            )

        // ==========================================
        // CHECK FLOOR ID
        // ==========================================

        if (floorId.isNullOrBlank()) {

            Log.e(
                "FIREBASE",
                "FLOOR_ID is empty"
            )

            return
        }

        Log.d(
            "FIREBASE",
            "Opening floor: $floorName"
        )

        Log.d(
            "FIREBASE",
            "Floor ID: $floorId"
        )

        // ==========================================
        // GET ONLY ROOMS FOR THIS FLOOR
        // ==========================================

        firebaseRepository.getRooms(
            floorId
        ) { firebaseRooms ->

            Log.d(
                "FIREBASE",
                "Rooms returned for $floorName: " +
                        firebaseRooms.size
            )

            if (firebaseRooms.isEmpty()) {

                runOnUiThread {

                    recycler.adapter =
                        RoomAdapter(
                            mutableListOf()
                        )
                }

                return@getRooms
            }

            val rooms =
                mutableListOf<Room>()

            var completedRooms = 0

            // ==========================================
            // LOAD EACH ROOM
            // ==========================================

            for (firebaseRoom in firebaseRooms) {

                Log.d(
                    "FIREBASE",
                    "Room: ${effectiveRoomName(firebaseRoom)}"
                )

                Log.d(
                    "FIREBASE",
                    "Room ID: ${firebaseRoom.id}"
                )

                Log.d(
                    "FIREBASE",
                    "Room floorId: ${firebaseRoom.floorId}"
                )

                // ======================================
                // GET ONLY DEVICES FOR THIS ROOM
                // ======================================

                firebaseRepository.getDevices(
                    firebaseRoom.id
                ) { firebaseDevices ->

                    val devices =
                        mutableListOf<Device>()

                    // ==================================
                    // CONVERT DEVICES
                    // ==================================

                    for (
                    firebaseDevice
                    in firebaseDevices
                    ) {

                        Log.d(
                            "FIREBASE",
                            "Device: " +
                                    "${firebaseDevice.name}"
                        )

                        Log.d(
                            "FIREBASE",
                            "Device ID: " +
                                    "${firebaseDevice.id}"
                        )

                        Log.d(
                            "FIREBASE",
                            "Device roomId: " +
                                    "${firebaseDevice.roomId}"
                        )

                        val device =
                            Device(

                                id =
                                    firebaseDevice.id,

                                name =
                                    firebaseDevice.name,

                                type =
                                    firebaseDevice.type,

                                status =
                                    firebaseDevice.status,

                                isOn =
                                    firebaseDevice.isOn,

                                timer =
                                    firebaseDevice.timer,

                                maxTime =
                                    firebaseDevice.maxTime,

                                temperature =
                                    firebaseDevice.temperature,

                                brightness =
                                    firebaseDevice.brightness,

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

                        ScheduleService
                            .attachDevice(
                                device
                            )

                        devices.add(
                            device
                        )
                    }

                    // ==================================
                    // CREATE ROOM
                    // ==================================

                    val room =
                        Room(
                            roomName = effectiveRoomName(firebaseRoom),
                            devices = devices,
                            firebaseId = firebaseRoom.id,
                            icon = effectiveRoomIcon(firebaseRoom)
                        )

                    synchronized(rooms) {

                        rooms.add(room)

                        completedRooms++

                        Log.d(
                            "FIREBASE",
                            "Loaded: " +
                                    "${room.roomName} -> " +
                                    "${room.devices.size} devices"
                        )

                        // ==================================
                        // ALL ROOMS LOADED
                        // ==================================

                        if (
                            completedRooms ==
                            firebaseRooms.size
                        ) {

                            runOnUiThread {

                                val totalDevs = rooms.sumOf { it.devices.size }
                                findViewById<TextView>(R.id.txtFloorPlanCaption)?.text =
                                    "$floorName Grid Plan • ${rooms.size} Zones • $totalDevs Devices"

                                recycler.adapter =
                                    RoomAdapter(
                                        rooms
                                    )
                            }
                        }
                    }
                }
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

    private fun effectiveRoomIcon(
        room: com.example.smarthome.firebase.FirebaseRoom
    ): String {
        return room.roomIcon.trim()
            .ifBlank { room.icon.trim() }
            .ifBlank { room.emoji.trim() }
            .ifBlank { getRoomIcon(effectiveRoomName(room)) }
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
}