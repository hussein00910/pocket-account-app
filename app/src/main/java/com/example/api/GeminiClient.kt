package com.example.api

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: Content? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    @Json(name = "candidates") val candidates: List<Candidate>? = null
)

interface GeminiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(GeminiService::class.java)
    }

    suspend fun getFinancialInsight(ledgerSummary: String): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey == "MY_GEMINI_API_KEY" || apiKey.isBlank()) {
            return "Please configure your GEMINI_API_KEY in the AI Studio Secrets panel to enable real-time financial coach analysis."
        }
        val systemPrompt = """
            You are "Pocket Ledger Coach", a friendly, brilliant, and hyper-practical financial advisor.
            Given a user's transaction ledger summary, budgets, and net worth, analyze their financial trajectory and habits.
            Keep your analysis highly punchy, constructive, and structured. 
            Identify:
            1. Spending patterns & categories with unusually high expenses.
            2. Any budget leaks or upcoming challenges.
            3. Exactly 3 highly specific, highly actionable tips the user can apply today to save more.
            Format your response in a charming, spaced layout using bullet points. Refrain from boilerplate and keep it conversational yet direct. Limit responses to 200 words.
        """.trimIndent()

        val req = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = "Here is my current transaction log and budget summary:\n\n$ledgerSummary")))
            ),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
        )

        return try {
            val response = service.generateContent(apiKey, req)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "I examined your numbers but couldn't form suggestions right now. Try adding more transactions!"
        } catch (e: Exception) {
            "Pocket Ledger Coach is offline or rate-limited. Error details: ${e.localizedMessage ?: "Network error"}. Please check your connection and try again."
        }
    }
}
