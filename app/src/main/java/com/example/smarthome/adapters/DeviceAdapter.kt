package com.example.smarthome.adapters

import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smarthome.R
import com.example.smarthome.activities.DeviceDetailActivity
import com.example.smarthome.controller.DeviceController
import com.example.smarthome.controller.IronController
import com.example.smarthome.firebase.FirebaseRepository
import com.example.smarthome.models.Device
import com.example.smarthome.service.DeviceStateNotifier
import com.example.smarthome.service.DeviceStateStorage
import com.google.android.material.materialswitch.MaterialSwitch

class DeviceAdapter(
    private val deviceList: MutableList<Device>
) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    // ==================================================
    // VIEW HOLDER
    // ==================================================

    class DeviceViewHolder(view: View) :
        RecyclerView.ViewHolder(view) {

        val imgDevice: ImageView =
            view.findViewById(R.id.imgDevice)

        val txtName: TextView =
            view.findViewById(R.id.txtDeviceName)

        val txtSubtitle: TextView =
            view.findViewById(R.id.txtSubtitle)

        val txtTimer: TextView =
            view.findViewById(R.id.txtTimer)

        val txtPower: TextView =
            view.findViewById(R.id.txtPower)

        val txtStatus: TextView =
            view.findViewById(R.id.txtStatus)

        val deviceSwitch: MaterialSwitch =
            view.findViewById(R.id.deviceSwitch)
    }

    // ==================================================
    // CONTROLLERS
    // ==================================================

    private val controller =
        DeviceController()

    private val firebaseRepository =
        FirebaseRepository()

    // ==================================================
    // CREATE VIEW HOLDER
    // ==================================================

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DeviceViewHolder {

        val view =
            LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.item_device,
                    parent,
                    false
                )

        return DeviceViewHolder(view)
    }

    // ==================================================
    // ITEM COUNT
    // ==================================================

    override fun getItemCount(): Int =
        deviceList.size

    // ==================================================
    // BIND DEVICE
    // ==================================================

    override fun onBindViewHolder(
        holder: DeviceViewHolder,
        position: Int
    ) {

        val device =
            deviceList[position]

        // ==================================================
        // BASIC INFORMATION
        // ==================================================

        holder.txtName.text =
            device.name

        holder.txtTimer.visibility =
            View.GONE

        holder.txtPower.text =
            "${device.power} W"

        // ==================================================
        // DEVICE TYPE
        // ==================================================

        when (device.type.trim().lowercase()) {

            // --------------------------------------------------
            // OUTLET
            // --------------------------------------------------

            "outlet" -> {

                holder.imgDevice.setImageResource(
                    R.drawable.ic_device_outlet
                )

                holder.txtSubtitle.text =
                    "220V • Single"
            }

            // --------------------------------------------------
            // LIGHT
            // --------------------------------------------------

            "light" -> {

                holder.imgDevice.setImageResource(
                    R.drawable.ic_device_light
                )

                holder.txtSubtitle.text =
                    "Smart Lighting"
            }

            // --------------------------------------------------
            // CAMERA
            // --------------------------------------------------

            "camera" -> {

                holder.imgDevice.setImageResource(
                    R.drawable.ic_device_camera
                )

                holder.txtSubtitle.text =
                    "Mock Stream"
            }

            // --------------------------------------------------
            // IRON
            // --------------------------------------------------

            "iron" -> {

                holder.imgDevice.setImageResource(
                    R.drawable.ic_device_iron
                )

                holder.txtSubtitle.text =
                    "Maximum ${device.maxTime} Minutes"

                holder.txtTimer.visibility =
                    View.VISIBLE

                updateIronTimerText(
                    holder,
                    device
                )
            }

            // --------------------------------------------------
            // SWITCH
            // --------------------------------------------------

            "switch" -> {

                holder.imgDevice.setImageResource(
                    R.drawable.ic_device_switch
                )

                holder.txtSubtitle.text =
                    "3 Gang Switch"
            }
        }

        // ==================================================
        // STATUS
        // ==================================================

        holder.txtStatus.text =
            device.status

        updateStatusColor(
            holder,
            device.status
        )

        // ==================================================
        // VERY IMPORTANT
        //
        // Remove the listener before setting isChecked.
        //
        // Otherwise RecyclerView binding can trigger the
        // listener automatically.
        // ==================================================

        holder.deviceSwitch.setOnCheckedChangeListener(
            null
        )

        // The switch UI must always represent the same state as
        // the device model. This is especially important when the
        // user returns from DeviceDetailActivity, because RecyclerView
        // may reuse an old switch view holder.
        val actualIsOn =
            device.status.equals("ON", ignoreCase = true)

        device.isOn = actualIsOn

        holder.deviceSwitch.isChecked =
            actualIsOn

        holder.deviceSwitch.isEnabled =
            !device.status.equals("ERROR", ignoreCase = true) &&
            !device.status.equals("DISCONNECTED", ignoreCase = true)

        // ==================================================
        // DEVICE SWITCH LISTENER
        // ==================================================

        holder.deviceSwitch.setOnCheckedChangeListener {
                _,
                isChecked ->

            // ==================================================
            // IRON
            // ==================================================

            if (
                device.type.trim()
                    .lowercase() == "iron"
            ) {

                if (isChecked) {

                    IronController.startIron(
                        device,
                        createIronListener(holder)
                    )

                } else {

                    IronController.stopIron(
                        device
                    )
                }

            }

            // ==================================================
            // OTHER DEVICES
            // ==================================================

            else {

                if (isChecked) {

                    controller.turnOn(
                        device
                    )

                } else {

                    controller.turnOff(
                        device
                    )
                }
            }

            // ==================================================
            // UPDATE UI IMMEDIATELY
            // ==================================================

            holder.txtStatus.text =
                device.status

            updateStatusColor(
                holder,
                device.status
            )

            updateIronTimerText(
                holder,
                device
            )

            // ==================================================
            // SAVE LOCAL STATE
            // ==================================================

            DeviceStateStorage.saveDevice(
                device
            )

            // ==================================================
            // UPDATE FIREBASE
            // ==================================================

            updateFirebaseDevice(
                device
            )

            // ==================================================
            // NOTIFY OTHER PARTS OF APPLICATION
            // ==================================================

            DeviceStateNotifier.notifyDeviceChanged(
                device
            )
        }

        // ==================================================
        // OPEN DEVICE DETAILS
        // ==================================================

        holder.itemView.setOnClickListener {

            val intent =
                Intent(
                    holder.itemView.context,
                    DeviceDetailActivity::class.java
                )

            intent.putExtra(
                "DEVICE_ID",
                device.id
            )

            intent.putExtra(
                "DEVICE_STATUS",
                device.status
            )

            intent.putExtra(
                "DEVICE_IS_ON",
                device.isOn
            )

            intent.putExtra(
                "DEVICE_TEMPERATURE",
                device.temperature
            )

            intent.putExtra(
                "DEVICE_TIMER",
                device.timer
            )

            holder.itemView.context.startActivity(
                intent
            )
        }
    }

    // ==================================================
    // FIREBASE UPDATE
    // ==================================================

    private fun updateFirebaseDevice(
        device: Device
    ) {

        firebaseRepository.updateDeviceState(

            device.id,

            mapOf(

                // Main ON/OFF state
                "isOn" to device.isOn,

                // ON / OFF status
                "status" to device.status,

                // Electrical information
                "power" to device.power,

                "current" to device.current,

                "energyToday" to device.energyToday
            ),

            onSuccess = {

                // Firebase successfully updated.
            },

            onError = { exception ->

                exception.printStackTrace()
            }
        )
    }

    // ==================================================
    // IRON LISTENER
    // ==================================================

    private fun createIronListener(
        holder: DeviceViewHolder
    ): IronController.IronListener {

        return object :
            IronController.IronListener {

            override fun onTick(
                device: Device
            ) {

                holder.txtTimer.post {

                    val position =
                        holder.bindingAdapterPosition

                    if (
                        position != RecyclerView.NO_POSITION &&
                        position < deviceList.size &&
                        deviceList[position].id == device.id
                    ) {

                        // ------------------------------------------
                        // Remaining time
                        // ------------------------------------------

                        holder.txtTimer.text =
                            "Remaining : ${device.timer} s"

                        // ------------------------------------------
                        // Status
                        // ------------------------------------------

                        holder.txtStatus.text =
                            device.status

                        updateStatusColor(
                            holder,
                            device.status
                        )

                        // ------------------------------------------
                        // Synchronize switch
                        // ------------------------------------------

                        holder.deviceSwitch
                            .setOnCheckedChangeListener(
                                null
                            )

                        holder.deviceSwitch.isChecked =
                            device.isOn

                        holder.deviceSwitch
                            .setOnCheckedChangeListener {
                                    _,
                                    checked ->

                                if (checked) {

                                    IronController.startIron(
                                        device,
                                        createIronListener(
                                            holder
                                        )
                                    )

                                } else {

                                    IronController.stopIron(
                                        device
                                    )
                                }

                                // Save local state
                                DeviceStateStorage
                                    .saveDevice(device)

                                // Update Firebase
                                updateFirebaseDevice(
                                    device
                                )

                                // Update UI
                                holder.txtStatus.text =
                                    device.status

                                updateStatusColor(
                                    holder,
                                    device.status
                                )

                                updateIronTimerText(
                                    holder,
                                    device
                                )

                                // Notify application
                                DeviceStateNotifier
                                    .notifyDeviceChanged(
                                        device
                                    )
                            }
                    }
                }
            }

            override fun onFinished(
                device: Device
            ) {

                holder.txtTimer.post {

                    val position =
                        holder.bindingAdapterPosition

                    if (
                        position != RecyclerView.NO_POSITION &&
                        position < deviceList.size &&
                        deviceList[position].id == device.id
                    ) {

                        // ------------------------------------------
                        // Make sure the actual Device is OFF
                        // ------------------------------------------

                        device.isOn = false
                        device.status = "OFF"

                        // ------------------------------------------
                        // Remove listener before changing switch
                        // ------------------------------------------

                        holder.deviceSwitch
                            .setOnCheckedChangeListener(
                                null
                            )

                        holder.deviceSwitch.isChecked =
                            false

                        // ------------------------------------------
                        // Update UI
                        // ------------------------------------------

                        holder.txtStatus.text =
                            "OFF"

                        updateStatusColor(
                            holder,
                            "OFF"
                        )

                        holder.txtTimer.text =
                            "Remaining : 0 s"

                        updateIronTimerText(
                            holder,
                            device
                        )

                        // ------------------------------------------
                        // Save local state
                        // ------------------------------------------

                        DeviceStateStorage.saveDevice(
                            device
                        )

                        // ------------------------------------------
                        // Update Firebase
                        // ------------------------------------------

                        updateFirebaseDevice(
                            device
                        )

                        // ------------------------------------------
                        // Notify application
                        // ------------------------------------------

                        DeviceStateNotifier.notifyDeviceChanged(
                            device
                        )
                    }
                }
            }
        }
    }

    // ==================================================
    // IRON TIMER TEXT
    // ==================================================

    private fun updateIronTimerText(
        holder: DeviceViewHolder,
        device: Device
    ) {

        if (
            device.type.trim().lowercase() == "iron" &&
            device.isOn
        ) {

            holder.txtTimer.visibility =
                View.VISIBLE

            holder.txtTimer.text =
                "Remaining : ${device.timer} s"

        } else {

            if (
                device.type.trim().lowercase() == "iron"
            ) {

                holder.txtTimer.visibility =
                    View.VISIBLE

                holder.txtTimer.text =
                    "Remaining : 0 s"

            } else {

                holder.txtTimer.visibility =
                    View.GONE
            }
        }
    }

    // ==================================================
    // STATUS COLOR
    // ==================================================

    private fun updateStatusColor(
        holder: DeviceViewHolder,
        status: String
    ) {

        when (
            status.trim().uppercase()
        ) {

            // --------------------------------------------------
            // ON
            // --------------------------------------------------

            "ON" -> {

                holder.txtStatus.setTextColor(
                    Color.parseColor(
                        "#2E7D32"
                    )
                )
            }

            // --------------------------------------------------
            // OFF
            // --------------------------------------------------

            "OFF" -> {

                holder.txtStatus.setTextColor(
                    Color.GRAY
                )
            }

            // --------------------------------------------------
            // ERROR
            // --------------------------------------------------

            "ERROR" -> {

                holder.txtStatus.setTextColor(
                    Color.RED
                )
            }

            // --------------------------------------------------
            // DISCONNECTED
            // --------------------------------------------------

            "DISCONNECTED" -> {

                holder.txtStatus.setTextColor(
                    Color.parseColor(
                        "#FF9800"
                    )
                )
            }
        }
    }
}