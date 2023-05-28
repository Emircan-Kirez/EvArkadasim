package com.emircankirez.mobileproject.activities

import android.app.NotificationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.emircankirez.mobileproject.R
import com.emircankirez.mobileproject.adapter.MyRequestAdapter
import com.emircankirez.mobileproject.adapter.RequestAdapter
import com.emircankirez.mobileproject.databinding.ActivityMyRequestsBinding
import com.emircankirez.mobileproject.model.Request
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MyRequestsActivity : AppCompatActivity() {
    private lateinit var binding : ActivityMyRequestsBinding
    private lateinit var fDatabase : FirebaseFirestore
    private lateinit var fAuth : FirebaseAuth
    private lateinit var registration: ListenerRegistration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyRequestsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Tıklanmamış diğer bildirimleri sil
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()

        // firebase init
        fDatabase = Firebase.firestore
        fAuth = Firebase.auth

        val currentUser = fAuth.currentUser!!

        binding.recyclerViewMyRequests.layoutManager = LinearLayoutManager(this@MyRequestsActivity)

        // gönderilen requestleri dinle
        registration = fDatabase.collection("requests")
            .whereEqualTo("from", currentUser.uid)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if(error != null){
                    Toast.makeText(this@MyRequestsActivity, error.localizedMessage, Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }

                if(snapshot != null && !snapshot.isEmpty){
                    getMyRequests(snapshot)
                }else{
                    Toast.makeText(this@MyRequestsActivity, "Göndermiş olduğunuz bir eşleşme talebi bulunmamaktadır.", Toast.LENGTH_LONG).show()
                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        registration.remove()
    }

    private fun getMyRequests(snapshot: QuerySnapshot) {
        val requestList = ArrayList<Request>()
        for(ds in snapshot.documents){
            val request = ds.toObject(Request::class.java)!!
            requestList.add(request)
        }

        // adapter oluştur ve ata
        val adapter = MyRequestAdapter(requestList)
        binding.recyclerViewMyRequests.adapter = adapter
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