package com.emircankirez.mobileproject.login

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.emircankirez.mobileproject.R
import com.emircankirez.mobileproject.databinding.ActivitySignUpBinding
import com.emircankirez.mobileproject.model.User
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import java.io.ByteArrayOutputStream
import java.util.UUID

class SignUpActivity : AppCompatActivity() {
    private lateinit var binding : ActivitySignUpBinding
    private lateinit var fAuth : FirebaseAuth
    private lateinit var fDatabase : FirebaseFirestore
    private lateinit var fStorage: FirebaseStorage
    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionForGalleryLauncher: ActivityResultLauncher<String>
    private lateinit var permissionForCameraLauncher : ActivityResultLauncher<String>
    private var selectedBitmap : Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Firebase init
        fAuth = Firebase.auth
        fDatabase = Firebase.firestore
        fStorage = Firebase.storage

        registerLaunchers()

        binding.apply {
            btnSignUp.setOnClickListener { signUp(it) }
            imgUser.setOnClickListener { selectImage(it) }
        }
    }

    private fun registerLaunchers(){
        galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
            if(result.resultCode == RESULT_OK && result.data != null){
                val intentFromResult = result.data
                val selectedUri = intentFromResult!!.data
                if(selectedUri != null){
                    selectedBitmap = if(Build.VERSION.SDK_INT >= 28){
                        val source = ImageDecoder.createSource(contentResolver, selectedUri)
                        ImageDecoder.decodeBitmap(source)
                    }else{
                        MediaStore.Images.Media.getBitmap(contentResolver, selectedUri)
                    }
                    binding.imgUser.setImageBitmap(selectedBitmap)
                }
            }
        }

        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
            if(result.resultCode == AppCompatActivity.RESULT_OK && result.data != null){
                val bundle = result.data!!.extras
                bundle?.let {
                    selectedBitmap = bundle.get("data") as Bitmap
                    binding.imgUser.setImageBitmap(selectedBitmap)
                }
            }
        }

        permissionForGalleryLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){ result ->
            if(result){
                val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                galleryLauncher.launch(intentToGallery)
            }else{
                Toast.makeText(this@SignUpActivity, "Galeriden resim alabilmek için izin lazım", Toast.LENGTH_SHORT).show()
            }
        }

        permissionForCameraLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){ result ->
            if(result){
                val intentToCamera = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                cameraLauncher.launch(intentToCamera)
            }else{
                Toast.makeText(this@SignUpActivity, "Kamerayı kullanabilmek için izin lazım", Toast.LENGTH_SHORT).show()
            }

        }
    }

    private fun selectImage(view: View){
        val alertBuilder = AlertDialog.Builder(this@SignUpActivity)
        alertBuilder.setTitle("Resim Seç")
        alertBuilder.setMessage("Galeriden ya da kameranızı kullanarak resim seçin")
        alertBuilder.setIcon(R.drawable.baseline_image_24)
        alertBuilder.setPositiveButton("kamera"){ _, _ ->
            // cameradan resim al
            if(ContextCompat.checkSelfPermission(this@SignUpActivity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
                if(ActivityCompat.shouldShowRequestPermissionRationale(this@SignUpActivity, Manifest.permission.CAMERA)){
                    Snackbar.make(view, "Kamera için izin lazım", Snackbar.LENGTH_INDEFINITE).setAction("İzin ver"){
                        // izin al
                        permissionForCameraLauncher.launch(Manifest.permission.CAMERA)
                    }.show()
                }else{
                    permissionForCameraLauncher.launch(Manifest.permission.CAMERA)
                }
            }else{
                // izin alınmış ise
                val intentToCamera = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                cameraLauncher.launch(intentToCamera)
            }
        }

        alertBuilder.setNeutralButton("Kapat"){ dialog, _ ->
            dialog.dismiss()
        }

        alertBuilder.setNegativeButton("Galeri"){ _, _ ->
            // galeriden foto al
            if(ContextCompat.checkSelfPermission(this@SignUpActivity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                if(ActivityCompat.shouldShowRequestPermissionRationale(this@SignUpActivity, Manifest.permission.READ_EXTERNAL_STORAGE)){
                    Snackbar.make(view, "Galeri için izin lazım", Snackbar.LENGTH_INDEFINITE).setAction("İzin ver"){
                        // izin al
                        permissionForGalleryLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }.show()
                }else{
                    permissionForGalleryLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }else{
                // izin alınmış ise
                val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                galleryLauncher.launch(intentToGallery)
            }
        }
        alertBuilder.show()
    }

    private fun signUp(view: View?) {
        if (selectedBitmap != null) {
            binding.apply {
                // önce resmi storage'a kaydet
                val imageName = "${UUID.randomUUID()}.jpg"
                val imageReference = fStorage.reference.child("userImg").child(imageName)

                val baos = ByteArrayOutputStream()
                selectedBitmap?.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                val imgData: ByteArray = baos.toByteArray()

                imageReference.putBytes(imgData)
                    .addOnSuccessListener {
                        imageReference.downloadUrl.addOnSuccessListener { it ->
                            val profilePhotoUrl = it.toString()
                            val email = txtEmail.text.toString()
                            val password = txtPassword.text.toString()

                            /* Okul mailine doğrulama gitmediği için bu kod parçası kaldırılmıştır.
                            if(!email.endsWith("std.yildiz.edu.tr")){
                                Toast.makeText(this@SignUpActivity, "std.yildiz.edu.tr uzantılı mailinizi kullanmanız gerekmektedir", Toast.LENGTH_SHORT).show()
                                return@addOnSuccessListener
                            }*/

                            // resmin url'i başarılı bir şekilde alındıysa kayıt oluştur
                            fAuth.createUserWithEmailAndPassword(email, password)
                                .addOnSuccessListener {
                                    val currentUser = fAuth.currentUser!!

                                    // kayıt başarılı ise kullanıcıyı firestore'da sakla
                                    val user = User(currentUser.uid, txtName.text.toString(), txtSurname.text.toString(), txtEmail.text.toString(), profilePhotoUrl)

                                    fDatabase.collection("users").document(currentUser.uid)
                                        .set(user)
                                        .addOnSuccessListener {
                                            Toast.makeText(this@SignUpActivity, "Kayıt başarılı bir şekilde oluşturuldu.", Toast.LENGTH_SHORT).show()

                                            // verification mail gönder
                                            currentUser.sendEmailVerification()
                                                .addOnSuccessListener {
                                                    Toast.makeText(this@SignUpActivity, "Doğrulama maili başarılı bir şekilde gönderildi.", Toast.LENGTH_SHORT).show()
                                                }
                                                .addOnFailureListener {
                                                    Toast.makeText(this@SignUpActivity, it.localizedMessage, Toast.LENGTH_LONG).show()
                                                }

                                            // geçici bir konum bilgisini kaydet
                                            val location = hashMapOf<String, Any>(
                                                "lastLat" to 41.027419,
                                                "lastLng" to 28.982398,
                                                "lastLocTime" to Timestamp.now()
                                            )

                                            fDatabase.collection("locations").document(currentUser.uid)
                                                .set(location)

                                            fAuth.signOut()
                                            finish()
                                        }
                                        .addOnFailureListener {
                                            // kullanıcı bilgileri firestore'a kaydedilmediyse
                                            imageReference.delete() // yüklenen resmi sil
                                            currentUser.delete() // kaydı oluşturulan kullanıcıyı sil
                                            Toast.makeText(this@SignUpActivity, it.localizedMessage, Toast.LENGTH_LONG).show()
                                        }
                                }
                                .addOnFailureListener {
                                    imageReference.delete() // kayıt oluşturulamadıysa yüklenen resmi sil
                                    Toast.makeText(this@SignUpActivity, it.localizedMessage, Toast.LENGTH_LONG).show()
                                }
                        }
                        .addOnFailureListener {
                            Toast.makeText(this@SignUpActivity, it.localizedMessage, Toast.LENGTH_LONG).show()
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this@SignUpActivity, it.localizedMessage, Toast.LENGTH_LONG).show()
                    }
            }
        }else{
            Toast.makeText(this@SignUpActivity, "Bir profil fotoğrafı seçiniz.", Toast.LENGTH_SHORT).show()
        }
    }
}