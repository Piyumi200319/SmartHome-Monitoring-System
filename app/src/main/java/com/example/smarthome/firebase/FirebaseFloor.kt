package com.example.smarthome.firebase

data class FirebaseFloor(

    var id: String = "",

    // Current Firestore field
    var floorName: String = "",

    // Optional icon/emoji stored in Firestore
    var floorIcon: String = "",

    // Other common field names used by older seed data
    var icon: String = "",
    var emoji: String = "",

    // Backward-compatible field if an older document uses "name"
    var name: String = "",

    var order: Int = 0
)
