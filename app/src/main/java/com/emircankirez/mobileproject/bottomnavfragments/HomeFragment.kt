package com.emircankirez.mobileproject.bottomnavfragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.emircankirez.mobileproject.R
import com.emircankirez.mobileproject.adapter.UserAdapter
import com.emircankirez.mobileproject.databinding.FragmentHomeBinding
import com.emircankirez.mobileproject.model.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class HomeFragment : Fragment() {
    private var _binding : FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var fDatabase : FirebaseFirestore
    private lateinit var registration: ListenerRegistration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        registration.remove()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // firebase init
        fDatabase = Firebase.firestore

        binding.apply {
            btnSearch.setOnClickListener { search(it) }

            // başlangıçta filtre ve arama kapalı - bütün kullanıcılar gösterilir
            spinnerFilter.isEnabled = false
            btnSearch.isClickable = false
            txtSearch.isEnabled = false

            createSpinner()
            recyclerViewUsers.layoutManager = LinearLayoutManager(requireContext())

            // veri değişirse onu dinler
            registration = fDatabase.collection("users").addSnapshotListener { _, error ->
                if(error != null){
                    Toast.makeText(requireContext(), error.localizedMessage, Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if(!cbFilter.isChecked){ // filtre kapalı -> bütün kullanıcıları göster
                    getAllUsers()
                }else {
                    search(view)
                }
            }


            cbFilter.setOnCheckedChangeListener { _, isChecked ->
                if(isChecked){
                    cbFilter.text = "Filtre: Açık"
                    spinnerFilter.isEnabled = true
                    btnSearch.isClickable = true
                    txtSearch.isEnabled = true
                    btnSearch.setImageResource(R.drawable.baseline_search_24)
                }else{
                    cbFilter.text = "Filtre: Kapalı"
                    txtSearch.setText("")
                    spinnerFilter.isEnabled = false
                    btnSearch.isClickable = false
                    txtSearch.isEnabled = false
                    btnSearch.setImageResource(R.drawable.baseline_search_off_24)
                    // filtre yokken bütün herkesi göster
                    getAllUsers()
                }
            }

        }
    }

    private fun createSpinner(){
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.filters,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerFilter.adapter = adapter
        }
    }


    private fun getAllUsers(){
        binding.progressBar.visibility = View.VISIBLE

        fDatabase.collection("users")
            .orderBy("name") // isme göre sıralı
            .get()
            .addOnSuccessListener { snapshot ->
                organiseRecyclerView(snapshot)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), it.localizedMessage, Toast.LENGTH_LONG).show()
            }
        binding.progressBar.visibility = View.INVISIBLE
    }

    private fun search(view : View){
        binding.apply {
            val searchMessage = txtSearch.text.toString()
            when (spinnerFilter.selectedItem.toString()) {
                "İsim" -> searchByName(searchMessage)
                "İsim Soyisim" -> searchByNameAndSurname(searchMessage)
                "Sınıf" -> searchByGrade(searchMessage)
                "Kampüse uzaklık" -> searchByDistance(searchMessage)
                "Evde kalma süresi" -> searchByTimeToBeAtHome(searchMessage)
            }
        }
    }

    private fun searchByName(name: String) {
        binding.progressBar.visibility = View.VISIBLE

        fDatabase.collection("users")
            .whereEqualTo("name", name)
            .orderBy("surname")
            .get()
            .addOnSuccessListener { snapshot ->
                organiseRecyclerView(snapshot)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), it.localizedMessage, Toast.LENGTH_LONG).show()
            }
        binding.progressBar.visibility = View.INVISIBLE
    }

    private fun searchByNameAndSurname(fullName: String) {
        val idx = fullName.lastIndexOf(' ')
        if (idx == -1) {
            Toast.makeText(requireContext(), "Geçersiz format: İsim ve soyisimler arasında ' ' bırakarak yazınız.", Toast.LENGTH_LONG).show()
            return
        }

        val firstName = fullName.substring(0, idx)
        val lastName = fullName.substring(idx + 1)

        binding.progressBar.visibility = View.VISIBLE
        fDatabase.collection("users")
            .whereEqualTo("name", firstName)
            .whereEqualTo("surname", lastName)
            .get()
            .addOnSuccessListener { snapshot ->
                organiseRecyclerView(snapshot)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), it.localizedMessage, Toast.LENGTH_LONG).show()
            }
        binding.progressBar.visibility = View.INVISIBLE
    }

    private fun searchByGrade(searchMessage: String) {
        if(searchMessage.toIntOrNull() == null){
            Toast.makeText(requireContext(), "Lütfen bir tamsayı giriniz.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE

        val grade = searchMessage.toInt()
        fDatabase.collection("users")
            .whereEqualTo("grade", grade)
            .orderBy("name")
            .get()
            .addOnSuccessListener { snapshot ->
                organiseRecyclerView(snapshot)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), it.localizedMessage, Toast.LENGTH_LONG).show()
            }
        binding.progressBar.visibility = View.INVISIBLE
    }

    private fun searchByDistance(searchMessage : String) {
        if(searchMessage.toDoubleOrNull() == null){
            Toast.makeText(requireContext(), "Küsüratlı sayı için '.' kullanınız.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        val distance = searchMessage.toDouble()
        fDatabase.collection("users")
            .whereEqualTo("distance", distance)
            .orderBy("name")
            .get()
            .addOnSuccessListener { snapshot ->
                organiseRecyclerView(snapshot)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), it.localizedMessage, Toast.LENGTH_LONG).show()
            }
        binding.progressBar.visibility = View.INVISIBLE
    }

    private fun searchByTimeToBeAtHome(searchMessage: String) {
        if(searchMessage.toIntOrNull() == null){
            Toast.makeText(requireContext(), "Lütfen bir tamsayı giriniz.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        val timeToBeAtHome = searchMessage.toInt()
        fDatabase.collection("users")
            .whereEqualTo("timeToBeAtHome", timeToBeAtHome)
            .orderBy("name")
            .get()
            .addOnSuccessListener { snapshot ->
                organiseRecyclerView(snapshot)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), it.localizedMessage, Toast.LENGTH_LONG).show()
            }
        binding.progressBar.visibility = View.INVISIBLE
    }

    private fun organiseRecyclerView(snapshot: QuerySnapshot){
        val users = ArrayList<User>()
        if(!snapshot.isEmpty){
            for (document in snapshot.documents){
                val user = document.toObject(User::class.java)!!
                users.add(user)
            }
            // Toast.makeText(requireContext(), "${users.size} adet öğrenci bulundu.", Toast.LENGTH_LONG).show()
        }else{
            Toast.makeText(requireContext(), "Hiç öğrenci bulunamadı.", Toast.LENGTH_LONG).show()
        }

        val adapter = UserAdapter(users)
        binding.recyclerViewUsers.adapter = adapter
    }
}