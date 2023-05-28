package com.emircankirez.mobileproject.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import com.emircankirez.mobileproject.R
import com.emircankirez.mobileproject.databinding.ActivityMainBinding
import com.emircankirez.mobileproject.login.SignInActivity
import com.emircankirez.mobileproject.model.Request
import com.emircankirez.mobileproject.model.User
import com.emircankirez.mobileproject.notification.ConfirmedNotificationService
import com.emircankirez.mobileproject.notification.NewRequestNotificationService
import com.emircankirez.mobileproject.notification.RejectedNotificationService
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Date

class MainActivity : AppCompatActivity(), LocationListener {
    private lateinit var binding : ActivityMainBinding
    private lateinit var fAuth : FirebaseAuth
    private lateinit var fDatabase : FirebaseFirestore
    private lateinit var sharedPreferences : SharedPreferences
    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // firebase init
        fAuth = Firebase.auth
        fDatabase = Firebase.firestore

        sharedPreferences = getSharedPreferences("userInfo", Context.MODE_PRIVATE)

        // uygulamayı kullanan kullanıcının bilgilerini dinle
        fDatabase.collection("users").document(fAuth.currentUser?.uid!!)
            .addSnapshotListener { value, error ->
                if(error != null){
                    Toast.makeText(this, error.localizedMessage, Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if((value != null) && value.exists()){
                    val user = value.toObject(User::class.java)!!
                    saveUserInLocal(user)
                }
            }

        createNotificationChannel()

        // bana gelen ve bekleniyor durumunda olan istekleri dinle
        fDatabase.collection("requests")
            .whereEqualTo("to", fAuth.currentUser?.uid!!)
            .whereEqualTo("state", "Bekleniyor")
            .whereGreaterThanOrEqualTo("date", Date(sharedPreferences.getLong("lastLogin", 0))) // son giriş tarihinden itibaren yeni bir istek var mı?
            .addSnapshotListener { value, error ->
                if(error != null){
                    Toast.makeText(this, error.localizedMessage, Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }

                if((value != null) && !value.isEmpty){
                    for(dc in value.documentChanges){
                        when (dc.type) {
                            DocumentChange.Type.ADDED -> {
                                // yeni bir bekleniyor durumunda istek geldiyse notification oluştur
                                val request = dc.document.toObject(Request::class.java)
                                fDatabase.collection("users").document(request.from)
                                    .get()
                                    .addOnSuccessListener { snapshot ->
                                        if(snapshot.exists()){
                                            val user = snapshot.toObject(User::class.java)!! // istek atan kişinin bilgileri
                                            val newRequestNotificationService = NewRequestNotificationService(applicationContext)
                                            newRequestNotificationService.showNotification(user.name, user.surname)
                                        }
                                    }
                            }

                            else -> {}
                        }
                    }
                }
            }


        // benim gönderdiğim istekleri dinle -> iki durum değişimi: bekleniyor -> onaylandı / reddedildi.
        fDatabase.collection("requests")
            .whereEqualTo("from", fAuth.currentUser?.uid!!)
            .addSnapshotListener { value, error ->
                if(error != null){
                    Toast.makeText(this, error.localizedMessage, Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if((value != null) && !value.isEmpty){
                    for(dc in value.documentChanges){
                        when (dc.type) {
                            DocumentChange.Type.MODIFIED -> {
                                // bekleniyor durumunda olan istek onaylandıysa ya da reddedildiyse notification oluştur
                                val request = dc.document.toObject(Request::class.java)
                                fDatabase.collection("users").document(request.to)
                                    .get()
                                    .addOnSuccessListener { snapshot ->
                                        if(snapshot.exists()){
                                            val user = snapshot.toObject(User::class.java)!! // istek atılan kişinin bilgileri
                                            if(request.state == "Onaylandı"){
                                                val confirmedNotificationService = ConfirmedNotificationService(applicationContext)
                                                confirmedNotificationService.showNotification(user.name, user.surname)
                                            }else if(request.state == "Reddedildi"){
                                                val rejectedNotificationService = RejectedNotificationService(applicationContext)
                                                rejectedNotificationService.showNotification(user.name, user.surname)
                                            }
                                        }
                                    }
                            }

                            else -> {}
                        }
                    }
                }
            }


        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHost) as NavHostFragment
        val navController = navHostFragment.navController
        //binding.bottomNavView.setupWithNavController(navController)

        binding.bottomNavView.selectedItemId = R.id.home

        binding.apply {
            bottomNavView.setOnItemSelectedListener {
                when (it.itemId) {
                    R.id.profile -> {
                        navController.navigate(R.id.action_global_profileFragment)
                        true
                    }

                    R.id.home -> {
                        navController.navigate(R.id.action_global_homeFragment)
                        true
                    }

                    R.id.map -> {
                        navController.navigate(R.id.action_global_mapFragment)
                        true
                    }

                    else -> false
                }
            }
        }

        registerLauncher()
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)){
                Snackbar.make(binding.root, "Konum için izin gerekli", Snackbar.LENGTH_INDEFINITE).setAction("İzin ver"){ result ->
                    // request permission
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }.show()
            }else{
                // request permission
                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }else{
            val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,1 * 60 * 1000,0f, this) // her 1dkda bir konumumu sakla
        }
    }

