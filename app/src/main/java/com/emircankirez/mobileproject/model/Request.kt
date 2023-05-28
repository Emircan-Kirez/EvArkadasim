package com.emircankirez.mobileproject.model

import com.google.firebase.Timestamp
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize

data class Request (val documentId : String, val from : String, val to : String, val date : Timestamp, val state : String) : Parcelable {
   constructor() : this("", "", "", Timestamp(0,0), "")
}