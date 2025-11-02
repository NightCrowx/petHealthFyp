package utar.edu.my.fyp

import android.app.Activity
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
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import utar.edu.my.fyp.petschedule.adapters.UserSessionManager

class EditUserProfileActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var etFirstName: TextInputEditText
    private lateinit var etLastName: TextInputEditText
    private lateinit var etAge: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etReenterPassword: TextInputEditText
    private lateinit var btnUpdateProfile: Button
    private lateinit var ivAvatar: ImageView
    private lateinit var btnSelectAvatar: Button

    // Password layout components
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var reenterPasswordLayout: TextInputLayout

    private var selectedImageUri: Uri? = null
    private var currentAvatarUrl: String? = null
    private var currentEmail: String? = null

    private lateinit var auth: FirebaseAuth
    private lateinit var dbRef: DatabaseReference
    private lateinit var storageRef: StorageReference

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
        setContentView(R.layout.activity_edit_profile)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        dbRef = FirebaseDatabase.getInstance().getReference("user")
        storageRef = FirebaseStorage.getInstance().reference

        // Initialize toolbar
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "Edit Profile"

        // Initialize UI components
        initializeViews()

        // Setup password toggle functionality
        setupPasswordToggle()

        // Load current user data
        loadCurrentUserData()

        // Handle avatar selection
        btnSelectAvatar.setOnClickListener {
            selectAvatarImage()
        }

        // Handle update profile button click
        btnUpdateProfile.setOnClickListener {
            updateUserProfile()
        }

        // Handle back button
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun initializeViews() {
        etFirstName = findViewById(R.id.et_firstname)
        etLastName = findViewById(R.id.et_lastname)
        etAge = findViewById(R.id.et_age)
        etEmail = findViewById(R.id.et_email)
        etPhone = findViewById(R.id.et_phone)
        etPassword = findViewById(R.id.et_password)
        etReenterPassword = findViewById(R.id.et_reenter_password)
        btnUpdateProfile = findViewById(R.id.btn_update_profile)
        ivAvatar = findViewById(R.id.iv_avatar)
        btnSelectAvatar = findViewById(R.id.btn_select_avatar)
        passwordLayout = findViewById(R.id.password_layout)
        reenterPasswordLayout = findViewById(R.id.reenter_password_layout)
        etEmail.isEnabled = false
    }

    private fun selectAvatarImage() {
        imagePickerLauncher.launch("image/*")
    }

    private fun setupPasswordToggle() {
        // Handle password field toggle
        passwordLayout.setEndIconOnClickListener {
            val isPasswordVisible = etPassword.inputType == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            if (isPasswordVisible) {
                etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            } else {
                etPassword.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            }
            etPassword.setSelection(etPassword.text?.length ?: 0)
        }

        // Handle re-enter password field toggle
        reenterPasswordLayout.setEndIconOnClickListener {
            val isPasswordVisible = etReenterPassword.inputType == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            if (isPasswordVisible) {
                etReenterPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            } else {
                etReenterPassword.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            }
            etReenterPassword.setSelection(etReenterPassword.text?.length ?: 0)
        }
    }

    private fun loadCurrentUserData() {
        val userId = UserSessionManager.getCurrentUserId(this)
        if (userId == null) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        dbRef.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val firstName = snapshot.child("firstName").getValue(String::class.java) ?: ""
                    val lastName = snapshot.child("lastName").getValue(String::class.java) ?: ""
                    val age = snapshot.child("age").getValue(String::class.java) ?: ""
                    val email = snapshot.child("email").getValue(String::class.java) ?: ""
                    val phone = snapshot.child("phone").getValue(String::class.java) ?: ""
                    currentAvatarUrl = snapshot.child("avatarUrl").getValue(String::class.java)

                    // Populate fields
                    etFirstName.setText(firstName)
                    etLastName.setText(lastName)
                    etAge.setText(age)
                    etEmail.setText(email)
                    etPhone.setText(phone)

                    // Load avatar if available
                    if (!currentAvatarUrl.isNullOrEmpty()) {
                        Glide.with(this@EditUserProfileActivity)
                            .load(currentAvatarUrl)
                            .placeholder(R.drawable.icuser)
                            .error(R.drawable.icuser)
                            .into(ivAvatar)
                    }
                } else {
                    Toast.makeText(this@EditUserProfileActivity, "User data not found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@EditUserProfileActivity, "Failed to load user data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateUserProfile() {
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

        if (TextUtils.isEmpty(phone) || !phone.matches(Regex("^\\d{10,15}$"))) {
            etPhone.error = "Enter a valid phone number"
            return
        }

        // Password validation only if password fields are filled
        if (!TextUtils.isEmpty(password)) {
            if (password.length < 6) {
                etPassword.error = "Password must be at least 6 characters"
                return
            }
            if (password != reenterPassword) {
                etReenterPassword.error = "Passwords do not match"
                return
            }
        }

        // Update profile (with or without password change)
        if (!TextUtils.isEmpty(password)) {
            updatePasswordAndProfile(firstName, lastName, age, email, phone, password)
        } else {
            updateProfileOnly(firstName, lastName, age, email, phone)
        }
    }

    private fun updatePasswordAndProfile(
        firstName: String,
        lastName: String,
        age: String,
        email: String,
        phone: String,
        password: String
    ) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            currentUser.updatePassword(password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        updateProfileOnly(firstName, lastName, age, email, phone)
                    } else {
                        Toast.makeText(this, "Failed to update password: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }

    private fun updateProfileOnly(
        firstName: String,
        lastName: String,
        age: String,
        email: String,
        phone: String
    ) {
        selectedImageUri?.let { uri ->
            uploadAvatarAndUpdateProfile(firstName, lastName, age, email, phone, uri)
        } ?: run {
            saveUserDataWithAvatar(firstName, lastName, age, email, phone, currentAvatarUrl)
        }
    }

    private fun uploadAvatarAndUpdateProfile(
        firstName: String,
        lastName: String,
        age: String,
        email: String,
        phone: String,
        imageUri: Uri
    ) {
        val userId = UserSessionManager.getCurrentUserId(this) ?: return
        val avatarRef = storageRef.child("avatars/$userId.jpg")

        avatarRef.putFile(imageUri)
            .addOnSuccessListener { taskSnapshot ->
                avatarRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    saveUserDataWithAvatar(firstName, lastName, age, email, phone, downloadUri.toString())
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to upload avatar: ${e.message}", Toast.LENGTH_LONG).show()
                // Continue update without new avatar
                saveUserDataWithAvatar(firstName, lastName, age, email, phone, currentAvatarUrl)
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
        val userId = UserSessionManager.getCurrentUserId(this) ?: return

        val user = hashMapOf(
            "firstName" to firstName,
            "lastName" to lastName,
            "age" to age,
            "email" to email,
            "phone" to phone,
            "avatarUrl" to (avatarUrl ?: "")
        )

        dbRef.child(userId).updateChildren(user as Map<String, Any>)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_LONG).show()
                setResult(Activity.RESULT_OK)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Failed to update profile: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}