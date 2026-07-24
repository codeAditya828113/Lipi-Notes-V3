package com.example.network

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val mediaTypeJson = "application/json; charset=utf-8".toMediaType()

    /**
     * Sends drawing bitmap to Gemini 3.5 Flash for indexing, transcribing, and summarization.
     */
    suspend fun analyzeHandwriting(bitmap: Bitmap): HandwritingAnalysisResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext HandwritingAnalysisResult(
                transcription = "Gemini API key is not configured.",
                summary = "Please set your API key in AI Studio secrets.",
                tags = listOf("Error", "Missing_Key")
            )
        }

        try {
            // Convert bitmap to Base64 JPEG
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
            val base64Image = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

            val promptText = """
                Analyze this handwritten notes canvas. 
                1. Transcribe any handwritten English text, letters, notes, mathematical formulas, or sketches you recognize.
                2. Summarize the contents of the notes.
                3. Provide a list of 3-5 indexing tags.
                
                Provide your response strictly as a JSON object matching this schema:
                {
                  "transcription": "The transcribed text",
                  "summary": "Brief summary of the note contents",
                  "tags": ["tag1", "tag2", "tag3"]
                }
                Do not include any Markdown, backticks, or formatting other than raw JSON.
            """.trimIndent()

            // Build raw request body via standard JSONObject
            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", promptText)
                            })
                            put(JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", "image/jpeg")
                                    put("data", base64Image)
                                })
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.3)
                })
            }

            val requestBody = requestJson.toString().toRequestBody(mediaTypeJson)
            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw Exception("Unsuccessful API call: code ${response.code} - ${response.message}")
                }
                val bodyString = response.body?.string() ?: throw Exception("Empty response body from Gemini.")
                
                // Parse Gemini API Response
                val responseJson = JSONObject(bodyString)
                val candidates = responseJson.optJSONArray("candidates")
                val firstCandidate = candidates?.optJSONObject(0)
                val content = firstCandidate?.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                val firstPart = parts?.optJSONObject(0)
                val textResponse = firstPart?.optString("text") ?: throw Exception("Invalid response structure.")

                // Parse Inner JSON returned by Gemini
                val innerJson = JSONObject(textResponse)
                val transcription = innerJson.optString("transcription", "")
                val summary = innerJson.optString("summary", "")
                val tagsArray = innerJson.optJSONArray("tags")
                val tagsList = mutableListOf<String>()
                if (tagsArray != null) {
                    for (i in 0 until tagsArray.length()) {
                        tagsList.add(tagsArray.optString(i))
                    }
                }

                HandwritingAnalysisResult(transcription, summary, tagsList)
            }
        } catch (e: Exception) {
            HandwritingAnalysisResult(
                transcription = "",
                summary = "Failed to transcribe handwriting: ${e.message}",
                tags = listOf("Error", "Transcription_Failed")
            )
        }
    }

    /**
     * Transcribes audio using gemini-3.5-flash as requested.
     */
    suspend fun transcribeAudio(audioBytes: ByteArray, mimeType: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Gemini API Key missing. Please set your API key in AI Studio."
        }

        try {
            val base64Audio = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
            val promptText = "Please transcribe this spoken audio note accurately into written text. Provide ONLY the final transcription, do not add headers or commentary."

            // Build JSON Request Body
            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", promptText)
                            })
                            put(JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", mimeType)
                                    put("data", base64Audio)
                                })
                            })
                        })
                    })
                })
            }

            val requestBody = requestJson.toString().toRequestBody(mediaTypeJson)
            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw Exception("Unsuccessful API call: code ${response.code} - ${response.message}")
                }
                val bodyString = response.body?.string() ?: throw Exception("Empty response body.")
                
                val responseJson = JSONObject(bodyString)
                val candidates = responseJson.optJSONArray("candidates")
                val firstCandidate = candidates?.optJSONObject(0)
                val content = firstCandidate?.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                val firstPart = parts?.optJSONObject(0)
                
                firstPart?.optString("text") ?: "Transcription not retrieved."
            }
        } catch (e: Exception) {
            "Audio transcription failed: ${e.message}"
        }
    }
}

data class HandwritingAnalysisResult(
    val transcription: String,
    val summary: String,
    val tags: List<String>
)
