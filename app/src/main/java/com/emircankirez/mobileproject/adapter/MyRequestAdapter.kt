package com.emircankirez.mobileproject.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.emircankirez.mobileproject.R
import com.emircankirez.mobileproject.databinding.MyRequestItemBinding
import com.emircankirez.mobileproject.model.Request
import com.emircankirez.mobileproject.model.User
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat

class MyRequestAdapter (private val myRequestList : ArrayList<Request>) : RecyclerView.Adapter<MyRequestAdapter.MyRequestHolder>() {
    private var fDatabase = Firebase.firestore
    class MyRequestHolder(val binding : MyRequestItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyRequestHolder {
        val binding = MyRequestItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyRequestHolder(binding)
    }

    override fun getItemCount(): Int {
        return myRequestList.size
    }

    override fun onBindViewHolder(holder: MyRequestHolder, position: Int) {
        holder.binding.apply {
            val request = myRequestList[position]

            fDatabase.collection("users").document(request.to)
                .get()
                .addOnSuccessListener { snapshot ->
                    if(snapshot.exists()){
                        val user = snapshot.toObject(User::class.java)!!

                        when(request.state){
                            "Bekleniyor" -> imgState.setImageResource(R.drawable.baseline_question_mark_24)
                            "Onaylandı" -> imgState.setImageResource(R.drawable.baseline_done_outline_24)
                            "Reddedildi" -> imgState.setImageResource(R.drawable.baseline_do_not_disturb_alt_24)
                        }
                        txtMyRequestInfo.text = "${user.name} ${user.surname} adlı kişiye gönderilen eşleşme talebinin durumu: '${request.state}'"
                        txtDate.text = SimpleDateFormat("dd/MM/yyyy").format(request.date.toDate())
                        txtTime.text = SimpleDateFormat("HH:mm").format(request.date.toDate())
                    }
                }
                .addOnFailureListener {
                    txtMyRequestInfo.text = "Kullanıcı verisi alınamadı."
                    Toast.makeText(holder.itemView.context, "Kullanıcı bilgileri alınırken hata meydana geldi.", Toast.LENGTH_LONG).show()
                }
        }
    }
}