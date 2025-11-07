package com.mobdeve.s17.group39.itismob_mco.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.GoogleAuthProvider
import com.mobdeve.s17.group39.itismob_mco.R
import com.mobdeve.s17.group39.itismob_mco.databinding.LoginActivityBinding
import com.mobdeve.s17.group39.itismob_mco.ui.homepage.HomeActivity
import androidx.core.content.edit

class LoginActivity : AppCompatActivity() {

    private lateinit var data: ArrayList<LoginOnboardingModel>
    private lateinit var binding: LoginActivityBinding
    private lateinit var adapter: LoginAdapter
    lateinit var mGoogleSignInClient: GoogleSignInClient
    val Req_Code: Int = 123
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LoginActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        FirebaseApp.initializeApp(this)

        this.data = loadData()

        // Snap on scroll thingy
        val snapHelper: SnapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(binding.loginRv)

        this.adapter = LoginAdapter(data)
        this.binding.loginRv.adapter = adapter
        this.binding.loginRv.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.HORIZONTAL,
            false
        )
        setupTabLayoutIndicator()

        // Google Auth stuff
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail() // Fetch email from G
            .requestProfile() // Fetch profile details from G
            .build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
        firebaseAuth = FirebaseAuth.getInstance()

        binding.loginBtn.setOnClickListener {
            signInGoogle()
        }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }
    }

    private fun signInGoogle() {
        val signInIntent: Intent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, Req_Code)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Req_Code) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleResult(task)
        }
    }

    private fun handleResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account: GoogleSignInAccount? = completedTask.getResult(ApiException::class.java)
            if (account != null) {
                UpdateUI(account)
            } else {
                Toast.makeText(this, "Google sign in failed", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            Toast.makeText(this, "Failed: " + e.statusCode, Toast.LENGTH_SHORT).show()
        }
    }

    private fun UpdateUI(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val firebaseUser = firebaseAuth.currentUser

                if (firebaseUser != null) {
                    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

                    val user = hashMapOf(
                        "uid" to firebaseUser.uid,
                        "username" to account.displayName,
                        "email" to account.email,
                        "profile_picture" to account.photoUrl?.toString(),
                        "bio" to null,
                        "favorites" to null,
                        "date_created" to com.google.firebase.Timestamp.now(),
                        "date_updated" to com.google.firebase.Timestamp.now()
                    )

                    db.collection("users").document(firebaseUser.uid)
                        .set(user)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Welcome, ${account.displayName}!", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, HomeActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                        .addOnFailureListener { e: Exception ->
                            Toast.makeText(this, "Failed to save user data: ${e.message}", Toast.LENGTH_LONG).show()
                        }

                }
            } else {
                Toast.makeText(this, "Firebase authentication failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }


    private fun setupTabLayoutIndicator() {
        for (i in 0 until 3) {
            binding.loginIndicator.addTab(binding.loginIndicator.newTab())
        }

        binding.loginRv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                if (firstVisibleItemPosition != RecyclerView.NO_POSITION) {
                    binding.loginIndicator.getTabAt(firstVisibleItemPosition)?.select()
                }
            }
        })
    }

    private fun loadData(): ArrayList<LoginOnboardingModel> {
        val tempData = ArrayList<LoginOnboardingModel>()
        tempData.add(
            LoginOnboardingModel(
                "Your gateway to countless stories.",
                "Track the books you love, rate and review every " +
                        "page-turner, and build your virtual bookshelf for the world to see.",
                R.drawable.reading_goose_1
            )
        )
        tempData.add(
            LoginOnboardingModel(
                "Your Book Club, Without the Schedule.",
                "Share your literary journey with friends. Form clubs, spark discussions " +
                        "on your favorite passages, and see what your network is devouring next. " +
                        "Reading is better together.",
                R.drawable.reading_goose_2
            )
        )

        tempData.add(
            LoginOnboardingModel(
                "Your Next Favorite Book is Waiting.",
                "Unleash the power of the world's most passionate reading community. " +
                        "With millions of titles and countless user-generated reviews and shelves, " +
                        "your next five-star read is just a search away.",
                R.drawable.reading_goose_3
            )
        )
        return tempData
    }
}