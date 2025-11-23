package com.mobdeve.s17.group39.itismob_mco.features.profile

import android.content.Intent
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.mobdeve.s17.group39.itismob_mco.R
import com.mobdeve.s17.group39.itismob_mco.database.UsersDatabase
import com.mobdeve.s17.group39.itismob_mco.databinding.ProfileActivityLayoutBinding
import com.mobdeve.s17.group39.itismob_mco.features.authentication.login.LoginActivity
import com.mobdeve.s17.group39.itismob_mco.models.UserModel

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ProfileActivityLayoutBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ProfileActivityLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Set up current user data
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Get user data from Firestore
            UsersDatabase.getById(currentUser.uid)
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        val userModel = documentSnapshot.toObject(UserModel::class.java)

                        // Set current user pfp
                        val photoUrl = userModel?.profilePicture ?: currentUser.photoUrl?.toString()
                        if (!photoUrl.isNullOrEmpty()) {
                            Glide.with(binding.root.context)
                                .load(photoUrl)
                                .placeholder(R.drawable.user_pfp_placeholder)
                                .error(R.drawable.user_pfp_placeholder)
                                .circleCrop()
                                .into(binding.profilePicIv)
                        } else {
                            binding.profilePicIv.setImageResource(R.drawable.user_pfp_placeholder)
                        }

                        // Set current username
                        val username = userModel?.username ?: currentUser.displayName ?: "User"
                        binding.profileNameEt.text = SpannableStringBuilder(username)

                        // Set current bio if available
                        val bio = userModel?.bio ?: ""
                        binding.profileBioEt.text = SpannableStringBuilder(bio)
                    }
                }
        }

        binding.updateProfileBtn.setOnClickListener {
            updateProfile()
        }

        binding.logoutBtn.setOnClickListener {
            logout()
        }
    }

    private fun updateProfile() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val newUsername = binding.profileNameEt.text.toString().trim()
            val newBio = binding.profileBioEt.text.toString().trim()

            if (newUsername.isEmpty()) {
                Toast.makeText(this, "Username cannot be empty", Toast.LENGTH_SHORT).show()
                return
            }

            val updates = hashMapOf<String, Any>(
                "username" to newUsername,
                "bio" to newBio,
                "date_updated" to com.google.firebase.Timestamp.now()
            )

            UsersDatabase.update(currentUser.uid, updates)
                .addOnSuccessListener {
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun logout() {
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