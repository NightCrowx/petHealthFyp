package utar.edu.my.fyp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import utar.edu.my.fyp.petschedule.ui.AddPetActivity

class AddPetNewUser : AppCompatActivity() {

    private lateinit var btnAddPet: Button

    // Register for activity result
    private val addPetLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Pet was successfully added, navigate to Dashboard
            val intent = Intent(this, DashboardPage::class.java)
            startActivity(intent)
            finish()
        }
        // If result is not OK, stay on this screen (user can try adding pet again)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_pet_new_user)

        btnAddPet = findViewById(R.id.add_pet)

        btnAddPet.setOnClickListener {
            val intent = Intent(this, AddPetActivity::class.java)
            addPetLauncher.launch(intent) // Use launcher instead of startActivity
        }
    }
}