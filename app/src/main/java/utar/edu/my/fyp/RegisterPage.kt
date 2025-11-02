package utar.edu.my.fyp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
import android.util.Patterns
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import utar.edu.my.fyp.petschedule.ui.PetViewModel

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

    // Avatar components
    private lateinit var ivAvatar: ImageView
    private lateinit var btnSelectAvatar: Button
    private var selectedImageUri: Uri? = null
    private lateinit var storageRef: StorageReference

    // Password layout components
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var reenterPasswordLayout: TextInputLayout
    private lateinit var emailLayout: TextInputLayout

    private lateinit var auth: FirebaseAuth
    private lateinit var dbRef: DatabaseReference
    private lateinit var petViewModel: PetViewModel

    companion object {
        private const val ALLOWED_EMAIL_DOMAIN = "@paw.com"
    }

    // Image picker launcher
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            ivAvatar.setImageURI(it)
            Toast.makeText(this, "Avatar selected", Toast.LENGTH_SHORT).show()
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_page)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        dbRef = FirebaseDatabase.getInstance().getReference("user")
        storageRef = FirebaseStorage.getInstance().reference

        // Initialize PetViewModel
        petViewModel = ViewModelProvider(this)[PetViewModel::class.java]

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

        // Initialize avatar components
        ivAvatar = findViewById(R.id.iv_avatar)
        btnSelectAvatar = findViewById(R.id.btn_select_avatar)

        // Initialize password layout components
        passwordLayout = findViewById(R.id.password_layout)
        reenterPasswordLayout = findViewById(R.id.reenter_password_layout)
        emailLayout = findViewById(R.id.email_layout)

        // Setup password toggle functionality
        setupPasswordToggle()

        // Handle avatar selection
        btnSelectAvatar.setOnClickListener {
            selectAvatarImage()
        }

        // Handle sign-up button click
        btnSignUp.setOnClickListener {
            registerUser()
        }

        // Navigate to Login Page when user clicks "Login"
        tvLogin.setOnClickListener {
            val intent = Intent(this, MainPage::class.java)
            startActivity(intent)
            finish() // Prevent going back to registration page
        }
    }

    private fun selectAvatarImage() {
        imagePickerLauncher.launch("image/*")
    }

    private fun setupPasswordToggle() {
        // Handle password field toggle
        passwordLayout.setEndIconOnClickListener {
            val isPasswordVisible = etPassword.inputType == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            if (isPasswordVisible) {
                // Currently visible, hide it
                etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            } else {
                // Currently hidden, show it
                etPassword.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            }
            // Move cursor to end
            etPassword.setSelection(etPassword.text?.length ?: 0)
        }

        // Handle re-enter password field toggle
        reenterPasswordLayout.setEndIconOnClickListener {
            val isPasswordVisible = etReenterPassword.inputType == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            if (isPasswordVisible) {
                // Currently visible, hide it
                etReenterPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            } else {
                // Currently hidden, show it
                etReenterPassword.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            }
            // Move cursor to end
            etReenterPassword.setSelection(etReenterPassword.text?.length ?: 0)
        }
    }

    private fun isValidPawEmail(email: String): Boolean {
        return email.endsWith(ALLOWED_EMAIL_DOMAIN) &&
                Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun validateEmail(): Boolean {
        val email = etEmail.text.toString().trim()

        return when {
            TextUtils.isEmpty(email) -> {
                emailLayout.error = "Email is required"
                false
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                emailLayout.error = "Enter a valid email format"
                false
            }
            !email.endsWith(ALLOWED_EMAIL_DOMAIN) -> {
                emailLayout.error = "Only $ALLOWED_EMAIL_DOMAIN emails are allowed"
                false
            }
            else -> {
                emailLayout.error = null
                true
            }
        }
    }

    private fun uploadAvatarAndRegisterUser(
        firstName: String,
        lastName: String,
        age: String,
        email: String,
        phone: String,
        password: String
    ) {
        selectedImageUri?.let { uri ->
            val userId = auth.currentUser?.uid ?: return
            val avatarRef = storageRef.child("avatars/$userId.jpg")

            avatarRef.putFile(uri)
                .addOnSuccessListener { taskSnapshot ->
                    // Get download URL
                    avatarRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        saveUserDataWithAvatar(firstName, lastName, age, email, phone, downloadUri.toString())
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to upload avatar: ${e.message}", Toast.LENGTH_LONG).show()
                    // Continue registration without avatar
                    saveUserDataWithAvatar(firstName, lastName, age, email, phone, null)
                }
        } ?: run {
            // No avatar selected, continue without avatar
            saveUserDataWithAvatar(firstName, lastName, age, email, phone, null)
        }
    }

    private fun saveUserDataWithAvatar(
        firstName: String,
        lastName: String,
        age: String,
        email: String,
        phone: String,
        avatarUrl: String?
    ) {
        val userId = auth.currentUser?.uid ?: return

        val user = hashMapOf(
            "firstName" to firstName,
            "lastName" to lastName,
            "age" to age,
            "email" to email,
            "phone" to phone,
            "avatarUrl" to (avatarUrl ?: "")
        )

        dbRef.child(userId).setValue(user)
            .addOnSuccessListener {
                Toast.makeText(this, "Registration successful!", Toast.LENGTH_LONG).show()
                // Navigate to MainPage after successful registration
                navigateToMainPage()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Failed to save user data: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun registerUser() {
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
        if (!validateEmail()) {
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
                    val firebaseUser = auth.currentUser
                    val userId = firebaseUser?.uid // Use Firebase Auth UID

                    if (userId != null) {
                        // Upload avatar and save user data
                        uploadAvatarAndRegisterUser(
                            firstName,
                            lastName,
                            age,
                            email,
                            phone,
                            password
                        )
                    }
                } else {
                    val errorMessage = task.exception?.message
                    when {  // ← REPLACE the existing if-else with this when block
                        errorMessage?.contains("The email address is already in use") == true -> {
                            Toast.makeText(
                                this,
                                "Email already registered. Please log in.",
                                Toast.LENGTH_LONG
                            ).show()
                            // Navigate to login page for existing users
                            val intent = Intent(this, MainPage::class.java)
                            startActivity(intent)
                            finish()
                        }

                        errorMessage?.contains("The email address is badly formatted") == true -> {
                            emailLayout.error = "Please enter a valid email format"
                        }

                        else -> {
                            Toast.makeText(
                                this,
                                "Registration failed: $errorMessage",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
    }

    // Navigate to MainPage after successful registration
    private fun navigateToMainPage() {
        // Sign out the user after registration so they need to log in
        auth.signOut()

        val intent = Intent(this, MainPage::class.java)
        startActivity(intent)
        finish()
    }

}