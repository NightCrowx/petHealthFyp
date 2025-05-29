package utar.edu.my.fyp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class AddPetNewUser : AppCompatActivity() {

    private lateinit var btnAddPet: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_pet_new_user)


        btnAddPet = findViewById(R.id.add_pet)


        btnAddPet.setOnClickListener {
            val intent = Intent(this, RegisterPetProfilePage::class.java)
            startActivity(intent)
        }
    }
}
