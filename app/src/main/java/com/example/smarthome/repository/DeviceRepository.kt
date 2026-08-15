package com.example.smarthome.repository

import com.example.smarthome.data.SampleData
import com.example.smarthome.models.Device

class DeviceRepository {

    // ==================================================
    // GET DEVICES BY ROOM
    // ==================================================

    fun getDevices(roomName: String): MutableList<Device> {

        val floors = SampleData.getFloors()

        for (floor in floors) {

            for (room in floor.rooms) {

                if (room.roomName == roomName) {

                    return room.devices
                }
            }
        }

        return mutableListOf()
    }

    // ==================================================
    // GET DEVICE BY ID
    // ==================================================

    fun getDeviceById(deviceId: String): Device? {

        val floors = SampleData.getFloors()

        for (floor in floors) {

            for (room in floor.rooms) {

                for (device in room.devices) {

                    if (device.id == deviceId) {
                        return device
                    }
                }
            }
        }

        return null
    }

    // ==================================================
    // GET ALL DEVICES
    // ==================================================

    fun getAllDevices(): MutableList<Device> {

        val allDevices = mutableListOf<Device>()

        val floors = SampleData.getFloors()

        for (floor in floors) {

            for (room in floor.rooms) {

                allDevices.addAll(
                    room.devices
                )
            }
        }

        return allDevices
    }
}