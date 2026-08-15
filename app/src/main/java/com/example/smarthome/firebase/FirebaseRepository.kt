package com.example.smarthome.firebase

import com.example.smarthome.models.Device
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
class FirebaseRepository {

    private val db =
        FirebaseFirestore.getInstance()

    // ==========================================
    // GET FLOORS
    // ==========================================

    fun getFloors(
        onResult: (List<FirebaseFloor>) -> Unit
    ) {

        db.collection("floors")
            .orderBy("order")
            .get()
            .addOnSuccessListener { result ->

                val floors =
                    mutableListOf<FirebaseFloor>()

                for (document in result.documents) {

                    val floor =
                        document.toObject(
                            FirebaseFloor::class.java
                        )

                    if (floor != null) {

                        floor.id =
                            document.id

                        floors.add(floor)
                    }
                }

                onResult(floors)
            }
    }

    // ==========================================
    // GET ROOMS FOR A FLOOR
    // ==========================================

    fun getRooms(
        floorId: String,
        onResult: (List<FirebaseRoom>) -> Unit
    ) {

        db.collection("rooms")
            .whereEqualTo(
                "floorId",
                floorId
            )
            .get()
            .addOnSuccessListener { result ->

                val rooms =
                    mutableListOf<FirebaseRoom>()

                for (document in result.documents) {

                    val room =
                        document.toObject(
                            FirebaseRoom::class.java
                        )

                    if (room != null) {

                        room.id =
                            document.id

                        rooms.add(room)
                    }
                }

                onResult(rooms)
            }
    }

// ==========================================
// GET DEVICES FOR A ROOM
// ==========================================

    fun getDevices(
        roomId: String,
        onResult: (List<FirebaseDevice>) -> Unit
    ) {

        db.collection("devices")
            .whereEqualTo("roomId", roomId)
            .get()
            .addOnSuccessListener { result ->

                val devices = mutableListOf<FirebaseDevice>()

                for (document in result.documents) {

                    val device =
                        document.toObject(
                            FirebaseDevice::class.java
                        )

                    if (device != null) {
                        device.id = document.id
                        devices.add(device)
                    }
                }

                onResult(devices)
            }
    }

    // ==========================================
    // LISTEN TO ROOM DEVICES REAL TIME
    // ==========================================

    fun listenToDevices(
        roomId: String,
        onChanged: (List<FirebaseDevice>) -> Unit,
        onError: (Exception) -> Unit = {}
    ): ListenerRegistration {

        return db.collection("devices")
            .whereEqualTo("roomId", roomId)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                if (snapshot == null) return@addSnapshotListener

                val devices = mutableListOf<FirebaseDevice>()

                for (document in snapshot.documents) {

                    val device =
                        document.toObject(
                            FirebaseDevice::class.java
                        )

                    if (device != null) {
                        device.id = document.id
                        devices.add(device)
                    }
                }

                onChanged(devices)
            }
    }

    fun getDeviceById(
        deviceId: String,
        onResult: (FirebaseDevice?) -> Unit
    ) {

        db.collection("devices")
            .document(deviceId)
            .get()
            .addOnSuccessListener { document ->

                if (document.exists()) {

                    val device =
                        document.toObject(
                            FirebaseDevice::class.java
                        )

                    if (device != null) {
                        device.id = document.id
                    }

                    onResult(device)

                } else {

                    onResult(null)
                }
            }
    }

    fun updateDeviceState(
        deviceId: String,
        updates: Map<String, Any?>,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {

        db.collection("devices")
            .document(deviceId)
            .update(updates)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }

    fun listenToDevice(
        deviceId: String,
        onChanged: (FirebaseDevice) -> Unit,
        onError: (Exception) -> Unit = {}
    ): ListenerRegistration {

        return db.collection("devices")
            .document(deviceId)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {

                    onError(error)

                    return@addSnapshotListener
                }

                if (
                    snapshot != null &&
                    snapshot.exists()
                ) {

                    val device =
                        snapshot.toObject(
                            FirebaseDevice::class.java
                        )

                    if (device != null) {

                        device.id =
                            snapshot.id

                        onChanged(device)
                    }
                }
            }
    }

    // ==========================================
