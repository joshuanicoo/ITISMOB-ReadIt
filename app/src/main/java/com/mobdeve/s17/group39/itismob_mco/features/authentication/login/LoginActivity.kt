package com.mobdeve.s17.group39.itismob_mco.features.authentication.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.mobdeve.s17.group39.itismob_mco.R
import com.mobdeve.s17.group39.itismob_mco.database.UsersDatabase
import com.mobdeve.s17.group39.itismob_mco.databinding.LoginActivityBinding
import com.mobdeve.s17.group39.itismob_mco.features.homepage.HomeActivity
import com.mobdeve.s17.group39.itismob_mco.models.UserModel
import com.mobdeve.s17.group39.itismob_mco.utils.JwtUtils
import com.mobdeve.s17.group39.itismob_mco.utils.SharedPrefsManager

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: LoginActivityBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPrefs: SharedPrefsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LoginActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Initialize utilities
        sharedPrefs = SharedPrefsManager(this)
        auth = FirebaseAuth.getInstance()

        // Check if user has already logged in
        if (sharedPrefs.isUserLoggedIn()) {
            navigateToHome()
            return
        }

        // Initialize onboarding data
        setupOnboarding()

        // Configure Google Sign-In
        configureGoogleSignIn()

        // Handle google sign in button click
        binding.loginBtn.setOnClickListener {
            signInGoogle()
        }
    }

    private fun setupOnboarding() {
        val data = loadData()
        val adapter = LoginAdapter(data)

        val snapHelper: SnapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(binding.loginRv)

        binding.loginRv.adapter = adapter
        binding.loginRv.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.HORIZONTAL,
            false
        )
        setupTabLayoutIndicator()
    }

    private fun configureGoogleSignIn() {
        try {
            val webClientId = getString(R.string.default_web_client_id)
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .requestProfile()
                .build()

            googleSignInClient = GoogleSignIn.getClient(this, gso)
        } catch (e: Exception) {
            Toast.makeText(this, "Error setting up sign in", Toast.LENGTH_LONG).show()
        }
    }

    private fun signInGoogle() {
        try {
            val signInIntent: Intent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot start sign in", Toast.LENGTH_SHORT).show()
        }
    }

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                try {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    handleGoogleSignInResult(task)
                } catch (e: Exception) {
                    Toast.makeText(this, "Error processing sign in", Toast.LENGTH_SHORT).show()
                }
            }
            Activity.RESULT_CANCELED -> {
                // User cancelled - no action needed
            }
            else -> {
                Toast.makeText(this, "Sign in failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleGoogleSignInResult(task: com.google.android.gms.tasks.Task<GoogleSignInAccount>) {
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                authenticateWithFirebase(account)
            } else {
                Toast.makeText(this, "Sign in failed", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            Toast.makeText(this, "Sign in failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun authenticateWithFirebase(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)

        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    if (firebaseUser != null) {
                        checkUserExistsAndProceed(firebaseUser.uid, account)
                    } else {
                        Toast.makeText(this, "Authentication error", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Authentication failed", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun checkUserExistsAndProceed(documentId: String, account: GoogleSignInAccount) {
        UsersDatabase.getById(documentId).addOnCompleteListener { userTask ->
            if (userTask.isSuccessful) {
                val document = userTask.result
                if (document != null && document.exists()) {
                    handleExistingUser(documentId, account)
                } else {
                    handleNewUser(documentId, account)
                }
            } else {
                handleNewUser(documentId, account)
            }
        }
    }

    private fun handleExistingUser(documentId: String, account: GoogleSignInAccount) {
        UsersDatabase.getDocumentReference(documentId).update("date_updated", Timestamp.now())
            .addOnSuccessListener {
                completeAuthentication(documentId, account, "Welcome back, ${account.displayName}!")
            }
            .addOnFailureListener {
                completeAuthentication(documentId, account, "Welcome back, ${account.displayName}!")
            }
    }

    private fun handleNewUser(documentId: String, account: GoogleSignInAccount) {
        val user = UserModel(
            documentId = documentId,
            username = account.displayName ?: "User",
            email = account.email ?: "",
            profilePicture = account.photoUrl?.toString(),
            bio = null,
            favorites = emptyList(),
            dateCreated = Timestamp.now(),
            dateUpdated = Timestamp.now()
        )

        UsersDatabase.createWithId(documentId, user)
            .addOnSuccessListener {
                completeAuthentication(documentId, account, "Welcome to ${R.string.app_name}, ${account.displayName}!")
            }
            .addOnFailureListener { e ->
                completeAuthentication(documentId, account, "Welcome, ${account.displayName}!")
            }
    }

    private fun completeAuthentication(userId: String, account: GoogleSignInAccount, message: String) {
        try {
            val token = JwtUtils.createToken(userId, account.email ?: "", account.displayName ?: "User")
            sharedPrefs.saveAuthToken(token)

            // Save user info
            sharedPrefs.saveUserInfo(
                userId = userId,
                email = account.email ?: "",
                name = account.displayName ?: "User",
                profilePicture = account.photoUrl?.toString() // Save the profile picture URL
            )

            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            navigateToHome()
        } catch (e: Exception) {
            Toast.makeText(this, "Authentication complete", Toast.LENGTH_LONG).show()
            navigateToHome()
        }
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
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
                "Track the books you love, rate and review every page-turner, and build your virtual bookshelf for the world to see.",
                R.drawable.reading_goose_1
            )
        )
        tempData.add(
            LoginOnboardingModel(
                "Your Book Club, Without the Schedule.",
                "Share your literary journey with friends. Form clubs, spark discussions on your favorite passages, and see what your network is devouring next. Reading is better together.",
                R.drawable.reading_goose_2
            )
        )
        tempData.add(
            LoginOnboardingModel(
                "Your Next Favorite Book is Waiting.",
                "Unleash the power of the world's most passionate reading community. With millions of titles and countless user-generated reviews and shelves, your next five-star read is just a search away.",
                R.drawable.reading_goose_3
            )
        )
        return tempData
    }
}