package utar.edu.my.fyp

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.material.card.MaterialCardView
import org.json.JSONArray
import org.json.JSONObject

class DiagnosisResultPage : AppCompatActivity() {

    private lateinit var petImageView: ImageView
    private lateinit var animalTypeText: TextView
    private lateinit var speciesText: TextView
    private lateinit var breedText: TextView
    private lateinit var healthStatusCard: MaterialCardView
    private lateinit var healthStatusText: TextView
    private lateinit var diseasesLayout: LinearLayout
    private lateinit var recommendationsText: TextView
    private lateinit var severityCard: MaterialCardView
    private lateinit var severityText: TextView
    private lateinit var backButton: Button
    private lateinit var findClinicButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diagnosis_result_page)

        initializeViews()
        setupAnimations()
        processAnalysisResults()
        setupClickListeners()
    }

    private fun initializeViews() {
        petImageView = findViewById(R.id.petImageView)
        animalTypeText = findViewById(R.id.animalTypeText)
        speciesText = findViewById(R.id.speciesText)
        breedText = findViewById(R.id.breedText)
        healthStatusCard = findViewById(R.id.healthStatusCard)
        healthStatusText = findViewById(R.id.healthStatusText)
        diseasesLayout = findViewById(R.id.diseasesLayout)
        recommendationsText = findViewById(R.id.recommendationsText)
        severityCard = findViewById(R.id.severityCard)
        severityText = findViewById(R.id.severityText)
        backButton = findViewById(R.id.backButton)
        findClinicButton = findViewById(R.id.findClinicButton)
    }

    private fun setupAnimations() {
        // Initial animation setup - cards start invisible and animate in
        val cards = listOf(
            findViewById<MaterialCardView>(R.id.healthStatusCard),
            findViewById<MaterialCardView>(R.id.severityCard),
            // Add other card views as needed
        )

        // Set initial state
        cards.forEach { card ->
            card.alpha = 0f
            card.translationY = 100f
        }

        // Animate cards in sequence
        Handler(Looper.getMainLooper()).postDelayed({
            animateCardsIn(cards)
        }, 300)
    }

    private fun animateCardsIn(cards: List<View>) {
        cards.forEachIndexed { index, card ->
            Handler(Looper.getMainLooper()).postDelayed({
                val fadeIn = ObjectAnimator.ofFloat(card, "alpha", 0f, 1f)
                val slideUp = ObjectAnimator.ofFloat(card, "translationY", 100f, 0f)
                val scale = ObjectAnimator.ofFloat(card, "scaleX", 0.8f, 1f)
                val scaleY = ObjectAnimator.ofFloat(card, "scaleY", 0.8f, 1f)

                val animatorSet = AnimatorSet()
                animatorSet.playTogether(fadeIn, slideUp, scale, scaleY)
                animatorSet.duration = 500
                animatorSet.interpolator = OvershootInterpolator(0.8f)
                animatorSet.start()
            }, index * 150L)
        }
    }

    private fun processAnalysisResults() {
        val analysisResult = intent.getStringExtra("analysis_result")
        val rawResponse = intent.getStringExtra("raw_response")
        val imageUri = intent.getStringExtra("image_uri")

        // Display the uploaded image with proper scaling and animation
        imageUri?.let {
            try {
                val uri = Uri.parse(it)
                petImageView.setImageURI(uri)
                petImageView.scaleType = ImageView.ScaleType.CENTER_CROP

                // Add subtle animation to image
                animateImageLoad()

                Log.d("DiagnosisResult", "Image loaded successfully from URI: $it")
            } catch (e: Exception) {
                Log.e("DiagnosisResult", "Error loading image from URI: $it", e)
                petImageView.setImageResource(android.R.drawable.ic_menu_camera)
            }
        }

        try {
            if (analysisResult != null) {
                parseAndDisplayResults(JSONObject(analysisResult))
            } else if (rawResponse != null) {
                displayRawResponse(rawResponse)
            } else {
                showError("No analysis results received")
            }
        } catch (e: Exception) {
            Log.e("DiagnosisResult", "Error parsing results", e)
            rawResponse?.let { displayRawResponse(it) } ?: showError("Error processing results")
        }
    }

    private fun animateImageLoad() {
        petImageView.alpha = 0f
        petImageView.scaleX = 1.1f
        petImageView.scaleY = 1.1f

        val fadeIn = ObjectAnimator.ofFloat(petImageView, "alpha", 0f, 1f)
        val scaleDownX = ObjectAnimator.ofFloat(petImageView, "scaleX", 1.1f, 1f)
        val scaleDownY = ObjectAnimator.ofFloat(petImageView, "scaleY", 1.1f, 1f)

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(fadeIn, scaleDownX, scaleDownY)
        animatorSet.duration = 800
        animatorSet.interpolator = AccelerateDecelerateInterpolator()
        animatorSet.start()
    }

    private fun parseAndDisplayResults(result: JSONObject) {
        try {
            // Animal classification with enhanced animation
            val animalType = result.optString("animal_type", "Unknown")
            val species = result.optString("species", "Not identified")
            val breed = result.optString("breed", "Not identified")

            animateTextChange(animalTypeText, "Animal: ${animalType.capitalize()}")
            animateTextChange(speciesText, "Species: $species", 200)
            animateTextChange(breedText, "Breed: $breed", 400)

            // Health status with enhanced visual feedback
            val healthStatus = result.optString("health_status", "unknown")
            animateHealthStatus(healthStatus)

            // Diseases with enhanced chip animations
            displayDiseases(result.optJSONArray("diseases"))

            // Recommendations
            val recommendations = result.optString("recommendations", "No specific recommendations")
            animateTextChange(recommendationsText, recommendations, 600)

            // Severity and vet requirement
            val severity = result.optString("severity", "")
            val requiresVet = result.optBoolean("requires_vet", false)

            if (healthStatus.lowercase() == "unhealthy" && severity.isNotEmpty()) {
                animateSeverityCard(severity)
            } else {
                severityCard.isVisible = false
            }

            // Show clinic alert if needed
            if (requiresVet || healthStatus.lowercase() == "unhealthy") {
                Handler(Looper.getMainLooper()).postDelayed({
                    showEnhancedClinicAlert(severity)
                }, 1000)
            }

            // Show/hide find clinic button with animation
            animateFindClinicButton(healthStatus.lowercase() == "unhealthy")

        } catch (e: Exception) {
            Log.e("DiagnosisResult", "Error parsing JSON result", e)
            showError("Error displaying results")
        }
    }

    private fun animateTextChange(textView: TextView, newText: String, delay: Long = 0) {
        Handler(Looper.getMainLooper()).postDelayed({
            val fadeOut = ObjectAnimator.ofFloat(textView, "alpha", 1f, 0f)
            fadeOut.duration = 200

            fadeOut.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    textView.text = newText
                    val fadeIn = ObjectAnimator.ofFloat(textView, "alpha", 0f, 1f)
                    fadeIn.duration = 300
                    fadeIn.start()
                }
            })

            fadeOut.start()
        }, delay)
    }

    private fun animateHealthStatus(healthStatus: String) {
        val statusText = healthStatus.capitalize()

        // Get the appropriate color and drawable based on health status
        val (cardColor, backgroundDrawable) = when (healthStatus.lowercase()) {
            "healthy" -> Pair(
                getColor(R.color.healthy_green),
                ContextCompat.getDrawable(this, R.drawable.health_status_gradient)
            )
            "unhealthy" -> Pair(
                getColor(R.color.unhealthy_red),
                ContextCompat.getDrawable(this, R.drawable.health_status_unhealthy_gradient)
            )
            else -> Pair(
                getColor(R.color.unknown_gray),
                ContextCompat.getDrawable(this, R.drawable.health_status_unknown_gradient)
            )
        }

        // First, set the card background color directly
        healthStatusCard.setCardBackgroundColor(cardColor)

        // If you have gradient drawables, apply them after a short delay
        Handler(Looper.getMainLooper()).postDelayed({
            backgroundDrawable?.let {
                healthStatusCard.background = it
            }
        }, 100)

        val gradientDrawable = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(
                cardColor,
                adjustBrightness(cardColor, 0.8f)
            )
        )
        gradientDrawable.cornerRadius = 60f // 20dp * 3 for corner radius
        healthStatusCard.background = gradientDrawable

        // Pulse animation for health status
        val pulse = ObjectAnimator.ofFloat(healthStatusCard, "scaleX", 1f, 1.05f, 1f)
        val pulseY = ObjectAnimator.ofFloat(healthStatusCard, "scaleY", 1f, 1.05f, 1f)

        val pulseSet = AnimatorSet()
        pulseSet.playTogether(pulse, pulseY)
        pulseSet.duration = 1000
        pulseSet.interpolator = AccelerateDecelerateInterpolator()

        // Text change animation
        Handler(Looper.getMainLooper()).postDelayed({
            animateTextChange(healthStatusText, statusText)
        }, 400)

        pulseSet.start()
    }

    // Helper function to adjust brightness for gradient effect
    private fun adjustBrightness(color: Int, factor: Float): Int {
        val r = (android.graphics.Color.red(color) * factor).toInt().coerceIn(0, 255)
        val g = (android.graphics.Color.green(color) * factor).toInt().coerceIn(0, 255)
        val b = (android.graphics.Color.blue(color) * factor).toInt().coerceIn(0, 255)
        return android.graphics.Color.rgb(r, g, b)
    }

    private fun animateSeverityCard(severity: String) {
        severityCard.isVisible = true
        severityCard.alpha = 0f
        severityCard.translationY = 50f

        val backgroundDrawable = when (severity.lowercase()) {
            "low" -> ContextCompat.getDrawable(this, R.drawable.severity_low_gradient)
            "medium" -> ContextCompat.getDrawable(this, R.drawable.severity_gradient)
            "high" -> ContextCompat.getDrawable(this, R.drawable.severity_high_gradient)
            else -> ContextCompat.getDrawable(this, R.drawable.severity_gradient)
        }

        severityCard.background = backgroundDrawable

        val fadeIn = ObjectAnimator.ofFloat(severityCard, "alpha", 0f, 1f)
        val slideUp = ObjectAnimator.ofFloat(severityCard, "translationY", 50f, 0f)

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(fadeIn, slideUp)
        animatorSet.duration = 500
        animatorSet.interpolator = OvershootInterpolator(0.5f)
        animatorSet.start()

        Handler(Looper.getMainLooper()).postDelayed({
            animateTextChange(severityText, "Severity: ${severity.capitalize()}")
        }, 250)
    }

    private fun animateFindClinicButton(shouldShow: Boolean) {
        if (shouldShow) {
            findClinicButton.isVisible = true
            findClinicButton.alpha = 0f
            findClinicButton.scaleX = 0.8f
            findClinicButton.scaleY = 0.8f

            Handler(Looper.getMainLooper()).postDelayed({
                val fadeIn = ObjectAnimator.ofFloat(findClinicButton, "alpha", 0f, 1f)
                val scaleUpX = ObjectAnimator.ofFloat(findClinicButton, "scaleX", 0.8f, 1f)
                val scaleUpY = ObjectAnimator.ofFloat(findClinicButton, "scaleY", 0.8f, 1f)

                val animatorSet = AnimatorSet()
                animatorSet.playTogether(fadeIn, scaleUpX, scaleUpY)
                animatorSet.duration = 400
                animatorSet.interpolator = OvershootInterpolator(0.5f)
                animatorSet.start()
            }, 800)
        } else {
            findClinicButton.isVisible = false
        }
    }

    private fun displayDiseases(diseasesArray: JSONArray?) {
        diseasesLayout.removeAllViews()

        if (diseasesArray != null && diseasesArray.length() > 0) {
            var hasRealDiseases = false
            for (i in 0 until diseasesArray.length()) {
                val disease = diseasesArray.getString(i)
                if (disease != "No diseases detected") {
                    addEnhancedDiseaseChip(disease, i * 100L)
                    hasRealDiseases = true
                }
            }

            if (!hasRealDiseases) {
                addNoDiseasesView()
            }
        } else {
            addNoDiseasesView()
        }
    }

    private fun addEnhancedDiseaseChip(disease: String, delay: Long = 0) {
        val chip = TextView(this).apply {
            text = disease
            background = ContextCompat.getDrawable(this@DiagnosisResultPage, R.drawable.disease_chip_background_enhanced)
            setTextColor(getColor(android.R.color.white))
            textSize = 14f
            setPadding(32, 16, 32, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(8, 8, 8, 8)
            }

            // Add shadow effect
            elevation = 8f

            // Initial state for animation
            alpha = 0f
            scaleX = 0.8f
            scaleY = 0.8f
        }

        diseasesLayout.addView(chip)

        // Animate chip appearance
        Handler(Looper.getMainLooper()).postDelayed({
            val fadeIn = ObjectAnimator.ofFloat(chip, "alpha", 0f, 1f)
            val scaleUpX = ObjectAnimator.ofFloat(chip, "scaleX", 0.8f, 1f)
            val scaleUpY = ObjectAnimator.ofFloat(chip, "scaleY", 0.8f, 1f)

            val animatorSet = AnimatorSet()
            animatorSet.playTogether(fadeIn, scaleUpX, scaleUpY)
            animatorSet.duration = 300
            animatorSet.interpolator = OvershootInterpolator(0.3f)
            animatorSet.start()
        }, delay)
    }

    private fun addNoDiseasesView() {
        val noDiseasesView = TextView(this).apply {
            text = " No diseases detected"
            background = ContextCompat.getDrawable(this@DiagnosisResultPage, R.drawable.no_disease_background)
            setTextColor(getColor(android.R.color.white))
            textSize = 16f
            setPadding(32, 24, 32, 24)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            gravity = android.view.Gravity.CENTER
            elevation = 4f

            // Initial state for animation
            alpha = 0f
            scaleX = 0.9f
            scaleY = 0.9f
        }

        diseasesLayout.addView(noDiseasesView)

        // Animate appearance
        Handler(Looper.getMainLooper()).postDelayed({
            val fadeIn = ObjectAnimator.ofFloat(noDiseasesView, "alpha", 0f, 1f)
            val scaleUpX = ObjectAnimator.ofFloat(noDiseasesView, "scaleX", 0.9f, 1f)
            val scaleUpY = ObjectAnimator.ofFloat(noDiseasesView, "scaleY", 0.9f, 1f)

            val animatorSet = AnimatorSet()
            animatorSet.playTogether(fadeIn, scaleUpX, scaleUpY)
            animatorSet.duration = 500
            animatorSet.interpolator = OvershootInterpolator(0.2f)
            animatorSet.start()
        }, 300)
    }

    private fun displayRawResponse(response: String) {
        // Fallback display for raw response with animation
        animateTextChange(animalTypeText, "Analysis Complete")
        animateTextChange(speciesText, "", 100)
        animateTextChange(breedText, "", 200)
        animateTextChange(healthStatusText, "See recommendations below", 300)
        animateTextChange(recommendationsText, response, 400)

        // Show find clinic button as precaution
        animateFindClinicButton(true)
    }

    private fun showError(message: String) {
        animateTextChange(animalTypeText, "Error")
        animateTextChange(speciesText, "", 100)
        animateTextChange(breedText, "", 200)
        animateTextChange(healthStatusText, "Analysis Failed", 300)
        animateTextChange(recommendationsText, message, 400)

        healthStatusCard.background = ContextCompat.getDrawable(this, R.drawable.health_status_unknown_gradient)
    }

    private fun showEnhancedClinicAlert(severity: String) {
        val alertTitle = when (severity.lowercase()) {
            "high" -> "⚠️ Immediate Veterinary Attention Recommended"
            "medium" -> "🏥 Veterinary Consultation Recommended"
            "low" -> "💡 Consider Veterinary Consultation"
            else -> "🏥 Veterinary Consultation Recommended"
        }

        val alertMessage = when (severity.lowercase()) {
            "high" -> "Your pet may require immediate medical attention. Would you like to find nearby veterinary clinics?"
            "medium" -> "Your pet may need professional medical evaluation. Would you like to locate nearby veterinary clinics?"
            "low" -> "While not urgent, it would be beneficial to consult with a veterinarian. Would you like to find nearby clinics?"
            else -> "Based on the analysis, your pet may benefit from veterinary consultation. Would you like to find nearby clinics?"
        }

        val alertDialog = AlertDialog.Builder(this)
            .setTitle(alertTitle)
            .setMessage(alertMessage)
            .setPositiveButton("Find Nearby Clinics") { _, _ ->
                // Add button press animation
                animateButtonPress(findClinicButton) {
                    navigateToNearbyClinic()
                }
            }
            .setNegativeButton("Not Now") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .create()

        alertDialog.show()

        // Style the alert dialog buttons
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
            setBackgroundColor(ContextCompat.getColor(this@DiagnosisResultPage, R.color.unhealthy_red))
            setTextColor(ContextCompat.getColor(this@DiagnosisResultPage, android.R.color.white))
        }
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener {
            animateButtonPress(backButton) {
                finish()
            }
        }

        findClinicButton.setOnClickListener {
            animateButtonPress(findClinicButton) {
                navigateToNearbyClinic()
            }
        }

        // Add interactive touch feedback to cards
        val cards = listOf(healthStatusCard, severityCard)
        cards.forEach { card ->
            card.setOnClickListener {
                animateCardPress(card)
            }
        }
    }

    private fun animateButtonPress(button: View, onComplete: () -> Unit) {
        val scaleDown = ObjectAnimator.ofFloat(button, "scaleX", 1f, 0.95f)
        val scaleDownY = ObjectAnimator.ofFloat(button, "scaleY", 1f, 0.95f)
        val scaleUp = ObjectAnimator.ofFloat(button, "scaleX", 0.95f, 1f)
        val scaleUpY = ObjectAnimator.ofFloat(button, "scaleY", 0.95f, 1f)

        val downSet = AnimatorSet()
        downSet.playTogether(scaleDown, scaleDownY)
        downSet.duration = 100

        val upSet = AnimatorSet()
        upSet.playTogether(scaleUp, scaleUpY)
        upSet.duration = 100

        downSet.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                upSet.start()
            }
        })

        upSet.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                onComplete()
            }
        })

        downSet.start()
    }

    private fun animateCardPress(card: View) {
        val pulse = ObjectAnimator.ofFloat(card, "scaleX", 1f, 1.02f, 1f)
        val pulseY = ObjectAnimator.ofFloat(card, "scaleY", 1f, 1.02f, 1f)
        val elevation = ObjectAnimator.ofFloat(card, "elevation", card.elevation, card.elevation + 4f, card.elevation)

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(pulse, pulseY, elevation)
        animatorSet.duration = 200
        animatorSet.interpolator = AccelerateDecelerateInterpolator()
        animatorSet.start()
    }

    private fun navigateToNearbyClinic() {
        try {
            // Navigate to your existing MapsActivity
            val intent = Intent(this, MapsActivity::class.java)
            intent.putExtra("search_query", "veterinary clinic")
            intent.putExtra("from_diagnosis", true)
            startActivity(intent)

            // Add transition animation
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        } catch (e: Exception) {
            // Fallback: Open Google Maps with veterinary search
            try {
                val geoUri = Uri.parse("geo:0,0?q=veterinary+clinic+near+me")
                val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
                mapIntent.setPackage("com.google.android.apps.maps")
                startActivity(mapIntent)
            } catch (ex: Exception) {
                // Show animated toast
                showAnimatedToast("Unable to open maps. Please search for veterinary clinics manually.")
            }
        }
    }

    private fun showAnimatedToast(message: String) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        } else {
            runOnUiThread {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun finish() {
        super.finish()
        // Add custom exit transition
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }
}