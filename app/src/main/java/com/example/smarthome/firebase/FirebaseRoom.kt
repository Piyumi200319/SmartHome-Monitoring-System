package com.example.smarthome.firebase

data class FirebaseRoom(

    var id: String = "",

    // Current Firestore field
    var roomName: String = "",

    // Optional icon/emoji stored in Firestore
    var roomIcon: String = "",

    // Other common field names used by older seed data
    var icon: String = "",
    var emoji: String = "",

    // Backward-compatible field if an older document uses "name"
    var name: String = "",

    var floorId: String = ""
)
