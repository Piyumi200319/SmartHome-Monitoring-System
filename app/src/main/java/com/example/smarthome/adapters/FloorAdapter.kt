package com.example.smarthome.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smarthome.R
import com.example.smarthome.activities.FloorActivity
import com.example.smarthome.models.Floor

class FloorAdapter(
    private val floorList: List<Floor>
) : RecyclerView.Adapter<FloorAdapter.FloorViewHolder>() {

    class FloorViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        val imgFloorIcon: ImageView =
            itemView.findViewById(
                R.id.imgFloorIcon
            )

        val txtFloorName: TextView =
            itemView.findViewById(
                R.id.txtFloorName
            )

        val txtRoomCount: TextView =
            itemView.findViewById(
                R.id.txtRoomCount
            )
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): FloorViewHolder {

        val view =
            LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.item_floor,
                    parent,
                    false
                )

        return FloorViewHolder(view)
    }


    override fun getItemCount(): Int =
        floorList.size


    override fun onBindViewHolder(
        holder: FloorViewHolder,
        position: Int
    ) {

        val floor =
            floorList[position]


        // ==========================================
        // FLOOR NAME
        // ==========================================

        holder.txtFloorName.text =
            floor.floorName
                .trim()
                .ifBlank {
                    "Floor"
                }


        // ==========================================
        // ROOM COUNT
        // ==========================================

        holder.txtRoomCount.text =
            "${floor.rooms.size} Rooms"


        // ==========================================
        // MATERIAL FLOOR ICON
        // ==========================================

        holder.imgFloorIcon.setImageResource(
            getFloorIcon(
                floor.floorName
            )
        )


        // ==========================================
        // CLICK FLOOR
        // ==========================================

        holder.itemView.setOnClickListener {

            val intent =
                Intent(
                    holder.itemView.context,
                    FloorActivity::class.java
                )


            // IMPORTANT:
            // Send Firebase ID, not only the name.

            intent.putExtra(
                "FLOOR_ID",
                floor.firebaseId
            )


            intent.putExtra(
                "FLOOR_NAME",
                floor.floorName
            )


            holder.itemView.context.startActivity(
                intent
            )
        }
    }


    // ==========================================
    // MATERIAL FLOOR ICON SELECTION
    // ==========================================

    private fun getFloorIcon(
        floorName: String
    ): Int {

        return when {

            floorName.contains(
                "ground",
                ignoreCase = true
            ) -> {

                R.drawable.ic_home_modern
            }


            floorName.contains(
                "first",
                ignoreCase = true
            ) -> {

                R.drawable.ic_floor_modern
            }


            floorName.contains(
                "second",
                ignoreCase = true
            ) -> {

                R.drawable.ic_floor_modern
            }


            floorName.contains(
                "third",
                ignoreCase = true
            ) -> {

                R.drawable.ic_floor_modern
            }


            floorName.contains(
                "basement",
                ignoreCase = true
            ) -> {

                R.drawable.ic_floor_modern
            }


            else -> {

                R.drawable.ic_home_modern
            }
        }
    }
}