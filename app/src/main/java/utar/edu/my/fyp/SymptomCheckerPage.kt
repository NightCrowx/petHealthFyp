package utar.edu.my.fyp

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class SymptomCheckerPage : AppCompatActivity() {

    private lateinit var petImage: ImageView
    private lateinit var symptomInput: EditText
    private lateinit var resultText: TextView
    private var selectedImageUri: Uri? = null

    private val IMAGE_PICK_CODE = 1001
    private val CAMERA_CODE = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_symptom_checker_page)

        petImage = findViewById(R.id.petImage)
        symptomInput = findViewById(R.id.symptomInput)
        resultText = findViewById(R.id.resultText)

        findViewById<Button>(R.id.btnUploadImage).setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, IMAGE_PICK_CODE)
        }

        findViewById<Button>(R.id.btnOpenCamera).setOnClickListener {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(intent, CAMERA_CODE)
        }

        findViewById<Button>(R.id.btnAnalyze).setOnClickListener {
            val symptomText = symptomInput.text.toString()
            if (selectedImageUri != null && symptomText.isNotEmpty()) {
                processDiagnosis(selectedImageUri!!, symptomText)
            } else {
                Toast.makeText(
                    this,
                    "Please select an image and describe symptoms",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == IMAGE_PICK_CODE && data != null) {
                selectedImageUri = data.data
                petImage.setImageURI(selectedImageUri)
            } else if (requestCode == CAMERA_CODE && data != null) {
                val photo = data.extras?.get("data") as? Bitmap
                selectedImageUri = getImageUriFromBitmap(photo)
                petImage.setImageURI(selectedImageUri)
            }
        }
    }

    private fun getImageUriFromBitmap(bitmap: Bitmap?): Uri? {
        val bytes = ByteArrayOutputStream()
        bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(contentResolver, bitmap, "PetImage", null)
        return Uri.parse(path)
    }

    private fun encodeImageToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    private fun processDiagnosis(imageUri: Uri, symptoms: String) {
        val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
        val base64Image = encodeImageToBase64(bitmap)

        CoroutineScope(Dispatchers.IO).launch {
            val response = callDonutModelAPI(base64Image, symptoms)
            withContext(Dispatchers.Main) {
                resultText.text = response
            }
        }
    }

    private fun callDonutModelAPI(base64Image: String, question: String): String {
        val apiUrl = "https://router.huggingface.co/hf-inference/pipeline/feature-extraction/dmis-lab/biobert-v1.1"
        val apiToken = "Bearer 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"

        val jsonRequest = JSONObject().apply {
            put("image", base64Image)
            put("question", question)
        }

        val client = OkHttpClient()
        val requestBody = RequestBody.create(
            "application/json".toMediaType(),
            jsonRequest.toString()
        )

        val request = Request.Builder()
            .url(apiUrl)
            .addHeader("Authorization", apiToken)
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        return try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.string() ?: "No response body"
            } else {
                "❌ Error: ${response.code}"
            }
        } catch (e: Exception) {
            "❌ Exception: ${e.message}"
        }
    }
}
