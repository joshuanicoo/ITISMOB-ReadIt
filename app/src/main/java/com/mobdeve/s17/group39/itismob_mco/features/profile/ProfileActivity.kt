package com.mobdeve.s17.group39.itismob_mco.features.profile

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.mobdeve.s17.group39.itismob_mco.R
import com.mobdeve.s17.group39.itismob_mco.databinding.ProfileActivityLayoutBinding
import com.mobdeve.s17.group39.itismob_mco.features.authentication.login.LoginActivity
import com.mobdeve.s17.group39.itismob_mco.utils.SharedPrefsManager

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ProfileActivityLayoutBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPrefs: SharedPrefsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ProfileActivityLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        sharedPrefs = SharedPrefsManager(this)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.updateProfileBtn.setOnClickListener {
            Toast.makeText(this, "Updated successfully", Toast.LENGTH_SHORT).show()
        }

        binding.logoutBtn.setOnClickListener {
            logout()
        }
    }

    private fun logout() {
        // Clear local data first
        sharedPrefs.clearUserData()

        // Sign out from Firebase
        auth.signOut()

        // Sign out from Google
        googleSignInClient.signOut().addOnCompleteListener {
            navigateToLogin()
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}