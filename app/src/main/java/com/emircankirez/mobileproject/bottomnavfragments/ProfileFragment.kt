package com.emircankirez.mobileproject.bottomnavfragments

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.Toast
import com.bumptech.glide.Glide
import com.emircankirez.mobileproject.R
import com.emircankirez.mobileproject.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class ProfileFragment : Fragment() {
    private var _binding : FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var sharedPreferences : SharedPreferences
    private lateinit var fAuth : FirebaseAuth
    private lateinit var fDatabase : FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // firebase init
        fAuth = Firebase.auth
        fDatabase = Firebase.firestore

        sharedPreferences = requireActivity().getSharedPreferences("userInfo", Context.MODE_PRIVATE)

        binding.apply {
            btnSave.setOnClickListener { saveUserInfo(it) }

            // bütün bilgileri eşle
            progressBar.visibility = View.VISIBLE
            createGradeSpinner()
            createStateSpinner()
            if (sharedPreferences.getString("photoUrl", "") != "")
                Glide.with(this@ProfileFragment).load(sharedPreferences.getString("photoUrl", "")).into(imgUser)
            txtName.setText(sharedPreferences.getString("name", ""))
            txtSurname.setText(sharedPreferences.getString("surname", ""))
            txtDepartment.setText(sharedPreferences.getString("department", ""))
            txtDistance.setText(sharedPreferences.getString("distance", ""))
            txtTimeToBeAtHome.setText(sharedPreferences.getString("timeToBeAtHome", ""))
            txtEmail.text = sharedPreferences.getString("emailAddress", "")
            txtPhoneNumber.setText(sharedPreferences.getString("phoneNumber", ""))
            progressBar.visibility = View.INVISIBLE

            spinnerState.onItemSelectedListener = object : OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
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
                            txtDistance.setText("")
                            txtDistanceInfo.visibility = View.GONE
                            txtDistance.visibility = View.GONE
                            txtTimeToBeAtHome.setText("")
                            txtTimeToBeAtHomeInfo.visibility = View.GONE
                            txtTimeToBeAtHome.visibility = View.GONE
                            txtKm.visibility = View.GONE
                            txtDay.visibility = View.GONE
                        }
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {

                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun createGradeSpinner(){
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.grades,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerGrade.adapter = adapter
            val position = adapter.getPosition(sharedPreferences.getString("grade", "1"))
            binding.spinnerGrade.setSelection(position)
        }
    }

    private fun createStateSpinner(){
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.states,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerState.adapter = adapter
            val position = adapter.getPosition(sharedPreferences.getString("state", "Aramıyor"))
            binding.spinnerState.setSelection(position)
        }
    }

    private fun saveUserInfo(view: View){
        binding.apply {
            val name = txtName.text.toString()
            val surname = txtSurname.text.toString()
            val department = txtDepartment.text.toString()
            val grade = spinnerGrade.selectedItem.toString().toInt()
            val state = spinnerState.selectedItem.toString()
            var distance : Double = 0.0
            var timeToBeAtHome : Int = 0

            if(state != "Aramıyor"){
                if(txtDistance.text.toString().toDoubleOrNull() == null){
                    Toast.makeText(requireContext(), "Bilgileriniz kaydedilemedi. Uzaklık için bir sayı giriniz.", Toast.LENGTH_SHORT).show()
                    return
                }

                if(txtTimeToBeAtHome.text.toString().toIntOrNull() == null){
                    Toast.makeText(requireContext(), "Bilgileriniz kaydedilemedi. Evde kalma süresi için bir sayı giriniz.", Toast.LENGTH_SHORT).show()
                    return
                }

                distance = txtDistance.text.toString().toDouble()
                timeToBeAtHome = txtTimeToBeAtHome.text.toString().toInt()
            }

            val phoneNumber = txtPhoneNumber.text.toString()

            // bütün yeni bilgileri firestore'a kaydet - değişim olacağı için MainActivity ztn shared'a kaydedecek
            val updates = hashMapOf<String, Any>(
                "name" to name,
                "surname" to surname,
                "department" to department,
                "grade" to grade,
                "state" to state,
                "distance" to distance,
                "timeToBeAtHome" to timeToBeAtHome,
                "phoneNumber" to phoneNumber
            )

            fDatabase.collection("users").document(fAuth.currentUser?.uid!!).update(updates)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Bilgileriniz kaydedildi.", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), it.localizedMessage, Toast.LENGTH_LONG).show()
                }
        }
    }
}