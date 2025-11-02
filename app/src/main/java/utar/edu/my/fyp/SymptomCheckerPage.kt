package utar.edu.my.fyp

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Matrix
import androidx.lifecycle.lifecycleScope
import androidx.exifinterface.media.ExifInterface
import com.google.android.material.bottomnavigation.BottomNavigationView
import utar.edu.my.fyp.petschedule.ui.MainActivity
import java.io.InputStream
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class SymptomCheckerPage : AppCompatActivity() {

    private val PICK_IMAGE_REQUEST = 1

    private lateinit var imageView: ImageView
    private lateinit var uploadButton: Button
    private lateinit var analyseButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var loadingText: TextView
    private lateinit var loadingContainer: LinearLayout
    private lateinit var placeholderLayout: LinearLayout
    private lateinit var uploadStatus: View
    private lateinit var aiIcon: ImageView
    private lateinit var tipsButton: ImageButton
    private lateinit var tipsFloatingCard: MaterialCardView
    private lateinit var closeTipsButton: ImageButton
    private lateinit var bottomNav: BottomNavigationView
    private val CAPTURE_IMAGE_REQUEST = 2
    private val CAMERA_PERMISSION_REQUEST = 100
    private var capturedImageUri: Uri? = null

    private var selectedBitmap: Bitmap? = null
    private var selectedImageUri: Uri? = null
    private var isTipsVisible = false

    // Loading text cycler housekeeping
    private val loadingHandler by lazy { Handler(mainLooper) }
    private var loadingRunnable: Runnable? = null


    private val openAiApiKey = "API KEY"
    private val openAiApiUrl = "https://api.openai.com/v1/chat/completions"

    private val client = OkHttpClient.Builder()
        .connectTimeout(90, TimeUnit.SECONDS)  // Increased timeout
        .writeTimeout(90, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)  // Enable retry
        .build()




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_symptom_checker_page)

        initializeViews()
        setupClickListeners()
        startInitialAnimations()

        // Validate API key on startup
        validateApiKey()

        bottomNav = findViewById(R.id.bottomNav)

        // Bottom navigation listener
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, DashboardPage::class.java))
                    true
                }
                R.id.nav_schedule -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                R.id.nav_symptom -> {
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

    private fun validateApiKey() {
        if (openAiApiKey.isEmpty() || openAiApiKey == "your-api-key-here") {
            showToast("OpenAI API key not configured. Please check your configuration.")
            Log.e("SymptomChecker", "Invalid or missing OpenAI API key")
        }
    }

    private fun initializeViews() {
        imageView = findViewById(R.id.imageView)
        uploadButton = findViewById(R.id.uploadBtn)
        analyseButton = findViewById(R.id.analyseBtn)
        progressBar = findViewById(R.id.progressBar)
        loadingText = findViewById(R.id.loadingText)
        loadingContainer = findViewById(R.id.loadingContainer)
        placeholderLayout = findViewById(R.id.placeholderLayout)
        uploadStatus = findViewById(R.id.uploadStatus)
        aiIcon = findViewById(R.id.aiIcon)
        tipsButton = findViewById(R.id.tipsButton)
        tipsFloatingCard = findViewById(R.id.tipsFloatingCard)
        closeTipsButton = findViewById(R.id.closeTipsButton)


        loadingContainer.isVisible = false
        tipsFloatingCard.isVisible = false
        analyseButton.isEnabled = false

        startAiIconAnimation()
    }

    private fun setupClickListeners() {
        uploadButton.setOnClickListener {
            animateButtonPress(it) { showImageSourceDialog() }
        }

        analyseButton.setOnClickListener {
            animateButtonPress(it) {
                val bmp = selectedBitmap
                if (bmp != null) analyzeImageWithOpenAI(bmp)
                else showToast("Please upload an image first")
            }
        }

        tipsButton.setOnClickListener {
            animateButtonPress(it) { toggleTipsVisibility() }
        }

        closeTipsButton.setOnClickListener {
            animateButtonPress(it) { hideTips() }
        }

        // Make placeholder clickable for better UX
        placeholderLayout.setOnClickListener {
            animateButtonPress(it) { showImageSourceDialog() }
        }
    }

    private fun startInitialAnimations() {
        // Only animate views that actually exist/are visible
        val cards = listOfNotNull<View>(
            findViewById(R.id.instructionsCard),
            placeholderLayout
        )

        cards.forEachIndexed { index, card ->
            card.alpha = 0f
            card.translationY = 50f
            card.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(600)
                .setStartDelay((index * 200).toLong())
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    private fun startAiIconAnimation() {
        val pulse = ObjectAnimator.ofFloat(aiIcon, "alpha", 0.3f, 1f, 0.3f).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }
        pulse.start()
    }

    private fun animateButtonPress(view: View, action: () -> Unit) {
        val scaleDown = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.95f),
                ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.95f)
            )
            duration = 100
        }

        val scaleUp = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, "scaleX", 0.95f, 1f),
                ObjectAnimator.ofFloat(view, "scaleY", 0.95f, 1f)
            )
            duration = 100
        }

        scaleDown.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                scaleUp.start()
                action()
            }
        })
        scaleDown.start()
    }

    private fun toggleTipsVisibility() {
        if (isTipsVisible) hideTips() else showTips()
    }

    private fun showTips() {
        if (isTipsVisible) return
        isTipsVisible = true
        tipsFloatingCard.visibility = View.VISIBLE

        tipsFloatingCard.alpha = 0f
        tipsFloatingCard.translationY = -50f
        tipsFloatingCard.scaleX = 0.9f
        tipsFloatingCard.scaleY = 0.9f

        tipsFloatingCard.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(300)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // Auto-hide after 10s
        Handler(mainLooper).postDelayed({ if (isTipsVisible) hideTips() }, 10_000)
    }

    private fun hideTips() {
        if (!isTipsVisible) return
        isTipsVisible = false

        tipsFloatingCard.animate()
            .alpha(0f)
            .translationY(-30f)
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(250)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction { tipsFloatingCard.visibility = View.GONE }
            .start()
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"  // Ensure only images are selectable
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            PICK_IMAGE_REQUEST -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    selectedImageUri = data.data
                    processSelectedImage(selectedImageUri)
                }
            }
            CAPTURE_IMAGE_REQUEST -> {
                if (resultCode == Activity.RESULT_OK) {
                    selectedImageUri = capturedImageUri
                    processSelectedImage(capturedImageUri)
                }
            }
        }
    }

    private fun processSelectedImage(uri: Uri?) {
        try {
            val imageUri = uri ?: throw IllegalStateException("No image URI received")
            Log.d("SymptomChecker", "Processing image URI: $imageUri")

            val bitmap = loadAndProcessBitmap(imageUri)
            if (bitmap != null) {
                selectedBitmap = bitmap

                // Display image
                imageView.setImageBitmap(bitmap)
                imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                placeholderLayout.visibility = View.GONE
                updateUploadStatus(true)
                analyseButton.isEnabled = true

                animateUploadSuccess()
                showToast("Image loaded successfully! Tap Analyze to continue.")
                Log.d("SymptomChecker", "Image loaded successfully. Size: ${bitmap.width}x${bitmap.height}")
            } else {
                throw IOException("Failed to load image from URI")
            }
        } catch (e: Exception) {
            Log.e("SymptomChecker", "Error loading image", e)
            showToast("Error loading image: ${e.localizedMessage}")
            resetImageView()
        }
    }

    // IMPROVED: Better image loading with rotation handling
    private fun loadAndProcessBitmap(uri: Uri): Bitmap? {
        return try {
            // First, get image orientation
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val exif = inputStream?.let { ExifInterface(it) }
            inputStream?.close()

            val orientation = exif?.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            ) ?: ExifInterface.ORIENTATION_NORMAL

            // Load bitmap
            val originalBitmap = contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            } ?: return null

            // Apply rotation if needed
            val rotatedBitmap = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(originalBitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(originalBitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(originalBitmap, 270f)
                else -> originalBitmap
            }

            rotatedBitmap
        } catch (e: Exception) {
            Log.e("SymptomChecker", "Error processing bitmap", e)
            null
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun updateUploadStatus(success: Boolean) {
        val color = if (success) "#10B981" else "#E5E7EB" // Green / Gray
        uploadStatus.backgroundTintList = ColorStateList.valueOf(Color.parseColor(color))
    }

    private fun animateUploadSuccess() {
        val pulseX = ObjectAnimator.ofFloat(uploadStatus, "scaleX", 1f, 1.3f, 1f)
        val pulseY = ObjectAnimator.ofFloat(uploadStatus, "scaleY", 1f, 1.3f, 1f)
        AnimatorSet().apply {
            playTogether(pulseX, pulseY)
            duration = 300
            start()
        }

        analyseButton.animate()
            .alpha(1f)
            .scaleX(1.02f)
            .scaleY(1.02f)
            .setDuration(200)
            .withEndAction {
                analyseButton.animate().scaleX(1f).scaleY(1f).setDuration(200).start()
            }
            .start()
    }

    private fun resetImageView() {
        placeholderLayout.visibility = View.VISIBLE
        updateUploadStatus(false)
        selectedBitmap = null
        selectedImageUri = null
        analyseButton.isEnabled = false
    }

    private fun analyzeImageWithOpenAI(bitmap: Bitmap) {
        showLoadingState(true)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Log.d("SymptomChecker", "Starting image analysis...")

                // IMPROVED: Better base64 conversion with validation
                val base64Image = convertBitmapToBase64(bitmap)
                if (base64Image.isEmpty()) {
                    throw IOException("Failed to convert image to base64")
                }

                Log.d("SymptomChecker", "Base64 conversion completed, calling API...")
                val response = callOpenAIVisionAPI(base64Image)

                if (response.isEmpty()) {
                    throw IOException("Empty response from API")
                }

                withContext(Dispatchers.Main) {
                    Log.d("SymptomChecker", "API response received")
                    showLoadingState(false)
                    handleAnalysisResponse(response)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("SymptomChecker", "Analysis failed: ${e.message}", e)
                    showLoadingState(false)
                    showDetailedError(e)
                }
            }
        }
    }

    private fun showLoadingState(isLoading: Boolean) {
        if (isLoading) {
            loadingContainer.visibility = View.VISIBLE
            loadingContainer.alpha = 0f
            loadingContainer.animate().alpha(1f).setDuration(300).start()
            animateLoadingText()
        } else {
            // Stop text cycler immediately
            loadingRunnable?.let { loadingHandler.removeCallbacks(it) }
            loadingRunnable = null

            loadingContainer.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction { loadingContainer.visibility = View.GONE }
                .start()
        }
        analyseButton.isEnabled = !isLoading
        uploadButton.isEnabled = !isLoading
        if (isLoading) Log.d("SymptomChecker", "Loading state shown")
    }

    private fun animateLoadingText() {
        val texts = arrayOf(
            "AI is analyzing your pet's image...",
            "Scanning for health indicators...",
            "Processing visual data...",
            "Generating health insights..."
        )
        var currentIndex = 0

        // Clear any prior runnable to avoid duplicates
        loadingRunnable?.let { loadingHandler.removeCallbacks(it) }

        loadingRunnable = object : Runnable {
            override fun run() {
                if (loadingContainer.visibility == View.VISIBLE) {
                    loadingText.alpha = 0f
                    loadingText.text = texts[currentIndex % texts.size]
                    loadingText.animate().alpha(1f).setDuration(300).start()
                    currentIndex++
                    loadingHandler.postDelayed(this, 2000)
                }
            }
        }
        loadingHandler.post(loadingRunnable!!)
    }

    // IMPROVED: Better API call with enhanced error handling
    private suspend fun callOpenAIVisionAPI(base64Image: String): String {
        return withContext(Dispatchers.IO) {
            var attempt = 0
            val maxAttempts = 3

            while (attempt < maxAttempts) {
                try {
                    attempt++
                    Log.d("SymptomChecker", "API call attempt $attempt/$maxAttempts")

                    val jsonBody = JSONObject().apply {
                        put("model", "gpt-4o")
                        put("max_tokens", 1500) // Increased token limit
                        put("temperature", 0.3) // Lower temperature for more consistent results
                        put("messages", JSONArray().apply {
                            put(JSONObject().apply {
                                put("role", "user")
                                put("content", JSONArray().apply {
                                    put(JSONObject().apply {
                                        put("type", "text")
                                        put("text", """
                                            You are a veterinary AI assistant. Analyze this pet image and provide a comprehensive health assessment.
                                            
                                            Please respond ONLY with valid JSON in this exact format (no markdown, no extra text):
                                            {
                                                "animal_type": "dog/cat/rabbit/bird/other",
                                                "species": "specific species if identifiable",
                                                "breed": "breed name or 'mixed breed' if unclear",
                                                "health_status": "healthy/unhealthy/unclear",
                                                "diseases": ["list of potential diseases or conditions, or 'No diseases detected' if healthy"],
                                                "recommendations": "clear, actionable health recommendations",
                                                "severity": "low/medium/high/none",
                                                "requires_vet": true/false,
                                                "confidence": "high/medium/low"
                                            }
                                            
                                            Important guidelines:
                                            - Be specific but not alarmist
                                            - If the pet appears healthy, set health_status to "healthy" and diseases to ["No diseases detected"]
                                            - Focus on visible signs and symptoms
                                            - Provide practical recommendations
                                            - Set requires_vet to true for any concerning findings
                                        """.trimIndent())
                                    })
                                    put(JSONObject().apply {
                                        put("type", "image_url")
                                        put("image_url", JSONObject().apply {
                                            put("url", "data:image/jpeg;base64,$base64Image")
                                            put("detail", "high") // Request high detail analysis
                                        })
                                    })
                                })
                            })
                        })
                    }

                    val requestBody = jsonBody.toString()
                        .toRequestBody("application/json".toMediaType())

                    val request = Request.Builder()
                        .url(openAiApiUrl)
                        .addHeader("Authorization", "Bearer $openAiApiKey")
                        .addHeader("Content-Type", "application/json")
                        .addHeader("User-Agent", "PetHealthChecker/1.0")
                        .post(requestBody)
                        .build()

                    Log.d("SymptomChecker", "Sending request to OpenAI... (${base64Image.length} chars)")

                    val response = client.newCall(request).execute()
                    val responseBody = response.body?.string().orEmpty()

                    Log.d("SymptomChecker", "Response code: ${response.code}")
                    Log.d("SymptomChecker", "Response headers: ${response.headers}")

                    if (response.isSuccessful && responseBody.isNotEmpty()) {
                        Log.d("SymptomChecker", "Response body preview: ${responseBody.take(300)}...")

                        val jsonResponse = JSONObject(responseBody)
                        val choices = jsonResponse.optJSONArray("choices")

                        if (choices != null && choices.length() > 0) {
                            val content = choices
                                .getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content")

                            Log.d("SymptomChecker", "Extracted content: ${content.take(200)}...")
                            response.close()
                            return@withContext content
                        } else {
                            response.close()
                            throw IOException("No choices in API response")
                        }
                    } else {
                        val errorMsg = when (response.code) {
                            401 -> "Authentication failed. Check API key and billing status."
                            429 -> "Rate limit exceeded. Please wait and try again."
                            400 -> "Invalid request format. Response: ${responseBody.take(200)}"
                            500, 502, 503, 504 -> "OpenAI server error. Please try again."
                            else -> "API call failed: ${response.code} - ${response.message}. Response: ${responseBody.take(200)}"
                        }

                        response.close()

                        if (response.code == 429 && attempt < maxAttempts) {
                            Log.w("SymptomChecker", "Rate limited, waiting before retry...")
                            delay(2000L * attempt) // Progressive backoff
                            continue
                        }

                        throw IOException(errorMsg)
                    }
                } catch (e: Exception) {
                    Log.e("SymptomChecker", "API call attempt $attempt failed", e)

                    if (attempt >= maxAttempts) {
                        throw e
                    }

                    // Wait before retry
                    delay(1000L * attempt)
                }
            }

            throw IOException("All API call attempts failed")
        }
    }

    // IMPROVED: Better base64 conversion with optimization
    private fun convertBitmapToBase64(bitmap: Bitmap): String {
        return try {
            // Optimize image size for API
            val maxSize = 1024 // Increased max size for better quality
            val width = bitmap.width
            val height = bitmap.height

            Log.d("SymptomChecker", "Original bitmap size: ${width}x${height}")

            val ratio = if (width > height) {
                maxSize.toFloat() / width
            } else {
                maxSize.toFloat() / height
            }

            val newWidth = (width * ratio).toInt().coerceAtLeast(1)
            val newHeight = (height * ratio).toInt().coerceAtLeast(1)

            val resizedBitmap = if (ratio < 1.0f) {
                Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
            } else {
                bitmap // Don't upscale small images
            }

            Log.d("SymptomChecker", "Resized bitmap size: ${resizedBitmap.width}x${resizedBitmap.height}")

            val baos = ByteArrayOutputStream()
            val quality = if (resizedBitmap.width * resizedBitmap.height > 500000) 70 else 85
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)

            val byteArray = baos.toByteArray()
            val base64 = Base64.encodeToString(byteArray, Base64.NO_WRAP)

            Log.d("SymptomChecker", "Final image size: ${byteArray.size} bytes, Base64 length: ${base64.length}")

            // Validate base64
            if (base64.isEmpty()) {
                throw IOException("Base64 encoding failed - empty result")
            }

            base64
        } catch (e: Exception) {
            Log.e("SymptomChecker", "Error converting bitmap to base64", e)
            ""
        }
    }

    // IMPROVED: Better response handling with multiple parsing strategies
    private fun handleAnalysisResponse(response: String) {
        try {
            Log.d("SymptomChecker", "Processing response: ${response.take(200)}...")

            // Strategy 1: Try to find JSON within the response
            var jsonString = extractJson(response)

            // Strategy 2: If no JSON found, try the whole response
            if (jsonString.isEmpty()) {
                jsonString = response.trim()
            }

            // Strategy 3: Validate and parse JSON
            if (jsonString.isNotEmpty() && (jsonString.startsWith("{") || jsonString.contains("{"))) {
                try {
                    // Clean up the JSON string
                    val cleanJson = cleanJsonString(jsonString)
                    Log.d("SymptomChecker", "Attempting to parse JSON: ${cleanJson.take(300)}...")

                    // Validate JSON structure
                    val analysisResult = JSONObject(cleanJson)

                    // Check if required fields exist
                    if (analysisResult.has("health_status") || analysisResult.has("animal_type")) {
                        // Navigate to results page with analysis data
                        val intent = Intent(this, DiagnosisResultPage::class.java).apply {
                            putExtra("analysis_result", cleanJson)
                            putExtra("image_uri", selectedImageUri?.toString().orEmpty())
                        }
                        startActivity(intent)
                        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                        return
                    }
                } catch (jsonException: Exception) {
                    Log.w("SymptomChecker", "JSON parsing failed", jsonException)
                }
            }

            // Fallback: Use raw response
            Log.w("SymptomChecker", "Using raw response as fallback")
            val intent = Intent(this, DiagnosisResultPage::class.java).apply {
                putExtra("raw_response", response)
                putExtra("image_uri", selectedImageUri?.toString().orEmpty())
            }
            startActivity(intent)
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)

        } catch (e: Exception) {
            Log.e("SymptomChecker", "Error processing response", e)
            showToast("Error processing results: ${e.localizedMessage}")
        }
    }

    private fun extractJson(response: String): String {
        return try {
            // Remove markdown code blocks
            var cleaned = response.replace("```json", "").replace("```", "")

            // Find JSON boundaries
            val jsonStart = cleaned.indexOf('{')
            val jsonEnd = cleaned.lastIndexOf('}') + 1

            if (jsonStart != -1 && jsonEnd > jsonStart && jsonEnd <= cleaned.length) {
                cleaned.substring(jsonStart, jsonEnd)
            } else {
                ""
            }
        } catch (e: Exception) {
            Log.w("SymptomChecker", "Error extracting JSON", e)
            ""
        }
    }

    private fun cleanJsonString(json: String): String {
        return json.trim()
            .replace("\n", "")
            .replace("\r", "")
            .replace("\\", "")
            .replace("\"\"", "\"")
    }

    private fun showDetailedError(exception: Exception) {
        val errorMessage = when {
            exception.message?.contains("401") == true ->
                "Authentication Error\n\nPlease check:\n• Your OpenAI API key is correct\n• You have credits in your OpenAI account\n• Your API key has proper permissions"
            exception.message?.contains("429") == true ->
                "Rate limit exceeded\n\nPlease wait a moment and try again."
            exception.message?.contains("400") == true ->
                "Invalid request\n\nPlease try with a different image."
            exception.message?.contains("Empty response") == true ->
                "No response received\n\nPlease check your internet connection and try again."
            exception.message?.contains("base64") == true ->
                "Image processing error\n\nPlease try with a different image or check if the image is corrupted."
            else ->
                "Analysis failed\n\n${exception.localizedMessage ?: "Unknown error occurred"}"
        }

        AlertDialog.Builder(this)
            .setTitle("Analysis Error")
            .setMessage(errorMessage)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .setNegativeButton("Retry") { dialog, _ ->
                dialog.dismiss()
                selectedBitmap?.let { analyzeImageWithOpenAI(it) }
            }
            .show()
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery")

        AlertDialog.Builder(this)
            .setTitle("Select Image Source")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> {
                        if (checkCameraPermission()) {
                            openCamera()
                        } else {
                            requestCameraPermission()
                        }
                    }
                    1 -> openImagePicker()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST
        )
    }

    private fun openCamera() {
        try {
            val photoFile = createImageFile()
            capturedImageUri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                photoFile
            )

            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, capturedImageUri)
            startActivityForResult(intent, CAPTURE_IMAGE_REQUEST)
        } catch (e: Exception) {
            Log.e("SymptomChecker", "Error opening camera", e)
            showToast("Error opening camera: ${e.localizedMessage}")
        }
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "PET_${timeStamp}_"
        val storageDir = getExternalFilesDir("Pictures")
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            CAMERA_PERMISSION_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamera()
                } else {
                    showToast("Camera permission is required to take photos")
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up loading text cycler to avoid leaks
        loadingRunnable?.let { loadingHandler.removeCallbacks(it) }
        loadingRunnable = null
    }
}