package utar.edu.my.fyp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView

class DashboardPage : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard_page)


        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)


        bottomNav.selectedItemId = R.id.nav_home


        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, DashboardPage::class.java))
                    true
                }
                R.id.nav_chatbot -> {
                    startActivity(Intent(this, AiChatbotPage::class.java))
                    true
                }

                R.id.nav_symptom -> {
                    startActivity(Intent(this, SymptomCheckerPage::class.java))
                    true
                }

                R.id.nav_navigation -> {
                    startActivity(Intent(this, MapsActivity::class.java))
                    true
                }

                else -> false
            }
        }
    }
}

