package com.example.data.ai

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ChatMessage(
    val role: String, // "user" or "model"
    val text: String,
    val isSheet: Boolean = false,
    val sheetTitle: String? = null
)

class GeminiAiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val systemPrompt = """
        Tu es l'Assistant Bac IA de "Mon Classeur Bac", un tuteur pédagogique d'excellence spécialisé dans le programme officiel du Baccalauréat tunisien (sections: Sciences expérimentales, Mathématiques, Technique, Économie et Gestion, Lettres, Informatique).
        Tu réponds avec bienveillance, précision académique et clarté.
        Tu maîtrises le français et l'arabe et réponds dans la langue choisie par l'élève.
        Pour les explications scientifiques et formules, utilise une mise en page structurée avec des titres clairs, des puces, et des conseils pratiques pour l'épreuve du Bac.
    """.trimIndent()

    companion object {
        const val DEFAULT_API_KEY = "AIzaSyAykWKD5d9qxr6veTHajsDY8JkIfO1YwPA"
    }

    fun getApiKey(customKey: String? = null): String {
        val trimmedCustom = customKey?.trim()
        if (!trimmedCustom.isNullOrBlank() && trimmedCustom != "MY_GEMINI_API_KEY" && trimmedCustom != "PLACEHOLDER") {
            return trimmedCustom
        }
        val buildKey = try {
            BuildConfig.GEMINI_API_KEY.trim()
        } catch (_: Exception) {
            ""
        }
        if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY" && buildKey != "PLACEHOLDER" && !buildKey.startsWith("AQ.")) {
            return buildKey
        }
        return DEFAULT_API_KEY
    }

    fun hasValidApiKey(customKey: String? = null): Boolean {
        val key = getApiKey(customKey)
        return key.isNotBlank() && key != "MY_GEMINI_API_KEY" && key != "PLACEHOLDER"
    }

    suspend fun testApiKey(apiKeyToTest: String): Result<String> = withContext(Dispatchers.IO) {
        val key = getApiKey(apiKeyToTest)
        try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent?key=$key"
            val requestJson = JSONObject()
                .put("contents", JSONArray().put(
                    JSONObject().put("parts", JSONArray().put(JSONObject().put("text", "Bonjour, réponds en un mot: OK")))
                ))
            val body = requestJson.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder().url(endpoint).post(body).build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    Result.success("Connexion réussie avec l'Assistant Gemini !")
                } else {
                    val errorMsg = try {
                        JSONObject(responseBody).optJSONObject("error")?.optString("message") ?: "Erreur HTTP ${response.code}"
                    } catch (_: Exception) {
                        "Erreur HTTP ${response.code}"
                    }
                    Result.failure(IllegalStateException(errorMsg))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateResponse(
        prompt: String,
        history: List<ChatMessage> = emptyList(),
        customKey: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey(customKey)
        if (!hasValidApiKey(customKey)) {
            return@withContext Result.failure(
                IllegalStateException("Clé API Gemini non configurée.")
            )
        }

        try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val contentsArray = JSONArray()

            // Append history turns
            history.takeLast(6).forEach { msg ->
                val partObj = JSONObject().put("text", msg.text)
                val partsArray = JSONArray().put(partObj)
                val contentObj = JSONObject()
                    .put("role", if (msg.role == "user") "user" else "model")
                    .put("parts", partsArray)
                contentsArray.put(contentObj)
            }

            // Current prompt
            val currentPart = JSONObject().put("text", prompt)
            val currentContent = JSONObject()
                .put("role", "user")
                .put("parts", JSONArray().put(currentPart))
            contentsArray.put(currentContent)

            // System instruction
            val systemInstruction = JSONObject().put(
                "parts",
                JSONArray().put(JSONObject().put("text", systemPrompt))
            )

            val requestJson = JSONObject()
                .put("contents", contentsArray)
                .put("systemInstruction", systemInstruction)
                .put(
                    "generationConfig",
                    JSONObject()
                        .put("temperature", 0.7)
                        .put("maxOutputTokens", 2048)
                )

            val body = requestJson.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(endpoint)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val errorMsg = try {
                        JSONObject(responseBody).optJSONObject("error")?.optString("message") ?: "Erreur ${response.code}"
                    } catch (_: Exception) {
                        "Erreur HTTP ${response.code}"
                    }
                    return@withContext Result.failure(IllegalStateException(errorMsg))
                }

                val jsonResponse = JSONObject(responseBody)
                val candidates = jsonResponse.optJSONArray("candidates")
                val firstCandidate = candidates?.optJSONObject(0)
                val content = firstCandidate?.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                val text = parts?.optJSONObject(0)?.optString("text")

                if (!text.isNullOrBlank()) {
                    Result.success(text)
                } else {
                    Result.failure(IllegalStateException("Réponse vide de l'assistant."))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateRevisionSheetContent(
        subjectName: String,
        chapterName: String?,
        sheetType: String,
        topicDetails: String,
        customKey: String? = null
    ): Result<String> {
        val prompt = """
            Rédige une fiche de révision complète et pédagogique pour le Baccalauréat tunisien.
            Matière : $subjectName
            Chapitre : ${chapterName ?: "Général"}
            Type de fiche : $sheetType
            Sujet / Notions clés : $topicDetails

            Structure la fiche impérativement avec :
            1. TITRE DE LA FICHE (en lettres capitales)
            2. OBJECTIFS & DÉFINITIONS CLÉS
            3. LOIS, FORMULES ET RÈGLES ESSENTIELLES (très précises)
            4. MÉTHODOLOGIE DE RÉSOLUTION & ÉTAPES BAC
            5. PIÈGES FRÉQUENTS ET CONSEILS DE L'EXAMINATEUR

            Reste concis, clair, synthétique et directement utile pour réviser la veille d'un devoir de contrôle ou de l'épreuve du Bac.
        """.trimIndent()

        return generateResponse(prompt, customKey = customKey)
    }
}
