package com.example.smarthome.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.smarthome.R
import com.example.smarthome.models.Notification

class NotificationAdapter(
    private val notificationList:
    MutableList<Notification>
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    class NotificationViewHolder(
        view: View
    ) : RecyclerView.ViewHolder(view) {

        val imgIcon: ImageView =
            view.findViewById(R.id.imgNotificationIcon)

        val txtTitle: TextView =
            view.findViewById(R.id.txtTitle)

        val txtMessage: TextView =
            view.findViewById(R.id.txtMessage)

        val txtTime: TextView =
            view.findViewById(R.id.txtTime)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): NotificationViewHolder {

        val view =
            LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.item_notification,
                    parent,
                    false
                )

        return NotificationViewHolder(view)
    }

    override fun getItemCount(): Int =
        notificationList.size

    override fun onBindViewHolder(
        holder: NotificationViewHolder,
        position: Int
    ) {

        val notification =
            notificationList[position]

        holder.imgIcon.setImageResource(
            com.example.smarthome.R.drawable.ic_notifications_modern
        )

        holder.txtTitle.text =
            notification.title

        holder.txtMessage.text =
            notification.message

        holder.txtTime.text =
            notification.time
    }
}