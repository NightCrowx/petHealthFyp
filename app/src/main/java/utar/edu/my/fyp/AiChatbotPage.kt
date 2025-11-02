package utar.edu.my.fyp

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import utar.edu.my.fyp.petschedule.adapters.UserSessionManager
import androidx.cardview.widget.CardView

class AiChatbotPage : AppCompatActivity() {

    private lateinit var messageContainer: LinearLayout
    private lateinit var userInput: EditText
    private lateinit var sendBtn: Button
    private lateinit var scrollView: ScrollView
    private lateinit var imageButton: ImageButton
    private lateinit var attachedImagePreview: ImageView
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var loadingCard: CardView
    private lateinit var auth: FirebaseAuth
    private lateinit var closeChatBtn: ImageButton

    // Quick education buttons
    private lateinit var quickEducationContainer: HorizontalScrollView

    private var base64Image: String? = null
    private var selectedBitmap: Bitmap? = null
    private var userAvatarUrl: String? = null // Store user avatar URL
    private val IMAGE_PICK_CODE = 1001
    private val CAMERA_CODE = 1002

    companion object {
        private const val TAG = "AiChatbotPage"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai_chatbot_page)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        messageContainer = findViewById(R.id.messageContainer)
        userInput = findViewById(R.id.userInput)
        sendBtn = findViewById(R.id.sendBtn)
        scrollView = findViewById(R.id.scrollView)
        imageButton = findViewById(R.id.imageSelectBtn)
        loadingIndicator = findViewById(R.id.loadingIndicator)
        loadingCard = findViewById(R.id.loadingCard)
        attachedImagePreview = findViewById(R.id.attachedImagePreview)
        quickEducationContainer = findViewById(R.id.quickEducationContainer)
        closeChatBtn = findViewById(R.id.closeChatBtn)

        // Setup quick education buttons
        setupQuickEducationButtons()

        // Load user avatar when activity starts
        loadUserAvatar()

        addMessageBubble("Hello! I'm Doctor Paw, your PawLife AI assistant. How can I help you and your pet today?", isUser = false, null)

        val removeImageBtn = findViewById<ImageButton>(R.id.removeImageBtn)
        val imagePreviewContainer = findViewById<CardView>(R.id.imagePreviewContainer)

        removeImageBtn.setOnClickListener {
            selectedBitmap = null
            attachedImagePreview.setImageBitmap(null)
            imagePreviewContainer.visibility = View.GONE
        }

        sendBtn.setOnClickListener {
            val message = userInput.text.toString().trim()
            if (message.isNotEmpty() || selectedBitmap != null) {
                addMessageBubble(message, isUser = true, image = selectedBitmap)
                callChatGPTWithImage(message)
                userInput.text.clear()
                selectedBitmap = null
                base64Image = null
                attachedImagePreview.setImageBitmap(null)
                findViewById<CardView>(R.id.imagePreviewContainer).visibility = View.GONE
            }
        }

