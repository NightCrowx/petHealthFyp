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
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class RegisterPage : AppCompatActivity() {

    private lateinit var etFirstName: TextInputEditText
    private lateinit var etLastName: TextInputEditText
    private lateinit var etAge: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etReenterPassword: TextInputEditText
    private lateinit var btnSignUp: Button
    private lateinit var tvLogin: TextView

    private lateinit var auth: FirebaseAuth
    private lateinit var dbRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_page)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        dbRef = FirebaseDatabase.getInstance().getReference("user")

        // Initialize UI components
        etFirstName = findViewById(R.id.et_firstname)
        etLastName = findViewById(R.id.et_lastname)
        etAge = findViewById(R.id.et_age)
        etEmail = findViewById(R.id.et_email)
        etPhone = findViewById(R.id.et_phone)
        etPassword = findViewById(R.id.et_password)
        etReenterPassword = findViewById(R.id.et_reenter_password)
        btnSignUp = findViewById(R.id.btn_signup)
        tvLogin = findViewById(R.id.tvLogin)

        // Handle sign-up button click
        btnSignUp.setOnClickListener {
            getNextUserId { userId ->
                registerUser(userId)
            }
        }

        // Navigate to Login Page when user clicks "Login"
        tvLogin.setOnClickListener {
            val intent = Intent(this, MainPage::class.java)
            startActivity(intent)
            finish() // Prevent going back to registration page
        }
    }

    // Function to retrieve and generate the next userId
    private fun getNextUserId(callback: (String) -> Unit) {
        dbRef.orderByKey().limitToLast(1).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                // Get the last userId and increment it
                val lastUserId = snapshot.children.last().key
                val nextNumber = if (lastUserId != null && lastUserId.startsWith("user_")) {
                    lastUserId.removePrefix("user_").toInt() + 1
                } else {
                    1
                }
                val newUserId = String.format("user_%03d", nextNumber) // Format as user_001, user_002
                callback(newUserId)
            } else {
                callback("user_001") // If no users exist, start from user_001
            }
        }.addOnFailureListener {
            callback("user_001") // Default if any error occurs
        }
    }

    private fun registerUser(userId: String) {
        val firstName = etFirstName.text.toString().trim()
        val lastName = etLastName.text.toString().trim()
        val age = etAge.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val reenterPassword = etReenterPassword.text.toString().trim()

        // Validate Inputs
        if (TextUtils.isEmpty(firstName)) {
            etFirstName.error = "First name is required"
            return
        }
        if (TextUtils.isEmpty(lastName)) {
            etLastName.error = "Last name is required"
            return
        }
        if (TextUtils.isEmpty(age) || !age.matches(Regex("\\d+"))) {
            etAge.error = "Enter a valid age"
            return
        }
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Enter a valid email"
            return
        }
        if (TextUtils.isEmpty(phone) || !phone.matches(Regex("^\\d{10,15}$"))) {
            etPhone.error = "Enter a valid phone number"
            return
        }
        if (TextUtils.isEmpty(password) || password.length < 6) {
            etPassword.error = "Password must be at least 6 characters"
            return
        }
        if (password != reenterPassword) {
            etReenterPassword.error = "Passwords do not match"
            return
        }

        // Firebase Authentication: Create User
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {

                    val user = hashMapOf(
                        "firstName" to firstName,
                        "lastName" to lastName,
                        "age" to age,
                        "email" to email,
                        "phone" to phone,

                    )

                    dbRef.child(userId).setValue(user)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Registration successful!", Toast.LENGTH_LONG).show()
                            val intent = Intent(this, MainPage::class.java)
                            startActivity(intent)
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to save user data: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                } else {
                    val errorMessage = task.exception?.message
                    if (errorMessage?.contains("The email address is already in use") == true) {
                        Toast.makeText(this, "Email already registered. Please log in.", Toast.LENGTH_LONG).show()
                        val intent = Intent(this, MainPage::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, "Registration failed: $errorMessage", Toast.LENGTH_LONG).show()
                    }
                }
            }
    }
}
