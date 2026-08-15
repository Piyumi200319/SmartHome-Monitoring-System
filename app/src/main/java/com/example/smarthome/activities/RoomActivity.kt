package com.example.smarthome.activities

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smarthome.R
import com.example.smarthome.adapters.DeviceAdapter
import com.example.smarthome.firebase.FirebaseRepository
import com.example.smarthome.models.Device
import com.example.smarthome.service.DeviceStateNotifier
import com.example.smarthome.service.DeviceStateStorage
import com.google.firebase.firestore.ListenerRegistration

class RoomActivity : AppCompatActivity() {

    private lateinit var deviceAdapter: DeviceAdapter
    private lateinit var recycler: RecyclerView
    private lateinit var deviceList: MutableList<Device>

    private val deviceChangeListener:
                (Device) -> Unit =
        { changedDevice ->

            runOnUiThread {

                recycler.post {

                    val position =
                        deviceList.indexOfFirst {
                            it.id == changedDevice.id
                        }

                    if (
                        position != -1 &&
                        !recycler.isComputingLayout
                    ) {

                        deviceList[position] =
                            changedDevice

                        deviceAdapter.notifyItemChanged(
                            position
                        )
                    }
                }
            }
        }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_room
        )

        DeviceStateStorage.initialize(this)

        val roomName =
            intent.getStringExtra(
                "ROOM_NAME"
            ) ?: ""

        findViewById<TextView>(
            R.id.txtRoomTitle
        ).text = roomName

        recycler =
            findViewById(
                R.id.rvDevices
            )

        recycler.layoutManager =
            LinearLayoutManager(this)

        deviceList =
            mutableListOf()

        deviceAdapter =
            DeviceAdapter(
                deviceList
            )

        recycler.adapter =
            deviceAdapter

        val roomId =
            intent.getStringExtra(
                "ROOM_ID"
            ) ?: ""

        loadDevicesFromFirebase(roomId)
    }

    private var roomDevicesListener: ListenerRegistration? = null

    private fun loadDevicesFromFirebase(
        roomId: String
    ) {

        if (roomId.isEmpty()) {
            Log.e(
                "FIREBASE",
                "ROOM_ID is empty"
            )
            return
        }

        roomDevicesListener?.remove()

        roomDevicesListener = FirebaseRepository()
            .listenToDevices(
                roomId,
                onChanged = { firebaseDevices ->

                    runOnUiThread {

                        deviceList.clear()

                        for (firebaseDevice in firebaseDevices) {

                            val device =
                                convertFirebaseDevice(
                                    firebaseDevice
                                )

                            deviceList.add(device)
                        }

                        deviceAdapter.notifyDataSetChanged()

                        Log.d(
                            "FIREBASE",
                            "Real-time devices updated: ${deviceList.size}"
                        )
                    }
                },
                onError = { exception ->
                    Log.e(
                        "FIREBASE",
                        "Failed to listen to room devices: ${exception.message}",
                        exception
                    )
                }
            )
    }

    private fun convertFirebaseDevice(
        firebaseDevice:
        com.example.smarthome.firebase.FirebaseDevice
    ): Device {

        return Device(

            id = firebaseDevice.id,

            name = firebaseDevice.name,

            type = firebaseDevice.type,

            // Keep the two representations of the ON/OFF state
            // consistent. The status field is what is displayed to
            // the user, so use it as the source of truth if the two
            // Firebase fields ever become temporarily out of sync.
            status = firebaseDevice.status,

            isOn = if (
                firebaseDevice.status.equals("ON", ignoreCase = true)
            ) {
                true
            } else if (
                firebaseDevice.status.equals("OFF", ignoreCase = true)
            ) {
                false
            } else {
                firebaseDevice.isOn
            },

            timer = firebaseDevice.timer,

            maxTime = firebaseDevice.maxTime,

            temperature = firebaseDevice.temperature,

            brightness = firebaseDevice.brightness,

            recording = firebaseDevice.recording,

            motionDetection =
                firebaseDevice.motionDetection,

            nightVision =
                firebaseDevice.nightVision,

            fps = firebaseDevice.fps,

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

    override fun onStart() {

        super.onStart()

        DeviceStateNotifier.registerListener(
            deviceChangeListener
        )
    }

    override fun onStop() {

        DeviceStateNotifier.unregisterListener(
            deviceChangeListener
        )

        super.onStop()
    }

    override fun onResume() {

        super.onResume()

        // Re-load the current device states whenever we return
        // from the detail screen. This keeps the switch position,
        // status text and Firebase state synchronized.
        if (::deviceAdapter.isInitialized) {

            val roomId =
                intent.getStringExtra("ROOM_ID") ?: ""

            if (roomId.isNotEmpty()) {
                loadDevicesFromFirebase(roomId)
            } else {
                deviceAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onDestroy() {
        roomDevicesListener?.remove()
        roomDevicesListener = null
        super.onDestroy()
    }
}