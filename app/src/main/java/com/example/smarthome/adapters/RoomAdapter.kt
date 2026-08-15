package com.example.smarthome.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smarthome.R
import com.example.smarthome.activities.RoomActivity
import com.example.smarthome.models.Room

class RoomAdapter(
    private val rooms: MutableList<Room>
) : RecyclerView.Adapter<RoomAdapter.RoomViewHolder>() {

    inner class RoomViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        val imgRoomIcon: ImageView =
            itemView.findViewById(
                R.id.imgRoomIcon
            )

        val txtRoomName: TextView =
            itemView.findViewById(
                R.id.txtRoomName
            )

        val txtDeviceCount: TextView =
            itemView.findViewById(
                R.id.txtDeviceCount
            )
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RoomViewHolder {

        val view =
            LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.item_room,
                    parent,
                    false
                )

        return RoomViewHolder(view)
    }


    override fun onBindViewHolder(
        holder: RoomViewHolder,
        position: Int
    ) {

        val room =
            rooms[position]


        // ==========================================
        // ROOM NAME
        // ==========================================

        holder.txtRoomName.text =
            room.roomName
                .trim()
                .ifBlank {
                    "Room"
                }


        // ==========================================
        // ROOM MATERIAL ICON
        // ==========================================

        holder.imgRoomIcon.setImageResource(
            getRoomIcon(
                room.roomName
            )
        )


        // ==========================================
        // DEVICE COUNT
        // ==========================================

        val count =
            room.devices.size

        holder.txtDeviceCount.text =
            if (count == 1) {
                "1 Device"
            } else {
                "$count Devices"
            }


        // ==========================================
        // OPEN ROOM
        // ==========================================

        holder.itemView.setOnClickListener {

            val intent =
                Intent(
                    holder.itemView.context,
                    RoomActivity::class.java
                )

            intent.putExtra(
                "ROOM_ID",
                room.firebaseId
            )

            intent.putExtra(
                "ROOM_NAME",
                room.roomName
            )

            holder.itemView.context.startActivity(
                intent
            )
        }
    }


    override fun getItemCount(): Int =
        rooms.size


    // ==========================================
    // MATERIAL ROOM ICON
    // ==========================================

    private fun getRoomIcon(
        roomName: String
    ): Int {

        val name =
            roomName.trim()


        return when {

            name.contains(
                "living",
                ignoreCase = true
            ) ||
                    name.contains(
                        "lounge",
                        ignoreCase = true
                    ) -> {

                R.drawable.ic_room_living
            }


            name.contains(
                "bedroom",
                ignoreCase = true
            ) ||
                    name.contains(
                        "bed room",
                        ignoreCase = true
                    ) ||
                    name.contains(
                        "bed",
                        ignoreCase = true
                    ) -> {

                R.drawable.ic_room_bed
            }


            name.contains(
                "kitchen",
                ignoreCase = true
            ) -> {

                R.drawable.ic_room_kitchen
            }


            name.contains(
                "bath",
                ignoreCase = true
            ) ||
                    name.contains(
                        "toilet",
                        ignoreCase = true
                    ) -> {

                R.drawable.ic_room_bathroom
            }


            // Garage
            name.contains(
                "garage",
                ignoreCase = true
            ) -> {

                R.drawable.ic_home_modern
            }


            // Office / Study
            name.contains(
                "office",
                ignoreCase = true
            ) ||
                    name.contains(
                        "study",
                        ignoreCase = true
                    ) -> {

                R.drawable.ic_floor_modern
            }


            // Dining
            name.contains(
                "dining",
                ignoreCase = true
            ) -> {

                R.drawable.ic_room_kitchen
            }


            // Garden / Balcony
            name.contains(
                "garden",
                ignoreCase = true
            ) ||
                    name.contains(
                        "yard",
                        ignoreCase = true
                    ) ||
                    name.contains(
                        "balcony",
                        ignoreCase = true
                    ) -> {

                R.drawable.ic_home_modern
            }


            // Hall
            name.contains(
                "hall",
                ignoreCase = true
            ) -> {

                R.drawable.ic_home_modern
            }


            // Default
            else -> {

                R.drawable.ic_home_modern
            }
        }
    }
}