    @SuppressLint("MissingPermission")
    private fun registerLauncher() {
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){ result ->
            if(result){
                Toast.makeText(this, "Konum izni verildi", Toast.LENGTH_SHORT).show()
                val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,1 * 60 * 1000,0f, this) // her 1dkda bir konumumu sakla
            }else{
                Toast.makeText(this, "Konum izni verilmedi", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createNotificationChannel() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            // New Request Notification Channel
            val newRequestchannel = NotificationChannel(
                NewRequestNotificationService.CHANNEL_ID,
                "New Request",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            newRequestchannel.description = "Used for new coming request"
            notificationManager.createNotificationChannel(newRequestchannel)

            // Confirmed Request Notification Channel
            val confirmedRequestchannel = NotificationChannel(
                ConfirmedNotificationService.CHANNEL_ID,
                "Confirmed Request",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            confirmedRequestchannel.description = "Used for new coming request"
            notificationManager.createNotificationChannel(confirmedRequestchannel)

            // Rejected Request Notification Channel
            val rejectedRequestchannel = NotificationChannel(
                RejectedNotificationService.CHANNEL_ID,
                "New Request",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            rejectedRequestchannel.description = "Used for new coming request"
            notificationManager.createNotificationChannel(rejectedRequestchannel)
        }
    }

    private fun saveUserInLocal(user : User){
        sharedPreferences.edit()
            .putString("name", user.name)
            .putString("surname", user.surname)
            .putString("emailAddress", user.email)
            .putString("photoUrl", user.photoUrl)
            .putString("department", user.department)
            .putString("grade", user.grade.toString())
            .putString("state", user.state)
            .putString("distance", user.distance.toString())
            .putString("timeToBeAtHome", user.timeToBeAtHome.toString())
            .putString("phoneNumber", user.phoneNumber)
            .putLong("lastLogin", user.lastLogin)
            .apply()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.top_nav, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.sign_out -> {
                // çıkış yapmadan önce kaydet
                val lastLoginUpdate = hashMapOf<String, Any>(
                    "lastLogin" to Timestamp.now().seconds * 1000L
                )
                fDatabase.collection("users").document(fAuth.currentUser?.uid!!)
                    .update(lastLoginUpdate)

                //sign out
                fAuth.signOut()
                val intent = Intent(this@MainActivity, SignInActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
                finish()
            }
            R.id.myRequests -> {
                // intent to myRequest activity
                val intent = Intent(this@MainActivity, MyRequestsActivity::class.java)
                startActivity(intent)
            }
            R.id.requests -> {
                // intent to request activity
                val intent = Intent(this@MainActivity, RequestsActivity::class.java)
                startActivity(intent)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onStop() {
        super.onStop()

        val lastLoginUpdate = hashMapOf<String, Any>(
            "lastLogin" to Timestamp.now().seconds * 1000L
        )

        if(fAuth.currentUser != null){
            fDatabase.collection("users").document(fAuth.currentUser?.uid!!)
                .update(lastLoginUpdate)
        }

    }

    override fun onLocationChanged(location: Location) {
        val locationUpdates = hashMapOf<String, Any>(
            "lastLat" to location.latitude,
            "lastLng" to location.longitude,
            "lastLocTime" to Timestamp.now()
        )

        if(fAuth.currentUser != null){
            fDatabase.collection("locations").document(fAuth.currentUser?.uid!!)
                .update(locationUpdates)
        }

    }

    override fun onProviderEnabled(provider: String) {}

    override fun onProviderDisabled(provider: String) {}

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

}