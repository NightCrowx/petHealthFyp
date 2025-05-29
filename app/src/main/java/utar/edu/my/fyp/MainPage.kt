package utar.edu.my.fyp

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Patterns
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class MainPage : AppCompatActivity() {

    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnSignIn: Button
    private lateinit var tvSignUp: TextView

    private lateinit var auth: FirebaseAuth
    private lateinit var dbRef: DatabaseReference


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        dbRef = FirebaseDatabase.getInstance().getReference("user")

        // Initialize UI Components
        etEmail = findViewById(R.id.emailInput)
        etPassword = findViewById(R.id.passwordInput)
        btnSignIn = findViewById(R.id.signInBtn)
        tvSignUp = findViewById(R.id.signUpText)

        // Handle Login Click
        btnSignIn.setOnClickListener {
            loginUser()
        }

        // Navigate to Sign-Up Page if user doesn't have an account
        tvSignUp.setOnClickListener {
            val intent = Intent(this, RegisterPage::class.java)
            startActivity(intent)
        }
    }

    private fun loginUser() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        // Validate email and password fields
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Enter a valid email"
            return
        }
        if (TextUtils.isEmpty(password) || password.length < 6) {
            etPassword.error = "Password must be at least 6 characters"
            return
        }

        // Firebase Authentication: Sign in
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Login Successful, check have pet profile or not
                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        checkUserPetProfile(userId)
                    }
                } else {
                    // Login Failed, show error
                    Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun checkUserPetProfile(userId: String) {
        // Reference to user's pets data
        dbRef.child(userId).child("pets")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists() && snapshot.childrenCount > 0) {
                        //  User has pets → Navigate to Dashboard
                        Toast.makeText(this@MainPage, "Welcome back!", Toast.LENGTH_LONG).show()
                        val intent = Intent(this@MainPage, DashboardPage::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        //  No pet profile → Navigate to AddPetNewUser
                        Toast.makeText(
                            this@MainPage,
                            "No pet profile found. Add your first pet!",
                            Toast.LENGTH_LONG
                        ).show()
                        val intent = Intent(this@MainPage, AddPetNewUser::class.java)
                        startActivity(intent)
                        finish()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@MainPage,
                        "Database error: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }
}
