package utar.edu.my.fyp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Patterns
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.SignInButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import utar.edu.my.fyp.petschedule.adapters.UserSessionManager
import utar.edu.my.fyp.petschedule.ui.PetViewModel

class MainPage : AppCompatActivity() {

    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnSignIn: Button
    private lateinit var tvSignUp: TextView
    private lateinit var googleBtn: SignInButton
    private lateinit var rememberMeCheckbox: CheckBox

    private lateinit var auth: FirebaseAuth
    private lateinit var dbRef: DatabaseReference
    private lateinit var googleClient: GoogleSignInClient
    private lateinit var petViewModel: PetViewModel

    // Activity Result launcher for Google sign-in intent
    private val googleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleGoogleResult(task)
        } else {
            Toast.makeText(this, "Google Sign-In canceled", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize PetViewModel
        petViewModel = ViewModelProvider(this)[PetViewModel::class.java]

        // Firebase
        auth = FirebaseAuth.getInstance()
        dbRef = FirebaseDatabase.getInstance().getReference("user")

        // UI
        etEmail = findViewById(R.id.emailInput)
        etPassword = findViewById(R.id.passwordInput)
        btnSignIn = findViewById(R.id.signInBtn)
        tvSignUp = findViewById(R.id.signUpText)
        googleBtn = findViewById(R.id.googleSignInBtn)
        rememberMeCheckbox = findViewById(R.id.rememberMeCheckbox)

        // Load saved credentials if remember me was enabled
        loadSavedCredentials()

        // Email/Password login
        btnSignIn.setOnClickListener { loginUser() }

        // Navigate to Register
        tvSignUp.setOnClickListener {
            startActivity(Intent(this, RegisterPage::class.java))
        }

        // Configure Google Sign-In (uses client id from google-services.json)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleClient = GoogleSignIn.getClient(this, gso)

        // Start Google flow
        googleBtn.setSize(SignInButton.SIZE_WIDE)
        googleBtn.setOnClickListener { startGoogleSignIn() }
    }

    private fun loadSavedCredentials() {
        if (UserSessionManager.isRememberMeEnabled(this)) {
            val savedEmail = UserSessionManager.getSavedEmail(this)
            val savedPassword = UserSessionManager.getSavedPassword(this)

            savedEmail?.let { etEmail.setText(it) }
            savedPassword?.let { etPassword.setText(it) }
            rememberMeCheckbox.isChecked = true
        }
    }

    private fun loginUser() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        // Clear previous errors
        etEmail.error = null
        etPassword.error = null

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Email is invalid"
            return
        }
        if (TextUtils.isEmpty(password) || password.length < 6) {
            etPassword.error = "Password is invalid"
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        // SET CURRENT USER IN SESSION
                        UserSessionManager.setCurrentUserId(this, userId)
                        petViewModel.setCurrentUser(userId)

                        // Handle remember me functionality
                        if (rememberMeCheckbox.isChecked) {
                            UserSessionManager.setRememberMe(this, true, email, password)
                        } else {
                            UserSessionManager.setRememberMe(this, false)
                        }

                        checkUserPetProfileInLocalDB()
                    }
                } else {
                    // Log the error for debugging
                    android.util.Log.e("LOGIN_ERROR", "Authentication failed", task.exception)

                    // Handle specific Firebase authentication errors
                    val exception = task.exception
                    when {
                        exception is com.google.firebase.auth.FirebaseAuthInvalidUserException -> {
                            etEmail.error = "Email is not registered"
                            Toast.makeText(this, "Email is not registered", Toast.LENGTH_LONG).show()
                        }
                        exception is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> {
                            etPassword.error = "Password is incorrect"
                            Toast.makeText(this, "Password is incorrect", Toast.LENGTH_LONG).show()
                        }
                        exception is com.google.firebase.auth.FirebaseAuthUserCollisionException -> {
                            Toast.makeText(this, "Account already exists with different sign-in method", Toast.LENGTH_LONG).show()
                        }
                        exception is com.google.firebase.FirebaseNetworkException -> {
                            Toast.makeText(this, "Network error. Please check your connection.", Toast.LENGTH_LONG).show()
                        }
                        else -> {
                            // Fallback to message parsing
                            val errorMessage = exception?.message?.lowercase() ?: ""
                            when {
                                errorMessage.contains("password") || errorMessage.contains("credential") -> {
                                    etPassword.error = "Password is incorrect"
                                    Toast.makeText(this, "Password is incorrect", Toast.LENGTH_LONG).show()
                                }
                                errorMessage.contains("user") || errorMessage.contains("email") -> {
                                    etEmail.error = "Email is not registered"
                                    Toast.makeText(this, "Email is not registered", Toast.LENGTH_LONG).show()
                                }
                                else -> {
                                    Toast.makeText(this, "Login failed: ${exception?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                }
            }
    }

    // -------- Google Sign-In ----------
    private fun startGoogleSignIn() {
        googleLauncher.launch(googleClient.signInIntent)
    }

    private fun handleGoogleResult(task: com.google.android.gms.tasks.Task<GoogleSignInAccount>) {
        try {
            val account = task.getResult(ApiException::class.java)!!
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            auth.signInWithCredential(credential)
                .addOnCompleteListener(this) { signInTask ->
                    if (signInTask.isSuccessful) {
                        val firebaseUser = auth.currentUser
                        firebaseUser?.let { user ->
                            // Save Google user to Firebase (for user profile only)
                            saveGoogleUserToDatabase(user, account)
                        }
                    } else {
                        Toast.makeText(
                            this, "Firebase auth failed: ${signInTask.exception?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        } catch (e: ApiException) {
            Toast.makeText(this, "Google sign-in failed: ${e.statusCode}", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveGoogleUserToDatabase(firebaseUser: com.google.firebase.auth.FirebaseUser, googleAccount: GoogleSignInAccount) {
        val userId = firebaseUser.uid

        // SET CURRENT USER IN SESSION
        UserSessionManager.setCurrentUserId(this, userId)
        petViewModel.setCurrentUser(userId)

        dbRef.child(userId).get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                val userData = hashMapOf(
                    "firstName" to (googleAccount.givenName ?: ""),
                    "lastName" to (googleAccount.familyName ?: ""),
                    "email" to (googleAccount.email ?: ""),
                    "avatarUrl" to (googleAccount.photoUrl?.toString() ?: ""),
                    "age" to "",
                    "phone" to ""
                )

                dbRef.child(userId).setValue(userData)
                    .addOnSuccessListener {
                        checkUserPetProfileInLocalDB()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to save Google user data: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            } else {
                checkUserPetProfileInLocalDB()
            }
        }.addOnFailureListener { error ->
            Toast.makeText(this, "Database error: ${error.message}", Toast.LENGTH_LONG).show()
        }
    }

    // -------- Check Local Database for Pets ----------
    private fun checkUserPetProfileInLocalDB() {
        petViewModel.allPets.observe(this) { pets ->
            // Remove observer immediately to prevent multiple triggers
            petViewModel.allPets.removeObservers(this)

            if (pets != null && pets.isNotEmpty()) {
                // User has pets - go to dashboard
                Toast.makeText(this, "Welcome! Pets found in your device.", Toast.LENGTH_LONG).show()
                navigateToDashboard()
            } else {
                // No pets found - go to add pet screen
                Toast.makeText(this, "No pet profile found. Add your first pet!", Toast.LENGTH_LONG).show()
                navigateToAddPet()
            }
        }
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, DashboardPage::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToAddPet() {
        val intent = Intent(this, AddPetNewUser::class.java)
        startActivity(intent)
        finish()
    }
}