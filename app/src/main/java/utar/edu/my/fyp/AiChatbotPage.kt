package utar.edu.my.fyp

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
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

class AiChatbotPage : AppCompatActivity() {

    private lateinit var messageContainer: LinearLayout
    private lateinit var userInput: EditText
    private lateinit var sendBtn: Button
    private lateinit var scrollView: ScrollView
    private lateinit var imageButton: ImageButton
    private lateinit var attachedImagePreview: ImageView
    private lateinit var loadingIndicator: ProgressBar

    private var base64Image: String? = null
    private var selectedBitmap: Bitmap? = null
    private val IMAGE_PICK_CODE = 1001
    private val CAMERA_CODE = 1002


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai_chatbot_page)

        messageContainer = findViewById(R.id.messageContainer)
        userInput = findViewById(R.id.userInput)
        sendBtn = findViewById(R.id.sendBtn)
        scrollView = findViewById(R.id.scrollView)
        imageButton = findViewById(R.id.imageSelectBtn)
        loadingIndicator = findViewById(R.id.loadingIndicator)
        attachedImagePreview = findViewById(R.id.attachedImagePreview)

        addMessageBubble("👋 Hello! I'm Doctor Edu. How can I help you?", isUser = false,null)

        val removeImageBtn = findViewById<ImageButton>(R.id.removeImageBtn)
        val imagePreviewContainer = findViewById<FrameLayout>(R.id.imagePreviewContainer)

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
                findViewById<FrameLayout>(R.id.imagePreviewContainer).visibility = View.GONE
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
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        attachedImagePreview.setImageBitmap(selectedBitmap)
        attachedImagePreview.visibility = View.VISIBLE
        findViewById<FrameLayout>(R.id.imagePreviewContainer).visibility = View.VISIBLE

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
                findViewById<FrameLayout>(R.id.imagePreviewContainer).visibility = View.VISIBLE
            }
        }
    }


    private fun addMessageBubble(text: String, isUser: Boolean, image: Bitmap?) {
        val verticalLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 12, 0, 12)
        }

        val horizontalLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val icon = ImageView(this).apply {
            setImageResource(if (isUser) R.drawable.icuser else R.drawable.icvet)
            layoutParams = LinearLayout.LayoutParams(80, 80)
        }

        val textView = TextView(this).apply {
            this.text = text
            setBackgroundResource(if (isUser) R.drawable.chatbubble else R.drawable.chatbubblebot)
            setPadding(24, 16, 24, 16)
            setTextColor(resources.getColor(android.R.color.black))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(20, 0, 20, 0)
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


        image?.let {
            val imageLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            val imageView = ImageView(this).apply {
                setImageBitmap(it)
                layoutParams = LinearLayout.LayoutParams(400, LinearLayout.LayoutParams.WRAP_CONTENT)
                adjustViewBounds = true
                setPadding(10, 10, 10, 10)
            }

            if (isUser) {

                val spacer = Space(this).apply {
                    layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
                }
                imageLayout.addView(spacer)
                imageLayout.addView(imageView)
            } else {

                imageLayout.addView(imageView)
            }

            verticalLayout.addView(imageLayout)
        }

        messageContainer.addView(verticalLayout)
        scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }


        if (!isUser) {
            val translatePrompt = TextView(this).apply {
                this.text = "🌐 Translate "
                textSize = 14f
                setTextColor(ContextCompat.getColor(context, android.R.color.holo_blue_dark))
                setPadding(0, 10, 20, 0)
                textAlignment = View.TEXT_ALIGNMENT_VIEW_END
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
        loadingIndicator.visibility = View.VISIBLE

        val apiKey = "Bearer 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"
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
                put("content", "You are a helpful study assistant. Analyze image and user question.")
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
                    loadingIndicator.visibility = View.GONE
                    addMessageBubble(" Error: ${e.message}", isUser = false, image = null)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                val rawReply = try {
                    JSONObject(body ?: "").getJSONArray("choices")
                        .getJSONObject(0).getJSONObject("message").getString("content")
                } catch (e: Exception) {
                    "Error parsing response"
                }

                val cleanedReply = rawReply
                    .replace("***", "")
                    .replace("**", "")
                    .replace("*", "")
                    .replace("\n", "\n\n")
                    .replace("\\*", "")
                    .replace("^#{1,6}\\s*", "")

                runOnUiThread {
                    loadingIndicator.visibility = View.GONE
                    addMessageBubble(cleanedReply, isUser = false, image = null)


                }
            }
        })
    }

    private fun translateTextWithGPT(prompt: String, originalText: String, container: LinearLayout,languageLabel: String) {
        val apiKey = "Bearer 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"
        val apiUrl = "https://api.openai.com/v1/chat/completions"

        val messages = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "system")
                put("content", "You are a helpful assistant that translates answers.")
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
            loadingIndicator.visibility = View.VISIBLE
        }

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@AiChatbotPage, "Translation failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseText = response.body?.string()
                val translatedReply = try {
                    JSONObject(responseText ?: "").getJSONArray("choices")
                        .getJSONObject(0).getJSONObject("message").getString("content")
                } catch (e: Exception) {
                    "Translation error."
                }

                runOnUiThread {
                    loadingIndicator.visibility = View.GONE
                    val horizontalLayout = LinearLayout(this@AiChatbotPage).apply {
                        orientation = LinearLayout.HORIZONTAL
                        setPadding(0, 12, 0, 12)
                    }

                    val botIcon = ImageView(this@AiChatbotPage).apply {
                        setImageResource(R.drawable.icvet)
                        layoutParams = LinearLayout.LayoutParams(80, 80)
                    }

                    val translationBubble = TextView(this@AiChatbotPage).apply {
                        text = "🌐 Translate to $languageLabel:\n$translatedReply"
                        setBackgroundResource(R.drawable.chatbubblebot)
                        setPadding(24, 16, 24, 16)
                        setTextColor(ContextCompat.getColor(context, android.R.color.black))
                        textSize = 14f
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                            setMargins(20, 0, 20, 0)
                        }
                    }

                    horizontalLayout.addView(botIcon)
                    horizontalLayout.addView(translationBubble)

                    container.addView(horizontalLayout)
                    scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
                }

            }
        })
    }




}
