package com.example.smarthome.data

import com.example.smarthome.models.Device
import com.example.smarthome.models.Floor
import com.example.smarthome.models.Room
import com.example.smarthome.utils.Constants

object SampleData {

    // Create the data only ONCE.
    // The same Device objects are reused throughout the app.
    private val floorData: MutableList<Floor> by lazy {

        // ---------------- KITCHEN ----------------

        val kitchen = Room(
            roomName = "Kitchen",
            devices = mutableListOf(

                Device(
                    id = "1",
                    name = "Wall Outlet",
                    type = "Outlet",
                    status = Constants.STATUS_ON,
                    isOn = true,
                    power = 250,
                    voltage = 230,
                    current = 1.1,
                    energyToday = 0.30
                ),

                Device(
                    id = "2",
                    name = "Multi Switch",
                    type = "Switch",
                    status = Constants.STATUS_ON,
                    isOn = true,
                    switch1 = true,
                    switch2 = false,
                    switch3 = true,
                    power = 3,
                    voltage = 230,
                    current = 0.01,
                    energyToday = 0.01
                ),

                Device(
                    id = "3",
                    name = "Iron",
                    type = "Iron",
                    status = Constants.STATUS_OFF,
                    isOn = false,
                    timer = 120,
                    maxTime = 120,
                    temperature = 120,
                    targetTemperature = 120,
                    heating = false,
                    safetyMode = "SAFE",
                    power = 1200,
                    voltage = 230,
                    current = 5.2,
                    energyToday = 0.82
                )
            )
        )

        // ---------------- LIVING ROOM ----------------

        val livingRoom = Room(
            roomName = "Living Room",
            devices = mutableListOf(

                Device(
                    id = "4",
                    name = "Ceiling Light",
                    type = "Light",
                    status = Constants.STATUS_ON,
                    brightness = 80,
                    isOn = true,
                    power = 15,
                    voltage = 230,
                    current = 0.07,
                    energyToday = 0.05
                ),

                Device(
                    id = "5",
                    name = "Security Camera",
                    type = "Camera",
                    status = Constants.STATUS_ON,
                    isOn = true,
                    recording = false,
                    motionDetection = true,
                    nightVision = false,
                    fps = 30,
                    resolution = "1080P",
                    power = 12,
                    voltage = 12,
                    current = 1.0,
                    energyToday = 0.10
                )
            )
        )

        // ---------------- GARAGE ----------------

        val garage = Room(
            roomName = "Garage",
            devices = mutableListOf(

                Device(
                    id = "6",
                    name = "Garage Outlet",
                    type = "Outlet",
                    status = Constants.STATUS_OFF,
                    isOn = false,
                    power = 250,
                    voltage = 230,
                    current = 1.1,
                    energyToday = 0.30
                ),

                Device(
                    id = "7",
                    name = "Garage Light",
                    type = "Light",
                    status = Constants.STATUS_ON,
                    brightness = 80,
                    isOn = true,
                    power = 15,
                    voltage = 230,
                    current = 0.07,
                    energyToday = 0.05
                )
            )
        )

        // ---------------- GROUND FLOOR ----------------

        val groundFloor = Floor(
            floorName = "Ground Floor",
            rooms = mutableListOf(
                kitchen,
                livingRoom,
                garage
            )
        )

        // ---------------- BEDROOM ----------------

        val bedroom = Room(
            roomName = "Bedroom",
            devices = mutableListOf(

                Device(
                    id = "8",
                    name = "Bedside Lamp",
                    type = "Light",
                    status = Constants.STATUS_OFF,
                    brightness = 80,
                    isOn = false,
                    power = 15,
                    voltage = 230,
                    current = 0.07,
                    energyToday = 0.02
                )
            )
        )

        // ---------------- STUDY ----------------

        val study = Room(
            roomName = "Study",
            devices = mutableListOf(

                Device(
                    id = "9",
                    name = "Study Camera",
                    type = "Camera",
                    status = Constants.STATUS_ON,
                    isOn = true,
                    recording = false,
                    motionDetection = true,
                    nightVision = false,
                    fps = 30,
                    resolution = "1080P",
                    power = 12,
                    voltage = 12,
                    current = 1.0,
                    energyToday = 0.12
                )
            )
        )

        // ---------------- FIRST FLOOR ----------------

        val firstFloor = Floor(
            floorName = "First Floor",
            rooms = mutableListOf(
                bedroom,
                study
            )
        )

        // Complete data set
        mutableListOf(
            groundFloor,
            firstFloor
        )
    }

    // Always return the SAME list and SAME Device objects.
    fun getFloors(): MutableList<Floor> {
        return floorData
    }
}