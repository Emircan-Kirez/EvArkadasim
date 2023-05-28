package com.emircankirez.mobileproject.activities

import android.app.NotificationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.emircankirez.mobileproject.R
import com.emircankirez.mobileproject.adapter.RequestAdapter
import com.emircankirez.mobileproject.databinding.ActivityRequestsBinding
import com.emircankirez.mobileproject.model.Request
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class RequestsActivity : AppCompatActivity() {
    private lateinit var binding : ActivityRequestsBinding
    private lateinit var fDatabase : FirebaseFirestore
    private lateinit var fAuth : FirebaseAuth
    private lateinit var registration: ListenerRegistration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRequestsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Tıklanmamış diğer bildirimleri sil
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()

        // firebase init
        fDatabase = Firebase.firestore
        fAuth = Firebase.auth

        val currentUser = fAuth.currentUser!!

        binding.recyclerViewRequests.layoutManager = LinearLayoutManager(this@RequestsActivity)

        // kendisine gelen ve durumu 'Bekleniyor' olan requestleri dinle
        registration = fDatabase.collection("requests")
            .whereEqualTo("to", currentUser.uid)
            .whereEqualTo("state", "Bekleniyor")
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if(error != null){
                    Toast.makeText(this@RequestsActivity, error.localizedMessage, Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }

                if(snapshot != null && !snapshot.isEmpty){
                    getRequests(snapshot)
                }else{
                    val requestList = ArrayList<Request>()
                    val adapter = RequestAdapter(requestList)
                    binding.recyclerViewRequests.adapter = adapter
                    Toast.makeText(this@RequestsActivity, "Bekleyen bir eşleşme talebiniz bulunmamaktadır.", Toast.LENGTH_LONG).show()
                }
            }

    }

    override fun onDestroy() {
        super.onDestroy()
        registration.remove()
    }

    private fun getRequests(snapshot: QuerySnapshot) {
        val requestList = ArrayList<Request>()
        for(ds in snapshot.documents){
            val request = ds.toObject(Request::class.java)!!
            requestList.add(request)
        }

        // adapter oluştur ve ata
        val adapter = RequestAdapter(requestList)
        binding.recyclerViewRequests.adapter = adapter
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.back, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == R.id.btnBack)
            finish()
        return super.onOptionsItemSelected(item)
    }
}