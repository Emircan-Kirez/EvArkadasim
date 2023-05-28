package com.emircankirez.mobileproject.model

import com.google.firebase.Timestamp

data class LastLocation(val lastLat : Double, val lastLng : Double, val lastLocTime : Timestamp) {
    constructor() : this(0.0, 0.0, Timestamp.now())
}