        imageButton.setOnClickListener {
            val options = arrayOf("Take Photo", "Choose from Gallery")
            AlertDialog.Builder(this)
                .setTitle("Select Image")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> startActivityForResult(Intent(MediaStore.ACTION_IMAGE_CAPTURE), CAMERA_CODE)
                        1 -> startActivityForResult(Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI), IMAGE_PICK_CODE)
                    }
                }.show()
        }

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), 100)
        }

        closeChatBtn.setOnClickListener {
           navigateBackToDashboard()
        }
    }

    private fun setupQuickEducationButtons() {
        val buttonContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16, 8, 16, 8)
        }

        val educationTopics = arrayOf(
            "🐕 Dog Training Tips",
            "🐱 Cat Behaviour",
            "🦴 Pet Nutrition",
            "🏥 Emergency Care",
            "✂️ Grooming Guide",
            "🎾 Exercise Needs",
            "🧠 Mental Stimulation",
            "👶 Puppy Care",
            "👴 Senior Pet Care"
        )

        educationTopics.forEach { topic ->
            val button = Button(this).apply {
                text = topic
                textSize = 12f
                setPadding(24, 12, 24, 12)
                setTextColor(ContextCompat.getColor(context, android.R.color.white))
                background = ContextCompat.getDrawable(context, R.drawable.quick_education_button_bg)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(8, 0, 8, 0)
                }

                setOnClickListener {
                    val educationQuestion = when (topic) {
                        "🐕 Dog Training Tips" -> "Can you give me some effective dog training tips for beginners?"
                        "🐱 Cat Behaviour" -> "Help me understand common cat behaviours and what they mean."
                        "🦴 Pet Nutrition" -> "What should I know about proper nutrition for my pet?"
                        "🏥 Emergency Care" -> "What are the signs of pet emergencies I should watch for?"
                        "✂️ Grooming Guide" -> "How often should I groom my pet and what's the best way to do it?"
                        "🎾 Exercise Needs" -> "How much exercise does my pet need daily?"
                        "🧠 Mental Stimulation" -> "What are good ways to provide mental stimulation for pets?"
                        "👶 Puppy Care" -> "What are the essential things to know about puppy care?"
                        "👴 Senior Pet Care" -> "How should I care for my senior pet's changing needs?"
                        else -> topic
                    }

                    // Set the question in the input field and automatically send it
                    userInput.setText(educationQuestion)
                    // Automatically send the question
                    addMessageBubble(educationQuestion, isUser = true, image = null)
                    callChatGPTWithImage(educationQuestion)
                    userInput.text.clear()
                }
            }
            buttonContainer.addView(button)
        }

        quickEducationContainer.addView(buttonContainer)
    }

    private fun loadUserAvatar() {
        val userId = UserSessionManager.getCurrentUserId(this)
        if (userId != null) {
            // First, get data from Firebase Auth (for Google Sign-In users)
            val currentUser = auth.currentUser
            val googlePhotoUrl = currentUser?.photoUrl?.toString()

            val dbRef = FirebaseDatabase.getInstance().getReference("user").child(userId)
            dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val avatarUrl = snapshot.child("avatarUrl").getValue(String::class.java)

                        // Determine which photo to use - prefer database, fallback to Google
                        userAvatarUrl = if (!avatarUrl.isNullOrEmpty()) {
                            avatarUrl
                        } else {
                            googlePhotoUrl
                        }
                    } else {
                        // If no database record exists, use Google Sign-In data
                        userAvatarUrl = googlePhotoUrl
                    }

                    Log.d(TAG, "User avatar URL loaded: $userAvatarUrl")
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Failed to load user avatar: ${error.message}")
                    // Fallback to Google Sign-In data on database error
                    userAvatarUrl = googlePhotoUrl
                }
            })
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            selectedBitmap = when (requestCode) {
                CAMERA_CODE -> data?.extras?.get("data") as? Bitmap
                IMAGE_PICK_CODE -> data?.data?.let {
                    MediaStore.Images.Media.getBitmap(contentResolver, it)
                }
                else -> null
            }

            selectedBitmap?.let {
                base64Image = encodeImageToBase64(it)
                attachedImagePreview.setImageBitmap(it)
                findViewById<CardView>(R.id.imagePreviewContainer).visibility = View.VISIBLE
            }
        }
    }



    private fun addMessageBubble(text: String, isUser: Boolean, image: Bitmap?) {
        val verticalLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 16, 0, 16)
        }

        // Only create text bubble if there's actual text content
        if (text.isNotEmpty()) {
            val horizontalLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = if (isUser) android.view.Gravity.END else android.view.Gravity.START
            }

            val icon = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(80, 80).apply {
                    setMargins(12, 0, 12, 0)
                }
                scaleType = ImageView.ScaleType.CENTER_CROP

                if (isUser) {
                    // Load user avatar or use default
                    if (!userAvatarUrl.isNullOrEmpty()) {
                        Glide.with(this@AiChatbotPage)
                            .load(userAvatarUrl)
                            .placeholder(R.drawable.icuser)
                            .error(R.drawable.icuser)
                            .circleCrop()
                            .into(this)
                    } else {
                        setImageResource(R.drawable.icuser)
                    }
                } else {
                    // Bot icon
                    setImageResource(R.drawable.icvet)
                }
            }

            val textView = TextView(this).apply {
                this.text = text
                setBackgroundResource(if (isUser) R.drawable.chatbubble_user_pawlife else R.drawable.chatbubble_bot_pawlife)
                setPadding(24, 16, 24, 16)
                setTextColor(resources.getColor(android.R.color.black))
                textSize = 16f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(12, 0, 12, 0)
                }
            }

            if (isUser) {
                horizontalLayout.addView(textView)
                horizontalLayout.addView(icon)
            } else {
                horizontalLayout.addView(icon)
                horizontalLayout.addView(textView)
            }

            verticalLayout.addView(horizontalLayout)
        }

        // Add image if present
        image?.let {
            val imageLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = if (isUser) android.view.Gravity.END else android.view.Gravity.START
                setPadding(0, if (text.isEmpty()) 0 else 8, 0, 0) // No top padding if no text above
            }

            // Add user icon for image-only messages
            if (text.isEmpty()) {
                val icon = ImageView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(80, 80).apply {
                        setMargins(12, 0, 12, 0)
                    }
                    scaleType = ImageView.ScaleType.CENTER_CROP

                    if (isUser) {
                        if (!userAvatarUrl.isNullOrEmpty()) {
                            Glide.with(this@AiChatbotPage)
                                .load(userAvatarUrl)
                                .placeholder(R.drawable.icuser)
                                .error(R.drawable.icuser)
                                .circleCrop()
                                .into(this)
                        } else {
                            setImageResource(R.drawable.icuser)
                        }
                    } else {
                        setImageResource(R.drawable.icvet)
                    }
                }

                if (isUser) {
                    // For user: image first, then icon
                    // We'll add icon after image below
                } else {
                    // For bot: icon first, then image
                    imageLayout.addView(icon)
                }
            }

            val imageView = ImageView(this).apply {
                setImageBitmap(it)
                layoutParams = LinearLayout.LayoutParams(400, LinearLayout.LayoutParams.WRAP_CONTENT)
                adjustViewBounds = true
                setPadding(10, 10, 10, 10)
                setBackgroundResource(R.drawable.chatbubble_bot_pawlife)
            }

            if (isUser) {
                if (text.isEmpty()) {
                    // Image-only message: add image, then icon
                    imageLayout.addView(imageView)
                    val icon = ImageView(this).apply {
                        layoutParams = LinearLayout.LayoutParams(80, 80).apply {
                            setMargins(12, 0, 12, 0)
                        }
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        if (!userAvatarUrl.isNullOrEmpty()) {
                            Glide.with(this@AiChatbotPage)
                                .load(userAvatarUrl)
                                .placeholder(R.drawable.icuser)
                                .error(R.drawable.icuser)
                                .circleCrop()
                                .into(this)
                        } else {
                            setImageResource(R.drawable.icuser)
                        }
                    }
                    imageLayout.addView(icon)
                } else {
                    // Message with text: align with existing text bubble
                    val spacer = Space(this).apply {
                        layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
                    }
                    imageLayout.addView(spacer)
                    imageLayout.addView(imageView)
                }
            } else {
                if (text.isEmpty()) {
                    // Bot image-only message: icon already added above
                    imageLayout.addView(imageView)
                } else {
                    // Bot message with text: align with existing text bubble
                    val iconSpacer = Space(this).apply {
                        layoutParams = LinearLayout.LayoutParams(104, 0) // 80dp + margins
                    }
                    imageLayout.addView(iconSpacer)
                    imageLayout.addView(imageView)
                }
            }

            verticalLayout.addView(imageLayout)
        }

        messageContainer.addView(verticalLayout)
        scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }

        // Add translation option for bot messages (only if there's text)
        if (!isUser && text.isNotEmpty()) {
            val translatePrompt = TextView(this).apply {
                this.text = "🌐 Translate "
                textSize = 14f
                setTextColor(ContextCompat.getColor(context, android.R.color.holo_blue_dark))
                setPadding(104, 10, 20, 0) // Align with bot message
                textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                isClickable = true
                isFocusable = true
            }

            translatePrompt.setOnClickListener {
                val options = arrayOf("English", "Malay", "Chinese")
                AlertDialog.Builder(this)
                    .setTitle("Choose Translation Language")
                    .setItems(options) { _, which ->
                        val langPrompt = when (options[which]) {
                            "Malay" -> "Translate this to Malay:"
                            "Chinese" -> "Translate this to Chinese:"
                            else -> "Translate this to English:"
                        }

                        val label = options[which]
                        translateTextWithGPT(langPrompt, text, verticalLayout, label)
                    }.show()
            }

            verticalLayout.addView(translatePrompt)
        }
    }

    private fun encodeImageToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }

    private fun callChatGPTWithImage(userMessage: String) {
        loadingCard.visibility = View.VISIBLE

        val apiKey = "API KEY"
        val apiUrl = "https://api.openai.com/v1/chat/completions"

        val contentArray = JSONArray()

        selectedBitmap?.let {
            if (base64Image == null) {
                base64Image = encodeImageToBase64(it)
            }

            contentArray.put(JSONObject().apply {
                put("type", "image_url")
                put("image_url", JSONObject().apply {
                    put("url", "data:image/jpeg;base64,$base64Image")
                })
            })
        }

        if (userMessage.isNotEmpty()) {
            contentArray.put(JSONObject().apply {
                put("type", "text")
                put("text", userMessage)
            })
        }

        val messages = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "system")
                put("content", """You are Doctor Paw, a specialized veterinary AI assistant focused exclusively on pet education and care. 

Your expertise covers:
- Pet health, diseases, symptoms, and treatments
- Pet behavior, training, and psychology  
- Pet nutrition and diet recommendations
- Pet grooming and hygiene
- Pet care routines and wellness
- Different pet species (dogs, cats, birds, rabbits, fish, reptiles, etc.)
- Pet safety and emergency care
- Pet products and accessories

IMPORTANT RULES:
1. ONLY answer questions related to pets, animals, and pet care
2. If a user asks about anything unrelated to pets (human health, technology, general knowledge, etc.), politely redirect them by saying: "I'm Doctor Paw, your specialized pet care assistant. I can only help with questions about pets, animals, and pet care. Please ask me something about your furry, feathered, or scaly friends!"
3. Always provide helpful, accurate pet care advice
4. Recommend consulting with a real veterinarian for serious health concerns
5. Be friendly and educational in your responses""")
            })
            put(JSONObject().apply {
                put("role", "user")
                put("content", contentArray)
            })
        }

        val jsonBody = JSONObject().apply {
            put("model", "gpt-4o")
            put("messages", messages)
            put("max_tokens", 1000)
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(apiUrl)
            .addHeader("Authorization", apiKey)
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        val client = OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    loadingCard.visibility = View.GONE
                    addMessageBubble("Sorry, I'm having trouble connecting right now. Please try again later. Error: ${e.message}", isUser = false, image = null)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                val rawReply = try {
                    JSONObject(body ?: "").getJSONArray("choices")
                        .getJSONObject(0).getJSONObject("message").getString("content")
                } catch (e: Exception) {
                    "Sorry, I couldn't process that request properly. Please try again."
                }

                val cleanedReply = rawReply
                    .replace("***", "")
                    .replace("**", "")
                    .replace("*", "")
                    .replace("\n", "\n\n")
                    .replace("\\*", "")
                    .replace("^#{1,6}\\s*".toRegex(), "")

                runOnUiThread {
                    loadingCard.visibility = View.GONE
                    addMessageBubble(cleanedReply, isUser = false, image = null)
                }
            }
        })
    }

    private fun translateTextWithGPT(prompt: String, originalText: String, container: LinearLayout, languageLabel: String) {
        val apiKey = "API KEY"
        val apiUrl = "https://api.openai.com/v1/chat/completions"

        val messages = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "system")
                put("content", "You are a helpful assistant that translates veterinary and pet care advice accurately.")
            })
            put(JSONObject().apply {
                put("role", "user")
                put("content", "$prompt\n$originalText")
            })
        }

        val jsonBody = JSONObject().apply {
            put("model", "gpt-4o")
            put("messages", messages)
            put("max_tokens", 500)
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(apiUrl)
            .addHeader("Authorization", apiKey)
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        runOnUiThread {
            loadingCard.visibility = View.VISIBLE
        }

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    loadingCard.visibility = View.GONE
                    Toast.makeText(this@AiChatbotPage, "Translation failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseText = response.body?.string()
                val translatedReply = try {
                    JSONObject(responseText ?: "").getJSONArray("choices")
                        .getJSONObject(0).getJSONObject("message").getString("content")
                } catch (e: Exception) {
                    "Translation error occurred."
                }

                runOnUiThread {
                    loadingCard.visibility = View.GONE
                    val translationLayout = LinearLayout(this@AiChatbotPage).apply {
                        orientation = LinearLayout.HORIZONTAL
                        setPadding(0, 12, 0, 12)
                        gravity = android.view.Gravity.START
                    }

                    val botIcon = ImageView(this@AiChatbotPage).apply {
                        setImageResource(R.drawable.icvet)
                        layoutParams = LinearLayout.LayoutParams(80, 80).apply {
                            setMargins(12, 0, 12, 0)
                        }
                    }

                    val translationBubble = TextView(this@AiChatbotPage).apply {
                        text = "🌐 Translation ($languageLabel):\n$translatedReply"
                        setBackgroundResource(R.drawable.chatbubble_bot_pawlife)
                        setPadding(24, 16, 24, 16)
                        setTextColor(ContextCompat.getColor(context, android.R.color.black))
                        textSize = 14f
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                            setMargins(12, 0, 12, 0)
                        }
                    }

                    translationLayout.addView(botIcon)
                    translationLayout.addView(translationBubble)

                    container.addView(translationLayout)
                    scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
                }
            }
        })
    }
    private fun navigateBackToDashboard() {
        finish()
    }
}
