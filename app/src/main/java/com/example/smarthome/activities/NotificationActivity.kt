package com.example.smarthome.activities

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smarthome.R
import com.example.smarthome.adapters.NotificationAdapter
import com.example.smarthome.service.NotificationService

class NotificationActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: NotificationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_notifications
        )

        // --------------------------------------------------
        // RECYCLER VIEW
        // --------------------------------------------------

        recycler =
            findViewById(
                R.id.rvNotifications
            )

        recycler.layoutManager =
            LinearLayoutManager(this)

        adapter =
            NotificationAdapter(
                NotificationService.getNotifications()
            )

        recycler.adapter =
            adapter

        // --------------------------------------------------
        // CLEAR ALL
        // --------------------------------------------------

        val btnClear =
            findViewById<Button>(
                R.id.btnClearNotifications
            )

        btnClear.setOnClickListener {

            NotificationService.clearNotifications()

            adapter.notifyDataSetChanged()
        }
    }

    // --------------------------------------------------
    // REFRESH WHEN RETURNING TO SCREEN
    // --------------------------------------------------

    override fun onResume() {

        super.onResume()

        if (::adapter.isInitialized) {

            adapter.notifyDataSetChanged()
        }
    }
}