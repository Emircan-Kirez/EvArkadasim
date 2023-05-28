package com.emircankirez.mobileproject.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.emircankirez.mobileproject.activities.UserDetailsActivity
import com.emircankirez.mobileproject.databinding.RequestItemBinding
import com.emircankirez.mobileproject.model.Request
import com.emircankirez.mobileproject.model.User
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat

class RequestAdapter (private val requestList : ArrayList<Request>) : RecyclerView.Adapter<RequestAdapter.RequestHolder>() {
    private var fDatabase = Firebase.firestore

    class RequestHolder (val binding : RequestItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestHolder {
        val binding = RequestItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RequestHolder(binding)
    }

    override fun getItemCount(): Int {
        return requestList.size
    }

    override fun onBindViewHolder(holder: RequestHolder, position: Int) {
        holder.binding.apply {
            val request = requestList[position]

            fDatabase.collection("users").document(request.from)
                .get()
                .addOnSuccessListener { snapshot ->
                    if(snapshot.exists()){
                        val user = snapshot.toObject(User::class.java)!!
                        txtRequestInfo.text = "${user.name} ${user.surname} adlı kişiden bir eşleşme talebi alındı."
                        txtDate.text = SimpleDateFormat("dd/MM/yyyy").format(request.date.toDate())
                        txtTime.text = SimpleDateFormat("HH:mm").format(request.date.toDate())

                        btnDetails.setOnClickListener {
                            val intent = Intent(holder.itemView.context, UserDetailsActivity::class.java)
                            intent.putExtra("request", request)
                            intent.putExtra("user", user)
                            holder.itemView.context.startActivity(intent)
                        }
                    }else{
                        txtRequestInfo.text = "Kullanıcını bulunamadı."
                        btnDetails.visibility = View.INVISIBLE
                    }
                }
                .addOnFailureListener {
                    txtRequestInfo.text = "Kullanıcı verisi alınamadı."
                    Toast.makeText(holder.itemView.context, "Kullanıcı bilgileri alınırken hata meydana geldi.", Toast.LENGTH_SHORT).show()
                }

        }
    }
}
