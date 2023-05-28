package com.emircankirez.mobileproject.model

import android.os.Parcelable
import com.google.firebase.Timestamp
import kotlinx.parcelize.Parcelize
import java.sql.Time
import java.util.Date

@Parcelize
data class User(
    val uid : String,
    val name : String,
    val surname : String,
    val email : String,
    val photoUrl : String,
    val department : String = "",
    val grade : Int = 1,
    val state : String = "Aramıyor",
    val distance : Double = 0.0,
    val timeToBeAtHome : Int = 0,
    val phoneNumber : String = "",
    val lastLogin : Long  = 0
    ) : Parcelable {
    constructor() : this("", "", "", "", "")

}