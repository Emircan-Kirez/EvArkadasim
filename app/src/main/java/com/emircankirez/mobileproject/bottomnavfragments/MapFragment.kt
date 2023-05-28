package com.emircankirez.mobileproject.bottomnavfragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.emircankirez.mobileproject.R
import com.emircankirez.mobileproject.databinding.FragmentMapBinding
import com.emircankirez.mobileproject.model.LastLocation
import com.emircankirez.mobileproject.model.User
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat

class MapFragment : Fragment() {
    private var _binding : FragmentMapBinding? = null
    private val binding get() = _binding!!
    private lateinit var permissionLauncher : ActivityResultLauncher<String>
    private lateinit var fDatabase : FirebaseFirestore
    private lateinit var fAuth : FirebaseAuth

    @SuppressLint("MissingPermission")
    private val callback = OnMapReadyCallback { googleMap ->
        /**
         * Manipulates the map once available.
         * This callback is triggered when the map is ready to be used.
         * This is where we can add markers or lines, add listeners or move the camera.
         * In this case, we just add a marker near Sydney, Australia.
         * If Google Play services is not installed on the device, the user will be prompted to
         * install it inside the SupportMapFragment. This method will only be triggered once the
         * user has installed Google Play services and returned to the app.
         */

        if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION)){
                Snackbar.make(binding.root, "Permission needed for location", Snackbar.LENGTH_INDEFINITE).setAction("Give Permission"){ result ->
                    // request permission
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }.show()
            }else{
                // request permission
                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }else{
            val locationManager = requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if(lastLocation != null){
                val userLatLng = LatLng(lastLocation.latitude, lastLocation.longitude)
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
            }
            googleMap.isMyLocationEnabled = true // harita üzerinde kendi konumunu mavi nokta yapar.

            getLocations(googleMap)
        }
    }

    private fun getLocations(googleMap: GoogleMap) {
        fDatabase.collection("locations")
            .get()
            .addOnSuccessListener { locationSnapshots ->
                if(locationSnapshots != null && !locationSnapshots.isEmpty){
                    for(locationSnapshot in locationSnapshots){
                        val uid = locationSnapshot.id
                        fDatabase.collection("users").document(uid)
                            .get()
                            .addOnSuccessListener { userSnapshot ->
                                val userInfo = userSnapshot.toObject(User::class.java)!!
                                // Aramıyor durumunda olanları haritada gösterme - kendisini map'te marker ile göstermeye gerek yok
                                if(userInfo.state != "Aramıyor" && locationSnapshot.id != fAuth.currentUser?.uid){ //
                                    val location = locationSnapshot.toObject(LastLocation::class.java)
                                    googleMap.addMarker(MarkerOptions().position(LatLng(location.lastLat, location.lastLng))
                                        .title("${userInfo.name} ${userInfo.surname}")
                                        .snippet(SimpleDateFormat("dd/MM/yyyy HH:mm").format(location.lastLocTime.toDate()))
                                    )
                                }
                            }
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), it.localizedMessage, Toast.LENGTH_SHORT).show()
            }
    }

    private fun registerLauncher() {
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){ result ->
            if(result){
                Toast.makeText(requireContext(), "Konum izni verildi", Toast.LENGTH_SHORT).show()
            }else{
                Toast.makeText(requireContext(), "Konum izni verilmedi", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
        registerLauncher()
        fDatabase = Firebase.firestore
        fAuth = Firebase.auth
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}