// LISTEN TO ALL DEVICES IN REAL TIME
// ==========================================

    fun listenToAllDevices(
        onChanged: (List<FirebaseDevice>) -> Unit,
        onError: (Exception) -> Unit = {}
    ): ListenerRegistration {

        return db.collection("devices")
            .addSnapshotListener { snapshot, error ->

                if (error != null) {

                    onError(error)

                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    return@addSnapshotListener
                }

                val devices =
                    mutableListOf<FirebaseDevice>()

                for (document in snapshot.documents) {

                    val device =
                        document.toObject(
                            FirebaseDevice::class.java
                        )

                    if (device != null) {

                        device.id =
                            document.id

                        devices.add(device)
                    }
                }

                onChanged(devices)
            }
    }
    fun turnIronOn(
        device: Device,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {

        db.collection("devices")
            .document(device.id)
            .update(
                mapOf(
                    "isOn" to true,
                    "status" to "ON",
                    "timer" to device.maxTime,
                    "heating" to true,
                    "power" to 1200,
                    "current" to (
                            if (device.voltage > 0) {
                                1200.0 /
                                        device.voltage.toDouble()
                            } else {
                                0.0
                            }
                            ),
                    "safetyMode" to "SAFE"
                )
            )
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener {
                onError(it)
            }
    }

    fun turnIronOff(
        device: Device,
        safetyShutdown: Boolean = false,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {

        db.collection("devices")
            .document(device.id)
            .update(
                mapOf(
                    "isOn" to false,
                    "status" to "OFF",
                    "timer" to 0,
                    "heating" to false,
                    "power" to 0,
                    "current" to 0.0,
                    "safetyMode" to
                            if (safetyShutdown)
                                "AUTO SHUTDOWN"
                            else
                                "SAFE"
                )
            )
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener {
                onError(it)
            }
    }

    // ==========================================
    // LISTEN TO FLOORS REAL TIME
    // ==========================================

    fun listenToFloors(
        onChanged: (List<FirebaseFloor>) -> Unit,
        onError: (Exception) -> Unit = {}
    ): ListenerRegistration {

        return db.collection("floors")
            .orderBy("order")
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                if (snapshot == null) return@addSnapshotListener

                val floors = mutableListOf<FirebaseFloor>()

                for (document in snapshot.documents) {

                    val floor = document.toObject(FirebaseFloor::class.java)

                    if (floor != null) {
                        floor.id = document.id
                        floors.add(floor)
                    }
                }

                onChanged(floors)
            }
    }

    // ==========================================
    // ADD FLOOR
    // ==========================================

    fun addFloor(
        floorName: String,
        order: Int,
        icon: String = "floor",
        onSuccess: (String) -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        val newFloor = mapOf(
            "floorName" to floorName,
            "name" to floorName,
            "order" to order,
            "floorIcon" to icon,
            "icon" to icon
        )

        db.collection("floors")
            .add(newFloor)
            .addOnSuccessListener { docRef ->
                onSuccess(docRef.id)
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }

    // ==========================================
    // ADD ROOM
    // ==========================================

    fun addRoom(
        roomName: String,
        floorId: String,
        icon: String = "room",
        onSuccess: (String) -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        val newRoom = mapOf(
            "roomName" to roomName,
            "name" to roomName,
            "floorId" to floorId,
            "roomIcon" to icon,
            "icon" to icon
        )

        db.collection("rooms")
            .add(newRoom)
            .addOnSuccessListener { docRef ->
                onSuccess(docRef.id)
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }

    // ==========================================
    // ADD DEVICE
    // ==========================================

    fun addDevice(
        name: String,
        type: String,
        roomId: String,
        onSuccess: (String) -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        val defaultPower = when (type.lowercase()) {
            "iron" -> 1200
            "outlet" -> 250
            "light" -> 15
            "camera" -> 12
            "switch" -> 3
            else -> 10
        }

        val newDevice = mapOf(
            "name" to name,
            "type" to type,
            "status" to "OFF",
            "isOn" to false,
            "roomId" to roomId,
            "power" to defaultPower,
            "voltage" to 230,
            "current" to 0.0,
            "energyToday" to 0.0,
            "brightness" to 80,
            "maxTime" to 120,
            "timer" to 120,
            "safetyMode" to "SAFE",
            "switch1" to false,
            "switch2" to false,
            "switch3" to false
        )

        db.collection("devices")
            .add(newDevice)
            .addOnSuccessListener { docRef ->
                onSuccess(docRef.id)
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }
}