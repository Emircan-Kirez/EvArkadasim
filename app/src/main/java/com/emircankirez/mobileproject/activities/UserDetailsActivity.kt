package com.emircankirez.mobileproject.activities

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.emircankirez.mobileproject.R
import com.emircankirez.mobileproject.databinding.ActivityUserDetailsBinding
import com.emircankirez.mobileproject.model.Request
import com.emircankirez.mobileproject.model.User
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class UserDetailsActivity : AppCompatActivity() {
    private lateinit var binding : ActivityUserDetailsBinding
    private lateinit var fAuth : FirebaseAuth
    private lateinit var fDatabase : FirebaseFirestore
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // firebase init
        fAuth = Firebase.auth
        fDatabase = Firebase.firestore

        sharedPreferences = getSharedPreferences("userInfo", Context.MODE_PRIVATE)

        val intent = intent
        val request = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("request", Request::class.java)
        } else {
            intent.getParcelableExtra("request") as Request?
        }

        if(request != null) { // request ekranından gelmişizdir.
            binding.apply {
                btnSendRequest.visibility = View.GONE
                btnConfirm.visibility = View.VISIBLE
                btnReject.visibility = View.VISIBLE
            }
        }

        val user = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("user", User::class.java)
        } else {
            intent.getParcelableExtra("user") as User?
        }

        if(user == null){
            Toast.makeText(this@UserDetailsActivity, "Kullanıcı bilgileri alınamadı!!", Toast.LENGTH_SHORT).show()
            finish()
        }else{
            binding.apply {
                createGradeSpinner(user)
                createStateSpinner(user)
                if (user.photoUrl != "")
                    Glide.with(this@UserDetailsActivity).load(user.photoUrl).into(imgUser)
                spinnerGrade.isEnabled = false
                spinnerState.isEnabled = false
                txtName.text = user.name
                txtSurname.text = user.surname
                txtDepartment.text = user.department
                txtDistance.text = user.distance.toString()
                txtTimeToBeAtHome.text = user.timeToBeAtHome.toString()
                txtEmail.text = user.email
                txtPhoneNumber.text = user.phoneNumber

                // "Aramıyor" durumunda olan biri talep butonunu görmesin - kendisine istek atamasın
                if(sharedPreferences.getString("state", "") == "Aramıyor" || fAuth.currentUser?.uid == user.uid)
                    btnSendRequest.visibility = View.INVISIBLE

                when (spinnerState.selectedItem.toString()) {
                    "Kalacak ev arıyor" -> {
                        txtDistanceInfo.visibility = View.VISIBLE
                        txtDistance.visibility = View.VISIBLE
                        txtTimeToBeAtHomeInfo.visibility = View.VISIBLE
                        txtTimeToBeAtHome.visibility = View.VISIBLE
                        txtKm.visibility = View.VISIBLE
                        txtDay.visibility = View.VISIBLE
                        txtDistanceInfo.text = "Kampüse istenen ev uzaklığı:"
                    }

                    "Ev arkadaşı arıyor" -> {
                        txtDistanceInfo.visibility = View.VISIBLE
                        txtDistance.visibility = View.VISIBLE
                        txtTimeToBeAtHomeInfo.visibility = View.VISIBLE
                        txtTimeToBeAtHome.visibility = View.VISIBLE
                        txtKm.visibility = View.VISIBLE
                        txtDay.visibility = View.VISIBLE
                        txtDistanceInfo.text = "Kampüse olan ev uzaklığı:"
                    }

                    else -> {
                        txtDistance.text = ""
                        txtDistanceInfo.visibility = View.GONE
                        txtDistance.visibility = View.GONE
                        txtTimeToBeAtHome.text = ""
                        txtTimeToBeAtHomeInfo.visibility = View.GONE
                        txtTimeToBeAtHome.visibility = View.GONE
                        txtKm.visibility = View.GONE
                        txtDay.visibility = View.GONE
                    }
                }

                btnConfirm.setOnClickListener {
                    val requestUpdate = hashMapOf<String, Any>(
                        "state" to "Onaylandı",
                        "date" to Timestamp.now()
                    )

                    // request'in durumunu güncelle
                    fDatabase.collection("requests").document(request!!.documentId).update(requestUpdate)
                        .addOnSuccessListener {
                            Toast.makeText(this@UserDetailsActivity, "Eşleşme talebi onaylandı.", Toast.LENGTH_SHORT).show()

                            // kullanıcıların durumunu güncelle
                            val userUpdate = hashMapOf<String, Any>("state" to "Aramıyor")

                            fDatabase.collection("users").document(request.from).update(userUpdate)
                            fDatabase.collection("users").document(request.to).update(userUpdate)

                            // gönderen ve alıcı tarafın yapmış olduğu ve "Bekleniyor" durumunda olan bütün istekleri sil
                            // gönderen ve alıcı tarafa gelen istekleri reddet

                            finish()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this@UserDetailsActivity, it.localizedMessage, Toast.LENGTH_LONG).show()
                        }
                }

                btnReject.setOnClickListener {
                    val requestUpdate = hashMapOf<String, Any>(
                        "state" to "Reddedildi",
                        "date" to Timestamp.now()
                    )

                    fDatabase.collection("requests").document(request!!.documentId).update(requestUpdate)
                        .addOnSuccessListener {
                            Toast.makeText(this@UserDetailsActivity, "Eşleşme talebi reddedildi.", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this@UserDetailsActivity, it.localizedMessage, Toast.LENGTH_LONG).show()
                        }
                }

                btnEmail.setOnClickListener {
                    val emailIntent = Intent(Intent.ACTION_SEND)
                    emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(user.email))
                    //need this to prompts email client only
                    emailIntent.type = "message/rfc822"
                    if (intent.resolveActivity(packageManager) != null) {
                        startActivity(Intent.createChooser(emailIntent, "Uygulamayı seçiniz:"))
                    }
                }

                btnWhatsapp.setOnClickListener {
                    val phoneNumber = txtPhoneNumber.text.toString().filterNot { it.isWhitespace() }
                    val installed = appInstalledOrNot("com.whatsapp")
                    if (installed){
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.data = Uri.parse("http://api.whatsapp.com/send?phone=$phoneNumber")
                        startActivity(intent)
                    }else {
                        Toast.makeText(this@UserDetailsActivity,"Cihazınızda whatsapp yüklü değil.", Toast.LENGTH_SHORT).show()
                    }
                }

                btnSendRequest.setOnClickListener {
                    val currentUser = fAuth.currentUser!!

                    // "Aramıyor" durumunda olan ya da kullanıcıyla aynı durumda olan eşleşme taleplerini gönderme
                    if(user.state != "Aramıyor" && sharedPreferences.getString("state", "") != user.state){

                        // daha öncesinde ilgili kullanıcıya bir istek göndermiş ve bekleniyor durumundaysa istek göndermesin
                        fDatabase.collection("requests")
                            .whereEqualTo("from", currentUser.uid)
                            .whereEqualTo("to", user.uid)
                            .whereEqualTo("state", "Bekleniyor")
                            .get()
                            .addOnSuccessListener { value ->
                                if(value != null && !value.isEmpty){
                                    Toast.makeText(
                                        this@UserDetailsActivity,
                                        "'Bekleniyor' durumunda olan bir eşleşme talebiniz bulunduğu için yeni bir eşleşme talebi gönderemezsiniz.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }else{ // herhangi bir bekleniyor isteği olmadığı için yeni istek gönderilir
                                    val documentId = fDatabase.collection("requests").document().id // firestore'dan id al
                                    val newRequest = Request(documentId, currentUser.uid, user.uid, Timestamp.now(), "Bekleniyor")
                                    fDatabase.collection("requests").document(documentId)
                                        .set(newRequest)
                                        .addOnSuccessListener {
                                            Toast.makeText(this@UserDetailsActivity, "Talep başarılı bir şekilde gönderildi.", Toast.LENGTH_SHORT).show()
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(this@UserDetailsActivity, it.localizedMessage, Toast.LENGTH_LONG).show()
                                        }
                                }
                            }
                            .addOnFailureListener {
                                Toast.makeText(this@UserDetailsActivity, it.localizedMessage, Toast.LENGTH_LONG).show()
                            }
                    }else if(user.state == "Aramıyor"){
                        Toast.makeText(this@UserDetailsActivity, "'Aramıyor' durumunda olan birine eşleşme talebi gönderemezsiniz.", Toast.LENGTH_LONG).show()
                    }else{
                        Toast.makeText(this@UserDetailsActivity, "Sizinle aynı durumda (${user.state}) olan birine eşleşme talebi gönderemezsiniz.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun appInstalledOrNot(url: String): Boolean {
        val packageManager = packageManager
        val appInstalled: Boolean = try {
            packageManager.getPackageInfo(url, PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
        return appInstalled
    }

    private fun createGradeSpinner(user : User){
        ArrayAdapter.createFromResource(
            this@UserDetailsActivity,
            R.array.grades,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerGrade.adapter = adapter
            val position = adapter.getPosition(user.grade.toString())
            binding.spinnerGrade.setSelection(position)
        }
    }

    private fun createStateSpinner(user : User){
        ArrayAdapter.createFromResource(
            this@UserDetailsActivity,
            R.array.states,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerState.adapter = adapter
            val position = adapter.getPosition(user.state)
            binding.spinnerState.setSelection(position)
        }
    }
}