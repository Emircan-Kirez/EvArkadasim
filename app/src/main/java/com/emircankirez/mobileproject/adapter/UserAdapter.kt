package com.emircankirez.mobileproject.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.emircankirez.mobileproject.activities.UserDetailsActivity
import com.emircankirez.mobileproject.databinding.UserItemBinding
import com.emircankirez.mobileproject.model.User

class UserAdapter (private val userList : ArrayList<User>) : RecyclerView.Adapter<UserAdapter.UserHolder>() {
    class UserHolder (val binding : UserItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserHolder {
        val binding = UserItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserHolder(binding)
    }

    override fun getItemCount(): Int {
        return userList.size
    }

    override fun onBindViewHolder(holder: UserHolder, position: Int) {
        val user = userList[position]

        holder.binding.apply {
            if(user.photoUrl != "")
                Glide.with(holder.itemView).load(user.photoUrl).into(imgUser)
            txtNameSurname.text = "${user.name} ${user.surname}"
            txtState.text = "Durum: ${user.state}"

            if(user.state == "Aramıyor"){
                txtDistance.visibility = View.GONE
                txtTimeToBeAtHome.visibility = View.GONE
            }else{
                txtDistance.text = "Kampüse uzaklık: ${user.distance}"
                txtTimeToBeAtHome.text = "Evde kalma süresi: ${user.timeToBeAtHome}"
                txtDistance.visibility = View.VISIBLE
                txtTimeToBeAtHome.visibility = View.VISIBLE
            }
        }

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, UserDetailsActivity::class.java)
            intent.putExtra("user", userList[position])
            holder.itemView.context.startActivity(intent)
        }

    }
}