package com.example.smarthome.activities

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.smarthome.R
import com.example.smarthome.firebase.FirebaseDevice
import com.example.smarthome.firebase.FirebaseRepository
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.ListenerRegistration
import java.util.Locale

class ReportActivity : AppCompatActivity() {

    private val firebaseRepository = FirebaseRepository()
    private var devicesListener: ListenerRegistration? = null
    private var latestDevices: List<FirebaseDevice> = emptyList()

    private lateinit var txtTodayEnergy: TextView
    private lateinit var txtCurrentPower: TextView
    private lateinit var txtTotalEnergy: TextView
    private lateinit var txtAveragePower: TextView

    private lateinit var txtIronEnergy: TextView
    private lateinit var txtLightEnergy: TextView
    private lateinit var txtOutletEnergy: TextView
    private lateinit var txtCameraEnergy: TextView

    private lateinit var txtReportSummary: TextView
    private lateinit var btnGenerateReport: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)

        initializeViews()
        startRealtimeEnergyListener()
    }

    private fun initializeViews() {
        txtTodayEnergy = findViewById(R.id.txtTodayEnergy)
        txtCurrentPower = findViewById(R.id.txtCurrentPower)
        txtTotalEnergy = findViewById(R.id.txtTotalEnergy)
        txtAveragePower = findViewById(R.id.txtAveragePower)

        txtIronEnergy = findViewById(R.id.txtIronEnergy)
        txtLightEnergy = findViewById(R.id.txtLightEnergy)
        txtOutletEnergy = findViewById(R.id.txtOutletEnergy)
        txtCameraEnergy = findViewById(R.id.txtCameraEnergy)

        txtReportSummary = findViewById(R.id.txtReportSummary)
        btnGenerateReport = findViewById(R.id.btnGenerateReport)

        btnGenerateReport.setOnClickListener {
            if (latestDevices.isEmpty()) {
                Toast.makeText(
                    this,
                    "No device data available",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                txtReportSummary.text = buildDetailedReport(latestDevices)
                Toast.makeText(
                    this,
                    "Report generated from live Firebase data",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // ==================================================
    // REAL-TIME FIREBASE ENERGY LISTENER
    // ==================================================

    private fun startRealtimeEnergyListener() {
        devicesListener = firebaseRepository.listenToAllDevices(
            onChanged = { devices ->
                runOnUiThread {
                    latestDevices = devices
                    updateReport(devices)
                }
            },
            onError = {
                runOnUiThread {
                    txtReportSummary.text =
                        "Unable to synchronize energy data. Please check the Firebase connection."
                }
            }
        )
    }

    private fun updateReport(devices: List<FirebaseDevice>) {
        if (devices.isEmpty()) {
            showEmptyReport()
            return
        }

        // Active devices are devices currently turned ON
        val activeDevices = devices.filter {
            it.isOn || it.status.equals("ON", ignoreCase = true)
        }

        // Current power is sum of power from active ON devices
        val currentPower = activeDevices.sumOf { it.power }
        val todayEnergy = devices.sumOf { it.energyToday }

        val averagePower = if (activeDevices.isNotEmpty()) {
            activeDevices.map { it.power }.average()
        } else {
            0.0
        }

        val ironEnergy = calculateEnergy(devices, "iron")
        val lightEnergy = calculateEnergy(devices, "light")
        val outletEnergy = calculateEnergy(devices, "outlet", "switch")
        val cameraEnergy = calculateEnergy(devices, "camera")

        txtTodayEnergy.text = String.format(
            Locale.US,
            "%.2f kWh",
            todayEnergy
        )

        txtCurrentPower.text = String.format(
            Locale.US,
            "%d W",
            currentPower
        )

        txtTotalEnergy.text = String.format(
            Locale.US,
            "%.2f kWh",
            todayEnergy
        )

        txtAveragePower.text = String.format(
            Locale.US,
            "%.0f W",
            averagePower
        )

        txtIronEnergy.text = String.format(
            Locale.US,
            "%.2f kWh",
            ironEnergy
        )

        txtLightEnergy.text = String.format(
            Locale.US,
            "%.2f kWh",
            lightEnergy
        )

        txtOutletEnergy.text = String.format(
            Locale.US,
            "%.2f kWh",
            outletEnergy
        )

        txtCameraEnergy.text = String.format(
            Locale.US,
            "%.2f kWh",
            cameraEnergy
        )

        txtReportSummary.text = buildSummary(
            devices,
            todayEnergy,
            currentPower,
            averagePower
        )
    }

    private fun calculateEnergy(
        devices: List<FirebaseDevice>,
        vararg types: String
    ): Double {
        return devices
            .filter { device ->
                types.any { t -> device.type.contains(t, ignoreCase = true) || device.name.contains(t, ignoreCase = true) }
            }
            .sumOf { it.energyToday }
    }

    private fun buildSummary(
        devices: List<FirebaseDevice>,
        todayEnergy: Double,
        currentPower: Int,
        averagePower: Double
    ): String {
        val totalDevices = devices.size
        val devicesOn = devices.count {
            it.isOn || it.status.equals("ON", ignoreCase = true)
        }
        val devicesOff = totalDevices - devicesOn

        return String.format(
            Locale.US,
            "Your SmartHome currently has %d devices. %d devices are ON and %d devices are OFF.\n\n" +
                    "Current power usage is %d W. Today's synchronized energy consumption is %.2f kWh. " +
                    "The average power among active devices is %.0f W.",
            totalDevices,
            devicesOn,
            devicesOff,
            currentPower,
            todayEnergy,
            averagePower
        )
    }

    private fun buildDetailedReport(
        devices: List<FirebaseDevice>
    ): String {
        val activeDevices = devices.filter {
            it.isOn || it.status.equals("ON", ignoreCase = true)
        }
        val todayEnergy = devices.sumOf { it.energyToday }
        val currentPower = activeDevices.sumOf { it.power }
        val averagePower = if (activeDevices.isNotEmpty()) {
            activeDevices.map { it.power }.average()
        } else {
            0.0
        }

        val devicesOn = activeDevices.size
        val ironEnergy = calculateEnergy(devices, "iron")
        val lightEnergy = calculateEnergy(devices, "light")
        val outletEnergy = calculateEnergy(devices, "outlet", "switch")
        val cameraEnergy = calculateEnergy(devices, "camera")

        return String.format(
            Locale.US,
            "Energy Usage Report\n\n" +
                    "Total Devices : %d\n" +
                    "Devices ON : %d\n" +
                    "Devices OFF : %d\n\n" +
                    "Current Power : %d W\n" +
                    "Average Power : %.0f W\n" +
                    "Today's Energy : %.2f kWh\n\n" +
                    "Device Consumption\n" +
                    "Iron : %.2f kWh\n" +
                    "Lights : %.2f kWh\n" +
                    "Outlets : %.2f kWh\n" +
                    "Cameras : %.2f kWh",
            devices.size,
            devicesOn,
            devices.size - devicesOn,
            currentPower,
            averagePower,
            todayEnergy,
            ironEnergy,
            lightEnergy,
            outletEnergy,
            cameraEnergy
        )
    }

    private fun showEmptyReport() {
        txtTodayEnergy.text = "0.00 kWh"
        txtCurrentPower.text = "0 W"
        txtTotalEnergy.text = "0.00 kWh"
        txtAveragePower.text = "0 W"
        txtIronEnergy.text = "0.00 kWh"
        txtLightEnergy.text = "0.00 kWh"
        txtOutletEnergy.text = "0.00 kWh"
        txtCameraEnergy.text = "0.00 kWh"
        txtReportSummary.text = "No device data is currently available."
    }

    override fun onDestroy() {
        devicesListener?.remove()
        devicesListener = null
        super.onDestroy()
    }
}
