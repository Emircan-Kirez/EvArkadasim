package com.emircankirez.mobileproject.login

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.emircankirez.mobileproject.activities.MainActivity
import com.emircankirez.mobileproject.databinding.ActivitySignInBinding
import com.emircankirez.mobileproject.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase

class SignInActivity : AppCompatActivity() {
    private lateinit var binding : ActivitySignInBinding
    private lateinit var fAuth : FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // firebase init
        fAuth = Firebase.auth

        binding.apply {
            btnSignIn.setOnClickListener { signIn(it) }

            txtForgotPassword.setOnClickListener {
                val intent = Intent(this@SignInActivity, ForgotPasswordActivity::class.java)
                startActivity(intent)
            }

            txtNoAccount.setOnClickListener {
                val intent = Intent(this@SignInActivity, SignUpActivity::class.java)
                startActivity(intent)
            }

            txtVerification.setOnClickListener {
                val email = binding.txtEmail.text.toString()
                val password = binding.txtPassword.text.toString()

                if(email.isEmpty() || password.isEmpty()){
                    Toast.makeText(this@SignInActivity, "Boşlukları doldurunuz.", Toast.LENGTH_SHORT).show()
                }else{
                    fAuth.signInWithEmailAndPassword(email, password)
                        .addOnSuccessListener {
                            val user = fAuth.currentUser!!
                            if(user.isEmailVerified){
                                Toast.makeText(this@SignInActivity, "Bu hesap zaten doğrulanmış.", Toast.LENGTH_SHORT).show()
                            }else {
                                user.sendEmailVerification()
                                    .addOnSuccessListener {
                                        Toast.makeText(this@SignInActivity, "Hesabınızı doğrulamak için yeni doğrulama maili başarılı bir şekilde gönderildi.", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(this@SignInActivity, it.localizedMessage, Toast.LENGTH_LONG).show()
                                    }
                            }
                            fAuth.signOut()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this@SignInActivity, it.localizedMessage, Toast.LENGTH_LONG).show()
                        }
                }
            }
        }
    }

    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = fAuth.currentUser
        if (currentUser != null) {
            reload()
        }
    }

    private fun reload(){
        val intent = Intent(this@SignInActivity, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun signIn(view : View){
        val email = binding.txtEmail.text.toString()
        val password = binding.txtPassword.text.toString()

        if(email.isEmpty() || password.isEmpty()){
            Toast.makeText(this@SignInActivity, "Boşlukları doldurunuz.", Toast.LENGTH_SHORT).show()
            return
        }

        fAuth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                val user = fAuth.currentUser!!
                if(user.isEmailVerified){ // kullanıcı hesabını doğrulamadan uygulamaya giriş yapamıyor.
                    reload()
                }else {
                    Toast.makeText(this@SignInActivity, "Hesabınızı doğrulamadan giriş yapamazsınız!!", Toast.LENGTH_SHORT).show()
                    user.sendEmailVerification()
                        .addOnSuccessListener {
                            Toast.makeText(this@SignInActivity, "Hesabınızı doğrulamak için yeni doğrulama maili başarılı bir şekilde gönderildi.", Toast.LENGTH_LONG).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this@SignInActivity, it.localizedMessage, Toast.LENGTH_LONG).show()
                        }
                    fAuth.signOut()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this@SignInActivity, it.localizedMessage, Toast.LENGTH_LONG).show()
            }
    }
}